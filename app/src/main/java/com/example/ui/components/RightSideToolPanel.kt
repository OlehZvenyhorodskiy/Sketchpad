package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.HslaColor

@Composable
fun RightSideToolPanel(
    strokeWidth: Float,
    strokeOpacity: Float,
    currentColor: HslaColor,
    recentColors: List<HslaColor>,
    onWidthChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onColorSelect: (HslaColor) -> Unit,
    onOpenFullColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    val presetWidths = listOf(2f, 6f, 12f, 20f, 32f, 50f)
    val presetOpacities = listOf(0.25f, 0.5f, 0.75f, 1f)

    val defaultPalette = listOf(
        HslaColor(0f, 0f, 0f),       // Black
        HslaColor(0f, 0.85f, 0.5f),   // Red
        HslaColor(210f, 0.9f, 0.5f),  // Blue
        HslaColor(130f, 0.8f, 0.4f),  // Green
        HslaColor(45f, 1f, 0.5f),     // Yellow
        HslaColor(270f, 0.8f, 0.5f)   // Purple
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle Expand Handle
        Surface(
            onClick = { isExpanded = !isExpanded },
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            tonalElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Панель інструментів",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded Side Menu Card
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .width(260.dp)
                ) {
                    // Header & Live Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Налаштування пензля",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Live Preview Circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((strokeWidth * 0.7f).dp.coerceIn(4.dp, 30.dp))
                                    .clip(CircleShape)
                                    .background(currentColor.copy(alpha = strokeOpacity).toColor())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Thickness Slider
                    Text(
                        text = "Товщина лінії: ${strokeWidth.toInt()} px",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = strokeWidth,
                        onValueChange = onWidthChange,
                        valueRange = 1f..60f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    )

                    // Quick Thickness Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetWidths.forEach { w ->
                            val isSelected = (strokeWidth - w).let { Math.abs(it) < 2.5f }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .clickable { onWidthChange(w) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${w.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Opacity / Transparency Slider
                    Text(
                        text = "Прозорість: ${(strokeOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = strokeOpacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.05f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    )

                    // Quick Opacity Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetOpacities.forEach { op ->
                            val isSelected = (strokeOpacity - op).let { Math.abs(it) < 0.1f }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .clickable { onOpacityChange(op) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${(op * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. Quick Color Swatches
                    Text(
                        text = "Палітра кольорів",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        defaultPalette.take(5).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color.toColor())
                                    .border(
                                        width = if (color == currentColor) 2.5.dp else 1.dp,
                                        color = if (color == currentColor) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelect(color) }
                            )
                        }

                        // Custom Color Picker Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onOpenFullColorPicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Палітра",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
