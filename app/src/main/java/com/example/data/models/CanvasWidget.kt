package com.example.data.models

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class WidgetType {
    CALCULATOR,
    TIMER,
    STICKY_NOTE
}

@JsonClass(generateAdapter = true)
data class CanvasWidgetEntity(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType,
    val x: Float,
    val y: Float,
    val width: Float = 220f,
    val height: Float = 180f,
    val content: String = ""
)
