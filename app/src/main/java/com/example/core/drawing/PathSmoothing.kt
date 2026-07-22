package com.example.core.drawing

import android.graphics.Path
import com.example.data.models.StrokePoint
import kotlin.math.sqrt

/**
 * Catmull-Rom spline interpolation для гладких кривих.
 * Замінює простий quadraticTo на адаптивний spline з pressure interpolation.
 */
object PathSmoothing {

    /**
     * Створює гладкий Path через Catmull-Rom spline.
     * @param points Точки з pressure
     * @param tension 0.0 = м'який, 1.0 = жорсткий (default 0.5)
     * @param segments Кількість інтерполяційних сегментів між точками
     */
    fun createCatmullRomPath(
        points: List<StrokePoint>,
        tension: Float = 0.5f,
        segments: Int = 8
    ): Path {
        val path = Path()
        if (points.size < 2) {
            if (points.size == 1) {
                path.moveTo(points[0].x, points[0].y)
                path.lineTo(points[0].x + 0.1f, points[0].y + 0.1f)
            }
            return path
        }

        if (points.size == 2) {
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        path.moveTo(points[0].x, points[0].y)

        for (i in 0 until points.size - 1) {
            val p0 = points.getOrElse(i - 1) { points[i] }
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points.getOrElse(i + 2) { points[i + 1] }

            for (t in 1..segments) {
                val tt = t.toFloat() / segments
                val tt2 = tt * tt
                val tt3 = tt2 * tt

                val x = 0.5f * (
                    (2f * p1.x) +
                    (-p0.x + p2.x) * tt * tension +
                    (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * tt2 * tension +
                    (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * tt3 * tension
                )
                val y = 0.5f * (
                    (2f * p1.y) +
                    (-p0.y + p2.y) * tt * tension +
                    (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * tt2 * tension +
                    (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * tt3 * tension
                )

                path.lineTo(x, y)
            }
        }

        return path
    }

    /**
     * Адаптивне згладжування: додає проміжні точки там, де кут між
     * сегментами > threshold. Зменшує кількість точок на прямих ділянках.
     */
    fun adaptiveSimplify(points: List<StrokePoint>, epsilon: Float = 2.0f): List<StrokePoint> {
        if (points.size < 3) return points
        return ramerDouglasPeucker(points, epsilon)
    }

    private fun ramerDouglasPeucker(points: List<StrokePoint>, epsilon: Float): List<StrokePoint> {
        if (points.size < 3) return points

        var maxDist = 0f
        var maxIdx = 0
        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIdx = i
            }
        }

        return if (maxDist > epsilon) {
            val left = ramerDouglasPeucker(points.subList(0, maxIdx + 1), epsilon)
            val right = ramerDouglasPeucker(points.subList(maxIdx, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(point: StrokePoint, lineStart: StrokePoint, lineEnd: StrokePoint): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0f) return sqrt((point.x - lineStart.x) * (point.x - lineStart.x) +
                                   (point.y - lineStart.y) * (point.y - lineStart.y))
        return kotlin.math.abs(dy * point.x - dx * point.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / len
    }

    /**
     * Інтерполяція ширини лінії на основі pressure.
     * Повертає список ширин для кожної точки.
     */
    fun interpolateWidths(
        points: List<StrokePoint>,
        baseWidth: Float,
        pressureMin: Float = 0.1f,
        pressureMax: Float = 1.0f,
        widthMultiplier: Float = 1.5f
    ): List<Float> {
        return points.map { p ->
            val normalizedPressure = ((p.pressure - pressureMin) / (pressureMax - pressureMin))
                .coerceIn(0f, 1f)
            baseWidth * (0.3f + normalizedPressure * widthMultiplier)
        }
    }
}
