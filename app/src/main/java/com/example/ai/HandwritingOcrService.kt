package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
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

/**
 * Service for converting handwritten notes / drawings on canvas into digital text
 * using Gemini Vision API.
 */
class HandwritingOcrService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun recognizeHandwriting(context: Context, bitmap: Bitmap): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = GeminiAssistantService.getApiKey(context)
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Gemini API key is not configured"))
                }

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
                val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "Будь ласка, розпізнай весь рукописний текст з цього зображення. Поверни ТІЛЬКИ розпізнаний текст без додаткових коментарів.")
                                })
                                put(JSONObject().apply {
                                    put("inline_data", JSONObject().apply {
                                        put("mime_type", "image/png")
                                        put("data", base64Image)
                                    })
                                })
                            })
                        })
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(RuntimeException("OCR failed: ${response.code}"))
                }

                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "").trim()
                        return@withContext Result.success(text)
                    }
                }

                Result.failure(RuntimeException("No text detected"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
