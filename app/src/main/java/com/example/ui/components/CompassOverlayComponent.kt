package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun CompassOverlayComponent(
    isVisible: Boolean,
    onCloseClick: () -> Unit
) {
    if (!isVisible) return

    var centerPoint by remember { mutableStateOf(Offset(400f, 500f)) }
    var radius by remember { mutableFloatStateOf(160f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val currentPos = change.position
                        val dist = hypot((currentPos.x - centerPoint.x).toDouble(), (currentPos.y - centerPoint.y).toDouble()).toFloat()
                        if (dist > 30f) {
                            radius = dist
                        } else {
                            centerPoint += dragAmount
                        }
                    }
                }
        ) {
            // Draw Center Pivot
            drawCircle(Color.Red, radius = 6.dp.toPx(), center = centerPoint)
            drawCircle(Color.White, radius = 3.dp.toPx(), center = centerPoint)

            // Draw Compass Circle Guideline
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = radius,
                center = centerPoint,
                style = Stroke(width = 2.dp.toPx())
            )

            // Radius Line
            drawLine(
                color = Color(0xFF38BDF8),
                start = centerPoint,
                end = Offset(centerPoint.x + radius, centerPoint.y),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Radius Text Badge
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
            shadowElevation = 4.dp,
            modifier = Modifier.offset {
                IntOffset((centerPoint.x + radius / 2).roundToInt(), (centerPoint.y - 30).roundToInt())
            }
        ) {
            Text(
                text = "${radius.roundToInt()} px",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Close Button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp,
            modifier = Modifier.offset {
                IntOffset((centerPoint.x - 18).roundToInt(), (centerPoint.y - 60).roundToInt())
            }
        ) {
            IconButton(onClick = onCloseClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_compass), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
