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
import androidx.compose.ui.window.*
import com.example.desktop.export.DesktopExportManager
import com.example.desktop.input.DesktopShortcutManager
import com.example.desktop.theme.DesktopThemeSpecs
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.ui.DesktopCanvasEditorScreen
import com.example.desktop.ui.ExitProtectionDialog
import com.example.desktop.ui.LoadingScreen
import com.example.shared.model.ToolType
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

fun main() = application {
    val windowState = rememberWindowState(width = 1380.dp, height = 880.dp)
    val viewModel = remember { DesktopViewModel() }
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val showExitDialog by viewModel.showExitProtectionDialog.collectAsState()

    var isLoaded by remember { mutableStateOf(false) }

    val shortcutManager = remember(viewModel) {
        DesktopShortcutManager(
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onToolSelected = { viewModel.selectTool(it) },
            onAdjustBrushSize = { delta -> viewModel.setBrushSize(viewModel.brushSize.value + delta) },
            onSetOpacity = { opacity -> viewModel.setBrushOpacity(opacity) },
            onToggleWhiteCanvas = { viewModel.toggleWhiteCanvasMode() },
            onSave = { viewModel.saveProject() },
            onNewPage = { viewModel.addNewPage() }
        )
    }

    val themeSpec = remember(currentTheme, isDark) {
        DesktopThemeSpecs.forStyle(currentTheme, isDark = isDark)
    }

    Window(
        onCloseRequest = {
            if (hasUnsavedChanges) {
                viewModel.setShowExitProtectionDialog(true)
            } else {
                viewModel.onDispose()
                exitApplication()
            }
        },
        title = "Sketchpad Pro - Tablet & Desktop Drawing Canvas (v2.0.0)",
        state = windowState
    ) {
        // Native Windows Menu Bar
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item("New Page", onClick = { viewModel.addNewPage() })
                Item("Open...", onClick = {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Відкрити проект Sketchpad"
                        fileFilter = FileNameExtensionFilter("Sketchpad Project (*.sketchpad)", "sketchpad")
                    }
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        // Open project
                    }
                })
                Item("Save", onClick = { viewModel.saveProject() })
                Item("Save As...", onClick = {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Зберегти проект як"
                        fileFilter = FileNameExtensionFilter("Sketchpad Project (*.sketchpad)", "sketchpad")
                    }
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        val f = chooser.selectedFile
                        val target = if (f.name.endsWith(".sketchpad")) f else File("${f.absolutePath}.sketchpad")
                        DesktopExportManager.exportToSketchpadProject(viewModel.canvas.value, viewModel.pages.value, target)
                    }
                })
                Separator()
                Item("Export as PNG...", onClick = {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Експорт PNG"
                        fileFilter = FileNameExtensionFilter("PNG Image (*.png)", "png")
                    }
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        val f = chooser.selectedFile
                        val target = if (f.name.endsWith(".png")) f else File("${f.absolutePath}.png")
                        DesktopExportManager.exportToRaster(viewModel.currentPage, target, "PNG")
                    }
                })
                Item("Export as SVG...", onClick = {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Експорт SVG"
                        fileFilter = FileNameExtensionFilter("SVG Vector (*.svg)", "svg")
                    }
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        val f = chooser.selectedFile
                        val target = if (f.name.endsWith(".svg")) f else File("${f.absolutePath}.svg")
                        DesktopExportManager.exportToSvg(viewModel.currentPage, target)
                    }
                })
                Item("Export as PDF...", onClick = {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Експорт PDF"
                        fileFilter = FileNameExtensionFilter("PDF Document (*.pdf)", "pdf")
                    }
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        val f = chooser.selectedFile
                        val target = if (f.name.endsWith(".pdf")) f else File("${f.absolutePath}.pdf")
                        DesktopExportManager.exportToPdf(viewModel.pages.value, target)
                    }
                })
                Separator()
                Item("Exit", onClick = {
                    if (hasUnsavedChanges) {
                        viewModel.setShowExitProtectionDialog(true)
                    } else {
                        viewModel.onDispose()
                        exitApplication()
                    }
                })
            }

            Menu("Edit", mnemonic = 'E') {
                Item("Undo", onClick = { viewModel.undo() })
                Item("Redo", onClick = { viewModel.redo() })
                Separator()
                Item("Clear Canvas", onClick = { viewModel.clearCurrentPage() })
            }

            Menu("View", mnemonic = 'V') {
                Item("Zoom In (+25%)", onClick = { viewModel.setZoomScale(viewModel.zoomScale.value + 0.25f) })
                Item("Zoom Out (-25%)", onClick = { viewModel.setZoomScale(viewModel.zoomScale.value - 0.25f) })
                Item("Reset Zoom (100%)", onClick = { viewModel.setZoomScale(1.0f) })
                Separator()
                Item("Toggle White Canvas Mode (F11)", onClick = { viewModel.toggleWhiteCanvasMode() })
                Item("Toggle Dark / Light Theme", onClick = { viewModel.toggleDarkTheme() })
            }

            Menu("Tools", mnemonic = 'T') {
                Item("Pen (B)", onClick = { viewModel.selectTool(ToolType.PEN) })
                Item("Pencil (P)", onClick = { viewModel.selectTool(ToolType.PENCIL) })
                Item("Calligraphy", onClick = { viewModel.selectTool(ToolType.FOUNTAIN_PEN) })
                Item("Marker (M)", onClick = { viewModel.selectTool(ToolType.MARKER) })
                Item("Eraser (E)", onClick = { viewModel.selectTool(ToolType.ERASER) })
                Item("Selector (S)", onClick = { viewModel.selectTool(ToolType.SELECTOR) })
                Item("Ruler (R)", onClick = { viewModel.selectTool(ToolType.RULER) })
                Item("Text Block", onClick = { viewModel.selectTool(ToolType.TEXT) })
            }

            Menu("Layers", mnemonic = 'L') {
                Item("New Layer", onClick = { viewModel.addLayer() })
                Item("Manage Layers...", onClick = { viewModel.setShowLayersModal(true) })
            }

            Menu("Academic", mnemonic = 'A') {
                Item("Code Lab (Python/C/C++)...", onClick = { viewModel.setShowCodeLabDialog(true) })
                Item("Study Cards SM-2...", onClick = { viewModel.setShowStudyDeckDialog(true) })
                Item("Insert Math Chart...", onClick = { viewModel.insertChart() })
            }

            Menu("AI", mnemonic = 'I') {
                Item("Open AI Assistant Window", onClick = { viewModel.setShowAiWindow(true) })
                Item("Configure AI Providers & Keys...", onClick = { viewModel.setShowAiProviderModal(true) })
            }

            Menu("Help", mnemonic = 'H') {
                Item("About Sketchpad Pro", onClick = {
                    JOptionPane.showMessageDialog(
                        null,
                        "Sketchpad Pro v2.0.0\nDesktop & Tablet Canvas for Windows 10/11\nCreated by Oleh Zvenyhorodskiy",
                        "About Sketchpad Pro",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                })
            }
        }

        CompositionLocalProvider(LocalThemeSpec provides themeSpec) {
            MaterialTheme(colorScheme = themeSpec.colorScheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { shortcutManager.handleKeyEvent(it) }
                ) {
                    if (!isLoaded) {
                        LoadingScreen(
                            onLoaded = { isLoaded = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DesktopCanvasEditorScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Exit Protection Confirmation Dialog
                    if (showExitDialog) {
                        ExitProtectionDialog(
                            onSaveAndExit = {
                                viewModel.saveProject()
                                viewModel.setShowExitProtectionDialog(false)
                                viewModel.onDispose()
                                exitApplication()
                            },
                            onExitWithoutSaving = {
                                viewModel.setShowExitProtectionDialog(false)
                                viewModel.onDispose()
                                exitApplication()
                            },
                            onCancel = {
                                viewModel.setShowExitProtectionDialog(false)
                            }
                        )
                    }
                }
            }
        }
    }
}
