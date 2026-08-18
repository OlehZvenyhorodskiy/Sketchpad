package com.example.desktop.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class ChatMessage(
    val role: String, // "user" or "model" / "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object AiModelDefaults {
    val GEMINI_DEFAULT = "gemini-2.5-flash"
    val GEMINI_MODELS = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro")

    val OPENAI_DEFAULT = "gpt-4o"
    val OPENAI_MODELS = listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "o1-mini")

    val ANTHROPIC_DEFAULT = "claude-3-5-sonnet-20241022"
    val ANTHROPIC_MODELS = listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")

    val DEEPSEEK_DEFAULT = "deepseek-chat"
    val DEEPSEEK_MODELS = listOf("deepseek-chat", "deepseek-reasoner")
}

interface AiProvider {
    val id: String
    val displayName: String
    val supportsVision: Boolean
    val availableModels: List<String>
    val defaultModel: String
    val apiKeyHelpUrl: String
    val apiKeyHelpText: String
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
    override val availableModels: List<String> = AiModelDefaults.GEMINI_MODELS
    override val defaultModel: String = AiModelDefaults.GEMINI_DEFAULT
    override val apiKeyHelpUrl: String = "https://aistudio.google.com/apikey"
    override val apiKeyHelpText: String = "Створіть ключ на aistudio.google.com/apikey"

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
    ): String = withContext(Dispatchers.IO) {
        val modelName = model?.ifBlank { null } ?: defaultModel
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
                return@withContext "Помилка Gemini API (${response.code}): $bodyString"
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
            "Помилка зв'язку з Gemini: ${e.localizedMessage}"
        }
    }
}

class OpenAiProvider : AiProvider {
    override val id: String = "OPENAI"
    override val displayName: String = "OpenAI (GPT)"
    override val supportsVision: Boolean = true
    override val availableModels: List<String> = AiModelDefaults.OPENAI_MODELS
    override val defaultModel: String = AiModelDefaults.OPENAI_DEFAULT
    override val apiKeyHelpUrl: String = "https://platform.openai.com/api-keys"
    override val apiKeyHelpText: String = "Створіть ключ на platform.openai.com/api-keys"

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
    ): String = withContext(Dispatchers.IO) {
        val targetUrl = endpoint?.ifBlank { null } ?: "https://api.openai.com/v1/chat/completions"
        val targetModel = model?.ifBlank { null } ?: defaultModel

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
            "Помилка зв'язку з OpenAI: ${e.localizedMessage}"
        }
    }
}

class AnthropicProvider : AiProvider {
    override val id: String = "ANTHROPIC"
    override val displayName: String = "Anthropic Claude"
    override val supportsVision: Boolean = true
    override val availableModels: List<String> = AiModelDefaults.ANTHROPIC_MODELS
    override val defaultModel: String = AiModelDefaults.ANTHROPIC_DEFAULT
    override val apiKeyHelpUrl: String = "https://console.anthropic.com/settings/keys"
    override val apiKeyHelpText: String = "Створіть ключ на console.anthropic.com"

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
    ): String = withContext(Dispatchers.IO) {
        val targetUrl = endpoint?.ifBlank { null } ?: "https://api.anthropic.com/v1/messages"
        val targetModel = model?.ifBlank { null } ?: defaultModel

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
                return@withContext contentArr.getJSONObject(0).optString("text", "")
            }
            "Не вдалося розпарсити відповідь Anthropic."
        } catch (e: Exception) {
            "Помилка зв'язку з Anthropic: ${e.localizedMessage}"
        }
    }
}

class DeepSeekProvider : AiProvider {
    override val id: String = "DEEPSEEK"
    override val displayName: String = "DeepSeek AI"
    override val supportsVision: Boolean = false
    override val availableModels: List<String> = AiModelDefaults.DEEPSEEK_MODELS
    override val defaultModel: String = AiModelDefaults.DEEPSEEK_DEFAULT
    override val apiKeyHelpUrl: String = "https://platform.deepseek.com/api_keys"
    override val apiKeyHelpText: String = "Створіть ключ на platform.deepseek.com"

    private val openAiProvider = OpenAiProvider()

    override suspend fun query(
        text: String,
        imageBase64: String?,
        apiKey: String,
        endpoint: String?,
        model: String?
    ): String {
        val targetEndpoint = endpoint?.ifBlank { null } ?: "https://api.deepseek.com/chat/completions"
        val targetModel = model?.ifBlank { null } ?: defaultModel
        return openAiProvider.query(text, null, apiKey, targetEndpoint, targetModel)
    }
}

class CustomOpenAiCompatibleProvider : AiProvider {
    override val id: String = "CUSTOM"
    override val displayName: String = "Custom OpenAI-compatible"
    override val supportsVision: Boolean = true
    override val availableModels: List<String> = emptyList()
    override val defaultModel: String = ""
    override val apiKeyHelpUrl: String = ""
    override val apiKeyHelpText: String = "Введіть URL та ключ свого OpenAI-сумісного провайдера (напр. Ollama / LM Studio)"

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

class DesktopAiPreferences {
    private val configFile = File(System.getProperty("user.home"), ".sketchpad/ai_config.json")
    private val keys = mutableMapOf<String, String>()
    private val endpoints = mutableMapOf<String, String>()
    private val models = mutableMapOf<String, String>()
    var selectedProviderId: String = "GEMINI"

    init {
        load()
    }

    fun getKey(providerId: String): String = keys[providerId] ?: ""
    fun setKey(providerId: String, key: String) {
        keys[providerId] = key
        save()
    }

    fun getEndpoint(providerId: String): String = endpoints[providerId] ?: ""
    fun setEndpoint(providerId: String, ep: String) {
        endpoints[providerId] = ep
        save()
    }

    fun getModel(providerId: String): String = models[providerId] ?: ""
    fun setModel(providerId: String, m: String) {
        models[providerId] = m
        save()
    }

    private fun load() {
        try {
            if (configFile.exists()) {
                val json = JSONObject(configFile.readText())
                selectedProviderId = json.optString("selectedProviderId", "GEMINI")
                val kObj = json.optJSONObject("keys")
                kObj?.keys()?.forEach { k -> keys[k] = kObj.getString(k) }
                val epObj = json.optJSONObject("endpoints")
                epObj?.keys()?.forEach { k -> endpoints[k] = epObj.getString(k) }
                val mObj = json.optJSONObject("models")
                mObj?.keys()?.forEach { k -> models[k] = mObj.getString(k) }
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        try {
            configFile.parentFile?.mkdirs()
            val json = JSONObject().apply {
                put("selectedProviderId", selectedProviderId)
                put("keys", JSONObject(keys as Map<*, *>))
                put("endpoints", JSONObject(endpoints as Map<*, *>))
                put("models", JSONObject(models as Map<*, *>))
            }
            configFile.writeText(json.toString(2))
        } catch (_: Exception) {}
    }
}
