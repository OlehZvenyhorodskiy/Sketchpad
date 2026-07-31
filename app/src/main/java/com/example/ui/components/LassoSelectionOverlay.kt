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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*

@Composable
fun LassoSelectionOverlay(
    isActive: Boolean,
    scale: Float,
    panOffset: Offset,
    onLassoComplete: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    var screenPoints by remember { mutableStateOf(emptyList<Offset>()) }
    val currentScale by rememberUpdatedState(scale)
    val currentPanOffset by rememberUpdatedState(panOffset)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isActive) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    screenPoints = listOf(down.position)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        if (change.pressed) {
                            screenPoints = screenPoints + change.position
                            change.consume()
                        }
                    } while (change.pressed)

                    if (screenPoints.size > 3) {
                        val worldPoints = screenPoints.map { sp ->
                            Offset(
                                (sp.x - currentPanOffset.x) / currentScale,
                                (sp.y - currentPanOffset.y) / currentScale
                            )
                        }
                        onLassoComplete(worldPoints)
                    }
                    screenPoints = emptyList()
                }
            }
    ) {
        if (screenPoints.size > 1) {
            val path = Path().apply {
                moveTo(screenPoints[0].x, screenPoints[0].y)
                screenPoints.forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, Color(0x2238BDF8))
            drawPath(
                path = path,
                color = Color(0xFF38BDF8),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                )
            )
        }
    }
}
