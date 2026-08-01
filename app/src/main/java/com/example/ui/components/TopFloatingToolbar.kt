package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.ui.theme.ThemedPanel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
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
import com.example.R

@Composable
fun TopFloatingToolbar(
    currentTool: ToolType,
    eraserMode: EraserMode,
    strokeWidth: Float,
    strokeOpacity: Float,
    currentColor: HslaColor,
    rulerVisible: Boolean,
    isSlidersVertical: Boolean,
    selectionMode: com.example.data.models.SelectionMode = com.example.data.models.SelectionMode.SINGLE,
    onToolSelect: (ToolType) -> Unit,
    onEraserModeToggle: () -> Unit,
    onSelectionModeToggle: () -> Unit = {},
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeOpacityChange: (Float) -> Unit,
    onColorPickerClick: () -> Unit,
    onToggleSliderOrientation: () -> Unit,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp),
        horizontalArrangement = if (isSlidersVertical)
            Arrangement.Center
        else
            Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ═══════════════════════════════════════════════════════════
        // LEFT PILL: Width Slider — ВИДИМИЙ ЛИШЕ у горизонтальному режимі
        // ═══════════════════════════════════════════════════════════
        if (!isSlidersVertical) {
            ThemedPanel(
                modifier = Modifier.weight(1f),
                shadowElevation = 6.dp,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                    Icon(
                        imageVector = Icons.Default.WidthNormal,
                        contentDescription = stringResource(R.string.thickness),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    val isEraser = currentTool == ToolType.ERASER
                    val previewOutline = if (currentColor.lightness > 0.82f) Color(0xFF334155)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${if (isEraser) com.example.core.drawing.DrawingEngine.eraserDiameter(strokeWidth).toInt() else strokeWidth.toInt()} px",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, previewOutline.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEraser) {
                            Box(
                                modifier = Modifier
                                    .size(strokeWidth.dp.coerceIn(2.dp, 28.dp))
                                    .border(2.dp, previewOutline, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(strokeWidth.dp.coerceIn(2.dp, 28.dp))
                                    .clip(CircleShape)
                                    .background(currentColor.copy(alpha = strokeOpacity).toColor())
                                    .border(1.dp, previewOutline, CircleShape)
                            )
                        }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        val presets = if (currentTool == ToolType.ERASER) {
                            listOf(2f to 2, 6f to 6, 12f to 12)
                        } else {
                            listOf(2f to 2, 5f to 5, 12f to 12)
                        }
                        presets.forEach { (preset, label) ->
                            WidthPresetButton(
                                label = label,
                                selected = kotlin.math.abs(strokeWidth - preset) < 0.25f,
                                onClick = { onStrokeWidthChange(preset) }
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // CENTER PILL: Drawing Tools — ЗАВЖДИ ВИДИМИЙ
        // ═══════════════════════════════════════════════════════════
        ThemedPanel(
            modifier = if (isSlidersVertical) {
                // Keep the actual tool strip content-sized and centered. A fill-width panel
                // looked off-centre because the unused scroll area accumulated on the right.
                Modifier.widthIn(max = 760.dp)
            } else Modifier.widthIn(max = 430.dp),
            shadowElevation = 8.dp,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ToolIconButton(
                    icon = Icons.Default.TouchApp,
                    label = stringResource(R.string.pointer_tool),
                    isSelected = currentTool == ToolType.POINTER,
                    onClick = { onToolSelect(ToolType.POINTER) }
                )
                ToolIconButton(
                    icon = Icons.Default.Create,
                    label = stringResource(R.string.pen),
                    isSelected = currentTool == ToolType.PEN,
                    onClick = { onToolSelect(ToolType.PEN) }
                )
                if (isSlidersVertical) {
                    ToolIconButton(
                        icon = Icons.Default.Brush,
                        label = stringResource(R.string.pencil),
                        isSelected = currentTool == ToolType.PENCIL,
                        onClick = { onToolSelect(ToolType.PENCIL) }
                    )
                    ToolIconButton(
                        icon = Icons.Default.Edit,
                        label = stringResource(R.string.ink_pen),
                        isSelected = currentTool == ToolType.FOUNTAIN_PEN,
                        onClick = { onToolSelect(ToolType.FOUNTAIN_PEN) }
                    )
                    ToolIconButton(
                        icon = Icons.Default.Highlight,
                        label = stringResource(R.string.marker),
                        isSelected = currentTool == ToolType.MARKER,
                        onClick = { onToolSelect(ToolType.MARKER) }
                    )
                    ToolIconButton(
                        icon = Icons.Default.Brush,
                        label = stringResource(R.string.ink_brush),
                        isSelected = currentTool == ToolType.INK_PEN,
                        onClick = { onToolSelect(ToolType.INK_PEN) }
                    )
                    ToolIconButton(
                        icon = Icons.Default.BlurOn,
                        label = stringResource(R.string.airbrush),
                        isSelected = currentTool == ToolType.AIRBRUSH,
                        onClick = { onToolSelect(ToolType.AIRBRUSH) }
                    )
                    ToolIconButton(
                        icon = Icons.Default.Texture,
                        label = stringResource(R.string.crayon),
                        isSelected = currentTool == ToolType.CRAYON,
                        onClick = { onToolSelect(ToolType.CRAYON) }
                    )
                    ToolIconButton(
                        icon = Icons.Default.WaterDrop,
                        label = stringResource(R.string.watercolor_brush),
                        isSelected = currentTool == ToolType.WATERCOLOR_BRUSH,
                        onClick = { onToolSelect(ToolType.WATERCOLOR_BRUSH) }
                    )
                }
                val selectorIcon = if (selectionMode == com.example.data.models.SelectionMode.LASSO) Icons.Default.Gesture else Icons.Default.SelectAll
                ToolIconButton(
                    icon = selectorIcon,
                    label = if (selectionMode == com.example.data.models.SelectionMode.LASSO) stringResource(R.string.selection_lasso) else stringResource(R.string.selection_single),
                    isSelected = currentTool == ToolType.SELECTOR,
                    onClick = {
                        if (currentTool == ToolType.SELECTOR) onSelectionModeToggle()
                        else onToolSelect(ToolType.SELECTOR)
                    }
                )
                ToolIconButton(
                    icon = Icons.Default.Backspace,
                    label = if (eraserMode == EraserMode.OBJECT) stringResource(R.string.eraser_object) else stringResource(R.string.eraser_pixel),
                    isSelected = currentTool == ToolType.ERASER,
                    onClick = {
                        if (currentTool == ToolType.ERASER) onEraserModeToggle()
                        else onToolSelect(ToolType.ERASER)
                    }
                )
                ToolIconButton(
                    icon = Icons.Default.FormatColorFill,
                    label = stringResource(R.string.fill_tool),
                    isSelected = currentTool == ToolType.FILL,
                    onClick = { onToolSelect(ToolType.FILL) }
                )
                ToolIconButton(
                    icon = Icons.Default.Colorize,
                    label = stringResource(R.string.eyedropper_tool),
                    isSelected = currentTool == ToolType.EYEDROPPER,
                    onClick = { onToolSelect(ToolType.EYEDROPPER) }
                )
                ToolIconButton(
                    icon = Icons.Default.Title,
                    label = stringResource(R.string.text_tool),
                    isSelected = currentTool == ToolType.TEXT,
                    onClick = { onToolSelect(ToolType.TEXT) }
                )
                ToolIconButton(
                    icon = Icons.Default.Straighten,
                    label = stringResource(R.string.ruler),
                    isSelected = rulerVisible,
                    onClick = { onToolSelect(ToolType.RULER) }
                )

                // Color Swatch
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(currentColor.toColor())
                        .border(
                            2.dp,
                            if (currentColor.lightness > 0.82f) Color(0xFF334155)
                            else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .clickable { onColorPickerClick() }
                )

                // Layout Orientation Toggle
                IconButton(
                    onClick = onToggleSliderOrientation,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = stringResource(R.string.slider_orientation),
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
            ThemedPanel(
                modifier = Modifier.weight(1f),
                shadowElevation = 6.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = stringResource(R.string.opacity_percent, (strokeOpacity * 100).toInt()),
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

@Composable
private fun WidthPresetButton(label: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = label.toString(),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}
