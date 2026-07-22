package com.example.brush

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.example.core.drawing.PathSmoothing
import com.example.data.models.StrokePoint
import kotlin.random.Random

/**
 * Движок рендеринг пензля з підтримкою pressure, tilt, jitter, scatter.
 */
object BrushEngine {

    fun renderStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        brush: BrushProfile,
        color: Int,
        scale: Float = 1f
    ) {
        if (points.size < 2) return

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
            alpha = (brush.opacity * 255).toInt()
            if (brush.isDashed) {
                pathEffect = android.graphics.DashPathEffect(
                    brush.dashPattern.map { it * scale }.toFloatArray(), 0f
                )
            }
        }

        // Згладжування
        val smoothedPoints = PathSmoothing.adaptiveSimplify(points, epsilon = 1.5f / scale)
        val path = PathSmoothing.createCatmullRomPath(smoothedPoints, tension = brush.smoothing)

        // Pressure-based width: використовуємо середній pressure
        val avgPressure = points.map { it.pressure }.average().toFloat()
        val adjustedPressure = brush.pressureCurve.apply(avgPressure)
        val width = brush.baseWidth * scale *
            (1f - brush.pressureSensitivity + brush.pressureSensitivity * adjustedPressure)

        paint.strokeWidth = width.coerceAtLeast(0.5f)

        // Jitter
        if (brush.jitter > 0f) {
            val jitterPath = Path()
            var first = true
            smoothedPoints.forEach { p ->
                val jx = p.x + (Random.nextFloat() - 0.5f) * brush.jitter * scale
                val jy = p.y + (Random.nextFloat() - 0.5f) * brush.jitter * scale
                if (first) { jitterPath.moveTo(jx, jy); first = false }
                else jitterPath.lineTo(jx, jy)
            }
            canvas.drawPath(jitterPath, paint)
        } else {
            canvas.drawPath(path, paint)
        }

        // Scatter (для spray)
        if (brush.scatter > 0f) {
            val scatterPaint = Paint().apply {
                isAntiAlias = true
                this.color = color
                alpha = (brush.opacity * 0.3f * 255).toInt()
            }
            points.forEach { p ->
                val count = (brush.scatter * 2).toInt()
                repeat(count) {
                    val sx = p.x + (Random.nextFloat() - 0.5f) * brush.scatter * 2 * scale
                    val sy = p.y + (Random.nextFloat() - 0.5f) * brush.scatter * 2 * scale
                    val radius = Random.nextFloat() * 1.5f * scale
                    canvas.drawCircle(sx, sy, radius, scatterPaint)
                }
            }
        }
    }
}
