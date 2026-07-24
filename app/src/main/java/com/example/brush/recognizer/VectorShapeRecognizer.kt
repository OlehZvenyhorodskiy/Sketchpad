package com.example.brush.recognizer

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

data class Point(val x: Float, val y: Float)

enum class RecognizedShape { RECTANGLE, CIRCLE, TRIANGLE, LINE }

object VectorShapeRecognizer {

    fun recognizeShape(points: List<Offset>): RecognizedShape? {
        if (points.size < 5) return null

        val simplified = ramerDouglasPeucker(points, epsilon = 10f)
        val isClosed = hypot(
            (points.first().x - points.last().x).toDouble(),
            (points.first().y - points.last().y).toDouble()
        ) < 50.0

        return when {
            isClosed && simplified.size in 4..6 -> RecognizedShape.RECTANGLE
            isClosed && simplified.size > 8 -> RecognizedShape.CIRCLE
            isClosed && simplified.size == 4 -> RecognizedShape.TRIANGLE
            !isClosed && simplified.size == 2 -> RecognizedShape.LINE
            else -> null
        }
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

    private fun perpendicularDistance(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (mag == 0f) return hypot((point.x - lineStart.x).toDouble(), (point.y - lineStart.y).toDouble()).toFloat()
        val p = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (mag * mag)
        return if (p < 0) {
            hypot((point.x - lineStart.x).toDouble(), (point.y - lineStart.y).toDouble()).toFloat()
        } else if (p > 1) {
            hypot((point.x - lineEnd.x).toDouble(), (point.y - lineEnd.y).toDouble()).toFloat()
        } else {
            val projX = lineStart.x + p * dx
            val projY = lineStart.y + p * dy
            hypot((point.x - projX).toDouble(), (point.y - projY).toDouble()).toFloat()
        }
    }
}
