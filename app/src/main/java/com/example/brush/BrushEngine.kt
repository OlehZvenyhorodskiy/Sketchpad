package com.example.brush

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.core.drawing.PathSmoothing
import com.example.data.models.StrokePoint
import java.io.File
import kotlin.random.Random

/**
 * Brush rendering engine supporting pressure, tilt, jitter, scatter, and texture stamp.
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

        // 1. Texture Stamp Rendering (if texturePath exists and is valid)
        if (!brush.texturePath.isNullOrBlank() && File(brush.texturePath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(brush.texturePath)
                if (bitmap != null) {
                    val stampPaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                        alpha = (brush.opacity * 255).toInt()
                    }
                    val stampSize = brush.baseWidth * scale * 2.5f
                    points.forEach { p ->
                        val dstRect = RectF(
                            p.x - stampSize / 2f,
                            p.y - stampSize / 2f,
                            p.x + stampSize / 2f,
                            p.y + stampSize / 2f
                        )
                        canvas.drawBitmap(bitmap, null, dstRect, stampPaint)
                    }
                    return
                }
            } catch (_: Exception) {}
        }

        // 2. Standard Path Rendering
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

        // Smoothing
        val smoothedPoints = PathSmoothing.adaptiveSimplify(points, epsilon = 1.5f / scale)
        val path = PathSmoothing.createCatmullRomPath(smoothedPoints, tension = brush.smoothing)

        // Pressure-based width
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

        // Scatter (spray effect)
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
