package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.toArgb
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                HsvColorPicker(
                    initialColor = currentColor.toColor(),
                    onColorSelected = { selected ->
                        val hsla = HslaColor.fromArgb(selected.toArgb())
                        hue = hsla.hue
                        saturation = hsla.saturation
                        lightness = hsla.lightness
                    }
                )
            }

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
fun HsvColorPicker(
    initialColor: Color = Color.Red,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Hue Ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = atan2(change.position.y - center.y, change.position.x - center.x)
                        hue = ((angle * 180f / Math.PI.toFloat()) + 360f) % 360f
                        onColorSelected(Color.hsv(hue, saturation, value))
                    }
                }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true
            )
        }

        // Inner Saturation/Value Square
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        onColorSelected(Color.hsv(hue, saturation, value))
                    }
                }
        ) {
            val hueColor = Color.hsv(hue, 1f, 1f)
            drawRect(brush = Brush.verticalGradient(listOf(Color.White, Color.Transparent)))
            drawRect(brush = Brush.horizontalGradient(listOf(Color.Transparent, hueColor)))

            // Selection indicator circle
            val handlePos = Offset(saturation * size.width, (1f - value) * size.height)
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = handlePos,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.hsv(hue, saturation, value),
                radius = 6.dp.toPx(),
                center = handlePos
            )
        }
    }
}
