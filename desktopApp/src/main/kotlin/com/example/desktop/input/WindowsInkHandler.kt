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

class DesktopShortcutManager(
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onToolSelected: (ToolType) -> Unit,
    val onAdjustBrushSize: (delta: Float) -> Unit,
    val onSetOpacity: (opacity: Float) -> Unit,
    val onToggleWhiteCanvas: () -> Unit,
    val onSave: () -> Unit,
    val onNewPage: () -> Unit
) {
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        val ctrl = event.isCtrlPressed

        return when {
            ctrl && event.key == Key.Z -> {
                onUndo()
                true
            }
            (ctrl && event.key == Key.Y) || (ctrl && event.isShiftPressed && event.key == Key.Z) -> {
                onRedo()
                true
            }
            ctrl && event.key == Key.S -> {
                onSave()
                true
            }
            ctrl && event.key == Key.N -> {
                onNewPage()
                true
            }
            !ctrl && event.key == Key.B -> {
                onToolSelected(ToolType.PEN)
                true
            }
            !ctrl && event.key == Key.P -> {
                onToolSelected(ToolType.PENCIL)
                true
            }
            !ctrl && event.key == Key.E -> {
                onToolSelected(ToolType.ERASER)
                true
            }
            !ctrl && event.key == Key.M -> {
                onToolSelected(ToolType.MARKER)
                true
            }
            !ctrl && event.key == Key.S -> {
                onToolSelected(ToolType.SELECTOR)
                true
            }
            !ctrl && event.key == Key.R -> {
                onToolSelected(ToolType.RULER)
                true
            }
            !ctrl && event.key == Key.LeftBracket -> {
                onAdjustBrushSize(-2f)
                true
            }
            !ctrl && event.key == Key.RightBracket -> {
                onAdjustBrushSize(+2f)
                true
            }
            !ctrl && event.key == Key.One -> { onSetOpacity(0.1f); true }
            !ctrl && event.key == Key.Two -> { onSetOpacity(0.2f); true }
            !ctrl && event.key == Key.Three -> { onSetOpacity(0.3f); true }
            !ctrl && event.key == Key.Four -> { onSetOpacity(0.4f); true }
            !ctrl && event.key == Key.Five -> { onSetOpacity(0.5f); true }
            !ctrl && event.key == Key.Six -> { onSetOpacity(0.6f); true }
            !ctrl && event.key == Key.Seven -> { onSetOpacity(0.7f); true }
            !ctrl && event.key == Key.Eight -> { onSetOpacity(0.8f); true }
            !ctrl && event.key == Key.Nine -> { onSetOpacity(0.9f); true }
            !ctrl && event.key == Key.Zero -> { onSetOpacity(1.0f); true }
            !ctrl && event.key == Key.F11 -> {
                onToggleWhiteCanvas()
                true
            }
            else -> false
        }
    }
}
