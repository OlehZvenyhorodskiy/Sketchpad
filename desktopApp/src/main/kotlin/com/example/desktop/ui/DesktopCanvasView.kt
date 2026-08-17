package com.example.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.example.desktop.DesktopViewModel
import com.example.desktop.input.WindowsInkHandler
import com.example.shared.core.DrawingMath
import com.example.shared.model.*
import java.util.UUID

@Composable
fun DesktopCanvasView(
    viewModel: DesktopViewModel,
    modifier: Modifier = Modifier
) {
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()
    val brushOpacity by viewModel.brushOpacity.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val symmetryMode by viewModel.symmetryMode.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val currentPage = pages.getOrElse(currentPageIndex) { pages.first() }

    // Active in-progress stroke
    var activeStrokePoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var activeStrokeId by remember { mutableStateOf(UUID.randomUUID().toString()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(currentTool, brushSize, brushOpacity, currentColor, zoomScale, panOffset) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeStrokeId = UUID.randomUUID().toString()
                        val worldX = (offset.x - panOffset.first) / zoomScale
                        val worldY = (offset.y - panOffset.second) / zoomScale
                        activeStrokePoints = listOf(StrokePoint(worldX, worldY, pressure = 0.5f))
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val sample = WindowsInkHandler.processPointerChange(change)
                        val worldX = (sample.x - panOffset.first) / zoomScale
                        val worldY = (sample.y - panOffset.second) / zoomScale
                        val newPoint = StrokePoint(worldX, worldY, pressure = sample.pressure)

                        activeStrokePoints = activeStrokePoints + newPoint

                        if (currentTool == ToolType.ERASER) {
                            // Erase on the fly
                            val activeLayer = currentPage.getActiveLayer()
                            val erased = activeLayer.strokes.flatMap { s ->
                                DrawingMath.eraseStrokeAlongPath(s, listOf(newPoint), brushSize)
                            }
                            viewModel.executeCommand(
                                EraseStrokesCommand(activeLayer.id, activeLayer.strokes, erased)
                            )
                        }
                    },
                    onDragEnd = {
                        if (activeStrokePoints.isNotEmpty() && currentTool != ToolType.ERASER) {
                            val stroke = StrokeEntity(
                                id = activeStrokeId,
                                tool = currentTool,
                                colorHsla = currentColor.copy(alpha = brushOpacity),
                                baseWidth = brushSize,
                                points = activeStrokePoints
                            )
                            viewModel.commitStroke(stroke)
                        }
                        activeStrokePoints = emptyList()
                    },
                    onDragCancel = {
                        activeStrokePoints = emptyList()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Background Grid Pattern
            drawBackgroundGrid(currentPage.backgroundPattern, currentPage.backgroundSpacing * zoomScale, panOffset)

            // 2. Visible Layers Bottom-Up
            currentPage.visibleLayersBottomUp().forEach { layer ->
                val layerAlpha = layer.opacity

                layer.strokes.forEach { stroke ->
                    drawSingleStroke(stroke, zoomScale, panOffset, layerAlpha)
                }

                layer.shapes.forEach { shape ->
                    drawSingleShape(shape, zoomScale, panOffset, layerAlpha)
                }
            }

            // 3. Active in-progress stroke
            if (activeStrokePoints.isNotEmpty() && currentTool != ToolType.ERASER) {
                val tempStroke = StrokeEntity(
                    id = activeStrokeId,
                    tool = currentTool,
                    colorHsla = currentColor.copy(alpha = brushOpacity),
                    baseWidth = brushSize,
                    points = activeStrokePoints
                )
                drawSingleStroke(tempStroke, zoomScale, panOffset, 1.0f)

                // Symmetry preview
                val symStrokes = DrawingMath.generateSymmetricStrokes(tempStroke, symmetryMode, canvasW / (2f * zoomScale), canvasH / (2f * zoomScale))
                symStrokes.drop(1).forEach { sym ->
                    drawSingleStroke(sym, zoomScale, panOffset, 0.7f)
                }
            }

            // 4. Symmetry guide lines
            if (symmetryMode != SymmetryMode.NONE) {
                val midX = canvasW / 2f
                val midY = canvasH / 2f
                val guideColor = Color(0x6600E5FF)
                val dashStroke = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))

                if (symmetryMode == SymmetryMode.VERTICAL || symmetryMode == SymmetryMode.QUAD) {
                    drawLine(guideColor, Offset(midX, 0f), Offset(midX, canvasH), strokeWidth = 1.5f)
                }
                if (symmetryMode == SymmetryMode.HORIZONTAL || symmetryMode == SymmetryMode.QUAD) {
                    drawLine(guideColor, Offset(0f, midY), Offset(canvasW, midY), strokeWidth = 1.5f)
                }
            }
        }
    }
}

private fun DrawScope.drawSingleStroke(stroke: StrokeEntity, scale: Float, pan: Pair<Float, Float>, layerAlpha: Float) {
    if (stroke.points.isEmpty()) return
    val sw = DrawingMath.strokeRenderWidth(stroke.tool, stroke.baseWidth, scale)
    val alpha = DrawingMath.strokeRenderAlpha(stroke.tool, stroke.colorHsla.alpha, layerAlpha)
    val color = Color(stroke.colorHsla.toArgbInt()).copy(alpha = alpha)

    if (stroke.points.size == 1) {
        val p = stroke.points[0]
        drawCircle(color, radius = sw / 2f, center = Offset(p.x * scale + pan.first, p.y * scale + pan.second))
        return
    }

    val path = Path()
    val smoothPoints = DrawingMath.interpolateCatmullRom(stroke.points)
    path.moveTo(smoothPoints[0].x * scale + pan.first, smoothPoints[0].y * scale + pan.second)
    for (i in 1 until smoothPoints.size) {
        path.lineTo(smoothPoints[i].x * scale + pan.first, smoothPoints[i].y * scale + pan.second)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = sw,
            cap = if (stroke.tool == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawSingleShape(shape: ShapeEntity, scale: Float, pan: Pair<Float, Float>, layerAlpha: Float) {
    val x = shape.x * scale + pan.first
    val y = shape.y * scale + pan.second
    val w = shape.width * scale
    val h = shape.height * scale
    val strokeColor = Color(shape.strokeColor).copy(alpha = layerAlpha)
    val fillColor = Color(shape.fillColor).copy(alpha = layerAlpha)

    when (shape.shapeType) {
        ShapeType.CIRCLE -> {
            if (shape.fillColor != 0) {
                drawOval(fillColor, topLeft = Offset(x, y), size = Size(w, h))
            }
            drawOval(strokeColor, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(shape.strokeWidth * scale))
        }
        else -> {
            if (shape.fillColor != 0) {
                drawRect(fillColor, topLeft = Offset(x, y), size = Size(w, h))
            }
            drawRect(strokeColor, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(shape.strokeWidth * scale))
        }
    }
}

private fun DrawScope.drawBackgroundGrid(pattern: BackgroundPattern, spacing: Float, pan: Pair<Float, Float>) {
    if (pattern == BackgroundPattern.BLANK || pattern == BackgroundPattern.NONE || spacing <= 5f) return
    val gridColor = Color(0xFFE2E8F0)
    val w = size.width
    val h = size.height

    val startX = (pan.first % spacing)
    val startY = (pan.second % spacing)

    when (pattern) {
        BackgroundPattern.GRID_SQUARE -> {
            var x = startX
            while (x < w) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                x += spacing
            }
            var y = startY
            while (y < h) {
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += spacing
            }
        }
        BackgroundPattern.DOTTED, BackgroundPattern.DOT_GRID -> {
            var x = startX
            while (x < w) {
                var y = startY
                while (y < h) {
                    drawCircle(gridColor, radius = 1.5f, center = Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }
        BackgroundPattern.LINED -> {
            var y = startY
            while (y < h) {
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += spacing
            }
        }
        else -> {}
    }
}
