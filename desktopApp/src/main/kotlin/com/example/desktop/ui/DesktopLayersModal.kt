package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.desktop.theme.LocalThemeSpec
import com.example.shared.model.LayerEntity

@Composable
fun DesktopLayersModal(
    layers: List<LayerEntity>,
    activeLayerId: String,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onUpdateOpacity: (String, Float) -> Unit,
    onMoveLayerUp: (Int) -> Unit,
    onMoveLayerDown: (Int) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(420.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Icon(Icons.Default.Layers, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Керування шарами", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = themeSpec.colorScheme.onSurface)
                        Badge(containerColor = themeSpec.accentColor.copy(alpha = 0.2f)) {
                            Text("${layers.size}", color = themeSpec.accentColor, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onAddLayer, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Додати шар", tint = themeSpec.accentColor)
                        }
                        IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider()

                // Layers List (Rendered Top-Down)
                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(layers.reversed()) { revIdx, layer ->
                        val originalIdx = layers.size - 1 - revIdx
                        val isActive = layer.id == activeLayerId

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) themeSpec.accentColor.copy(alpha = 0.12f) else themeSpec.colorScheme.surfaceContainer,
                            border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, themeSpec.accentColor) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLayer(layer.id) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Layer Name & Elements count
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = layer.name,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = themeSpec.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "(${layer.totalElements} ел.)",
                                            fontSize = 11.sp,
                                            color = themeSpec.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Action buttons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Visibility
                                        IconButton(
                                            onClick = { onToggleVisibility(layer.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Видимість",
                                                tint = if (layer.isVisible) themeSpec.accentColor else themeSpec.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Move Up
                                        IconButton(
                                            onClick = { onMoveLayerUp(originalIdx) },
                                            enabled = originalIdx < layers.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Вгору", modifier = Modifier.size(15.dp))
                                        }

                                        // Move Down
                                        IconButton(
                                            onClick = { onMoveLayerDown(originalIdx) },
                                            enabled = originalIdx > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз", modifier = Modifier.size(15.dp))
                                        }

                                        // Delete Layer
                                        if (layers.size > 1) {
                                            IconButton(
                                                onClick = { onDeleteLayer(layer.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Видалити", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Opacity Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Прозорість:", fontSize = 10.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = layer.opacity,
                                        onValueChange = { onUpdateOpacity(layer.id, it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f).height(18.dp),
                                        colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
                                    )
                                    Text("${(layer.opacity * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeSpec.accentColor)
                                }
                            }
                        }
                    }
                }

                // Add Layer button
                Button(
                    onClick = onAddLayer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Створити новий шар", color = Color.White)
                }
            }
        }
    }
}
