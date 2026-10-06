package com.dwarshb.network

import android.util.Base64
import com.dwarshb.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class PayPalTokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("expires_in") val expiresIn: Int?
)

data class PayPalOrderResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("status") val status: String?
)

data class ConnectionTestResult(
    val success: Boolean,
    val httpCode: Int,
    val message: String,
    val details: String? = null
)

class PayPalApiException(message: String) : Exception(message)

object PayPalSandboxClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Dynamic runtime overrides (editable from Settings UI)
    var customClientId: String? = null
    var customClientSecret: String? = null
    var customBaseUrl: String? = null

    val effectiveBaseUrl: String
        get() {
            val custom = customBaseUrl?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                val url = BuildConfig.PAYPAL_BASE_URL
                if (!url.isNullOrBlank()) url else "https://api-m.sandbox.paypal.com"
            } catch (_: Exception) {
                "https://api-m.sandbox.paypal.com"
            }
        }

    val effectiveClientId: String
        get() {
            val custom = customClientId?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                BuildConfig.PAYPAL_CLIENT_ID ?: ""
            } catch (_: Exception) {
                ""
            }
        }

    val effectiveClientSecret: String
        get() {
            val custom = customClientSecret?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                BuildConfig.PAYPAL_SECRET ?: ""
            } catch (_: Exception) {
                ""
            }
        }

    /**
     * Diagnostic check: Tests live OAuth2 connectivity against PayPal Sandbox
     */
    suspend fun testConnection(
        overrideId: String? = null,
        overrideSecret: String? = null
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val cId = (overrideId ?: effectiveClientId).trim()
        val cSecret = (overrideSecret ?: effectiveClientSecret).trim()

        if (cId.isBlank()) {
            return@withContext ConnectionTestResult(
                success = false,
                httpCode = 0,
                message = "Client ID is empty. Please enter your PayPal Sandbox Client ID."
            )
        }
        if (cSecret.isBlank()) {
            return@withContext ConnectionTestResult(
                success = false,
                httpCode = 0,
                message = "Client Secret is empty. Please enter your PayPal Sandbox Secret."
            )
        }
        if (cId == cSecret) {
            return@withContext ConnectionTestResult(
                success = false,
                httpCode = 401,
                message = "Client ID and Secret are identical! The Secret is distinct from the Client ID on developer.paypal.com.",
                details = "In PayPal Developer Dashboard under 'Apps & Credentials', click 'Show' next to Secret to reveal the separate secret string."
            )
        }

        try {
            val credentials = "$cId:$cSecret"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

            val tokenRequestBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build()

            val tokenRequest = Request.Builder()
                .url("$effectiveBaseUrl/v1/oauth2/token")
                .header("Authorization", basicAuth)
                .header("Accept", "application/json")
                .post(tokenRequestBody)
                .build()

            val response = client.newCall(tokenRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val tokenData = gson.fromJson(responseBody, PayPalTokenResponse::class.java)
                ConnectionTestResult(
                    success = true,
                    httpCode = response.code,
                    message = "Successfully authenticated with PayPal Sandbox!",
                    details = "Token received. Valid for ${tokenData.expiresIn ?: 32400} seconds. Live order creation is ready."
                )
            } else {
                ConnectionTestResult(
                    success = false,
                    httpCode = response.code,
                    message = "PayPal rejected credentials (HTTP ${response.code})",
                    details = responseBody
                )
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                success = false,
                httpCode = -1,
                message = "Network error connecting to PayPal: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Executes real PayPal Sandbox OAuth2 token generation and Order Creation
     */
    suspend fun createAndCaptureOrder(
        productTitle: String,
        priceString: String
    ): PaymentResponse = withContext(Dispatchers.IO) {
        val cId = effectiveClientId.trim()
        val cSecret = effectiveClientSecret.trim()

        if (cId.isBlank() || cSecret.isBlank()) {
            throw PayPalApiException("PayPal Client ID or Secret is not configured. Open Settings (⚙️) to enter your credentials.")
        }

        if (cId == cSecret) {
            throw PayPalApiException("Your PayPal Client ID and Secret are identical ($cId). The Secret is a separate key found by clicking 'Show' next to Secret on developer.paypal.com.")
        }

        // Step 1: OAuth2 Token Exchange
        val credentials = "$cId:$cSecret"
        val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val tokenRequestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .build()

        val tokenRequest = Request.Builder()
            .url("$effectiveBaseUrl/v1/oauth2/token")
            .header("Authorization", basicAuth)
            .header("Accept", "application/json")
            .post(tokenRequestBody)
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        val tokenResponseBody = tokenResponse.body?.string() ?: ""

        if (!tokenResponse.isSuccessful) {
            throw PayPalApiException("PayPal Sandbox Authentication Failed (HTTP ${tokenResponse.code}): $tokenResponseBody")
        }

        val tokenData = gson.fromJson(tokenResponseBody, PayPalTokenResponse::class.java)
        val accessToken = tokenData.accessToken ?: throw PayPalApiException("PayPal returned no access_token in response: $tokenResponseBody")

        // Step 2: Create Order with CAPTURE Intent on PayPal Sandbox
        val cleanPrice = priceString.replace("$", "").trim()
        if (cleanPrice.isBlank()) {
            throw PayPalApiException("Valid product price is required for PayPal checkout.")
        }
        val numericPrice = cleanPrice
        val orderJson = """
            {
                "intent": "CAPTURE",
                "purchase_units": [
                    {
                        "reference_id": "AGENTCART-${System.currentTimeMillis()}",
                        "description": "${productTitle.take(50)}",
                        "amount": {
                            "currency_code": "USD",
                            "value": "$numericPrice"
                        }
                    }
                ]
            }
        """.trimIndent()

        val orderRequest = Request.Builder()
            .url("$effectiveBaseUrl/v2/checkout/orders")
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .post(orderJson.toRequestBody("application/json".toMediaType()))
            .build()

        val orderResponse = client.newCall(orderRequest).execute()
        val orderResponseBody = orderResponse.body?.string() ?: ""

        if (!orderResponse.isSuccessful) {
            throw PayPalApiException("PayPal Sandbox Order Creation Failed (HTTP ${orderResponse.code}): $orderResponseBody")
        }

        val orderData = gson.fromJson(orderResponseBody, PayPalOrderResponse::class.java)
        val orderId = orderData.id ?: throw PayPalApiException("PayPal order response did not contain an ID: $orderResponseBody")

        PaymentResponse(
            status = orderData.status ?: "CREATED",
            transactionId = orderId,
            captureTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        )
    }
}
