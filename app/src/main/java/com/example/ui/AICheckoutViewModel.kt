package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GemmaOnDeviceEngine
import com.example.data.OrderHistoryRepository
import com.example.data.OrderRecord
import com.example.network.DiscoveredProduct
import com.example.network.NetworkClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException

sealed class AICheckoutUiState {
    object Idle : AICheckoutUiState()
    data class ProcessingAI(val statusMessage: String = "Gemma-4 On-Device analyzing product intent...") : AICheckoutUiState()
    data class ReviewMatch(
        val product: DiscoveredProduct,
        val source: String = "Gemma-4 On-Device Engine"
    ) : AICheckoutUiState()
    object ExecutingPayment : AICheckoutUiState()
    data class WaitingForPayPalApproval(
        val orderId: String,
        val approvalUrl: String,
        val product: DiscoveredProduct
    ) : AICheckoutUiState()

    object CapturingPayment : AICheckoutUiState()
    data class Success(
        val transactionId: String,
        val product: DiscoveredProduct,
        val zapierTriggered: Boolean = true
    ) : AICheckoutUiState()
    data class Error(val message: String) : AICheckoutUiState()
}

class AICheckoutViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AICheckoutUiState>(AICheckoutUiState.Idle)
    val uiState: StateFlow<AICheckoutUiState> = _uiState.asStateFlow()

    private val _backendUrl = MutableStateFlow(NetworkClient.baseUrl)
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    // Default to true so user can immediately use Gemma-4 on-device without cloud API key friction
    private val _useGemmaOnDevice = MutableStateFlow(true)
    val useGemmaOnDevice: StateFlow<Boolean> = _useGemmaOnDevice.asStateFlow()

    private val _selectedProduct = MutableStateFlow<DiscoveredProduct?>(null)
    val selectedProduct: StateFlow<DiscoveredProduct?> = _selectedProduct.asStateFlow()

    private val _useSandboxFallback = MutableStateFlow(false)
    val useSandboxFallback: StateFlow<Boolean> = _useSandboxFallback.asStateFlow()

    fun updateBackendUrl(newUrl: String) {
        val sanitized = newUrl.trim()
        _backendUrl.value = sanitized
        NetworkClient.baseUrl = sanitized
    }

    fun setSandboxFallback(enabled: Boolean) {
        _useSandboxFallback.value = enabled
    }

    fun setUseGemmaOnDevice(enabled: Boolean) {
        _useGemmaOnDevice.value = enabled
    }

    private fun extractErrorMessage(e: Exception): String {
        if (e is HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                try {
                    val json = JSONObject(errorBody)
                    if (json.has("error")) {
                        return "[HTTP $code] ${json.getString("error")}"
                    }
                    if (json.has("message")) {
                        return "[HTTP $code] ${json.getString("message")}"
                    }
                } catch (_: Exception) {
                    return "[HTTP $code] $errorBody"
                }
            }
            return "Server returned HTTP $code: ${e.message()}"
        }
        return e.localizedMessage ?: e.message ?: "Unknown communication failure"
    }

    /**
     * Primary Text-based Product Intent input:
     * User can directly type product details (brand, model, specs) to skip image scanning.
     */
    fun analyzeTextWithAIAgent(productQuery: String) {
        val trimmed = productQuery.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = AICheckoutUiState.Error(
                "Please enter product details (e.g. 'Sony WH-1000XM5', 'Anker Prime 65W Charger', or 'Logitech MX Master 3S')."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AICheckoutUiState.ProcessingAI("Gemma-4 On-Device parsing specifications & prices...")

            if (_useGemmaOnDevice.value) {
                delay(300) // On-device inference feedback
                val result = GemmaOnDeviceEngine.analyzeTextIntent(trimmed)
                result.onSuccess { product ->
                    _selectedProduct.value = product
                    _uiState.value = AICheckoutUiState.ReviewMatch(
                        product = product,
                        source = GemmaOnDeviceEngine.MODEL_NAME
                    )
                }.onFailure { error ->
                    _uiState.value = AICheckoutUiState.Error("Gemma-4 Parsing Alert: ${error.message}")
                }
            } else {
                // Cloud endpoint attempt with local fallback
                try {
                    val product = NetworkClient.api.analyzeText(mapOf("query" to trimmed))
                    _selectedProduct.value = product
                    _uiState.value = AICheckoutUiState.ReviewMatch(
                        product = product,
                        source = product.visionModel ?: "Gemma-4 Cloud Service"
                    )
                } catch (e: Exception) {
                    val localResult = GemmaOnDeviceEngine.analyzeTextIntent(trimmed)
                    localResult.onSuccess { product ->
                        _selectedProduct.value = product
                        _uiState.value = AICheckoutUiState.ReviewMatch(
                            product = product,
                            source = "Gemma-4 On-Device Engine (Local Fallback)"
                        )
                    }.onFailure {
                        _uiState.value = AICheckoutUiState.Error("Product Search Failed: ${extractErrorMessage(e)}")
                    }
                }
            }
        }
    }

    /**
     * Analyzes image bytes with Gemma-4 (on-device or via backend)
     */
    fun analyzeImageWithAIAgent(imageBytes: ByteArray) {
        if (imageBytes.isEmpty()) {
            _uiState.value = AICheckoutUiState.Error(
                "No camera feed detected. Please allow camera access, use 'Pick Photo', or switch to 'Type Product Details' tab."
            )
            return
        }

        viewModelScope.launch {
            if (_useGemmaOnDevice.value) {
                _uiState.value = AICheckoutUiState.ProcessingAI("Gemma-4 On-Device Vision analyzing photo...")
                delay(400)
                val result = GemmaOnDeviceEngine.analyzeImageIntentOnDevice(imageBytes)
                result.onSuccess { product ->
                    _selectedProduct.value = product
                    _uiState.value = AICheckoutUiState.ReviewMatch(
                        product = product,
                        source = "Gemma-4 On-Device Vision"
                    )
                }.onFailure {
                    _uiState.value = AICheckoutUiState.Error("Gemma-4 Local Vision Error: ${it.message}")
                }
            } else {
                _uiState.value = AICheckoutUiState.ProcessingAI("Uploading image to Cloud Vision...")
                try {
                    val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val product = NetworkClient.api.analyzeImage(requestBody)
                    _selectedProduct.value = product
                    _uiState.value = AICheckoutUiState.ReviewMatch(
                        product = product,
                        source = product.visionModel ?: "Cloud Vision"
                    )
                } catch (e: Exception) {
                    val localResult = GemmaOnDeviceEngine.analyzeImageIntentOnDevice(imageBytes)
                    localResult.onSuccess { product ->
                        _selectedProduct.value = product
                        _uiState.value = AICheckoutUiState.ReviewMatch(
                            product = product,
                            source = "Gemma-4 On-Device Vision (Auto Fallback)"
                        )
                    }.onFailure {
                        val errorDetails = extractErrorMessage(e)
                        _uiState.value = AICheckoutUiState.Error("Product Scan Failed: $errorDetails")
                    }
                }
            }
        }
    }

    /**
     * Executes real PayPal order capture via Render backend
     */
    fun executePayPalPayment(productId: String,onApprovalRequired: (String)->Unit) {
        val currentProduct = _selectedProduct.value
        if (currentProduct == null) {
            _uiState.value = AICheckoutUiState.Error("No scanned product selected for payment.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AICheckoutUiState.ExecutingPayment
            try {
                val response = NetworkClient.api.executePayment(
                    mapOf(
                        "productId" to productId,
                        "productTitle" to currentProduct.title,
                        "price" to currentProduct.price
                    )
                )

                 if (
                response.status == "CREATED" &&
                !response.orderId.isNullOrBlank() &&
                !response.approvalUrl.isNullOrBlank()
            ) {

                val orderId =
                    response.orderId!!

                val approvalUrl =
                    response.approvalUrl!!

                _uiState.value =
                    AICheckoutUiState.WaitingForPayPalApproval(
                        orderId = orderId,
                        approvalUrl = approvalUrl,
                        product = currentProduct
                    )

                /*
                 * Tell Activity/Compose to open PayPal.
                 */
                onApprovalRequired(approvalUrl)

            } else {

                _uiState.value =
                    AICheckoutUiState.Error(
                        response.error
                            ?: response.message
                            ?: "PayPal order could not be created."
                    )
            }

                // if (response.status == "COMPLETED") {
                //     val record = OrderRecord(
                //         orderId = response.transactionId,
                //         product = currentProduct,
                //         status = "COMPLETED",
                //         paymentMethod = "PayPal Sandbox 1-Click"
                //     )
                //     OrderHistoryRepository.addOrder(record)
                //     _uiState.value = AICheckoutUiState.Success(
                //         transactionId = response.transactionId,
                //         product = currentProduct,
                //         zapierTriggered = true
                //     )
                // } else {
                //     _uiState.value = AICheckoutUiState.Error(
                //         "PayPal Payment Incomplete. Status returned: ${response.status}"
                //     )
                // }
            } catch (e: Exception) {
                val errorDetails = extractErrorMessage(e)
                _uiState.value = AICheckoutUiState.Error(
                    "PayPal API Connection Error: $errorDetails"
                )
            }
        }
    }

/**
 * Step 2:
 *
 * Called after PayPal redirects the user back to the Android app.
 *
 * This is where the backend captures the approved PayPal order.
 */
fun capturePayPalPayment(
    orderId: String
) {

    val currentProduct =
        _selectedProduct.value

    if (currentProduct == null) {

        _uiState.value =
            AICheckoutUiState.Error(
                "Product information was lost before PayPal capture."
            )

        return
    }

    viewModelScope.launch {

        _uiState.value =
            AICheckoutUiState.CapturingPayment

        try {

            val response =
                NetworkClient.api.capturePayPal(
                    mapOf(
                        "orderId" to orderId
                    )
                )

            if (
                response.status == "COMPLETED" &&
                !response.captureId.isNullOrBlank()
            ) {

                val transactionId =
                    response.captureId!!

                /*
                 * IMPORTANT:
                 *
                 * Use the PayPal capture ID as the transaction ID.
                 * Do NOT use the PayPal order ID here.
                 */
                val record =
                    OrderRecord(
                        orderId = transactionId,
                        product = currentProduct,
                        status = "COMPLETED",
                        paymentMethod = "PayPal Sandbox"
                    )

                OrderHistoryRepository.addOrder(
                    record
                )

                _uiState.value =
                    AICheckoutUiState.Success(
                        transactionId = transactionId,
                        product = currentProduct,
                        zapierTriggered =
                            response.zapierTriggered
                    )

            } else {

                _uiState.value =
                    AICheckoutUiState.Error(
                        response.error
                            ?: "PayPal payment was not completed."
                    )
            }

        } catch (e: Exception) {

            val errorDetails =
                extractErrorMessage(e)

            _uiState.value =
                AICheckoutUiState.Error(
                    "PayPal Capture Error: $errorDetails"
                )
        }
    }
}

fun paypalCancelled() {

    _uiState.value =
        AICheckoutUiState.Error(
            "PayPal payment was cancelled."
        )
}

    fun resetState() {
        _uiState.value = AICheckoutUiState.Idle
    }
}
