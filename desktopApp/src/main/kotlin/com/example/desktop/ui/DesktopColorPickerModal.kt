package com.example.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.theme.toColor
import com.example.shared.model.HslaColor
import kotlin.math.*

@Composable
fun DesktopColorPickerModal(
    initialColor: HslaColor,
    recentColors: List<HslaColor>,
    onColorSelected: (HslaColor) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var hue by remember { mutableStateOf(initialColor.hue) }
    var saturation by remember { mutableStateOf(initialColor.saturation) }
    var lightness by remember { mutableStateOf(initialColor.lightness) }
    var alpha by remember { mutableStateOf(initialColor.alpha) }

    val presetPalette = listOf(
        HslaColor.BLACK,
        HslaColor.WHITE,
        HslaColor.RED,
        HslaColor(350f, 0.85f, 0.45f, 1f), // Crimson
        HslaColor.BLUE,
        HslaColor(190f, 0.9f, 0.45f, 1f),  // Cyan
        HslaColor.GREEN,
        HslaColor(45f, 0.95f, 0.5f, 1f),   // Gold
        HslaColor(25f, 0.95f, 0.5f, 1f),   // Orange
        HslaColor.PURPLE,
        HslaColor(300f, 0.8f, 0.5f, 1f),   // Magenta
        HslaColor(210f, 0.2f, 0.45f, 1f)   // Slate
    )

    val currentColor = remember(hue, saturation, lightness, alpha) {
        HslaColor(hue, saturation, lightness, alpha)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(360.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Вибір кольору", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeSpec.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                // Color Wheel & Saturation Square
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Hue Ring Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val touch = change.position
                                    val angle = (atan2(touch.y - center.y, touch.x - center.x) * 180f / Math.PI.toFloat() + 360f) % 360f
                                    hue = angle
                                }
                            }
                    ) {
                        val strokeWidth = 24.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2f

                        // Draw rainbow hue sweep
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                    Color.Blue, Color.Magenta, Color.Red
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )

                        // Hue thumb indicator
                        val angleRad = (hue * Math.PI / 180f).toFloat()
                        val thumbX = size.width / 2f + radius * cos(angleRad)
                        val thumbY = size.height / 2f + radius * sin(angleRad)
                        drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(thumbX, thumbY))
                        drawCircle(Color.Black, radius = 7.dp.toPx(), center = Offset(thumbX, thumbY), style = Stroke(2.dp.toPx()))
                    }

                    // Inner Saturation / Lightness Square
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.hsl(hue, 1f, 0.5f))
                            .pointerInput(hue) {
                                detectDragGestures { change, _ ->
                                    saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                    lightness = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // White to transparent horizontal
                            drawRect(
                                Brush.horizontalGradient(listOf(Color.White, Color.Transparent))
                            )
                            // Transparent to black vertical
                            drawRect(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                            )

                            // Thumb
                            val tx = saturation * size.width
                            val ty = (1f - lightness) * size.height
                            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(tx, ty))
                            drawCircle(Color.Black, radius = 5.dp.toPx(), center = Offset(tx, ty), style = Stroke(1.5.dp.toPx()))
                        }
                    }
                }

                // Opacity Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Прозорість (Alpha)", fontSize = 11.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                        Text("${(alpha * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeSpec.accentColor)
                    }
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
                    )
                }

                // Live Preview & Color Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentColor.toColor())
                            .border(1.dp, themeSpec.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format("#%08X", currentColor.toArgbInt()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = themeSpec.colorScheme.onSurface
                        )
                        Text(
                            text = "H:${hue.toInt()}° S:${(saturation * 100).toInt()}% L:${(lightness * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = themeSpec.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Presets Strip
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Пресети", fontSize = 11.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetPalette) { p ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(p.toColor())
                                    .border(1.dp, themeSpec.colorScheme.outlineVariant, CircleShape)
                                    .clickable {
                                        hue = p.hue
                                        saturation = p.saturation
                                        lightness = p.lightness
                                        alpha = p.alpha
                                    }
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Скасувати", color = themeSpec.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            onColorSelected(currentColor)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Застосувати", color = Color.White)
                    }
                }
            }
        }
    }
}
