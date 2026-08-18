package com.example.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.theme.ThemedPanel
import com.example.desktop.theme.toColor
import com.example.shared.model.EraserMode
import com.example.shared.model.HslaColor
import com.example.shared.model.SelectionMode
import com.example.shared.model.ToolType

@Composable
fun DesktopTopFloatingToolbar(
    currentTool: ToolType,
    onToolSelected: (ToolType) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    strokeOpacity: Float,
    onStrokeOpacityChange: (Float) -> Unit,
    currentColor: HslaColor,
    onOpenColorPicker: () -> Unit,
    eraserMode: EraserMode,
    onToggleEraserMode: () -> Unit,
    selectionMode: SelectionMode,
    onToggleSelectionMode: () -> Unit,
    useVerticalSliders: Boolean,
    onToggleOrientation: () -> Unit,
    isDrawing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val isEraser = currentTool == ToolType.ERASER
    val isSelector = currentTool == ToolType.SELECTOR
    val toolbarAlpha = if (isDrawing) 0.4f else 1.0f

    ThemedPanel(
        modifier = modifier
            .padding(top = 10.dp)
            .alpha(toolbarAlpha),
        surfaceAlpha = 0.96f
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Width Slider & Presets (Visible only in horizontal mode)
            if (!useVerticalSliders) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.width(180.dp)
                ) {
                    // Preview Swatch
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEraser) Color(0xFFEF4444)
                                else currentColor.toColor().copy(alpha = strokeOpacity)
                            )
                            .border(1.dp, themeSpec.colorScheme.outlineVariant, CircleShape)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEraser) "Ластик" else "Товщина",
                                fontSize = 10.sp,
                                color = themeSpec.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${strokeWidth.toInt()}px",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeSpec.accentColor
                            )
                        }
                        Slider(
                            value = strokeWidth,
                            onValueChange = onStrokeWidthChange,
                            valueRange = if (isEraser) 2f..60f else 1f..50f,
                            modifier = Modifier.height(20.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = themeSpec.accentColor,
                                activeTrackColor = themeSpec.accentColor
                            )
                        )
                    }

                    // Quick presets
                    val presets = if (isEraser) listOf(2f, 6f, 12f) else listOf(2f, 5f, 12f)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        presets.forEach { p ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (strokeWidth == p) themeSpec.accentColor.copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                    .clickable { onStrokeWidthChange(p) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${p.toInt()}",
                                    fontSize = 9.sp,
                                    fontWeight = if (strokeWidth == p) FontWeight.Bold else FontWeight.Normal,
                                    color = if (strokeWidth == p) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                VerticalDivider(modifier = Modifier.height(26.dp).padding(horizontal = 2.dp))
            }

            // 2. Main Tool Buttons
            ToolIconButton(
                icon = Icons.Default.TouchApp,
                label = "Вказівник",
                isSelected = currentTool == ToolType.POINTER,
                onClick = { onToolSelected(ToolType.POINTER) }
            )

            ToolIconButton(
                icon = Icons.Default.Create,
                label = "Перо",
                isSelected = currentTool == ToolType.PEN,
                onClick = { onToolSelected(ToolType.PEN) }
            )

            ToolIconButton(
                icon = Icons.Default.Edit,
                label = "Олівець",
                isSelected = currentTool == ToolType.PENCIL,
                onClick = { onToolSelected(ToolType.PENCIL) }
            )

            ToolIconButton(
                icon = Icons.Default.Brush,
                label = "Чорнило",
                isSelected = currentTool == ToolType.INK_PEN,
                onClick = { onToolSelected(ToolType.INK_PEN) }
            )

            ToolIconButton(
                icon = Icons.Default.Draw,
                label = "Каліграфія",
                isSelected = currentTool == ToolType.FOUNTAIN_PEN,
                onClick = { onToolSelected(ToolType.FOUNTAIN_PEN) }
            )

            ToolIconButton(
                icon = Icons.Default.Highlight,
                label = "Маркер",
                isSelected = currentTool == ToolType.MARKER,
                onClick = { onToolSelected(ToolType.MARKER) }
            )

            ToolIconButton(
                icon = Icons.Default.GraphicEq,
                label = "Лазер",
                isSelected = currentTool == ToolType.LASER,
                onClick = { onToolSelected(ToolType.LASER) }
            )

            ToolIconButton(
                icon = Icons.Default.Gradient,
                label = "Аерограф",
                isSelected = currentTool == ToolType.AIRBRUSH,
                onClick = { onToolSelected(ToolType.AIRBRUSH) }
            )

            // Selector Button (with mode toggle)
            Box {
                ToolIconButton(
                    icon = if (selectionMode == SelectionMode.LASSO) Icons.Default.Gesture else Icons.Default.SelectAll,
                    label = if (selectionMode == SelectionMode.LASSO) "Ласо" else "Виділення",
                    isSelected = isSelector,
                    onClick = {
                        if (isSelector) onToggleSelectionMode()
                        else onToolSelected(ToolType.SELECTOR)
                    }
                )
            }

            // Eraser Button (with mode toggle)
            ToolIconButton(
                icon = Icons.Default.Backspace,
                label = if (eraserMode == EraserMode.OBJECT) "Ластик (Об'єкт)" else "Ластик (Піксель)",
                isSelected = isEraser,
                onClick = {
                    if (isEraser) onToggleEraserMode()
                    else onToolSelected(ToolType.ERASER)
                }
            )

            ToolIconButton(
                icon = Icons.Default.FormatColorFill,
                label = "Заливка",
                isSelected = currentTool == ToolType.FILL,
                onClick = { onToolSelected(ToolType.FILL) }
            )

            ToolIconButton(
                icon = Icons.Default.Colorize,
                label = "Піпетка",
                isSelected = currentTool == ToolType.EYEDROPPER,
                onClick = { onToolSelected(ToolType.EYEDROPPER) }
            )

            ToolIconButton(
                icon = Icons.Default.Title,
                label = "Текст",
                isSelected = currentTool == ToolType.TEXT,
                onClick = { onToolSelected(ToolType.TEXT) }
            )

            ToolIconButton(
                icon = Icons.Default.Straighten,
                label = "Лінійка",
                isSelected = currentTool == ToolType.RULER,
                onClick = { onToolSelected(ToolType.RULER) }
            )

            VerticalDivider(modifier = Modifier.height(26.dp).padding(horizontal = 2.dp))

            // 3. Color Swatch Trigger Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(currentColor.toColor())
                    .border(2.dp, themeSpec.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape)
                    .clickable { onOpenColorPicker() }
            )

            // 4. Orientation Switcher
            IconButton(
                onClick = onToggleOrientation,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (useVerticalSliders) Icons.Default.ViewAgenda else Icons.Default.ViewSidebar,
                    contentDescription = "Змінити розташування слайдерів",
                    tint = themeSpec.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 5. Opacity Slider (Visible only in horizontal mode)
            if (!useVerticalSliders) {
                VerticalDivider(modifier = Modifier.height(26.dp).padding(horizontal = 2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.width(140.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Прозорість", fontSize = 10.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                            Text("${(strokeOpacity * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeSpec.accentColor)
                        }
                        Slider(
                            value = strokeOpacity,
                            onValueChange = onStrokeOpacityChange,
                            valueRange = 0.05f..1.0f,
                            modifier = Modifier.height(20.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = themeSpec.accentColor,
                                activeTrackColor = themeSpec.accentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) themeSpec.accentColor.copy(alpha = 0.22f) else Color.Transparent,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) themeSpec.accentColor else themeSpec.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
