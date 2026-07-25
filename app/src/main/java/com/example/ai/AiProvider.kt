package com.example.ai

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface AiProvider {
    val id: String
    val displayName: String
    val supportsVision: Boolean
    suspend fun query(
        text: String,
        imageBase64: String?,
        apiKey: String,
        endpoint: String? = null,
        model: String? = null
    ): String
}

class GeminiProvider : AiProvider {
    override val id: String = "GEMINI"
    override val displayName: String = "Google Gemini"
    override val supportsVision: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun query(
        text: String,
        imageBase64: String?,
        apiKey: String,
        endpoint: String?,
        model: String?
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val modelName = model?.ifBlank { null } ?: "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            val contentsArr = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArr = JSONArray()

                val textPart = JSONObject().apply {
                    put("text", text)
                }
                partsArr.put(textPart)

                if (!imageBase64.isNullOrBlank()) {
                    val imagePart = JSONObject().apply {
                        val inlineData = JSONObject().apply {
                            put("mime_type", "image/png")
                            put("data", imageBase64)
                        }
                        put("inline_data", inlineData)
                    }
                    partsArr.put(imagePart)
                }

                put("parts", partsArr)
            }
            contentsArr.put(contentObj)
            put("contents", contentsArr)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Помилка API ($id ${response.code}): $bodyString"
            }
            val resObj = JSONObject(bodyString)
            val candidates = resObj.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "Без відповіді.")
                }
            }
            "Не вдалося розпарсити відповідь Gemini."
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Error querying Gemini", e)
            "Помилка зв'язку з Gemini: ${e.localizedMessage}"
        }
    }
}

class OpenAiProvider : AiProvider {
    override val id: String = "OPENAI"
    override val displayName: String = "OpenAI GPT-4o"
    override val supportsVision: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun query(
        text: String,
        imageBase64: String?,
        apiKey: String,
        endpoint: String?,
        model: String?
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val targetUrl = endpoint?.ifBlank { null } ?: "https://api.openai.com/v1/chat/completions"
        val targetModel = model?.ifBlank { null } ?: "gpt-4o-mini"

        val jsonBody = JSONObject().apply {
            put("model", targetModel)
            val messagesArr = JSONArray()
            val msgObj = JSONObject().apply {
                put("role", "user")
                if (!imageBase64.isNullOrBlank()) {
                    val contentArr = JSONArray()
                    contentArr.put(JSONObject().apply {
                        put("type", "text")
                        put("text", text)
                    })
                    contentArr.put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/png;base64,$imageBase64")
                        })
                    })
                    put("content", contentArr)
                } else {
                    put("content", text)
                }
            }
            messagesArr.put(msgObj)
            put("messages", messagesArr)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(targetUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Помилка OpenAI (${response.code}): $bodyString"
            }
            val resObj = JSONObject(bodyString)
            val choices = resObj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                return@withContext message?.optString("content", "Порожня відповідь") ?: "Порожня відповідь"
            }
            "Не вдалося розпарсити відповідь OpenAI."
        } catch (e: Exception) {
            Log.e("OpenAiProvider", "Error querying OpenAI", e)
            "Помилка зв'язку з OpenAI: ${e.localizedMessage}"
        }
    }
}

class AnthropicProvider : AiProvider {
    override val id: String = "ANTHROPIC"
    override val displayName: String = "Anthropic Claude"
    override val supportsVision: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun query(
        text: String,
        imageBase64: String?,
        apiKey: String,
        endpoint: String?,
        model: String?
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val targetUrl = endpoint?.ifBlank { null } ?: "https://api.anthropic.com/v1/messages"
        val targetModel = model?.ifBlank { null } ?: "claude-3-5-haiku-20241022"

        val jsonBody = JSONObject().apply {
            put("model", targetModel)
            put("max_tokens", 1024)
            val messagesArr = JSONArray()
            val msgObj = JSONObject().apply {
                put("role", "user")
                val contentArr = JSONArray()
                if (!imageBase64.isNullOrBlank()) {
                    contentArr.put(JSONObject().apply {
                        put("type", "image")
                        put("source", JSONObject().apply {
                            put("type", "base64")
                            put("media_type", "image/png")
                            put("data", imageBase64)
                        })
                    })
                }
                contentArr.put(JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                })
                put("content", contentArr)
            }
            messagesArr.put(msgObj)
            put("messages", messagesArr)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(targetUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Помилка Anthropic (${response.code}): $bodyString"
            }
            val resObj = JSONObject(bodyString)
            val contentArr = resObj.optJSONArray("content")
            if (contentArr != null && contentArr.length() > 0) {
                val firstContent = contentArr.getJSONObject(0)
                return@withContext firstContent.optString("text", "")
            }
            "Не вдалося розпарсити відповідь Anthropic."
        } catch (e: Exception) {
            Log.e("AnthropicProvider", "Error querying Anthropic", e)
            "Помилка зв'язку з Anthropic: ${e.localizedMessage}"
        }
    }
}

class DeepSeekProvider : AiProvider {
    override val id: String = "DEEPSEEK"
    override val displayName: String = "DeepSeek AI"
    override val supportsVision: Boolean = false

    private val openAiProvider = OpenAiProvider()

    override suspend fun query(
        text: String,
        imageBase64: String?,
        apiKey: String,
        endpoint: String?,
        model: String?
    ): String {
        val targetEndpoint = endpoint?.ifBlank { null } ?: "https://api.deepseek.com/chat/completions"
        val targetModel = model?.ifBlank { null } ?: "deepseek-chat"
        return openAiProvider.query(text, null, apiKey, targetEndpoint, targetModel)
    }
}

class CustomOpenAiCompatibleProvider : AiProvider {
    override val id: String = "CUSTOM"
    override val displayName: String = "Custom OpenAI-compatible"
    override val supportsVision: Boolean = true

    private val openAiProvider = OpenAiProvider()

    override suspend fun query(
        text: String,
        imageBase64: String?,
        apiKey: String,
        endpoint: String?,
        model: String?
    ): String {
        return openAiProvider.query(text, imageBase64, apiKey, endpoint, model)
    }
}

object AiProviderRegistry {
    val providers: List<AiProvider> = listOf(
        GeminiProvider(),
        OpenAiProvider(),
        AnthropicProvider(),
        DeepSeekProvider(),
        CustomOpenAiCompatibleProvider()
    )

    fun getProvider(id: String): AiProvider {
        return providers.find { it.id.equals(id, ignoreCase = true) } ?: providers.first()
    }
}
