package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.data.models.BackgroundPattern
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridBackgroundSelector(
    currentPattern: BackgroundPattern,
    onSelect: (BackgroundPattern) -> Unit,
    onDismiss: () -> Unit
) {
    val patterns = BackgroundPattern.entries.toList()
    val labels = mapOf(
        BackgroundPattern.BLANK to stringResource(R.string.background_none),
        BackgroundPattern.NONE to stringResource(R.string.background_none),
        BackgroundPattern.LINED to stringResource(R.string.background_lined),
        BackgroundPattern.DOTTED to stringResource(R.string.dots),
        BackgroundPattern.GRID_SQUARE to stringResource(R.string.background_square_grid),
        BackgroundPattern.GRID_ISOMETRIC to stringResource(R.string.background_isometric),
        BackgroundPattern.PROTRACTOR to stringResource(R.string.background_protractor),
        BackgroundPattern.MUSIC_STAFF to stringResource(R.string.background_music_staff),
        BackgroundPattern.GRAPH_MM to stringResource(R.string.background_millimeter),
        BackgroundPattern.DOT_GRID to stringResource(R.string.background_dot_paper),
        BackgroundPattern.CORNELL_NOTES to stringResource(R.string.background_cornell),
        BackgroundPattern.KANBAN_TEMPLATE to stringResource(R.string.background_kanban),
        BackgroundPattern.ISO_3D to stringResource(R.string.background_3d_sketch)
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.canvas_background), style = MaterialTheme.typography.titleLarge)
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
