package com.dwarshb.network

data class PayPalCreateOrderResponse(
    val status: String,
    val orderId: String?,
    val transactionId: String?,
    val approvalUrl: String?,
    val productId: String?,
    val productTitle: String?,
    val amount: String?,
    val currency: String?,
    val message: String?,
    val error: String?
)

data class PayPalCaptureResponse(
    val status: String,
    val orderId: String?,
    val transactionId: String?,
    val captureId: String?,
    val paypalOrderStatus: String?,
    val captureStatus: String?,
    val amount: String?,
    val currency: String?,
    val zapierTriggered: Boolean = false,
    val error: String?
)