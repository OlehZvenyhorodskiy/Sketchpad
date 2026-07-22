package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.models.BackgroundPattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridBackgroundSelector(
    currentPattern: BackgroundPattern,
    onSelect: (BackgroundPattern) -> Unit,
    onDismiss: () -> Unit
) {
    val patterns = BackgroundPattern.entries.toList()
    val labels = mapOf(
        BackgroundPattern.BLANK to "Без фону",
        BackgroundPattern.NONE to "Без фону",
        BackgroundPattern.LINED to "Лінійка",
        BackgroundPattern.DOTTED to "Точки",
        BackgroundPattern.GRID_SQUARE to "Квадратна сітка",
        BackgroundPattern.GRID_ISOMETRIC to "Ізометрія",
        BackgroundPattern.PROTRACTOR to "Транспортир",
        BackgroundPattern.MUSIC_STAFF to "Нотоносець"
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp).padding(bottom = 32.dp)) {
            Text("Фон полотна", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(patterns) { pattern ->
                    FilterChip(
                        selected = pattern == currentPattern,
                        onClick = { onSelect(pattern) },
                        label = { Text(labels[pattern] ?: pattern.name) }
                    )
                }
            }
        }
    }
}
