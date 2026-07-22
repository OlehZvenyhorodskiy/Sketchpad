package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import com.example.data.models.StrokePoint

@Composable
fun LassoSelectionOverlay(
    isActive: Boolean,
    onSelectionComplete: (List<StrokePoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    var lassoPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isDrawing by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDrawing = true
                    lassoPoints = listOf(down.position)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        if (change.pressed) {
                            lassoPoints = lassoPoints + change.position
                            change.consume()
                        }
                    } while (change.pressed)

                    isDrawing = false
                    if (lassoPoints.size > 3) {
                        onSelectionComplete(lassoPoints.map { StrokePoint(it.x, it.y, 1f, 0f) })
                    }
                    lassoPoints = emptyList()
                }
            }
    ) {
        if (lassoPoints.size > 1) {
            val path = Path().apply {
                moveTo(lassoPoints[0].x, lassoPoints[0].y)
                lassoPoints.forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, Color(0x332196F3))  // semi-transparent blue
            drawPath(path, Color(0xFF2196F3), style = Stroke(2f))
        }
    }
}
