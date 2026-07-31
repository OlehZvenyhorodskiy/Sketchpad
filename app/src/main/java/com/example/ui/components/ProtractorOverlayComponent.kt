package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.roundToInt

@Composable
fun ProtractorOverlayComponent(
    isVisible: Boolean,
    onCloseClick: () -> Unit
) {
    if (!isVisible) return

    var centerOffset by remember { mutableStateOf(Offset(300f, 400f)) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        centerOffset += dragAmount
                    }
                }
        ) {
            val radius = 180.dp.toPx()
            translate(centerOffset.x, centerOffset.y) {
                rotate(rotationAngle) {
                    // Draw semi-circle protractor body
                    drawArc(
                        color = Color(0x33000000),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(-radius, -radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.85f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(-radius, -radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(-radius, -radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw degree ticks (0 to 180 deg)
                    for (deg in 0..180 step 5) {
                        val rad = Math.toRadians((deg + 180).toDouble())
                        val tickLen = if (deg % 10 == 0) 14.dp.toPx() else 8.dp.toPx()
                        val innerR = radius - tickLen
                        val x1 = (radius * Math.cos(rad)).toFloat()
                        val y1 = (radius * Math.sin(rad)).toFloat()
                        val x2 = (innerR * Math.cos(rad)).toFloat()
                        val y2 = (innerR * Math.sin(rad)).toFloat()

                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = if (deg % 30 == 0) 2.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    // Center origin cross
                    drawLine(Color.Red, Offset(-12.dp.toPx(), 0f), Offset(12.dp.toPx(), 0f), 2.dp.toPx())
                    drawLine(Color.Red, Offset(0f, -12.dp.toPx()), Offset(0f, 12.dp.toPx()), 2.dp.toPx())
                }
            }
        }

        // Close button overlay handle
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset { IntOffset(centerOffset.x.roundToInt() - 20, centerOffset.y.roundToInt() - 160) }
        ) {
            IconButton(onClick = onCloseClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_protractor), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
