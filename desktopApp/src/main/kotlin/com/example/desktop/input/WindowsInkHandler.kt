package com.example.desktop.input

import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerInputChange
import com.example.shared.model.ToolType

data class StylusInputSample(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val tilt: Float = 0f,
    val isEraser: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

object WindowsInkHandler {

    /**
     * Extracts stylus pressure and tilt from AWT / Compose Pointer event.
     * Wacom / Surface Pen styluses report normalized pressure (0.0 - 1.0).
     */
    fun processPointerChange(change: PointerInputChange, fallbackPressure: Float = 0.5f): StylusInputSample {
        val rawPressure = change.pressure
        val pressure = if (rawPressure > 0f) rawPressure.coerceIn(0.05f, 1.0f) else fallbackPressure
        return StylusInputSample(
            x = change.position.x,
            y = change.position.y,
            pressure = pressure,
            timestampMs = change.uptimeMillis
        )
    }
}
