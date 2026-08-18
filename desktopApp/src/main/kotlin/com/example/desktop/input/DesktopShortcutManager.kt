package com.example.desktop.input

import androidx.compose.ui.input.key.*
import com.example.shared.model.ToolType

class DesktopShortcutManager(
    private val onUndo: () -> Unit,
    private val onRedo: () -> Unit,
    private val onToolSelected: (ToolType) -> Unit,
    private val onAdjustBrushSize: (Float) -> Unit,
    private val onSetOpacity: (Float) -> Unit,
    private val onToggleWhiteCanvas: () -> Unit,
    private val onSave: () -> Unit,
    private val onNewPage: () -> Unit
) {
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        val isCtrl = event.isCtrlPressed || event.isMetaPressed

        // Ctrl Combinations
        if (isCtrl) {
            when (event.key) {
                Key.Z -> {
                    if (event.isShiftPressed) onRedo() else onUndo()
                    return true
                }
                Key.Y -> {
                    onRedo()
                    return true
                }
                Key.S -> {
                    onSave()
                    return true
                }
                Key.N -> {
                    onNewPage()
                    return true
                }
            }
        }

        // Single Key Tool Shortcuts
        when (event.key) {
            Key.B -> {
                onToolSelected(ToolType.PEN)
                return true
            }
            Key.P -> {
                onToolSelected(ToolType.PENCIL)
                return true
            }
            Key.M -> {
                onToolSelected(ToolType.MARKER)
                return true
            }
            Key.E -> {
                onToolSelected(ToolType.ERASER)
                return true
            }
            Key.S -> {
                onToolSelected(ToolType.SELECTOR)
                return true
            }
            Key.R -> {
                onToolSelected(ToolType.RULER)
                return true
            }
            Key.T -> {
                onToolSelected(ToolType.TEXT)
                return true
            }
            Key.LeftBracket -> {
                onAdjustBrushSize(-2f)
                return true
            }
            Key.RightBracket -> {
                onAdjustBrushSize(2f)
                return true
            }
            Key.F11 -> {
                onToggleWhiteCanvas()
                return true
            }
        }

        return false
    }
}
