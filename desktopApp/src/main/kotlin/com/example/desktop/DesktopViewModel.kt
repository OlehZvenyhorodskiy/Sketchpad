package com.example.desktop

import com.example.desktop.theme.AppThemeStyle
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

    private val _symmetryMode = MutableStateFlow(SymmetryMode.NONE)
    val symmetryMode: StateFlow<SymmetryMode> = _symmetryMode.asStateFlow()

    private val _activeLayerId = MutableStateFlow("default")
    val activeLayerId: StateFlow<String> = _activeLayerId.asStateFlow()

    // Viewport & Pan/Zoom
    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Pair(0f, 0f))
    val panOffset: StateFlow<Pair<Float, Float>> = _panOffset.asStateFlow()

    // UI Dialogs
    private val _showLayersPanel = MutableStateFlow(false)
    val showLayersPanel: StateFlow<Boolean> = _showLayersPanel.asStateFlow()

    private val _showPairingDialog = MutableStateFlow(false)
    val showPairingDialog: StateFlow<Boolean> = _showPairingDialog.asStateFlow()

    private val _showPerformanceOverlay = MutableStateFlow(false)
    val showPerformanceOverlay: StateFlow<Boolean> = _showPerformanceOverlay.asStateFlow()

    private val _whiteCanvasMode = MutableStateFlow(false)
    val whiteCanvasMode: StateFlow<Boolean> = _whiteCanvasMode.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppThemeStyle.SYSTEM_DEFAULT)
    val currentTheme: StateFlow<AppThemeStyle> = _currentTheme.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Reference Image (Semi-transparent background underlay)
    private val _referenceImagePath = MutableStateFlow<String?>(null)
    val referenceImagePath: StateFlow<String?> = _referenceImagePath.asStateFlow()

    private val _referenceImageOpacity = MutableStateFlow(0.4f)
    val referenceImageOpacity: StateFlow<Float> = _referenceImageOpacity.asStateFlow()

    // Version History Snapshots
    private val _snapshots = MutableStateFlow<List<Pair<String, PageEntity>>>(emptyList())
    val snapshots: StateFlow<List<Pair<String, PageEntity>>> = _snapshots.asStateFlow()

    // Undo / Redo Command Stacks
    private val undoStack = ArrayDeque<CanvasCommand>()
    private val redoStack = ArrayDeque<CanvasCommand>()

    // Performance Metrics
    private val _fps = MutableStateFlow(60)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _strokeLatencyMs = MutableStateFlow(4L)
    val strokeLatencyMs: StateFlow<Long> = _strokeLatencyMs.asStateFlow()

    private val _packetsPerSec = MutableStateFlow(0)
    val packetsPerSec: StateFlow<Int> = _packetsPerSec.asStateFlow()

    // SketchLink Server
    val sketchLinkServer = SketchLinkServer(port = 8765)
    private var packetCount = 0

    init {
        sketchLinkServer.start()
        listenToSketchLinkPackets()
        startMetricsLoop()
    }

    private fun listenToSketchLinkPackets() {
        scope.launch {
            sketchLinkServer.incomingPackets.collect { packet ->
                packetCount++
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
                    SketchLinkPacketType.CLEAR_CANVAS -> {
                        clearCurrentPage()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startMetricsLoop() {
        scope.launch {
            while (isActive) {
                delay(1000)
                _packetsPerSec.value = packetCount
                packetCount = 0
            }
        }
    }

    fun selectTool(tool: ToolType) { _currentTool.value = tool }
    fun setBrushSize(size: Float) { _brushSize.value = size.coerceIn(1f, 100f) }
    fun setBrushOpacity(opacity: Float) { _brushOpacity.value = opacity.coerceIn(0.05f, 1.0f) }
    fun setColor(color: HslaColor) {
        _currentColor.value = color
        if (color !in _recentColors.value) {
            _recentColors.value = (listOf(color) + _recentColors.value).take(8)
        }
    }
    fun setSymmetryMode(mode: SymmetryMode) { _symmetryMode.value = mode }
    fun toggleWhiteCanvasMode() { _whiteCanvasMode.value = !_whiteCanvasMode.value }
    fun toggleLayersPanel() { _showLayersPanel.value = !_showLayersPanel.value }
    fun togglePairingDialog() { _showPairingDialog.value = !_showPairingDialog.value }
    fun togglePerformanceOverlay() { _showPerformanceOverlay.value = !_showPerformanceOverlay.value }
    fun setTheme(theme: AppThemeStyle) { _currentTheme.value = theme }
    fun toggleDarkTheme() { _isDarkTheme.value = !_isDarkTheme.value }

    fun setZoomScale(scale: Float) { _zoomScale.value = scale.coerceIn(0.1f, 10.0f) }
    fun updatePan(dx: Float, dy: Float) {
        _panOffset.value = Pair(_panOffset.value.first + dx, _panOffset.value.second + dy)
    }

    fun setReferenceImage(path: String?, opacity: Float = 0.4f) {
        _referenceImagePath.value = path
        _referenceImageOpacity.value = opacity
    }

    fun createSnapshot(name: String = "Snapshot ${System.currentTimeMillis() % 10000}") {
        _snapshots.value = _snapshots.value + (name to currentPage)
    }

    fun restoreSnapshot(snapshot: PageEntity) {
        val updated = _pages.value.mapIndexed { idx, p ->
            if (idx == _currentPageIndex.value) snapshot else p
        }
        _pages.value = updated
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
    }

    fun commitStroke(stroke: StrokeEntity, canvasCenter: Pair<Float, Float> = Pair(960f, 540f)) {
        val symMode = _symmetryMode.value
        val strokesToAdd = DrawingMath.generateSymmetricStrokes(stroke, symMode, canvasCenter.first, canvasCenter.second)

        strokesToAdd.forEach { s ->
            val cmd = AddStrokeCommand(_activeLayerId.value, s)
            executeCommand(cmd)
        }
    }

    fun executeCommand(command: CanvasCommand) {
        val updated = command.execute(currentPage)
        updateCurrentPage(updated)
        undoStack.addLast(command)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val cmd = undoStack.removeLast()
            val updated = cmd.undo(currentPage)
            updateCurrentPage(updated)
            redoStack.addLast(cmd)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val cmd = redoStack.removeLast()
            val updated = cmd.execute(currentPage)
            updateCurrentPage(updated)
            undoStack.addLast(cmd)
        }
    }

    fun clearCurrentPage() {
        val updated = currentPage.withUpdatedLayer(_activeLayerId.value) { layer ->
            layer.copy(strokes = emptyList(), shapes = emptyList(), textBlocks = emptyList())
        }
        updateCurrentPage(updated)
    }

    // Layers
    fun addLayer(name: String = "Шар ${_pages.value.first().getEffectiveLayers().size + 1}") {
        val newLayer = LayerEntity(name = name)
        val updated = currentPage.copy(layers = currentPage.getEffectiveLayers() + newLayer, activeLayerId = newLayer.id)
        _activeLayerId.value = newLayer.id
        updateCurrentPage(updated)
    }

    fun deleteLayer(layerId: String) {
        val layers = currentPage.getEffectiveLayers()
        if (layers.size <= 1) return
        val updatedLayers = layers.filterNot { it.id == layerId }
        val updated = currentPage.copy(layers = updatedLayers, activeLayerId = updatedLayers.first().id)
        _activeLayerId.value = updatedLayers.first().id
        updateCurrentPage(updated)
    }

    fun setActiveLayer(layerId: String) {
        _activeLayerId.value = layerId
        updateCurrentPage(currentPage.copy(activeLayerId = layerId))
    }

    fun toggleLayerVisibility(layerId: String) {
        val updated = currentPage.withUpdatedLayer(layerId) { it.copy(isVisible = !it.isVisible) }
        updateCurrentPage(updated)
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val updated = currentPage.withUpdatedLayer(layerId) { it.copy(opacity = opacity.coerceIn(0f, 1f)) }
        updateCurrentPage(updated)
    }

    fun setLayerBlendMode(layerId: String, blendMode: BlendMode) {
        val updated = currentPage.withUpdatedLayer(layerId) { it.copy(blendMode = blendMode) }
        updateCurrentPage(updated)
    }

    // Pages
    fun addNewPage() {
        val newPage = PageEntity(canvasId = _canvas.value.id, pageIndex = _pages.value.size)
        _pages.value = _pages.value + newPage
        _currentPageIndex.value = _pages.value.lastIndex
    }

    fun selectPage(index: Int) {
        if (index in _pages.value.indices) {
            _currentPageIndex.value = index
        }
    }

    private fun updateCurrentPage(page: PageEntity) {
        val currentIdx = _currentPageIndex.value
        _pages.value = _pages.value.mapIndexed { idx, p -> if (idx == currentIdx) page else p }
    }

    fun onDispose() {
        sketchLinkServer.stop()
        scope.cancel()
    }
}
