package com.example.ui.editor

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import com.example.ui.components.PanelType
import com.example.ui.components.VerticalFloatingSidePanel
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import com.example.ui.components.AudioManagementSheet
import com.example.ui.components.LayersBottomSheet
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.content.FileProvider
import com.example.R
import com.example.audio.RecordingStatus
import com.example.data.models.EraserMode
import com.example.data.models.CanvasReferenceCaptureSession
import com.example.data.models.CanvasReferenceDestination
import com.example.data.models.CanvasReferenceNavigationRequest
import com.example.data.models.CanvasReferenceSource
import com.example.data.models.CanvasViewport
import com.example.data.models.FlashcardEntity
import com.example.data.models.ToolType
import com.example.data.repository.StudyDeckRepository
import com.example.di.AppModule
import com.example.ui.components.BottomLeftOverlay
import com.example.ui.components.CanvasReferenceDestinationDialog
import com.example.ui.components.CanvasReferenceListDialog
import com.example.ui.components.CanvasReferenceListItem
import com.example.ui.components.CanvasReferenceNavigationEffect
import com.example.ui.components.CanvasReferenceTargetCaptureBar
import com.example.ui.components.CanvasReferenceUiText
import com.example.ui.components.CanvasTopMenuBottomSheet
import com.example.ui.components.CodeLabDialog
import com.example.ui.components.ColorPickerBottomSheet
import com.example.ui.components.FloatingAiWindow
import com.example.ui.components.GeminiChatBottomSheet
import com.example.ui.components.InsertMenuBottomSheet
import com.example.ui.components.MiniSlidersOverlay
import com.example.ui.components.PageStripBottomSheet
import com.example.ui.components.RightSideToolPanel
import com.example.ui.components.RulerOverlayComponent
import com.example.ui.components.StudyDeckDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasEditorScreen(
    viewModel: CanvasEditorViewModel,
    onBackClick: () -> Unit,
    onOpenThemeSettings: () -> Unit = {},
    referenceCaptureSession: CanvasReferenceCaptureSession? = null,
    referenceNavigationRequest: CanvasReferenceNavigationRequest? = null,
    onReferenceCaptureStarted: (CanvasReferenceCaptureSession) -> Unit = {},
    onReferenceCaptureFinished: () -> Unit = {},
    onOpenCanvasReference: (CanvasReferenceNavigationRequest) -> Unit = {},
    onReferenceNavigationConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val audioStartedText = stringResource(R.string.audio_started)
    val audioPermissionText = stringResource(R.string.audio_permission_required)
    val obsidianExportDoneText = stringResource(R.string.obsidian_export_done)
    val obsidianExportFailedText = stringResource(R.string.obsidian_export_failed)
    val obsidianFolderDeniedText = stringResource(R.string.obsidian_folder_denied)
    val linkSavedText = stringResource(R.string.link_saved)
    val linkSaveFailedText = stringResource(R.string.link_save_failed)
    val referenceUiText = CanvasReferenceUiText(
        addTitle = stringResource(R.string.link_to_note),
        selectionLabel = { count -> context.getString(R.string.selected_items_count, count) },
        searchHint = stringResource(R.string.search_sketchpads),
        noDestinations = stringResource(R.string.no_matching_pages),
        pageLabel = { index -> context.getString(R.string.page_number, index + 1) },
        pageCountLabel = { count -> context.getString(R.string.pages_count, count) },
        cancel = stringResource(R.string.cancel),
        frameTarget = stringResource(R.string.frame_destination),
        frameTargetHint = stringResource(R.string.frame_destination_hint),
        saveLink = stringResource(R.string.save_link),
        linksTitle = stringResource(R.string.linked_notes),
        noLinks = stringResource(R.string.no_links),
        open = stringResource(R.string.open),
        delete = stringResource(R.string.delete)
    )
    val coroutineScope = rememberCoroutineScope()
    val referenceRepository = remember(context) {
        AppModule.provideCanvasReferenceRepository(context)
    }
    val studyDeckRepository = remember(context) { StudyDeckRepository(context) }
    val canvas by viewModel.canvas.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val currentLayers = remember(pages, currentPageIndex) {
        pages.getOrNull(currentPageIndex)?.getEffectiveLayers() ?: emptyList()
    }
    val currentTool by viewModel.currentTool.collectAsState()
    val eraserMode by viewModel.eraserMode.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val strokeOpacity by viewModel.strokeOpacity.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedElementIds by viewModel.selectedElementIds.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val drawWithFingers by viewModel.drawWithFingers.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val rulerState by viewModel.rulerState.collectAsState()
    val audioStatus by viewModel.audioStatus.collectAsState()
    val audioRecordings by viewModel.audioRecordings.collectAsState()
    val latestRecording = audioRecordings.firstOrNull()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isAiWindowVisible by viewModel.isAiWindowVisible.collectAsState()
    val currentPage = pages.getOrNull(currentPageIndex)

    val showLayersPanel by viewModel.showLayersPanel.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()

    // Bottom sheets state
    var showTopMenuSheet by remember { mutableStateOf(false) }
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showInsertSheet by remember { mutableStateOf(false) }
    var showPageStripSheet by remember { mutableStateOf(false) }
    var showGeminiSheet by remember { mutableStateOf(false) }
    var showProviderPicker by remember { mutableStateOf(false) }
    val selectedProviderId by viewModel.selectedProviderId.collectAsState()
    val providerDisplayName = remember(selectedProviderId) {
        com.example.ai.AiProviderRegistry.getProvider(selectedProviderId).displayName
    }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showAudioPill by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputVal by remember { mutableStateOf("") }
    var showMathFunctionDialog by remember { mutableStateOf(false) }
    var mathFormulaVal by remember { mutableStateOf("sin(x)") }
    var mathXMinVal by remember { mutableStateOf("-10") }
    var mathXMaxVal by remember { mutableStateOf("10") }
    var showChartDialog by remember { mutableStateOf(false) }
    var chartWithSteps by remember { mutableStateOf(false) }
    var chartXStepVal by remember { mutableStateOf("1.0") }
    var chartYStepVal by remember { mutableStateOf("5.0") }
    var showCodeLab by remember { mutableStateOf(false) }
    var showStudyDeck by remember { mutableStateOf(false) }
    var selectedStudyDeckId by remember { mutableStateOf<String?>(null) }
    var editingCodeBlockId by remember { mutableStateOf<String?>(null) }
    var showReferenceDestination by remember { mutableStateOf(false) }
    var showReferenceList by remember { mutableStateOf(false) }
    var referenceDestinations by remember { mutableStateOf<List<CanvasReferenceDestination>>(emptyList()) }
    var referencesForSelection by remember {
        mutableStateOf<List<com.example.data.models.CanvasReferenceEntity>>(emptyList())
    }
    val studyDeckSummaries by remember(studyDeckRepository) {
        studyDeckRepository.observeDecks()
    }.collectAsState(initial = emptyList())
    val selectedStudyDeck by remember(selectedStudyDeckId, studyDeckRepository) {
        selectedStudyDeckId?.let(studyDeckRepository::observeDeck) ?: flowOf(null)
    }.collectAsState(initial = null)
    val studyCards by remember(selectedStudyDeckId, studyDeckRepository) {
        selectedStudyDeckId?.let(studyDeckRepository::observeCards) ?: flowOf(emptyList<FlashcardEntity>())
    }.collectAsState(initial = emptyList())
    val dueStudyCards by remember(selectedStudyDeckId, studyDeckRepository) {
        selectedStudyDeckId?.let(studyDeckRepository::observeDueCards) ?: flowOf(emptyList<FlashcardEntity>())
    }.collectAsState(initial = emptyList())

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Viewport dimensions for centering spawned elements
    var viewportWidthPx by remember { mutableStateOf(0f) }
    var viewportHeightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(currentPage?.id, selectedElementIds) {
        val pageId = currentPage?.id
        if (pageId == null || selectedElementIds.isEmpty()) {
            referencesForSelection = emptyList()
            return@LaunchedEffect
        }
        referenceRepository.observeOutgoingFromPage(pageId).collectLatest { references ->
            referencesForSelection = references.filter {
                it.hasExactSourceSelection(selectedElementIds)
            }
        }
    }

    LaunchedEffect(referenceCaptureSession, pages) {
        val session = referenceCaptureSession ?: return@LaunchedEffect
        if (session.destination.canvasId == canvas?.id) {
            viewModel.setCurrentPageById(session.destination.pageId)
        }
    }

    LaunchedEffect(referenceNavigationRequest, pages) {
        val request = referenceNavigationRequest ?: return@LaunchedEffect
        if (request.canvasId == canvas?.id) {
            viewModel.setCurrentPageById(request.pageId)
        }
    }

    LaunchedEffect(audioRecordings) {
        if (audioRecordings.isNotEmpty()) {
            showAudioPill = true
        }
    }

    val academicStatusMessage by viewModel.academicStatusMessage.collectAsState()
    LaunchedEffect(academicStatusMessage) {
        academicStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearAcademicStatus()
        }
    }

    val isSlidersVertical by viewModel.isSlidersVertical.collectAsState()

    // Image Picker Launcher
    val insertImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.insertImage(
                uri = it,
                viewportWidth = viewportWidthPx,
                viewportHeight = viewportHeightPx,
                panOffsetX = panOffset.x,
                panOffsetY = panOffset.y,
                scale = zoomScale
            )
        }
    }

    // Audio Permission Launcher
    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioRecording()
            Toast.makeText(context, audioStartedText, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, audioPermissionText, Toast.LENGTH_SHORT).show()
        }
    }

    val obsidianPreferences = remember(context) {
        context.getSharedPreferences("obsidian_export", android.content.Context.MODE_PRIVATE)
    }
    val obsidianVaultLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri: android.net.Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            obsidianPreferences.edit().putString("vault_uri", uri.toString()).apply()
            viewModel.exportToObsidian(uri) { result ->
                Toast.makeText(
                    context,
                    if (result.isSuccess) obsidianExportDoneText else obsidianExportFailedText,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: SecurityException) {
            Toast.makeText(context, obsidianFolderDeniedText, Toast.LENGTH_SHORT).show()
        }
    }
    val exportToObsidian = {
        val savedUri = obsidianPreferences.getString("vault_uri", null)?.let(android.net.Uri::parse)
        if (savedUri == null) {
            obsidianVaultLauncher.launch(null)
        } else {
            viewModel.exportToObsidian(savedUri) { result ->
                Toast.makeText(
                    context,
                    if (result.isSuccess) obsidianExportDoneText else obsidianExportFailedText,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        containerColor = canvas?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.background,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else if (event.isCtrlPressed) {
                when (event.key) {
                    Key.Z -> { viewModel.undo(); true }
                    Key.Y -> { viewModel.redo(); true }
                    Key.S -> { showExportDialog = true; true }
                    Key.C -> { viewModel.copySelectedElements(); true }
                    Key.V -> { viewModel.pasteElements(); true }
                    else -> false
                }
            } else if (event.key == Key.Delete || event.key == Key.Backspace) {
                viewModel.deleteSelectedElements()
                true
            } else false
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = canvas?.title ?: stringResource(R.string.canvas_fallback),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Audio Recorder Button & Waveform
                    val isRecording = audioStatus is RecordingStatus.Recording
                    if (isRecording) {
                        val recStatus = audioStatus as RecordingStatus.Recording
                        val totalSeconds = recStatus.durationMs / 1000
                        val mins = totalSeconds / 60
                        val secs = totalSeconds % 60
                        val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

                        com.example.ui.components.AudioWaveformVisualizer(
                            isRecording = true,
                            recordingTimeText = formattedTime,
                            amplitudes = recStatus.amplitudes,
                            strokeWidth = strokeWidth
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopAudioRecording()
                                Toast.makeText(context, context.getString(R.string.lecture_recording_saved), Toast.LENGTH_SHORT).show()
                            } else {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startAudioRecording()
                                    Toast.makeText(context, audioStartedText, Toast.LENGTH_SHORT).show()
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = stringResource(R.string.record_audio),
                            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Audio Recording List
                    IconButton(onClick = { showAudioSheet = true }) {
                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = stringResource(R.string.audio_notes))
                    }

                    // Layers Panel Button
                    IconButton(onClick = { viewModel.toggleLayersPanel() }) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = stringResource(R.string.layers))
                    }

                    // Undo
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }

                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }

                    // Export Share Button
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(R.string.export))
                    }

                    // Three Dots Top Menu
                    IconButton(onClick = { showTopMenuSheet = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.canvas_page_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        },
        floatingActionButton = {
            // AI Assistant FAB
            FloatingActionButton(
                onClick = {
                    val id = viewModel.getSelectedProviderIdSync()
                    val hasKey = viewModel.getApiKeyForProvider(id).isNotBlank()
                    if (id.isBlank() || (id == "GEMINI" && !viewModel.hasExplicitProviderChoice()) || !hasKey) {
                        showProviderPicker = true
                    } else {
                        viewModel.showAiWindow()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.ai_assistant))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        viewportWidthPx = coords.size.width.toFloat()
                        viewportHeightPx = coords.size.height.toFloat()
                    }
            ) {
            // 1. Interactive Canvas
            InteractiveCanvas(
                canvasEntity = canvas,
                pageEntity = viewModel.currentPage,
                currentTool = currentTool,
                strokeWidth = strokeWidth,
                strokeOpacity = strokeOpacity,
                currentColor = currentColor,
                drawWithFingers = drawWithFingers,
                rulerState = rulerState,
                zoomScale = zoomScale,
                viewportPanOffset = panOffset,
                onZoomChanged = { viewModel.setZoomScale(it) },
                onPanOffsetChanged = { viewModel.updatePanOffset(it) },
                onStrokeAdded = { stroke -> viewModel.addStrokeToCurrentPage(stroke) },
                onEraserMarkAdded = { mark -> viewModel.addEraserMarkToCurrentPage(mark) },
                onEraseAtPoint = { pt, radius -> viewModel.eraseAtPoint(pt, radius) },
                onBeginEraserGesture = { viewModel.beginEraserGesture() },
                onEndEraserGesture = { viewModel.endEraserGesture() },
                onTwoFingerTap = { viewModel.undo() },
                onMoveShape = { id, x, y -> viewModel.updateShapePosition(id, x, y) },
                onMoveText = { id, x, y -> viewModel.updateTextPosition(id, x, y) },
                onMoveImage = { id, x, y -> viewModel.updateImagePosition(id, x, y) },
                onMoveChart = { id, x, y -> viewModel.updateChartPosition(id, x, y) },
                onDeleteElement = { id, type -> viewModel.deleteElement(id, type) },
                onRotateElement = { id, type -> viewModel.rotateElement(id, type) },
                onUpdateImageOpacity = { id, op -> viewModel.updateImageOpacity(id, op) },
                onResizeElement = { id, type, w, h, anchor ->
                    when (type) {
                        "SHAPE" -> viewModel.updateShapeSize(id, w, h)
                        "IMAGE" -> viewModel.updateImageSize(id, w, h)
                        "CHART" -> viewModel.updateChartSize(id, w, h, anchor)
                        "TEXT" -> viewModel.updateTextSize(id, w, h)
                    }
                },
                selectionMode = selectionMode,
                selectedElementIds = selectedElementIds,
                onLassoComplete = { worldPts -> viewModel.selectElementsInLasso(worldPts) },
                onBeginMoveSelectedGroup = { viewModel.beginMoveSelectedElements() },
                onMoveSelectedGroup = { dx, dy -> viewModel.moveSelectedElements(dx, dy) },
                onEndMoveSelectedGroup = { viewModel.endMoveSelectedElements() },
                onResizeAndMoveElement = { id, type, w, h, x, y, anchor ->
                    viewModel.resizeAndMoveElement(id, type, w, h, x, y, anchor)
                },
                getCachedBitmap = { viewModel.getCachedBitmap(it) },
                onPreloadImage = { viewModel.preloadImageBitmap(it) },
                onEditCodeBlock = { id ->
                    editingCodeBlockId = id
                    showCodeLab = true
                },
                onRunCodeBlock = { id -> viewModel.runCodeBlock(id) }
            )
            }

            // 1b. Lasso Selection Overlay
            com.example.ui.components.LassoSelectionOverlay(
                isActive = currentTool == com.example.data.models.ToolType.SELECTOR &&
                    selectionMode == com.example.data.models.SelectionMode.LASSO &&
                    selectedElementIds.isEmpty(),
                scale = zoomScale,
                panOffset = panOffset,
                onLassoComplete = { worldPts -> viewModel.selectElementsInLasso(worldPts) }
            )

            // 2. Ruler Overlay
            RulerOverlayComponent(
                rulerState = rulerState,
                onRulerChange = { viewModel.setRulerState(it) },
                onCloseClick = { viewModel.setRulerState(rulerState.copy(isVisible = false)) }
            )

            // 3. Top Floating Drawing Toolbar with Left (Width) & Right (Opacity) Sliders
            com.example.ui.components.TopFloatingToolbar(
                currentTool = currentTool,
                eraserMode = eraserMode,
                strokeWidth = strokeWidth,
                strokeOpacity = strokeOpacity,
                currentColor = currentColor,
                rulerVisible = rulerState.isVisible,
                isSlidersVertical = isSlidersVertical,
                selectionMode = selectionMode,
                onToolSelect = { viewModel.selectTool(it, viewportWidthPx, viewportHeightPx) },
                onEraserModeToggle = {
                    val nextMode = if (eraserMode == EraserMode.OBJECT) EraserMode.PIXEL else EraserMode.OBJECT
                    viewModel.setEraserMode(nextMode)
                },
                onSelectionModeToggle = {
                    val nextMode = if (selectionMode == com.example.data.models.SelectionMode.SINGLE) com.example.data.models.SelectionMode.LASSO else com.example.data.models.SelectionMode.SINGLE
                    viewModel.setSelectionMode(nextMode)
                },
                onStrokeWidthChange = { viewModel.setStrokeWidth(it) },
                onStrokeOpacityChange = { viewModel.setStrokeOpacity(it) },
                onColorPickerClick = { showColorPickerSheet = true },
                onToggleSliderOrientation = { viewModel.toggleSliderOrientation() },
                isLandscape = isLandscape,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Вертикальні бічні панелі (Width & Opacity)
            AnimatedVisibility(
                visible = isSlidersVertical,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                VerticalFloatingSidePanel(
                    panelType = PanelType.WIDTH,
                    value = strokeWidth,
                    valueRange = 1f..50f,
                    displayText = "${strokeWidth.toInt()} px",
                    currentColor = currentColor,
                    opacity = strokeOpacity,
                    onValueChange = { viewModel.setStrokeWidth(it) },
                    isEraser = currentTool == ToolType.ERASER,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = isSlidersVertical,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                VerticalFloatingSidePanel(
                    panelType = PanelType.OPACITY,
                    value = strokeOpacity,
                    valueRange = 0.05f..1f,
                    displayText = "${(strokeOpacity * 100).toInt()}%",
                    currentColor = currentColor,
                    opacity = strokeOpacity,
                    onValueChange = { viewModel.setStrokeOpacity(it) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // ─── Audio Recording Indicator (під час запису) ───
            val isRecordingAudio = audioStatus is RecordingStatus.Recording
            val recordingData = audioStatus as? RecordingStatus.Recording

            AnimatedVisibility(
                visible = isRecordingAudio,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Пульсуюча червона точка
                        val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )

                        // Waveform Visualizer з реальними амплітудами
                        com.example.ui.components.AudioWaveformVisualizer(
                            isRecording = true,
                            recordingTimeText = recordingData?.let {
                                val sec = it.durationMs / 1000
                                String.format(java.util.Locale.US, "%02d:%02d", sec / 60, sec % 60)
                            } ?: "00:00",
                            amplitudes = recordingData?.amplitudes ?: emptyList(),
                            strokeWidth = 4f
                        )

                        // Кнопка STOP
                        FilledTonalButton(
                            onClick = { viewModel.stopAudioRecording() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, stringResource(R.string.stop), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.stop), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Audio Player Bar (Audio Player Pill) shown when recording exists or playing
            val isPlaying = audioStatus is RecordingStatus.Playing && (audioStatus as RecordingStatus.Playing).isPlaying
            val currentPosMs = if (audioStatus is RecordingStatus.Playing) (audioStatus as RecordingStatus.Playing).currentPositionMs else 0L
            val totalDurMs = if (audioStatus is RecordingStatus.Playing) (audioStatus as RecordingStatus.Playing).totalDurationMs else (latestRecording?.durationMs ?: 0L)

            AnimatedVisibility(
                visible = showAudioPill
                          && (latestRecording != null || audioStatus is RecordingStatus.Playing)
                          && audioStatus !is RecordingStatus.Recording,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .fillMaxWidth(0.92f)
            ) {
                com.example.ui.components.AudioPlayerPill(
                    isPlaying = isPlaying,
                    currentPositionMs = currentPosMs,
                    totalDurationMs = totalDurMs,
                    onPlayPauseClick = {
                        if (audioStatus is RecordingStatus.Playing) {
                            if (isPlaying) {
                                viewModel.pauseAudioPlayback()
                            } else {
                                viewModel.resumeAudioPlayback()
                            }
                        } else if (latestRecording != null) {
                            viewModel.playAudioRecording(latestRecording.filePath)
                        }
                    },
                    onSeek = { newPos ->
                        viewModel.seekAudioPlayback(newPos.toLong())
                    },
                    onDeleteClick = {
                        if (latestRecording != null) {
                            viewModel.deleteAudioRecording(latestRecording)
                            showAudioPill = false
                            Toast.makeText(context, context.getString(R.string.audio_deleted), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismissClick = {
                        showAudioPill = false
                        viewModel.stopAudioPlayback()
                    }
                )
            }

            // Right side panel removed per user request for clean canvas space



            // 5. Bottom 'Додати' Floating Button
            ExtendedFloatingActionButton(
                onClick = { showInsertSheet = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add)) },
                text = { Text(stringResource(R.string.add), fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )

            // 6. Bottom Left Overlay (Page count & Zoom)
            BottomLeftOverlay(
                currentPage = currentPageIndex,
                totalPages = pages.size,
                zoomPercentage = (zoomScale * 100).toInt(),
                onPageIndicatorClick = { showPageStripSheet = true },
                onZoomIndicatorClick = {
                    val nextZoom = when {
                        zoomScale < 1f -> 1f
                        zoomScale < 2f -> 2f
                        zoomScale < 3f -> 3f
                        else -> 1f
                    }
                    viewModel.setZoomScale(nextZoom)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            )

            if (selectedElementIds.isNotEmpty() && referenceCaptureSession == null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 84.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                coroutineScope.launch {
                                    referenceDestinations = referenceRepository.listDestinations()
                                    showReferenceDestination = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_link))
                        }
                        if (referencesForSelection.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    if (referencesForSelection.size == 1) {
                                        onOpenCanvasReference(referencesForSelection.first().toNavigationRequest())
                                    } else {
                                        coroutineScope.launch {
                                            referenceDestinations = referenceRepository.listDestinations()
                                            showReferenceList = true
                                        }
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.open_links))
                            }
                        }
                    }
                }
            }

            val activeCaptureSession = referenceCaptureSession?.takeIf { session ->
                session.destination.canvasId == canvas?.id && session.destination.pageId == currentPage?.id
            }
            if (activeCaptureSession != null && viewportWidthPx > 0f && viewportHeightPx > 0f) {
                CanvasReferenceTargetCaptureBar(
                    destination = activeCaptureSession.destination,
                    currentViewport = CanvasViewport.fromCanvasTransform(
                        panX = panOffset.x,
                        panY = panOffset.y,
                        zoom = zoomScale,
                        viewportWidthPx = viewportWidthPx,
                        viewportHeightPx = viewportHeightPx
                    ),
                    onConfirm = { viewport ->
                        coroutineScope.launch {
                            runCatching {
                                referenceRepository.saveReference(
                                    activeCaptureSession.createDraft(
                                        viewport = viewport,
                                        targetElementIds = selectedElementIds
                                    )
                                )
                            }.onSuccess {
                                Toast.makeText(context, linkSavedText, Toast.LENGTH_SHORT).show()
                                viewModel.updateSelectedElementIds(emptySet())
                                onReferenceCaptureFinished()
                            }.onFailure { error ->
                                Toast.makeText(context, error.message ?: linkSaveFailedText, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onCancel = onReferenceCaptureFinished,
                    text = referenceUiText,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 76.dp, start = 16.dp, end = 16.dp)
                )
            }

            CanvasReferenceNavigationEffect(
                request = referenceNavigationRequest?.takeIf {
                    it.canvasId == canvas?.id && it.pageId == currentPage?.id
                },
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                currentPan = panOffset,
                currentZoom = zoomScale,
                onTransform = { targetPan, targetZoom ->
                    viewModel.updatePanOffset(targetPan)
                    viewModel.setZoomScale(targetZoom)
                },
                onTargetReached = { request ->
                    viewModel.updateSelectedElementIds(request.targetElementIds)
                    onReferenceNavigationConsumed()
                }
            )
        }
    }

    // Bottom Sheets & Dialogs
    if (showCodeLab) {
        val editingBlock = editingCodeBlockId?.let(viewModel::getCodeBlock)
        CodeLabDialog(
            onDismiss = {
                showCodeLab = false
                editingCodeBlockId = null
            },
            onAddToCanvas = { language, source, result ->
                if (editingBlock != null) {
                    viewModel.updateCodeBlock(editingBlock.id, language, source, result)
                } else {
                    viewModel.insertCodeBlock(
                        language = language,
                        source = source,
                        result = result,
                        viewportWidth = viewportWidthPx,
                        viewportHeight = viewportHeightPx,
                        panOffsetX = panOffset.x,
                        panOffsetY = panOffset.y,
                        scale = zoomScale
                    )
                }
                showCodeLab = false
                editingCodeBlockId = null
            },
            initialLanguage = editingBlock?.language ?: com.example.data.models.CodeLanguage.PYTHON,
            initialSource = editingBlock?.source ?: """
                topic = "Thermodynamics"
                temperature = 20 + 5
                print(topic, temperature)
            """.trimIndent()
        )
    }

    if (showStudyDeck) {
        StudyDeckDialog(
            deckSummaries = studyDeckSummaries,
            selectedDeck = selectedStudyDeck,
            cards = studyCards,
            dueCards = dueStudyCards,
            onSelectDeck = { selectedStudyDeckId = it },
            onCreateDeck = { title, description ->
                coroutineScope.launch {
                    runCatching {
                        studyDeckRepository.createDeck(
                            title = title,
                            description = description,
                            canvasId = canvas?.id,
                            pageId = currentPage?.id
                        )
                    }.onSuccess { deck ->
                        selectedStudyDeckId = deck.id
                    }
                }
            },
            onAddCard = { deckId, prompt, answer, hint ->
                coroutineScope.launch {
                    studyDeckRepository.addCard(
                        deckId = deckId,
                        prompt = prompt,
                        answer = answer,
                        hint = hint,
                        sourceCanvasId = canvas?.id,
                        sourcePageId = currentPage?.id,
                        sourceElementIds = selectedElementIds.toList()
                    )
                }
            },
            onGradeCard = { card, grade ->
                coroutineScope.launch { studyDeckRepository.reviewCard(card.id, grade) }
            },
            onDeleteCard = { card ->
                coroutineScope.launch { studyDeckRepository.deleteCard(card) }
            },
            onDismiss = { showStudyDeck = false }
        )
    }

    if (showReferenceDestination && currentPage != null && canvas != null && selectedElementIds.isNotEmpty()) {
        CanvasReferenceDestinationDialog(
            destinations = referenceDestinations,
            sourceSelectionSize = selectedElementIds.size,
            onDestinationSelected = { destination ->
                val source = CanvasReferenceSource(
                    canvasId = canvas!!.id,
                    pageId = currentPage.id,
                    elementIds = selectedElementIds
                )
                showReferenceDestination = false
                viewModel.updateSelectedElementIds(emptySet())
                onReferenceCaptureStarted(
                    CanvasReferenceCaptureSession(
                        source = source,
                        destination = destination,
                        label = canvas?.title.orEmpty()
                    )
                )
            },
            onDismiss = { showReferenceDestination = false },
            text = referenceUiText
        )
    }

    if (showReferenceList) {
        val destinationPages = referenceDestinations
            .flatMap { it.pages }
            .associateBy { it.pageId }
        CanvasReferenceListDialog(
            references = referencesForSelection.map { reference ->
                val destination = destinationPages[reference.targetPageId]
                CanvasReferenceListItem(
                    reference = reference,
                    destinationCanvasTitle = destination?.canvasTitle ?: "Sketchpad",
                    destinationPageIndex = destination?.pageIndex ?: 0
                )
            },
            onOpen = { request ->
                showReferenceList = false
                onOpenCanvasReference(request)
            },
            onDelete = { referenceId ->
                coroutineScope.launch {
                    referenceRepository.deleteReference(referenceId)
                }
            },
            onDismiss = { showReferenceList = false },
            text = referenceUiText
        )
    }

    if (showTopMenuSheet) {
        CanvasTopMenuBottomSheet(
            currentBgColor = canvas?.backgroundColor ?: 0xFFFFFFFF.toInt(),
            currentPattern = canvas?.backgroundPattern ?: com.example.data.models.BackgroundPattern.BLANK,
            currentPreset = canvas?.pageSizePreset ?: com.example.data.models.PageSizePreset.UNLIMITED,
            onBgColorChange = { viewModel.updateBackgroundColor(it) },
            onPatternChange = { viewModel.updateBackgroundPattern(it) },
            onPresetChange = { preset, w, h -> viewModel.updatePageSizePreset(preset, w, h) },
            onOpenCustomColorPicker = {
                showTopMenuSheet = false
                showColorPickerSheet = true
            },
            onOpenThemeSettings = onOpenThemeSettings,
            onDismiss = { showTopMenuSheet = false }
        )
    }

    if (showColorPickerSheet) {
        ColorPickerBottomSheet(
            initialColor = currentColor,
            recentColors = recentColors,
            onColorSelected = { viewModel.setColor(it) },
            onDismiss = { showColorPickerSheet = false }
        )
    }

    if (showInsertSheet) {
        InsertMenuBottomSheet(
            drawWithFingers = drawWithFingers,
            onDrawWithFingersChange = { viewModel.setDrawWithFingers(it) },
            onInsertImageClick = { insertImageLauncher.launch("image/*") },
            onInsertTextClick = { showTextInputDialog = true },
            onInsertShapeClick = { shapeType ->
                viewModel.insertShape(
                    shapeType = shapeType,
                    viewportWidth = viewportWidthPx,
                    viewportHeight = viewportHeightPx,
                    panOffsetX = panOffset.x,
                    panOffsetY = panOffset.y,
                    scale = zoomScale
                )
            },
            onInsertChartClick = { showChartDialog = true },
            onPasteContentClick = {
                viewModel.insertText(
                    text = context.getString(R.string.pasted_from_clipboard),
                    viewportWidth = viewportWidthPx,
                    viewportHeight = viewportHeightPx,
                    panOffsetX = panOffset.x,
                    panOffsetY = panOffset.y,
                    scale = zoomScale
                )
            },
            onRecognizeShapeClick = { viewModel.recognizeAndVectorizeLastStroke() },
            onPlotFunctionClick = { viewModel.plotFunctionFromStrokes() },
            onLatexConvertClick = { viewModel.convertHandwritingToLatex() },
            onInsertCodeClick = {
                editingCodeBlockId = null
                showCodeLab = true
            },
            onSummarizeNotesClick = {
                viewModel.showAiWindow()
                viewModel.sendAiPrompt("Analyze the entire current note and give me a concise structured summary, key formulas, and unclear gaps.")
            },
            onCreateQuizClick = {
                viewModel.showAiWindow()
                viewModel.sendAiPrompt("Create five university-level self-test questions from the entire current note, then put the answers after a divider.")
            },
            onSuggestLinksClick = {
                viewModel.showAiWindow()
                viewModel.sendAiPrompt("Find concepts in this note that should link to related pages or exact canvas regions. Return suggested Obsidian-style links with short reasons.")
            },
            onStudyDeckClick = { showStudyDeck = true },
            onDismiss = { showInsertSheet = false }
        )
    }

    if (showPageStripSheet) {
        PageStripBottomSheet(
            pages = pages,
            currentPageIndex = currentPageIndex,
            onPageSelected = { viewModel.setCurrentPage(it) },
            onAddPageClick = { viewModel.addNewPage() },
            onDeletePageClick = { viewModel.deletePage(it) },
            onDismiss = { showPageStripSheet = false }
        )
    }

    if (showGeminiSheet) {
        GeminiChatBottomSheet(
            messages = chatMessages,
            isLoading = isAiLoading,
            onSendMessage = { viewModel.sendAiPrompt(it) },
            onDismiss = { showGeminiSheet = false },
            onSaveApiKey = { key -> viewModel.saveApiKey(key) },
            initialApiKey = viewModel.getStoredApiKey(),
            selectedProviderDisplayName = providerDisplayName,
            onChangeProvider = {
                showGeminiSheet = false
                showProviderPicker = true
            }
        )
    }

    if (isAiWindowVisible) {
        FloatingAiWindow(
            messages = chatMessages,
            isLoading = isAiLoading,
            onSendMessage = { viewModel.sendAiPrompt(it) },
            onClose = { viewModel.hideAiWindow() },
            onSaveApiKey = { key -> viewModel.saveApiKey(key) },
            initialApiKey = viewModel.getStoredApiKey(),
            selectedProviderDisplayName = providerDisplayName,
            onChangeProvider = { showProviderPicker = true }
        )
    }

    if (showProviderPicker) {
        com.example.ui.components.AiProviderPickerSheet(
            onPick = { providerId, apiKey, endpoint, model ->
                viewModel.selectAiProvider(providerId, apiKey, endpoint, model)
                showProviderPicker = false
                viewModel.showAiWindow()
            },
            onDismiss = { showProviderPicker = false },
            currentProviderId = selectedProviderId,
            getKeyForProvider = { viewModel.getApiKeyForProvider(it) },
            getEndpointForProvider = { viewModel.getCustomEndpoint(it) },
            getModelForProvider = { viewModel.getCustomModel(it) }
        )
    }

    // Math Function Plotter Dialog
    if (showMathFunctionDialog) {
        AlertDialog(
            onDismissRequest = { showMathFunctionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.insert_function_chart))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.function_formula_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = mathFormulaVal,
                        onValueChange = { mathFormulaVal = it },
                        label = { Text(stringResource(R.string.formula_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mathXMinVal,
                            onValueChange = { mathXMinVal = it },
                            label = { Text("X min") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mathXMaxVal,
                            onValueChange = { mathXMaxVal = it },
                            label = { Text("X max") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        showChartDialog = true
                        showMathFunctionDialog = false
                    }) {
                        Text(stringResource(R.string.empty_grid))
                    }
                    Button(onClick = {
                        val xMin = mathXMinVal.toFloatOrNull() ?: -10f
                        val xMax = mathXMaxVal.toFloatOrNull() ?: 10f
                        viewModel.insertMathFunctionChart(
                            formula = mathFormulaVal.trim().ifEmpty { "sin(x)" },
                            xMin = xMin,
                            xMax = xMax,
                            viewportWidth = viewportWidthPx,
                            viewportHeight = viewportHeightPx,
                            panOffsetX = panOffset.x,
                            panOffsetY = panOffset.y,
                            scale = zoomScale
                        )
                        showMathFunctionDialog = false
                    }) {
                        Text(stringResource(R.string.plot_graph))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMathFunctionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Text Input Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text(stringResource(R.string.insert_text)) },
            text = {
                OutlinedTextField(
                    value = textInputVal,
                    onValueChange = { textInputVal = it },
                    label = { Text(stringResource(R.string.your_text)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (textInputVal.isNotBlank()) {
                        viewModel.insertText(
                            text = textInputVal.trim(),
                            viewportWidth = viewportWidthPx,
                            viewportHeight = viewportHeightPx,
                            panOffsetX = panOffset.x,
                            panOffsetY = panOffset.y,
                            scale = zoomScale
                        )
                        textInputVal = ""
                    }
                    showTextInputDialog = false
                }) {
                    Text(stringResource(R.string.insert))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_note_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.export_note_hint))
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportAllPagesPdf { file ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_notebook_pdf)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.export_all_pdf))
                    }

                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportPdf { file ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_page_pdf)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.export_page_pdf))
                    }

                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportPng { file ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_image)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.export_png))
                    }

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            exportToObsidian()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.export_obsidian))
                    }

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportImage { file ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/svg+xml"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_svg)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.export_svg))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showLayersPanel) {
        val currentLayers = remember(pages, currentPageIndex) {
            pages.getOrNull(currentPageIndex)?.getEffectiveLayers() ?: emptyList()
        }
        LayersBottomSheet(
            layers = currentLayers,
            activeLayerId = activeLayerId ?: viewModel.currentPage?.activeLayerId,
            onAddLayer = { viewModel.addLayer() },
            onSelectLayer = { viewModel.setActiveLayer(it) },
            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onOpacityChange = { id, op -> viewModel.setLayerOpacity(id, op) },
            onMoveUp = { viewModel.moveLayerUp(it) },
            onMoveDown = { viewModel.moveLayerDown(it) },
            onRename = { id, name -> viewModel.renameLayer(id, name) },
            onDeleteLayer = { viewModel.deleteLayer(it) },
            onDismiss = { viewModel.toggleLayersPanel() }
        )
    }

    if (showAudioSheet) {
        val isPlaying = audioStatus is RecordingStatus.Playing && (audioStatus as RecordingStatus.Playing).isPlaying
        val currentPlayingPath = if (audioStatus is RecordingStatus.Playing) (audioStatus as RecordingStatus.Playing).filePath else null

        AudioManagementSheet(
            recordings = audioRecordings,
            currentlyPlayingPath = currentPlayingPath,
            isPlaying = isPlaying,
            onPlayClick = { recording -> viewModel.playAudioRecording(recording.filePath) },
            onPauseClick = { viewModel.pauseAudioPlayback() },
            onRenameClick = { recording, name -> viewModel.renameAudioRecording(recording, name) },
            onDeleteClick = { recording -> viewModel.deleteAudioRecording(recording) },
            onDismiss = { showAudioSheet = false }
        )
    }

    if (showChartDialog) {
        AlertDialog(
            onDismissRequest = { showChartDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.insert_chart))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.choose_grid_type), style = MaterialTheme.typography.bodyMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { chartWithSteps = false }
                    ) {
                        RadioButton(selected = !chartWithSteps, onClick = { chartWithSteps = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.empty_chart))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { chartWithSteps = true }
                    ) {
                        RadioButton(selected = chartWithSteps, onClick = { chartWithSteps = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.chart_with_steps))
                    }

                    if (chartWithSteps) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = chartXStepVal,
                                onValueChange = { chartXStepVal = it },
                                label = { Text(stringResource(R.string.x_step)) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = chartYStepVal,
                                onValueChange = { chartYStepVal = it },
                                label = { Text(stringResource(R.string.y_step)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val xs = (if (chartWithSteps) chartXStepVal.toFloatOrNull() else 1f)?.takeIf { it > 0f } ?: 1f
                    val ys = (if (chartWithSteps) chartYStepVal.toFloatOrNull() else 5f)?.takeIf { it > 0f } ?: 5f
                    viewModel.insertChart(
                        showAxisLabels = chartWithSteps,
                        xStep = xs,
                        yStep = ys,
                        viewportWidth = viewportWidthPx,
                        viewportHeight = viewportHeightPx,
                        panOffsetX = panOffset.x,
                        panOffsetY = panOffset.y,
                        scale = zoomScale
                    )
                    showChartDialog = false
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
