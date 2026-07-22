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
import com.example.data.models.CanvasEntity
import com.example.data.models.ChartElementEntity
import com.example.data.models.EraserMode
import com.example.data.models.HslaColor
import com.example.data.models.ImageElementEntity
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

    private val audioRecorderManager = AudioRecorderManager(context)
    private val geminiService = GeminiAssistantService()
    private val exportManager = ExportManager(context)

    val audioStatus: StateFlow<RecordingStatus> = audioRecorderManager.status

    private val _canvas = MutableStateFlow<CanvasEntity?>(null)
    val canvas: StateFlow<CanvasEntity?> = _canvas.asStateFlow()

    private val _pages = MutableStateFlow<List<PageEntity>>(emptyList())
    val pages: StateFlow<List<PageEntity>> = _pages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    private val _eraserMode = MutableStateFlow(EraserMode.OBJECT)
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

    private val _rulerState = MutableStateFlow(RulerState())
    val rulerState: StateFlow<RulerState> = _rulerState.asStateFlow()

    // Undo / Redo history per page snapshot
    private val pageUndoHistory = mutableListOf<PageEntity>()
    private val pageRedoHistory = mutableListOf<PageEntity>()

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

    init {
        viewModelScope.launch {
            repository.getCanvasById(canvasId).collect { c ->
                _canvas.value = c
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

    fun selectTool(tool: ToolType) {
        if (tool == ToolType.RULER) {
            _rulerState.value = _rulerState.value.copy(isVisible = !_rulerState.value.isVisible)
        } else {
            _currentTool.value = tool
        }
    }

    fun setEraserMode(mode: EraserMode) {
        _eraserMode.value = mode
    }

    fun setStrokeWidth(w: Float) {
        _strokeWidth.value = w.coerceIn(1f, 50f)
    }

    fun setStrokeOpacity(op: Float) {
        _strokeOpacity.value = op.coerceIn(0.05f, 1f)
    }

    fun setColor(color: HslaColor) {
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

    fun addStrokeToCurrentPage(stroke: StrokeEntity) {
        val page = currentPage ?: return
        pushUndoState(page)
        val updatedStrokes = page.strokes + stroke
        val updatedPage = page.copy(strokes = updatedStrokes)
        updateCurrentPage(updatedPage)
    }

    fun eraseAtPoint(point: Offset, radius: Float) {
        val page = currentPage ?: return
        pushUndoState(page)
        if (_eraserMode.value == EraserMode.OBJECT) {
            val updatedStrokes = page.strokes.filterNot { stroke ->
                DrawingEngine.isPointInStroke(point, stroke, radius)
            }
            updateCurrentPage(page.copy(strokes = updatedStrokes))
        } else {
            val updatedStrokes = mutableListOf<StrokeEntity>()
            page.strokes.forEach { stroke ->
                val erased = DrawingEngine.erasePixelMode(stroke, point, radius)
                updatedStrokes.addAll(erased)
            }
            updateCurrentPage(page.copy(strokes = updatedStrokes))
        }
    }

    fun undo() {
        if (pageUndoHistory.isNotEmpty()) {
            val page = currentPage ?: return
            pageRedoHistory.add(page)
            val previousPage = pageUndoHistory.removeAt(pageUndoHistory.size - 1)
            updateCurrentPage(previousPage)
        }
    }

    fun redo() {
        if (pageRedoHistory.isNotEmpty()) {
            val page = currentPage ?: return
            pageUndoHistory.add(page)
            val nextPage = pageRedoHistory.removeAt(pageRedoHistory.size - 1)
            updateCurrentPage(nextPage)
        }
    }

    private fun pushUndoState(page: PageEntity) {
        if (pageUndoHistory.size >= 50) pageUndoHistory.removeAt(0)
        pageUndoHistory.add(page)
        pageRedoHistory.clear()
    }

    private fun updateCurrentPage(page: PageEntity) {
        viewModelScope.launch {
            repository.updatePage(page)
        }
    }

    fun updateBackgroundColor(colorInt: Int) {
        val c = _canvas.value ?: return
        viewModelScope.launch {
            repository.updateCanvas(c.copy(backgroundColor = colorInt))
        }
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

    fun insertShape(shapeType: ShapeType, targetX: Float = 160f, targetY: Float = 160f) {
        val page = currentPage ?: return
        pushUndoState(page)
        val newShape = ShapeEntity(
            shapeType = shapeType,
            x = targetX,
            y = targetY,
            width = 180f,
            height = 180f,
            fillColor = _currentColor.value.copy(alpha = 0.2f).toArgbInt(),
            strokeColor = _currentColor.value.toArgbInt()
        )
        updateCurrentPage(page.copy(shapes = page.shapes + newShape))
    }

    fun insertText(text: String, targetX: Float = 160f, targetY: Float = 160f) {
        val page = currentPage ?: return
        pushUndoState(page)
        val newText = TextBlockEntity(
            text = text,
            x = targetX,
            y = targetY,
            color = _currentColor.value.toArgbInt()
        )
        updateCurrentPage(page.copy(textBlocks = page.textBlocks + newText))
    }

    fun insertMathFunctionChart(formula: String = "sin(x)", xMin: Float = -10f, xMax: Float = 10f, targetX: Float = 160f, targetY: Float = 160f) {
        val page = currentPage ?: return
        pushUndoState(page)
        val graphW = 380f
        val graphH = 260f

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

            val canvasX = targetX + normX.toFloat() * graphW
            val canvasY = targetY + normY.toFloat() * graphH
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
            x = targetX,
            y = targetY - 28f,
            fontSize = 15f,
            color = _currentColor.value.toArgbInt()
        )

        val gridChart = ChartElementEntity(
            x = targetX,
            y = targetY,
            width = graphW,
            height = graphH
        )

        updateCurrentPage(
            page.copy(
                charts = page.charts + gridChart,
                strokes = page.strokes + chartStroke,
                textBlocks = page.textBlocks + textLabel
            )
        )
    }

    private fun evaluateMathFormula(formula: String, x: Double): Double {
        val clean = formula.lowercase().replace(" ", "")
        return try {
            when {
                clean.contains("sin") -> {
                    val factor = clean.replace("sin(x)", "").replace("sin", "").replace("*", "").toDoubleOrNull() ?: 1.0
                    Math.sin(x) * factor
                }
                clean.contains("cos") -> {
                    val factor = clean.replace("cos(x)", "").replace("cos", "").replace("*", "").toDoubleOrNull() ?: 1.0
                    Math.cos(x) * factor
                }
                clean.contains("tan") -> Math.tan(x)
                clean.contains("x*x") || clean.contains("x^2") -> x * x
                clean.contains("x*x*x") || clean.contains("x^3") -> x * x * x
                clean.contains("sqrt") -> Math.sqrt(x)
                clean.contains("1/x") -> if (x != 0.0) 1.0 / x else 0.0
                else -> {
                    if (clean.contains("*x")) {
                        val m = clean.substringBefore("*x").toDoubleOrNull() ?: 1.0
                        val b = clean.substringAfter("+", "0").toDoubleOrNull() ?: 0.0
                        m * x + b
                    } else if (clean.contains("x")) {
                        val b = clean.substringAfter("+", "0").toDoubleOrNull() ?: 0.0
                        x + b
                    } else {
                        clean.toDoubleOrNull() ?: 0.0
                    }
                }
            }
        } catch (e: Exception) {
            0.0
        }
    }

    fun insertChart(targetX: Float = 160f, targetY: Float = 160f) {
        val page = currentPage ?: return
        pushUndoState(page)
        val newChart = ChartElementEntity(
            x = targetX,
            y = targetY,
            width = 380f,
            height = 260f
        )
        updateCurrentPage(page.copy(charts = page.charts + newChart))
    }

    fun insertImage(uri: android.net.Uri, targetX: Float = 160f, targetY: Float = 160f) {
        val page = currentPage ?: return
        viewModelScope.launch {
            val imagePath = repository.saveImportedImage(uri)
            val file = File(imagePath)
            var w = 340f
            var h = 240f
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
            }

            val newImg = ImageElementEntity(
                id = UUID.randomUUID().toString(),
                sourceUri = imagePath,
                x = targetX,
                y = targetY,
                width = w,
                height = h,
                opacity = 1.0f
            )
            pushUndoState(page)
            updateCurrentPage(page.copy(images = page.images + newImg))
        }
    }

    fun deleteElement(id: String, type: String) {
        val page = currentPage ?: return
        pushUndoState(page)
        when (type) {
            "SHAPE" -> updateCurrentPage(page.copy(shapes = page.shapes.filterNot { it.id == id }))
            "IMAGE" -> updateCurrentPage(page.copy(images = page.images.filterNot { it.id == id }))
            "TEXT" -> updateCurrentPage(page.copy(textBlocks = page.textBlocks.filterNot { it.id == id }))
            "CHART" -> updateCurrentPage(page.copy(charts = page.charts.filterNot { it.id == id }))
        }
    }

    fun rotateElement(id: String, type: String) {
        val page = currentPage ?: return
        pushUndoState(page)
        when (type) {
            "IMAGE" -> {
                val updated = page.images.map {
                    if (it.id == id) it.copy(rotation = (it.rotation + 90f) % 360f) else it
                }
                updateCurrentPage(page.copy(images = updated))
            }
            "SHAPE" -> {
                val updated = page.shapes.map {
                    if (it.id == id) it.copy(rotation = (it.rotation + 90f) % 360f) else it
                }
                updateCurrentPage(page.copy(shapes = updated))
            }
            "CHART" -> {
                val updated = page.charts.map {
                    if (it.id == id) it.copy(width = it.height, height = it.width) else it
                }
                updateCurrentPage(page.copy(charts = updated))
            }
            "TEXT" -> {
                val updated = page.textBlocks.map {
                    if (it.id == id) it.copy(width = it.height, height = it.width) else it
                }
                updateCurrentPage(page.copy(textBlocks = updated))
            }
        }
    }

    fun updateImageOpacity(imageId: String, opacity: Float) {
        val page = currentPage ?: return
        val updatedImages = page.images.map {
            if (it.id == imageId) it.copy(opacity = opacity.coerceIn(0.1f, 1.0f)) else it
        }
        updateCurrentPage(page.copy(images = updatedImages))
    }

    fun updateImageSize(imageId: String, width: Float, height: Float) {
        val page = currentPage ?: return
        val updatedImages = page.images.map {
            if (it.id == imageId) it.copy(width = width.coerceAtLeast(50f), height = height.coerceAtLeast(50f)) else it
        }
        updateCurrentPage(page.copy(images = updatedImages))
    }

    fun updateShapeSize(shapeId: String, width: Float, height: Float) {
        val page = currentPage ?: return
        val updatedShapes = page.shapes.map {
            if (it.id == shapeId) it.copy(width = width.coerceAtLeast(30f), height = height.coerceAtLeast(30f)) else it
        }
        updateCurrentPage(page.copy(shapes = updatedShapes))
    }

    fun updateChartSize(chartId: String, width: Float, height: Float) {
        val page = currentPage ?: return
        val updatedCharts = page.charts.map {
            if (it.id == chartId) it.copy(width = width.coerceAtLeast(100f), height = height.coerceAtLeast(100f)) else it
        }
        updateCurrentPage(page.copy(charts = updatedCharts))
    }

    fun updateShapePosition(shapeId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val updatedShapes = page.shapes.map {
            if (it.id == shapeId) it.copy(x = newX, y = newY) else it
        }
        updateCurrentPage(page.copy(shapes = updatedShapes))
    }

    fun updateTextPosition(textId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val updatedTexts = page.textBlocks.map {
            if (it.id == textId) it.copy(x = newX, y = newY) else it
        }
        updateCurrentPage(page.copy(textBlocks = updatedTexts))
    }

    fun updateImagePosition(imageId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val updatedImages = page.images.map {
            if (it.id == imageId) it.copy(x = newX, y = newY) else it
        }
        updateCurrentPage(page.copy(images = updatedImages))
    }

    fun updateChartPosition(chartId: String, newX: Float, newY: Float) {
        val page = currentPage ?: return
        val updatedCharts = page.charts.map {
            if (it.id == chartId) it.copy(x = newX, y = newY) else it
        }
        updateCurrentPage(page.copy(charts = updatedCharts))
    }

    fun setCurrentPage(index: Int) {
        if (index in 0 until _pages.value.size) {
            _currentPageIndex.value = index
        }
    }

    fun addNewPage() {
        viewModelScope.launch {
            val page = repository.addPage(canvasId)
            _currentPageIndex.value = _pages.value.size
        }
    }

    fun deletePage(page: PageEntity) {
        viewModelScope.launch {
            repository.deletePage(page)
        }
    }

    // Audio Recording Controls
    fun startAudioRecording() {
        audioRecorderManager.startRecording()
    }

    fun stopAudioRecording() {
        val (path, durationMs) = audioRecorderManager.stopRecording()
        if (path != null && durationMs > 500) {
            viewModelScope.launch {
                repository.saveAudioRecording(canvasId, path, durationMs)
            }
        }
    }

    fun playAudioRecording(recording: AudioRecordingEntity) {
        audioRecorderManager.startPlayback(recording.filePath)
    }

    // AI Chat query
    fun sendAiPrompt(prompt: String) {
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val title = _canvas.value?.title ?: "Конспект"
            val response = geminiService.queryCanvasAssistant(
                userPrompt = prompt,
                pages = _pages.value,
                canvasTitle = title
            )
            _isAiLoading.value = false
            val aiMsg = ChatMessage(text = response, isUser = false)
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    // Export PDF/Image
    fun exportPdf(onSuccess: (File) -> Unit) {
        val c = _canvas.value ?: return
        viewModelScope.launch {
            val file = exportManager.exportCanvasToPdf(c, _pages.value)
            onSuccess(file)
        }
    }

    fun exportImage(onSuccess: (File) -> Unit) {
        val c = _canvas.value ?: return
        val p = currentPage ?: return
        viewModelScope.launch {
            val bitmap = exportManager.exportPageToBitmap(c, p, scaleRatio = 2.0f)
            val file = exportManager.saveBitmapToFile(bitmap, "canvas_export_${System.currentTimeMillis()}.png")
            onSuccess(file)
        }
    }

    fun saveCanvasThumbnail(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            repository.saveThumbnail(canvasId, bitmap)
        }
    }
}
