package com.example.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.theme.LocalThemeSpec
import kotlin.math.*

@Composable
fun DesktopRulerOverlay(
    center: Offset,
    angleRad: Float,
    length: Float,
    width: Float,
    onMove: (Offset) -> Unit,
    onRotate: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val degrees = ((angleRad * 180f / Math.PI.toFloat()) % 360f + 360f) % 360f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(degrees = degrees, pivot = center) {
                val halfL = length / 2f
                val halfW = width / 2f

                // Ruler Body
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(center.x - halfL, center.y - halfW),
                    size = androidx.compose.ui.geometry.Size(length, width),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(center.x - halfL, center.y - halfW),
                    size = androidx.compose.ui.geometry.Size(length, width),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(1.5.dp.toPx())
                )

                // Top & Bottom millimeter ticks
                val mmStep = 10f
                var x = center.x - halfL + 20f
                var tickIdx = 0
                while (x < center.x + halfL - 20f) {
                    val tickH = if (tickIdx % 10 == 0) 18.dp.toPx() else if (tickIdx % 5 == 0) 12.dp.toPx() else 6.dp.toPx()
                    // Top ticks
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(x, center.y - halfW),
                        end = Offset(x, center.y - halfW + tickH),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Bottom ticks
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(x, center.y + halfW),
                        end = Offset(x, center.y + halfW - tickH),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += mmStep
                    tickIdx++
                }
            }
        }

        // Center Move Handle
        Surface(
            shape = CircleShape,
            color = themeSpec.accentColor,
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset((center.x - 20).dp, (center.y - 20).dp)
                .size(40.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onMove(center + dragAmount)
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрити лінійку", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Rotate Handle on Right
        val rotHandleX = center.x + (length / 2f - 20f) * cos(angleRad)
        val rotHandleY = center.y + (length / 2f - 20f) * sin(angleRad)

        Surface(
            shape = CircleShape,
            color = Color(0xFF818CF8),
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset((rotHandleX - 16).dp, (rotHandleY - 16).dp)
                .size(32.dp)
                .pointerInput(center) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val diff = change.position - center
                        val newAngle = atan2(diff.y, diff.x)
                        onRotate(newAngle)
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(
                    text = "${degrees.toInt()}°",
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun DesktopProtractorOverlay(
    center: Offset,
    radius: Float = 220f,
    onMove: (Offset) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onMove(center + dragAmount)
                    }
                }
        ) {
            // Semi-circle background
            drawArc(
                color = Color.White.copy(alpha = 0.85f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            drawArc(
                color = Color(0xFF38BDF8),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(2.dp.toPx())
            )

            // Degree marks every 10 deg
            for (deg in 0..180 step 5) {
                val rad = (deg + 180) * Math.PI.toFloat() / 180f
                val isMajor = deg % 10 == 0
                val tickLen = if (deg % 30 == 0) 20.dp.toPx() else if (isMajor) 14.dp.toPx() else 8.dp.toPx()

                val startX = center.x + radius * cos(rad)
                val startY = center.y + radius * sin(rad)
                val endX = center.x + (radius - tickLen) * cos(rad)
                val endY = center.y + (radius - tickLen) * sin(rad)

                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = (if (isMajor) 1.5f else 1f).dp.toPx()
                )
            }
        }

        // Close Button at bottom center
        Surface(
            shape = CircleShape,
            color = themeSpec.accentColor,
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset((center.x - 16).dp, (center.y - 16).dp)
                .size(32.dp)
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрити транспортир", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
