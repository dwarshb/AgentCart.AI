package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OrderHistoryRepository
import com.example.data.OrderRecord
import com.example.network.DiscoveredProduct
import com.example.network.NetworkClient
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
    data class ProcessingAI(val statusMessage: String = "Uploading to Gemini Flash Vision...") : AICheckoutUiState()
    data class ReviewMatch(
        val product: DiscoveredProduct,
        val source: String = "Gemini Flash Multimodal Vision"
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
     * Sends captured image bytes to Gemini Flash Vision on Render backend
     */
    fun analyzeImageWithAIAgent(imageBytes: ByteArray) {
        if (imageBytes.isEmpty()) {
            _uiState.value = AICheckoutUiState.Error(
                "No camera feed detected. Please allow camera access or use 'Pick Photo' to select an image from your device."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AICheckoutUiState.ProcessingAI("Uploading image to Google Gemini Flash Vision...")
            try {
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val product = NetworkClient.api.analyzeImage(requestBody)
                _selectedProduct.value = product
                _uiState.value = AICheckoutUiState.ReviewMatch(
                    product = product,
                    source = product.visionModel ?: "Gemini Flash Vision"
                )
            } catch (e: Exception) {
                val errorDetails = extractErrorMessage(e)
                _uiState.value = AICheckoutUiState.Error(
                    "Product Scan Failed: $errorDetails"
                )
            }
        }
    }

    /**
     * Executes real PayPal order capture via Render backend
     */
    fun executePayPalPayment(productId: String) {
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

                if (response.status == "COMPLETED") {
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
                } else {
                    _uiState.value = AICheckoutUiState.Error(
                        "PayPal Payment Incomplete. Status returned: ${response.status}"
                    )
                }
            } catch (e: Exception) {
                val errorDetails = extractErrorMessage(e)
                _uiState.value = AICheckoutUiState.Error(
                    "PayPal API Connection Error: $errorDetails"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AICheckoutUiState.Idle
    }
}
