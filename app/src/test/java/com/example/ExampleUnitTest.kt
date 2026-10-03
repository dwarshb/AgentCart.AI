package com.example

import com.example.data.OrderHistoryRepository
import com.example.data.OrderRecord
import com.example.data.SampleCatalog
import com.example.network.DiscoveredProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSampleCatalogNotEmpty() {
        assertTrue(SampleCatalog.items.isNotEmpty())
        val firstItem = SampleCatalog.items.first()
        assertEquals("PROD-99018", firstItem.id)
        assertNotNull(firstItem.price)
    }

    @Test
    fun testOrderRecordCreation() {
        val product = SampleCatalog.items.first()
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
        val initialCount = OrderHistoryRepository.orders.value.size
        val product = SampleCatalog.items[1]
        val order = OrderRecord(
            orderId = "TEST-ORDER-2",
            product = product
        )
        OrderHistoryRepository.addOrder(order)
        assertEquals(initialCount + 1, OrderHistoryRepository.orders.value.size)
        assertEquals("TEST-ORDER-2", OrderHistoryRepository.orders.value.first().orderId)
    }
}
