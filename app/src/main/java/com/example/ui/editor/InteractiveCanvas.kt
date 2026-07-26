package com.example.ui.editor

import android.view.MotionEvent
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
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
    onEraseAtPoint: (Offset, Float) -> Unit,
    onTwoFingerTap: () -> Unit,
    onMoveShape: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveText: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveImage: (String, Float, Float) -> Unit = { _, _, _ -> },
    onMoveChart: (String, Float, Float) -> Unit = { _, _, _ -> },
    onDeleteElement: (String, String) -> Unit = { _, _ -> },
    onRotateElement: (String, String) -> Unit = { _, _ -> },
    onUpdateImageOpacity: (String, Float) -> Unit = { _, _ -> },
    onResizeElement: (String, String, Float, Float) -> Unit = { _, _, _, _ -> },
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
    var eraserTouchPos by remember { mutableStateOf<Offset?>(null) }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementType by remember { mutableStateOf<String?>(null) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var elementOriginalPos by remember { mutableStateOf(Offset.Zero) }
    var elementOriginalSize by remember { mutableStateOf(Offset.Zero) }
    var isResizingCorner by remember { mutableStateOf(false) }
    var resizingCorner by remember { mutableStateOf<String?>(null) }

    var rulerGuideEdge by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var cursorPos by remember { mutableStateOf<Offset?>(null) }
    var previewPulse by remember { mutableStateOf(false) }

    LaunchedEffect(strokeWidth, strokeOpacity, currentColor) {
        previewPulse = true
        kotlinx.coroutines.delay(600)
        previewPulse = false
    }

    // Preload image bitmaps off the UI thread via ViewModel LruCache
    val imageUris = remember(pageEntity) {
        pageEntity?.getEffectiveLayers()?.flatMap { it.images }?.map { it.sourceUri }?.distinct() ?: emptyList()
    }
    imageUris.forEach { uri ->
        if (getCachedBitmap(uri) == null) {
            LaunchedEffect(uri) {
                onPreloadImage(uri)
            }
        }
    }

    // Default to dark background (#121212) if not specified or white
    val bgColor = canvasEntity?.backgroundColor?.let {
        if (it == 0xFFFFFFFF.toInt()) Color(0xFF121212) else Color(it)
    } ?: Color(0xFF121212)

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
                                val oldScale = currentScale
                                val newScale = (oldScale * zoom).coerceIn(0.5f, 8.0f)
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
            .pointerInteropFilter { motionEvent ->
                // ═══════════════════════════════════════════════════════
                // 0a. Перевірка касання у верхній зоні тулбара (TopFloatingToolbar) — не знімаємо виділення
                // ═══════════════════════════════════════════════════════
                val topToolbarHeightPx = with(density) { 110.dp.toPx() }
                if (motionEvent.y <= topToolbarHeightPx) {
                    return@pointerInteropFilter false
                }

                // Захищаємо плаваючий тулбар виділення
                // Він з'являється лише коли selectedElementId != null
                val floatingToolbarBottomPx = topToolbarHeightPx + with(density) { 70.dp.toPx() }
                if (selectedElementId != null && motionEvent.y <= floatingToolbarBottomPx) {
                    return@pointerInteropFilter false
                }

                // ═══════════════════════════════════════════════════════
                // 0b. Перевірка касання в області лінійки — пропускаємо до RulerOverlayComponent
                // ═══════════════════════════════════════════════════════
                if (rulerState.isVisible && isTouchInsideRuler(motionEvent, rulerState)) {
                    return@pointerInteropFilter false
                }

                // ═══════════════════════════════════════════════════════
                // 1. Мультитач (2+ пальці) — скасовуємо штрих, даємо зуму працювати
                // ═══════════════════════════════════════════════════════
                if (motionEvent.pointerCount > 1) {
                    activeStrokePoints.clear()
                    eraserTouchPos = null
                    return@pointerInteropFilter false
                }

                // ═══════════════════════════════════════════════════════
                // 2. SMART PALM REJECTION (для неофіційних/пасивних стилусів Xiaomi та пальця)
                // ═══════════════════════════════════════════════════════
                if (com.example.core.gesture.PalmRejectionFilter.shouldRejectEvent(motionEvent)) {
                    if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                        activeStrokePoints.clear()
                        eraserTouchPos = null
                    }
                    return@pointerInteropFilter true  // Відхиляємо тільки велику долоню (area > 900px² або major > 55px)
                }

                val safeScale = currentScale.coerceIn(0.1f, 10.0f)
                val x = (motionEvent.x - panOffset.x) / safeScale
                val y = (motionEvent.y - panOffset.y) / safeScale
                var rawPoint = Offset(x, y)

                val pressure = if (motionEvent.pressure > 0f) motionEvent.pressure else 0.5f
                val tilt = motionEvent.getAxisValue(MotionEvent.AXIS_TILT)

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        activeStrokePoints.clear()
                        if (currentTool != ToolType.SELECTOR && currentTool != ToolType.ERASER) {
                            if (rulerState.isVisible) {
                                val g = rulerState.nearestEdge(rawPoint, guideZone = (40f / safeScale + rulerState.width / 2f))
                                if (g != null) {
                                    rulerGuideEdge = g.second
                                    rawPoint = g.first
                                } else {
                                    rulerGuideEdge = null
                                    rulerState.snapPointIfClose(rawPoint)?.let { snapped -> rawPoint = snapped }
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

                            // Check corner resize touch for active selection (ALL 4 CORNERS)
                            val selId = selectedElementId
                            val selType = selectedElementType
                            var cornerHit = false
                            if (selId != null && selType != null && pageEntity != null) {
                                var cornerRect: Rect? = null
                                var elemRotation = 0f
                                when (selType) {
                                    "SHAPE" -> pageEntity.findShape(selId)?.let {
                                        cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                                        elemRotation = it.rotation
                                    }
                                    "IMAGE" -> pageEntity.findImage(selId)?.let {
                                        cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                                        elemRotation = it.rotation
                                    }
                                    "TEXT" -> pageEntity.findText(selId)?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
                                    "CHART" -> pageEntity.findChart(selId)?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
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
                                    }
                                }
                            }

                            if (!cornerHit) {
                                selectedElementId = null
                                selectedElementType = null

                                pageEntity?.let { page ->
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
                                                }
                                            }
                                            layer.textBlocks.reversed().forEach { text ->
                                                if (selectedElementId == null && rawPoint.x >= text.x - margin && rawPoint.x <= text.x + text.width + margin &&
                                                    rawPoint.y >= text.y - margin && rawPoint.y <= text.y + text.height + margin) {
                                                    selectedElementId = text.id
                                                    selectedElementType = "TEXT"
                                                    elementOriginalPos = Offset(text.x, text.y)
                                                    elementOriginalSize = Offset(text.width, text.height)
                                                }
                                            }
                                            layer.charts.reversed().forEach { chart ->
                                                if (selectedElementId == null && rawPoint.x >= chart.x - margin && rawPoint.x <= chart.x + chart.width + margin &&
                                                    rawPoint.y >= chart.y - margin && rawPoint.y <= chart.y + chart.height + margin) {
                                                    selectedElementId = chart.id
                                                    selectedElementType = "CHART"
                                                    elementOriginalPos = Offset(chart.x, chart.y)
                                                    elementOriginalSize = Offset(chart.width, chart.height)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (currentTool == ToolType.ERASER) {
                            eraserTouchPos = rawPoint
                            onEraseAtPoint(rawPoint, strokeWidth * 2.5f)
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
                    MotionEvent.ACTION_MOVE -> {
                        when (currentTool) {
                            ToolType.SELECTOR -> {
                                val id = selectedElementId
                                val type = selectedElementType
                                if (id != null && type != null) {
                                    val dx = rawPoint.x - dragStartOffset.x
                                    val dy = rawPoint.y - dragStartOffset.y
                                    if (isResizingCorner) {
                                        val origW = elementOriginalSize.x
                                        val origH = elementOriginalSize.y
                                        val origX = elementOriginalPos.x
                                        val origY = elementOriginalPos.y

                                        var newW = origW
                                        var newH = origH
                                        var newX = origX
                                        var newY = origY

                                        when (resizingCorner) {
                                            "BR" -> {
                                                newW = (origW + dx).coerceAtLeast(60f)
                                                newH = (origH + dy).coerceAtLeast(60f)
                                            }
                                            "BL" -> {
                                                newW = (origW - dx).coerceAtLeast(60f)
                                                newH = (origH + dy).coerceAtLeast(60f)
                                                newX = origX + (origW - newW)
                                            }
                                            "TR" -> {
                                                newW = (origW + dx).coerceAtLeast(60f)
                                                newH = (origH - dy).coerceAtLeast(60f)
                                                newY = origY + (origH - newH)
                                            }
                                            "TL" -> {
                                                newW = (origW - dx).coerceAtLeast(60f)
                                                newH = (origH - dy).coerceAtLeast(60f)
                                                newX = origX + (origW - newW)
                                                newY = origY + (origH - newH)
                                            }
                                        }

                                        onResizeElement(id, type, newW, newH)
                                        if (newX != origX || newY != origY) {
                                            when (type) {
                                                "SHAPE" -> onMoveShape(id, newX, newY)
                                                "IMAGE" -> onMoveImage(id, newX, newY)
                                                "TEXT" -> onMoveText(id, newX, newY)
                                                "CHART" -> onMoveChart(id, newX, newY)
                                            }
                                        }
                                    } else {
                                        val newX = elementOriginalPos.x + dx
                                        val newY = elementOriginalPos.y + dy
                                        when (type) {
                                            "SHAPE" -> onMoveShape(id, newX, newY)
                                            "IMAGE" -> onMoveImage(id, newX, newY)
                                            "TEXT" -> onMoveText(id, newX, newY)
                                            "CHART" -> onMoveChart(id, newX, newY)
                                        }
                                    }
                                }
                            }
                            ToolType.ERASER -> {
                                cursorPos = rawPoint
                                eraserTouchPos = rawPoint
                                onEraseAtPoint(rawPoint, strokeWidth * 2.5f)
                            }
                            else -> {
                                rulerGuideEdge?.let { edge ->
                                    rawPoint = rulerState.projectOn(edge, rawPoint)
                                } ?: run {
                                    rulerState.snapPointIfClose(rawPoint)?.let { snapped -> rawPoint = snapped }
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
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (currentTool != ToolType.ERASER && currentTool != ToolType.SELECTOR && activeStrokePoints.isNotEmpty()) {
                            val newStroke = StrokeEntity(
                                tool = currentTool,
                                colorHsla = currentColor.copy(alpha = strokeOpacity),
                                baseWidth = strokeWidth,
                                points = activeStrokePoints.toList()
                            )
                            onStrokeAdded(newStroke)
                        }
                        activeStrokePoints.clear()
                        eraserTouchPos = null
                        resizingCorner = null
                        rulerGuideEdge = null
                        cursorPos = null
                    }
                }
                true
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
                        val pivotX = image.x * currentScale + panOffset.x + (image.width * currentScale) / 2f
                        val pivotY = image.y * currentScale + panOffset.y + (image.height * currentScale) / 2f

                        rotate(degrees = image.rotation, pivot = Offset(pivotX, pivotY)) {
                            drawIntoCanvas { canvas ->
                                try {
                                    val bitmap = getCachedBitmap(image.sourceUri)
                                    if (bitmap != null) {
                                        val paint = android.graphics.Paint().apply {
                                            alpha = (image.opacity.coerceIn(0.1f, 1.0f) * layerAlpha * 255).toInt()
                                            isAntiAlias = true
                                            isFilterBitmap = true
                                        }
                                        val dstRect = android.graphics.RectF(
                                            image.x * currentScale + panOffset.x,
                                            image.y * currentScale + panOffset.y,
                                            (image.x + image.width) * currentScale + panOffset.x,
                                            (image.y + image.height) * currentScale + panOffset.y
                                        )
                                        canvas.nativeCanvas.drawBitmap(bitmap, null, dstRect, paint)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("InteractiveCanvas", "Error rendering image bitmap", e)
                                }
                            }
                            if (getCachedBitmap(image.sourceUri) == null) {
                                drawRect(
                                    color = Color(0x4438BDF8),
                                    topLeft = Offset(
                                        image.x * currentScale + panOffset.x,
                                        image.y * currentScale + panOffset.y
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        image.width * currentScale,
                                        image.height * currentScale
                                    ),
                                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
                                )
                            }
                        }
                    }

                    // ─── 2b. SHAPES (фігури) ───
                    layer.shapes.forEach { shape ->
                        val pivotX = shape.x * currentScale + panOffset.x + (shape.width * currentScale) / 2f
                        val pivotY = shape.y * currentScale + panOffset.y + (shape.height * currentScale) / 2f

                        rotate(degrees = shape.rotation, pivot = Offset(pivotX, pivotY)) {
                            val path = DrawingEngine.createShapePath(
                                shape.shapeType,
                                Rect(
                                    shape.x * currentScale + panOffset.x,
                                    shape.y * currentScale + panOffset.y,
                                    (shape.x + shape.width) * currentScale + panOffset.x,
                                    (shape.y + shape.height) * currentScale + panOffset.y
                                )
                            )
                            val fillColor = Color(shape.fillColor)
                            val strokeColor = Color(shape.strokeColor)
                            drawPath(path, fillColor.copy(alpha = fillColor.alpha * layerAlpha))
                            drawPath(path, strokeColor.copy(alpha = strokeColor.alpha * layerAlpha), style = Stroke(shape.strokeWidth * currentScale))
                        }
                    }

                    // ─── 2c. CHARTS (графіки / координатна сітка) ───
                    layer.charts.forEach { chart ->
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

                        // Grid lines (vertical) based on xStep
                        var currWorldX = Math.ceil((chart.xMin / stepX).toDouble()).toFloat() * stepX
                        while (currWorldX <= chart.xMax) {
                            val relX = (currWorldX - chart.xMin) / xSpan
                            val lineScreenX = cx + relX * cw
                            if (lineScreenX in cx..cx + cw) {
                                drawLine(
                                    color = (if (isDarkBackground) Color(0x33FFFFFF) else Color(0x22000000)).copy(alpha = layerAlpha),
                                    start = Offset(lineScreenX, cy),
                                    end = Offset(lineScreenX, cy + ch),
                                    strokeWidth = 0.8f
                                )
                            }
                            currWorldX += stepX
                        }

                        // Grid lines (horizontal) based on yStep
                        var currWorldY = Math.ceil((chart.yMin / stepY).toDouble()).toFloat() * stepY
                        while (currWorldY <= chart.yMax) {
                            val relY = 1f - (currWorldY - chart.yMin) / ySpan
                            val lineScreenY = cy + relY * ch
                            if (lineScreenY in cy..cy + ch) {
                                drawLine(
                                    color = (if (isDarkBackground) Color(0x33FFFFFF) else Color(0x22000000)).copy(alpha = layerAlpha),
                                    start = Offset(cx, lineScreenY),
                                    end = Offset(cx + cw, lineScreenY),
                                    strokeWidth = 0.8f
                                )
                            }
                            currWorldY += stepY
                        }

                        // Axes (X and Y through origin 0,0)
                        val relZeroX = ((0f - chart.xMin) / xSpan).coerceIn(0f, 1f)
                        val relZeroY = (1f - (0f - chart.yMin) / ySpan).coerceIn(0f, 1f)
                        val axisXScreenY = cy + relZeroY * ch
                        val axisYScreenX = cx + relZeroX * cw

                        val axisStrokeWidth = 2.5f * currentScale
                        val axisColor = if (isDarkBackground) Color(0xFFCBD5E1) else Color(0xFF475569)
                        drawLine(axisColor, Offset(cx, axisXScreenY), Offset(cx + cw, axisXScreenY), strokeWidth = axisStrokeWidth)
                        drawLine(axisColor, Offset(axisYScreenX, cy), Offset(axisYScreenX, cy + ch), strokeWidth = axisStrokeWidth)

                        // Axis labels
                        if (chart.axisLabelsVisible) {
                            drawIntoCanvas { canvas ->
                                val textPaint = android.text.TextPaint().apply {
                                    color = if (isDarkBackground) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                                    textSize = (10f * currentScale).coerceIn(8f, 16f)
                                    isAntiAlias = true
                                }
                                // X-axis labels
                                var wx = Math.ceil((chart.xMin / stepX).toDouble()).toFloat() * stepX
                                while (wx <= chart.xMax) {
                                    val relX = (wx - chart.xMin) / xSpan
                                    val screenX = cx + relX * cw
                                    if (screenX in cx..cx + cw) {
                                        val labelText = if (wx == wx.toInt().toFloat()) wx.toInt().toString() else String.format(java.util.Locale.US, "%.1f", wx)
                                        canvas.nativeCanvas.drawText(labelText, screenX - 6f * currentScale, (axisXScreenY + 14f * currentScale).coerceAtMost(cy + ch - 4f), textPaint)
                                    }
                                    wx += stepX
                                }
                                // Y-axis labels
                                var wy = Math.ceil((chart.yMin / stepY).toDouble()).toFloat() * stepY
                                while (wy <= chart.yMax) {
                                    if (wy != 0f) {
                                        val relY = 1f - (wy - chart.yMin) / ySpan
                                        val screenY = cy + relY * ch
                                        if (screenY in cy..cy + ch) {
                                            val labelText = if (wy == wy.toInt().toFloat()) wy.toInt().toString() else String.format(java.util.Locale.US, "%.1f", wy)
                                            canvas.nativeCanvas.drawText(labelText, (axisYScreenX + 4f * currentScale).coerceAtMost(cx + cw - 12f), screenY + 4f * currentScale, textPaint)
                                        }
                                    }
                                    wy += stepY
                                }
                            }
                        }
                    }

                    // ─── 2d. TEXT BLOCKS (текстові блоки) ───
                    layer.textBlocks.forEach { textBlock ->
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

                    // ─── 2e. STROKES (штрихи / малюнки) ───
                    layer.strokes.forEach { stroke ->
                        val path = DrawingEngine.createSmoothPath(stroke.points, scale = currentScale, panX = panOffset.x, panY = panOffset.y)

                        val sw = when (stroke.tool) {
                            ToolType.PENCIL -> stroke.baseWidth * currentScale * 0.9f
                            ToolType.FOUNTAIN_PEN -> stroke.baseWidth * currentScale * 1.5f
                            ToolType.MARKER -> stroke.baseWidth * currentScale * 3.5f
                            ToolType.INK_PEN -> stroke.baseWidth * currentScale * 1.2f
                            ToolType.LASER -> stroke.baseWidth * currentScale * 2.0f
                            else -> stroke.baseWidth * currentScale
                        }

                        val strokeAlpha = when (stroke.tool) {
                            ToolType.MARKER -> 0.38f * layerAlpha
                            ToolType.PENCIL -> stroke.colorHsla.alpha * 0.85f * layerAlpha
                            else -> stroke.colorHsla.alpha * layerAlpha
                        }

                        val drawColor = stroke.colorHsla.copy(alpha = strokeAlpha).toColor()

                        // Glow for laser
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
                }
            }

            // 3. Render Active Drawing Stroke
            if (activeStrokePoints.isNotEmpty()) {
                val activePath = DrawingEngine.createSmoothPath(activeStrokePoints, scale = currentScale, panX = panOffset.x, panY = panOffset.y)

                val activeWidth = when (currentTool) {
                    ToolType.PENCIL -> strokeWidth * currentScale * 0.9f
                    ToolType.FOUNTAIN_PEN -> strokeWidth * currentScale * 1.5f
                    ToolType.MARKER -> strokeWidth * currentScale * 3.5f
                    ToolType.INK_PEN -> strokeWidth * currentScale * 1.2f
                    ToolType.LASER -> strokeWidth * currentScale * 2.0f
                    else -> strokeWidth * currentScale
                }

                val activeAlpha = when (currentTool) {
                    ToolType.MARKER -> 0.38f
                    ToolType.PENCIL -> strokeOpacity * 0.85f
                    else -> strokeOpacity
                }

                drawPath(
                    path = activePath,
                    color = currentColor.copy(alpha = activeAlpha).toColor(),
                    style = Stroke(
                        width = activeWidth,
                        cap = if (currentTool == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
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
            if (selId != null && selType != null && pageEntity != null) {
                var elemRect: Rect? = null
                var elemRotation = 0f

                when (selType) {
                    "SHAPE" -> pageEntity.findShape(selId)?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                        elemRotation = it.rotation
                    }
                    "IMAGE" -> pageEntity.findImage(selId)?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                        elemRotation = it.rotation
                    }
                    "TEXT" -> pageEntity.findText(selId)?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                    }
                    "CHART" -> pageEntity.findChart(selId)?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
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

            // 6. Brush Cursor & Slider Change Preview Circle Overlay
            cursorPos?.let { cp ->
                val screen = Offset(cp.x * currentScale + panOffset.x, cp.y * currentScale + panOffset.y)
                when (currentTool) {
                    ToolType.ERASER -> drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = (strokeWidth * 2.5f / 2f) * currentScale,
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
                    drawCircle(
                        color = currentColor.copy(alpha = (strokeOpacity * 0.6f).coerceAtLeast(0.3f)).toColor(),
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
                                onResizeElement(selId, selType, newW, newH)
                            }) {
                                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Збільшити", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Resize smaller
                            IconButton(onClick = {
                                val newW = (elemSize!!.x * 0.8f).coerceAtLeast(60f)
                                val newH = (elemSize!!.y * 0.8f).coerceAtLeast(60f)
                                onResizeElement(selId, selType, newW, newH)
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

private fun isTouchInsideRuler(event: MotionEvent, ruler: RulerState): Boolean {
    val touchX = event.x
    val touchY = event.y

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
