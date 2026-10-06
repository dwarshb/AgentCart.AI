package com.dwarshb

import com.dwarshb.data.OrderHistoryRepository
import com.dwarshb.data.OrderRecord
import com.dwarshb.network.DiscoveredProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun createTestProduct(id: String = "PROD-TEST-1", title: String = "Test Product"): DiscoveredProduct {
        return DiscoveredProduct(
            id = id,
            title = title,
            price = "$49.99",
            confidenceScore = "Verified",
            merchantName = "Direct Checkout",
            category = "Electronics"
        )
    }

    @Test
    fun testOrderRecordCreation() {
        val product = createTestProduct("PROD-99018", "Anker 65W GaN Charger")
        val order = OrderRecord(
            orderId = "SANDBOX-ORD-TEST-1",
            product = product,
            status = "COMPLETED"
        )
        assertEquals("SANDBOX-ORD-TEST-1", order.orderId)
        assertEquals("COMPLETED", order.status)
        assertTrue(order.formattedDate.isNotEmpty())
    }

    @Test
    fun testOrderHistoryRepositoryAdd() {
        OrderHistoryRepository.clearHistory()
        assertEquals(0, OrderHistoryRepository.orders.value.size)

        val product = createTestProduct("PROD-TEST-2", "Sony WH-1000XM5")
        val order = OrderRecord(
            orderId = "TEST-ORDER-2",
            product = product
        )
        OrderHistoryRepository.addOrder(order)
        assertEquals(1, OrderHistoryRepository.orders.value.size)
        assertEquals("TEST-ORDER-2", OrderHistoryRepository.orders.value.first().orderId)
    }
}
