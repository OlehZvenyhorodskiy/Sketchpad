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
import kotlin.math.cos
import kotlin.math.sin

enum class RulerMode {
    RULER,
    PROTRACTOR,
    COMPASS
}

fun ToolType.isDrawingTool(): Boolean = when (this) {
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

    /**
     * Finds the first physical contact between a moving stylus and either ruler edge. Coordinates
     * are deliberately screen-space because the ruler itself is a screen overlay.
     */
    fun edgeContact(
        previousPoint: Offset,
        currentPoint: Offset,
        contactZone: Float = 18f
    ): Pair<Offset, Pair<Offset, Offset>>? {
        if (!isVisible) return null
        nearestEdge(currentPoint, contactZone)?.let { return it }

        val edges = getEdgeLines().let { listOf(it.first, it.second) }
        return edges.mapNotNull { edge ->
            segmentIntersection(previousPoint, currentPoint, edge.first, edge.second)
                ?.let { contact -> Triple(contact, edge, (contact - previousPoint).getDistanceSquared()) }
        }.minByOrNull { it.third }?.let { it.first to it.second }
    }

    fun contains(point: Offset, padding: Float = 0f): Boolean {
        if (!isVisible) return false
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val relative = point - center
        val localX = relative.x * cosA + relative.y * sinA
        val localY = -relative.x * sinA + relative.y * cosA
        return abs(localX) <= length / 2f + padding && abs(localY) <= width / 2f + padding
    }

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

    private fun segmentIntersection(a: Offset, b: Offset, c: Offset, d: Offset): Offset? {
        val r = b - a
        val s = d - c
        val denominator = r.x * s.y - r.y * s.x
        if (abs(denominator) < 0.0001f) return null
        val cMinusA = c - a
        val t = (cMinusA.x * s.y - cMinusA.y * s.x) / denominator
        val u = (cMinusA.x * r.y - cMinusA.y * r.x) / denominator
        return if (t in 0f..1f && u in 0f..1f) a + r * t else null
    }
}

object DrawingEngine {

    fun strokeRenderWidth(tool: ToolType, baseWidth: Float, scale: Float = 1f): Float {
        val multiplier = when (tool) {
            ToolType.PENCIL -> 0.9f
            ToolType.FOUNTAIN_PEN -> 1.5f
            ToolType.MARKER -> 3.5f
            ToolType.INK_PEN -> 1.2f
            ToolType.AIRBRUSH -> 3.8f
            ToolType.CRAYON -> 1.7f
            ToolType.WATERCOLOR_BRUSH -> 3.1f
            ToolType.LASER -> 2.0f
            else -> 1.0f
        }
        return baseWidth * scale * multiplier
    }

    fun strokeRenderAlpha(tool: ToolType, colorAlpha: Float = 1f, layerAlpha: Float = 1f): Float {
        val toolAlpha = when (tool) {
            ToolType.MARKER -> 0.38f
            ToolType.PENCIL -> colorAlpha * 0.85f
            ToolType.AIRBRUSH -> colorAlpha * 0.22f
            ToolType.CRAYON -> colorAlpha * 0.78f
            ToolType.WATERCOLOR_BRUSH -> colorAlpha * 0.24f
            else -> colorAlpha
        }
        return toolAlpha * layerAlpha
    }

    /**
     * Diameter in canvas pixels. The control value, cursor and persisted CLEAR mask are 1:1.
     * Keeping the smallest diameter at one pixel lets the user shave either edge of a thick
     * stroke instead of inevitably cutting through its full width.
     */
    fun eraserDiameter(controlWidth: Float): Float = controlWidth.coerceIn(1f, 50f)

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
        val effectiveRadius = radius + strokeVisualRadius(stroke)
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

    /**
     * Hit-tests the complete swept eraser capsule rather than only delivered pointer samples.
     * This keeps a fast 2 px swipe persistent after UP instead of briefly clearing ink and then
     * restoring it because no sampled point happened to land on the stroke.
     */
    fun doesEraserPathAffectStroke(
        eraserPoints: List<StrokePoint>,
        eraserWidth: Float,
        stroke: StrokeEntity
    ): Boolean {
        if (eraserPoints.isEmpty() || stroke.points.isEmpty() || eraserWidth <= 0f) return false
        // This radius is only a target-membership test. The actual cleared corridor still uses
        // exactly eraserWidth pixels in RasterStrokeCompositor.
        val radius = eraserWidth / 2f + strokeVisualRadius(stroke)
        val radiusSq = radius * radius
        if (eraserPoints.size == 1) {
            return isPointInStroke(Offset(eraserPoints.first().x, eraserPoints.first().y), stroke, eraserWidth / 2f)
        }
        if (stroke.points.size == 1) {
            val point = stroke.points.first()
            return eraserPoints.zipWithNext().any { (start, end) ->
                squaredDistanceToSegment(point, start, end) <= radiusSq
            }
        }
        return eraserPoints.zipWithNext().any { (eraserStart, eraserEnd) ->
            stroke.points.zipWithNext().any { (strokeStart, strokeEnd) ->
                squaredDistanceBetweenSegments(eraserStart, eraserEnd, strokeStart, strokeEnd) <= radiusSq
            }
        }
    }

    private fun strokeVisualRadius(stroke: StrokeEntity): Float {
        val outerPass = when (stroke.tool) {
            ToolType.PENCIL -> 1.65f
            ToolType.AIRBRUSH -> 1.75f
            ToolType.CRAYON -> 1.2f
            ToolType.WATERCOLOR_BRUSH -> 1.35f
            ToolType.LASER -> 2.2f
            else -> 1f
        }
        return strokeRenderWidth(stroke.tool, stroke.baseWidth) * outerPass / 2f
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

    private fun squaredDistanceBetweenSegments(
        firstStart: StrokePoint,
        firstEnd: StrokePoint,
        secondStart: StrokePoint,
        secondEnd: StrokePoint
    ): Float {
        if (segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)) return 0f
        return minOf(
            squaredDistanceToSegment(firstStart, secondStart, secondEnd),
            squaredDistanceToSegment(firstEnd, secondStart, secondEnd),
            squaredDistanceToSegment(secondStart, firstStart, firstEnd),
            squaredDistanceToSegment(secondEnd, firstStart, firstEnd)
        )
    }

    private fun segmentsIntersect(a: StrokePoint, b: StrokePoint, c: StrokePoint, d: StrokePoint): Boolean {
        fun cross(from: StrokePoint, to: StrokePoint, point: StrokePoint): Float =
            (to.x - from.x) * (point.y - from.y) - (to.y - from.y) * (point.x - from.x)
        fun onSegment(start: StrokePoint, point: StrokePoint, end: StrokePoint): Boolean =
            point.x in minOf(start.x, end.x)..maxOf(start.x, end.x) &&
                point.y in minOf(start.y, end.y)..maxOf(start.y, end.y)

        val abC = cross(a, b, c)
        val abD = cross(a, b, d)
        val cdA = cross(c, d, a)
        val cdB = cross(c, d, b)
        if ((abC > 0f) != (abD > 0f) && (cdA > 0f) != (cdB > 0f)) return true
        val epsilon = 0.0001f
        return (abs(abC) <= epsilon && onSegment(a, c, b)) ||
            (abs(abD) <= epsilon && onSegment(a, d, b)) ||
            (abs(cdA) <= epsilon && onSegment(c, a, d)) ||
            (abs(cdB) <= epsilon && onSegment(c, b, d))
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
