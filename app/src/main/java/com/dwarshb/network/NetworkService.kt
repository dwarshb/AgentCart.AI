package com.dwarshb.network

import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class DiscoveredProduct(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: String,
    @SerializedName("confidenceScore") val confidenceScore: String,
    @SerializedName("merchantName") val merchantName: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("category") val category: String? = "Consumer Electronics",
    @SerializedName("currency") val currency: String? = "USD",
    @SerializedName("visionModel") val visionModel: String? = null,
    @SerializedName("geminiRawOutput") val geminiRawOutput: String? = null,
    @SerializedName("geminiStatus") val geminiStatus: String? = null
)

data class PaymentResponse(
    @SerializedName("status") val status: String,
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("approvalUrl") val approvalUrl: String? = null,
    @SerializedName("captureTimestamp") val captureTimestamp: String? = null
)

interface AgentCartApi {
    @POST("/api/process-agent-intent")
    suspend fun analyzeImage(@Body imageBytes: RequestBody): DiscoveredProduct

    @POST("/api/process-text-intent")
    suspend fun analyzeText(@Body payload: Map<String, String>): DiscoveredProduct

    // @POST("/api/execute-paypal")
    // suspend fun executePayment(@Body payload: Map<String, String>): PaymentResponse
    
    @POST("api/execute-paypal")
    suspend fun executePayment(@Body body: Map<String, String>): PayPalCreateOrderResponse
    
    @POST("api/capture-paypal")
    suspend fun capturePayPal(@Body body: Map<String, String>): PayPalCaptureResponse

}

object NetworkClient {
    // Live Render backend URL configured
    var baseUrl: String = "https://agentcart-ai.onrender.com"
        set(value) {
            val sanitized = if (value.endsWith("/")) value else "$value/"
            field = sanitized
            _cachedApi = null
        }

    private var _cachedApi: AgentCartApi? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: AgentCartApi
        get() {
            if (_cachedApi == null) {
                val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                _cachedApi = Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AgentCartApi::class.java)
            }
            return _cachedApi!!
        }
}
