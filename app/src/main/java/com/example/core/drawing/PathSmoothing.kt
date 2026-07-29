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
        segments: Int = 8,
        scale: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f
    ): Path {
        val path = Path()
        if (points.size < 2) {
            if (points.size == 1) {
                val x0 = points[0].x * scale + panX
                val y0 = points[0].y * scale + panY
                path.moveTo(x0, y0)
                path.lineTo(x0 + 0.1f, y0 + 0.1f)
            }
            return path
        }

        val x0 = points[0].x * scale + panX
        val y0 = points[0].y * scale + panY
        val x1 = points[1].x * scale + panX
        val y1 = points[1].y * scale + panY

        if (points.size == 2) {
            path.moveTo(x0, y0)
            path.lineTo(x1, y1)
            return path
        }

        path.moveTo(x0, y0)

        for (i in 0 until points.size - 1) {
            val p0 = points.getOrElse(i - 1) { points[i] }
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points.getOrElse(i + 2) { points[i + 1] }

            val p0x = p0.x * scale + panX
            val p0y = p0.y * scale + panY
            val p1x = p1.x * scale + panX
            val p1y = p1.y * scale + panY
            val p2x = p2.x * scale + panX
            val p2y = p2.y * scale + panY
            val p3x = p3.x * scale + panX
            val p3y = p3.y * scale + panY

            for (t in 1..segments) {
                val tt = t.toFloat() / segments
                val tt2 = tt * tt
                val tt3 = tt2 * tt

                val h00 = 2f * tt3 - 3f * tt2 + 1f
                val h10 = tt3 - 2f * tt2 + tt
                val h01 = -2f * tt3 + 3f * tt2
                val h11 = tt3 - tt2

                val m1x = tension * (p2x - p0x) * 0.5f
                val m1y = tension * (p2y - p0y) * 0.5f
                val m2x = tension * (p3x - p1x) * 0.5f
                val m2y = tension * (p3y - p1y) * 0.5f

                val x = h00 * p1x + h10 * m1x + h01 * p2x + h11 * m2x
                val y = h00 * p1y + h10 * m1y + h01 * p2y + h11 * m2y

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
