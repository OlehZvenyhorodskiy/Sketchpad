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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WidthNormal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.EraserMode
import com.example.data.models.HslaColor
import com.example.data.models.ToolType

@Composable
fun TopFloatingToolbar(
    currentTool: ToolType,
    eraserMode: EraserMode,
    strokeWidth: Float,
    strokeOpacity: Float,
    currentColor: HslaColor,
    rulerVisible: Boolean,
    isSlidersVertical: Boolean,
    onToolSelect: (ToolType) -> Unit,
    onEraserModeToggle: () -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeOpacityChange: (Float) -> Unit,
    onColorPickerClick: () -> Unit,
    onToggleSliderOrientation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. LEFT FLUID BLOCK: Sliders Surface taking ALL available space up to the central pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            tonalElevation = 4.dp,
            modifier = Modifier.weight(1f)
        ) {
            if (isSlidersVertical) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Thickness
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.WidthNormal,
                            contentDescription = "Товщина",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${strokeWidth.toInt()} px",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Slider(
                            value = strokeWidth,
                            onValueChange = onStrokeWidthChange,
                            valueRange = 1f..60f,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        )
                    }

                    // Row 2: Opacity
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = "Прозорість",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${(strokeOpacity * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Slider(
                            value = strokeOpacity,
                            onValueChange = onStrokeOpacityChange,
                            valueRange = 0.05f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thickness Section
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WidthNormal,
                            contentDescription = "Товщина",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${strokeWidth.toInt()} px",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Slider(
                            value = strokeWidth,
                            onValueChange = onStrokeWidthChange,
                            valueRange = 1f..60f,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        )
                    }

                    // Divider between Sliders
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )

                    // Opacity Section
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = "Прозорість",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${(strokeOpacity * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Slider(
                            value = strokeOpacity,
                            onValueChange = onStrokeOpacityChange,
                            valueRange = 0.05f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        )
                    }
                }
            }
        }

        // Stable 16px Gap between Sliders Block and Central Pill Toolbar
        Spacer(modifier = Modifier.width(16.dp))

        // 2. CENTRAL PILL: Main Drawing Tools & Colors
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pen
                ToolIconButton(
                    icon = Icons.Default.Create,
                    label = "Ручка",
                    isSelected = currentTool == ToolType.PEN,
                    onClick = { onToolSelect(ToolType.PEN) }
                )

                // Pencil
                ToolIconButton(
                    icon = Icons.Default.Brush,
                    label = "Олівець",
                    isSelected = currentTool == ToolType.PENCIL,
                    onClick = { onToolSelect(ToolType.PENCIL) }
                )

                // Ink Pen / Fountain Pen
                ToolIconButton(
                    icon = Icons.Default.FormatPaint,
                    label = "Перо",
                    isSelected = currentTool == ToolType.INK_PEN || currentTool == ToolType.FOUNTAIN_PEN,
                    onClick = { onToolSelect(ToolType.INK_PEN) }
                )

                // Selector / Lasso
                ToolIconButton(
                    icon = Icons.Default.CropSquare,
                    label = "Ласо",
                    isSelected = currentTool == ToolType.SELECTOR,
                    onClick = { onToolSelect(ToolType.SELECTOR) }
                )

                // Eraser
                ToolIconButton(
                    icon = Icons.Default.Radio,
                    label = if (eraserMode == EraserMode.OBJECT) "Стерка (Об'єкт)" else "Стерка (Піксель)",
                    isSelected = currentTool == ToolType.ERASER,
                    onClick = {
                        if (currentTool == ToolType.ERASER) {
                            onEraserModeToggle()
                        } else {
                            onToolSelect(ToolType.ERASER)
                        }
                    }
                )

                // Ruler
                ToolIconButton(
                    icon = Icons.Default.Straighten,
                    label = "Лінійка",
                    isSelected = rulerVisible,
                    onClick = { onToolSelect(ToolType.RULER) }
                )

                // Color Swatch
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(currentColor.toColor())
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onColorPickerClick() }
                )

                // Layout Orientation Flip Button
                IconButton(
                    onClick = onToggleSliderOrientation,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Орієнтація слайдерів",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

