package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.LayerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersBottomSheet(
    layers: List<LayerEntity>,
    activeLayerId: String?,
    onAddLayer: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Layers, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.layers_count, layers.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddLayer,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.new_layer), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Layers render top-to-bottom in UI (highest z-index on top)
            val reversedLayers = remember(layers) { layers.reversed() }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(reversedLayers, key = { _, layer -> layer.id }) { index, layer ->
                    val isActive = layer.id == activeLayerId
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = if (isActive) 4.dp else 1.dp,
                        modifier = Modifier.clickable { onSelectLayer(layer.id) }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { onToggleVisibility(layer.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = stringResource(R.string.visibility),
                                        tint = if (layer.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    if (renamingId == layer.id) {
                                        OutlinedTextField(
                                            value = renameText, onValueChange = { renameText = it },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp), singleLine = true
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            TextButton(onClick = { onRename(layer.id, renameText.trim()); renamingId = null }) {
                                                Text(stringResource(R.string.save), fontSize = 12.sp)
                                            }
                                            TextButton(onClick = { renamingId = null }) { Text(stringResource(R.string.cancel), fontSize = 12.sp) }
                                        }
                                    } else {
                                        Text(
                                            text = layer.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.layer_elements_opacity, layer.totalElements, (layer.opacity * 100).toInt()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = { onMoveUp(layer.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ArrowUpward, stringResource(R.string.move_up), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onMoveDown(layer.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ArrowDownward, stringResource(R.string.move_down), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { renamingId = layer.id; renameText = layer.name }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.rename), modifier = Modifier.size(16.dp))
                                }
                                if (layers.size > 1) {
                                    IconButton(onClick = { onDeleteLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.delete), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Layer opacity slider
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Opacity, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = layer.opacity,
                                    onValueChange = { onOpacityChange(layer.id, it) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
