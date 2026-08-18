package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.theme.AppThemeStyle
import com.example.desktop.theme.LocalThemeSpec
import com.example.shared.model.BackgroundPattern
import com.example.shared.model.PageSizePreset

@Composable
fun DesktopCanvasTopMenuModal(
    currentBackgroundColor: Int,
    onBackgroundColorChange: (Int) -> Unit,
    currentPattern: BackgroundPattern,
    onPatternChange: (BackgroundPattern) -> Unit,
    currentPreset: PageSizePreset,
    onPresetChange: (PageSizePreset) -> Unit,
    currentThemeStyle: AppThemeStyle,
    onThemeStyleChange: (AppThemeStyle) -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current

    val bgPresets = listOf(
        0xFFFFFFFF.toInt() to "Білий",
        0xFFF8FAFC.toInt() to "Світло-сірий",
        0xFFFFFBEB.toInt() to "Кремовий",
        0xFFEFF6FF.toInt() to "Блакитний",
        0xFF1E293B.toInt() to "Темний",
        0xFF000000.toInt() to "Чорний"
    )

    val patterns = listOf(
        BackgroundPattern.BLANK to "Чистий",
        BackgroundPattern.DOTTED to "Крапки",
        BackgroundPattern.LINED to "Лінійка",
        BackgroundPattern.GRID_SQUARE to "Клітинка",
        BackgroundPattern.GRID_ISOMETRIC to "Ізометрія",
        BackgroundPattern.CORNELL_NOTES to "Корнелл",
        BackgroundPattern.GRAPH_MM to "Міліметрівка",
        BackgroundPattern.MUSIC_STAFF to "Нотний стан"
    )

    val pageSizes = listOf(
        PageSizePreset.UNLIMITED to "Необмежене",
        PageSizePreset.A4_VERTICAL to "A4 Вертикальний",
        PageSizePreset.A4_HORIZONTAL to "A4 Горизонтальний",
        PageSizePreset.RATIO_16_9_HORIZONTAL to "16:9 Горизонтальний",
        PageSizePreset.LETTER_11X85 to "Letter"
    )

    val themes = listOf(
        AppThemeStyle.SYSTEM_DEFAULT to "Системна",
        AppThemeStyle.PAPER_NOTEBOOK to "Блокнот",
        AppThemeStyle.NEUMORPHISM to "Неоморфізм",
        AppThemeStyle.AMOLED_BLACK to "Amoled Black",
        AppThemeStyle.CHALKBOARD to "Шкільна дошка",
        AppThemeStyle.SEPIA_EINK to "E-Ink Сепія",
        AppThemeStyle.MIDNIGHT_INDIGO to "Нічний Indigo",
        AppThemeStyle.FOREST_STUDY to "Лісовий",
        AppThemeStyle.ROSE_QUARTZ to "Рожевий кварц",
        AppThemeStyle.HIGH_CONTRAST to "Високий контраст"
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(460.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Налаштування полотна та тем", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = themeSpec.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                // 1. Background Color
                Text("Колір фону", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(bgPresets) { (col, label) ->
                        val isSelected = currentBackgroundColor == col
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onBackgroundColorChange(col) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .border(
                                        if (isSelected) 2.5.dp else 1.dp,
                                        if (isSelected) themeSpec.accentColor else themeSpec.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                            Text(label, fontSize = 9.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 2. Background Pattern
                Text("Шаблон фонової сітки", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(patterns) { (pat, name) ->
                        val isSelected = currentPattern == pat
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPatternChange(pat) },
                            label = { Text(name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeSpec.accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = themeSpec.accentColor
                            )
                        )
                    }
                }

                // 3. Page Size Preset
                Text("Формат та розмір сторінки", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pageSizes) { (ps, name) ->
                        val isSelected = currentPreset == ps
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPresetChange(ps) },
                            label = { Text(name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeSpec.accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = themeSpec.accentColor
                            )
                        )
                    }
                }

                // 4. Themes & Styles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Стиль теми оформлення (10 стилів)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurfaceVariant)
                    IconButton(onClick = onToggleDarkTheme, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Темна / Світла",
                            tint = themeSpec.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(themes) { (style, name) ->
                        val isSelected = currentThemeStyle == style
                        FilterChip(
                            selected = isSelected,
                            onClick = { onThemeStyleChange(style) },
                            label = { Text(name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeSpec.accentColor.copy(alpha = 0.25f),
                                selectedLabelColor = themeSpec.accentColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor)
                ) {
                    Text("Готово", color = Color.White)
                }
            }
        }
    }
}
