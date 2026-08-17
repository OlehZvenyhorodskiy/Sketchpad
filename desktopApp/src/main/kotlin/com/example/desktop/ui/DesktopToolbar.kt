package com.example.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.DesktopViewModel
import com.example.desktop.theme.AppThemeStyle
import com.example.desktop.theme.LocalThemeSpec
import com.example.shared.model.HslaColor
import com.example.shared.model.SymmetryMode
import com.example.shared.model.ToolType

@Composable
fun DesktopToolbar(
    viewModel: DesktopViewModel,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val currentTool by viewModel.currentTool.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()
    val brushOpacity by viewModel.brushOpacity.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val symmetryMode by viewModel.symmetryMode.collectAsState()
    val connectedClients by viewModel.sketchLinkServer.connectedClientsCount.collectAsState()
    val whiteCanvasMode by viewModel.whiteCanvasMode.collectAsState()

    var showThemeMenu by remember { mutableStateOf(false) }
    var showSymmetryMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .padding(12.dp)
            .border(1.dp, themeSpec.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = themeSpec.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Undo / Redo
            IconButton(onClick = { viewModel.undo() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = themeSpec.colorScheme.onSurface)
            }
            IconButton(onClick = { viewModel.redo() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = themeSpec.colorScheme.onSurface)
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Tools: Pen, Pencil, Fountain Pen, Marker, Eraser, Selector, Ruler
            ToolButton(Icons.Default.Edit, "Перо (B)", currentTool == ToolType.PEN) {
                viewModel.selectTool(ToolType.PEN)
            }
            ToolButton(Icons.Default.Brush, "Олівець (P)", currentTool == ToolType.PENCIL) {
                viewModel.selectTool(ToolType.PENCIL)
            }
            ToolButton(Icons.Default.Gesture, "Каліграфія", currentTool == ToolType.FOUNTAIN_PEN) {
                viewModel.selectTool(ToolType.FOUNTAIN_PEN)
            }
            ToolButton(Icons.Default.Highlight, "Маркер (M)", currentTool == ToolType.MARKER) {
                viewModel.selectTool(ToolType.MARKER)
            }
            ToolButton(Icons.Default.AutoFixNormal, "Ластик (E)", currentTool == ToolType.ERASER) {
                viewModel.selectTool(ToolType.ERASER)
            }
            ToolButton(Icons.Default.CropFree, "Виділення (S)", currentTool == ToolType.SELECTOR) {
                viewModel.selectTool(ToolType.SELECTOR)
            }
            ToolButton(Icons.Default.Straighten, "Лінійка (R)", currentTool == ToolType.RULER) {
                viewModel.selectTool(ToolType.RULER)
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Brush Size & Opacity Sliders
            Column(modifier = Modifier.width(100.dp)) {
                Text("Розмір: ${brushSize.toInt()}px", fontSize = 10.sp, color = themeSpec.colorScheme.onSurface)
                Slider(
                    value = brushSize,
                    onValueChange = { viewModel.setBrushSize(it) },
                    valueRange = 1f..60f,
                    modifier = Modifier.height(18.dp)
                )
            }

            Column(modifier = Modifier.width(90.dp)) {
                Text("Прозорість: ${(brushOpacity * 100).toInt()}%", fontSize = 10.sp, color = themeSpec.colorScheme.onSurface)
                Slider(
                    value = brushOpacity,
                    onValueChange = { viewModel.setBrushOpacity(it) },
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.height(18.dp)
                )
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Recent Color Swatches
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                recentColors.take(5).forEach { color ->
                    val isSelected = currentColor == color
                    val rgb = color.toArgbInt()
                    val composeColor = Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(composeColor)
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color.White else Color.Gray.copy(alpha = 0.5f), CircleShape)
                            .clickable { viewModel.setColor(color) }
                    )
                }
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Symmetry Dropdown
            Box {
                IconButton(
                    onClick = { showSymmetryMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Flip,
                        contentDescription = "Симетрія",
                        tint = if (symmetryMode != SymmetryMode.NONE) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                    )
                }
                DropdownMenu(expanded = showSymmetryMenu, onDismissRequest = { showSymmetryMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Без симетрії") },
                        onClick = { viewModel.setSymmetryMode(SymmetryMode.NONE); showSymmetryMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Вертикальна вісь (Y)") },
                        onClick = { viewModel.setSymmetryMode(SymmetryMode.VERTICAL); showSymmetryMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Горизонтальна вісь (X)") },
                        onClick = { viewModel.setSymmetryMode(SymmetryMode.HORIZONTAL); showSymmetryMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Квадро-симетрія (4 осі)") },
                        onClick = { viewModel.setSymmetryMode(SymmetryMode.QUAD); showSymmetryMenu = false }
                    )
                }
            }

            // Layers Panel Button
            IconButton(
                onClick = { viewModel.toggleLayersPanel() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Шари", tint = themeSpec.colorScheme.onSurface)
            }

            // Performance Monitor Overlay Toggle
            IconButton(
                onClick = { viewModel.togglePerformanceOverlay() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = "Продуктивність", tint = themeSpec.colorScheme.onSurface)
            }

            // SketchLink Tablet Pairing Button
            FilledTonalButton(
                onClick = { viewModel.togglePairingDialog() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (connectedClients > 0) Color(0xFF16A34A).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Icon(
                    Icons.Default.TabletAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (connectedClients > 0) Color(0xFF16A34A) else themeSpec.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (connectedClients > 0) "$connectedClients планшет(и)" else "SketchLink",
                    fontSize = 11.sp,
                    color = if (connectedClients > 0) Color(0xFF16A34A) else themeSpec.colorScheme.onSurface
                )
            }

            // White Canvas Mode Toggle
            IconButton(
                onClick = { viewModel.toggleWhiteCanvasMode() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "White Canvas Mode (F11)",
                    tint = if (whiteCanvasMode) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                )
            }

            // Themes Dropdown
            Box {
                IconButton(onClick = { showThemeMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = "Теми", tint = themeSpec.colorScheme.onSurface)
                }
                DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                    AppThemeStyle.values().forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                viewModel.setTheme(style)
                                showThemeMenu = false
                            }
                        )
                    }
                }
            }

            // Export Button
            FilledIconButton(
                onClick = onExportClick,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeSpec.accentColor)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Експорт", tint = Color.Black)
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    tooltip: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) themeSpec.accentColor.copy(alpha = 0.25f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, themeSpec.accentColor) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = tooltip,
                tint = if (isSelected) themeSpec.accentColor else themeSpec.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
