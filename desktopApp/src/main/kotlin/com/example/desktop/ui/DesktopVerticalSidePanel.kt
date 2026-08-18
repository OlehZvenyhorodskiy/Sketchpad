package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.theme.ThemedPanel
import com.example.desktop.theme.toColor
import com.example.shared.model.HslaColor
import com.example.shared.model.ToolType

enum class SidePanelType {
    WIDTH,
    OPACITY
}

@Composable
fun DesktopVerticalSidePanel(
    panelType: SidePanelType,
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    currentColor: HslaColor,
    currentTool: ToolType,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val isEraser = currentTool == ToolType.ERASER
    val isWidth = panelType == SidePanelType.WIDTH

    ThemedPanel(
        modifier = modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        surfaceAlpha = 0.94f
    ) {
        Column(
            modifier = Modifier
                .width(44.dp)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Preview Swatch Circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEraser) Color(0xFFEF4444)
                        else currentColor.toColor().copy(alpha = if (isWidth) 1.0f else currentValue)
                    )
                    .border(1.dp, themeSpec.colorScheme.outlineVariant, CircleShape)
            )

            // Value text
            Text(
                text = if (isWidth) "${currentValue.toInt()}px" else "${(currentValue * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = themeSpec.accentColor
            )

            // Vertical Slider (Rotated -90 degrees)
            Box(
                modifier = Modifier
                    .height(160.dp)
                    .width(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = currentValue,
                    onValueChange = onValueChange,
                    valueRange = if (isWidth) (if (isEraser) 2f..60f else 1f..50f) else 0.05f..1.0f,
                    modifier = Modifier
                        .requiredWidth(150.dp)
                        .rotate(-90f),
                    colors = SliderDefaults.colors(
                        thumbColor = themeSpec.accentColor,
                        activeTrackColor = themeSpec.accentColor
                    )
                )
            }

            // Quick Presets
            if (isWidth) {
                val presets = if (isEraser) listOf(2f, 6f, 12f) else listOf(2f, 5f, 12f)
                presets.forEach { p ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (currentValue == p) themeSpec.accentColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { onValueChange(p) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${p.toInt()}",
                            fontSize = 10.sp,
                            fontWeight = if (currentValue == p) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentValue == p) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                        )
                    }
                }
            } else {
                val opacityPresets = listOf(0.25f, 0.50f, 1.0f)
                opacityPresets.forEach { op ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (currentValue == op) themeSpec.accentColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { onValueChange(op) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(op * 100).toInt()}%",
                            fontSize = 8.sp,
                            fontWeight = if (currentValue == op) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentValue == op) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
