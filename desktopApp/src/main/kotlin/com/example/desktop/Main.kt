package com.example.desktop

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.desktop.export.DesktopExportManager
import com.example.desktop.input.DesktopShortcutManager
import com.example.desktop.theme.SketchpadDesktopTheme
import com.example.desktop.ui.*
import com.example.shared.model.ToolType
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)
    val viewModel = remember { DesktopViewModel() }
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val showLayersPanel by viewModel.showLayersPanel.collectAsState()
    val showPairingDialog by viewModel.showPairingDialog.collectAsState()
    val showPerformanceOverlay by viewModel.showPerformanceOverlay.collectAsState()
    val whiteCanvasMode by viewModel.whiteCanvasMode.collectAsState()

    val shortcutManager = remember(viewModel) {
        DesktopShortcutManager(
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onToolSelected = { viewModel.selectTool(it) },
            onAdjustBrushSize = { delta -> viewModel.setBrushSize(viewModel.brushSize.value + delta) },
            onSetOpacity = { opacity -> viewModel.setBrushOpacity(opacity) },
            onToggleWhiteCanvas = { viewModel.toggleWhiteCanvasMode() },
            onSave = {
                val file = File(System.getProperty("user.home"), "Desktop/drawing.png")
                DesktopExportManager.exportToRaster(viewModel.currentPage, file, "PNG")
            },
            onNewPage = { viewModel.addNewPage() }
        )
    }

    Window(
        onCloseRequest = {
            viewModel.onDispose()
            exitApplication()
        },
        title = "Sketchpad Pro - Tablet & Desktop Drawing Canvas (v2.0.0)",
        state = windowState
    ) {
        SketchpadDesktopTheme(style = currentTheme, isDark = isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { shortcutManager.handleKeyEvent(it) }
            ) {
                // 1. Main Canvas
                DesktopCanvasView(viewModel = viewModel, modifier = Modifier.fillMaxSize())

                // 2. Floating Adaptive Toolbar (Fade out on White Canvas Mode)
                AnimatedVisibility(
                    visible = !whiteCanvasMode,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    DesktopToolbar(
                        viewModel = viewModel,
                        onExportClick = {
                            val chooser = JFileChooser().apply {
                                dialogTitle = "Експортувати малюнок"
                                fileFilter = FileNameExtensionFilter("PNG Image (*.png)", "png")
                                addChoosableFileFilter(FileNameExtensionFilter("PDF Document (*.pdf)", "pdf"))
                                addChoosableFileFilter(FileNameExtensionFilter("SVG Vector (*.svg)", "svg"))
                                addChoosableFileFilter(FileNameExtensionFilter("Sketchpad Project (*.sketchpad)", "sketchpad"))
                            }
                            val res = chooser.showSaveDialog(null)
                            if (res == JFileChooser.APPROVE_OPTION) {
                                val selectedFile = chooser.selectedFile
                                when (chooser.fileFilter.description) {
                                    "PDF Document (*.pdf)" -> {
                                        val f = if (selectedFile.name.endsWith(".pdf")) selectedFile else File("${selectedFile.absolutePath}.pdf")
                                        DesktopExportManager.exportToPdf(viewModel.pages.value, f)
                                    }
                                    "SVG Vector (*.svg)" -> {
                                        val f = if (selectedFile.name.endsWith(".svg")) selectedFile else File("${selectedFile.absolutePath}.svg")
                                        DesktopExportManager.exportToSvg(viewModel.currentPage, f)
                                    }
                                    "Sketchpad Project (*.sketchpad)" -> {
                                        val f = if (selectedFile.name.endsWith(".sketchpad")) selectedFile else File("${selectedFile.absolutePath}.sketchpad")
                                        DesktopExportManager.exportToSketchpadProject(viewModel.canvas.value, viewModel.pages.value, f)
                                    }
                                    else -> {
                                        val f = if (selectedFile.name.endsWith(".png")) selectedFile else File("${selectedFile.absolutePath}.png")
                                        DesktopExportManager.exportToRaster(viewModel.currentPage, f, "PNG")
                                    }
                                }
                            }
                        }
                    )
                }

                // 3. Layers Panel (Right Side)
                AnimatedVisibility(
                    visible = showLayersPanel && !whiteCanvasMode,
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp)
                ) {
                    DesktopLayersPanel(
                        viewModel = viewModel,
                        onClose = { viewModel.toggleLayersPanel() }
                    )
                }

                // 4. Performance HUD Overlay (Top-Left)
                if (showPerformanceOverlay) {
                    DesktopPerformanceOverlay(
                        viewModel = viewModel,
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 80.dp)
                    )
                }

                // 5. Tablet Pairing Dialog
                if (showPairingDialog) {
                    DesktopPairingDialog(
                        viewModel = viewModel,
                        onClose = { viewModel.togglePairingDialog() }
                    )
                }
            }
        }
    }
}
