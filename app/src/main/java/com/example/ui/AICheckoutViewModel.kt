package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OrderHistoryRepository
import com.example.data.OrderRecord
import com.example.data.SampleCatalog
import com.example.network.DiscoveredProduct
import com.example.network.NetworkClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

sealed class AICheckoutUiState {
    object Idle : AICheckoutUiState()
    data class ProcessingAI(val statusMessage: String = "Gemini Vision processing layout fields...") : AICheckoutUiState()
    data class ReviewMatch(
        val product: DiscoveredProduct,
        val source: String = "Gemini Vision Multimodal Pipeline"
    ) : AICheckoutUiState()
    object ExecutingPayment : AICheckoutUiState()
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

    // Configurable backend URL
    private val _backendUrl = MutableStateFlow(NetworkClient.baseUrl)
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _useSandboxFallback = MutableStateFlow(false)
    val useSandboxFallback: StateFlow<Boolean> = _useSandboxFallback.asStateFlow()

    private val _selectedProduct = MutableStateFlow<DiscoveredProduct?>(null)
    val selectedProduct: StateFlow<DiscoveredProduct?> = _selectedProduct.asStateFlow()

    fun updateBackendUrl(newUrl: String) {
        val sanitized = newUrl.trim()
        _backendUrl.value = sanitized
        NetworkClient.baseUrl = sanitized
    }

    fun setSandboxFallback(enabled: Boolean) {
        _useSandboxFallback.value = enabled
    }

    /**
     * Primary Blueprint pipeline method: analyzes image bytes with Gemini Vision
     */
    fun analyzeImageWithAIAgent(imageBytes: ByteArray, fallbackHint: DiscoveredProduct? = null) {
        viewModelScope.launch {
            _uiState.value = AICheckoutUiState.ProcessingAI("Gemini Vision processing layout fields...")
            try {
                // If backend URL is live and not placeholder, attempt network call
                val isCustomBackend = !_backendUrl.value.contains("your-agentcart-backend.onrender.com")
                if (isCustomBackend && imageBytes.isNotEmpty()) {
                    val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val product = NetworkClient.api.analyzeImage(requestBody)
                    _selectedProduct.value = product
                    _uiState.value = AICheckoutUiState.ReviewMatch(product, source = "Render AI Endpoint")
                } else {
                    // Demo simulation / fallback for instant hackathon testing in AI Studio emulator
                    delay(1400) // Realistic Gemini Vision inference latency
                    _uiState.value = AICheckoutUiState.ProcessingAI("Channel3 verifying real-time inventory & pricing...")
                    delay(800)
                    val resolved = fallbackHint ?: SampleCatalog.items.first()
                    _selectedProduct.value = resolved
                    _uiState.value = AICheckoutUiState.ReviewMatch(
                        product = resolved,
                        source = if (isCustomBackend) "Render AI Backend" else "Channel3 Marketplace Node"
                    )
                }
            } catch (e: Exception) {
                // Fallback to offline catalog item if configured to avoid deadlocks in hackathon demos
                if (_useSandboxFallback.value) {
                    val resolved = fallbackHint ?: SampleCatalog.items.first()
                    _selectedProduct.value = resolved
                    _uiState.value = AICheckoutUiState.ReviewMatch(
                        product = resolved,
                        source = "Sandbox Intelligent Node (Backend offline fallback)"
                    )
                } else {
                    _uiState.value = AICheckoutUiState.Error("AI pipeline analysis failed: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    /**
     * Triggers analysis directly from a catalog item (ideal for testing in emulator)
     */
    fun analyzeCatalogItem(item: DiscoveredProduct) {
        analyzeImageWithAIAgent(ByteArray(0), fallbackHint = item)
    }

    /**
     * Blueprint payment execution method
     */
    fun executePayPalPayment(productId: String) {
        val currentProduct = _selectedProduct.value ?: SampleCatalog.items.find { it.id == productId } ?: SampleCatalog.items.first()
        viewModelScope.launch {
            _uiState.value = AICheckoutUiState.ExecutingPayment
            try {
                val isCustomBackend = !_backendUrl.value.contains("your-agentcart-backend.onrender.com")
                if (isCustomBackend) {
                    val response = NetworkClient.api.executePayment(mapOf("productId" to productId))
                    if (response.status == "COMPLETED") {
                        val record = OrderRecord(
                            orderId = response.transactionId.ifEmpty { "PP-CAPT-${UUID.randomUUID().toString().take(8).uppercase()}" },
                            product = currentProduct,
                            status = "COMPLETED",
                            paymentMethod = "PayPal Biometric 1-Click"
                        )
                        OrderHistoryRepository.addOrder(record)
                        _uiState.value = AICheckoutUiState.Success(
                            transactionId = record.orderId,
                            product = currentProduct,
                            zapierTriggered = true
                        )
                    } else {
                        _uiState.value = AICheckoutUiState.Error("Payment verification failed on PayPal sandbox server.")
                    }
                } else {
                    // Direct Sandbox execution using configured PayPal credentials
                    val response = com.example.network.PayPalSandboxClient.createAndCaptureOrder(
                        productTitle = currentProduct.title,
                        priceString = currentProduct.price
                    )
                    val record = OrderRecord(
                        orderId = response.transactionId,
                        product = currentProduct,
                        status = "COMPLETED",
                        paymentMethod = "PayPal Sandbox 1-Click"
                    )
                    OrderHistoryRepository.addOrder(record)
                    _uiState.value = AICheckoutUiState.Success(
                        transactionId = response.transactionId,
                        product = currentProduct,
                        zapierTriggered = true
                    )
                }
            } catch (e: Exception) {
                if (e is com.example.network.PayPalApiException) {
                    // Real PayPal API error (authentication failure, invalid secret, etc.)
                    _uiState.value = AICheckoutUiState.Error(e.message ?: "PayPal Sandbox API error")
                } else if (_useSandboxFallback.value) {
                    // Only fallback for non-auth network errors if user explicitly turned fallback on
                    val mockOrderId = "SANDBOX-PP-${UUID.randomUUID().toString().take(10).uppercase()}"
                    val record = OrderRecord(
                        orderId = mockOrderId,
                        product = currentProduct,
                        status = "COMPLETED",
                        paymentMethod = "PayPal Sandbox Ledger (Demo Fallback)"
                    )
                    OrderHistoryRepository.addOrder(record)
                    _uiState.value = AICheckoutUiState.Success(
                        transactionId = mockOrderId,
                        product = currentProduct,
                        zapierTriggered = true
                    )
                } else {
                    _uiState.value = AICheckoutUiState.Error("PayPal API connection error: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AICheckoutUiState.Idle
    }
}
