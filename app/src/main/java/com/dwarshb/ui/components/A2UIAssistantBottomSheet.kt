package com.dwarshb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dwarshb.a2ui.A2UIEngine
import com.dwarshb.a2ui.A2UIMessage
import com.dwarshb.a2ui.A2UISender
import com.dwarshb.a2ui.A2UIWidgetType
import com.dwarshb.ui.components.a2ui.A2UIOrderConfirmationCard
import com.dwarshb.ui.components.a2ui.A2UIProductCard
import com.dwarshb.ui.components.a2ui.AppFunctionExecutionBadge
import com.dwarshb.ui.theme.PayPalBlue
import com.dwarshb.ui.theme.PayPalGold
import com.dwarshb.ui.theme.PayPalLightBlue
import com.dwarshb.ui.theme.PayPalNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun A2UIAssistantBottomSheet(
    onDismiss: () -> Unit,
    initialQuery: String? = null,
    onOrderPlaced: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val engine = remember { A2UIEngine(coroutineScope) }
    val conversationState by engine.conversation.collectAsState()

    var inputQuery by remember { mutableStateOf(initialQuery ?: "") }
    val listState = rememberLazyListState()

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            inputQuery = ""
            engine.sendUserPrompt(initialQuery)
        }
    }

    LaunchedEffect(conversationState.messages.size) {
        if (conversationState.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversationState.messages.size - 1)
        }
    }

    val sampleQueries = listOf(
        "Sony WH-1000XM5",
        "Anker Prime 65W Charger",
        "AirPods Pro 2",
        "Logitech MX Master 3S"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PayPalNavy,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PayPalGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Gemini A2UI Assistant",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PayPalBlue
                            ) {
                                Text(
                                    text = "AppFunctions",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Channel3 Commerce & 1-Click PayPal Engine",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = { engine.clearConversation() }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_a2ui_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Quick Prompt Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleQueries) { query ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PayPalBlue.copy(alpha = 0.08f),
                        modifier = Modifier.clickable {
                            inputQuery = ""
                            engine.sendUserPrompt("Find $query")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = PayPalNavy, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = query,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PayPalNavy
                            )
                        }
                    }
                }
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(conversationState.messages, key = { it.id }) { msg ->
                    A2UIMessageItem(
                        message = msg,
                        onPlaceOrder = { product ->
                            engine.executeOrderFromA2UI(product) { approvalUrl ->
                                onOrderPlaced?.invoke(approvalUrl)
                            }
                        },
                        isBusy = conversationState.isBusy
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text("Ask Gemini to find any product...", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputQuery.isNotBlank() && !conversationState.isBusy) {
                                val q = inputQuery
                                inputQuery = ""
                                engine.sendUserPrompt(q)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PayPalNavy,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("a2ui_chat_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = if (inputQuery.isNotBlank() && !conversationState.isBusy) PayPalNavy else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(enabled = inputQuery.isNotBlank() && !conversationState.isBusy) {
                            val q = inputQuery
                            inputQuery = ""
                            engine.sendUserPrompt(q)
                        }
                        .testTag("a2ui_send_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputQuery.isNotBlank() && !conversationState.isBusy) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun A2UIMessageItem(
    message: A2UIMessage,
    onPlaceOrder: (com.dwarshb.appfunctions.ProductSearchResult) -> Unit,
    isBusy: Boolean
) {
    when (message.sender) {
        A2UISender.USER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = PayPalNavy,
                    modifier = Modifier.fillMaxWidth(0.82f)
                ) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        A2UISender.APP_FUNCTION_EXECUTION -> {
            AppFunctionExecutionBadge(
                functionName = message.appFunctionName ?: "searchProduct",
                params = message.appFunctionParams
            )
        }

        A2UISender.GEMINI_AGENT, A2UISender.SYSTEM_EVENT -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Agent Text Message
                if (message.text.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PayPalNavy,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PayPalGold, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Render A2UI Rich Card Widget
                when (message.widgetType) {
                    A2UIWidgetType.PRODUCT_MATCH_CARD -> {
                        message.product?.let { prod ->
                            A2UIProductCard(
                                product = prod,
                                onPlaceOrder = onPlaceOrder,
                                isProcessingOrder = isBusy,
                                modifier = Modifier.padding(start = 34.dp)
                            )
                        }
                    }

                    A2UIWidgetType.ORDER_CONFIRMATION -> {
                        if (message.product != null && message.orderResult != null) {
                            A2UIOrderConfirmationCard(
                                product = message.product,
                                orderResult = message.orderResult,
                                modifier = Modifier.padding(start = 34.dp)
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
