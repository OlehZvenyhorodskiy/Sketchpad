package com.example.shared.protocol

import com.example.shared.model.HslaColor
import com.example.shared.model.StrokePoint
import com.example.shared.model.ToolType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class SketchLinkPacketType {
    HANDSHAKE_REQUEST,
    HANDSHAKE_RESPONSE,
    STROKE_DOWN,
    STROKE_MOVE,
    STROKE_UP,
    ERASER_MOVE,
    TOOL_SWITCH,
    CLEAR_CANVAS,
    PING,
    PONG
}

@Serializable
data class SketchLinkStrokeEvent(
    val strokeId: String,
    val tool: ToolType,
    val color: HslaColor,
    val baseWidth: Float,
    val point: StrokePoint
)

@Serializable
data class SketchLinkPacket(
    val type: SketchLinkPacketType,
    val sessionId: String = "",
    val pin: String = "",
    val strokeEvent: SketchLinkStrokeEvent? = null,
    val strokeEventsDelta: List<StrokePoint> = emptyList(),
    val strokeId: String? = null,
    val tool: ToolType? = null,
    val color: HslaColor? = null,
    val baseWidth: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String = JsonConfig.json.encodeToString(this)

    companion object {
        fun fromJson(json: String): SketchLinkPacket? = runCatching {
            JsonConfig.json.decodeFromString<SketchLinkPacket>(json)
        }.getOrNull()
    }
}

object JsonConfig {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}

@Serializable
data class PairingInfo(
    val serverIp: String,
    val port: Int = 8765,
    val pin: String,
    val hostName: String
) {
    fun toQrString(): String = "sketchlink://$serverIp:$port?pin=$pin&host=$hostName"
}
