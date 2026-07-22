package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            .padding(start = 16.dp, top = 8.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ═══════════════════════════════════════════════════════════
        // LEFT PILL: Width Slider — ВИДИМИЙ ЛИШЕ у горизонтальному режимі
        // ═══════════════════════════════════════════════════════════
        if (!isSlidersVertical) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                tonalElevation = 4.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                        valueRange = 1f..50f,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // CENTER PILL: Drawing Tools — ЗАВЖДИ ВИДИМИЙ
        // ═══════════════════════════════════════════════════════════
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            tonalElevation = 6.dp,
            modifier = if (isSlidersVertical) Modifier.fillMaxWidth() else Modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolIconButton(
                    icon = Icons.Default.Create,
                    label = "Ручка",
                    isSelected = currentTool == ToolType.PEN,
                    onClick = { onToolSelect(ToolType.PEN) }
                )
                ToolIconButton(
                    icon = Icons.Default.Brush,
                    label = "Олівець",
                    isSelected = currentTool == ToolType.PENCIL,
                    onClick = { onToolSelect(ToolType.PENCIL) }
                )
                ToolIconButton(
                    icon = Icons.Default.FormatPaint,
                    label = "Перо",
                    isSelected = currentTool == ToolType.INK_PEN || currentTool == ToolType.FOUNTAIN_PEN,
                    onClick = { onToolSelect(ToolType.INK_PEN) }
                )
                ToolIconButton(
                    icon = Icons.Default.Highlight,
                    label = "Маркер",
                    isSelected = currentTool == ToolType.MARKER,
                    onClick = { onToolSelect(ToolType.MARKER) }
                )
                ToolIconButton(
                    icon = Icons.Default.CropSquare,
                    label = "Ласо",
                    isSelected = currentTool == ToolType.SELECTOR,
                    onClick = { onToolSelect(ToolType.SELECTOR) }
                )
                ToolIconButton(
                    icon = Icons.Default.Radio,
                    label = if (eraserMode == EraserMode.OBJECT) "Стерка (Об'єкт)" else "Стерка (Піксель)",
                    isSelected = currentTool == ToolType.ERASER,
                    onClick = {
                        if (currentTool == ToolType.ERASER) onEraserModeToggle()
                        else onToolSelect(ToolType.ERASER)
                    }
                )
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

                // Layout Orientation Toggle
                IconButton(
                    onClick = onToggleSliderOrientation,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Орієнтація слайдерів",
                        tint = if (isSlidersVertical)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // RIGHT PILL: Opacity Slider — ВИДИМИЙ ЛИШЕ у горизонтальному режимі
        // ═══════════════════════════════════════════════════════════
        if (!isSlidersVertical) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                tonalElevation = 4.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
}

@Composable
fun ToolIconButton(
    icon: ImageVector,
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
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
