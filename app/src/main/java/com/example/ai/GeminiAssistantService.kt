package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.models.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Multi-provider AI assistant service.
 * Reads the Gemini API key from EncryptedSharedPreferences (or falls back to BuildConfig).
 */
class GeminiAssistantService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PREFS_NAME = "ai_keys_prefs"
        private const val KEY_GEMINI = "gemini_api_key"

        /**
         * Save Gemini API key using EncryptedSharedPreferences.
         */
        fun saveApiKey(context: Context, key: String) {
            try {
                val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    androidx.security.crypto.MasterKeys.getOrCreate(
                        androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                    ),
                    context,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                prefs.edit().putString(KEY_GEMINI, key).apply()
            } catch (e: Throwable) {
                // Fallback to regular SharedPreferences if crypto is unavailable
                Log.w("GeminiService", "EncryptedSharedPreferences unavailable, falling back", e)
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_GEMINI, key).apply()
            }
        }

        /**
         * Retrieve Gemini API key; tries EncryptedSharedPreferences, then fallback, then BuildConfig.
         */
        fun getApiKey(context: Context): String {
            // Try encrypted prefs first
            try {
                val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    androidx.security.crypto.MasterKeys.getOrCreate(
                        androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                    ),
                    context,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                val key = prefs.getString(KEY_GEMINI, null)
                if (!key.isNullOrBlank()) return key
            } catch (_: Throwable) {}

            // Then regular prefs
            val fallback = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_GEMINI, null)
            if (!fallback.isNullOrBlank()) return fallback

            // Final fallback: BuildConfig
            return try {
                com.example.BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                ""
            }
        }

        fun hasApiKey(context: Context): Boolean {
            val key = getApiKey(context)
            return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        }
    }

    suspend fun queryCanvasAssistant(
        userPrompt: String,
        pages: List<PageEntity>,
        canvasTitle: String,
        audioTranscripts: List<String> = emptyList(),
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (context != null) getApiKey(context) else {
            try { com.example.BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Будь ласка, вкажіть дійсний GEMINI_API_KEY в налаштуваннях / Secrets панелі AI Studio для використання AI-асистента."
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append("Контекст поточного конспекту/канви: \"$canvasTitle\"\n\n")

        pages.forEachIndexed { index, page ->
            contextBuilder.append("--- Сторінка ${index + 1} ---\n")
            val layers = page.getEffectiveLayers()
            val textBlocks = layers.flatMap { it.textBlocks }
            val shapes = layers.flatMap { it.shapes }
            val charts = layers.flatMap { it.charts }
            val strokes = layers.flatMap { it.strokes }

            if (textBlocks.isNotEmpty()) {
                contextBuilder.append("Текстові блоки:\n")
                textBlocks.forEach { tb ->
                    contextBuilder.append("- ${tb.text}\n")
                }
            }
            if (shapes.isNotEmpty()) {
                contextBuilder.append("Фігури на сторінці: ${shapes.joinToString { it.shapeType.name }}\n")
            }
            if (charts.isNotEmpty()) {
                contextBuilder.append("Графіки: ${charts.joinToString { it.title }}\n")
            }
            if (strokes.isNotEmpty()) {
                contextBuilder.append("Рукописних штрихів/ліній на сторінці: ${strokes.size}\n")
            }
        }

        if (audioTranscripts.isNotEmpty()) {
            contextBuilder.append("\nТранскрипт аудіозаписів лекції:\n")
            audioTranscripts.forEach { tr ->
                contextBuilder.append("- $tr\n")
            }
        }

        val systemInstruction = "Ти — інтелектуальний помічник конспекту MeCanvas. Твоє завдання — допомагати користувачеві вивчати матеріали, відповідати на запитання, пояснювати формули та робити короткі підсумки ЛИШЕ на основі наданого контексту конспекту. Відповідай українською мовою, чітко, структуровано та приязно."

        val jsonBody = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", "$contextBuilder\n\nЗапитання користувача: $userPrompt"))
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                }
                put("parts", partsArray)
            }
            put("systemInstruction", systemInstructionObj)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Помилка запиту до Gemini API (${response.code}). Перевірте з'єднання або ключі."
            }

            val jsonResponse = JSONObject(responseStr)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val answer = parts?.optJSONObject(0)?.optString("text")

            return@withContext answer ?: "Не вдалося отримати відповідь від AI."
        } catch (e: Exception) {
            Log.w("GeminiService", "Failed to query Gemini AI assistant", e)
            return@withContext "Помилка при запиті до AI: ${e.localizedMessage ?: "невідома помилка"}"
        }
    }
}
