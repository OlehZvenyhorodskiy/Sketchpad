package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

enum class TouchCursorShape {
    CIRCLE,
    RING,
    CROSSHAIR
}

@Composable
fun TouchIndicatorOverlay(
    touchPos: Offset?,
    shape: TouchCursorShape = TouchCursorShape.RING,
    color: Color = Color(0xFF38BDF8),
    radiusPx: Float = 18f,
    modifier: Modifier = Modifier
) {
    if (touchPos == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        when (shape) {
            TouchCursorShape.CIRCLE -> {
                drawCircle(color = color.copy(alpha = 0.4f), radius = radiusPx, center = touchPos)
            }
            TouchCursorShape.RING -> {
                drawCircle(color = color, radius = radiusPx, center = touchPos, style = Stroke(width = 2.5f))
            }
            TouchCursorShape.CROSSHAIR -> {
                drawLine(color = color, start = Offset(touchPos.x - radiusPx, touchPos.y), end = Offset(touchPos.x + radiusPx, touchPos.y), strokeWidth = 2f)
                drawLine(color = color, start = Offset(touchPos.x, touchPos.y - radiusPx), end = Offset(touchPos.x, touchPos.y + radiusPx), strokeWidth = 2f)
            }
        }
    }
}
