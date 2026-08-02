package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.drawing.DrawingEngine
import com.example.core.drawing.RasterStrokeCompositor
import com.example.core.drawing.RulerState
import com.example.data.models.BackgroundPattern
import com.example.data.models.CanvasEntity
import com.example.data.models.ChartElementEntity
import com.example.data.models.EraserMark
import com.example.data.models.EraserMode
import com.example.data.models.HslaColor
import com.example.data.models.ImageElementEntity
import com.example.data.models.PageEntity
import com.example.data.models.PageSizePreset
import com.example.data.models.ShapeEntity
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.TextBlockEntity
import com.example.data.models.ToolType
import com.example.data.models.isAttachedToChart
import com.example.data.models.resizeFramePreservingOrigin
import com.example.data.models.squarePixelsPerUnit
import com.example.data.models.withSquareGrid
import com.example.ui.components.CodeBlockCanvasCard
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InteractiveCanvas(
    canvasEntity: CanvasEntity?,
    pageEntity: PageEntity?,
    currentTool: ToolType,
    eraserMode: EraserMode = EraserMode.OBJECT,
    strokeWidth: Float,
    strokeOpacity: Float,
    currentColor: HslaColor,
    drawWithFingers: Boolean,
    palmRejectionEnabled: Boolean = true,
    rulerState: RulerState,
    zoomScale: Float,
    viewportPanOffset: Offset = Offset.Zero,
    onZoomChanged: (Float) -> Unit,
    onPanOffsetChanged: (Offset) -> Unit = {},
    onStrokeAdded: (StrokeEntity) -> Unit,
    onEraserMarkAdded: (com.example.data.models.EraserMark) -> Unit = {},
    onEraseAtPoint: (Offset, Float) -> Unit,
    onBeginEraserGesture: () -> Unit = {},
    onEndEraserGesture: () -> Unit = {},
    onTwoFingerTap: () -> Unit,
    onMoveShape: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveText: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveImage: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveChart: (String, Float, Float) -> Unit = { _, _, _ -> },
    onDeleteElement: (String, String) -> Unit = { _, _ -> },
    onRotateElement: (String, String) -> Unit = { _, _ -> },
    onUpdateImageOpacity: (String, Float) -> Unit = { _, _ -> },
    onResizeElement: (String, String, Float, Float, String) -> Unit = { _, _, _, _, _ -> },
    selectionMode: com.example.data.models.SelectionMode = com.example.data.models.SelectionMode.SINGLE,
    selectedElementIds: Set<String> = emptySet(),
    linkedElementIds: Set<String> = emptySet(),
    onElementSelected: (id: String, type: String) -> Unit = { _, _ -> },
    onLinkedElementActivated: (id: String) -> Unit = {},
    onFillElement: (id: String?, type: String?) -> Unit = { _, _ -> },
    onColorSampled: (HslaColor) -> Unit = {},
    onTextPositionRequested: (Offset) -> Unit = {},
    onLassoComplete: (List<Offset>) -> Unit = {},
    onBeginMoveSelectedGroup: () -> Unit = {},
    onMoveSelectedGroup: (Float, Float) -> Unit = { _, _ -> },
    onEndMoveSelectedGroup: () -> Unit = {},
    onResizeAndMoveElement: (String, String, Float, Float, Float, Float, String, Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    getCachedBitmap: (String) -> android.graphics.Bitmap? = { null },
    onPreloadImage: (String) -> Unit = {},
    onEditCodeBlock: (String) -> Unit = {},
    onRunCodeBlock: (String) -> Unit = {},
    onEditTextBlock: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var currentScale by remember { mutableStateOf(zoomScale) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    LaunchedEffect(zoomScale) {
        if (Math.abs(currentScale - zoomScale) > 0.05f) {
            currentScale = zoomScale
        }
    }

    LaunchedEffect(viewportPanOffset) {
        if ((panOffset - viewportPanOffset).getDistanceSquared() > 0.25f) {
            panOffset = viewportPanOffset
        }
    }

    val activeStrokePoints = remember { mutableStateListOf<StrokePoint>() }
    val activeEraserPoints = remember { mutableStateListOf<StrokePoint>() }
    var eraserTouchPos by remember { mutableStateOf<Offset?>(null) }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementType by remember { mutableStateOf<String?>(null) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var elementOriginalPos by remember { mutableStateOf(Offset.Zero) }
    var elementOriginalSize by remember { mutableStateOf(Offset.Zero) }
    var elementOriginalRotation by remember { mutableStateOf(0f) }
    var isResizingCorner by remember { mutableStateOf(false) }
    var resizingCorner by remember { mutableStateOf<String?>(null) }
    var isDraggingGroup by remember { mutableStateOf(false) }
    var isObjectEraserGesture by remember { mutableStateOf(false) }
    var transformPreview by remember { mutableStateOf<ElementTransform?>(null) }
    var pendingUtilityTap by remember { mutableStateOf<Pair<ToolType, Offset>?>(null) }
    var pendingUtilityDown by remember { mutableStateOf(Offset.Zero) }

    var rulerGuideEdge by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var cursorPos by remember { mutableStateOf<Offset?>(null) }
    var previewPulse by remember { mutableStateOf(false) }

    var pendingCommittedStroke by remember { mutableStateOf<StrokeEntity?>(null) }
    var activeStrokeChartId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pageEntity?.id) {
        // Compose can reuse this editor instance while navigating between pages/canvases. Never
        // carry a pending live preview into another document (where it looked like graph ink).
        pendingCommittedStroke = null
        activeStrokePoints.clear()
        activeEraserPoints.clear()
        activeStrokeChartId = null
        eraserTouchPos = null
        cursorPos = null
    }

    LaunchedEffect(pageEntity) {
        pendingCommittedStroke?.let { pending ->
            val exists = pageEntity?.getEffectiveLayers()?.any { layer ->
                layer.strokes.any { it.id == pending.id }
            } ?: false
            if (exists) {
                pendingCommittedStroke = null
            }
        }
    }

    LaunchedEffect(strokeWidth, strokeOpacity, currentColor) {
        previewPulse = true
        kotlinx.coroutines.delay(600)
        previewPulse = false
    }

    // Preload image bitmaps off the UI thread via ViewModel LruCache
    val imageUris = remember(pageEntity) {
        pageEntity?.getEffectiveLayers()?.flatMap { it.images }?.map { it.sourceUri }?.distinct() ?: emptyList()
    }
    val selectedGroupBounds = remember(pageEntity, selectedElementIds, selectedElementId) {
        // A chart's handwriting is a transform dependent, not a second set of handles. Keep
        // multi-selection affordances for lasso groups only; otherwise the group hit area masks
        // the chart's own resize handles.
        if (selectedElementId == null) pageEntity?.selectionBounds(selectedElementIds) else null
    }
    val latestPageEntity by rememberUpdatedState(pageEntity)
    val latestSelectedGroupBounds by rememberUpdatedState(selectedGroupBounds)
    val latestCurrentTool by rememberUpdatedState(currentTool)
    val latestEraserMode by rememberUpdatedState(eraserMode)
    val latestStrokeWidth by rememberUpdatedState(strokeWidth)
    val latestStrokeOpacity by rememberUpdatedState(strokeOpacity)
    val latestCurrentColor by rememberUpdatedState(currentColor)
    val latestCurrentScale by rememberUpdatedState(currentScale)
    val latestRulerState by rememberUpdatedState(rulerState)
    val latestPanOffset by rememberUpdatedState(panOffset)
    val latestDrawWithFingers by rememberUpdatedState(drawWithFingers)
    val latestPalmRejectionEnabled by rememberUpdatedState(palmRejectionEnabled)
    imageUris.forEach { uri ->
        if (getCachedBitmap(uri) == null) {
            LaunchedEffect(uri) {
                onPreloadImage(uri)
            }
        }
    }

    // Canvas background color (defaults to white if not specified)
    val bgColor = canvasEntity?.backgroundColor?.let { Color(it) } ?: Color.White

    val pattern = canvasEntity?.backgroundPattern ?: BackgroundPattern.DOTTED
    val isDarkBackground = (bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f) < 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val hasStylus = event.changes.any { change ->
                            (change.pressed || change.previousPressed) && change.type == PointerType.Stylus
                        }
                        // A touch beside an active stylus is palm input, never a zoom gesture.
                        // With no stylus present, Compose's transform helpers operate on the full
                        // pointer event and preserve normal two-finger navigation.
                        if (!hasStylus && event.changes.count { it.pressed } >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            if (zoom != 1f || pan != Offset.Zero) {
                                activeStrokePoints.clear()
                                activeEraserPoints.clear()
                                pendingUtilityTap = null
                                eraserTouchPos = null
                                cursorPos = null
                                val oldScale = currentScale
                                val newScale = (oldScale * zoom).coerceIn(0.1f, 10.0f)
                                val zoomFactor = newScale / oldScale
                                panOffset = centroid - (centroid - panOffset) * zoomFactor + pan
                                onPanOffsetChanged(panOffset)
                                if (newScale != oldScale) {
                                    currentScale = newScale
                                    onZoomChanged(newScale)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val currentTool = latestCurrentTool
                        val eraserMode = latestEraserMode
                        val strokeWidth = latestStrokeWidth
                        val strokeOpacity = latestStrokeOpacity
                        val currentColor = latestCurrentColor
                        val currentScale = latestCurrentScale
                        val rulerState = latestRulerState
                        val panOffset = latestPanOffset
                        val drawWithFingers = latestDrawWithFingers
                        val palmRejectionEnabled = latestPalmRejectionEnabled

                        val pointerEvent = awaitPointerEvent()
                        val activeChanges = pointerEvent.changes.filter { it.pressed || it.previousPressed }
                        val hasStylus = activeChanges.any { it.type == PointerType.Stylus }
                        val change = activeChanges.firstOrNull { it.type == PointerType.Stylus }
                            ?: activeChanges.firstOrNull()
                            ?: continue

                        // When anti-palm is enabled, a stylus always wins over a simultaneous
                        // touch. If finger drawing is disabled, a lone finger never starts an
                        // editing gesture either; it remains available as part of a two-finger
                        // canvas transform handled above.
                        if ((palmRejectionEnabled && hasStylus && change.type == PointerType.Touch) ||
                            (!drawWithFingers && change.type == PointerType.Touch)
                        ) {
                            change.consume()
                            continue
                        }
                        val action = when {
                            !change.previousPressed && change.pressed -> PointerAction.DOWN
                            change.previousPressed && !change.pressed -> PointerAction.UP
                            change.pressed -> PointerAction.MOVE
                            else -> continue
                        }
                        val screenPoint = change.position
                // ═══════════════════════════════════════════════════════
                // 0a. Перевірка касання у верхній зоні тулбара (TopFloatingToolbar)
                // ═══════════════════════════════════════════════════════
                val topToolbarHeightPx = with(density) { 90.dp.toPx() }
                if (action == PointerAction.DOWN && screenPoint.y <= topToolbarHeightPx && selectedElementId == null) {
                    continue
                }

                // ═══════════════════════════════════════════════════════
                // 0b. Перевірка касання в області лінійки — пропускаємо до RulerOverlayComponent
                // ═══════════════════════════════════════════════════════
                if (action == PointerAction.DOWN && rulerState.isVisible && isTouchOnRulerControls(screenPoint, rulerState)) {
                    continue
                }

                // ═══════════════════════════════════════════════════════
                // 1. Мультитач (2+ пальці) — скасовуємо штрих, даємо зуму працювати
                // ═══════════════════════════════════════════════════════
                val activeEditingPointers = pointerEvent.changes.count { pointer ->
                    pointer.pressed && (!palmRejectionEnabled || !hasStylus || pointer.type != PointerType.Touch)
                }
                if (activeEditingPointers > 1) {
                    activeStrokePoints.clear()
                    activeEraserPoints.clear()
                    pendingUtilityTap = null
                    if (currentTool == ToolType.ERASER) onEndEraserGesture()
                    isObjectEraserGesture = false
                    eraserTouchPos = null
                    cursorPos = null
                    continue
                }

                val safeScale = currentScale.coerceIn(0.1f, 10.0f)
                val x = (screenPoint.x - panOffset.x) / safeScale
                val y = (screenPoint.y - panOffset.y) / safeScale
                var rawPoint = Offset(x, y)

                val pressure = if (change.pressure > 0f) change.pressure else 0.5f
                val tilt = 0f

                when (action) {
                    PointerAction.DOWN -> {
                        activeStrokePoints.clear()
                        activeEraserPoints.clear()
                        activeStrokeChartId = null
                        pendingUtilityTap = null
                        transformPreview = null
                        if (currentTool.isStrokeTool()) {
                            if (rulerState.isVisible) {
                                val g = rulerState.nearestEdge(screenPoint, guideZone = 18f)
                                if (g != null) {
                                    rulerGuideEdge = g.second
                                    rawPoint = Offset(
                                        (g.first.x - panOffset.x) / safeScale,
                                        (g.first.y - panOffset.y) / safeScale
                                    )
                                } else if (rulerState.contains(screenPoint)) {
                                    // A physical ruler blocks a new mark from starting on its body.
                                    rulerGuideEdge = null
                                    continue
                                } else {
                                    rulerGuideEdge = null
                                }
                            } else {
                                rulerGuideEdge = null
                            }
                            cursorPos = rawPoint
                        } else {
                            rulerGuideEdge = null
                            if (currentTool == ToolType.ERASER) cursorPos = rawPoint
                        }

                        if (currentTool == ToolType.SELECTOR) {
                            dragStartOffset = rawPoint
                            isResizingCorner = false
                            resizingCorner = null
                            isDraggingGroup = selectedElementId == null &&
                                selectedElementIds.isNotEmpty() &&
                                latestSelectedGroupBounds?.contains(rawPoint) == true

                            if (isDraggingGroup) {
                                selectedElementId = null
                                selectedElementType = null
                                onBeginMoveSelectedGroup()
                            } else {
                            // Check corner resize touch for active selection (ALL 4 CORNERS)
                            val selId = selectedElementId
                            val selType = selectedElementType
                            var cornerHit = false
                            val inputPage = latestPageEntity
                            if (selId != null && selType != null && inputPage != null) {
                                var cornerRect: Rect? = null
                                var elemRotation = 0f
                                when (selType) {
                                    "SHAPE" -> inputPage.findShape(selId)?.let {
                                        cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                                        elemRotation = it.rotation
                                    }
                                    "IMAGE" -> inputPage.findImage(selId)?.let {
                                        cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                                        elemRotation = it.rotation
                                    }
                                    "TEXT" -> inputPage.findText(selId)?.let {
                                        cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                                        elemRotation = it.rotation
                                    }
                                    "CHART" -> inputPage.findChart(selId)?.let {
                                        cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                                        elemRotation = it.rotation
                                    }
                                    "CODE" -> inputPage.findCodeBlock(selId)?.let {
                                        cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                                        elemRotation = it.rotation
                                    }
                                }
                                cornerRect?.let { r ->
                                    val center = Offset((r.left + r.right) / 2f, (r.top + r.bottom) / 2f)
                                    val testPoint = if (elemRotation != 0f) {
                                        val angleRad = Math.toRadians(-elemRotation.toDouble())
                                        val cosA = Math.cos(angleRad).toFloat()
                                        val sinA = Math.sin(angleRad).toFloat()
                                        val dx = rawPoint.x - center.x
                                        val dy = rawPoint.y - center.y
                                        Offset(center.x + (dx * cosA - dy * sinA), center.y + (dx * sinA + dy * cosA))
                                    } else rawPoint

                                    val corners = listOf(
                                        "TL" to Offset(r.left, r.top),
                                        "TR" to Offset(r.right, r.top),
                                        "BL" to Offset(r.left, r.bottom),
                                        "BR" to Offset(r.right, r.bottom)
                                    )
                                    val hitRadius = 48.0 / currentScale
                                    var hitCorner: String? = null
                                    for ((name, corner) in corners) {
                                        val dist = Math.hypot((testPoint.x - corner.x).toDouble(), (testPoint.y - corner.y).toDouble())
                                        if (dist <= hitRadius) {
                                            hitCorner = name
                                            break
                                        }
                                    }
                                    if (hitCorner != null) {
                                        cornerHit = true
                                        isResizingCorner = true
                                        resizingCorner = hitCorner
                                        elementOriginalSize = Offset(r.width, r.height)
                                        elementOriginalPos = Offset(r.left, r.top)
                                        elementOriginalRotation = elemRotation
                                    }
                                }
                            }

                            if (!cornerHit) {
                                selectedElementId = null
                                selectedElementType = null

                                inputPage?.let { page ->
                                    val margin = 30f / currentScale
                                    page.getEffectiveLayers().reversed().forEach { layer ->
                                        if (selectedElementId == null) {
                                            layer.codeBlocks.reversed().forEach { block ->
                                                if (selectedElementId == null &&
                                                    rawPoint.x >= block.x - margin && rawPoint.x <= block.x + block.width + margin &&
                                                    rawPoint.y >= block.y - margin && rawPoint.y <= block.y + block.height + margin
                                                ) {
                                                    selectedElementId = block.id
                                                    selectedElementType = "CODE"
                                                    elementOriginalPos = Offset(block.x, block.y)
                                                    elementOriginalSize = Offset(block.width, block.height)
                                                    elementOriginalRotation = block.rotation
                                                }
                                            }
                                            layer.shapes.reversed().forEach { shape ->
                                                val center = Offset(shape.x + shape.width / 2f, shape.y + shape.height / 2f)
                                                val p = if (shape.rotation != 0f) {
                                                    val rad = Math.toRadians(-shape.rotation.toDouble())
                                                    val cA = Math.cos(rad).toFloat()
                                                    val sA = Math.sin(rad).toFloat()
                                                    val dx = rawPoint.x - center.x
                                                    val dy = rawPoint.y - center.y
                                                    Offset(center.x + (dx * cA - dy * sA), center.y + (dx * sA + dy * cA))
                                                } else rawPoint

                                                if (selectedElementId == null && p.x >= shape.x - margin && p.x <= shape.x + shape.width + margin &&
                                                    p.y >= shape.y - margin && p.y <= shape.y + shape.height + margin) {
                                                    selectedElementId = shape.id
                                                    selectedElementType = "SHAPE"
                                                    elementOriginalPos = Offset(shape.x, shape.y)
                                                    elementOriginalSize = Offset(shape.width, shape.height)
                                                    elementOriginalRotation = shape.rotation
                                                }
                                            }
                                            layer.images.reversed().forEach { img ->
                                                val center = Offset(img.x + img.width / 2f, img.y + img.height / 2f)
                                                val p = if (img.rotation != 0f) {
                                                    val rad = Math.toRadians(-img.rotation.toDouble())
                                                    val cA = Math.cos(rad).toFloat()
                                                    val sA = Math.sin(rad).toFloat()
                                                    val dx = rawPoint.x - center.x
                                                    val dy = rawPoint.y - center.y
                                                    Offset(center.x + (dx * cA - dy * sA), center.y + (dx * sA + dy * cA))
                                                } else rawPoint

                                                if (selectedElementId == null && p.x >= img.x - margin && p.x <= img.x + img.width + margin &&
                                                    p.y >= img.y - margin && p.y <= img.y + img.height + margin) {
                                                    selectedElementId = img.id
                                                    selectedElementType = "IMAGE"
                                                    elementOriginalPos = Offset(img.x, img.y)
                                                    elementOriginalSize = Offset(img.width, img.height)
                                                    elementOriginalRotation = img.rotation
                                                }
                                            }
                                            layer.textBlocks.reversed().forEach { text ->
                                                if (selectedElementId == null && rawPoint.x >= text.x - margin && rawPoint.x <= text.x + text.width + margin &&
                                                    rawPoint.y >= text.y - margin && rawPoint.y <= text.y + text.height + margin) {
                                                    selectedElementId = text.id
                                                    selectedElementType = "TEXT"
                                                    elementOriginalPos = Offset(text.x, text.y)
                                                    elementOriginalSize = Offset(text.width, text.height)
                                                    elementOriginalRotation = text.rotation
                                                }
                                            }
                                            layer.charts.reversed().forEach { chart ->
                                                if (selectedElementId == null && rawPoint.x >= chart.x - margin && rawPoint.x <= chart.x + chart.width + margin &&
                                                    rawPoint.y >= chart.y - margin && rawPoint.y <= chart.y + chart.height + margin) {
                                                    selectedElementId = chart.id
                                                    selectedElementType = "CHART"
                                                    elementOriginalPos = Offset(chart.x, chart.y)
                                                    elementOriginalSize = Offset(chart.width, chart.height)
                                                    elementOriginalRotation = chart.rotation
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isDraggingGroup) {
                                val selectedId = selectedElementId
                                val selectedType = selectedElementType
                                if (selectedId != null && selectedType != null) {
                                    onElementSelected(selectedId, selectedType)
                                }
                            }
                            }
                        } else if (currentTool == ToolType.POINTER ||
                            currentTool == ToolType.FILL ||
                            currentTool == ToolType.EYEDROPPER ||
                            currentTool == ToolType.TEXT
                        ) {
                            // Commit tap tools on UP. If a second finger joins, the pending action
                            // is cancelled and the gesture remains a pure pan/zoom operation.
                            pendingUtilityTap = currentTool to rawPoint
                            pendingUtilityDown = rawPoint
                        } else if (currentTool == ToolType.ERASER) {
                            cursorPos = rawPoint
                            eraserTouchPos = rawPoint
                            if (eraserMode == EraserMode.PIXEL) {
                                onBeginEraserGesture()
                                activeEraserPoints.add(StrokePoint(x = rawPoint.x, y = rawPoint.y, pressure = pressure, tilt = tilt, timestampMs = System.currentTimeMillis()))
                            } else {
                                onBeginEraserGesture()
                                isObjectEraserGesture = true
                                onEraseAtPoint(rawPoint, DrawingEngine.eraserDiameter(strokeWidth) / 2f)
                            }
                        } else {
                            activeStrokeChartId = pageEntity
                                ?.getEffectiveLayers()
                                ?.asReversed()
                                ?.asSequence()
                                ?.flatMap { it.charts.asReversed().asSequence() }
                                ?.firstOrNull { chart ->
                                    rawPoint.x in chart.x..(chart.x + chart.width) &&
                                        rawPoint.y in chart.y..(chart.y + chart.height)
                                }
                                ?.id
                            activeStrokePoints.add(
                                StrokePoint(
                                    x = rawPoint.x,
                                    y = rawPoint.y,
                                    pressure = pressure,
                                    tilt = tilt,
                                    timestampMs = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    PointerAction.MOVE -> {
                        when (currentTool) {
                            ToolType.SELECTOR -> {
                                if (isDraggingGroup) {
                                    val dx = rawPoint.x - dragStartOffset.x
                                    val dy = rawPoint.y - dragStartOffset.y
                                    if (dx != 0f || dy != 0f) {
                                        onMoveSelectedGroup(dx, dy)
                                        dragStartOffset = rawPoint
                                    }
                                } else {
                                    val id = selectedElementId
                                    val type = selectedElementType
                                    if (id != null && type != null) {
                                        val delta = rawPoint - dragStartOffset
                                        if (delta.getDistanceSquared() > 0.01f) {
                                            transformPreview = if (isResizingCorner) {
                                                val (minimumWidth, minimumHeight) = minimumElementSize(type)
                                                calculateResizeTransform(
                                                    id = id,
                                                    type = type,
                                                    originalPosition = elementOriginalPos,
                                                    originalSize = elementOriginalSize,
                                                    rotationDegrees = elementOriginalRotation,
                                                    dragDelta = delta,
                                                    corner = resizingCorner ?: "BR",
                                                    minimumWidth = minimumWidth,
                                                    minimumHeight = minimumHeight
                                                )
                                            } else {
                                                ElementTransform(
                                                    id = id,
                                                    type = type,
                                                    x = elementOriginalPos.x + delta.x,
                                                    y = elementOriginalPos.y + delta.y,
                                                    width = elementOriginalSize.x,
                                                    height = elementOriginalSize.y,
                                                    anchor = "BR",
                                                    isResizing = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            ToolType.ERASER -> {
                                cursorPos = rawPoint
                                eraserTouchPos = rawPoint
                                if (eraserMode == EraserMode.PIXEL) {
                                    val last = activeEraserPoints.lastOrNull()
                                    // Keep a roughly constant screen-space sampling distance. A one-pixel
                                    // eraser previously recorded hundreds of nearly identical points.
                                    val minDistance = (3.5f / safeScale).coerceIn(0.8f, 3f)
                                    if (last == null || Offset(last.x - rawPoint.x, last.y - rawPoint.y).getDistance() >= minDistance) {
                                        activeEraserPoints.add(
                                            StrokePoint(rawPoint.x, rawPoint.y, pressure, tilt, System.currentTimeMillis())
                                        )
                                    }
                                } else {
                                    onEraseAtPoint(rawPoint, DrawingEngine.eraserDiameter(strokeWidth) / 2f)
                                }
                            }
                            ToolType.POINTER,
                            ToolType.FILL,
                            ToolType.EYEDROPPER,
                            ToolType.TEXT -> {
                                if ((rawPoint - pendingUtilityDown).getDistance() > 12f / safeScale) {
                                    pendingUtilityTap = null
                                }
                            }
                            ToolType.RULER -> Unit
                            else -> {
                                val constrainedScreenPoint = rulerGuideEdge?.let { edge ->
                                    rulerState.projectOn(edge, screenPoint)
                                } ?: rulerState.edgeContact(
                                    previousPoint = change.previousPosition,
                                    currentPoint = screenPoint,
                                    contactZone = 18f
                                )?.let { (contact, edge) ->
                                    rulerGuideEdge = edge
                                    contact
                                }
                                if (constrainedScreenPoint != null) {
                                    rawPoint = Offset(
                                        (constrainedScreenPoint.x - panOffset.x) / safeScale,
                                        (constrainedScreenPoint.y - panOffset.y) / safeScale
                                    )
                                }
                                cursorPos = rawPoint
                                activeStrokePoints.add(
                                    StrokePoint(
                                        x = rawPoint.x,
                                        y = rawPoint.y,
                                        pressure = pressure,
                                        tilt = tilt,
                                        timestampMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                    PointerAction.UP -> {
                        pendingUtilityTap?.takeIf { it.first == currentTool }?.let { (tool, tapPoint) ->
                            when (tool) {
                                ToolType.POINTER -> latestPageEntity?.findTopElementAt(
                                    point = tapPoint,
                                    radius = 18f / safeScale,
                                    candidateIds = linkedElementIds
                                )?.let { onLinkedElementActivated(it.id) }
                                ToolType.FILL -> {
                                    val hit = latestPageEntity?.findTopElementAt(tapPoint, 14f / safeScale)
                                    onFillElement(hit?.id, hit?.type)
                                }
                                ToolType.EYEDROPPER -> latestPageEntity
                                    ?.sampleColorAt(tapPoint, 14f / safeScale)
                                    ?.let(onColorSampled)
                                ToolType.TEXT -> onTextPositionRequested(tapPoint)
                                else -> Unit
                            }
                        }
                        pendingUtilityTap = null
                        if (currentTool.isStrokeTool() && activeStrokePoints.isNotEmpty()) {
                            val newStroke = StrokeEntity(
                                tool = currentTool,
                                colorHsla = currentColor.copy(alpha = strokeOpacity),
                                baseWidth = strokeWidth,
                                points = activeStrokePoints.toList(),
                                parentChartId = activeStrokeChartId
                            )
                            pendingCommittedStroke = newStroke
                            onStrokeAdded(newStroke)
                        }
                        if (currentTool == ToolType.ERASER) {
                            if (eraserMode == EraserMode.PIXEL) {
                                if (activeEraserPoints.isNotEmpty()) {
                                    onEraserMarkAdded(
                                        com.example.data.models.EraserMark(
                                            points = activeEraserPoints.toList(),
                                            width = DrawingEngine.eraserDiameter(strokeWidth)
                                        )
                                    )
                                }
                                onEndEraserGesture()
                            } else if (isObjectEraserGesture) {
                                onEndEraserGesture()
                            }
                        }
                        transformPreview?.let { preview ->
                            onResizeAndMoveElement(
                                preview.id,
                                preview.type,
                                preview.width,
                                preview.height,
                                preview.x,
                                preview.y,
                                preview.anchor,
                                preview.isResizing
                            )
                        }
                        transformPreview = null
                        isObjectEraserGesture = false
                        activeStrokePoints.clear()
                        activeEraserPoints.clear()
                        activeStrokeChartId = null
                        eraserTouchPos = null
                        resizingCorner = null
                        if (isDraggingGroup) onEndMoveSelectedGroup()
                        isDraggingGroup = false
                        rulerGuideEdge = null
                        cursorPos = null
                    }
                }
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            if (canvasSize != size) {
                canvasSize = size
            }

            // Paper Sheet Mode (A4, Letter, etc.)
            canvasEntity?.let { c ->
                if (c.pageSizePreset != PageSizePreset.UNLIMITED) {
                    val (pageW, pageH) = when (c.pageSizePreset) {
                        PageSizePreset.A4_VERTICAL -> Pair(794f, 1123f)
                        PageSizePreset.A4_HORIZONTAL -> Pair(1123f, 794f)
                        PageSizePreset.RATIO_16_9_VERTICAL -> Pair(1080f, 1920f)
                        PageSizePreset.RATIO_16_9_HORIZONTAL -> Pair(1920f, 1080f)
                        PageSizePreset.LETTER_11X85 -> Pair(816f, 1056f)
                        PageSizePreset.CUSTOM -> Pair(c.customWidth ?: 800f, c.customHeight ?: 1200f)
                        else -> Pair(794f, 1123f)
                    }
                    val left = panOffset.x
                    val top = panOffset.y
                    val scaledW = pageW * currentScale
                    val scaledH = pageH * currentScale

                    // Shadow
                    drawRect(
                        color = Color(0x66000000),
                        topLeft = Offset(left + 10f * currentScale, top + 10f * currentScale),
                        size = androidx.compose.ui.geometry.Size(scaledW, scaledH)
                    )
                    // White Paper Sheet
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(scaledW, scaledH)
                    )
                    // Border
                    drawRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(scaledW, scaledH),
                        style = Stroke(width = 2f * currentScale)
                    )
                }
            }

            // 1. Render Background Grid / Pattern
            val gridColor = if (isDarkBackground) Color(0x28FFFFFF) else Color(0x331E293B)
            when (pattern) {
                BackgroundPattern.DOTTED -> {
                    val dotSpacing = 36f * currentScale
                    val dotRadius = (3f * Math.sqrt(currentScale.toDouble()).toFloat()).coerceIn(2f, 8f)
                    var x = panOffset.x % dotSpacing
                    if (x < 0) x += dotSpacing
                    while (x < canvasWidth) {
                        var y = panOffset.y % dotSpacing
                        if (y < 0) y += dotSpacing
                        while (y < canvasHeight) {
                            drawCircle(
                                color = gridColor,
                                radius = dotRadius,
                                center = Offset(Math.round(x).toFloat(), Math.round(y).toFloat())
                            )
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }
                BackgroundPattern.LINED -> {
                    val lineSpacing = 36f * currentScale
                    var y = panOffset.y % lineSpacing
                    if (y < 0) y += lineSpacing
                    while (y < canvasHeight) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.2f * currentScale.coerceAtLeast(0.8f)
                        )
                        y += lineSpacing
                    }
                }
                BackgroundPattern.GRID_SQUARE -> {
                    val spacing = 32f * currentScale
                    var x = panOffset.x % spacing
                    if (x < 0) x += spacing
                    while (x < canvasWidth) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), strokeWidth = 0.8f)
                        x += spacing
                    }
                    var y = panOffset.y % spacing
                    if (y < 0) y += spacing
                    while (y < canvasHeight) {
                        drawLine(gridColor, Offset(0f, y), Offset(canvasWidth, y), strokeWidth = 0.8f)
                        y += spacing
                    }
                }
                BackgroundPattern.GRID_ISOMETRIC -> {
                    val isoSpacing = 40f * currentScale
                    var startX = panOffset.x % (isoSpacing * 2) - canvasHeight
                    while (startX < canvasWidth + canvasHeight) {
                        drawLine(gridColor, Offset(startX, 0f), Offset(startX + canvasHeight * 0.577f, canvasHeight), strokeWidth = 0.7f)
                        startX += isoSpacing
                    }
                    startX = panOffset.x % (isoSpacing * 2) - canvasHeight
                    while (startX < canvasWidth + canvasHeight) {
                        drawLine(gridColor, Offset(startX + canvasHeight * 0.577f, 0f), Offset(startX, canvasHeight), strokeWidth = 0.7f)
                        startX += isoSpacing
                    }
                    var y = panOffset.y % isoSpacing
                    if (y < 0) y += isoSpacing
                    while (y < canvasHeight) {
                        drawLine(gridColor, Offset(0f, y), Offset(canvasWidth, y), strokeWidth = 0.7f)
                        y += isoSpacing
                    }
                }
                BackgroundPattern.PROTRACTOR -> {
                    val centerX = canvasWidth / 2 + panOffset.x
                    val centerY = canvasHeight / 2 + panOffset.y
                    val maxRadius = minOf(canvasWidth, canvasHeight) * 0.4f
                    for (r in 1..4) {
                        drawCircle(gridColor, radius = maxRadius * r / 4, center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(0.8f))
                    }
                    for (deg in 0 until 360 step 15) {
                        val rad = Math.toRadians(deg.toDouble())
                        val innerR = maxRadius * 0.1f
                        val outerR = maxRadius
                        drawLine(
                            gridColor,
                            Offset(centerX + (innerR * Math.cos(rad)).toFloat(), centerY + (innerR * Math.sin(rad)).toFloat()),
                            Offset(centerX + (outerR * Math.cos(rad)).toFloat(), centerY + (outerR * Math.sin(rad)).toFloat()),
                            strokeWidth = if (deg % 90 == 0) 1.5f else 0.6f
                        )
                    }
                }
                BackgroundPattern.MUSIC_STAFF -> {
                    val staffSpacing = 24f * currentScale
                    var y = panOffset.y % (staffSpacing * 8)
                    if (y < 0) y += staffSpacing * 8
                    while (y < canvasHeight) {
                        for (i in 0 until 5) {
                            drawLine(gridColor, Offset(0f, y + i * staffSpacing), Offset(canvasWidth, y + i * staffSpacing), strokeWidth = 1f)
                        }
                        y += staffSpacing * 8
                    }
                }
                BackgroundPattern.GRAPH_MM -> {
                    val mmSpacing = 8f * currentScale
                    var x = panOffset.x % mmSpacing
                    if (x < 0) x += mmSpacing
                    var colIdx = 0
                    while (x < canvasWidth) {
                        val isMajor = (colIdx % 5 == 0)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), strokeWidth = if (isMajor) 1.5f else 0.5f)
                        x += mmSpacing
                        colIdx++
                    }
                    var y = panOffset.y % mmSpacing
                    if (y < 0) y += mmSpacing
                    var rowIdx = 0
                    while (y < canvasHeight) {
                        val isMajor = (rowIdx % 5 == 0)
                        drawLine(gridColor, Offset(0f, y), Offset(canvasWidth, y), strokeWidth = if (isMajor) 1.5f else 0.5f)
                        y += mmSpacing
                        rowIdx++
                    }
                }
                BackgroundPattern.DOT_GRID -> {
                    val spacing = 28f * currentScale
                    var x = panOffset.x % spacing
                    if (x < 0) x += spacing
                    while (x < canvasWidth) {
                        var y = panOffset.y % spacing
                        if (y < 0) y += spacing
                        while (y < canvasHeight) {
                            drawCircle(color = gridColor, radius = 2f * currentScale.coerceAtLeast(0.8f), center = Offset(x, y))
                            y += spacing
                        }
                        x += spacing
                    }
                }
                BackgroundPattern.CORNELL_NOTES -> {
                    val marginX = 140f * currentScale + panOffset.x
                    val summaryY = canvasHeight - 160f * currentScale
                    drawLine(gridColor.copy(alpha = 0.6f), Offset(marginX, 0f), Offset(marginX, summaryY), strokeWidth = 2.5f)
                    drawLine(gridColor.copy(alpha = 0.6f), Offset(0f, summaryY), Offset(canvasWidth, summaryY), strokeWidth = 2.5f)
                }
                BackgroundPattern.KANBAN_TEMPLATE -> {
                    val colW = canvasWidth / 3f
                    drawLine(gridColor.copy(alpha = 0.5f), Offset(colW, 0f), Offset(colW, canvasHeight), strokeWidth = 2f)
                    drawLine(gridColor.copy(alpha = 0.5f), Offset(colW * 2, 0f), Offset(colW * 2, canvasHeight), strokeWidth = 2f)
                }
                BackgroundPattern.ISO_3D -> {
                    val isoSpacing = 65f * currentScale
                    var startX = panOffset.x % (isoSpacing * 2) - canvasHeight
                    while (startX < canvasWidth + canvasHeight) {
                        drawLine(gridColor, Offset(startX, 0f), Offset(startX + canvasHeight * 0.577f, canvasHeight), strokeWidth = 0.8f)
                        startX += isoSpacing
                    }
                    startX = panOffset.x % (isoSpacing * 2) - canvasHeight
                    while (startX < canvasWidth + canvasHeight) {
                        drawLine(gridColor, Offset(startX + canvasHeight * 0.577f, 0f), Offset(startX, canvasHeight), strokeWidth = 0.8f)
                        startX += isoSpacing
                    }
                }
                BackgroundPattern.BLANK, BackgroundPattern.NONE -> {}
            }

            // ═══════════════════════════════════════════════════════
            // 2. Render Page Elements per Layer (bottom to top)
            // ═══════════════════════════════════════════════════════
            pageEntity?.let { page ->
                page.visibleLayersBottomUp().forEach { layer ->
                    val layerAlpha = layer.opacity.coerceIn(0f, 1f)

                    // ─── 2a. IMAGES (зображення) ───
                    layer.images.forEach { image ->
                        val renderedImage = transformPreview
                            ?.takeIf { it.id == image.id && it.type == "IMAGE" }
                            ?.let { image.copy(x = it.x, y = it.y, width = it.width, height = it.height) }
                            ?: image
                        val pivotX = renderedImage.x * currentScale + panOffset.x + (renderedImage.width * currentScale) / 2f
                        val pivotY = renderedImage.y * currentScale + panOffset.y + (renderedImage.height * currentScale) / 2f

                        rotate(degrees = renderedImage.rotation, pivot = Offset(pivotX, pivotY)) {
                            drawIntoCanvas { canvas ->
                                try {
                                    val bitmap = getCachedBitmap(renderedImage.sourceUri)
                                    if (bitmap != null) {
                                        val paint = android.graphics.Paint().apply {
                                            alpha = (renderedImage.opacity.coerceIn(0.1f, 1.0f) * layerAlpha * 255).toInt()
                                            isAntiAlias = true
                                            isFilterBitmap = true
                                        }
                                        val dstRect = android.graphics.RectF(
                                            renderedImage.x * currentScale + panOffset.x,
                                            renderedImage.y * currentScale + panOffset.y,
                                            (renderedImage.x + renderedImage.width) * currentScale + panOffset.x,
                                            (renderedImage.y + renderedImage.height) * currentScale + panOffset.y
                                        )
                                        canvas.nativeCanvas.drawBitmap(bitmap, null, dstRect, paint)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("InteractiveCanvas", "Error rendering image bitmap", e)
                                }
                            }
                            if (getCachedBitmap(renderedImage.sourceUri) == null) {
                                drawRect(
                                    color = Color(0x4438BDF8),
                                    topLeft = Offset(
                                        renderedImage.x * currentScale + panOffset.x,
                                        renderedImage.y * currentScale + panOffset.y
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        renderedImage.width * currentScale,
                                        renderedImage.height * currentScale
                                    ),
                                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
                                )
                            }
                        }
                    }

                    // ─── 2b. SHAPES (фігури) ───
                    layer.shapes.forEach { shape ->
                        val renderedShape = transformPreview
                            ?.takeIf { it.id == shape.id && it.type == "SHAPE" }
                            ?.let { shape.copy(x = it.x, y = it.y, width = it.width, height = it.height) }
                            ?: shape
                        val pivotX = renderedShape.x * currentScale + panOffset.x + (renderedShape.width * currentScale) / 2f
                        val pivotY = renderedShape.y * currentScale + panOffset.y + (renderedShape.height * currentScale) / 2f

                        rotate(degrees = renderedShape.rotation, pivot = Offset(pivotX, pivotY)) {
                            val path = DrawingEngine.createShapePath(
                                renderedShape.shapeType,
                                Rect(
                                    renderedShape.x * currentScale + panOffset.x,
                                    renderedShape.y * currentScale + panOffset.y,
                                    (renderedShape.x + renderedShape.width) * currentScale + panOffset.x,
                                    (renderedShape.y + renderedShape.height) * currentScale + panOffset.y
                                )
                            )
                            val fillColor = Color(renderedShape.fillColor)
                            val strokeColor = Color(renderedShape.strokeColor)
                            drawPath(path, fillColor.copy(alpha = fillColor.alpha * layerAlpha))
                            drawPath(path, strokeColor.copy(alpha = strokeColor.alpha * layerAlpha), style = Stroke(renderedShape.strokeWidth * currentScale))
                        }
                    }

                    // ─── 2c. CHARTS (графіки / координатна сітка) ───
                    layer.charts.forEach { storedChart ->
                        val chart = transformPreview
                            ?.takeIf { it.id == storedChart.id && it.type == "CHART" }
                            ?.let { preview ->
                                if (preview.isResizing) {
                                    storedChart.resizeFramePreservingOrigin(
                                        newX = preview.x,
                                        newY = preview.y,
                                        newWidth = preview.width,
                                        newHeight = preview.height
                                    )
                                } else {
                                    storedChart.withSquareGrid().copy(
                                        x = preview.x,
                                        y = preview.y,
                                        width = preview.width,
                                        height = preview.height
                                    )
                                }
                            }
                            ?: storedChart
                        val cx = chart.x * currentScale + panOffset.x
                        val cy = chart.y * currentScale + panOffset.y
                        val cw = chart.width * currentScale
                        val ch = chart.height * currentScale
                        val chartCenter = Offset(cx + cw / 2f, cy + ch / 2f)

                        rotate(degrees = chart.rotation, pivot = chartCenter) {

                        // Background
                        val chartBgColor = if (chart.backgroundColor != 0) Color(chart.backgroundColor) else if (isDarkBackground) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        drawRect(
                            color = chartBgColor.copy(alpha = chartBgColor.alpha * layerAlpha),
                            topLeft = Offset(cx, cy),
                            size = androidx.compose.ui.geometry.Size(cw, ch)
                        )

                        // Border
                        drawRect(
                            color = (if (isDarkBackground) Color(0xFF475569) else Color(0xFFCBD5E1)).copy(alpha = layerAlpha),
                            topLeft = Offset(cx, cy),
                            size = androidx.compose.ui.geometry.Size(cw, ch),
                            style = Stroke(width = 1.5f)
                        )

                        val stepX = if (chart.xStep > 0f) chart.xStep else 1f
                        val stepY = if (chart.yStep > 0f) chart.yStep else 1f
                        val unitsToPixelsX = chart.squarePixelsPerUnit()
                        val unitsToPixelsY = unitsToPixelsX
                        val localOriginX = chart.originOffsetX.takeIf { it >= 0f } ?: chart.width / 2f
                        val localOriginY = chart.originOffsetY.takeIf { it >= 0f } ?: chart.height / 2f
                        val axisYScreenX = cx + localOriginX * currentScale
                        val axisXScreenY = cy + localOriginY * currentScale
                        val visibleXMin = -localOriginX / unitsToPixelsX
                        val visibleXMax = (chart.width - localOriginX) / unitsToPixelsX
                        val visibleYMin = -(chart.height - localOriginY) / unitsToPixelsY
                        val visibleYMax = localOriginY / unitsToPixelsY
                        val xTicks = axisTickValues(visibleXMin, visibleXMax, stepX)
                        val yTicks = axisTickValues(visibleYMin, visibleYMax, stepY)

                        // A coordinate plane is anchored at (0, 0), not at the centre of its
                        // frame. Enlarging the frame therefore adds cells around the existing
                        // plane without shifting either axis to a different cell.
                        val gridSpacingX = unitsToPixelsX * currentScale
                        val gridSpacingY = unitsToPixelsY * currentScale
                        // At low canvas zoom, skip lattice indices instead of changing their
                        // spacing. This keeps the visible grid and every tick anchored to 0.
                        val gridStrideX = kotlin.math.ceil(2f / gridSpacingX).toInt().coerceAtLeast(1)
                        val gridStrideY = kotlin.math.ceil(2f / gridSpacingY).toInt().coerceAtLeast(1)
                        val gridColor = (if (isDarkBackground) Color(0x42FFFFFF) else Color(0x30000000))
                            .copy(alpha = layerAlpha)
                        val firstGridX = kotlin.math.ceil((cx - axisYScreenX) / gridSpacingX).toInt()
                        val lastGridX = kotlin.math.floor((cx + cw - axisYScreenX) / gridSpacingX).toInt()
                        val alignedFirstGridX = kotlin.math.ceil(firstGridX.toFloat() / gridStrideX).toInt() * gridStrideX
                        for (gridIndex in alignedFirstGridX..lastGridX step gridStrideX) {
                            val gridX = axisYScreenX + gridIndex * gridSpacingX
                            drawLine(gridColor, Offset(gridX, cy), Offset(gridX, cy + ch), strokeWidth = 1f)
                        }
                        val firstGridY = kotlin.math.ceil((cy - axisXScreenY) / gridSpacingY).toInt()
                        val lastGridY = kotlin.math.floor((cy + ch - axisXScreenY) / gridSpacingY).toInt()
                        val alignedFirstGridY = kotlin.math.ceil(firstGridY.toFloat() / gridStrideY).toInt() * gridStrideY
                        for (gridIndex in alignedFirstGridY..lastGridY step gridStrideY) {
                            val gridY = axisXScreenY + gridIndex * gridSpacingY
                            drawLine(gridColor, Offset(cx, gridY), Offset(cx + cw, gridY), strokeWidth = 1f)
                        }

                        // Axes (X and Y through origin 0,0)
                        val axisStrokeWidth = (2.5f * currentScale).coerceAtLeast(2f)
                        val axisColor = if (isDarkBackground) Color(0xFFCBD5E1) else Color(0xFF475569)
                        val xAxisVisible = axisXScreenY in cy..(cy + ch)
                        val yAxisVisible = axisYScreenX in cx..(cx + cw)
                        if (xAxisVisible) drawLine(axisColor, Offset(cx, axisXScreenY), Offset(cx + cw, axisXScreenY), strokeWidth = axisStrokeWidth)
                        if (yAxisVisible) drawLine(axisColor, Offset(axisYScreenX, cy), Offset(axisYScreenX, cy + ch), strokeWidth = axisStrokeWidth)
                        val tickHalfLength = (4f * currentScale).coerceIn(3f, 10f)
                        // Ticks describe every physical grid cell. Numeric labels can be hidden,
                        // but the small reference marks must remain visible at every zoom level.
                        if (xAxisVisible) for (gridIndex in firstGridX..lastGridX) {
                            val tickX = axisYScreenX + gridIndex * gridSpacingX
                            drawLine(axisColor, Offset(tickX, axisXScreenY - tickHalfLength), Offset(tickX, axisXScreenY + tickHalfLength), strokeWidth = axisStrokeWidth * 0.65f)
                        }
                        if (yAxisVisible) for (gridIndex in firstGridY..lastGridY) {
                            val tickY = axisXScreenY + gridIndex * gridSpacingY
                            drawLine(axisColor, Offset(axisYScreenX - tickHalfLength, tickY), Offset(axisYScreenX + tickHalfLength, tickY), strokeWidth = axisStrokeWidth * 0.65f)
                        }

                        // Axis labels
                        if (chart.showAxisLabels && chart.axisLabelsVisible) {
                            val labelTextSize = with(density) { 11.sp.toPx() }.coerceIn(14f, 24f)
                            drawIntoCanvas { canvas ->
                                val textPaint = android.text.TextPaint().apply {
                                    color = if (isDarkBackground) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                                    textSize = labelTextSize
                                    isAntiAlias = true
                                }
                                // X-axis labels
                                if (xAxisVisible) xTicks.forEach { wx ->
                                    val screenX = axisYScreenX + wx * unitsToPixelsX * currentScale
                                    if (screenX in cx..cx + cw) {
                                        val labelText = if (wx == wx.toInt().toFloat()) wx.toInt().toString() else String.format(java.util.Locale.US, "%.1f", wx)
                                        canvas.nativeCanvas.drawText(labelText, screenX - 6f * currentScale, (axisXScreenY + 14f * currentScale).coerceAtMost(cy + ch - 4f), textPaint)
                                    }
                                }
                                // Y-axis labels
                                if (yAxisVisible) yTicks.forEach { wy ->
                                    if (kotlin.math.abs(wy) > 0.0001f) {
                                        val screenY = axisXScreenY - wy * unitsToPixelsY * currentScale
                                        if (screenY in cy..cy + ch) {
                                            val labelText = if (wy == wy.toInt().toFloat()) wy.toInt().toString() else String.format(java.util.Locale.US, "%.1f", wy)
                                            canvas.nativeCanvas.drawText(labelText, (axisYScreenX + 4f * currentScale).coerceAtMost(cx + cw - 12f), screenY + 4f * currentScale, textPaint)
                                        }
                                    }
                                }
                                if (xAxisVisible) canvas.nativeCanvas.drawText("X", cx + cw - 18f, axisXScreenY - 8f, textPaint)
                                if (yAxisVisible) canvas.nativeCanvas.drawText("Y", axisYScreenX + 8f, cy + 18f, textPaint)
                            }
                        }
                    }

                    // ─── 2d. TEXT BLOCKS (текстові блоки) ───
                    layer.textBlocks.forEach { storedTextBlock ->
                        val textBlock = transformPreview
                            ?.takeIf { it.id == storedTextBlock.id && it.type == "TEXT" }
                            ?.let { storedTextBlock.copy(x = it.x, y = it.y, width = it.width, height = it.height) }
                            ?: storedTextBlock
                        val textCenter = Offset(
                            (textBlock.x + textBlock.width / 2f) * currentScale + panOffset.x,
                            (textBlock.y + textBlock.height / 2f) * currentScale + panOffset.y
                        )
                        rotate(degrees = textBlock.rotation, pivot = textCenter) {
                        drawIntoCanvas { canvas ->
                            val textPaint = android.text.TextPaint().apply {
                                val baseColor = if (textBlock.color == 0xFF000000.toInt() && isDarkBackground)
                                    android.graphics.Color.WHITE
                                else
                                    textBlock.color
                                color = baseColor
                                alpha = (android.graphics.Color.alpha(baseColor) * layerAlpha).toInt()
                                textSize = textBlock.fontSize * currentScale * 1.5f
                                isAntiAlias = true
                                isFakeBoldText = textBlock.isBold
                                isUnderlineText = textBlock.isUnderline
                                typeface = when (textBlock.fontFamily.uppercase()) {
                                    "SERIF" -> android.graphics.Typeface.SERIF
                                    "MONO" -> android.graphics.Typeface.MONOSPACE
                                    else -> android.graphics.Typeface.SANS_SERIF
                                }.let { family ->
                                    android.graphics.Typeface.create(
                                        family,
                                        when {
                                            textBlock.isBold && textBlock.isItalic -> android.graphics.Typeface.BOLD_ITALIC
                                            textBlock.isBold -> android.graphics.Typeface.BOLD
                                            textBlock.isItalic -> android.graphics.Typeface.ITALIC
                                            else -> android.graphics.Typeface.NORMAL
                                        }
                                    )
                                }
                            }
                            val maxWidth = (textBlock.width * currentScale).toInt().coerceAtLeast(100)
                            val staticLayout = android.text.StaticLayout.Builder
                                .obtain(textBlock.text, 0, textBlock.text.length, textPaint, maxWidth)
                                .setAlignment(when (textBlock.alignment.uppercase()) {
                                    "CENTER" -> android.text.Layout.Alignment.ALIGN_CENTER
                                    "RIGHT" -> android.text.Layout.Alignment.ALIGN_OPPOSITE
                                    else -> android.text.Layout.Alignment.ALIGN_NORMAL
                                })
                                .setLineSpacing(2f, 1f)
                                .build()

                            canvas.nativeCanvas.save()
                            canvas.nativeCanvas.translate(
                                textBlock.x * currentScale + panOffset.x,
                                textBlock.y * currentScale + panOffset.y
                            )
                            staticLayout.draw(canvas.nativeCanvas)
                            canvas.nativeCanvas.restore()
                        }
                        }
                    }

                    // ─── 2e. STROKES & ERASER MARKS (fixed logical-pixel raster cache) ───
                    if (layer.strokes.isNotEmpty() || layer.eraserMarks.isNotEmpty() || (activeEraserPoints.isNotEmpty() && !layer.isLocked)) {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas

                            val hasLiveMask = activeEraserPoints.isNotEmpty() && !layer.isLocked
                            val chartPreview = transformPreview?.takeIf { it.type == "CHART" }
                            val previewSourceChart = chartPreview?.let { preview ->
                                layer.charts.firstOrNull { it.id == preview.id }
                            }
                            fun belongsToPreviewChart(stroke: StrokeEntity): Boolean {
                                val chart = previewSourceChart ?: return false
                                return stroke.isAttachedToChart(chart)
                            }
                            fun transformChartPoint(point: StrokePoint): StrokePoint {
                                val chart = previewSourceChart ?: return point
                                val preview = chartPreview ?: return point
                                // Resizing a graph reveals more fixed-size cells. Only a whole
                                // graph move carries its handwriting along in the live preview.
                                if (preview.isResizing) return point
                                return point.copy(
                                    x = point.x + preview.x - chart.x,
                                    y = point.y + preview.y - chart.y
                                )
                            }
                            layer.strokes.forEach { storedStroke ->
                                val attachedToPreview = belongsToPreviewChart(storedStroke)
                                val stroke = if (attachedToPreview) {
                                    storedStroke.copy(points = storedStroke.points.map(::transformChartPoint))
                                } else storedStroke
                                val masksForStroke = layer.eraserMarks.map { mark ->
                                    if (attachedToPreview) mark.copy(points = mark.points.map(::transformChartPoint)) else mark
                                }
                                val masksToRender = if (hasLiveMask) {
                                    masksForStroke + EraserMark(
                                        points = activeEraserPoints.toList(),
                                        width = DrawingEngine.eraserDiameter(strokeWidth),
                                        affectedStrokeIds = listOf(storedStroke.id)
                                    )
                                } else {
                                    masksForStroke
                                }
                                // A stroke is rasterized in logical canvas pixels once, then zoomed with
                                // nearest-neighbour sampling. Pixel erasing changes that raster only;
                                // it never splits or smooths the persisted stroke geometry.
                                RasterStrokeCompositor.drawRasterStroke(
                                    nativeCanvas,
                                    stroke,
                                    masks = masksToRender,
                                    layerAlpha = layerAlpha,
                                    scale = currentScale,
                                    panX = panOffset.x,
                                    panY = panOffset.y,
                                    useCache = !hasLiveMask
                                )
                            }
                        }
                    }
                }
            }

            // 3. Render Active Drawing Stroke (double-buffered with pendingCommittedStroke to eliminate 1-frame disappearance)
            val pendingAlreadyStored = pendingCommittedStroke?.let { pending ->
                pageEntity?.getEffectiveLayers()?.any { layer ->
                    layer.strokes.any { stored -> stored.id == pending.id }
                } == true
            } == true
            val pendingToDraw = pendingCommittedStroke?.takeUnless { pendingAlreadyStored }
            val strokePointsToDraw = if (activeStrokePoints.isNotEmpty()) activeStrokePoints.toList() else pendingToDraw?.points
            val activeTool = if (activeStrokePoints.isNotEmpty()) currentTool else (pendingToDraw?.tool ?: currentTool)
            val activeBaseWidth = if (activeStrokePoints.isNotEmpty()) strokeWidth else (pendingToDraw?.baseWidth ?: strokeWidth)
            val activeOpacity = if (activeStrokePoints.isNotEmpty()) strokeOpacity else (pendingToDraw?.colorHsla?.alpha ?: strokeOpacity)
            val activeColor = if (activeStrokePoints.isNotEmpty()) currentColor else (pendingToDraw?.colorHsla ?: currentColor)

            if (!strokePointsToDraw.isNullOrEmpty()) {
                val activeStroke = if (activeStrokePoints.isNotEmpty()) {
                    StrokeEntity(
                        tool = activeTool,
                        colorHsla = activeColor.copy(alpha = activeOpacity),
                        baseWidth = activeBaseWidth,
                        points = strokePointsToDraw
                    )
                } else {
                    pendingToDraw
                }
                if (activeStroke != null) {
                    drawIntoCanvas { canvas ->
                        RasterStrokeCompositor.drawRasterStroke(
                            canvas.nativeCanvas,
                            activeStroke,
                            masks = emptyList(),
                            scale = currentScale,
                            panX = panOffset.x,
                            panY = panOffset.y,
                            useCache = false
                        )
                    }
                }
            }

            // 4. Precision Circle Eraser Preview Indicator
            eraserTouchPos?.let { pos ->
                val screenX = pos.x * currentScale + panOffset.x
                val screenY = pos.y * currentScale + panOffset.y
                val circleRadius = DrawingEngine.eraserDiameter(strokeWidth) * currentScale / 2f

                drawCircle(
                    color = Color(0x33EF4444),
                    radius = circleRadius,
                    center = Offset(screenX, screenY)
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = circleRadius,
                    center = Offset(screenX, screenY),
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                )
            }

            // Linked objects are always recognizable, even before switching to the pointer tool.
            if (pageEntity != null) {
                linkedElementIds.forEach { linkedId ->
                    if (linkedId !in selectedElementIds) {
                        pageEntity.selectionBounds(setOf(linkedId))?.let { bounds ->
                            val left = bounds.left * currentScale + panOffset.x
                            val top = bounds.top * currentScale + panOffset.y
                            val right = bounds.right * currentScale + panOffset.x
                            val bottom = bounds.bottom * currentScale + panOffset.y
                            val linkColor = Color(0xFF8B5CF6)
                            drawRect(
                                color = linkColor.copy(alpha = 0.9f),
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = Stroke(
                                    width = if (currentTool == ToolType.POINTER) 3f else 1.7f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f)
                                )
                            )
                            val badge = Offset(right, top)
                            drawCircle(linkColor, radius = 8.dp.toPx(), center = badge)
                            drawCircle(Color.White, radius = 3.dp.toPx(), center = badge, style = Stroke(width = 1.5f))
                        }
                        }
                    }
                }
            }

            // 5. Highlight Selected Element Bounding Box
            val selId = selectedElementId
            val selType = selectedElementType
            if (selectedGroupBounds == null && selId != null && selType != null && pageEntity != null) {
                var elemRect: Rect? = null
                var elemRotation = 0f
                val preview = transformPreview?.takeIf { it.id == selId && it.type == selType }

                when (selType) {
                    "SHAPE" -> pageEntity.findShape(selId)?.let {
                        elemRect = preview?.let { p -> Rect(p.x, p.y, p.x + p.width, p.y + p.height) }
                            ?: Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                        elemRotation = it.rotation
                    }
                    "IMAGE" -> pageEntity.findImage(selId)?.let {
                        elemRect = preview?.let { p -> Rect(p.x, p.y, p.x + p.width, p.y + p.height) }
                            ?: Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                        elemRotation = it.rotation
                    }
                    "TEXT" -> pageEntity.findText(selId)?.let {
                        elemRect = preview?.let { p -> Rect(p.x, p.y, p.x + p.width, p.y + p.height) }
                            ?: Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                        elemRotation = it.rotation
                    }
                    "CHART" -> pageEntity.findChart(selId)?.let {
                        elemRect = preview?.let { p -> Rect(p.x, p.y, p.x + p.width, p.y + p.height) }
                            ?: Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                        elemRotation = it.rotation
                    }
                    "CODE" -> pageEntity.findCodeBlock(selId)?.let {
                        elemRect = preview?.let { p -> Rect(p.x, p.y, p.x + p.width, p.y + p.height) }
                            ?: Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                        elemRotation = it.rotation
                    }
                }

                elemRect?.let { r ->
                    val left = r.left * currentScale + panOffset.x
                    val top = r.top * currentScale + panOffset.y
                    val right = r.right * currentScale + panOffset.x
                    val bottom = r.bottom * currentScale + panOffset.y
                    val centerOffset = Offset((left + right) / 2f, (top + bottom) / 2f)

                    rotate(degrees = elemRotation, pivot = centerOffset) {
                        drawRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )

                        val handleRadius = 6.dp.toPx()
                        drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(left, top))
                        drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(right, top))
                        drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(left, bottom))
                        drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(right, bottom))
                    }
                }
            }

            selectedGroupBounds?.let { bounds ->
                val left = bounds.left * currentScale + panOffset.x
                val top = bounds.top * currentScale + panOffset.y
                val right = bounds.right * currentScale + panOffset.x
                val bottom = bounds.bottom * currentScale + panOffset.y
                val handleRadius = 6.dp.toPx()

                drawRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
                drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(left, top))
                drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(right, top))
                drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(left, bottom))
                drawCircle(color = Color(0xFF38BDF8), radius = handleRadius, center = Offset(right, bottom))
            }

            // 6. Brush Cursor & Slider Change Preview Circle Overlay
            cursorPos?.let { cp ->
                val screen = Offset(cp.x * currentScale + panOffset.x, cp.y * currentScale + panOffset.y)
                when (currentTool) {
                    ToolType.ERASER -> drawCircle(
                        color = if (isDarkBackground) Color.White else Color(0xFF334155),
                        radius = DrawingEngine.eraserDiameter(strokeWidth) * currentScale / 2f,
                        center = screen,
                        style = Stroke(2f)
                    )
                    ToolType.SELECTOR -> {}
                    else -> drawCircle(
                        color = currentColor.copy(alpha = strokeOpacity).toColor(),
                        radius = (strokeWidth / 2f) * currentScale,
                        center = screen
                    )
                }
            } ?: run {
                if (previewPulse && currentTool != ToolType.SELECTOR) {
                    val viewportCenter = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    if (currentTool == ToolType.ERASER) {
                        drawCircle(
                            color = if (isDarkBackground) Color.White else Color(0xFF334155),
                            radius = DrawingEngine.eraserDiameter(strokeWidth) * currentScale / 2f,
                            center = viewportCenter,
                            style = Stroke(2f * currentScale)
                        )
                    } else {
                        drawCircle(
                            color = currentColor.copy(alpha = strokeOpacity).toColor(),
                            radius = (strokeWidth / 2f) * currentScale,
                            center = viewportCenter
                        )
                        drawCircle(
                            color = if (isDarkBackground) Color.White.copy(alpha = 0.9f) else Color(0xFF334155).copy(alpha = 0.9f),
                            radius = (strokeWidth / 2f) * currentScale,
                            center = viewportCenter,
                            style = Stroke(1.5f)
                        )
                    }
                }
            }
        }

        pageEntity?.visibleLayersBottomUp()?.forEach { layer ->
            layer.codeBlocks.forEach { storedCodeBlock ->
                val codeBlock = transformPreview
                    ?.takeIf { it.id == storedCodeBlock.id && it.type == "CODE" }
                    ?.let { storedCodeBlock.copy(x = it.x, y = it.y, width = it.width, height = it.height) }
                    ?: storedCodeBlock
                CodeBlockCanvasCard(
                    codeBlock = codeBlock,
                    scale = currentScale,
                    panOffset = panOffset,
                    onEdit = { onEditCodeBlock(codeBlock.id) },
                    onRun = { onRunCodeBlock(codeBlock.id) },
                    onDelete = { onDeleteElement(codeBlock.id, "CODE") },
                    onSelect = {
                        selectedElementId = codeBlock.id
                        selectedElementType = "CODE"
                        elementOriginalPos = Offset(codeBlock.x, codeBlock.y)
                        elementOriginalSize = Offset(codeBlock.width, codeBlock.height)
                        elementOriginalRotation = codeBlock.rotation
                        onElementSelected(codeBlock.id, "CODE")
                    },
                    isSelected = codeBlock.id in selectedElementIds,
                    isInteractive = currentTool == ToolType.SELECTOR
                )
            }
        }

        // Floating Action Toolbar Overlay for Selected Element
        val selId = selectedElementId
        val selType = selectedElementType
        val density = LocalDensity.current
        if (selId != null && selType != null && pageEntity != null) {
            var elemPos: Offset? = null
            var elemSize: Offset? = null
            var currentImgOpacity = 1.0f

            when (selType) {
                "SHAPE" -> pageEntity.findShape(selId)?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                }
                "IMAGE" -> pageEntity.findImage(selId)?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                    currentImgOpacity = it.opacity
                }
                "TEXT" -> pageEntity.findText(selId)?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                }
                "CHART" -> pageEntity.findChart(selId)?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                }
                "CODE" -> pageEntity.findCodeBlock(selId)?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                }
            }
            transformPreview?.takeIf { it.id == selId && it.type == selType }?.let { preview ->
                elemPos = Offset(preview.x, preview.y)
                elemSize = Offset(preview.width, preview.height)
            }

            if (elemPos != null && elemSize != null) {
                val offsetYPx = with(density) { 56.dp.toPx() }
                val screenX = (elemPos!!.x * currentScale + panOffset.x).roundToInt()
                val screenY = ((elemPos!!.y * currentScale + panOffset.y) - offsetYPx).roundToInt().coerceAtLeast(16)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(screenX, screenY) }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitFirstDown().consume()
                                }
                            }
                        }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Resize bigger
                            IconButton(onClick = {
                                val newW = elemSize!!.x * 1.25f
                                val newH = elemSize!!.y * 1.25f
                                onResizeElement(selId, selType, newW, newH, if (selType == "CHART") "CENTER" else "BR")
                            }) {
                                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = stringResource(R.string.zoom_in), tint = MaterialTheme.colorScheme.primary)
                            }

                            // Resize smaller
                            IconButton(onClick = {
                                val newW = (elemSize!!.x * 0.8f).coerceAtLeast(60f)
                                val newH = (elemSize!!.y * 0.8f).coerceAtLeast(60f)
                                onResizeElement(selId, selType, newW, newH, if (selType == "CHART") "CENTER" else "BR")
                            }) {
                                Icon(imageVector = Icons.Default.ZoomOut, contentDescription = stringResource(R.string.zoom_out), tint = MaterialTheme.colorScheme.primary)
                            }

                            // Image Opacity toggle
                            if (selType == "IMAGE") {
                                IconButton(onClick = {
                                    val nextOpacity = when {
                                        currentImgOpacity > 0.85f -> 0.6f
                                        currentImgOpacity > 0.5f -> 0.3f
                                        else -> 1.0f
                                    }
                                    onUpdateImageOpacity(selId, nextOpacity)
                                }) {
                                    Icon(imageVector = Icons.Default.Opacity, contentDescription = stringResource(R.string.opacity), tint = MaterialTheme.colorScheme.primary)
                                }
                                Text("${(currentImgOpacity * 100).roundToInt()}%", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }

                            if (selType == "TEXT") {
                                IconButton(onClick = { onEditTextBlock(selId) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit text",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Rotate button
                            IconButton(onClick = {
                                onRotateElement(selId, selType)
                            }) {
                                Icon(imageVector = Icons.Default.RotateRight, contentDescription = stringResource(R.string.rotate), tint = MaterialTheme.colorScheme.primary)
                            }

                            // Delete button
                            IconButton(onClick = {
                                onDeleteElement(selId, selType)
                                selectedElementId = null
                                selectedElementType = null
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class PointerAction { DOWN, MOVE, UP }

internal data class ElementTransform(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val anchor: String,
    val isResizing: Boolean = true
)

private fun minimumElementSize(type: String): Pair<Float, Float> = when (type) {
    "SHAPE" -> 30f to 30f
    "IMAGE" -> 50f to 50f
    "TEXT" -> 60f to 30f
    "CHART" -> 100f to 100f
    "CODE" -> 180f to 120f
    else -> 30f to 30f
}

/** Keeps the opposite corner fixed, including for rotated shapes and images. */
internal fun calculateResizeTransform(
    id: String,
    type: String,
    originalPosition: Offset,
    originalSize: Offset,
    rotationDegrees: Float,
    dragDelta: Offset,
    corner: String,
    minimumWidth: Float,
    minimumHeight: Float
): ElementTransform {
    val angle = Math.toRadians(rotationDegrees.toDouble())
    val cosine = kotlin.math.cos(angle).toFloat()
    val sine = kotlin.math.sin(angle).toFloat()
    val localDx = dragDelta.x * cosine + dragDelta.y * sine
    val localDy = -dragDelta.x * sine + dragDelta.y * cosine
    val widthDelta = if (corner == "TL" || corner == "BL") -localDx else localDx
    val heightDelta = if (corner == "TL" || corner == "TR") -localDy else localDy
    val newWidth = (originalSize.x + widthDelta).coerceAtLeast(minimumWidth)
    val newHeight = (originalSize.y + heightDelta).coerceAtLeast(minimumHeight)

    val centerShiftX = (newWidth - originalSize.x) / 2f *
        if (corner == "TL" || corner == "BL") -1f else 1f
    val centerShiftY = (newHeight - originalSize.y) / 2f *
        if (corner == "TL" || corner == "TR") -1f else 1f
    val worldShiftX = centerShiftX * cosine - centerShiftY * sine
    val worldShiftY = centerShiftX * sine + centerShiftY * cosine
    val originalCenterX = originalPosition.x + originalSize.x / 2f
    val originalCenterY = originalPosition.y + originalSize.y / 2f

    return ElementTransform(
        id = id,
        type = type,
        x = originalCenterX + worldShiftX - newWidth / 2f,
        y = originalCenterY + worldShiftY - newHeight / 2f,
        width = newWidth,
        height = newHeight,
        anchor = corner
    )
}

/** Generates ticks from integer indices to avoid accumulated Float error dropping grid lines. */
internal fun axisTickValues(
    minimum: Float,
    maximum: Float,
    step: Float,
    maximumTickCount: Int = 501
): List<Float> {
    if (!minimum.isFinite() || !maximum.isFinite() || !step.isFinite() ||
        maximum < minimum || step <= 0f || maximumTickCount <= 0
    ) return emptyList()
    val endpointTolerance = step.toDouble() * 0.0001
    val firstIndex = kotlin.math.ceil((minimum.toDouble() - endpointTolerance) / step.toDouble()).toLong()
    val lastIndex = kotlin.math.floor((maximum.toDouble() + endpointTolerance) / step.toDouble()).toLong()
    if (firstIndex > lastIndex) return emptyList()
    val count = lastIndex - firstIndex + 1L
    val stride = kotlin.math.ceil(count.toDouble() / maximumTickCount).toLong().coerceAtLeast(1L)
    val ticks = mutableListOf<Float>()
    var index = firstIndex
    while (index <= lastIndex) {
        val value = (index.toFloat() * step).let { if (kotlin.math.abs(it) < step * 0.0001f) 0f else it }
        ticks += value
        if (lastIndex - index < stride) break
        index += stride
    }
    if (minimum <= 0f && maximum >= 0f && ticks.none { kotlin.math.abs(it) < step * 0.0001f }) {
        ticks += 0f
        ticks.sort()
    }
    return ticks
}

private fun isTouchOnRulerControls(point: Offset, ruler: RulerState): Boolean {
    val touchX = point.x
    val touchY = point.y

    val centerDist = Math.hypot(
        (touchX - ruler.center.x).toDouble(),
        (touchY - ruler.center.y).toDouble()
    )
    if (centerDist < 38.0) return true

    val dx = kotlin.math.cos(ruler.angleRad) * (ruler.length / 2f)
    val dy = kotlin.math.sin(ruler.angleRad) * (ruler.length / 2f)
    val rightX = ruler.center.x + dx
    val rightY = ruler.center.y + dy
    val rightDist = Math.hypot(
        (touchX - rightX).toDouble(),
        (touchY - rightY).toDouble()
    )
    if (rightDist < 34.0) return true

    val leftX = ruler.center.x - dx
    val leftY = ruler.center.y - dy
    return Math.hypot((touchX - leftX).toDouble(), (touchY - leftY).toDouble()) < 30.0
}

private fun ToolType.isStrokeTool(): Boolean = when (this) {
    ToolType.PEN,
    ToolType.PENCIL,
    ToolType.INK_PEN,
    ToolType.FOUNTAIN_PEN,
    ToolType.MARKER,
    ToolType.AIRBRUSH,
    ToolType.CRAYON,
    ToolType.WATERCOLOR_BRUSH,
    ToolType.LASER -> true
    else -> false
}

private data class CanvasElementHit(val id: String, val type: String)

private fun PageEntity.findTopElementAt(
    point: Offset,
    radius: Float,
    candidateIds: Set<String>? = null
): CanvasElementHit? {
    fun allowed(id: String): Boolean = candidateIds == null || id in candidateIds
    visibleLayersBottomUp().asReversed().forEach { layer ->
        layer.codeBlocks.asReversed().firstOrNull { block ->
            allowed(block.id) && point.x in (block.x - radius)..(block.x + block.width + radius) &&
                point.y in (block.y - radius)..(block.y + block.height + radius)
        }?.let { return CanvasElementHit(it.id, "CODE") }
        layer.strokes.asReversed().firstOrNull { stroke ->
            allowed(stroke.id) && DrawingEngine.isPointInStroke(point, stroke, radius)
        }?.let { return CanvasElementHit(it.id, "STROKE") }
        layer.textBlocks.asReversed().firstOrNull { text ->
            allowed(text.id) && point.x in (text.x - radius)..(text.x + text.width + radius) &&
                point.y in (text.y - radius)..(text.y + text.height + radius)
        }?.let { return CanvasElementHit(it.id, "TEXT") }
        layer.charts.asReversed().firstOrNull { chart ->
            allowed(chart.id) && point.x in (chart.x - radius)..(chart.x + chart.width + radius) &&
                point.y in (chart.y - radius)..(chart.y + chart.height + radius)
        }?.let { return CanvasElementHit(it.id, "CHART") }
        layer.images.asReversed().firstOrNull { image ->
            allowed(image.id) && point.x in (image.x - radius)..(image.x + image.width + radius) &&
                point.y in (image.y - radius)..(image.y + image.height + radius)
        }?.let { return CanvasElementHit(it.id, "IMAGE") }
        layer.shapes.asReversed().firstOrNull { shape ->
            allowed(shape.id) && point.x in (shape.x - radius)..(shape.x + shape.width + radius) &&
                point.y in (shape.y - radius)..(shape.y + shape.height + radius)
        }?.let { return CanvasElementHit(it.id, "SHAPE") }
    }
    return null
}

private fun PageEntity.sampleColorAt(point: Offset, radius: Float): HslaColor? {
    visibleLayersBottomUp().asReversed().forEach { layer ->
        layer.strokes.asReversed().firstOrNull {
            DrawingEngine.isPointInStroke(point, it, radius)
        }?.let { return it.colorHsla }
        layer.textBlocks.asReversed().firstOrNull {
            point.x in it.x..(it.x + it.width) && point.y in it.y..(it.y + it.height)
        }?.let { return HslaColor.fromArgb(it.color) }
        layer.shapes.asReversed().firstOrNull {
            point.x in it.x..(it.x + it.width) && point.y in it.y..(it.y + it.height)
        }?.let { shape ->
            return HslaColor.fromArgb(if (shape.fillColor != 0) shape.fillColor else shape.strokeColor)
        }
    }
    return null
}

private fun PageEntity.findShape(id: String): ShapeEntity? =
    getEffectiveLayers().flatMap { it.shapes }.find { it.id == id }

private fun PageEntity.findImage(id: String): ImageElementEntity? =
    getEffectiveLayers().flatMap { it.images }.find { it.id == id }

private fun PageEntity.findText(id: String): TextBlockEntity? =
    getEffectiveLayers().flatMap { it.textBlocks }.find { it.id == id }

private fun PageEntity.findChart(id: String): ChartElementEntity? =
    getEffectiveLayers().flatMap { it.charts }.find { it.id == id }

private fun PageEntity.findCodeBlock(id: String): com.example.data.models.CodeBlockEntity? =
    getEffectiveLayers().flatMap { it.codeBlocks }.find { it.id == id }

private fun PageEntity.selectionBounds(ids: Set<String>): Rect? {
    if (ids.isEmpty()) return null
    val bounds = getEffectiveLayers().flatMap { layer ->
        buildList {
            layer.strokes.filter { it.id in ids }.forEach { strokeBounds(it)?.let(::add) }
            layer.shapes.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
            layer.images.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
            layer.textBlocks.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
            layer.charts.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
            layer.codeBlocks.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
        }
    }
    if (bounds.isEmpty()) return null
    return Rect(
        left = bounds.minOf { it.left },
        top = bounds.minOf { it.top },
        right = bounds.maxOf { it.right },
        bottom = bounds.maxOf { it.bottom }
    )
}
