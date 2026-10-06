package com.dwarshb.data

import com.dwarshb.network.DiscoveredProduct
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
    // Starts completely empty - only real completed transactions are stored
    private val _orders = MutableStateFlow<List<OrderRecord>>(emptyList())
    val orders: StateFlow<List<OrderRecord>> = _orders.asStateFlow()

    fun addOrder(record: OrderRecord) {
        _orders.value = listOf(record) + _orders.value
    }

    fun clearHistory() {
        _orders.value = emptyList()
    }
}
