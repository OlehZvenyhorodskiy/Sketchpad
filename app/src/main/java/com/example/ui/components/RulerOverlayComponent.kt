package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.drawing.RulerState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RulerOverlayComponent(
    rulerState: RulerState,
    onRulerChange: (RulerState) -> Unit,
    onCloseClick: () -> Unit
) {
    if (!rulerState.isVisible) return

    val rulerStateRef = rememberUpdatedState(rulerState)
    val onRulerChangeRef = rememberUpdatedState(onRulerChange)
    var isCenterDragging by remember { mutableStateOf(false) }
    var isRightDragging by remember { mutableStateOf(false) }
    var localCenter by remember { mutableStateOf(rulerState.center) }
    var localAngleRad by remember { mutableStateOf(rulerState.angleRad) }
    var localLength by remember { mutableStateOf(rulerState.length) }

    LaunchedEffect(rulerState.center) {
        if (!isCenterDragging) localCenter = rulerState.center
    }
    LaunchedEffect(rulerState.angleRad, rulerState.length) {
        if (!isRightDragging) {
            localAngleRad = rulerState.angleRad
            localLength = rulerState.length
        }
    }

    val displayCenter = if (isCenterDragging) localCenter else rulerState.center
    val displayAngleRad = if (isRightDragging) localAngleRad else rulerState.angleRad
    val displayLength = if (isRightDragging) localLength else rulerState.length
    val angleDegrees = normalizedDegrees(displayAngleRad)
    val accent = MaterialTheme.colorScheme.primary
    val body = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    val markings = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            translate(left = displayCenter.x, top = displayCenter.y) {
                rotate(degrees = angleDegrees, pivot = Offset.Zero) {
                    val halfLength = displayLength / 2f
                    val halfHeight = rulerState.width / 2f
                    drawRoundRect(
                        color = body,
                        topLeft = Offset(-halfLength, -halfHeight),
                        size = androidx.compose.ui.geometry.Size(displayLength, rulerState.width),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = 0.9f),
                        topLeft = Offset(-halfLength, -halfHeight),
                        size = androidx.compose.ui.geometry.Size(displayLength, rulerState.width),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                        style = Stroke(width = 2.2f)
                    )

                    // Five minor divisions form one labelled major unit.
                    val step = 16f
                    val inset = 14f
                    val startX = -halfLength + inset
                    val available = (displayLength - inset * 2f).coerceAtLeast(0f)
                    val divisions = (available / step).toInt()
                    val labelPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.argb(
                            (markings.alpha * 255).roundToInt(),
                            (markings.red * 255).roundToInt(),
                            (markings.green * 255).roundToInt(),
                            (markings.blue * 255).roundToInt()
                        )
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    for (index in 0..divisions) {
                        val x = startX + index * step
                        val major = index % 5 == 0
                        val tickLength = if (major) 24f else if (index % 5 == 0) 20f else 12f
                        drawLine(
                            color = if (major) accent else markings,
                            start = Offset(x, -halfHeight),
                            end = Offset(x, -halfHeight + tickLength),
                            strokeWidth = if (major) 2.2f else 1.15f
                        )
                        drawLine(
                            color = if (major) accent else markings,
                            start = Offset(x, halfHeight),
                            end = Offset(x, halfHeight - tickLength),
                            strokeWidth = if (major) 2.2f else 1.15f
                        )
                        if (major) {
                            drawContext.canvas.nativeCanvas.drawText(
                                (index / 5).toString(),
                                x,
                                -halfHeight + tickLength + 15f,
                                labelPaint
                            )
                        }
                    }
                }
            }
        }

        // The small center grip moves the ruler without covering the drawing edge.
        Surface(
            shape = CircleShape,
            color = accent,
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset { IntOffset((displayCenter.x - 20.dp.toPx()).roundToInt(), (displayCenter.y - 20.dp.toPx()).roundToInt()) }
                .size(40.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isCenterDragging = true
                            localCenter = rulerStateRef.value.center
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            localCenter += amount
                            onRulerChangeRef.value(rulerStateRef.value.copy(center = localCenter))
                        },
                        onDragEnd = { isCenterDragging = false },
                        onDragCancel = { isCenterDragging = false }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.OpenWith, stringResource(R.string.move_ruler), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(21.dp))
            }
        }

        val directionX = cos(displayAngleRad)
        val directionY = sin(displayAngleRad)
        val rightHandle = Offset(
            displayCenter.x + directionX * displayLength / 2f,
            displayCenter.y + directionY * displayLength / 2f
        )
        val leftHandle = Offset(
            displayCenter.x - directionX * displayLength / 2f,
            displayCenter.y - directionY * displayLength / 2f
        )

        Surface(
            shape = CircleShape,
            color = accent,
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset { IntOffset((rightHandle.x - 18.dp.toPx()).roundToInt(), (rightHandle.y - 18.dp.toPx()).roundToInt()) }
                .size(36.dp)
                .pointerInput(Unit) {
                    var pointer = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            isRightDragging = true
                            localAngleRad = rulerStateRef.value.angleRad
                            localLength = rulerStateRef.value.length
                            pointer = rightHandle
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            pointer += amount
                            val vector = pointer - displayCenter
                            localLength = (vector.getDistance().coerceIn(160f, 1200f) * 2f)
                            localAngleRad = snapAngle(atan2(vector.y, vector.x))
                            onRulerChangeRef.value(
                                rulerStateRef.value.copy(angleRad = localAngleRad, length = localLength)
                            )
                        },
                        onDragEnd = { isRightDragging = false },
                        onDragCancel = { isRightDragging = false }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.RotateRight, stringResource(R.string.rotate_scale_ruler), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // Angle readout follows the rotation grip instead of floating over the drawing.
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            shadowElevation = 3.dp,
            modifier = Modifier.offset {
                IntOffset((rightHandle.x - 30.dp.toPx()).roundToInt(), (rightHandle.y + 22.dp.toPx()).roundToInt())
            }
        ) {
            Text(
                text = "${angleDegrees.roundToInt()}°",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        Surface(
            onClick = onCloseClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            shadowElevation = 5.dp,
            modifier = Modifier
                .offset { IntOffset((leftHandle.x - 16.dp.toPx()).roundToInt(), (leftHandle.y - 16.dp.toPx()).roundToInt()) }
                .size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, stringResource(R.string.hide_ruler), tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun normalizedDegrees(angleRad: Float): Float {
    val fullTurn = (2.0 * PI).toFloat()
    val normalized = ((angleRad % fullTurn) + fullTurn) % fullTurn
    val degrees = normalized * 180f / PI.toFloat()
    return if (abs(degrees - 360f) < 0.05f) 0f else degrees
}

private fun snapAngle(rawAngle: Float): Float {
    val snapStep = (PI / 4.0).toFloat()
    val candidate = (rawAngle / snapStep).roundToInt() * snapStep
    val snapDistance = abs(atan2(sin(rawAngle - candidate), cos(rawAngle - candidate)))
    return if (snapDistance <= Math.toRadians(5.0).toFloat()) candidate else rawAngle
}
