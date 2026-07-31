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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.drawing.DrawingEngine
import com.example.core.drawing.RulerState
import com.example.data.models.BackgroundPattern
import com.example.data.models.CanvasEntity
import com.example.data.models.ChartElementEntity
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
    rulerState: RulerState,
    zoomScale: Float,
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
    onLassoComplete: (List<Offset>) -> Unit = {},
    onBeginMoveSelectedGroup: () -> Unit = {},
    onMoveSelectedGroup: (Float, Float) -> Unit = { _, _ -> },
    onEndMoveSelectedGroup: () -> Unit = {},
    onResizeAndMoveElement: (String, String, Float, Float, Float, Float, String) -> Unit = { _, _, _, _, _, _, _ -> },
    getCachedBitmap: (String) -> android.graphics.Bitmap? = { null },
    onPreloadImage: (String) -> Unit = {},
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

    var rulerGuideEdge by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var cursorPos by remember { mutableStateOf<Offset?>(null) }
    var previewPulse by remember { mutableStateOf(false) }

    var pendingCommittedStroke by remember { mutableStateOf<StrokeEntity?>(null) }

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
    val selectedGroupBounds = remember(pageEntity, selectedElementIds) {
        pageEntity?.selectionBounds(selectedElementIds)
    }
    val latestPageEntity by rememberUpdatedState(pageEntity)
    val latestSelectedGroupBounds by rememberUpdatedState(selectedGroupBounds)
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
            .pointerInput(drawWithFingers) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            if (zoom != 1f || pan != Offset.Zero) {
                                activeStrokePoints.clear()
                                activeEraserPoints.clear()
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
            .pointerInput(
                currentTool,
                eraserMode,
                strokeWidth,
                strokeOpacity,
                currentColor,
                currentScale,
                selectedElementIds,
                rulerState,
                panOffset
            ) {
                awaitPointerEventScope {
                    while (true) {
                        val pointerEvent = awaitPointerEvent()
                        val change = pointerEvent.changes.firstOrNull() ?: continue
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
                if (screenPoint.y <= topToolbarHeightPx && selectedElementId == null) {
                    continue
                }

                // ═══════════════════════════════════════════════════════
                // 0b. Перевірка касання в області лінійки — пропускаємо до RulerOverlayComponent
                // ═══════════════════════════════════════════════════════
                if (rulerState.isVisible && isTouchInsideRuler(screenPoint, rulerState)) {
                    continue
                }

                // ═══════════════════════════════════════════════════════
                // 1. Мультитач (2+ пальці) — скасовуємо штрих, даємо зуму працювати
                // ═══════════════════════════════════════════════════════
                if (pointerEvent.changes.count { it.pressed } > 1) {
                    activeStrokePoints.clear()
                    activeEraserPoints.clear()
                    if (isObjectEraserGesture) onEndEraserGesture()
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
                        transformPreview = null
                        if (currentTool != ToolType.SELECTOR && currentTool != ToolType.ERASER) {
                            if (rulerState.isVisible) {
                                val g = rulerState.nearestEdge(rawPoint, guideZone = (40f / safeScale + rulerState.width / 2f))
                                if (g != null) {
                                    rulerGuideEdge = g.second
                                    rawPoint = g.first
                                } else {
                                    rulerGuideEdge = null
                                    rulerState.snapPointIfClose(rawPoint, thresholdDp = 16f, scale = safeScale)?.let { snapped -> rawPoint = snapped }
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
                            isDraggingGroup = selectedElementIds.isNotEmpty() && latestSelectedGroupBounds?.contains(rawPoint) == true

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
                                    "TEXT" -> inputPage.findText(selId)?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
                                    "CHART" -> inputPage.findChart(selId)?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
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
                                                    elementOriginalRotation = 0f
                                                }
                                            }
                                            layer.charts.reversed().forEach { chart ->
                                                if (selectedElementId == null && rawPoint.x >= chart.x - margin && rawPoint.x <= chart.x + chart.width + margin &&
                                                    rawPoint.y >= chart.y - margin && rawPoint.y <= chart.y + chart.height + margin) {
                                                    selectedElementId = chart.id
                                                    selectedElementType = "CHART"
                                                    elementOriginalPos = Offset(chart.x, chart.y)
                                                    elementOriginalSize = Offset(chart.width, chart.height)
                                                    elementOriginalRotation = 0f
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            }
                        } else if (currentTool == ToolType.ERASER) {
                            cursorPos = rawPoint
                            eraserTouchPos = rawPoint
                            if (eraserMode == EraserMode.PIXEL) {
                                activeEraserPoints.add(StrokePoint(x = rawPoint.x, y = rawPoint.y, pressure = pressure, tilt = tilt, timestampMs = System.currentTimeMillis()))
                            } else {
                                onBeginEraserGesture()
                                isObjectEraserGesture = true
                                onEraseAtPoint(rawPoint, strokeWidth * 2.5f)
                            }
                        } else {
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
                                                    anchor = "BR"
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
                                    val minDistance = (strokeWidth * 0.12f).coerceIn(0.5f, 2f)
                                    if (last == null || Offset(last.x - rawPoint.x, last.y - rawPoint.y).getDistance() >= minDistance) {
                                        activeEraserPoints.add(
                                            StrokePoint(rawPoint.x, rawPoint.y, pressure, tilt, System.currentTimeMillis())
                                        )
                                    }
                                } else {
                                    onEraseAtPoint(rawPoint, strokeWidth * 2.5f)
                                }
                            }
                            else -> {
                                rulerGuideEdge?.let { edge ->
                                    rawPoint = rulerState.projectOn(edge, rawPoint)
                                } ?: run {
                                    rulerState.snapPointIfClose(rawPoint, thresholdDp = 16f, scale = safeScale)?.let { snapped -> rawPoint = snapped }
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
                        if (currentTool != ToolType.ERASER && currentTool != ToolType.SELECTOR && activeStrokePoints.isNotEmpty()) {
                            val newStroke = StrokeEntity(
                                tool = currentTool,
                                colorHsla = currentColor.copy(alpha = strokeOpacity),
                                baseWidth = strokeWidth,
                                points = activeStrokePoints.toList()
                            )
                            pendingCommittedStroke = newStroke
                            onStrokeAdded(newStroke)
                        }
                        if (currentTool == ToolType.ERASER) {
                            if (eraserMode == EraserMode.PIXEL && activeEraserPoints.isNotEmpty()) {
                                onEraserMarkAdded(
                                    com.example.data.models.EraserMark(
                                        points = activeEraserPoints.toList(),
                                        width = strokeWidth * 5f
                                    )
                                )
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
                                preview.anchor
                            )
                        }
                        transformPreview = null
                        isObjectEraserGesture = false
                        activeStrokePoints.clear()
                        activeEraserPoints.clear()
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
                            ?.let { storedChart.copy(x = it.x, y = it.y, width = it.width, height = it.height) }
                            ?: storedChart
                        val cx = chart.x * currentScale + panOffset.x
                        val cy = chart.y * currentScale + panOffset.y
                        val cw = chart.width * currentScale
                        val ch = chart.height * currentScale

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

                        val xSpan = (chart.xMax - chart.xMin).let { if (it <= 0f) 20f else it }
                        val ySpan = (chart.yMax - chart.yMin).let { if (it <= 0f) 20f else it }
                        val stepX = if (chart.xStep > 0f) chart.xStep else 1f
                        val stepY = if (chart.yStep > 0f) chart.yStep else 1f
                        val xTicks = axisTickValues(chart.xMin, chart.xMax, stepX)
                        val yTicks = axisTickValues(chart.yMin, chart.yMax, stepY)

                        // Grid lines (vertical) based on xStep
                        xTicks.forEach { currWorldX ->
                            val relX = (currWorldX - chart.xMin) / xSpan
                            val lineScreenX = cx + relX * cw
                            if (lineScreenX in (cx - 1f)..(cx + cw + 1f)) {
                                drawLine(
                                    color = (if (isDarkBackground) Color(0x33FFFFFF) else Color(0x22000000)).copy(alpha = layerAlpha),
                                    start = Offset(lineScreenX, cy),
                                    end = Offset(lineScreenX, cy + ch),
                                    strokeWidth = (1f * currentScale).coerceAtLeast(1f)
                                )
                            }
                        }

                        // Grid lines (horizontal) based on yStep
                        yTicks.forEach { currWorldY ->
                            val relY = 1f - (currWorldY - chart.yMin) / ySpan
                            val lineScreenY = cy + relY * ch
                            if (lineScreenY in (cy - 1f)..(cy + ch + 1f)) {
                                drawLine(
                                    color = (if (isDarkBackground) Color(0x33FFFFFF) else Color(0x22000000)).copy(alpha = layerAlpha),
                                    start = Offset(cx, lineScreenY),
                                    end = Offset(cx + cw, lineScreenY),
                                    strokeWidth = (1f * currentScale).coerceAtLeast(1f)
                                )
                            }
                        }

                        // Axes (X and Y through origin 0,0)
                        val relZeroX = ((0f - chart.xMin) / xSpan).coerceIn(0f, 1f)
                        val relZeroY = (1f - (0f - chart.yMin) / ySpan).coerceIn(0f, 1f)
                        val axisXScreenY = cy + relZeroY * ch
                        val axisYScreenX = cx + relZeroX * cw

                        val axisStrokeWidth = (2.5f * currentScale).coerceAtLeast(2f)
                        val axisColor = if (isDarkBackground) Color(0xFFCBD5E1) else Color(0xFF475569)
                        drawLine(axisColor, Offset(cx, axisXScreenY), Offset(cx + cw, axisXScreenY), strokeWidth = axisStrokeWidth)
                        drawLine(axisColor, Offset(axisYScreenX, cy), Offset(axisYScreenX, cy + ch), strokeWidth = axisStrokeWidth)
                        val tickHalfLength = (4f * currentScale).coerceIn(3f, 10f)
                        xTicks.forEach { value ->
                            val tickX = cx + (value - chart.xMin) / xSpan * cw
                            drawLine(axisColor, Offset(tickX, axisXScreenY - tickHalfLength), Offset(tickX, axisXScreenY + tickHalfLength), strokeWidth = axisStrokeWidth * 0.65f)
                        }
                        yTicks.forEach { value ->
                            val tickY = cy + (1f - (value - chart.yMin) / ySpan) * ch
                            drawLine(axisColor, Offset(axisYScreenX - tickHalfLength, tickY), Offset(axisYScreenX + tickHalfLength, tickY), strokeWidth = axisStrokeWidth * 0.65f)
                        }

                        // Axis labels
                        if (chart.showAxisLabels && chart.axisLabelsVisible) {
                            val labelTextSize = with(density) { 12.sp.toPx() * currentScale }.coerceIn(24f, 60f)
                            drawIntoCanvas { canvas ->
                                val textPaint = android.text.TextPaint().apply {
                                    color = if (isDarkBackground) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                                    textSize = labelTextSize
                                    isAntiAlias = true
                                }
                                // X-axis labels
                                xTicks.forEach { wx ->
                                    val relX = (wx - chart.xMin) / xSpan
                                    val screenX = cx + relX * cw
                                    if (screenX in cx..cx + cw) {
                                        val labelText = if (wx == wx.toInt().toFloat()) wx.toInt().toString() else String.format(java.util.Locale.US, "%.1f", wx)
                                        canvas.nativeCanvas.drawText(labelText, screenX - 6f * currentScale, (axisXScreenY + 14f * currentScale).coerceAtMost(cy + ch - 4f), textPaint)
                                    }
                                }
                                // Y-axis labels
                                yTicks.forEach { wy ->
                                    if (wy != 0f) {
                                        val relY = 1f - (wy - chart.yMin) / ySpan
                                        val screenY = cy + relY * ch
                                        if (screenY in cy..cy + ch) {
                                            val labelText = if (wy == wy.toInt().toFloat()) wy.toInt().toString() else String.format(java.util.Locale.US, "%.1f", wy)
                                            canvas.nativeCanvas.drawText(labelText, (axisYScreenX + 4f * currentScale).coerceAtMost(cx + cw - 12f), screenY + 4f * currentScale, textPaint)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── 2d. TEXT BLOCKS (текстові блоки) ───
                    layer.textBlocks.forEach { storedTextBlock ->
                        val textBlock = transformPreview
                            ?.takeIf { it.id == storedTextBlock.id && it.type == "TEXT" }
                            ?.let { storedTextBlock.copy(x = it.x, y = it.y, width = it.width, height = it.height) }
                            ?: storedTextBlock
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
                            }
                            val maxWidth = (textBlock.width * currentScale).toInt().coerceAtLeast(100)
                            val staticLayout = android.text.StaticLayout.Builder
                                .obtain(textBlock.text, 0, textBlock.text.length, textPaint, maxWidth)
                                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
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

                    // ─── 2e. STROKES & ERASER MARKS (через saveLayer + PorterDuff CLEAR) ───
                    if (layer.strokes.isNotEmpty() || layer.eraserMarks.isNotEmpty() || (activeEraserPoints.isNotEmpty() && !layer.isLocked)) {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val rect = android.graphics.RectF(0f, 0f, size.width, size.height)
                            val saveCount = nativeCanvas.saveLayer(rect, null)

                            layer.strokes.forEach { stroke ->
                                val path = DrawingEngine.createSmoothPath(stroke.points, scale = currentScale, panX = panOffset.x, panY = panOffset.y)

                                val sw = DrawingEngine.strokeRenderWidth(stroke.tool, stroke.baseWidth, currentScale)
                                val strokeAlpha = DrawingEngine.strokeRenderAlpha(stroke.tool, stroke.colorHsla.alpha, layerAlpha)

                                val drawColor = stroke.colorHsla.copy(alpha = strokeAlpha).toColor()

                                if (stroke.tool == ToolType.LASER) {
                                    drawPath(
                                        path = path,
                                        color = drawColor.copy(alpha = 0.4f * layerAlpha),
                                        style = Stroke(width = sw * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = drawColor,
                                    style = Stroke(
                                        width = sw,
                                        cap = if (stroke.tool == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Render legacy masks and the current gesture with a real round CLEAR brush.
                            if (layer.eraserMarks.isNotEmpty() || (activeEraserPoints.isNotEmpty() && !layer.isLocked)) {
                                val clearPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                }

                                fun drawClearStroke(points: List<StrokePoint>, width: Float) {
                                    clearPaint.strokeWidth = width * currentScale
                                    if (points.size == 1) {
                                        clearPaint.style = android.graphics.Paint.Style.FILL
                                        val point = points.first()
                                        nativeCanvas.drawCircle(
                                            point.x * currentScale + panOffset.x,
                                            point.y * currentScale + panOffset.y,
                                            width * currentScale / 2f,
                                            clearPaint
                                        )
                                        clearPaint.style = android.graphics.Paint.Style.STROKE
                                    } else if (points.isNotEmpty()) {
                                        val path = DrawingEngine.createSmoothPath(points, scale = currentScale, panX = panOffset.x, panY = panOffset.y).asAndroidPath()
                                        nativeCanvas.drawPath(path, clearPaint)
                                    }
                                }

                                layer.eraserMarks.forEach { mark -> drawClearStroke(mark.points, mark.width) }
                                if (activeEraserPoints.isNotEmpty() && !layer.isLocked) {
                                    drawClearStroke(activeEraserPoints, strokeWidth * 5f)
                                }
                            }

                            nativeCanvas.restoreToCount(saveCount)
                        }
                    }
                }
            }

            // 3. Render Active Drawing Stroke (double-buffered with pendingCommittedStroke to eliminate 1-frame disappearance)
            val strokePointsToDraw = if (activeStrokePoints.isNotEmpty()) activeStrokePoints.toList() else pendingCommittedStroke?.points
            val activeTool = if (activeStrokePoints.isNotEmpty()) currentTool else (pendingCommittedStroke?.tool ?: currentTool)
            val activeBaseWidth = if (activeStrokePoints.isNotEmpty()) strokeWidth else (pendingCommittedStroke?.baseWidth ?: strokeWidth)
            val activeOpacity = if (activeStrokePoints.isNotEmpty()) strokeOpacity else (pendingCommittedStroke?.colorHsla?.alpha ?: strokeOpacity)
            val activeColor = if (activeStrokePoints.isNotEmpty()) currentColor else (pendingCommittedStroke?.colorHsla ?: currentColor)

            if (!strokePointsToDraw.isNullOrEmpty()) {
                val activePath = DrawingEngine.createSmoothPath(strokePointsToDraw, scale = currentScale, panX = panOffset.x, panY = panOffset.y)
                val activeWidth = DrawingEngine.strokeRenderWidth(activeTool, activeBaseWidth, currentScale)
                val activeAlpha = DrawingEngine.strokeRenderAlpha(activeTool, activeOpacity)

                drawPath(
                    path = activePath,
                    color = activeColor.copy(alpha = activeAlpha).toColor(),
                    style = Stroke(
                        width = activeWidth,
                        cap = if (activeTool == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 4. Precision Circle Eraser Preview Indicator
            eraserTouchPos?.let { pos ->
                val screenX = pos.x * currentScale + panOffset.x
                val screenY = pos.y * currentScale + panOffset.y
                val circleRadius = strokeWidth * currentScale * 2.5f

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
                    }
                    "CHART" -> pageEntity.findChart(selId)?.let {
                        elemRect = preview?.let { p -> Rect(p.x, p.y, p.x + p.width, p.y + p.height) }
                            ?: Rect(it.x, it.y, it.x + it.width, it.y + it.height)
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
                        color = Color.White.copy(alpha = 0.8f),
                        radius = strokeWidth * 2.5f * currentScale,
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
                            color = Color.White.copy(alpha = 0.85f),
                            radius = strokeWidth * 2.5f * currentScale,
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
                            color = Color.White.copy(alpha = 0.8f),
                            radius = (strokeWidth / 2f) * currentScale,
                            center = viewportCenter,
                            style = Stroke(1.5f)
                        )
                    }
                }
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
                        .fillMaxSize()
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
                                onResizeElement(selId, selType, newW, newH, "BR")
                            }) {
                                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Збільшити", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Resize smaller
                            IconButton(onClick = {
                                val newW = (elemSize!!.x * 0.8f).coerceAtLeast(60f)
                                val newH = (elemSize!!.y * 0.8f).coerceAtLeast(60f)
                                onResizeElement(selId, selType, newW, newH, "BR")
                            }) {
                                Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Зменшити", tint = MaterialTheme.colorScheme.primary)
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
                                    Icon(imageVector = Icons.Default.Opacity, contentDescription = "Прозорість", tint = MaterialTheme.colorScheme.primary)
                                }
                                Text("${(currentImgOpacity * 100).roundToInt()}%", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }

                            // Rotate button
                            IconButton(onClick = {
                                onRotateElement(selId, selType)
                            }) {
                                Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Повернути", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Delete button
                            IconButton(onClick = {
                                onDeleteElement(selId, selType)
                                selectedElementId = null
                                selectedElementType = null
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Видалити", tint = MaterialTheme.colorScheme.error)
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
    val anchor: String
)

private fun minimumElementSize(type: String): Pair<Float, Float> = when (type) {
    "SHAPE" -> 30f to 30f
    "IMAGE" -> 50f to 50f
    "TEXT" -> 60f to 30f
    "CHART" -> 100f to 100f
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

private fun isTouchInsideRuler(point: Offset, ruler: RulerState): Boolean {
    val touchX = point.x
    val touchY = point.y

    val centerDist = Math.hypot(
        (touchX - ruler.center.x).toDouble(),
        (touchY - ruler.center.y).toDouble()
    )
    if (centerDist < 150.0) return true

    val dx = kotlin.math.cos(ruler.angleRad) * (ruler.length / 2f)
    val dy = kotlin.math.sin(ruler.angleRad) * (ruler.length / 2f)
    val rightX = ruler.center.x + dx
    val rightY = ruler.center.y + dy
    val rightDist = Math.hypot(
        (touchX - rightX).toDouble(),
        (touchY - rightY).toDouble()
    )
    if (rightDist < 120.0) return true

    val panelLeft = ruler.center.x - 140f
    val panelRight = ruler.center.x + 140f
    val panelTop = ruler.center.y - 100f
    val panelBottom = ruler.center.y - 30f
    if (touchX in panelLeft..panelRight && touchY in panelTop..panelBottom) {
        return true
    }

    val cosA = kotlin.math.cos(ruler.angleRad)
    val sinA = kotlin.math.sin(ruler.angleRad)
    val relX = touchX - ruler.center.x
    val relY = touchY - ruler.center.y
    val localX = relX * cosA + relY * sinA
    val localY = -relX * sinA + relY * cosA
    return kotlin.math.abs(localX) <= ruler.length / 2 + 40f && kotlin.math.abs(localY) <= ruler.width / 2 + 30f
}

private fun PageEntity.findShape(id: String): ShapeEntity? =
    getEffectiveLayers().flatMap { it.shapes }.find { it.id == id }

private fun PageEntity.findImage(id: String): ImageElementEntity? =
    getEffectiveLayers().flatMap { it.images }.find { it.id == id }

private fun PageEntity.findText(id: String): TextBlockEntity? =
    getEffectiveLayers().flatMap { it.textBlocks }.find { it.id == id }

private fun PageEntity.findChart(id: String): ChartElementEntity? =
    getEffectiveLayers().flatMap { it.charts }.find { it.id == id }

private fun PageEntity.selectionBounds(ids: Set<String>): Rect? {
    if (ids.isEmpty()) return null
    val bounds = getEffectiveLayers().flatMap { layer ->
        buildList {
            layer.shapes.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
            layer.images.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
            layer.textBlocks.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
            layer.charts.filter { it.id in ids }.forEach { add(Rect(it.x, it.y, it.x + it.width, it.y + it.height)) }
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
