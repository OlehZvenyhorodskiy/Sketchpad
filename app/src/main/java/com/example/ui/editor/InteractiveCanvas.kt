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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.drawing.DrawingEngine
import com.example.core.drawing.RulerState
import com.example.data.models.BackgroundPattern
import com.example.data.models.CanvasEntity
import com.example.data.models.EraserMode
import com.example.data.models.HslaColor
import com.example.data.models.PageEntity
import com.example.data.models.PageSizePreset
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
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
    modifier: Modifier = Modifier
) {
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

    val bitmapCache = remember { mutableStateMapOf<String, android.graphics.Bitmap>() }

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
                // Palm Rejection Filter
                if (com.example.core.gesture.PalmRejectionFilter.shouldRejectEvent(motionEvent)) {
                    return@pointerInteropFilter true
                }

                if (motionEvent.pointerCount > 1) {
                    activeStrokePoints.clear()
                    eraserTouchPos = null
                    return@pointerInteropFilter false
                }

                val x = (motionEvent.x - panOffset.x) / currentScale
                val y = (motionEvent.y - panOffset.y) / currentScale
                var rawPoint = Offset(x, y)

                if (currentTool != ToolType.SELECTOR) {
                    rulerState.snapPointIfClose(rawPoint)?.let { snapped ->
                        rawPoint = snapped
                    }
                }

                val pressure = if (motionEvent.pressure > 0f) motionEvent.pressure else 0.5f
                val tilt = motionEvent.getAxisValue(MotionEvent.AXIS_TILT)

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        activeStrokePoints.clear()
                        if (currentTool == ToolType.SELECTOR) {
                            dragStartOffset = rawPoint
                            isResizingCorner = false

                            // Check corner resize touch for active selection
                            val selId = selectedElementId
                            val selType = selectedElementType
                            var cornerHit = false
                            if (selId != null && selType != null && pageEntity != null) {
                                var cornerRect: Rect? = null
                                when (selType) {
                                    "SHAPE" -> pageEntity.shapes.find { it.id == selId }?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
                                    "IMAGE" -> pageEntity.images.find { it.id == selId }?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
                                    "TEXT" -> pageEntity.textBlocks.find { it.id == selId }?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
                                    "CHART" -> pageEntity.charts.find { it.id == selId }?.let { cornerRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
                                }
                                cornerRect?.let { r ->
                                    val distToCorner = Math.hypot((rawPoint.x - r.right).toDouble(), (rawPoint.y - r.bottom).toDouble())
                                    if (distToCorner <= 48.0 / currentScale) {
                                        cornerHit = true
                                        isResizingCorner = true
                                        elementOriginalSize = Offset(r.width, r.height)
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
                                                if (selectedElementId == null && rawPoint.x >= shape.x - margin && rawPoint.x <= shape.x + shape.width + margin &&
                                                    rawPoint.y >= shape.y - margin && rawPoint.y <= shape.y + shape.height + margin) {
                                                    selectedElementId = shape.id
                                                    selectedElementType = "SHAPE"
                                                    elementOriginalPos = Offset(shape.x, shape.y)
                                                    elementOriginalSize = Offset(shape.width, shape.height)
                                                }
                                            }
                                            layer.images.reversed().forEach { img ->
                                                if (selectedElementId == null && rawPoint.x >= img.x - margin && rawPoint.x <= img.x + img.width + margin &&
                                                    rawPoint.y >= img.y - margin && rawPoint.y <= img.y + img.height + margin) {
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
                                        val newW = (elementOriginalSize.x + dx).coerceAtLeast(60f)
                                        val newH = (elementOriginalSize.y + dy).coerceAtLeast(60f)
                                        onResizeElement(id, type, newW, newH)
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
                                eraserTouchPos = rawPoint
                                onEraseAtPoint(rawPoint, strokeWidth * 2.5f)
                            }
                            else -> {
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

            // 2. Render Page Elements per Layer (bottom to top)
            pageEntity?.let { page ->
                page.visibleLayersBottomUp().forEach { layer ->
                    val layerAlpha = layer.opacity.coerceIn(0f, 1f)

                    // Images
                    layer.images.forEach { image ->
                        val pivotX = image.x * currentScale + panOffset.x + (image.width * currentScale) / 2f
                        val pivotY = image.y * currentScale + panOffset.y + (image.height * currentScale) / 2f
                        rotate(degrees = image.rotation, pivot = Offset(pivotX, pivotY)) {
                            drawIntoCanvas { canvas ->
                                try {
                                    val file = File(image.sourceUri)
                                    if (file.exists()) {
                                        val bitmap = bitmapCache.getOrPut(image.sourceUri) {
                                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                        }
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
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    // Shapes
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

                    // Charts
                    layer.charts.forEach { chart ->
                        val cx = chart.x * currentScale + panOffset.x
                        val cy = chart.y * currentScale + panOffset.y
                        val cw = chart.width * currentScale
                        val ch = chart.height * currentScale

                        val chartBgColor = if (isDarkBackground) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        val chartBorderColor = if (isDarkBackground) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        val chartGridColor = if (isDarkBackground) Color(0x3394A3B8) else Color(0x22475569)

                        drawRect(
                            color = chartBgColor.copy(alpha = chartBgColor.alpha * layerAlpha),
                            topLeft = Offset(cx, cy),
                            size = androidx.compose.ui.geometry.Size(cw, ch)
                        )
                        drawRect(
                            color = chartBorderColor.copy(alpha = chartBorderColor.alpha * layerAlpha),
                            topLeft = Offset(cx, cy),
                            size = androidx.compose.ui.geometry.Size(cw, ch),
                            style = Stroke(2f * currentScale)
                        )

                        val squareSize = 32f * currentScale
                        val cols = (cw / squareSize).toInt().coerceAtLeast(1)
                        val rows = (ch / squareSize).toInt().coerceAtLeast(1)

                        for (i in 1..cols) {
                            val xPos = cx + i * squareSize
                            if (xPos < cx + cw) {
                                drawLine(
                                    color = chartGridColor.copy(alpha = chartGridColor.alpha * layerAlpha),
                                    start = Offset(xPos, cy),
                                    end = Offset(xPos, cy + ch),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        for (j in 1..rows) {
                            val yPos = cy + j * squareSize
                            if (yPos < cy + ch) {
                                drawLine(
                                    color = chartGridColor.copy(alpha = chartGridColor.alpha * layerAlpha),
                                    start = Offset(cx, yPos),
                                    end = Offset(cx + cw, yPos),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        val midX = cx + cw / 2f
                        val midY = cy + ch / 2f

                        drawLine(
                            color = chartBorderColor.copy(alpha = chartBorderColor.alpha * layerAlpha),
                            start = Offset(cx + 6f, midY),
                            end = Offset(cx + cw - 6f, midY),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = chartBorderColor.copy(alpha = chartBorderColor.alpha * layerAlpha),
                            start = Offset(midX, cy + ch - 6f),
                            end = Offset(midX, cy + 6f),
                            strokeWidth = 2.5f
                        )
                    }

                    // Text Blocks
                    layer.textBlocks.forEach { textBlock ->
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = if (textBlock.color == 0xFF000000.toInt() && isDarkBackground) android.graphics.Color.WHITE else textBlock.color
                                textSize = textBlock.fontSize * currentScale * 1.5f
                                isAntiAlias = true
                                isFakeBoldText = textBlock.isBold
                            }
                            canvas.nativeCanvas.drawText(
                                textBlock.text,
                                textBlock.x * currentScale + panOffset.x,
                                (textBlock.y + textBlock.fontSize) * currentScale + panOffset.y,
                                paint
                            )
                        }
                    }

                    // Strokes
                    layer.strokes.forEach { stroke ->
                        val scaledPoints = stroke.points.map { p ->
                            StrokePoint(
                                x = p.x * currentScale + panOffset.x,
                                y = p.y * currentScale + panOffset.y,
                                pressure = p.pressure,
                                tilt = p.tilt
                            )
                        }
                        val path = DrawingEngine.createSmoothPath(scaledPoints)

                        val strokeWidth = when (stroke.tool) {
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

                        if (stroke.tool == ToolType.LASER) {
                            drawPath(
                                path = path,
                                color = drawColor.copy(alpha = 0.4f * layerAlpha),
                                style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        drawPath(
                            path = path,
                            color = drawColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = if (stroke.tool == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // 3. Render Active Drawing Stroke
            if (activeStrokePoints.isNotEmpty()) {
                val scaledPoints = activeStrokePoints.map { p ->
                    StrokePoint(
                        x = p.x * currentScale + panOffset.x,
                        y = p.y * currentScale + panOffset.y,
                        pressure = p.pressure,
                        tilt = p.tilt
                    )
                }
                val activePath = DrawingEngine.createSmoothPath(scaledPoints)

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

                when (selType) {
                    "SHAPE" -> pageEntity.shapes.find { it.id == selId }?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                    }
                    "IMAGE" -> pageEntity.images.find { it.id == selId }?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                    }
                    "TEXT" -> pageEntity.textBlocks.find { it.id == selId }?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                    }
                    "CHART" -> pageEntity.charts.find { it.id == selId }?.let {
                        elemRect = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
                    }
                }

                elemRect?.let { r ->
                    val left = r.left * currentScale + panOffset.x
                    val top = r.top * currentScale + panOffset.y
                    val right = r.right * currentScale + panOffset.x
                    val bottom = r.bottom * currentScale + panOffset.y

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

        // Floating Action Toolbar Overlay for Selected Element
        val selId = selectedElementId
        val selType = selectedElementType
        val density = LocalDensity.current
        if (selId != null && selType != null && pageEntity != null) {
            var elemPos: Offset? = null
            var elemSize: Offset? = null
            var currentImgOpacity = 1.0f

            when (selType) {
                "SHAPE" -> pageEntity.shapes.find { it.id == selId }?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                }
                "IMAGE" -> pageEntity.images.find { it.id == selId }?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                    currentImgOpacity = it.opacity
                }
                "TEXT" -> pageEntity.textBlocks.find { it.id == selId }?.let {
                    elemPos = Offset(it.x, it.y)
                    elemSize = Offset(it.width, it.height)
                }
                "CHART" -> pageEntity.charts.find { it.id == selId }?.let {
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Opacity, contentDescription = "Прозорість", tint = MaterialTheme.colorScheme.primary)
                                        Text("${(currentImgOpacity * 100).roundToInt()}%", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                                    }
                                }
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
