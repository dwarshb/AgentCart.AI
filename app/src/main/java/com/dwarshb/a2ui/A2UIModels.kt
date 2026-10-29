package com.dwarshb.a2ui

import com.dwarshb.appfunctions.OrderResult
import com.dwarshb.appfunctions.ProductSearchResult

enum class A2UISender {
    USER,
    GEMINI_AGENT,
    APP_FUNCTION_EXECUTION,
    SYSTEM_EVENT
}

enum class A2UIWidgetType {
    TEXT_ONLY,
    PRODUCT_MATCH_CARD,
    OFFERS_COMPARISON,
    ORDER_CONFIRMATION,
    ERROR_NOTICE
}

data class A2UIMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: A2UISender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val widgetType: A2UIWidgetType = A2UIWidgetType.TEXT_ONLY,
    val product: ProductSearchResult? = null,
    val orderResult: OrderResult? = null,
    val isExecutingAppFunction: Boolean = false,
    val appFunctionName: String? = null,
    val appFunctionParams: String? = null
)

data class A2UIConversationState(
    val messages: List<A2UIMessage> = emptyList(),
    val isBusy: Boolean = false,
    val activeQuery: String = "",
    val currentSelectedProduct: ProductSearchResult? = null,
    val activeOrder: OrderResult? = null
)
