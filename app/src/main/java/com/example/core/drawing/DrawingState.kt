package com.example.core.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.PathMeasure
import com.example.data.models.HslaColor
import com.example.data.models.ShapeEntity
import com.example.data.models.ShapeType
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

enum class RulerMode {
    RULER,
    PROTRACTOR,
    COMPASS
}

data class RulerState(
    val isVisible: Boolean = false,
    val center: Offset = Offset(400f, 400f),
    val angleRad: Float = 0f,
    val length: Float = 700f,
    val width: Float = 90f,
    val mode: RulerMode = RulerMode.RULER
) {
    fun getEdgeLines(): Pair<Pair<Offset, Offset>, Pair<Offset, Offset>> {
        val dx = cos(angleRad) * length / 2f
        val dy = sin(angleRad) * length / 2f
        val nx = -sin(angleRad) * width / 2f
        val ny = cos(angleRad) * width / 2f

        val topStart = Offset(center.x - dx + nx, center.y - dy + ny)
        val topEnd = Offset(center.x + dx + nx, center.y + dy + ny)
        val bottomStart = Offset(center.x - dx - nx, center.y - dy - ny)
        val bottomEnd = Offset(center.x + dx - nx, center.y + dy - ny)

        return Pair(Pair(topStart, topEnd), Pair(bottomStart, bottomEnd))
    }

    fun nearestEdge(point: Offset, guideZone: Float): Pair<Offset, Pair<Offset, Offset>>? {
        if (!isVisible) return null
        val (top, bottom) = getEdgeLines()
        val pTop = projectPointToSegment(point, top.first, top.second)
        val pBot = projectPointToSegment(point, bottom.first, bottom.second)
        val dTop = (point - pTop).getDistance()
        val dBot = (point - pBot).getDistance()
        val best = if (dTop <= dBot) pTop to top else pBot to bottom
        return if (minOf(dTop, dBot) <= guideZone) (best.first to best.second) else null
    }

    fun projectOn(edge: Pair<Offset, Offset>, point: Offset): Offset = projectPointToSegment(point, edge.first, edge.second)

    fun snapPointIfClose(point: Offset, thresholdDp: Float = 16f, scale: Float = 1f): Offset? {
        if (!isVisible) return null
        val (top, bottom) = getEdgeLines()
        val effectiveThreshold = thresholdDp / scale.coerceAtLeast(0.1f)

        val snapTop = projectPointToSegment(point, top.first, top.second)
        if ((point - snapTop).getDistance() <= effectiveThreshold) return snapTop

        val snapBottom = projectPointToSegment(point, bottom.first, bottom.second)
        if ((point - snapBottom).getDistance() <= effectiveThreshold) return snapBottom

        return null
    }

    fun projectPointToSegment(p: Offset, a: Offset, b: Offset): Offset {
        val ab = b - a
        val abSq = ab.x * ab.x + ab.y * ab.y
        if (abSq == 0f) return a
        val ap = p - a
        val t = ((ap.x * ab.x + ap.y * ab.y) / abSq).coerceIn(0f, 1f)
        return Offset(a.x + t * ab.x, a.y + t * ab.y)
    }
}

object DrawingEngine {

    fun strokeRenderWidth(tool: ToolType, baseWidth: Float, scale: Float = 1f): Float {
        val multiplier = when (tool) {
            ToolType.PENCIL -> 0.9f
            ToolType.FOUNTAIN_PEN -> 1.5f
            ToolType.MARKER -> 3.5f
            ToolType.INK_PEN -> 1.2f
            ToolType.LASER -> 2.0f
            else -> 1.0f
        }
        return baseWidth * scale * multiplier
    }

    fun strokeRenderAlpha(tool: ToolType, colorAlpha: Float = 1f, layerAlpha: Float = 1f): Float {
        val toolAlpha = when (tool) {
            ToolType.MARKER -> 0.38f
            ToolType.PENCIL -> colorAlpha * 0.85f
            else -> colorAlpha
        }
        return toolAlpha * layerAlpha
    }

    fun createSmoothPath(points: List<StrokePoint>, scale: Float = 1f, panX: Float = 0f, panY: Float = 0f): Path {
        if (points.isEmpty()) return Path()
        if (points.size == 1) {
            val p = points[0]
            val x0 = p.x * scale + panX
            val y0 = p.y * scale + panY
            val radius = (p.pressure * 2.5f * scale).coerceAtLeast(1.5f * scale)
            return Path().apply {
                addOval(Rect(x0 - radius, y0 - radius, x0 + radius, y0 + radius))
            }
        }
        if (points.size == 2) {
            return Path().apply {
                val x0 = points[0].x * scale + panX
                val y0 = points[0].y * scale + panY
                val x1 = points[1].x * scale + panX
                val y1 = points[1].y * scale + panY
                moveTo(x0, y0)
                lineTo(x1, y1)
            }
        }
        return PathSmoothing.createCatmullRomPath(points, tension = 0.5f, segments = 6, scale = scale, panX = panX, panY = panY).asComposePath()
    }

    fun isPointInStroke(point: Offset, stroke: StrokeEntity, radius: Float): Boolean {
        if (stroke.points.isEmpty()) return false
        val effectiveRadius = radius + strokeRenderWidth(stroke.tool, stroke.baseWidth) / 2f
        val checkRadiusSq = effectiveRadius * effectiveRadius
        if (stroke.points.size == 1) {
            return squaredDistance(point, stroke.points.first()) <= checkRadiusSq
        }
        for (index in 0 until stroke.points.lastIndex) {
            if (squaredDistanceToSegment(point, stroke.points[index], stroke.points[index + 1]) <= checkRadiusSq) {
                return true
            }
        }
        return false
    }

    fun erasePixelMode(stroke: StrokeEntity, eraserPos: Offset, radius: Float): List<StrokeEntity> {
        return eraseStrokeAlongPath(stroke, listOf(StrokePoint(eraserPos.x, eraserPos.y)), radius)
    }

    /**
     * Cuts a stroke with a swept circular eraser. The original implementation only removed stored
     * control points, so a long segment could survive even when the eraser visibly crossed it. It
     * also left coarse, square-looking gaps. We densify the polyline, find the circle boundaries,
     * and keep boundary points so the renderer's round caps produce a natural cut.
     */
    fun eraseStrokeAlongPath(
        stroke: StrokeEntity,
        eraserPoints: List<StrokePoint>,
        radius: Float
    ): List<StrokeEntity> {
        if (stroke.points.isEmpty() || eraserPoints.isEmpty() || radius <= 0f) return listOf(stroke)

        val effectiveRadius = radius + strokeRenderWidth(stroke.tool, stroke.baseWidth) / 2f
        val sampleStep = (effectiveRadius / 3f).coerceIn(0.75f, 2.5f)
        val denseStroke = densify(stroke.points, sampleStep)
        val denseEraser = densify(eraserPoints, sampleStep)
        val radiusSq = effectiveRadius * effectiveRadius

        val strokeMinX = denseStroke.minOf { it.x }
        val strokeMaxX = denseStroke.maxOf { it.x }
        val strokeMinY = denseStroke.minOf { it.y }
        val strokeMaxY = denseStroke.maxOf { it.y }
        val eraserMinX = denseEraser.minOf { it.x } - effectiveRadius
        val eraserMaxX = denseEraser.maxOf { it.x } + effectiveRadius
        val eraserMinY = denseEraser.minOf { it.y } - effectiveRadius
        val eraserMaxY = denseEraser.maxOf { it.y } + effectiveRadius
        if (strokeMaxX < eraserMinX || strokeMinX > eraserMaxX ||
            strokeMaxY < eraserMinY || strokeMinY > eraserMaxY
        ) return listOf(stroke)

        fun isErased(point: StrokePoint): Boolean =
            squaredDistanceToPath(point, denseEraser) <= radiusSq

        val chunks = mutableListOf<List<StrokePoint>>()
        var chunk = mutableListOf<StrokePoint>()
        var previous = denseStroke.first()
        var previousErased = isErased(previous)
        var erasedAnyPoint = previousErased
        if (!previousErased) chunk += previous

        for (index in 1 until denseStroke.size) {
            val current = denseStroke[index]
            val currentErased = isErased(current)
            erasedAnyPoint = erasedAnyPoint || currentErased
            when {
                !previousErased && !currentErased -> chunk += current
                !previousErased && currentErased -> {
                    chunk += findEraserBoundary(previous, current, ::isErased)
                    if (chunk.size >= 2) chunks += chunk
                    chunk = mutableListOf()
                }
                previousErased && !currentErased -> {
                    chunk = mutableListOf(findEraserBoundary(current, previous, ::isErased), current)
                }
            }
            previous = current
            previousErased = currentErased
        }
        if (chunk.size >= 2) chunks += chunk
        if (!erasedAnyPoint) return listOf(stroke)

        return chunks.mapIndexed { index, points ->
            stroke.copy(
                id = if (index == 0) stroke.id else java.util.UUID.randomUUID().toString(),
                points = points
            )
        }
    }

    private fun densify(points: List<StrokePoint>, maxStep: Float): List<StrokePoint> {
        if (points.size < 2) return points
        val result = ArrayList<StrokePoint>()
        result += points.first()
        for (index in 0 until points.lastIndex) {
            val start = points[index]
            val end = points[index + 1]
            val dx = end.x - start.x
            val dy = end.y - start.y
            val distance = sqrt(dx * dx + dy * dy)
            val segments = max(1, ceil(distance / maxStep).toInt())
            for (segment in 1..segments) {
                result += interpolate(start, end, segment.toFloat() / segments)
            }
        }
        return result
    }

    private fun findEraserBoundary(
        outside: StrokePoint,
        inside: StrokePoint,
        isErased: (StrokePoint) -> Boolean
    ): StrokePoint {
        var outsideT = 0f
        var insideT = 1f
        repeat(10) {
            val middle = (outsideT + insideT) / 2f
            if (isErased(interpolate(outside, inside, middle))) insideT = middle else outsideT = middle
        }
        return interpolate(outside, inside, outsideT)
    }

    private fun interpolate(start: StrokePoint, end: StrokePoint, fraction: Float): StrokePoint = StrokePoint(
        x = start.x + (end.x - start.x) * fraction,
        y = start.y + (end.y - start.y) * fraction,
        pressure = start.pressure + (end.pressure - start.pressure) * fraction,
        tilt = start.tilt + (end.tilt - start.tilt) * fraction,
        timestampMs = start.timestampMs + ((end.timestampMs - start.timestampMs) * fraction).toLong()
    )

    private fun squaredDistanceToPath(point: StrokePoint, path: List<StrokePoint>): Float {
        if (path.size == 1) return squaredDistance(point, path.first())
        var minimum = Float.POSITIVE_INFINITY
        for (index in 0 until path.lastIndex) {
            minimum = minOf(minimum, squaredDistanceToSegment(point, path[index], path[index + 1]))
        }
        return minimum
    }

    private fun squaredDistance(a: Offset, b: StrokePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun squaredDistance(a: StrokePoint, b: StrokePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun squaredDistanceToSegment(point: Offset, start: StrokePoint, end: StrokePoint): Float =
        squaredDistanceToSegment(StrokePoint(point.x, point.y), start, end)

    private fun squaredDistanceToSegment(point: StrokePoint, start: StrokePoint, end: StrokePoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lengthSq = dx * dx + dy * dy
        if (lengthSq <= 0.000001f) return squaredDistance(point, start)
        val fraction = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSq).coerceIn(0f, 1f)
        val closestX = start.x + dx * fraction
        val closestY = start.y + dy * fraction
        val distanceX = point.x - closestX
        val distanceY = point.y - closestY
        return distanceX * distanceX + distanceY * distanceY
    }

    fun createShapePath(shapeType: ShapeType, rect: Rect): Path {
        val path = Path()
        when (shapeType) {
            ShapeType.CIRCLE -> {
                path.addOval(rect)
            }
            ShapeType.SQUARE -> {
                path.addRect(rect)
            }
            ShapeType.TRIANGLE -> {
                path.moveTo(rect.center.x, rect.top)
                path.lineTo(rect.right, rect.bottom)
                path.lineTo(rect.left, rect.bottom)
                path.close()
            }
            ShapeType.ARROW -> {
                val midY = rect.center.y
                val shaftHeight = rect.height * 0.25f
                val headWidth = rect.width * 0.35f
                path.moveTo(rect.left, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, rect.top)
                path.lineTo(rect.right, midY)
                path.lineTo(rect.right - headWidth, rect.bottom)
                path.lineTo(rect.right - headWidth, midY + shaftHeight / 2)
                path.lineTo(rect.left, midY + shaftHeight / 2)
                path.close()
            }
            ShapeType.BOLD_ARROW -> {
                val midY = rect.center.y
                val shaftHeight = rect.height * 0.4f
                val headWidth = rect.width * 0.4f
                path.moveTo(rect.left, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, midY - shaftHeight / 2)
                path.lineTo(rect.right - headWidth, rect.top)
                path.lineTo(rect.right, midY)
                path.lineTo(rect.right - headWidth, rect.bottom)
                path.lineTo(rect.right - headWidth, midY + shaftHeight / 2)
                path.lineTo(rect.left, midY + shaftHeight / 2)
                path.close()
            }
            ShapeType.STAR -> {
                val cx = rect.center.x
                val cy = rect.center.y
                val outerR = minOf(rect.width, rect.height) / 2f
                val innerR = outerR * 0.4f
                val points = 5
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = i * Math.PI / points - Math.PI / 2
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            ShapeType.HEXAGON -> {
                val cx = rect.center.x
                val cy = rect.center.y
                val rx = rect.width / 2f
                val ry = rect.height / 2f
                for (i in 0 until 6) {
                    val angle = i * Math.PI / 3 - Math.PI / 6
                    val x = cx + (rx * cos(angle)).toFloat()
                    val y = cy + (ry * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            ShapeType.PENTAGON -> {
                val cx = rect.center.x
                val cy = rect.center.y
                val rx = rect.width / 2f
                val ry = rect.height / 2f
                for (i in 0 until 5) {
                    val angle = i * 2 * Math.PI / 5 - Math.PI / 2
                    val x = cx + (rx * cos(angle)).toFloat()
                    val y = cy + (ry * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            ShapeType.CLOUD -> {
                path.addOval(Rect(rect.left, rect.top + rect.height * 0.2f, rect.left + rect.width * 0.6f, rect.bottom))
                path.addOval(Rect(rect.left + rect.width * 0.25f, rect.top, rect.right - rect.width * 0.25f, rect.bottom - rect.height * 0.15f))
                path.addOval(Rect(rect.right - rect.width * 0.6f, rect.top + rect.height * 0.2f, rect.right, rect.bottom))
            }
            ShapeType.SPEECH_BUBBLE -> {
                val bubbleRect = Rect(rect.left, rect.top, rect.right, rect.bottom - rect.height * 0.25f)
                path.addOval(bubbleRect)
                path.moveTo(rect.left + rect.width * 0.3f, rect.bottom - rect.height * 0.3f)
                path.lineTo(rect.left + rect.width * 0.15f, rect.bottom)
                path.lineTo(rect.left + rect.width * 0.45f, rect.bottom - rect.height * 0.25f)
                path.close()
            }
        }
        return path
    }
}
