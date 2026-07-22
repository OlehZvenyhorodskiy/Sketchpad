package com.example.brush

import com.example.data.models.ToolType

/**
 * Профіль пензля: визначає поведінку stroke.
 */
data class BrushProfile(
    val id: String,
    val name: String,
    val toolType: ToolType,
    val baseWidth: Float = 4f,
    val pressureCurve: PressureCurve = PressureCurve.LINEAR,
    val pressureSensitivity: Float = 1.0f,   // 0 = ігнорувати pressure, 1 = повний вплив
    val tiltSensitivity: Float = 0.0f,       // вплив нахилу на ширину
    val spacing: Float = 0.15f,              // відстань між "відбитками" (0.01-1.0 від ширини)
    val jitter: Float = 0f,                  // випадкове зміщення (0-5px)
    val scatter: Float = 0f,                 // розкидання частинок
    val opacity: Float = 1f,
    val flow: Float = 1f,                    // накопичення фарби
    val smoothing: Float = 0.5f,             // згладжування Catmull-Rom tension
    val textureResId: Int? = null,           // текстура пензля (bitmap)
    val isDashed: Boolean = false,
    val dashPattern: FloatArray = floatArrayOf(10f, 5f)
)

enum class PressureCurve {
    LINEAR,        // лінійна
    EASE_IN,       // повільний старт
    EASE_OUT,      // повільний кінець
    EASE_IN_OUT,   // S-подібна
    HEAVY,         // швидкий набір товщини
    LIGHT;         // повільний набір товщини

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
