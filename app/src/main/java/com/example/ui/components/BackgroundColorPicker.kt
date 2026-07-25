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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.data.models.HslaColor

val CANVAS_BACKGROUND_PRESETS = listOf(
    0xFFFFFFFF.toInt(), // Білий
    0xFFF8FAFC.toInt(), // Світло-сірий
    0xFFFDFBF7.toInt(), // Кремовий
    0xFFF0F9FF.toInt(), // Ніжно-блакитний
    0xFF121212.toInt(), // Темний
    0xFF000000.toInt()  // Чорний
)

@Composable
fun BackgroundColorPicker(
    selectedColor: Int,
    onSelectColor: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Фон сторінки",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(CANVAS_BACKGROUND_PRESETS) { hex ->
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onSelectColor(hex) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showCustomPicker = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Власний колір",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showCustomPicker) {
        val initialHsla = HslaColor.fromArgb(selectedColor)
        ColorPickerBottomSheet(
            initialColor = initialHsla,
            recentColors = emptyList(),
            onColorSelected = { selected ->
                onSelectColor(selected.toArgbInt())
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}
