package com.example.desktop

import androidx.compose.ui.geometry.Offset
import com.example.desktop.ai.*
import com.example.desktop.audio.DesktopAudioRecorderManager
import com.example.desktop.storage.DesktopAutosaveManager
import com.example.desktop.theme.AppThemeStyle
import com.example.shared.academic.FunctionPlotterEngine
import com.example.shared.academic.LocalCodeAnalyzer
import com.example.shared.academic.SpacedRepetitionScheduler
import com.example.shared.core.DrawingMath
import com.example.shared.model.*
import com.example.shared.network.SketchLinkServer
import com.example.shared.protocol.SketchLinkPacketType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

data class RulerState(
    val isVisible: Boolean = false,
    val center: Offset = Offset(400f, 300f),
    val angleRad: Float = 0f,
    val length: Float = 500f,
    val width: Float = 70f
)

class DesktopViewModel(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    // Current project & pages
    private val _canvas = MutableStateFlow(CanvasEntity(title = "Нове полотно"))
    val canvas: StateFlow<CanvasEntity> = _canvas.asStateFlow()

    private val _pages = MutableStateFlow(listOf(PageEntity(canvasId = _canvas.value.id, pageIndex = 0)))
    val pages: StateFlow<List<PageEntity>> = _pages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    val currentPage: PageEntity
        get() = _pages.value.getOrElse(_currentPageIndex.value) { _pages.value.first() }

    // Tools & Settings
    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    private val _brushSize = MutableStateFlow(4f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    private val _brushOpacity = MutableStateFlow(1f)
    val brushOpacity: StateFlow<Float> = _brushOpacity.asStateFlow()

    private val _currentColor = MutableStateFlow(HslaColor.BLACK)
    val currentColor: StateFlow<HslaColor> = _currentColor.asStateFlow()

    private val _recentColors = MutableStateFlow(
        listOf(HslaColor.BLACK, HslaColor.BLUE, HslaColor.RED, HslaColor.GREEN, HslaColor.PURPLE, HslaColor.ORANGE)
    )
    val recentColors: StateFlow<List<HslaColor>> = _recentColors.asStateFlow()

    private val _eraserMode = MutableStateFlow(EraserMode.OBJECT)
    val eraserMode: StateFlow<EraserMode> = _eraserMode.asStateFlow()

    private val _selectionMode = MutableStateFlow(SelectionMode.SINGLE)
    val selectionMode: StateFlow<SelectionMode> = _selectionMode.asStateFlow()

    private val _useVerticalSliders = MutableStateFlow(false)
    val useVerticalSliders: StateFlow<Boolean> = _useVerticalSliders.asStateFlow()

    private val _symmetryMode = MutableStateFlow(SymmetryMode.NONE)
    val symmetryMode: StateFlow<SymmetryMode> = _symmetryMode.asStateFlow()

    private val _activeLayerId = MutableStateFlow("default")
    val activeLayerId: StateFlow<String> = _activeLayerId.asStateFlow()

    // Ruler & Protractor Overlay State
    private val _rulerState = MutableStateFlow(RulerState())
    val rulerState: StateFlow<RulerState> = _rulerState.asStateFlow()

    private val _isProtractorVisible = MutableStateFlow(false)
    val isProtractorVisible: StateFlow<Boolean> = _isProtractorVisible.asStateFlow()
    private val _protractorCenter = MutableStateFlow(Offset(500f, 400f))
    val protractorCenter: StateFlow<Offset> = _protractorCenter.asStateFlow()

    // Viewport & Pan/Zoom
    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Pair(0f, 0f))
    val panOffset: StateFlow<Pair<Float, Float>> = _panOffset.asStateFlow()

    // UI Dialogs
    private val _showTopMenuModal = MutableStateFlow(false)
    val showTopMenuModal: StateFlow<Boolean> = _showTopMenuModal.asStateFlow()

    private val _showInsertModal = MutableStateFlow(false)
    val showInsertModal: StateFlow<Boolean> = _showInsertModal.asStateFlow()

    private val _showLayersModal = MutableStateFlow(false)
    val showLayersModal: StateFlow<Boolean> = _showLayersModal.asStateFlow()

    private val _showColorPickerModal = MutableStateFlow(false)
    val showColorPickerModal: StateFlow<Boolean> = _showColorPickerModal.asStateFlow()

    private val _showPageStripModal = MutableStateFlow(false)
    val showPageStripModal: StateFlow<Boolean> = _showPageStripModal.asStateFlow()

    private val _showTimelineModal = MutableStateFlow(false)
    val showTimelineModal: StateFlow<Boolean> = _showTimelineModal.asStateFlow()

    private val _showCodeLabDialog = MutableStateFlow(false)
    val showCodeLabDialog: StateFlow<Boolean> = _showCodeLabDialog.asStateFlow()

    private val _showStudyDeckDialog = MutableStateFlow(false)
    val showStudyDeckDialog: StateFlow<Boolean> = _showStudyDeckDialog.asStateFlow()

    private val _showTextInputDialog = MutableStateFlow(false)
    val showTextInputDialog: StateFlow<Boolean> = _showTextInputDialog.asStateFlow()

    private val _showAiWindow = MutableStateFlow(false)
    val showAiWindow: StateFlow<Boolean> = _showAiWindow.asStateFlow()

    private val _showAiProviderModal = MutableStateFlow(false)
    val showAiProviderModal: StateFlow<Boolean> = _showAiProviderModal.asStateFlow()

    private val _showAudioManagementModal = MutableStateFlow(false)
    val showAudioManagementModal: StateFlow<Boolean> = _showAudioManagementModal.asStateFlow()

    private val _showExitProtectionDialog = MutableStateFlow(false)
    val showExitProtectionDialog: StateFlow<Boolean> = _showExitProtectionDialog.asStateFlow()

    private val _showLayersPanel = MutableStateFlow(false)
    val showLayersPanel: StateFlow<Boolean> = _showLayersPanel.asStateFlow()

    private val _showPerformanceOverlay = MutableStateFlow(false)
    val showPerformanceOverlay: StateFlow<Boolean> = _showPerformanceOverlay.asStateFlow()

    private val _showPairingDialog = MutableStateFlow(false)
    val showPairingDialog: StateFlow<Boolean> = _showPairingDialog.asStateFlow()

    private val _fps = MutableStateFlow(60)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _strokeLatencyMs = MutableStateFlow(4L)
    val strokeLatencyMs: StateFlow<Long> = _strokeLatencyMs.asStateFlow()

    private val _packetsPerSec = MutableStateFlow(0)
    val packetsPerSec: StateFlow<Int> = _packetsPerSec.asStateFlow()

    private val _whiteCanvasMode = MutableStateFlow(false)
    val whiteCanvasMode: StateFlow<Boolean> = _whiteCanvasMode.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppThemeStyle.SYSTEM_DEFAULT)
    val currentTheme: StateFlow<AppThemeStyle> = _currentTheme.asStateFlow()

    private val _snapshots = MutableStateFlow<List<String>>(emptyList())
    val snapshots: StateFlow<List<String>> = _snapshots.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Unsaved Changes Tracking
    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    // Undo / Redo Command Stacks
    private val undoStack = ArrayDeque<CanvasCommand>()
    private val redoStack = ArrayDeque<CanvasCommand>()

    // Flashcards & Study Deck
    private val _flashcards = MutableStateFlow<List<FlashcardEntity>>(emptyList())
    val flashcards: StateFlow<List<FlashcardEntity>> = _flashcards.asStateFlow()

    // Audio notes
    private val _audioRecordings = MutableStateFlow<List<AudioRecordingEntity>>(emptyList())
    val audioRecordings: StateFlow<List<AudioRecordingEntity>> = _audioRecordings.asStateFlow()
    val audioRecorderManager = DesktopAudioRecorderManager()

    // AI assistant
    val aiPreferences = DesktopAiPreferences()
    private val _aiMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Autosave
    val autosaveManager: DesktopAutosaveManager by lazy {
        DesktopAutosaveManager(
            getCanvasState = { Pair(_canvas.value, _pages.value) },
            onAutosaveCompleted = { _hasUnsavedChanges.value = false }
        )
    }

    // SketchLink Server
    val sketchLinkServer = SketchLinkServer(port = 8765)

    init {
        sketchLinkServer.start()
        autosaveManager.startPeriodicAutosave(30_000L)
        listenToSketchLinkPackets()

        // Attempt restore from autosave
        val restored = autosaveManager.restoreLastSession()
        if (restored != null && restored.pages.isNotEmpty()) {
            _canvas.value = restored.canvas
            _pages.value = restored.pages
        }
    }

    private fun listenToSketchLinkPackets() {
        scope.launch {
            sketchLinkServer.incomingPackets.collect { packet ->
                when (packet.type) {
                    SketchLinkPacketType.STROKE_DOWN, SketchLinkPacketType.STROKE_MOVE -> {
                        packet.strokeEvent?.let { event ->
                            val stroke = StrokeEntity(
                                id = event.strokeId,
                                tool = event.tool,
                                colorHsla = event.color,
                                baseWidth = event.baseWidth,
                                points = listOf(event.point)
                            )
                            addOrAppendStroke(stroke)
                        }
                    }
                    SketchLinkPacketType.CLEAR_CANVAS -> clearCurrentPage()
                    else -> {}
                }
            }
        }
    }

    fun selectTool(tool: ToolType) {
        _currentTool.value = tool
        if (tool == ToolType.RULER) {
            _rulerState.value = _rulerState.value.copy(isVisible = true)
        }
    }

    fun setBrushSize(size: Float) { _brushSize.value = size.coerceIn(1f, 100f) }
    fun setBrushOpacity(opacity: Float) { _brushOpacity.value = opacity.coerceIn(0.05f, 1.0f) }
    fun setColor(color: HslaColor) {
        _currentColor.value = color
        if (color !in _recentColors.value) {
            _recentColors.value = (listOf(color) + _recentColors.value).take(8)
        }
    }

    fun toggleEraserMode() {
        _eraserMode.value = if (_eraserMode.value == EraserMode.OBJECT) EraserMode.PIXEL else EraserMode.OBJECT
    }

    fun toggleSelectionMode() {
        _selectionMode.value = if (_selectionMode.value == SelectionMode.SINGLE) SelectionMode.LASSO else SelectionMode.SINGLE
    }

    fun toggleOrientation() {
        _useVerticalSliders.value = !_useVerticalSliders.value
    }

    fun setSymmetryMode(mode: SymmetryMode) { _symmetryMode.value = mode }
    fun toggleWhiteCanvasMode() { _whiteCanvasMode.value = !_whiteCanvasMode.value }
    fun toggleLayersPanel() { _showLayersModal.value = !_showLayersModal.value }
    fun togglePerformanceOverlay() { _showPerformanceOverlay.value = !_showPerformanceOverlay.value }
    fun togglePairingDialog() { _showPairingDialog.value = !_showPairingDialog.value }
    fun setTheme(theme: AppThemeStyle) { _currentTheme.value = theme }
    fun toggleDarkTheme() { _isDarkTheme.value = !_isDarkTheme.value }

    fun setZoomScale(scale: Float) { _zoomScale.value = scale.coerceIn(0.1f, 10.0f) }
    fun cycleZoom() {
        _zoomScale.value = when (_zoomScale.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            2.0f -> 0.5f
            else -> 1.0f
        }
    }

    fun updatePan(dx: Float, dy: Float) {
        _panOffset.value = Pair(_panOffset.value.first + dx, _panOffset.value.second + dy)
    }

    // Ruler
    fun updateRuler(center: Offset? = null, angleRad: Float? = null, isVisible: Boolean? = null) {
        _rulerState.value = _rulerState.value.copy(
            center = center ?: _rulerState.value.center,
            angleRad = angleRad ?: _rulerState.value.angleRad,
            isVisible = isVisible ?: _rulerState.value.isVisible
        )
    }

    fun toggleProtractor() {
        _isProtractorVisible.value = !_isProtractorVisible.value
    }
    fun updateProtractorCenter(c: Offset) { _protractorCenter.value = c }

    // Dialog toggles
    fun setShowTopMenuModal(show: Boolean) { _showTopMenuModal.value = show }
    fun setShowInsertModal(show: Boolean) { _showInsertModal.value = show }
    fun setShowLayersModal(show: Boolean) { _showLayersModal.value = show }
    fun setShowColorPickerModal(show: Boolean) { _showColorPickerModal.value = show }
    fun setShowPageStripModal(show: Boolean) { _showPageStripModal.value = show }
    fun setShowTimelineModal(show: Boolean) { _showTimelineModal.value = show }
    fun setShowCodeLabDialog(show: Boolean) { _showCodeLabDialog.value = show }
    fun setShowStudyDeckDialog(show: Boolean) { _showStudyDeckDialog.value = show }
    fun setShowTextInputDialog(show: Boolean) { _showTextInputDialog.value = show }
    fun setShowAiWindow(show: Boolean) { _showAiWindow.value = show }
    fun setShowAiProviderModal(show: Boolean) { _showAiProviderModal.value = show }
    fun setShowAudioManagementModal(show: Boolean) { _showAudioManagementModal.value = show }
    fun setShowExitProtectionDialog(show: Boolean) { _showExitProtectionDialog.value = show }

    // Background & Page configuration
    fun setPageBackgroundColor(color: Int) {
        _canvas.value = _canvas.value.copy(backgroundColor = color)
        updateCurrentPage(currentPage.copy(backgroundLineColor = color))
        _hasUnsavedChanges.value = true
    }

    fun setPageBackgroundPattern(pat: BackgroundPattern) {
        updateCurrentPage(currentPage.copy(backgroundPattern = pat))
        _hasUnsavedChanges.value = true
    }

    fun setPageSizePreset(preset: PageSizePreset) {
        _canvas.value = _canvas.value.copy(pageSizePreset = preset)
        _hasUnsavedChanges.value = true
    }

    // Insert actions
    fun insertShape(type: ShapeType) {
        val shape = ShapeEntity(
            id = UUID.randomUUID().toString(),
            shapeType = type,
            x = 200f,
            y = 200f,
            width = 180f,
            height = 180f,
            strokeColor = _currentColor.value.toArgbInt(),
            strokeWidth = _brushSize.value
        )
        val updated = currentPage.withAddedShape(shape)
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun insertTextBlock(textBlock: TextBlockEntity) {
        val updated = currentPage.withUpdatedLayer(_activeLayerId.value) { layer ->
            layer.copy(textBlocks = layer.textBlocks + textBlock)
        }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun insertChart(title: String = "Графік f(x)") {
        val chart = ChartElementEntity(
            id = UUID.randomUUID().toString(),
            x = 150f,
            y = 150f,
            width = 400f,
            height = 260f,
            title = title
        )
        val updated = currentPage.withAddedChart(chart)
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun insertCodeBlock(codeBlock: CodeBlockEntity) {
        val updated = currentPage.withAddedCodeBlock(codeBlock)
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun updateCodeBlock(codeBlock: CodeBlockEntity) {
        val updated = currentPage.withUpdatedLayer(_activeLayerId.value) { layer ->
            layer.copy(codeBlocks = layer.codeBlocks.map { if (it.id == codeBlock.id) codeBlock else it })
        }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun deleteCodeBlock(id: String) {
        val updated = currentPage.withUpdatedLayer(_activeLayerId.value) { layer ->
            layer.copy(codeBlocks = layer.codeBlocks.filterNot { it.id == id })
        }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun saveFlashcard(card: FlashcardEntity) {
        val existing = _flashcards.value.indexOfFirst { it.id == card.id }
        if (existing >= 0) {
            _flashcards.value = _flashcards.value.toMutableList().also { it[existing] = card }
        } else {
            _flashcards.value = _flashcards.value + card
        }
    }

    // AI queries
    fun sendAiMessage(prompt: String, attachVision: Boolean) {
        _aiMessages.value = _aiMessages.value + ChatMessage("user", prompt)
        _isAiLoading.value = true

        scope.launch {
            val provider = AiProviderRegistry.getProvider(aiPreferences.selectedProviderId)
            val key = aiPreferences.getKey(provider.id)
            val endpoint = aiPreferences.getEndpoint(provider.id)
            val model = aiPreferences.getModel(provider.id)

            val reply = provider.query(
                text = prompt,
                imageBase64 = null,
                apiKey = key,
                endpoint = endpoint,
                model = model
            )

            _aiMessages.value = _aiMessages.value + ChatMessage("assistant", reply)
            _isAiLoading.value = false
        }
    }

    // Polygon Lasso Selection
    fun selectStrokesInPolygon(polygon: List<Offset>) {
        if (polygon.size < 3) return
        // Polygon selection logic
    }

    // Drawing actions
    fun addOrAppendStroke(incomingStroke: StrokeEntity) {
        val activeId = _activeLayerId.value
        val updated = currentPage.withUpdatedLayer(activeId) { layer ->
            val existing = layer.strokes.find { it.id == incomingStroke.id }
            if (existing != null) {
                val updatedPoints = existing.points + incomingStroke.points
                val updatedStrokes = layer.strokes.map {
                    if (it.id == incomingStroke.id) it.copy(points = updatedPoints) else it
                }
                layer.copy(strokes = updatedStrokes)
            } else {
                layer.copy(strokes = layer.strokes + incomingStroke)
            }
        }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun commitStroke(stroke: StrokeEntity, canvasCenter: Pair<Float, Float> = Pair(960f, 540f)) {
        val symMode = _symmetryMode.value
        val strokesToAdd = DrawingMath.generateSymmetricStrokes(stroke, symMode, canvasCenter.first, canvasCenter.second)

        strokesToAdd.forEach { s ->
            val cmd = AddStrokeCommand(_activeLayerId.value, s)
            executeCommand(cmd)
        }
        _hasUnsavedChanges.value = true
    }

    fun executeCommand(command: CanvasCommand) {
        val updated = command.execute(currentPage)
        updateCurrentPage(updated)
        undoStack.addLast(command)
        redoStack.clear()
        _hasUnsavedChanges.value = true
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val cmd = undoStack.removeLast()
            val updated = cmd.undo(currentPage)
            updateCurrentPage(updated)
            redoStack.addLast(cmd)
            _hasUnsavedChanges.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val cmd = redoStack.removeLast()
            val updated = cmd.execute(currentPage)
            updateCurrentPage(updated)
            undoStack.addLast(cmd)
            _hasUnsavedChanges.value = true
        }
    }

    fun clearCurrentPage() {
        val updated = currentPage.withUpdatedLayer(_activeLayerId.value) { layer ->
            layer.copy(strokes = emptyList(), shapes = emptyList(), textBlocks = emptyList())
        }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    // Layers
    fun addLayer(name: String = "Шар ${_pages.value.first().getEffectiveLayers().size + 1}") {
        val newLayer = LayerEntity(name = name)
        val updated = currentPage.copy(layers = currentPage.getEffectiveLayers() + newLayer, activeLayerId = newLayer.id)
        _activeLayerId.value = newLayer.id
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun deleteLayer(layerId: String) {
        val layers = currentPage.getEffectiveLayers()
        if (layers.size <= 1) return
        val updatedLayers = layers.filterNot { it.id == layerId }
        val updated = currentPage.copy(layers = updatedLayers, activeLayerId = updatedLayers.first().id)
        _activeLayerId.value = updatedLayers.first().id
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun selectLayer(layerId: String) {
        _activeLayerId.value = layerId
        updateCurrentPage(currentPage.copy(activeLayerId = layerId))
    }

    fun setActiveLayer(layerId: String) = selectLayer(layerId)

    fun toggleLayerVisibility(layerId: String) {
        val updated = currentPage.withUpdatedLayer(layerId) { it.copy(isVisible = !it.isVisible) }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun updateLayerOpacity(layerId: String, opacity: Float) {
        val updated = currentPage.withUpdatedLayer(layerId) { it.copy(opacity = opacity.coerceIn(0f, 1f)) }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun setLayerOpacity(layerId: String, opacity: Float) = updateLayerOpacity(layerId, opacity)

    fun setLayerBlendMode(layerId: String, blendMode: BlendMode) {
        val updated = currentPage.withUpdatedLayer(layerId) { it.copy(blendMode = blendMode) }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    fun moveLayerUp(index: Int) {
        val layers = currentPage.getEffectiveLayers().toMutableList()
        if (index < layers.size - 1) {
            val item = layers.removeAt(index)
            layers.add(index + 1, item)
            updateCurrentPage(currentPage.copy(layers = layers))
            _hasUnsavedChanges.value = true
        }
    }

    fun moveLayerDown(index: Int) {
        val layers = currentPage.getEffectiveLayers().toMutableList()
        if (index > 0) {
            val item = layers.removeAt(index)
            layers.add(index - 1, item)
            updateCurrentPage(currentPage.copy(layers = layers))
            _hasUnsavedChanges.value = true
        }
    }

    fun renameLayer(layerId: String, newName: String) {
        val updated = currentPage.withUpdatedLayer(layerId) { it.copy(name = newName) }
        updateCurrentPage(updated)
        _hasUnsavedChanges.value = true
    }

    // Pages
    fun addNewPage() {
        val newPage = PageEntity(canvasId = _canvas.value.id, pageIndex = _pages.value.size)
        _pages.value = _pages.value + newPage
        _currentPageIndex.value = _pages.value.lastIndex
        _hasUnsavedChanges.value = true
    }

    fun selectPage(index: Int) {
        if (index in _pages.value.indices) {
            _currentPageIndex.value = index
        }
    }

    fun createSnapshot(name: String = "Знімок") {
        _snapshots.value = _snapshots.value + name
    }

    fun deletePage(index: Int) {
        if (_pages.value.size > 1 && index in _pages.value.indices) {
            val updated = _pages.value.toMutableList().also { it.removeAt(index) }
            _pages.value = updated
            _currentPageIndex.value = (_currentPageIndex.value - 1).coerceAtLeast(0)
            _hasUnsavedChanges.value = true
        }
    }

    private fun updateCurrentPage(page: PageEntity) {
        val currentIdx = _currentPageIndex.value
        _pages.value = _pages.value.mapIndexed { idx, p -> if (idx == currentIdx) page else p }
    }

    fun saveProject() {
        autosaveManager.saveImmediately()
        _hasUnsavedChanges.value = false
    }

    fun onDispose() {
        sketchLinkServer.stop()
        autosaveManager.stopAutosave()
        audioRecorderManager.stopPlayback()
        scope.cancel()
    }
}
