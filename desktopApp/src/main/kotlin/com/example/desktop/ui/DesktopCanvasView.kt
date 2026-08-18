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
import com.example.desktop.RulerState
import com.example.desktop.input.WindowsInkHandler
import com.example.desktop.theme.toColor
import com.example.shared.academic.FunctionPlotterEngine
import com.example.shared.core.DrawingMath
import com.example.shared.model.*
import java.util.UUID
import kotlin.math.*

@Composable
fun DesktopCanvasView(
    viewModel: DesktopViewModel,
    isDrawingActive: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()
    val brushOpacity by viewModel.brushOpacity.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val eraserMode by viewModel.eraserMode.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val rulerState by viewModel.rulerState.collectAsState()

    val currentPage = pages.getOrElse(currentPageIndex) { pages.first() }

    // Active in-progress stroke
    var activeStrokePoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var activeLassoPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var activeStrokeId by remember { mutableStateOf(UUID.randomUUID().toString()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(currentPage.backgroundLineColor).copy(alpha = 0.05f))
            .pointerInput(currentTool, brushSize, brushOpacity, currentColor, zoomScale, panOffset, eraserMode, selectionMode, rulerState) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDrawingActive(true)
                        activeStrokeId = UUID.randomUUID().toString()
                        val worldX = (offset.x - panOffset.first) / zoomScale
                        val worldY = (offset.y - panOffset.second) / zoomScale

                        if (currentTool == ToolType.SELECTOR && selectionMode == SelectionMode.LASSO) {
                            activeLassoPoints = listOf(offset)
                        } else {
                            activeStrokePoints = listOf(StrokePoint(worldX, worldY, pressure = 0.5f))
                        }
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        change.consume()
                        val sample = WindowsInkHandler.processPointerChange(change)
                        var worldX = (sample.x - panOffset.first) / zoomScale
                        var worldY = (sample.y - panOffset.second) / zoomScale

                        // Snap to Ruler if ruler tool active and close
                        if (rulerState.isVisible) {
                            val rSnap = snapToRuler(Offset(worldX, worldY), rulerState)
                            if (rSnap != null) {
                                worldX = rSnap.x
                                worldY = rSnap.y
                            }
                        }

                        if (currentTool == ToolType.SELECTOR && selectionMode == SelectionMode.LASSO) {
                            activeLassoPoints = activeLassoPoints + change.position
                        } else {
                            val newPoint = StrokePoint(worldX, worldY, pressure = sample.pressure)
                            activeStrokePoints = activeStrokePoints + newPoint

                            // Live Eraser
                            if (currentTool == ToolType.ERASER) {
                                val activeLayer = currentPage.getActiveLayer()
                                if (eraserMode == EraserMode.OBJECT) {
                                    val remaining = activeLayer.strokes.filterNot { s ->
                                        DrawingMath.strokeIntersectsCircle(s, worldX, worldY, brushSize)
                                    }
                                    if (remaining.size != activeLayer.strokes.size) {
                                        viewModel.executeCommand(
                                            EraseStrokesCommand(activeLayer.id, activeLayer.strokes, remaining)
                                        )
                                    }
                                } else {
                                    val erased = activeLayer.strokes.flatMap { s ->
                                        DrawingMath.eraseStrokeAlongPath(s, listOf(newPoint), brushSize)
                                    }
                                    viewModel.executeCommand(
                                        EraseStrokesCommand(activeLayer.id, activeLayer.strokes, erased)
                                    )
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        isDrawingActive(false)
                        if (currentTool == ToolType.SELECTOR && selectionMode == SelectionMode.LASSO) {
                            // Select strokes inside lasso
                            val worldPoly = activeLassoPoints.map {
                                Offset((it.x - panOffset.first) / zoomScale, (it.y - panOffset.second) / zoomScale)
                            }
                            viewModel.selectStrokesInPolygon(worldPoly)
                            activeLassoPoints = emptyList()
                        } else if (activeStrokePoints.isNotEmpty() && currentTool != ToolType.ERASER && currentTool != ToolType.POINTER) {
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
                        isDrawingActive(false)
                        activeStrokePoints = emptyList()
                        activeLassoPoints = emptyList()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Background Grid Pattern
            drawBackgroundGrid(currentPage.backgroundPattern, currentPage.backgroundSpacing * zoomScale, panOffset)

            // 2. Render Page Layers Bottom-Up
            currentPage.visibleLayersBottomUp().forEach { layer ->
                val layerAlpha = layer.opacity

                // 2.1 Strokes
                layer.strokes.forEach { stroke ->
                    drawStrokeEntity(stroke, zoomScale, panOffset, layerAlpha)
                }

                // 2.2 Shapes
                layer.shapes.forEach { shape ->
                    drawShapeEntity(shape, zoomScale, panOffset, layerAlpha)
                }

                // 2.3 Charts
                layer.charts.forEach { chart ->
                    drawChartEntity(chart, zoomScale, panOffset, layerAlpha)
                }
            }

            // 3. Active in-progress stroke
            if (activeStrokePoints.isNotEmpty() && currentTool != ToolType.ERASER) {
                val activeStroke = StrokeEntity(
                    id = activeStrokeId,
                    tool = currentTool,
                    colorHsla = currentColor.copy(alpha = brushOpacity),
                    baseWidth = brushSize,
                    points = activeStrokePoints
                )
                drawStrokeEntity(activeStroke, zoomScale, panOffset, 1.0f)
            }
        }

        // 4. Lasso Overlay
        if (activeLassoPoints.isNotEmpty()) {
            DesktopLassoOverlay(points = activeLassoPoints)
        }

        // 5. Code Block Cards on Canvas
        currentPage.getActiveLayer().codeBlocks.forEach { codeBlock ->
            DesktopCodeBlockCanvasCard(
                codeBlock = codeBlock,
                onUpdate = { updated -> viewModel.updateCodeBlock(updated) },
                onDelete = { viewModel.deleteCodeBlock(codeBlock.id) },
                zoomScale = zoomScale,
                panOffset = panOffset
            )
        }
    }
}

private fun snapToRuler(pt: Offset, ruler: RulerState): Offset? {
    if (!ruler.isVisible) return null
    val dx = cos(ruler.angleRad) * ruler.length / 2f
    val dy = sin(ruler.angleRad) * ruler.length / 2f
    val nx = -sin(ruler.angleRad) * ruler.width / 2f
    val ny = cos(ruler.angleRad) * ruler.width / 2f

    val topStart = Offset(ruler.center.x - dx + nx, ruler.center.y - dy + ny)
    val topEnd = Offset(ruler.center.x + dx + nx, ruler.center.y + dy + ny)

    val dist = pointToSegmentDist(pt, topStart, topEnd)
    return if (dist < 20f) projectPointToSegment(pt, topStart, topEnd) else null
}

private fun pointToSegmentDist(p: Offset, a: Offset, b: Offset): Float {
    val proj = projectPointToSegment(p, a, b)
    return (p - proj).getDistance()
}

private fun projectPointToSegment(p: Offset, a: Offset, b: Offset): Offset {
    val ab = b - a
    val abSq = ab.x * ab.x + ab.y * ab.y
    if (abSq == 0f) return a
    val ap = p - a
    val t = ((ap.x * ab.x + ap.y * ab.y) / abSq).coerceIn(0f, 1f)
    return Offset(a.x + t * ab.x, a.y + t * ab.y)
}

private fun DrawScope.drawStrokeEntity(
    stroke: StrokeEntity,
    zoomScale: Float,
    panOffset: Pair<Float, Float>,
    layerAlpha: Float
) {
    if (stroke.points.size < 2) return

    val color = stroke.colorHsla.toColor().copy(alpha = stroke.colorHsla.alpha * layerAlpha)
    val strokeW = (stroke.baseWidth * zoomScale).coerceAtLeast(1f)

    val path = Path()
    val first = stroke.points.first()
    path.moveTo(first.x * zoomScale + panOffset.first, first.y * zoomScale + panOffset.second)

    for (i in 1 until stroke.points.size) {
        val p = stroke.points[i]
        path.lineTo(p.x * zoomScale + panOffset.first, p.y * zoomScale + panOffset.second)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeW,
            cap = if (stroke.tool == ToolType.MARKER) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawShapeEntity(
    shape: ShapeEntity,
    zoomScale: Float,
    panOffset: Pair<Float, Float>,
    layerAlpha: Float
) {
    val screenX = shape.x * zoomScale + panOffset.first
    val screenY = shape.y * zoomScale + panOffset.second
    val screenW = shape.width * zoomScale
    val screenH = shape.height * zoomScale

    val strokeColor = Color(shape.strokeColor).copy(alpha = layerAlpha)
    val strokeWidth = (shape.strokeWidth * zoomScale).coerceAtLeast(1f)

    when (shape.shapeType) {
        ShapeType.CIRCLE -> {
            drawOval(
                color = strokeColor,
                topLeft = Offset(screenX, screenY),
                size = Size(screenW, screenH),
                style = Stroke(strokeWidth)
            )
        }
        ShapeType.SQUARE -> {
            drawRect(
                color = strokeColor,
                topLeft = Offset(screenX, screenY),
                size = Size(screenW, screenH),
                style = Stroke(strokeWidth)
            )
        }
        ShapeType.TRIANGLE -> {
            val path = Path().apply {
                moveTo(screenX + screenW / 2f, screenY)
                lineTo(screenX + screenW, screenY + screenH)
                lineTo(screenX, screenY + screenH)
                close()
            }
            drawPath(path = path, color = strokeColor, style = Stroke(strokeWidth))
        }
        ShapeType.ARROW, ShapeType.BOLD_ARROW -> {
            drawLine(
                color = strokeColor,
                start = Offset(screenX, screenY + screenH / 2f),
                end = Offset(screenX + screenW, screenY + screenH / 2f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = strokeColor,
                start = Offset(screenX + screenW - 16f * zoomScale, screenY),
                end = Offset(screenX + screenW, screenY + screenH / 2f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = strokeColor,
                start = Offset(screenX + screenW - 16f * zoomScale, screenY + screenH),
                end = Offset(screenX + screenW, screenY + screenH / 2f),
                strokeWidth = strokeWidth
            )
        }
        else -> {
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(screenX, screenY),
                size = Size(screenW, screenH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * zoomScale, 12f * zoomScale),
                style = Stroke(strokeWidth)
            )
        }
    }
}

private fun DrawScope.drawChartEntity(
    chart: ChartElementEntity,
    zoomScale: Float,
    panOffset: Pair<Float, Float>,
    layerAlpha: Float
) {
    val screenX = chart.x * zoomScale + panOffset.first
    val screenY = chart.y * zoomScale + panOffset.second
    val screenW = chart.width * zoomScale
    val screenH = chart.height * zoomScale

    // Grid box
    drawRect(
        color = Color(0xFFF1F5F9).copy(alpha = layerAlpha),
        topLeft = Offset(screenX, screenY),
        size = Size(screenW, screenH)
    )
    drawRect(
        color = Color(0xFFCBD5E1).copy(alpha = layerAlpha),
        topLeft = Offset(screenX, screenY),
        size = Size(screenW, screenH),
        style = Stroke(1.5f * zoomScale)
    )

    // X & Y Axes
    val midX = screenX + screenW / 2f
    val midY = screenY + screenH / 2f
    drawLine(Color(0xFF64748B), Offset(screenX, midY), Offset(screenX + screenW, midY), 1.5f * zoomScale)
    drawLine(Color(0xFF64748B), Offset(midX, screenY), Offset(midX, screenY + screenH), 1.5f * zoomScale)
}

private fun DrawScope.drawBackgroundGrid(
    pattern: BackgroundPattern,
    spacing: Float,
    panOffset: Pair<Float, Float>
) {
    if (pattern == BackgroundPattern.BLANK || pattern == BackgroundPattern.NONE) return
    val gridColor = Color(0xFFE2E8F0)
    val startX = (panOffset.first % spacing) - spacing
    val startY = (panOffset.second % spacing) - spacing

    when (pattern) {
        BackgroundPattern.GRID_SQUARE -> {
            var x = startX
            while (x < size.width + spacing) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                x += spacing
            }
            var y = startY
            while (y < size.height + spacing) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                y += spacing
            }
        }
        BackgroundPattern.LINED -> {
            var y = startY
            while (y < size.height + spacing) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                y += spacing
            }
        }
        BackgroundPattern.DOTTED, BackgroundPattern.DOT_GRID -> {
            var x = startX
            while (x < size.width + spacing) {
                var y = startY
                while (y < size.height + spacing) {
                    drawCircle(gridColor, radius = 1.5f, center = Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }
        else -> {}
    }
}
