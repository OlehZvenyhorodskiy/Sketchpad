package com.example.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.theme.ThemedPanel
import com.example.desktop.theme.toColor
import com.example.shared.model.HslaColor

@Composable
fun DesktopRightSideToolPanel(
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    strokeOpacity: Float,
    onStrokeOpacityChange: (Float) -> Unit,
    currentColor: HslaColor,
    onColorSelected: (HslaColor) -> Unit,
    onOpenFullPalette: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val themeSpec = LocalThemeSpec.current

    val quickColors = listOf(
        HslaColor.BLACK,
        HslaColor.WHITE,
        HslaColor.RED,
        HslaColor.BLUE,
        HslaColor.GREEN,
        HslaColor.YELLOW,
        HslaColor.PURPLE,
        HslaColor(200f, 0.9f, 0.45f, 1f), // Cyan
        HslaColor(30f, 0.95f, 0.5f, 1f)   // Orange
    )

    val widthPresets = listOf(2f, 6f, 12f, 20f, 32f, 50f)
    val opacityPresets = listOf(0.25f, 0.50f, 0.75f, 1.0f)

    Row(
        modifier = modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle Handle Tab
        Surface(
            onClick = { isExpanded = !isExpanded },
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
            color = themeSpec.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            modifier = Modifier.height(72.dp).width(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Згорнути / Розгорнути панель пензля",
                    tint = themeSpec.accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expandable Content Card
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {
            ThemedPanel(
                modifier = Modifier
                    .width(220.dp)
                    .padding(start = 2.dp),
                surfaceAlpha = 0.96f
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Live Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = themeSpec.accentColor, modifier = Modifier.size(16.dp))
                            Text("Пензель", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeSpec.colorScheme.onSurface)
                        }

                        // Live Preview Circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(currentColor.toColor().copy(alpha = strokeOpacity))
                                .border(1.5.dp, themeSpec.colorScheme.outlineVariant, CircleShape)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // 1. Width Section
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Товщина", fontSize = 11.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                            Text("${strokeWidth.toInt()} px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeSpec.accentColor)
                        }
                        Slider(
                            value = strokeWidth,
                            onValueChange = onStrokeWidthChange,
                            valueRange = 1f..60f,
                            colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            widthPresets.forEach { w ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (strokeWidth == w) themeSpec.accentColor.copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable { onStrokeWidthChange(w) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${w.toInt()}",
                                        fontSize = 10.sp,
                                        fontWeight = if (strokeWidth == w) FontWeight.Bold else FontWeight.Normal,
                                        color = if (strokeWidth == w) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 2. Opacity Section
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Прозорість", fontSize = 11.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                            Text("${(strokeOpacity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeSpec.accentColor)
                        }
                        Slider(
                            value = strokeOpacity,
                            onValueChange = onStrokeOpacityChange,
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            opacityPresets.forEach { op ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp, 20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (strokeOpacity == op) themeSpec.accentColor.copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable { onStrokeOpacityChange(op) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${(op * 100).toInt()}%",
                                        fontSize = 9.sp,
                                        fontWeight = if (strokeOpacity == op) FontWeight.Bold else FontWeight.Normal,
                                        color = if (strokeOpacity == op) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 3. Quick Palette Swatches
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Швидка палітра", fontSize = 11.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.height(68.dp)
                        ) {
                            items(quickColors) { col ->
                                val isSelected = currentColor.toArgbInt() == col.toArgbInt()
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(col.toColor())
                                        .border(
                                            if (isSelected) 2.5.dp else 1.dp,
                                            if (isSelected) themeSpec.accentColor else themeSpec.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                        .clickable { onColorSelected(col) }
                                )
                            }

                            // Full Palette trigger button
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(themeSpec.colorScheme.surfaceContainer)
                                        .border(1.dp, themeSpec.colorScheme.outlineVariant, CircleShape)
                                    .clickable { onOpenFullPalette() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ColorLens, contentDescription = "Повна палітра", tint = themeSpec.accentColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
