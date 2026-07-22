package com.example.core.gesture

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*

/**
 * Обробник мультитач-жестів: pinch-zoom, two-finger rotate, double-tap zoom.
 */
class MultiTouchGestureHandler {

    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var rotation by mutableFloatStateOf(0f)

    var lastPan = Offset.Zero
    var lastScale = 1f
    var lastRotation = 0f

    fun onTransformStart() {
        lastPan = offset
        lastScale = scale
        lastRotation = rotation
    }

    fun onTransform(centroid: Offset, pan: Offset, zoom: Float, rotationDelta: Float) {
        scale = (lastScale * zoom).coerceIn(0.1f, 10f)
        offset = lastPan + pan
        rotation = lastRotation + rotationDelta
    }

    fun onDoubleTapZoom(tapPosition: Offset, viewportWidth: Float, viewportHeight: Float) {
        val targetScale = if (scale > 2f) 1f else scale * 2f
        scale = targetScale
        offset = Offset(
            viewportWidth / 2 - tapPosition.x * targetScale,
            viewportHeight / 2 - tapPosition.y * targetScale
        )
    }
}

fun Modifier.multiTouchGestures(
    handler: MultiTouchGestureHandler,
    onDoubleTap: (Offset) -> Unit = {}
): Modifier = this
    .pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom, rotationDelta ->
            if (handler.scale == handler.lastScale) {
                handler.onTransformStart()
            }
            handler.onTransform(centroid, pan, zoom, rotationDelta)
        }
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { offset -> onDoubleTap(offset) }
        )
    }
