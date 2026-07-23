package com.example.academic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.data.models.StrokeEntity

data class AROverlayState(
    val isArActive: Boolean = false,
    val targetPosition: Offset = Offset(500f, 500f),
    val targetScale: Float = 1.0f,
    val targetRotation: Float = 0f,
    val trackingConfidence: Float = 0.92f
)

object AROverlayEngine {

    fun computeARProjection(
        silhouetteStrokes: List<StrokeEntity>,
        frameSize: Size,
        handTrackOffset: Offset
    ): AROverlayState {
        val targetPos = Offset(
            x = handTrackOffset.x.coerceIn(50f, frameSize.width - 50f),
            y = handTrackOffset.y.coerceIn(50f, frameSize.height - 50f)
        )

        return AROverlayState(
            isArActive = true,
            targetPosition = targetPos,
            targetScale = 1.1f,
            targetRotation = 5.0f,
            trackingConfidence = 0.95f
        )
    }
}
