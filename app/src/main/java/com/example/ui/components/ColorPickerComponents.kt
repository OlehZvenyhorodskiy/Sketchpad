package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.models.HslaColor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    initialColor: HslaColor,
    recentColors: List<HslaColor>,
    onColorSelected: (HslaColor) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(initialColor.hue) }
    var saturation by remember { mutableFloatStateOf(initialColor.saturation) }
    var lightness by remember { mutableFloatStateOf(initialColor.lightness) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    val currentColor = HslaColor(hue, saturation, lightness, alpha)

    androidx.compose.runtime.LaunchedEffect(hue, saturation, lightness, alpha) {
        onColorSelected(currentColor)
    }

    val presetColors = remember {
        listOf(
            HslaColor(0f, 0f, 0f, 1f),       // Black
            HslaColor(0f, 0f, 1f, 1f),       // White
            HslaColor(0f, 1f, 0.5f, 1f),     // Pure Red
            HslaColor(350f, 0.8f, 0.45f, 1f),// Crimson
            HslaColor(220f, 0.9f, 0.55f, 1f),// Royal Blue
            HslaColor(195f, 0.9f, 0.5f, 1f), // Cyan / Sky Blue
            HslaColor(140f, 0.8f, 0.45f, 1f),// Emerald Green
            HslaColor(45f, 0.95f, 0.5f, 1f), // Yellow / Gold
            HslaColor(25f, 0.9f, 0.5f, 1f),  // Orange
            HslaColor(270f, 0.8f, 0.55f, 1f),// Purple
            HslaColor(310f, 0.8f, 0.5f, 1f), // Magenta / Pink
            HslaColor(210f, 0.2f, 0.4f, 1f)  // Slate Grey
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Вибір кольору",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentColor.toColor())
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        // Reset to factory black
                        hue = 0f
                        saturation = 0f
                        lightness = 0f
                        alpha = 1f
                    }) {
                        Text("Скинути")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Готово", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Standard Preset Swatches Palette
            Text(
                text = "Готові палітри",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(presetColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.toColor())
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable {
                                hue = color.hue
                                saturation = color.saturation
                                lightness = color.lightness
                                alpha = color.alpha
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentColors.isNotEmpty()) {
                Text(
                    text = "Нещодавні кольори",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color.toColor())
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    hue = color.hue
                                    saturation = color.saturation
                                    lightness = color.lightness
                                    alpha = color.alpha
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Custom Color Picker Canvas (Hue Ring + Saturation/Lightness Square)
            CustomColorPickerCanvas(
                hue = hue,
                saturation = saturation,
                lightness = lightness,
                onColorChanged = { newHue, newSat, newLight ->
                    hue = newHue
                    saturation = newSat
                    lightness = newLight
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Opacity slider
            Text(text = "Прозорість: ${(alpha * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = alpha,
                onValueChange = { alpha = it },
                valueRange = 0.05f..1f
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CustomColorPickerCanvas(
    hue: Float,
    saturation: Float,
    lightness: Float,
    onColorChanged: (hue: Float, saturation: Float, lightness: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val pos = change.position
                    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                    val radius = min(canvasSize.width, canvasSize.height) / 2f
                    val ringWidth = 24.dp.toPx()
                    val outerRadius = radius - ringWidth / 2f
                    val innerRadius = outerRadius - ringWidth / 2f

                    val dist = (pos - center).getDistance()
                    if (dist >= innerRadius - 10f && dist <= radius + 20f) {
                        val angle = (atan2(pos.y - center.y, pos.x - center.x) * 180f / Math.PI.toFloat() + 360f) % 360f
                        onColorChanged(angle, saturation, lightness)
                    } else if (abs(pos.x - center.x) <= innerRadius * 0.65f && abs(pos.y - center.y) <= innerRadius * 0.65f) {
                        val squareHalf = innerRadius * 0.6f
                        val newSat = ((pos.x - (center.x - squareHalf)) / (2f * squareHalf)).coerceIn(0f, 1f)
                        val newLight = (1f - (pos.y - (center.y - squareHalf)) / (2f * squareHalf)).coerceIn(0f, 1f)
                        onColorChanged(hue, newSat, newLight)
                    }
                }
            }
    ) {
        canvasSize = size
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f
        val ringWidth = 24.dp.toPx()
        val outerRadius = radius - ringWidth / 2f
        val innerRadius = outerRadius - ringWidth / 2f

        // Outer Hue Ring
        for (i in 0 until 360 step 2) {
            val color = Color.hsl(i.toFloat(), 1f, 0.5f)
            drawArc(
                color = color,
                startAngle = i.toFloat(),
                sweepAngle = 3.5f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
                style = Stroke(width = ringWidth)
            )
        }

        // Inner Saturation/Lightness Square
        val squareHalf = innerRadius * 0.6f
        val squareTopLeft = Offset(center.x - squareHalf, center.y - squareHalf)
        val squareSize = Size(squareHalf * 2f, squareHalf * 2f)

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.hsl(hue, 0f, lightness), Color.hsl(hue, 1f, lightness)),
                startX = squareTopLeft.x,
                endX = squareTopLeft.x + squareSize.width
            ),
            topLeft = squareTopLeft,
            size = squareSize
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                startY = squareTopLeft.y,
                endY = squareTopLeft.y + squareSize.height
            ),
            topLeft = squareTopLeft,
            size = squareSize
        )

        // Ring Selector Handle
        val selectorAngleRad = Math.toRadians(hue.toDouble()).toFloat()
        val selectorHandlePos = Offset(
            center.x + outerRadius * cos(selectorAngleRad),
            center.y + outerRadius * sin(selectorAngleRad)
        )
        drawCircle(color = Color.White, radius = 10.dp.toPx(), center = selectorHandlePos)
        drawCircle(color = Color.hsl(hue, 1f, 0.5f), radius = 7.dp.toPx(), center = selectorHandlePos)

        // Square Selector Handle
        val squareHandlePos = Offset(
            squareTopLeft.x + saturation * squareSize.width,
            squareTopLeft.y + (1f - lightness) * squareSize.height
        )
        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = squareHandlePos, style = Stroke(width = 3.dp.toPx()))
        drawCircle(color = Color.hsl(hue, saturation, lightness), radius = 6.dp.toPx(), center = squareHandlePos)
    }
}
