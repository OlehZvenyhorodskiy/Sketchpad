package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.drawing.RulerState
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

    val angleDeg = (rulerState.angleRad * 180f / Math.PI).toFloat()

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            translate(left = rulerState.center.x, top = rulerState.center.y) {
                rotate(degrees = angleDeg, pivot = Offset.Zero) {
                    val halfW = rulerState.length / 2f
                    val halfH = rulerState.width / 2f

                    // Semi-transparent ruler body
                    drawRect(
                        color = Color(0xD90F172A),
                        topLeft = Offset(-halfW, -halfH),
                        size = androidx.compose.ui.geometry.Size(rulerState.length, rulerState.width)
                    )
                    drawRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(-halfW, -halfH),
                        size = androidx.compose.ui.geometry.Size(rulerState.length, rulerState.width),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )

                    // Draw ruler millimeter and centimeter ticks
                    val step = 16f
                    var x = -halfW + 10f
                    var count = 0
                    while (x <= halfW - 10f) {
                        val isMajor = count % 5 == 0
                        val tickLen = if (isMajor) 24f else 12f

                        // Top edge ticks
                        drawLine(
                            color = if (isMajor) Color(0xFF38BDF8) else Color.White,
                            start = Offset(x, -halfH),
                            end = Offset(x, -halfH + tickLen),
                            strokeWidth = if (isMajor) 2.5f else 1.2f
                        )

                        // Bottom edge ticks
                        drawLine(
                            color = if (isMajor) Color(0xFF38BDF8) else Color.White,
                            start = Offset(x, halfH),
                            end = Offset(x, halfH - tickLen),
                            strokeWidth = if (isMajor) 2.5f else 1.2f
                        )

                        x += step
                        count++
                    }
                }
            }
        }

        // Center Handle: Smooth Pan Dragging
        Surface(
            shape = CircleShape,
            color = Color(0xFF0284C7),
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (rulerState.center.x - 24.dp.toPx()).roundToInt(),
                        (rulerState.center.y - 24.dp.toPx()).roundToInt()
                    )
                }
                .size(48.dp)
                .pointerInput(rulerState) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onRulerChange(rulerState.copy(center = rulerState.center + dragAmount))
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = "Перемістити лінійку",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Right Handle: Rotate & Scale Length
        val dx = cos(rulerState.angleRad) * (rulerState.length / 2f)
        val dy = sin(rulerState.angleRad) * (rulerState.length / 2f)
        val handleRightPos = Offset(rulerState.center.x + dx, rulerState.center.y + dy)

        Surface(
            shape = CircleShape,
            color = Color(0xFF38BDF8),
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (handleRightPos.x - 20.dp.toPx()).roundToInt(),
                        (handleRightPos.y - 20.dp.toPx()).roundToInt()
                    )
                }
                .size(40.dp)
                .pointerInput(rulerState) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val touchPos = change.position + handleRightPos
                        val vec = touchPos - rulerState.center
                        val newDist = vec.getDistance().coerceIn(200f, 1400f)
                        val newAngle = atan2(vec.y, vec.x)
                        onRulerChange(rulerState.copy(angleRad = newAngle, length = newDist * 2f))
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Обертати та маштабувати лінійку",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Control Panel Floating Overlay (Above Center)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF00F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (rulerState.center.x - 130.dp.toPx()).roundToInt(),
                        (rulerState.center.y - 80.dp.toPx()).roundToInt()
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Angle Readout Text
                Text(
                    text = "${angleDeg.roundToInt()}°",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Quick Snap Angle Buttons
                Surface(
                    onClick = { onRulerChange(rulerState.copy(angleRad = 0f)) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "0°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = { onRulerChange(rulerState.copy(angleRad = (Math.PI / 2).toFloat())) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "90°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = { onRulerChange(rulerState.copy(angleRad = (Math.PI / 4).toFloat())) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "45°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Close Button
                Surface(
                    onClick = onCloseClick,
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Сховати лінійку",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
