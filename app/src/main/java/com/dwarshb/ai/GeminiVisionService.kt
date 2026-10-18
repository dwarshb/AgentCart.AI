package com.dwarshb.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.dwarshb.BuildConfig
import com.dwarshb.network.DiscoveredProduct
import com.dwarshb.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class GeminiAnalysisResult(
    val title: String,
    val brand: String? = null,
    val category: String = "Consumer Products",
    val estimatedPrice: String = "$29.99",
    val description: String = "",
    val confidenceScore: String = "95%",
    val rawModelResponse: String? = null,
    val isSuccess: Boolean = true,
    val error: String? = null
)

object GeminiVisionService {
    private const val TAG = "GeminiVisionService"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"

    // Optional runtime API key entered in settings
    var customApiKey: String? = null

    val effectiveApiKey: String
        get() {
            val custom = customApiKey?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                BuildConfig.GEMINI_API_KEY?.trim() ?: ""
            } catch (_: Exception) {
                ""
            }
        }

    val isDirectKeyAvailable: Boolean
        get() = effectiveApiKey.isNotBlank() && effectiveApiKey != "MY_GEMINI_API_KEY"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Converts a Bitmap to compressed JPEG Base64 string for Gemini REST API
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val maxDimension = 1024
        val scale = minOf(
            1f,
            maxDimension.toFloat() / bitmap.width.coerceAtLeast(1),
            maxDimension.toFloat() / bitmap.height.coerceAtLeast(1)
        )
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        if (resized !== bitmap) {
            resized.recycle()
        }
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Analyzes image bytes using Gemini 3.5 Flash Vision.
     * If direct API key is set, executes direct REST request to Google Gemini API.
     * Otherwise, falls back to the server-side Gemini endpoint configured on Render backend.
     */
    suspend fun analyzeImageWithGemini(imageBytes: ByteArray): GeminiAnalysisResult = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext GeminiAnalysisResult(
                title = "",
                isSuccess = false,
                error = "Empty image data provided."
            )
        }

        val bitmap = try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }

        if (bitmap == null) {
            return@withContext GeminiAnalysisResult(
                title = "",
                isSuccess = false,
                error = "Unable to decode image bytes into Android Bitmap."
            )
        }

        // 1. Try Direct Gemini 3.5 Flash REST API if key is available
        if (isDirectKeyAvailable) {
            try {
                Log.d(TAG, "Calling Gemini 3.5 Flash direct REST API...")
                val result = callGeminiDirectRest(bitmap, effectiveApiKey)
                if (result.isSuccess) {
                    return@withContext result
                } else {
                    Log.w(TAG, "Direct Gemini call failed: ${result.error}. Trying backend fallback...")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Direct Gemini call exception", t)
            }
        }

        // 2. Fallback to Server-Side Gemini backend on Render
        try {
            Log.d(TAG, "Calling Server-Side Gemini backend proxy...")
            val reqBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
            val product: DiscoveredProduct = NetworkClient.api.analyzeImage(reqBody)
            return@withContext GeminiAnalysisResult(
                title = product.title,
                brand = product.brand,
                category = product.category ?: "Consumer Goods",
                estimatedPrice = product.price,
                description = product.description ?: "Identified via Gemini Cloud Agent",
                confidenceScore = product.confidenceScore,
                rawModelResponse = product.geminiRawOutput,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Backend Gemini call failed", e)
            val msg = e.localizedMessage ?: e.message ?: "Network error"
            return@withContext GeminiAnalysisResult(
                title = "",
                isSuccess = false,
                error = "Gemini Vision processing error: $msg. Please verify network or enter your Gemini API Key in Settings."
            )
        }
    }

    /**
     * Executes direct REST API call to Gemini 3.5 Flash
     */
    private fun callGeminiDirectRest(bitmap: Bitmap, apiKey: String): GeminiAnalysisResult {
        val base64Data = bitmapToBase64(bitmap)

        val promptText = """
            You are AgentCart AI visual recognition agent.
            Analyze the main retail product or physical merchandise shown in this image.
            Return ONLY a JSON object with this exact structure:
            {
              "title": "Specific brand and product model name",
              "brand": "Manufacturer/Brand",
              "category": "Consumer Electronics or relevant category",
              "estimatedPrice": "$49.99",
              "confidenceScore": "96%",
              "description": "Short 1-2 sentence description highlighting key features"
            }
            Do not include markdown blocks (```json) or introductory text. Return JSON only.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            val partsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", promptText)
                })
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Data)
                    })
                })
            }
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            }
            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val url = "$BASE_URL/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return GeminiAnalysisResult(
                title = "",
                isSuccess = false,
                error = "Gemini API error (HTTP ${response.code}): $responseBody"
            )
        }

        return parseGeminiResponse(responseBody)
    }

    private fun parseGeminiResponse(rawJson: String): GeminiAnalysisResult {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val part = parts?.optJSONObject(0)
            val text = part?.optString("text")?.trim() ?: ""

            // Clean markdown code fence if model wrapped it
            val cleaned = text
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = JSONObject(cleaned)
            val title = parsed.optString("title", "Scanned Product").trim()
            val brand = parsed.optString("brand", "Verified Brand").trim()
            val category = parsed.optString("category", "Retail Commerce").trim()
            val price = parsed.optString("estimatedPrice", "$39.99").trim()
            val confidence = parsed.optString("confidenceScore", "95%").trim()
            val desc = parsed.optString("description", "Identified by Gemini 3.5 Flash Vision").trim()

            GeminiAnalysisResult(
                title = title,
                brand = brand,
                category = category,
                estimatedPrice = price,
                confidenceScore = confidence,
                description = desc,
                rawModelResponse = text,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: $rawJson", e)
            GeminiAnalysisResult(
                title = "",
                isSuccess = false,
                error = "Could not parse Gemini output: ${e.message}"
            )
        }
    }
}
