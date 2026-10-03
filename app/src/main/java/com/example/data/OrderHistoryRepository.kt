package com.example.data

import com.example.network.DiscoveredProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OrderRecord(
    val orderId: String,
    val product: DiscoveredProduct,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED",
    val paymentMethod: String = "PayPal Instant Auth",
    val zapierDispatched: Boolean = true
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

object OrderHistoryRepository {
    private val _orders = MutableStateFlow<List<OrderRecord>>(
        listOf(
            OrderRecord(
                orderId = "SANDBOX-PP-78391240",
                product = DiscoveredProduct(
                    id = "PROD-99018",
                    title = "Anker Prime 65W GaN Charger",
                    price = "$39.99",
                    confidenceScore = "98% AI Certainty Index",
                    merchantName = "Channel3 Integrated Marketplace Node",
                    description = "Ultra-compact 3-port fast wall charger with PowerIQ 4.0 tech.",
                    category = "Electronics"
                ),
                timestamp = System.currentTimeMillis() - 86400000L * 2,
                status = "COMPLETED",
                paymentMethod = "PayPal Biometric 1-Click"
            )
        )
    )
    val orders: StateFlow<List<OrderRecord>> = _orders.asStateFlow()

    fun addOrder(record: OrderRecord) {
        _orders.value = listOf(record) + _orders.value
    }

    fun clearHistory() {
        _orders.value = emptyList()
    }
}

object SampleCatalog {
    val items = listOf(
        DiscoveredProduct(
            id = "PROD-99018",
            title = "Anker Prime 65W GaN Charger",
            price = "$39.99",
            confidenceScore = "98% AI Certainty Index",
            merchantName = "Channel3 Integrated Marketplace Node",
            description = "Ultra-compact 3-port fast wall charger with PowerIQ 4.0 and ActiveShield 2.0 temperature monitoring.",
            category = "Charging & Power"
        ),
        DiscoveredProduct(
            id = "PROD-88210",
            title = "Sony WH-1000XM5 Wireless ANC",
            price = "$348.00",
            confidenceScore = "99% AI Certainty Index",
            merchantName = "BestBuy via Channel3 Node",
            description = "Industry-leading noise canceling with 8 microphones, Auto NC Optimizer, and 30-hour battery life.",
            category = "Audio & Headphones"
        ),
        DiscoveredProduct(
            id = "PROD-77142",
            title = "Logitech MX Master 3S Wireless",
            price = "$99.99",
            confidenceScore = "96% AI Certainty Index",
            merchantName = "Channel3 Merchant Direct",
            description = "Ergonomic precision mouse with 8K DPI Darkfield tracking and MagSpeed electromagnetic scrolling.",
            category = "Computer Accessories"
        ),
        DiscoveredProduct(
            id = "PROD-66321",
            title = "Apple MagSafe FineWoven Wallet",
            price = "$59.00",
            confidenceScore = "94% AI Certainty Index",
            merchantName = "Apple Store Authorized Node",
            description = "Microtwill material crafted with strong built-in magnets that snap onto your iPhone with Find My support.",
            category = "Accessories"
        ),
        DiscoveredProduct(
            id = "PROD-55419",
            title = "DJI Osmo Pocket 3 Creator Combo",
            price = "$669.00",
            confidenceScore = "97% AI Certainty Index",
            merchantName = "B&H Photo via Channel3 Node",
            description = "1-inch CMOS sensor pocket gimbal camera with 4K/120fps recording and 2-inch rotatable OLED touchscreen.",
            category = "Cameras & Video"
        ),
        DiscoveredProduct(
            id = "PROD-44120",
            title = "Yeti Rambler 20 oz Insulated Tumbler",
            price = "$38.00",
            confidenceScore = "95% AI Certainty Index",
            merchantName = "Yeti Direct Commerce Node",
            description = "Double-wall vacuum-insulated stainless steel travel tumbler with MagSlider splash-resistant lid.",
            category = "Drinkware"
        )
    )
}
