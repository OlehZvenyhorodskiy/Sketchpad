package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import com.example.data.models.CanvasReferenceNavigationRequest

/**
 * Animates an editor transform to a Mini-Obsidian destination.
 *
 * The host should clear [request] in [onTargetReached]. When navigating to another sketchpad,
 * retain the request above the editor route and expose it here only after the target page is ready.
 */
@Composable
fun CanvasReferenceNavigationEffect(
    request: CanvasReferenceNavigationRequest?,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    currentPan: Offset,
    currentZoom: Float,
    onTransform: (pan: Offset, zoom: Float) -> Unit,
    onTargetReached: (CanvasReferenceNavigationRequest) -> Unit
) {
    val latestOnTransform by rememberUpdatedState(onTransform)
    val latestOnTargetReached by rememberUpdatedState(onTargetReached)

    LaunchedEffect(request, viewportWidthPx, viewportHeightPx) {
        val activeRequest = request ?: return@LaunchedEffect
        if (!viewportWidthPx.isFinite() || !viewportHeightPx.isFinite() ||
            viewportWidthPx <= 0f || viewportHeightPx <= 0f
        ) {
            return@LaunchedEffect
        }

        val startPan = Offset(
            x = currentPan.x.takeIf(Float::isFinite) ?: 0f,
            y = currentPan.y.takeIf(Float::isFinite) ?: 0f
        )
        val startZoom = currentZoom.takeIf(Float::isFinite)
            ?.coerceIn(com.example.data.models.CanvasViewport.MIN_ZOOM, com.example.data.models.CanvasViewport.MAX_ZOOM)
            ?: com.example.data.models.CanvasViewport.DEFAULT_ZOOM
        val targetViewport = activeRequest.viewport.normalized()
        val targetPan = Offset(
            x = targetViewport.panX(viewportWidthPx),
            y = targetViewport.panY(viewportHeightPx)
        )

        if (activeRequest.transitionDurationMillis == 0) {
            latestOnTransform(targetPan, targetViewport.zoom)
        } else {
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = activeRequest.transitionDurationMillis,
                    easing = FastOutSlowInEasing
                )
            ) {
                val progress = value
                latestOnTransform(
                    Offset(
                        x = startPan.x + (targetPan.x - startPan.x) * progress,
                        y = startPan.y + (targetPan.y - startPan.y) * progress
                    ),
                    startZoom + (targetViewport.zoom - startZoom) * progress
                )
            }
        }
        latestOnTargetReached(activeRequest)
    }
}
