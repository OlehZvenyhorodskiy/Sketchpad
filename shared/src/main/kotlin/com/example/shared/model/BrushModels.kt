package com.example.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class PressureCurve {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    HEAVY,
    LIGHT;

    fun apply(rawPressure: Float): Float {
        val p = rawPressure.coerceIn(0f, 1f)
        return when (this) {
            LINEAR -> p
            EASE_IN -> p * p
            EASE_OUT -> 1f - (1f - p) * (1f - p)
            EASE_IN_OUT -> if (p < 0.5f) 2f * p * p else 1f - 2f * (1f - p) * (1f - p)
            HEAVY -> Math.pow(p.toDouble(), 0.5).toFloat()
            LIGHT -> Math.pow(p.toDouble(), 2.0).toFloat()
        }
    }
}

@Serializable
data class BrushProfile(
    val id: String,
    val name: String,
    val toolType: ToolType,
    val baseWidth: Float = 4f,
    val pressureCurve: PressureCurve = PressureCurve.LINEAR,
    val pressureSensitivity: Float = 1.0f,
    val tiltSensitivity: Float = 0.0f,
    val spacing: Float = 0.15f,
    val jitter: Float = 0f,
    val scatter: Float = 0f,
    val opacity: Float = 1f,
    val flow: Float = 1f,
    val smoothing: Float = 0.5f,
    val isDashed: Boolean = false,
    val dashPattern: List<Float> = listOf(10f, 5f)
)

object BrushPresets {
    val builtIn: List<BrushProfile> = listOf(
        BrushProfile(
            id = "pen_fine", name = "Тонке перо", toolType = ToolType.PEN,
            baseWidth = 2f, pressureSensitivity = 0.8f, smoothing = 0.6f,
            pressureCurve = PressureCurve.EASE_IN_OUT
        ),
        BrushProfile(
            id = "pen_medium", name = "Середнє перо", toolType = ToolType.PEN,
            baseWidth = 4f, pressureSensitivity = 1.0f, smoothing = 0.5f,
            pressureCurve = PressureCurve.LINEAR
        ),
        BrushProfile(
            id = "pencil_hb", name = "Олівець HB", toolType = ToolType.PENCIL,
            baseWidth = 3f, pressureSensitivity = 0.9f, tiltSensitivity = 0.5f,
            smoothing = 0.4f, opacity = 0.85f, pressureCurve = PressureCurve.LIGHT,
            jitter = 0.3f
        ),
        BrushProfile(
            id = "pencil_2b", name = "Олівець 2B", toolType = ToolType.PENCIL,
            baseWidth = 5f, pressureSensitivity = 1.0f, tiltSensitivity = 0.7f,
            smoothing = 0.3f, opacity = 0.9f, pressureCurve = PressureCurve.HEAVY,
            jitter = 0.5f
        ),
        BrushProfile(
            id = "marker_wide", name = "Маркер", toolType = ToolType.MARKER,
            baseWidth = 12f, pressureSensitivity = 0.2f, smoothing = 0.7f,
            opacity = 0.38f, pressureCurve = PressureCurve.LINEAR
        ),
        BrushProfile(
            id = "calligraphy", name = "Каліграфія", toolType = ToolType.FOUNTAIN_PEN,
            baseWidth = 6f, pressureSensitivity = 1.0f, tiltSensitivity = 1.0f,
            smoothing = 0.5f, pressureCurve = PressureCurve.EASE_IN
        ),
        BrushProfile(
            id = "ink_brush", name = "Чорнильний пензель", toolType = ToolType.INK_PEN,
            baseWidth = 8f, pressureSensitivity = 1.0f, smoothing = 0.4f,
            flow = 0.8f, pressureCurve = PressureCurve.EASE_IN_OUT
        ),
        BrushProfile(
            id = "spray", name = "Спрей", toolType = ToolType.PEN,
            baseWidth = 20f, pressureSensitivity = 0.5f, scatter = 8f,
            spacing = 0.05f, opacity = 0.3f, jitter = 3f
        ),
        BrushProfile(
            id = "dashed", name = "Пунктир", toolType = ToolType.PEN,
            baseWidth = 3f, isDashed = true, dashPattern = listOf(12f, 8f),
            pressureSensitivity = 0f
        )
    )

    fun getById(id: String): BrushProfile = builtIn.find { it.id == id } ?: builtIn[0]
}
