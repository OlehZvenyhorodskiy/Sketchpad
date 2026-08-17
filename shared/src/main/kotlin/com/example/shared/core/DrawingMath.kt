package com.example.shared.core

import com.example.shared.model.HslaColor
import com.example.shared.model.StrokeEntity
import com.example.shared.model.StrokePoint
import com.example.shared.model.SymmetryMode
import com.example.shared.model.ToolType
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class Point2D(val x: Float, val y: Float)

object DrawingMath {

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

    /**
     * Catmull-Rom spline interpolation generating smooth sub-segments.
     */
    fun interpolateCatmullRom(
        points: List<StrokePoint>,
        tension: Float = 0.5f,
        segmentsPerPoint: Int = 6
    ): List<StrokePoint> {
        if (points.size < 3) return points
        val result = mutableListOf<StrokePoint>()
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < points.size) points[i + 2] else p2

            for (step in 1..segmentsPerPoint) {
                val t = step.toFloat() / segmentsPerPoint
                val t2 = t * t
                val t3 = t2 * t

                // Catmull-Rom basis with tension
                val s = (1f - tension) / 2f
                val h1 = -s * t3 + 2f * s * t2 - s * t
                val h2 = (2f - s) * t3 + (s - 3f) * t2 + 1f
                val h3 = (s - 2f) * t3 + (3f - 2f * s) * t2 + s * t
                val h4 = s * t3 - s * t2

                val x = h1 * p0.x + h2 * p1.x + h3 * p2.x + h4 * p3.x
                val y = h1 * p0.y + h2 * p1.y + h3 * p2.y + h4 * p3.y
                val pressure = p1.pressure + (p2.pressure - p1.pressure) * t
                val tilt = p1.tilt + (p2.tilt - p1.tilt) * t
                val azimuth = p1.azimuth + (p2.azimuth - p1.azimuth) * t
                val time = (p1.timestampMs + (p2.timestampMs - p1.timestampMs) * t).toLong()

                result.add(StrokePoint(x, y, pressure, tilt, azimuth, time))
            }
        }
        return result
    }

    /**
     * Ramer-Douglas-Peucker simplification algorithm for polylines.
     */
    fun simplifyPolyline(points: List<StrokePoint>, tolerance: Float): List<StrokePoint> {
        if (points.size <= 2 || tolerance <= 0f) return points
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        val ranges = ArrayDeque<Pair<Int, Int>>()
        ranges.add(0 to points.lastIndex)
        val toleranceSq = tolerance * tolerance

        while (ranges.isNotEmpty()) {
            val (startIndex, endIndex) = ranges.removeLast()
            if (endIndex - startIndex <= 1) continue
            var farthestIndex = -1
            var farthestDistanceSq = 0f

            for (index in startIndex + 1 until endIndex) {
                val distanceSq = squaredDistanceToSegment(points[index], points[startIndex], points[endIndex])
                if (distanceSq > farthestDistanceSq) {
                    farthestDistanceSq = distanceSq
                    farthestIndex = index
                }
            }

            if (farthestIndex >= 0 && farthestDistanceSq > toleranceSq) {
                keep[farthestIndex] = true
                ranges.add(startIndex to farthestIndex)
                ranges.add(farthestIndex to endIndex)
            }
        }

        return points.filterIndexed { index, _ -> keep[index] }
    }

    /**
     * Mirrored stroke generator for Smart Symmetry (Horizontal, Vertical, Quad axes).
     */
    fun generateSymmetricStrokes(
        stroke: StrokeEntity,
        symmetryMode: SymmetryMode,
        centerX: Float,
        centerY: Float
    ): List<StrokeEntity> {
        if (symmetryMode == SymmetryMode.NONE) return listOf(stroke)
        val list = mutableListOf(stroke)

        if (symmetryMode == SymmetryMode.VERTICAL || symmetryMode == SymmetryMode.QUAD) {
            val mirroredPoints = stroke.points.map { pt ->
                pt.copy(x = 2f * centerX - pt.x)
            }
            list.add(stroke.copy(id = java.util.UUID.randomUUID().toString(), points = mirroredPoints))
        }

        if (symmetryMode == SymmetryMode.HORIZONTAL || symmetryMode == SymmetryMode.QUAD) {
            val mirroredPoints = stroke.points.map { pt ->
                pt.copy(y = 2f * centerY - pt.y)
            }
            list.add(stroke.copy(id = java.util.UUID.randomUUID().toString(), points = mirroredPoints))
        }

        if (symmetryMode == SymmetryMode.QUAD) {
            val quadPoints = stroke.points.map { pt ->
                pt.copy(x = 2f * centerX - pt.x, y = 2f * centerY - pt.y)
            }
            list.add(stroke.copy(id = java.util.UUID.randomUUID().toString(), points = quadPoints))
        }

        return list
    }

    /**
     * Erases parts of a stroke that intersect the swept circular eraser path.
     */
    fun eraseStrokeAlongPath(
        stroke: StrokeEntity,
        eraserPoints: List<StrokePoint>,
        radius: Float
    ): List<StrokeEntity> {
        if (stroke.points.isEmpty() || eraserPoints.isEmpty() || radius <= 0f) return listOf(stroke)

        val effectiveRadius = radius + strokeRenderWidth(stroke.tool, stroke.baseWidth) / 2f
        val sampleStep = (effectiveRadius / 2f).coerceIn(1f, 4f)
        val simplifiedStroke = simplifyPolyline(stroke.points, tolerance = 0.35f)
        val simplifiedEraser = simplifyPolyline(
            eraserPoints,
            tolerance = (radius * 0.12f).coerceIn(0.6f, 1.8f)
        )
        val denseStroke = densify(simplifiedStroke, sampleStep)
        val denseEraser = simplifiedEraser
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

        val originalStart = denseStroke.first()
        val originalEnd = denseStroke.last()
        return chunks.mapIndexed { index, points ->
            val keepsOriginalStart = squaredDistance(points.first(), originalStart) < 0.0001f
            val keepsOriginalEnd = squaredDistance(points.last(), originalEnd) < 0.0001f
            stroke.copy(
                id = if (index == 0) stroke.id else java.util.UUID.randomUUID().toString(),
                points = points,
                startCapRound = keepsOriginalStart && stroke.startCapRound,
                endCapRound = keepsOriginalEnd && stroke.endCapRound
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
        azimuth = start.azimuth + (end.azimuth - start.azimuth) * fraction,
        timestampMs = start.timestampMs + ((end.timestampMs - start.timestampMs) * fraction).toLong()
    )

    fun squaredDistance(a: StrokePoint, b: StrokePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun squaredDistanceToPath(point: StrokePoint, path: List<StrokePoint>): Float {
        if (path.size == 1) return squaredDistance(point, path.first())
        var minimum = Float.POSITIVE_INFINITY
        for (index in 0 until path.lastIndex) {
            minimum = min(minimum, squaredDistanceToSegment(point, path[index], path[index + 1]))
        }
        return minimum
    }

    fun squaredDistanceToSegment(point: StrokePoint, start: StrokePoint, end: StrokePoint): Float {
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
}
