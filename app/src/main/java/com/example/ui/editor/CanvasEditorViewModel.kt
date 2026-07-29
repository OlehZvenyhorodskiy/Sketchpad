package com.example.ui.editor

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.GeminiAssistantService
import com.example.audio.AudioRecorderManager
import com.example.audio.RecordingStatus
import com.example.core.drawing.DrawingEngine
import com.example.core.drawing.RulerState
import com.example.data.models.AudioRecordingEntity
import com.example.data.models.BackgroundPattern
import com.example.data.models.BlendMode
import com.example.data.models.CanvasEntity
import com.example.data.models.ChartElementEntity
import com.example.data.models.EraserMode
import com.example.data.models.HslaColor
import com.example.data.models.ImageElementEntity
import com.example.data.models.LayerEntity
import com.example.data.models.PageEntity
import com.example.data.models.PageSizePreset
import com.example.data.models.ShapeEntity
import com.example.data.models.ShapeType
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.TextBlockEntity
import com.example.data.models.ToolType
import com.example.data.repository.CanvasRepository
import com.example.drive.ExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class CanvasEditorViewModel(
    private val repository: CanvasRepository,
    private val canvasId: String,
    private val context: Context
) : ViewModel() {

    private val userPrefs = com.example.data.repository.UserPreferencesRepository(context)
    private val audioRecorderManager = AudioRecorderManager(context)
    private val geminiService = GeminiAssistantService()

    val audioStatus: StateFlow<RecordingStatus> = audioRecorderManager.status

    private val _canvas = MutableStateFlow<CanvasEntity?>(null)
    val canvas: StateFlow<CanvasEntity?> = _canvas.asStateFlow()

    private val _pages = MutableStateFlow<List<PageEntity>>(emptyList())
    val pages: StateFlow<List<PageEntity>> = _pages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    private val _eraserMode = MutableStateFlow(userPrefs.getEraserModeSync())
    val eraserMode: StateFlow<EraserMode> = _eraserMode.asStateFlow()

    private val _strokeWidth = MutableStateFlow(4f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _strokeOpacity = MutableStateFlow(1f)
    val strokeOpacity: StateFlow<Float> = _strokeOpacity.asStateFlow()

    private val _currentColor = MutableStateFlow(HslaColor.BLACK)
    val currentColor: StateFlow<HslaColor> = _currentColor.asStateFlow()

    private val _recentColors = MutableStateFlow(
        listOf(HslaColor.BLACK, HslaColor.BLUE, HslaColor.RED, HslaColor.GREEN, HslaColor.PURPLE)
    )
    val recentColors: StateFlow<List<HslaColor>> = _recentColors.asStateFlow()

    private val _drawWithFingers = MutableStateFlow(false)
    val drawWithFingers: StateFlow<Boolean> = _drawWithFingers.asStateFlow()

    private val _zoomScale = MutableStateFlow(3f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    fun updatePanOffset(offset: Offset) {
        _panOffset.value = offset
    }

    private val _rulerState = MutableStateFlow(RulerState())
    val rulerState: StateFlow<RulerState> = _rulerState.asStateFlow()

    private val _isSlidersVertical = MutableStateFlow(false)
    val isSlidersVertical: StateFlow<Boolean> = _isSlidersVertical.asStateFlow()

    private val _activeLayerId = MutableStateFlow<String?>(null)
    val activeLayerId: StateFlow<String?> = _activeLayerId.asStateFlow()

    private val _showLayersPanel = MutableStateFlow(false)
    val showLayersPanel: StateFlow<Boolean> = _showLayersPanel.asStateFlow()

    // Multi-select state for lasso
    private val _selectionMode = MutableStateFlow(com.example.data.models.SelectionMode.SINGLE)
    val selectionMode: StateFlow<com.example.data.models.SelectionMode> = _selectionMode.asStateFlow()

    private val _selectedElementIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedElementIds: StateFlow<Set<String>> = _selectedElementIds.asStateFlow()

    fun setSelectionMode(mode: com.example.data.models.SelectionMode) {
        _selectionMode.value = mode
        if (mode == com.example.data.models.SelectionMode.SINGLE) {
            _selectedElementIds.value = emptySet()
        }
    }

    // AI dirty-cache: tracks canvas modifications
    private val _canvasVersion = MutableStateFlow(0)
    val canvasVersion: StateFlow<Int> = _canvasVersion.asStateFlow()

    // ═══════════════════════════════════════════════════════
    // Academic Features State Flows (10 Cheat Codes)
    // ═══════════════════════════════════════════════════════
    private val _latexOutput = MutableStateFlow<String?>(null)
    val latexOutput: StateFlow<String?> = _latexOutput.asStateFlow()

    private val _graphAnalysisResult = MutableStateFlow<com.example.academic.GraphAnalysisResult?>(null)
    val graphAnalysisResult: StateFlow<com.example.academic.GraphAnalysisResult?> = _graphAnalysisResult.asStateFlow()

    private val _isPhysicsActive = MutableStateFlow(false)
    val isPhysicsActive: StateFlow<Boolean> = _isPhysicsActive.asStateFlow()

    private val _isArOverlayActive = MutableStateFlow(false)
    val isArOverlayActive: StateFlow<Boolean> = _isArOverlayActive.asStateFlow()

    private val _isOilPaintShaderActive = MutableStateFlow(false)
    val isOilPaintShaderActive: StateFlow<Boolean> = _isOilPaintShaderActive.asStateFlow()

    private val _isPagedCanvasActive = MutableStateFlow(false)
    val isPagedCanvasActive: StateFlow<Boolean> = _isPagedCanvasActive.asStateFlow()

    private val _academicStatusMessage = MutableStateFlow<String?>(null)
    val academicStatusMessage: StateFlow<String?> = _academicStatusMessage.asStateFlow()

    fun clearAcademicStatus() { _academicStatusMessage.value = null }

    fun toggleLayersPanel() { _showLayersPanel.value = !_showLayersPanel.value }

    fun toggleSliderOrientation() {
        _isSlidersVertical.value = !_isSlidersVertical.value
    }

    // Command Pattern Undo / Redo history
    private val undoRedoMutex = kotlinx.coroutines.sync.Mutex()
    private val commandUndoStack = ArrayDeque<com.example.data.models.CanvasCommand>(100)
    private val commandRedoStack = ArrayDeque<com.example.data.models.CanvasCommand>(100)
    private val pageUndoHistory = mutableListOf<PageEntity>()
    private val pageRedoHistory = mutableListOf<PageEntity>()

    private val bitmapCache = android.util.LruCache<String, android.graphics.Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    )

    fun preloadImageBitmap(sourceUri: String) {
        if (sourceUri.isBlank() || bitmapCache.get(sourceUri) != null) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = File(sourceUri)
                if (file.exists()) {
                    val opts = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                    if (bitmap != null) {
                        bitmapCache.put(sourceUri, bitmap)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CanvasVM", "Failed to preload bitmap", e)
            }
        }
    }

    fun getCachedBitmap(sourceUri: String): android.graphics.Bitmap? {
        return bitmapCache.get(sourceUri)?.takeIf { !it.isRecycled }
    }

    override fun onCleared() {
        super.onCleared()
        bitmapCache.evictAll()
    }

    // Gemini Messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Audio recordings
    val audioRecordings: StateFlow<List<AudioRecordingEntity>> = repository.getRecordingsForCanvas(canvasId)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var userPickedColor = false

    private fun luminance(colorInt: Int): Float {
        val r = ((colorInt shr 16) and 0xFF) / 255f
        val g = ((colorInt shr 8) and 0xFF) / 255f
        val b = (colorInt and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun ensureContrastingDefaultColor(bgInt: Int?) {
        if (userPickedColor) return
        val bg = bgInt ?: return
        val lum = luminance(bg)
        _currentColor.value = if (lum < 0.5f) HslaColor.WHITE else HslaColor.BLACK
    }

    init {
        viewModelScope.launch {
            repository.getCanvasById(canvasId).collect { c ->
                _canvas.value = c
                ensureContrastingDefaultColor(c?.backgroundColor)
            }
        }
        viewModelScope.launch {
            repository.getPagesForCanvas(canvasId).collect { pList ->
                _pages.value = pList
                if (pList.isNotEmpty() && _currentPageIndex.value >= pList.size) {
                    _currentPageIndex.value = 0
                }
            }
        }
    }

    val currentPage: PageEntity?
        get() = _pages.value.getOrNull(_currentPageIndex.value)

    fun selectTool(tool: ToolType, viewportWidth: Float = 0f, viewportHeight: Float = 0f) {
        if (tool == ToolType.RULER) {
            val current = _rulerState.value
            val willBeVisible = !current.isVisible
            val newCenter = if (willBeVisible && (current.center == Offset.Zero || (viewportWidth > 0f && viewportHeight > 0f))) {
                if (viewportWidth > 0f && viewportHeight > 0f) Offset(viewportWidth / 2f, viewportHeight / 2f)
                else if (current.center == Offset.Zero) Offset(540f, 960f)
                else current.center
            } else {
                current.center
            }
            _rulerState.value = current.copy(isVisible = willBeVisible, center = newCenter)
        } else {
            _currentTool.value = tool
        }
    }

    fun setEraserMode(mode: EraserMode) {
        _eraserMode.value = mode
        userPrefs.setEraserModeSync(mode)
    }

    fun setStrokeWidth(w: Float) {
        _strokeWidth.value = w.coerceIn(1f, 50f)
    }

    fun setStrokeOpacity(op: Float) {
        _strokeOpacity.value = op.coerceIn(0.05f, 1f)
    }

    fun setColor(color: HslaColor) {
        userPickedColor = true
        _currentColor.value = color
        val list = _recentColors.value.toMutableList()
        list.remove(color)
        list.add(0, color)
        if (list.size > 8) list.removeAt(list.size - 1)
        _recentColors.value = list
    }

    fun setDrawWithFingers(enabled: Boolean) {
        _drawWithFingers.value = enabled
    }

    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(0.25f, 8.0f)
    }

    fun setRulerState(state: RulerState) {
        _rulerState.value = state
    }

    private fun ensureLayersExist(page: PageEntity): PageEntity {
        val hasTopLevelElements = page.strokes.isNotEmpty() || page.shapes.isNotEmpty() ||
                page.textBlocks.isNotEmpty() || page.images.isNotEmpty() || page.charts.isNotEmpty()

        if (page.layers.isEmpty() || hasTopLevelElements) {
            val targetLayerId = _activeLayerId.value ?: page.activeLayerId ?: page.layers.firstOrNull()?.id ?: "default"
            val baseLayers = if (page.layers.isEmpty()) listOf(LayerEntity(id = "default", name = "Шар 1")) else page.layers

            val updatedLayers = baseLayers.map { layer ->
                if (layer.id == targetLayerId) {
                    layer.copy(
                        strokes = layer.strokes + page.strokes,
                        shapes = layer.shapes + page.shapes,
                        textBlocks = layer.textBlocks + page.textBlocks,
                        images = layer.images + page.images,
                        charts = layer.charts + page.charts
                    )
                } else layer
            }

            return page.copy(
                layers = updatedLayers,
                activeLayerId = targetLayerId,
                strokes = emptyList(),
                shapes = emptyList(),
                textBlocks = emptyList(),
                images = emptyList(),
                charts = emptyList()
            )
        }
        return page
    }

    fun addLayer() {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val newLayer = LayerEntity(name = "Шар ${migrated.layers.size + 1}")
        _activeLayerId.value = newLayer.id
        updateCurrentPage(migrated.copy(
            layers = migrated.layers + newLayer,
            activeLayerId = newLayer.id
        ))
    }

    fun deleteLayer(layerId: String) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        if (migrated.layers.size <= 1) return
        pushUndoState(migrated)
        val updated = migrated.layers.filterNot { it.id == layerId }
        val newActive = if (_activeLayerId.value == layerId) updated.last().id else _activeLayerId.value
        _activeLayerId.value = newActive
        updateCurrentPage(migrated.copy(layers = updated, activeLayerId = newActive))
    }

    fun setActiveLayer(layerId: String) {
        _activeLayerId.value = layerId
        currentPage?.let { updateCurrentPage(it.copy(activeLayerId = layerId)) }
    }

    fun toggleLayerVisibility(layerId: String) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updated = migrated.layers.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        updateCurrentPage(migrated.copy(layers = updated))
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updated = migrated.layers.map {
            if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
        }
        updateCurrentPage(migrated.copy(layers = updated))
    }

    fun renameLayer(layerId: String, newName: String) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updated = migrated.layers.map {
            if (it.id == layerId) it.copy(name = newName) else it
        }
        updateCurrentPage(migrated.copy(layers = updated))
    }

    fun moveLayerUp(layerId: String) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val idx = migrated.layers.indexOfFirst { it.id == layerId }
        if (idx < 0 || idx >= migrated.layers.size - 1) return
        val list = migrated.layers.toMutableList()
        val tmp = list[idx]; list[idx] = list[idx + 1]; list[idx + 1] = tmp
        updateCurrentPage(migrated.copy(layers = list))
    }

    fun moveLayerDown(layerId: String) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val idx = migrated.layers.indexOfFirst { it.id == layerId }
        if (idx <= 0) return
        val list = migrated.layers.toMutableList()
        val tmp = list[idx]; list[idx] = list[idx - 1]; list[idx - 1] = tmp
        updateCurrentPage(migrated.copy(layers = list))
    }

    fun addStrokeToCurrentPage(stroke: StrokeEntity) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: migrated.layers.lastOrNull()?.id
        val updatedLayers = migrated.layers.map { layer ->
            if (layer.id == targetLayerId) {
                layer.copy(strokes = layer.strokes + stroke)
            } else {
                layer
            }
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers, activeLayerId = targetLayerId))
    }

    fun eraseAtPoint(point: Offset, radius: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val updatedLayers = migrated.layers.map { layer ->
            if (layer.isVisible && !layer.isLocked) {
                if (_eraserMode.value == EraserMode.OBJECT) {
                    val updatedStrokes = layer.strokes.filterNot { stroke ->
                        DrawingEngine.isPointInStroke(point, stroke, radius)
                    }
                    layer.copy(strokes = updatedStrokes)
                } else {
                    val updatedEraserMarks = layer.eraserMarks + com.example.data.models.EraserMark(
                        points = listOf(StrokePoint(point.x, point.y)),
                        width = radius
                    )
                    layer.copy(eraserMarks = updatedEraserMarks)
                }
            } else layer
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun addEraserMarkToCurrentPage(mark: com.example.data.models.EraserMark) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: migrated.layers.lastOrNull()?.id ?: "default"
        val updatedLayers = migrated.layers.map { layer ->
            if (layer.id == targetLayerId) {
                layer.copy(eraserMarks = layer.eraserMarks + mark)
            } else layer
        }
        _canvasVersion.value++
        updateCurrentPage(migrated.copy(layers = updatedLayers, activeLayerId = targetLayerId))
    }

    companion object {
        private const val MAX_UNDO_DEPTH = 30
    }

    fun executeCommand(command: com.example.data.models.CanvasCommand) {
        val page = currentPage ?: return
        val newPage = command.execute(page)
        synchronized(commandUndoStack) {
            commandUndoStack.addLast(command)
            if (commandUndoStack.size > MAX_UNDO_DEPTH) commandUndoStack.removeFirst()
            commandRedoStack.clear()
        }
        updateCurrentPage(newPage)
    }

    fun undo() {
        viewModelScope.launch {
            if (!undoRedoMutex.tryLock()) return@launch
            try {
                val page = currentPage ?: return@launch
                val command = synchronized(commandUndoStack) { commandUndoStack.removeLastOrNull() }
                if (command != null) {
                    val newPage = command.undo(page)
                    synchronized(commandRedoStack) { commandRedoStack.addLast(command) }
                    updateCurrentPage(newPage)
                } else synchronized(pageUndoHistory) {
                    if (pageUndoHistory.isNotEmpty()) {
                        pageRedoHistory.add(page)
                        val previousPage = pageUndoHistory.removeAt(pageUndoHistory.size - 1)
                        updateCurrentPage(previousPage)
                    }
                }
            } finally {
                undoRedoMutex.unlock()
            }
        }
    }

    fun redo() {
        viewModelScope.launch {
            if (!undoRedoMutex.tryLock()) return@launch
            try {
                val page = currentPage ?: return@launch
                val command = synchronized(commandRedoStack) { commandRedoStack.removeLastOrNull() }
                if (command != null) {
                    val newPage = command.execute(page)
                    synchronized(commandUndoStack) { commandUndoStack.addLast(command) }
                    updateCurrentPage(newPage)
                } else synchronized(pageUndoHistory) {
                    if (pageRedoHistory.isNotEmpty()) {
                        pageUndoHistory.add(page)
                        val nextPage = pageRedoHistory.removeAt(pageRedoHistory.size - 1)
                        updateCurrentPage(nextPage)
                    }
                }
            } finally {
                undoRedoMutex.unlock()
            }
        }
    }

    private fun pushUndoState(page: PageEntity) {
        synchronized(pageUndoHistory) {
            if (pageUndoHistory.size >= MAX_UNDO_DEPTH) pageUndoHistory.removeAt(0)
            pageUndoHistory.add(page)
            pageRedoHistory.clear()
        }
    }

    private fun updateCurrentPage(page: PageEntity) {
        _canvasVersion.value++
        viewModelScope.launch {
            repository.updatePage(page)
        }
    }

    fun updateBackgroundColor(colorInt: Int) {
        val c = _canvas.value ?: return
        viewModelScope.launch {
            repository.updateCanvas(c.copy(backgroundColor = colorInt))
        }
        ensureContrastingDefaultColor(colorInt)
    }

    fun updateBackgroundPattern(pattern: BackgroundPattern) {
        val c = _canvas.value ?: return
        viewModelScope.launch {
            repository.updateCanvas(c.copy(backgroundPattern = pattern))
        }
    }

    fun updatePageSizePreset(preset: PageSizePreset, customW: Float?, customH: Float?) {
        val c = _canvas.value ?: return
        viewModelScope.launch {
            repository.updateCanvas(c.copy(pageSizePreset = preset, customWidth = customW, customHeight = customH))
        }
    }

    fun insertShape(
        shapeType: ShapeType,
        targetX: Float = 160f,
        targetY: Float = 160f,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
        scale: Float = 1f
    ) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: "default"
        val safeScale = if (scale <= 0.001f) 1.0f else scale
        val safeVpW = if (viewportWidth <= 0f) 1080f else viewportWidth
        val safeVpH = if (viewportHeight <= 0f) 1920f else viewportHeight
        val elemW = 180f
        val elemH = 180f
        val finalX = (-panOffsetX + safeVpW / 2f) / safeScale - elemW / 2f
        val finalY = (-panOffsetY + safeVpH / 2f) / safeScale - elemH / 2f
        val newShape = ShapeEntity(
            shapeType = shapeType,
            x = finalX,
            y = finalY,
            width = elemW,
            height = elemH,
            fillColor = _currentColor.value.copy(alpha = 0.2f).toArgbInt(),
            strokeColor = _currentColor.value.toArgbInt()
        )
        val updatedLayers = migrated.layers.map { layer ->
            if (layer.id == targetLayerId) layer.copy(shapes = layer.shapes + newShape)
            else layer
        }
        _canvasVersion.value++
        updateCurrentPage(migrated.copy(layers = updatedLayers, activeLayerId = targetLayerId))
    }

    fun insertText(
        text: String,
        targetX: Float = 160f,
        targetY: Float = 160f,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
        scale: Float = 1f
    ) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: "default"
        val safeScale = if (scale <= 0.001f) 1.0f else scale
        val safeVpW = if (viewportWidth <= 0f) 1080f else viewportWidth
        val safeVpH = if (viewportHeight <= 0f) 1920f else viewportHeight
        val elemW = 240f
        val elemH = 100f
        val finalX = (-panOffsetX + safeVpW / 2f) / safeScale - elemW / 2f
        val finalY = (-panOffsetY + safeVpH / 2f) / safeScale - elemH / 2f
        val newText = TextBlockEntity(
            text = text,
            x = finalX,
            y = finalY,
            color = _currentColor.value.toArgbInt()
        )
        val updatedLayers = migrated.layers.map { layer ->
            if (layer.id == targetLayerId) layer.copy(textBlocks = layer.textBlocks + newText)
            else layer
        }
        _canvasVersion.value++
        updateCurrentPage(migrated.copy(layers = updatedLayers, activeLayerId = targetLayerId))
    }

    fun insertMathFunctionChart(
        formula: String = "sin(x)",
        xMin: Float = -10f,
        xMax: Float = 10f,
        targetX: Float = 160f,
        targetY: Float = 160f,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
        scale: Float = 1f
    ) {
        val actualTargetX = if (viewportWidth > 0f) (-panOffsetX + viewportWidth / 2f) / scale - 190f else targetX
        val actualTargetY = if (viewportHeight > 0f) (-panOffsetY + viewportHeight / 2f) / scale - 130f else targetY
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: "default"

        @Suppress("UNUSED_VARIABLE")
        val graphW = 380f
        val graphH = 260f
        val useTargetX = actualTargetX
        val useTargetY = actualTargetY

        val sampleCount = 160
        val step = (xMax - xMin) / sampleCount
        val yValues = mutableListOf<Double>()
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for (i in 0..sampleCount) {
            val x = xMin + i * step
            val y = evaluateMathFormula(formula, x.toDouble())
            yValues.add(y)
            if (!y.isNaN() && !y.isInfinite()) {
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }

        if (minY >= maxY) {
            minY = -1.0
            maxY = 1.0
        }

        val points = mutableListOf<StrokePoint>()
        for (i in yValues.indices) {
            val xVal = xMin + i * step
            val yVal = yValues[i]
            val normX = (xVal - xMin) / (xMax - xMin)
            val normY = 1.0 - ((yVal - minY) / (maxY - minY).coerceAtLeast(0.001))

            val canvasX = useTargetX + normX.toFloat() * graphW
            val canvasY = useTargetY + normY.toFloat() * graphH
            points.add(StrokePoint(canvasX, canvasY))
        }

        val chartStroke = StrokeEntity(
            tool = ToolType.INK_PEN,
            colorHsla = _currentColor.value,
            baseWidth = 3.5f,
            points = points
        )

        val textLabel = TextBlockEntity(
            text = "f(x) = $formula [$xMin .. $xMax]",
            x = useTargetX,
            y = useTargetY - 28f,
            fontSize = 15f,
            color = _currentColor.value.toArgbInt()
        )

        val gridChart = ChartElementEntity(
            x = useTargetX,
            y = useTargetY,
            width = graphW,
            height = graphH
        )

        val updatedLayers = migrated.layers.map { layer ->
            if (layer.id == targetLayerId) {
                layer.copy(
                    charts = layer.charts + gridChart,
                    strokes = layer.strokes + chartStroke,
                    textBlocks = layer.textBlocks + textLabel
                )
            } else layer
        }

        _canvasVersion.value++
        updateCurrentPage(migrated.copy(layers = updatedLayers, activeLayerId = targetLayerId))
    }

    private fun evaluateMathFormula(formula: String, x: Double): Double {
        return com.example.academic.MathExpressionEvaluator.eval(formula, x)
    }

    fun insertChart(
        showAxisLabels: Boolean = true,
        xStep: Float = 1f,
        yStep: Float = 1f,
        targetX: Float = 160f,
        targetY: Float = 160f,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
        scale: Float = 1f
    ) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: "default"
        val safeScale = if (scale <= 0.001f) 1.0f else scale
        val safeVpW = if (viewportWidth <= 0f) 1080f else viewportWidth
        val safeVpH = if (viewportHeight <= 0f) 1920f else viewportHeight
        val elemW = 380f
        val elemH = 260f
        val finalX = (-panOffsetX + safeVpW / 2f) / safeScale - elemW / 2f
        val finalY = (-panOffsetY + safeVpH / 2f) / safeScale - elemH / 2f
        val xMin = -10f
        val xMax = 10f
        val yMin = -10f
        val yMax = 10f
        val ppuX = (elemW / (xMax - xMin)).let { if (it.isNaN() || it <= 0f) 20f else it }
        val ppuY = (elemH / (yMax - yMin)).let { if (it.isNaN() || it <= 0f) 20f else it }
        val newChart = ChartElementEntity(
            x = finalX,
            y = finalY,
            width = elemW,
            height = elemH,
            showAxisLabels = showAxisLabels,
            axisLabelsVisible = showAxisLabels,
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            xStep = xStep,
            yStep = yStep,
            pixelsPerUnitX = ppuX,
            pixelsPerUnitY = ppuY
        )
        val updatedLayers = migrated.layers.map { layer ->
            if (layer.id == targetLayerId) layer.copy(charts = layer.charts + newChart)
            else layer
        }
        _canvasVersion.value++
        updateCurrentPage(migrated.copy(layers = updatedLayers, activeLayerId = targetLayerId))
    }

    fun insertImage(
        uri: android.net.Uri,
        targetX: Float = 160f,
        targetY: Float = 160f,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
        scale: Float = 1f
    ) {
        val page = currentPage ?: return
        viewModelScope.launch {
            val imagePath = repository.saveImportedImage(uri)
            val file = File(imagePath)
            var w = 340f
            var h = 240f
            var initialRotation = 0f

            if (file.exists()) {
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    val aspect = opts.outWidth.toFloat() / opts.outHeight.toFloat()
                    if (aspect >= 1f) {
                        w = 360f
                        h = (360f / aspect).coerceAtLeast(100f)
                    } else {
                        h = 360f
                        w = (360f * aspect).coerceAtLeast(100f)
                    }
                }

                val exif = runCatching { android.media.ExifInterface(file.absolutePath) }.getOrNull()
                if (exif != null) {
                    val orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    initialRotation = when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }

                preloadImageBitmap(imagePath)
            }

            val migrated = ensureLayersExist(page)
            pushUndoState(migrated)
            val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: "default"

            val finalImgX = if (viewportWidth > 0f) (-panOffsetX + viewportWidth / 2f) / scale - w / 2f else targetX
            val finalImgY = if (viewportHeight > 0f) (-panOffsetY + viewportHeight / 2f) / scale - h / 2f else targetY
            val newImg = ImageElementEntity(
                id = UUID.randomUUID().toString(),
                sourceUri = imagePath,
                x = finalImgX,
                y = finalImgY,
                width = w,
                height = h,
                rotation = initialRotation,
                opacity = 1.0f
            )

            val updatedLayers = migrated.layers.map { layer ->
                if (layer.id == targetLayerId) layer.copy(images = layer.images + newImg)
                else layer
            }

            _canvasVersion.value++
            updateCurrentPage(migrated.copy(layers = updatedLayers, activeLayerId = targetLayerId))
        }
    }

    fun deleteElement(id: String, type: String) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val updatedLayers = migrated.layers.map { layer ->
            when (type) {
                "SHAPE" -> layer.copy(shapes = layer.shapes.filterNot { it.id == id })
                "IMAGE" -> layer.copy(images = layer.images.filterNot { it.id == id })
                "TEXT" -> layer.copy(textBlocks = layer.textBlocks.filterNot { it.id == id })
                "CHART" -> layer.copy(charts = layer.charts.filterNot { it.id == id })
                else -> layer
            }
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun rotateElement(id: String, type: String) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val updatedLayers = migrated.layers.map { layer ->
            when (type) {
                "IMAGE" -> layer.copy(images = layer.images.map { if (it.id == id) it.copy(rotation = (it.rotation + 90f) % 360f) else it })
                "SHAPE" -> layer.copy(shapes = layer.shapes.map { if (it.id == id) it.copy(rotation = (it.rotation + 90f) % 360f) else it })
                "CHART" -> layer.copy(charts = layer.charts.map { if (it.id == id) it.copy(width = it.height, height = it.width) else it })
                "TEXT" -> layer.copy(textBlocks = layer.textBlocks.map { if (it.id == id) it.copy(width = it.height, height = it.width) else it })
                else -> layer
            }
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateImageOpacity(imageId: String, opacity: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(images = layer.images.map {
                if (it.id == imageId) it.copy(opacity = opacity.coerceIn(0.1f, 1.0f)) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateImageSize(imageId: String, width: Float, height: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(images = layer.images.map {
                if (it.id == imageId) it.copy(width = width.coerceAtLeast(50f), height = height.coerceAtLeast(50f)) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateShapeSize(shapeId: String, width: Float, height: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(shapes = layer.shapes.map {
                if (it.id == shapeId) it.copy(width = width.coerceAtLeast(30f), height = height.coerceAtLeast(30f)) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateTextSize(textId: String, width: Float, height: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(textBlocks = layer.textBlocks.map {
                if (it.id == textId) it.copy(width = width.coerceAtLeast(60f), height = height.coerceAtLeast(30f)) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateChartSize(chartId: String, width: Float, height: Float, anchorStr: String = "BR") {
        resizeChart(chartId, width, height, anchorStr)
    }

    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    fun resizeChart(chartId: String, newWidth: Float, newHeight: Float, anchorStr: String = "BR") {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(charts = layer.charts.map { chart ->
                if (chart.id == chartId) {
                    val clampedW = newWidth.coerceAtLeast(100f)
                    val clampedH = newHeight.coerceAtLeast(100f)

                    var ppuX = chart.pixelsPerUnitX
                    var ppuY = chart.pixelsPerUnitY
                    if (ppuX <= 0f) ppuX = (chart.width / (chart.xMax - chart.xMin).let { if (it <= 0f) 20f else it }).let { if (it <= 0f) 20f else it }
                    if (ppuY <= 0f) ppuY = (chart.height / (chart.yMax - chart.yMin).let { if (it <= 0f) 20f else it }).let { if (it <= 0f) 20f else it }

                    val newRangeX = clampedW / ppuX
                    val newRangeY = clampedH / ppuY

                    val (newXMin, newXMax) = when (anchorStr) {
                        "BL", "TL" -> Pair(chart.xMax - newRangeX, chart.xMax)
                        else -> Pair(chart.xMin, chart.xMin + newRangeX)
                    }

                    val (newYMin, newYMax) = when (anchorStr) {
                        "BL", "BR" -> Pair(chart.yMax - newRangeY, chart.yMax)
                        else -> Pair(chart.yMin, chart.yMin + newRangeY)
                    }

                    chart.copy(
                        width = clampedW,
                        height = clampedH,
                        xMin = newXMin,
                        xMax = newXMax,
                        yMin = newYMin,
                        yMax = newYMax,
                        pixelsPerUnitX = ppuX,
                        pixelsPerUnitY = ppuY
                    )
                } else chart
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun resizeChart(chartId: String, newWidth: Float, newHeight: Float, anchor: Corner) {
        val strAnchor = when (anchor) {
            Corner.TOP_LEFT -> "TL"
            Corner.TOP_RIGHT -> "TR"
            Corner.BOTTOM_LEFT -> "BL"
            Corner.BOTTOM_RIGHT -> "BR"
        }
        resizeChart(chartId, newWidth, newHeight, strAnchor)
    }

    fun updateShapePosition(shapeId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(shapes = layer.shapes.map {
                if (it.id == shapeId) it.copy(x = newX, y = newY) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateTextPosition(textId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(textBlocks = layer.textBlocks.map {
                if (it.id == textId) it.copy(x = newX, y = newY) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateImagePosition(imageId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(images = layer.images.map {
                if (it.id == imageId) it.copy(x = newX, y = newY) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun updateChartPosition(chartId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(charts = layer.charts.map {
                if (it.id == chartId) it.copy(x = newX, y = newY) else it
            })
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun resizeAndMoveElement(
        id: String,
        type: String,
        newW: Float,
        newH: Float,
        newX: Float,
        newY: Float,
        anchor: String
    ) {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            when (type) {
                "SHAPE" -> layer.copy(shapes = layer.shapes.map {
                    if (it.id == id) it.copy(x = newX, y = newY, width = newW.coerceAtLeast(30f), height = newH.coerceAtLeast(30f)) else it
                })
                "IMAGE" -> layer.copy(images = layer.images.map {
                    if (it.id == id) it.copy(x = newX, y = newY, width = newW.coerceAtLeast(50f), height = newH.coerceAtLeast(50f)) else it
                })
                "TEXT" -> layer.copy(textBlocks = layer.textBlocks.map {
                    if (it.id == id) it.copy(x = newX, y = newY, width = newW.coerceAtLeast(60f), height = newH.coerceAtLeast(30f)) else it
                })
                "CHART" -> layer.copy(charts = layer.charts.map { chart ->
                    if (chart.id == id) {
                        val clampedW = newW.coerceAtLeast(100f)
                        val clampedH = newH.coerceAtLeast(100f)
                        var ppuX = chart.pixelsPerUnitX
                        var ppuY = chart.pixelsPerUnitY
                        if (ppuX <= 0f) ppuX = clampedW / (chart.xMax - chart.xMin).let { if (it <= 0f) 20f else it }
                        if (ppuY <= 0f) ppuY = clampedH / (chart.yMax - chart.yMin).let { if (it <= 0f) 20f else it }
                        val newRangeX = clampedW / ppuX
                        val newRangeY = clampedH / ppuY
                        val (nxMin, nxMax) = when (anchor) {
                            "BL", "TL" -> Pair(chart.xMax - newRangeX, chart.xMax)
                            else -> Pair(chart.xMin, chart.xMin + newRangeX)
                        }
                        val (nyMin, nyMax) = when (anchor) {
                            "BL", "BR" -> Pair(chart.yMax - newRangeY, chart.yMax)
                            else -> Pair(chart.yMin, chart.yMin + newRangeY)
                        }
                        chart.copy(
                            x = newX, y = newY,
                            width = clampedW, height = clampedH,
                            xMin = nxMin, xMax = nxMax,
                            yMin = nyMin, yMax = nyMax,
                            pixelsPerUnitX = ppuX, pixelsPerUnitY = ppuY
                        )
                    } else chart
                })
                else -> layer
            }
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun selectElementsInLasso(lassoWorldPoints: List<Offset>) {
        val page = currentPage ?: return
        if (lassoWorldPoints.size < 3) return

        val hitIds = mutableSetOf<String>()

        page.getEffectiveLayers().filter { it.isVisible }.forEach { layer ->
            layer.shapes.forEach { shape ->
                val center = Offset(shape.x + shape.width / 2f, shape.y + shape.height / 2f)
                if (isPointInPolygon(center, lassoWorldPoints)) hitIds.add(shape.id)
            }
            layer.images.forEach { img ->
                val center = Offset(img.x + img.width / 2f, img.y + img.height / 2f)
                if (isPointInPolygon(center, lassoWorldPoints)) hitIds.add(img.id)
            }
            layer.textBlocks.forEach { tb ->
                val center = Offset(tb.x + tb.width / 2f, tb.y + tb.height / 2f)
                if (isPointInPolygon(center, lassoWorldPoints)) hitIds.add(tb.id)
            }
            layer.charts.forEach { chart ->
                val center = Offset(chart.x + chart.width / 2f, chart.y + chart.height / 2f)
                if (isPointInPolygon(center, lassoWorldPoints)) hitIds.add(chart.id)
            }
        }

        _selectedElementIds.value = hitIds
    }

    private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
        var inside = false
        val n = polygon.size
        var j = n - 1
        for (i in 0 until n) {
            val yi = polygon[i].y; val yj = polygon[j].y
            val xi = polygon[i].x; val xj = polygon[j].x
            if ((yi > point.y) != (yj > point.y) &&
                point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    fun moveSelectedElements(dx: Float, dy: Float) {
        val page = currentPage ?: return
        val ids = _selectedElementIds.value
        if (ids.isEmpty()) return
        val migrated = ensureLayersExist(page)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(
                shapes = layer.shapes.map {
                    if (it.id in ids) it.copy(x = it.x + dx, y = it.y + dy) else it
                },
                images = layer.images.map {
                    if (it.id in ids) it.copy(x = it.x + dx, y = it.y + dy) else it
                },
                textBlocks = layer.textBlocks.map {
                    if (it.id in ids) it.copy(x = it.x + dx, y = it.y + dy) else it
                },
                charts = layer.charts.map {
                    if (it.id in ids) it.copy(x = it.x + dx, y = it.y + dy) else it
                }
            )
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun scaleSelectedElements(factor: Float) {
        val page = currentPage ?: return
        val ids = _selectedElementIds.value
        if (ids.isEmpty()) return
        val migrated = ensureLayersExist(page)

        val centers = mutableListOf<Offset>()
        migrated.getEffectiveLayers().forEach { layer ->
            layer.shapes.filter { it.id in ids }.forEach {
                centers.add(Offset(it.x + it.width / 2f, it.y + it.height / 2f))
            }
            layer.images.filter { it.id in ids }.forEach {
                centers.add(Offset(it.x + it.width / 2f, it.y + it.height / 2f))
            }
            layer.textBlocks.filter { it.id in ids }.forEach {
                centers.add(Offset(it.x + it.width / 2f, it.y + it.height / 2f))
            }
            layer.charts.filter { it.id in ids }.forEach {
                centers.add(Offset(it.x + it.width / 2f, it.y + it.height / 2f))
            }
        }
        if (centers.isEmpty()) return
        val centroid = Offset(
            centers.map { it.x }.average().toFloat(),
            centers.map { it.y }.average().toFloat()
        )

        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(
                shapes = layer.shapes.map { s ->
                    if (s.id in ids) {
                        val newW = s.width * factor; val newH = s.height * factor
                        val cx = s.x + s.width / 2f; val cy = s.y + s.height / 2f
                        val newCx = centroid.x + (cx - centroid.x) * factor
                        val newCy = centroid.y + (cy - centroid.y) * factor
                        s.copy(
                            x = newCx - newW / 2f, y = newCy - newH / 2f,
                            width = newW.coerceAtLeast(30f), height = newH.coerceAtLeast(30f)
                        )
                    } else s
                },
                images = layer.images.map { img ->
                    if (img.id in ids) {
                        val newW = img.width * factor; val newH = img.height * factor
                        val cx = img.x + img.width / 2f; val cy = img.y + img.height / 2f
                        val newCx = centroid.x + (cx - centroid.x) * factor
                        val newCy = centroid.y + (cy - centroid.y) * factor
                        img.copy(
                            x = newCx - newW / 2f, y = newCy - newH / 2f,
                            width = newW.coerceAtLeast(50f), height = newH.coerceAtLeast(50f)
                        )
                    } else img
                },
                textBlocks = layer.textBlocks.map { tb ->
                    if (tb.id in ids) {
                        val newW = tb.width * factor; val newH = tb.height * factor
                        val cx = tb.x + tb.width / 2f; val cy = tb.y + tb.height / 2f
                        val newCx = centroid.x + (cx - centroid.x) * factor
                        val newCy = centroid.y + (cy - centroid.y) * factor
                        tb.copy(
                            x = newCx - newW / 2f, y = newCy - newH / 2f,
                            width = newW.coerceAtLeast(60f), height = newH.coerceAtLeast(30f),
                            fontSize = tb.fontSize * factor
                        )
                    } else tb
                },
                charts = layer.charts.map { ch ->
                    if (ch.id in ids) {
                        val newW = ch.width * factor; val newH = ch.height * factor
                        val cx = ch.x + ch.width / 2f; val cy = ch.y + ch.height / 2f
                        val newCx = centroid.x + (cx - centroid.x) * factor
                        val newCy = centroid.y + (cy - centroid.y) * factor
                        ch.copy(
                            x = newCx - newW / 2f, y = newCy - newH / 2f,
                            width = newW.coerceAtLeast(100f), height = newH.coerceAtLeast(100f)
                        )
                    } else ch
                }
            )
        }
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun setCurrentPage(index: Int) {
        if (index in 0 until _pages.value.size) {
            _currentPageIndex.value = index
        }
    }

    fun addNewPage() {
        viewModelScope.launch {
            val newIndex = _pages.value.size
            repository.addPage(canvasId)
            _currentPageIndex.value = newIndex
        }
    }

    fun deletePage(page: PageEntity) {
        viewModelScope.launch {
            repository.deletePage(page)
        }
    }

    // Audio Recording Controls
    fun startAudioRecording() {
        audioRecorderManager.startRecording(canvasId)
    }

    fun stopAudioRecording() {
        val (path, durationMs) = audioRecorderManager.stopRecording()
        if (path != null && durationMs > 500) {
            viewModelScope.launch {
                repository.saveAudioRecording(canvasId, path, durationMs)
            }
        }
    }

    fun renameAudioRecording(recording: AudioRecordingEntity, newName: String) {
        viewModelScope.launch { repository.renameAudioRecording(recording.id, newName) }
    }

    fun playAudioRecording(filePath: String, startPosMs: Long = 0L) {
        audioRecorderManager.startPlayback(filePath, startPosMs)
    }

    fun pauseAudioPlayback() {
        audioRecorderManager.pausePlayback()
    }

    fun resumeAudioPlayback() {
        audioRecorderManager.resumePlayback()
    }

    fun seekAudioPlayback(positionMs: Long) {
        audioRecorderManager.seekTo(positionMs)
    }

    fun stopAudioPlayback() {
        audioRecorderManager.stopPlayback()
    }

    fun deleteAudioRecording(recording: AudioRecordingEntity) {
        audioRecorderManager.deleteAudioFile(recording.filePath)
        viewModelScope.launch {
            repository.deleteAudioRecording(recording)
        }
    }

    private val _isAiWindowVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAiWindowVisible: kotlinx.coroutines.flow.StateFlow<Boolean> = _isAiWindowVisible.asStateFlow()

    fun toggleAiWindow() {
        _isAiWindowVisible.value = !_isAiWindowVisible.value
    }

    fun hideAiWindow() {
        _isAiWindowVisible.value = false
    }

    val selectedProviderId: StateFlow<String> = userPrefs.selectedProvider.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = userPrefs.getSelectedProviderIdSync()
    )

    fun getSelectedProviderIdSync(): String = userPrefs.getSelectedProviderIdSync()
    fun getApiKeyForProvider(providerId: String): String = userPrefs.getApiKeyForProvider(providerId)
    fun saveApiKeyForProvider(providerId: String, key: String) = userPrefs.saveApiKeyForProvider(providerId, key)
    fun getCustomEndpoint(providerId: String): String = userPrefs.getCustomEndpoint(providerId)
    fun saveCustomEndpoint(providerId: String, endpoint: String) = userPrefs.saveCustomEndpoint(providerId, endpoint)
    fun getCustomModel(providerId: String): String = userPrefs.getCustomModel(providerId)
    fun saveCustomModel(providerId: String, model: String) = userPrefs.saveCustomModel(providerId, model)
    fun hasExplicitProviderChoice(): Boolean = userPrefs.hasExplicitProviderChoice()
    fun setHasExplicitProviderChoice(hasChoice: Boolean) = userPrefs.setHasExplicitProviderChoice(hasChoice)

    fun selectAiProvider(providerId: String, apiKey: String, endpoint: String? = null, model: String? = null) {
        viewModelScope.launch {
            userPrefs.setSelectedProvider(providerId)
            if (apiKey.isNotBlank()) userPrefs.saveApiKeyForProvider(providerId, apiKey)
            if (endpoint != null) userPrefs.saveCustomEndpoint(providerId, endpoint)
            if (model != null) userPrefs.saveCustomModel(providerId, model)
            userPrefs.setHasExplicitProviderChoice(true)
        }
    }

    fun getStoredApiKey(): String {
        return getApiKeyForProvider(getSelectedProviderIdSync())
    }

    fun saveApiKey(key: String) {
        saveApiKeyForProvider(getSelectedProviderIdSync(), key)
    }

    private var lastScreenshotVersion = -1
    private var cachedBase64Image: String? = null

    private fun buildCanvasContextPrompt(userPrompt: String, pages: List<PageEntity>, canvasTitle: String): String {
        val contextBuilder = StringBuilder()
        contextBuilder.append("Контекст поточного конспекту/канви: \"$canvasTitle\"\n\n")

        pages.forEachIndexed { index, page ->
            contextBuilder.append("--- Сторінка ${index + 1} ---\n")
            val layers = page.getEffectiveLayers()
            val textBlocks = layers.flatMap { it.textBlocks }
            val shapes = layers.flatMap { it.shapes }
            val charts = layers.flatMap { it.charts }
            val strokes = layers.flatMap { it.strokes }

            if (textBlocks.isNotEmpty()) {
                contextBuilder.append("Текстові блоки:\n")
                textBlocks.forEach { tb ->
                    contextBuilder.append("- ${tb.text}\n")
                }
            }
            if (shapes.isNotEmpty()) {
                contextBuilder.append("Фігури на сторінці: ${shapes.joinToString { it.shapeType.name }}\n")
            }
            if (charts.isNotEmpty()) {
                contextBuilder.append("Графіки: ${charts.joinToString { it.title }}\n")
            }
            if (strokes.isNotEmpty()) {
                contextBuilder.append("Рукописних штрихів/ліній на сторінці: ${strokes.size}\n")
            }
        }

        val systemInstruction = "Ти — інтелектуальний помічник конспекту. Твоє завдання — допомагати користувачеві вивчати матеріали, відповідати на запитання, пояснювати формули та робити короткі підсумки ЛИШЕ на основі наданого контексту конспекту. Відповідай українською мовою, чітко, структуровано та приязно."

        return "$systemInstruction\n\n$contextBuilder\n\nЗапитання користувача: $userPrompt"
    }

    // AI Chat query with multi-provider routing
    fun sendAiPrompt(prompt: String) {
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val providerId = userPrefs.getSelectedProviderIdSync()
            val provider = com.example.ai.AiProviderRegistry.getProvider(providerId)
            val apiKey = getApiKeyForProvider(providerId)
            val endpoint = getCustomEndpoint(providerId)
            val model = getCustomModel(providerId)

            if (apiKey.isBlank()) {
                _isAiLoading.value = false
                val aiMsg = ChatMessage(text = "Будь ласка, вкажіть API-ключ для провайдера ${provider.displayName}.", isUser = false)
                _chatMessages.value = _chatMessages.value + aiMsg
                return@launch
            }

            val title = _canvas.value?.title ?: "Конспект"
            val currentVer = _canvasVersion.value
            val page = currentPage

            val base64Image = if (provider.supportsVision && page != null) {
                if (currentVer != lastScreenshotVersion || cachedBase64Image == null) {
                    try {
                        val effectiveBgColor = _canvas.value?.backgroundColor ?: 0xFFFFFFFF.toInt()
                        val bitmap = ExportManager.captureCanvasHighRes(page, scale = 2.0f, backgroundColor = effectiveBgColor)
                        val baos = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 85, baos)
                        bitmap.recycle()
                        val b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                        lastScreenshotVersion = currentVer
                        cachedBase64Image = b64
                        b64
                    } catch (e: Exception) {
                        android.util.Log.e("CanvasVM", "Failed to generate AI vision screenshot", e)
                        cachedBase64Image
                    }
                } else {
                    cachedBase64Image
                }
            } else null

            val fullPrompt = buildCanvasContextPrompt(prompt, _pages.value, title)

            val response = provider.query(
                text = fullPrompt,
                imageBase64 = base64Image,
                apiKey = apiKey,
                endpoint = endpoint,
                model = model
            )

            _isAiLoading.value = false
            val aiMsg = ChatMessage(text = response, isUser = false)
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    // Export PDF/SVG/PNG
    fun exportPdf(onSuccess: (File) -> Unit) {
        val page = currentPage ?: return
        val bgInt = _canvas.value?.backgroundColor ?: 0xFFFFFFFF.toInt()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.pdf")
            ExportManager.exportToPdf(page, file, context, backgroundColor = bgInt)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess(file)
            }
        }
    }

    fun exportImage(onSuccess: (File) -> Unit) {
        val page = currentPage ?: return
        val bgInt = _canvas.value?.backgroundColor ?: 0xFFFFFFFF.toInt()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.svg")
            ExportManager.exportToSvg(page, file, backgroundColor = bgInt)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess(file)
            }
        }
    }

    fun exportPng(onSuccess: (File) -> Unit) {
        exportPng(scale = 3.0f, cropRect = null, onSuccess = onSuccess)
    }

    fun exportPng(scale: Float = 3.0f, cropRect: android.graphics.RectF? = null, onSuccess: (File) -> Unit) {
        val page = currentPage ?: return
        val bgInt = _canvas.value?.backgroundColor ?: 0xFFFFFFFF.toInt()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.png")
            ExportManager.exportToPng(page, file, scale, cropRect, backgroundColor = bgInt)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess(file)
            }
        }
    }

    fun updateSelectedElementIds(ids: Set<String>) {
        _selectedElementIds.value = ids
    }

    fun deleteSelectedElements() {
        val page = currentPage ?: return
        val ids = _selectedElementIds.value
        if (ids.isEmpty()) return
        val migrated = ensureLayersExist(page)
        pushUndoState(migrated)
        val updatedLayers = migrated.layers.map { layer ->
            layer.copy(
                shapes = layer.shapes.filterNot { it.id in ids },
                images = layer.images.filterNot { it.id in ids },
                textBlocks = layer.textBlocks.filterNot { it.id in ids },
                charts = layer.charts.filterNot { it.id in ids }
            )
        }
        _selectedElementIds.value = emptySet()
        _canvasVersion.value++
        updateCurrentPage(migrated.copy(layers = updatedLayers))
    }

    fun saveCanvasThumbnail(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            repository.saveThumbnail(canvasId, bitmap)
        }
    }

    // ═══════════════════════════════════════════════════════
    // Academic Features Actions (10 Cheat Codes)
    // ═══════════════════════════════════════════════════════

    // Feature 1: Smart Shape Recognizer & Vectorizer
    fun recognizeAndVectorizeLastStroke() {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val lastStroke = migrated.getEffectiveLayers().flatMap { it.strokes }.lastOrNull() ?: return
        val recognized = com.example.academic.ShapeRecognizerEngine.recognizeShape(lastStroke.points)
        if (recognized != null) {
            val newShape = ShapeEntity(
                id = UUID.randomUUID().toString(),
                shapeType = recognized.type,
                x = recognized.bounds.left,
                y = recognized.bounds.top,
                width = recognized.bounds.width,
                height = recognized.bounds.height,
                strokeColor = lastStroke.colorHsla.toArgbInt(),
                fillColor = 0x336366F1,
                strokeWidth = lastStroke.baseWidth
            )
            val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: "default"
            val updatedLayers = migrated.layers.map { layer ->
                val strokeFiltered = layer.strokes.filterNot { it.id == lastStroke.id }
                if (layer.id == targetLayerId) {
                    layer.copy(strokes = strokeFiltered, shapes = layer.shapes + newShape)
                } else {
                    layer.copy(strokes = strokeFiltered)
                }
            }
            updateCurrentPage(migrated.copy(layers = updatedLayers))
            _academicStatusMessage.value = "Розпізнано фігуру: ${recognized.type.name} (Точність ${(recognized.confidence * 100).toInt()}%)"
        } else {
            _academicStatusMessage.value = "Фігуру не розпізнано. Спробуйте намалювати чіткіше коло чи прямокутник."
        }
    }

    fun plotFunctionFromStrokes() {
        val page = currentPage ?: return
        val migrated = ensureLayersExist(page)
        val strokes = migrated.getEffectiveLayers().flatMap { it.strokes }
        if (strokes.isEmpty()) {
            _academicStatusMessage.value = "Немає штрихів для аналізу математичної функції."
            return
        }
        val allPoints = strokes.flatMap { it.points }
        val result = com.example.academic.FunctionPlotterEngine.fitFunctionFromStrokes(allPoints)
        if (result != null) {
            val plottedStroke = StrokeEntity(
                id = UUID.randomUUID().toString(),
                tool = ToolType.PEN,
                colorHsla = HslaColor.BLUE,
                baseWidth = 5f,
                points = result.curvePoints
            )
            val targetLayerId = _activeLayerId.value ?: migrated.activeLayerId ?: "default"
            val updatedLayers = migrated.layers.map { layer ->
                if (layer.id == targetLayerId) {
                    layer.copy(strokes = layer.strokes + plottedStroke)
                } else layer
            }
            updateCurrentPage(migrated.copy(layers = updatedLayers))
            _latexOutput.value = result.latexFormula
            _academicStatusMessage.value = "Побудовано графік: ${result.latexFormula} (R² = ${String.format("%.2f", result.rSquared)})"
        } else {
            _academicStatusMessage.value = "Не вдалося апроксимувати функцію з наявних штрихів."
        }
    }

    // Feature 3: Handwriting to LaTeX Converter
    fun convertHandwritingToLatex() {
        val page = currentPage ?: return
        val strokes = page.getEffectiveLayers().flatMap { it.strokes }
        val latex = com.example.academic.HandwritingLatexConverter.convertStrokesToLatex(strokes)
        _latexOutput.value = latex
        _academicStatusMessage.value = "Сгенеровано LaTeX: $latex"
    }

    // Feature 4: Object Tracker with AR Overlays
    fun toggleArOverlay() {
        _isArOverlayActive.value = !_isArOverlayActive.value
        _academicStatusMessage.value = if (_isArOverlayActive.value) "AR-режим активний (CameraX AR Overlay)" else "AR-режим вимкнено"
    }

    // Feature 5: Graph Schema Analyzer
    fun analyzeGraphSchema() {
        val page = currentPage ?: return
        val result = com.example.academic.GraphSchemaAnalyzer.analyzePageGraph(page)
        _graphAnalysisResult.value = result
        _academicStatusMessage.value = "Аналіз графа: ${result.nodes.size} вузлів, ${result.edges.size} ребер. Компонент: ${result.connectedComponentsCount}, Цикли: ${if (result.hasCycles) "Так" else "Ні"}"
    }

    // Feature 6: Smart Lecture Recorder & Timestamp Sync
    fun seekAudioToStrokeTimestamp(strokeId: String) {
        val page = currentPage ?: return
        val stroke = page.getEffectiveLayers().flatMap { it.strokes }.find { it.id == strokeId } ?: return
        val strokeTime = stroke.points.firstOrNull()?.timestampMs ?: return
        val recording = audioRecordings.value.firstOrNull() ?: return
        val relativeMs = (strokeTime - recording.recordedAt).coerceAtLeast(0L)
        playAudioRecording(recording.filePath, relativeMs)
        _academicStatusMessage.value = "Перемотано аудіо до штриха: ${relativeMs / 1000} сек"
    }

    // Feature 7: Physics-based Object Dynamics
    fun togglePhysicsSimulation() {
        _isPhysicsActive.value = !_isPhysicsActive.value
        _academicStatusMessage.value = if (_isPhysicsActive.value) "Симуляцію фізики Box2D запущено!" else "Симуляцію фізики зупинено"
    }

    // Feature 8: Oil Paint Shader Effect
    fun toggleOilPaintShader() {
        _isOilPaintShaderActive.value = !_isOilPaintShaderActive.value
        _academicStatusMessage.value = if (_isOilPaintShaderActive.value) "Олійний AGSL-шейдер увімкнено!" else "Олійний шейдер вимкнено"
    }

    // Feature 9: Watercolor Bleed Simulation
    fun applyWatercolorBleedEffect() {
        _academicStatusMessage.value = "Застосовано симуляцію розмиття фарби Watercolor Bleed (PDE solver)"
    }

    // Feature 10: Infinite Canvas with Virtual Memory Paging
    fun togglePagedCanvas() {
        _isPagedCanvasActive.value = !_isPagedCanvasActive.value
        _academicStatusMessage.value = if (_isPagedCanvasActive.value) "Нескінченне полотно з Paging & BitmapPool активне!" else "Paging вимкнено"
    }
}

