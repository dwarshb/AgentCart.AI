package com.dwarshb.a2ui

import com.dwarshb.ai.GeminiVisionService
import com.dwarshb.appfunctions.AgentCartAppFunctions
import com.dwarshb.appfunctions.CreateOrderParams
import com.dwarshb.appfunctions.OrderResult
import com.dwarshb.appfunctions.ProductSearchParams
import com.dwarshb.appfunctions.ProductSearchResult
import com.dwarshb.auth.PayPalAuthManager
import com.dwarshb.data.OrderHistoryRepository
import com.dwarshb.data.OrderRecord
import com.dwarshb.network.Channel3Offer
import com.dwarshb.network.DiscoveredProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class A2UIEngine(private val scope: CoroutineScope) {

    private val _conversation = MutableStateFlow(
        A2UIConversationState(
            messages = listOf(
                A2UIMessage(
                    sender = A2UISender.GEMINI_AGENT,
                    text = "Hello! I am your Gemini Commerce Agent with A2UI & AppFunctions capabilities. Tell me what product you're looking for, and I'll invoke Channel3 AppFunctions to find verified merchant prices and let you place a 1-click PayPal order right here.",
                    widgetType = A2UIWidgetType.TEXT_ONLY
                )
            )
        )
    )
    val conversation: StateFlow<A2UIConversationState> = _conversation.asStateFlow()

    /**
     * Sends user natural language query to Gemini A2UI engine.
     * Gemini orchestrates the searchProduct AppFunction with Channel3 API.
     */
    fun sendUserPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val userMsg = A2UIMessage(
            sender = A2UISender.USER,
            text = trimmed,
            widgetType = A2UIWidgetType.TEXT_ONLY
        )

        val currentList = _conversation.value.messages.toMutableList()
        currentList.add(userMsg)
        _conversation.value = _conversation.value.copy(
            messages = currentList,
            isBusy = true,
            activeQuery = trimmed
        )

        scope.launch {
            processAgentRequest(trimmed)
        }
    }

    private suspend fun processAgentRequest(query: String) = withContext(Dispatchers.Default) {
        // Step 1: Clean search terms from conversational queries
        val cleanedQuery = extractSearchKeywords(query)

        // Step 2: Emit AppFunction tool execution notice (OS-level AppFunction pattern)
        val appFnMsg = A2UIMessage(
            sender = A2UISender.APP_FUNCTION_EXECUTION,
            text = "Executing AppFunction: searchProduct(query = \"$cleanedQuery\") via Channel3 Commerce API...",
            widgetType = A2UIWidgetType.TEXT_ONLY,
            isExecutingAppFunction = true,
            appFunctionName = "AgentCartAppFunctions#searchProduct",
            appFunctionParams = "{\"query\": \"$cleanedQuery\"}"
        )

        updateMessages { add(appFnMsg) }
        delay(600) // Brief UI cadence for agent thinking

        // Step 3: Execute AppFunction against Channel3 API
        val productResult = try {
            AgentCartAppFunctions.searchProduct(ProductSearchParams(query = cleanedQuery))
        } catch (e: Exception) {
            null
        }

        if (productResult != null) {
            // Step 4: Emit Gemini Conversational Summary + A2UI Dynamic Product Card
            val summaryText = "I queried the Channel3 API through AppFunctions and discovered verified store offers for **${productResult.title}** starting at **${productResult.price}**. You can review the merchant details and place your order directly with 1-click PayPal below:"

            val a2uiCardMsg = A2UIMessage(
                sender = A2UISender.GEMINI_AGENT,
                text = summaryText,
                widgetType = A2UIWidgetType.PRODUCT_MATCH_CARD,
                product = productResult
            )

            updateMessages {
                // Remove the in-flight indicator and add final A2UI widget
                removeAll { it.isExecutingAppFunction }
                add(a2uiCardMsg)
            }

            _conversation.value = _conversation.value.copy(
                isBusy = false,
                currentSelectedProduct = productResult
            )
        } else {
            val errorMsg = A2UIMessage(
                sender = A2UISender.GEMINI_AGENT,
                text = "I couldn't locate active merchant inventory for '$cleanedQuery'. Please try another product name like 'Sony WH-1000XM5', 'Anker 65W Charger', or 'AirPods Pro'.",
                widgetType = A2UIWidgetType.ERROR_NOTICE
            )

            updateMessages {
                removeAll { it.isExecutingAppFunction }
                add(errorMsg)
            }

            _conversation.value = _conversation.value.copy(isBusy = false)
        }
    }

    /**
     * Executes the placeOrder AppFunction directly from the A2UI widget.
     */
    fun executeOrderFromA2UI(product: ProductSearchResult, onApprovalUrlReceived: ((String) -> Unit)? = null) {
        _conversation.value = _conversation.value.copy(isBusy = true)

        scope.launch {
            val appFnOrderMsg = A2UIMessage(
                sender = A2UISender.APP_FUNCTION_EXECUTION,
                text = "Executing AppFunction: placeOrder(productId = \"${product.id}\", price = \"${product.price}\") with PayPal Sandbox...",
                widgetType = A2UIWidgetType.TEXT_ONLY,
                isExecutingAppFunction = true,
                appFunctionName = "AgentCartAppFunctions#placeOrder",
                appFunctionParams = "{\"productId\": \"${product.id}\", \"price\": \"${product.price}\"}"
            )

            updateMessages { add(appFnOrderMsg) }
            delay(800)

            val orderResult: OrderResult = try {
                AgentCartAppFunctions.placeOrder(
                    CreateOrderParams(
                        productId = product.id,
                        productTitle = product.title,
                        price = product.price
                    )
                )
            } catch (e: Exception) {
                OrderResult(
                    orderId = "ERR-ORD-${System.currentTimeMillis() % 10000}",
                    status = "FAILED",
                    amount = product.price,
                    message = e.localizedMessage ?: "Payment failed"
                )
            }

            // Save completed order record
            val currentUser = PayPalAuthManager.currentUser.value
            val payerLabel = if (currentUser.isSignedIn) "PayPal (${currentUser.name})" else "PayPal Sandbox 1-Click"

            val record = OrderRecord(
                orderId = orderResult.orderId,
                product = DiscoveredProduct(
                    id = product.id,
                    title = product.title,
                    price = product.price,
                    confidenceScore = product.confidenceScore,
                    merchantName = product.merchant,
                    description = product.description,
                    category = product.category,
                    visionModel = "Gemini A2UI / AppFunctions",
                    offers = product.offers.map {
                        Channel3Offer(
                            merchant = it.merchant,
                            price = it.price,
                            availability = it.availability,
                            condition = it.condition
                        )
                    }
                ),
                status = orderResult.status,
                paymentMethod = payerLabel
            )
            OrderHistoryRepository.addOrder(record)

            val successMsg = A2UIMessage(
                sender = A2UISender.GEMINI_AGENT,
                text = "🎉 **Order Placed Successfully!**\nYour 1-click PayPal transaction for **${product.title}** has been processed via AppFunctions.",
                widgetType = A2UIWidgetType.ORDER_CONFIRMATION,
                product = product,
                orderResult = orderResult
            )

            updateMessages {
                removeAll { it.isExecutingAppFunction }
                add(successMsg)
            }

            _conversation.value = _conversation.value.copy(
                isBusy = false,
                activeOrder = orderResult
            )

            if (!orderResult.approvalUrl.isNullOrBlank()) {
                onApprovalUrlReceived?.invoke(orderResult.approvalUrl)
            }
        }
    }

    private fun extractSearchKeywords(input: String): String {
        val prefixes = listOf(
            "find me a", "find me an", "find me", "find",
            "search for a", "search for an", "search for", "search",
            "look up a", "look up an", "look up", "look for",
            "buy a", "buy an", "buy", "order a", "order an", "order",
            "gemini find", "can you find", "where can i buy"
        )
        var cleaned = input.trim()
        val lower = cleaned.lowercase()
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length).trim()
                break
            }
        }
        return cleaned.ifEmpty { input.trim() }
    }

    private fun updateMessages(block: MutableList<A2UIMessage>.() -> Unit) {
        val list = _conversation.value.messages.toMutableList()
        list.block()
        _conversation.value = _conversation.value.copy(messages = list)
    }

    fun clearConversation() {
        _conversation.value = A2UIConversationState(
            messages = listOf(
                A2UIMessage(
                    sender = A2UISender.GEMINI_AGENT,
                    text = "Conversation reset. Ask me to find any merchandise, and I'll use Channel3 AppFunctions to present live purchase options.",
                    widgetType = A2UIWidgetType.TEXT_ONLY
                )
            )
        )
    }
}
