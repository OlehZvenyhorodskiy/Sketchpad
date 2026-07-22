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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.data.models.BackgroundPattern
import com.example.data.models.HslaColor
import com.example.data.models.PageSizePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasTopMenuBottomSheet(
    currentBgColor: Int,
    currentPattern: BackgroundPattern,
    currentPreset: PageSizePreset,
    onBgColorChange: (Int) -> Unit,
    onPatternChange: (BackgroundPattern) -> Unit,
    onPresetChange: (PageSizePreset, Float?, Float?) -> Unit,
    onOpenCustomColorPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val bgPresets = remember {
        listOf(
            Pair("Білий", 0xFFFFFFFF.toInt()),
            Pair("Сірий", 0xFFF1F5F9.toInt()),
            Pair("Чорний", 0xFF0F172A.toInt()),
            Pair("Кремовий", 0xFFFFFBEB.toInt()),
            Pair("М'ятний", 0xFFECFDF5.toInt()),
            Pair("Темно-синій", 0xFF1E1B4B.toInt())
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
            Text(
                text = "Налаштування сторінки канви",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Background Color Section
            Text(
                text = "Колір фону",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                bgPresets.forEach { (label, colorInt) ->
                    val isSelected = currentBgColor == colorInt
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onBgColorChange(colorInt) }
                    )
                }

                IconButton(onClick = onOpenCustomColorPicker) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Більше кольорів",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Background Pattern Section
            Text(
                text = "Тип фону (паттерн)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = currentPattern == BackgroundPattern.BLANK,
                    onClick = { onPatternChange(BackgroundPattern.BLANK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Чистий")
                }
                SegmentedButton(
                    selected = currentPattern == BackgroundPattern.DOTTED,
                    onClick = { onPatternChange(BackgroundPattern.DOTTED) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("В крапку")
                }
                SegmentedButton(
                    selected = currentPattern == BackgroundPattern.LINED,
                    onClick = { onPatternChange(BackgroundPattern.LINED) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("В лінійку")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Canvas Size Preset Section
            Text(
                text = "Розмір канви",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val presets = listOf(
                Pair(PageSizePreset.UNLIMITED, "Безлімітна"),
                Pair(PageSizePreset.A4_VERTICAL, "A4 (верт)"),
                Pair(PageSizePreset.A4_HORIZONTAL, "A4 (гор)"),
                Pair(PageSizePreset.RATIO_16_9_VERTICAL, "16:9 (верт)"),
                Pair(PageSizePreset.RATIO_16_9_HORIZONTAL, "16:9 (гор)"),
                Pair(PageSizePreset.LETTER_11X85, "Letter 11x8.5\""),
                Pair(PageSizePreset.CUSTOM, "Власний розмір")
            )

            var customW by remember { mutableStateOf("1200") }
            var customH by remember { mutableStateOf(1600.toString()) }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { (preset, label) ->
                    FilterChip(
                        selected = currentPreset == preset,
                        onClick = {
                            if (preset == PageSizePreset.CUSTOM) {
                                onPresetChange(preset, customW.toFloatOrNull() ?: 1200f, customH.toFloatOrNull() ?: 1600f)
                            } else {
                                onPresetChange(preset, null, null)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (currentPreset == PageSizePreset.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customW,
                        onValueChange = {
                            customW = it
                            val w = it.toFloatOrNull()
                            val h = customH.toFloatOrNull()
                            if (w != null && h != null) onPresetChange(PageSizePreset.CUSTOM, w, h)
                        },
                        label = { Text("Ширина (px)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customH,
                        onValueChange = {
                            customH = it
                            val w = customW.toFloatOrNull()
                            val h = it.toFloatOrNull()
                            if (w != null && h != null) onPresetChange(PageSizePreset.CUSTOM, w, h)
                        },
                        label = { Text("Висота (px)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
