package com.example.data.repository

import com.example.data.models.StrokeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Репозиторій для real-time колаборації.
 * Використовує WebSocket для синхронізації strokes між пристроями.
 * Conflict resolution: Last-Write-Wins (LWW) з timestamp.
 */
class CollaborationRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    data class SyncMessage(
        val type: String,          // "stroke_add", "stroke_delete", "layer_update"
        val canvasId: String,
        val userId: String,
        val timestamp: Long,
        val payload: String        // JSON serialized entity
    )

    private val _incomingStrokes = MutableSharedFlow<StrokeEntity>(extraBufferCapacity = 100)
    val incomingStrokes: Flow<StrokeEntity> = _incomingStrokes.asSharedFlow()

    private var webSocket: okhttp3.WebSocket? = null
    private val client = okhttp3.OkHttpClient()

    fun connect(canvasId: String, userId: String, serverUrl: String) {
        val request = okhttp3.Request.Builder()
            .url("$serverUrl/ws/canvas/$canvasId?user=$userId")
            .build()

        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val adapter = moshi.adapter(SyncMessage::class.java)
                    val msg = adapter.fromJson(text) ?: return
                    when (msg.type) {
                        "stroke_add" -> {
                            val strokeAdapter = moshi.adapter(StrokeEntity::class.java)
                            val stroke = strokeAdapter.fromJson(msg.payload) ?: return
                            _incomingStrokes.tryEmit(stroke)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        })
    }

    fun sendStroke(canvasId: String, userId: String, stroke: StrokeEntity) {
        try {
            val strokeAdapter = moshi.adapter(StrokeEntity::class.java)
            val payloadStr = strokeAdapter.toJson(stroke)
            val msg = SyncMessage(
                type = "stroke_add",
                canvasId = canvasId,
                userId = userId,
                timestamp = System.currentTimeMillis(),
                payload = payloadStr
            )
            val msgAdapter = moshi.adapter(SyncMessage::class.java)
            webSocket?.send(msgAdapter.toJson(msg))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
