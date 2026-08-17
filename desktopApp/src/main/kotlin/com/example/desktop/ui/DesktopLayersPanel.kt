package com.example.desktop.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.DesktopViewModel
import com.example.desktop.theme.LocalThemeSpec
import com.example.shared.model.BlendMode
import com.example.shared.model.LayerEntity

@Composable
fun DesktopLayersPanel(
    viewModel: DesktopViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    val currentPage = pages.getOrElse(currentPageIndex) { pages.first() }
    val layers = currentPage.getEffectiveLayers()

    Surface(
        modifier = modifier
            .width(280.dp)
            .border(1.dp, themeSpec.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = themeSpec.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Шари проекту", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрити", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(layers.reversed()) { layer ->
                    LayerRow(
                        layer = layer,
                        isActive = layer.id == activeLayerId,
                        onSelect = { viewModel.setActiveLayer(layer.id) },
                        onToggleVisibility = { viewModel.toggleLayerVisibility(layer.id) },
                        onOpacityChange = { viewModel.setLayerOpacity(layer.id, it) },
                        onBlendModeChange = { viewModel.setLayerBlendMode(layer.id, it) },
                        onDelete = { viewModel.deleteLayer(layer.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.addLayer() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Додати шар", color = Color.Black, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LayerRow(
    layer: LayerEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (BlendMode) -> Unit,
    onDelete: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var showBlendMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                if (isActive) 1.5.dp else 1.dp,
                if (isActive) themeSpec.accentColor else themeSpec.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) themeSpec.colorScheme.surfaceContainerHighest else themeSpec.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Видимість",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(layer.name, fontSize = 13.sp, fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else null)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Text(
                            layer.blendMode.name.take(4),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable { showBlendMenu = true }
                                .padding(4.dp),
                            color = themeSpec.accentColor
                        )
                        DropdownMenu(expanded = showBlendMenu, onDismissRequest = { showBlendMenu = false }) {
                            BlendMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name) },
                                    onClick = { onBlendModeChange(mode); showBlendMenu = false }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Видалити", modifier = Modifier.size(14.dp), tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }

            // Opacity slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Alpha", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Slider(
                    value = layer.opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.height(16.dp)
                )
            }
        }
    }
}
