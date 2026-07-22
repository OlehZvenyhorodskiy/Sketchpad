package com.example.ai

import com.example.BuildConfig
import com.example.data.models.PageEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

class GeminiAssistantService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun queryCanvasAssistant(
        userPrompt: String,
        pages: List<PageEntity>,
        canvasTitle: String,
        audioTranscripts: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Будь ласка, вкажіть дійсний GEMINI_API_KEY в налаштуваннях / Secrets панелі AI Studio для використання AI-асистента."
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append("Контекст поточного конспекту/канви: \"$canvasTitle\"\n\n")

        pages.forEachIndexed { index, page ->
            contextBuilder.append("--- Сторінка ${index + 1} ---\n")
            if (page.textBlocks.isNotEmpty()) {
                contextBuilder.append("Текстові блоки:\n")
                page.textBlocks.forEach { tb ->
                    contextBuilder.append("- ${tb.text}\n")
                }
            }
            if (page.shapes.isNotEmpty()) {
                contextBuilder.append("Фігури на сторінці: ${page.shapes.joinToString { it.shapeType.name }}\n")
            }
            if (page.charts.isNotEmpty()) {
                contextBuilder.append("Графіки: ${page.charts.joinToString { it.title }}\n")
            }
            if (page.strokes.isNotEmpty()) {
                contextBuilder.append("Рукописних штрихів/ліній на сторінці: ${page.strokes.size}\n")
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
            e.printStackTrace()
            return@withContext "Помилка зв'язку з AI-сервісом: ${e.localizedMessage}"
        }
    }
}
