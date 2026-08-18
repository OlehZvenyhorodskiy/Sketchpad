package com.example.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.DesktopViewModel
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.theme.ThemedPanel
import com.example.shared.model.ToolType
import java.io.File

@Composable
fun DesktopCanvasEditorScreen(
    viewModel: DesktopViewModel,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current

    // State collections
    val currentTool by viewModel.currentTool.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()
    val brushOpacity by viewModel.brushOpacity.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val eraserMode by viewModel.eraserMode.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val useVerticalSliders by viewModel.useVerticalSliders.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    val rulerState by viewModel.rulerState.collectAsState()
    val isProtractorVisible by viewModel.isProtractorVisible.collectAsState()
    val protractorCenter by viewModel.protractorCenter.collectAsState()
    val whiteCanvasMode by viewModel.whiteCanvasMode.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // Modals
    val showTopMenuModal by viewModel.showTopMenuModal.collectAsState()
    val showInsertModal by viewModel.showInsertModal.collectAsState()
    val showLayersModal by viewModel.showLayersModal.collectAsState()
    val showColorPickerModal by viewModel.showColorPickerModal.collectAsState()
    val showPageStripModal by viewModel.showPageStripModal.collectAsState()
    val showTimelineModal by viewModel.showTimelineModal.collectAsState()
    val showCodeLabDialog by viewModel.showCodeLabDialog.collectAsState()
    val showStudyDeckDialog by viewModel.showStudyDeckDialog.collectAsState()
    val showTextInputDialog by viewModel.showTextInputDialog.collectAsState()
    val showAiWindow by viewModel.showAiWindow.collectAsState()
    val showAiProviderModal by viewModel.showAiProviderModal.collectAsState()
    val showAudioModal by viewModel.showAudioManagementModal.collectAsState()
    val showExitDialog by viewModel.showExitProtectionDialog.collectAsState()

    // Audio & AI state
    val isRecordingAudio by viewModel.audioRecorderManager.isRecording.collectAsState()
    val isPlayingAudio by viewModel.audioRecorderManager.isPlaying.collectAsState()
    val audioAmplitudes by viewModel.audioRecorderManager.currentAmplitudes.collectAsState()
    val playbackProgress by viewModel.audioRecorderManager.playbackProgress.collectAsState()
    val audioRecordings by viewModel.audioRecordings.collectAsState()
    val aiMessages by viewModel.aiMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()

    var isDrawingActive by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Canvas
        DesktopCanvasView(
            viewModel = viewModel,
            isDrawingActive = { isDrawingActive = it },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Bar & Floating Toolbar (Fade out in White Canvas mode)
        AnimatedVisibility(
            visible = !whiteCanvasMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            DesktopTopFloatingToolbar(
                currentTool = currentTool,
                onToolSelected = { viewModel.selectTool(it) },
                strokeWidth = brushSize,
                onStrokeWidthChange = { viewModel.setBrushSize(it) },
                strokeOpacity = brushOpacity,
                onStrokeOpacityChange = { viewModel.setBrushOpacity(it) },
                currentColor = currentColor,
                onOpenColorPicker = { viewModel.setShowColorPickerModal(true) },
                eraserMode = eraserMode,
                onToggleEraserMode = { viewModel.toggleEraserMode() },
                selectionMode = selectionMode,
                onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                useVerticalSliders = useVerticalSliders,
                onToggleOrientation = { viewModel.toggleOrientation() },
                isDrawing = isDrawingActive
            )
        }

        // 3. Top Right Actions HUD
        AnimatedVisibility(
            visible = !whiteCanvasMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            ThemedPanel(surfaceAlpha = 0.94f) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Undo
                    IconButton(onClick = { viewModel.undo() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = themeSpec.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                    // Redo
                    IconButton(onClick = { viewModel.redo() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = themeSpec.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp))

                    // Audio Record / Waveform HUD
                    IconButton(
                        onClick = {
                            if (isRecordingAudio) {
                                val outFile = File(System.getProperty("user.home"), ".sketchpad/audio/rec_${System.currentTimeMillis()}.wav")
                                viewModel.audioRecorderManager.stopRecording(outFile)
                            } else {
                                viewModel.audioRecorderManager.startRecording()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecordingAudio) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                            contentDescription = "Аудіозапис",
                            tint = if (isRecordingAudio) Color(0xFFEF4444) else themeSpec.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Audio List
                    IconButton(onClick = { viewModel.setShowAudioManagementModal(true) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Список аудіо", tint = themeSpec.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }

                    // Layers
                    IconButton(onClick = { viewModel.setShowLayersModal(true) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Layers, contentDescription = "Шари", tint = themeSpec.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }

                    // Canvas Settings (TopMenu)
                    IconButton(onClick = { viewModel.setShowTopMenuModal(true) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Налаштування полотна", tint = themeSpec.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 4. Vertical Side Panels (When enabled)
        if (useVerticalSliders && !whiteCanvasMode) {
            DesktopVerticalSidePanel(
                panelType = SidePanelType.WIDTH,
                currentValue = brushSize,
                onValueChange = { viewModel.setBrushSize(it) },
                currentColor = currentColor,
                currentTool = currentTool,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            DesktopVerticalSidePanel(
                panelType = SidePanelType.OPACITY,
                currentValue = brushOpacity,
                onValueChange = { viewModel.setBrushOpacity(it) },
                currentColor = currentColor,
                currentTool = currentTool,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        } else if (!whiteCanvasMode) {
            // Right Side Expandable Brush Tool Panel
            DesktopRightSideToolPanel(
                strokeWidth = brushSize,
                onStrokeWidthChange = { viewModel.setBrushSize(it) },
                strokeOpacity = brushOpacity,
                onStrokeOpacityChange = { viewModel.setBrushOpacity(it) },
                currentColor = currentColor,
                onColorSelected = { viewModel.setColor(it) },
                onOpenFullPalette = { viewModel.setShowColorPickerModal(true) },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // 5. Ruler & Protractor Overlays
        if (rulerState.isVisible) {
            DesktopRulerOverlay(
                center = rulerState.center,
                angleRad = rulerState.angleRad,
                length = rulerState.length,
                width = rulerState.width,
                onMove = { viewModel.updateRuler(center = it) },
                onRotate = { viewModel.updateRuler(angleRad = it) },
                onClose = { viewModel.updateRuler(isVisible = false) }
            )
        }

        if (isProtractorVisible) {
            DesktopProtractorOverlay(
                center = protractorCenter,
                onMove = { viewModel.updateProtractorCenter(it) },
                onClose = { viewModel.toggleProtractor() }
            )
        }

        // 6. Bottom Left Overlay (Page & Zoom)
        AnimatedVisibility(
            visible = !whiteCanvasMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            DesktopBottomLeftOverlay(
                currentPageIndex = currentPageIndex,
                totalPages = pages.size,
                zoomScale = zoomScale,
                onPageIndicatorClick = { viewModel.setShowPageStripModal(true) },
                onZoomClick = { viewModel.cycleZoom() }
            )
        }

        // 7. Bottom Center "➕ ДОДАТИ" Button
        AnimatedVisibility(
            visible = !whiteCanvasMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
            Button(
                onClick = { viewModel.setShowInsertModal(true) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ДОДАТИ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
        }

        // 8. Bottom Right Floating AI FAB
        AnimatedVisibility(
            visible = !whiteCanvasMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.setShowAiWindow(true) },
                containerColor = themeSpec.accentColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(24.dp))
            }
        }

        // 9. Floating AI Assistant Window
        if (showAiWindow) {
            DesktopFloatingAiWindow(
                messages = aiMessages,
                onSendMessage = { prompt, vision -> viewModel.sendAiMessage(prompt, vision) },
                onOpenSettings = { viewModel.setShowAiProviderModal(true) },
                onClose = { viewModel.setShowAiWindow(false) },
                isLoading = isAiLoading
            )
        }

        // 10. Modals & Dialogs
        if (showTopMenuModal) {
            DesktopCanvasTopMenuModal(
                currentBackgroundColor = viewModel.canvas.value.backgroundColor,
                onBackgroundColorChange = { viewModel.setPageBackgroundColor(it) },
                currentPattern = viewModel.currentPage.backgroundPattern,
                onPatternChange = { viewModel.setPageBackgroundPattern(it) },
                currentPreset = viewModel.canvas.value.pageSizePreset,
                onPresetChange = { viewModel.setPageSizePreset(it) },
                currentThemeStyle = currentTheme,
                onThemeStyleChange = { viewModel.setTheme(it) },
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                onDismissRequest = { viewModel.setShowTopMenuModal(false) }
            )
        }

        if (showInsertModal) {
            DesktopInsertMenuModal(
                onInsertImage = { /* Image insert */ },
                onInsertText = { viewModel.setShowTextInputDialog(true) },
                onInsertChart = { viewModel.insertChart() },
                onInsertShape = { viewModel.insertShape(it) },
                onPasteClipboard = { /* Paste */ },
                onVectorize = { /* Auto-recognize */ },
                onPlotFunction = { viewModel.insertChart("f(x) = x² - 4") },
                onLatexOcr = { /* LaTeX OCR */ },
                onOpenCodeLab = { viewModel.setShowCodeLabDialog(true) },
                onOpenStudyDeck = { viewModel.setShowStudyDeckDialog(true) },
                onAiSummary = {
                    viewModel.setShowAiWindow(true)
                    viewModel.sendAiMessage("Зроби стислий структурований конспект поточної сторінки.", false)
                },
                onAiQuiz = {
                    viewModel.setShowAiWindow(true)
                    viewModel.sendAiMessage("Створи тест на 5 запитань з варіантами відповідей по темі цієї нотатки.", false)
                },
                onFindLinks = {
                    viewModel.setShowAiWindow(true)
                    viewModel.sendAiMessage("Знайди ключові поняття для створення взаємних посилань (Obsidian-style).", false)
                },
                onDismissRequest = { viewModel.setShowInsertModal(false) }
            )
        }

        if (showLayersModal) {
            DesktopLayersModal(
                layers = viewModel.currentPage.getEffectiveLayers(),
                activeLayerId = activeLayerId,
                onSelectLayer = { viewModel.selectLayer(it) },
                onAddLayer = { viewModel.addLayer() },
                onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                onUpdateOpacity = { id, op -> viewModel.updateLayerOpacity(id, op) },
                onMoveLayerUp = { viewModel.moveLayerUp(it) },
                onMoveLayerDown = { viewModel.moveLayerDown(it) },
                onRenameLayer = { id, name -> viewModel.renameLayer(id, name) },
                onDeleteLayer = { viewModel.deleteLayer(it) },
                onDismissRequest = { viewModel.setShowLayersModal(false) }
            )
        }

        if (showColorPickerModal) {
            DesktopColorPickerModal(
                initialColor = currentColor,
                recentColors = recentColors,
                onColorSelected = { viewModel.setColor(it) },
                onDismissRequest = { viewModel.setShowColorPickerModal(false) }
            )
        }

        if (showPageStripModal) {
            DesktopPageStripModal(
                pages = pages,
                currentPageIndex = currentPageIndex,
                onSelectPage = { viewModel.selectPage(it) },
                onAddPage = { viewModel.addNewPage() },
                onDeletePage = { viewModel.deletePage(it) },
                onDismissRequest = { viewModel.setShowPageStripModal(false) }
            )
        }

        if (showTimelineModal) {
            DesktopTimelineSliderModal(
                totalVersions = 10,
                currentVersionIndex = 0,
                onVersionChanged = {},
                onRestoreVersion = {},
                onDismissRequest = { viewModel.setShowTimelineModal(false) }
            )
        }

        if (showCodeLabDialog) {
            DesktopCodeLabDialog(
                onInsertCodeBlock = { viewModel.insertCodeBlock(it) },
                onDismissRequest = { viewModel.setShowCodeLabDialog(false) }
            )
        }

        if (showStudyDeckDialog) {
            DesktopStudyDeckDialog(
                cards = flashcards,
                onSaveCard = { viewModel.saveFlashcard(it) },
                onDismissRequest = { viewModel.setShowStudyDeckDialog(false) }
            )
        }

        if (showTextInputDialog) {
            DesktopTextInputDialog(
                onConfirm = { viewModel.insertTextBlock(it) },
                onDismissRequest = { viewModel.setShowTextInputDialog(false) }
            )
        }

        if (showAiProviderModal) {
            DesktopAiProviderPickerModal(
                preferences = viewModel.aiPreferences,
                onDismissRequest = { viewModel.setShowAiProviderModal(false) }
            )
        }

        if (showAudioModal) {
            DesktopAudioManagementModal(
                recordings = audioRecordings,
                isPlaying = isPlayingAudio,
                playingProgress = playbackProgress,
                onPlayRecording = { rec ->
                    viewModel.audioRecorderManager.startPlayback(File(rec.filePath))
                },
                onStopRecording = { viewModel.audioRecorderManager.stopPlayback() },
                onDeleteRecording = {},
                onDismissRequest = { viewModel.setShowAudioManagementModal(false) }
            )
        }
    }
}
