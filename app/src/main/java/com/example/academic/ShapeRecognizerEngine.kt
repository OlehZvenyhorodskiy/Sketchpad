package com.example.academic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.models.ShapeType
import com.example.data.models.StrokePoint
import kotlin.math.abs
import kotlin.math.hypot

data class RecognizedShape(
    val type: ShapeType,
    val bounds: Rect,
    val confidence: Float,
    val vertices: List<Offset> = emptyList()
)

object ShapeRecognizerEngine {

    fun recognizeShape(points: List<StrokePoint>): RecognizedShape? {
        if (points.size < 5) return null

        val offsets = points.map { Offset(it.x, it.y) }
        val minX = offsets.minOf { it.x }
        val maxX = offsets.maxOf { it.x }
        val minY = offsets.minOf { it.y }
        val maxY = offsets.maxOf { it.y }

        val width = maxX - minX
        val height = maxY - minY
        if (width < 20f || height < 20f) return null

        val center = Offset(minX + width / 2f, minY + height / 2f)
        val bounds = Rect(minX, minY, maxX, maxY)

        // 1. Circle check
        val radiusEstimate = (width + height) / 4f
        var maxDistDiff = 0f
        var totalDistDiff = 0f

        for (pt in offsets) {
            val dist = hypot(pt.x - center.x, pt.y - center.y)
            val diff = abs(dist - radiusEstimate)
            totalDistDiff += diff
            if (diff > maxDistDiff) maxDistDiff = diff
        }

        val avgDistDiff = totalDistDiff / offsets.size
        val circleRatio = avgDistDiff / radiusEstimate

        if (circleRatio < 0.22f) {
            val confidence = (1f - circleRatio).coerceIn(0.6f, 0.99f)
            return RecognizedShape(ShapeType.CIRCLE, bounds, confidence)
        }

        // 2. Corner / Vertex Detection (Ramer-Douglas-Peucker simplification)
        val simplified = ramerDouglasPeucker(offsets, epsilon = width * 0.06f)

        if (simplified.size == 3 || simplified.size == 4) {
            val confidence = 0.85f
            return RecognizedShape(ShapeType.TRIANGLE, bounds, confidence, simplified)
        } else if (simplified.size in 4..6) {
            val confidence = 0.90f
            return RecognizedShape(ShapeType.SQUARE, bounds, confidence, simplified)
        }

        // Fallback: Check bounding box aspect ratio and closedness
        val firstLastDist = hypot(offsets.first().x - offsets.last().x, offsets.first().y - offsets.last().y)
        val isClosed = firstLastDist < (width + height) * 0.25f

        if (isClosed) {
            return RecognizedShape(ShapeType.SQUARE, bounds, 0.75f)
        }

        return null
    }

    private fun ramerDouglasPeucker(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size < 3) return points

        var dmax = 0f
        var index = 0
        val end = points.size - 1

        for (i in 1 until end) {
            val d = perpendicularDistance(points[i], points[0], points[end])
            if (d > dmax) {
                index = i
                dmax = d
            }
        }

        return if (dmax > epsilon) {
            val recResults1 = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
            val recResults2 = ramerDouglasPeucker(points.subList(index, points.size), epsilon)
            recResults1.dropLast(1) + recResults2
        } else {
            listOf(points[0], points[end])
        }
    }

    private fun perpendicularDistance(pt: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val mag = hypot(dx, dy)
        if (mag == 0f) return hypot(pt.x - lineStart.x, pt.y - lineStart.y)

        val u = ((pt.x - lineStart.x) * dx + (pt.y - lineStart.y) * dy) / (mag * mag)
        val intersection = Offset(lineStart.x + u * dx, lineStart.y + u * dy)
        return hypot(pt.x - intersection.x, pt.y - intersection.y)
    }
}
