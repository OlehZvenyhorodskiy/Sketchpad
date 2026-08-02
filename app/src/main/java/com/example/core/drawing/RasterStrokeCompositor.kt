package com.example.core.drawing

import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.ui.graphics.asAndroidPath
import com.example.data.models.EraserMark
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType

/**
 * The single raster compositing contract for ink and pixel erasing.
 *
 * Strokes remain lightweight, editable input data, but the visible result is always drawn into a
 * Canvas layer before it reaches the screen or an export bitmap. A pixel eraser therefore clears
 * only covered raster pixels; it never rewrites a StrokeEntity into smaller centre-line chunks.
 */
object RasterStrokeCompositor {

    private const val MAX_RASTER_EDGE = 2048
    private val rasterCache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private data class RasterBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    fun drawStroke(
        canvas: Canvas,
        stroke: StrokeEntity,
        layerAlpha: Float = 1f,
        scale: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f
    ) {
        if (stroke.points.isEmpty()) return
        val path = DrawingEngine.createSmoothPath(stroke.points, scale, panX, panY).asAndroidPath()
        val width = DrawingEngine.strokeRenderWidth(stroke.tool, stroke.baseWidth, scale)
        val alpha = DrawingEngine.strokeRenderAlpha(stroke.tool, stroke.colorHsla.alpha, layerAlpha)

        fun pass(
            widthMultiplier: Float,
            alphaMultiplier: Float = 1f,
            squareCap: Boolean = false,
            dash: DashPathEffect? = null
        ) {
            val passWidth = width * widthMultiplier
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = passWidth
                strokeCap = if (squareCap) Paint.Cap.SQUARE else Paint.Cap.BUTT
                strokeJoin = Paint.Join.ROUND
                pathEffect = dash
                color = stroke.colorHsla.copy(alpha = alpha * alphaMultiplier).toArgbInt()
            }
            canvas.drawPath(path, paint)
            if (!squareCap) {
                val capPaint = Paint(paint).apply {
                    style = Paint.Style.FILL
                    pathEffect = null
                }
                val radius = passWidth / 2f
                if (stroke.startCapRound) {
                    val first = stroke.points.first()
                    canvas.drawCircle(first.x * scale + panX, first.y * scale + panY, radius, capPaint)
                }
                if (stroke.endCapRound && stroke.points.size > 1) {
                    val last = stroke.points.last()
                    canvas.drawCircle(last.x * scale + panX, last.y * scale + panY, radius, capPaint)
                }
            }
        }

        when (stroke.tool) {
            ToolType.PENCIL -> {
                pass(1.65f, 0.22f)
                pass(0.72f)
            }
            ToolType.AIRBRUSH -> {
                pass(1.75f, 0.18f)
                pass(1.25f, 0.35f)
                pass(0.72f)
            }
            ToolType.CRAYON -> {
                pass(1.2f, 0.34f)
                pass(0.78f, squareCap = true, dash = DashPathEffect(floatArrayOf(4f, 1.5f), 0f))
            }
            ToolType.WATERCOLOR_BRUSH -> {
                pass(1.35f, 0.25f)
                pass(1f, 0.55f)
                pass(0.58f)
            }
            ToolType.LASER -> {
                pass(2.2f, 0.4f)
                pass(1f)
            }
            ToolType.MARKER,
            ToolType.FOUNTAIN_PEN -> pass(1f, squareCap = true)
            else -> pass(1f)
        }
    }

    /** Draws a continuous hard-edged swept circle in logical canvas pixels. */
    fun clearMask(
        canvas: Canvas,
        mark: EraserMark,
        scale: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f
    ) = clearPoints(canvas, mark.points, mark.width, scale, panX, panY)

    fun clearPoints(
        canvas: Canvas,
        points: List<StrokePoint>,
        width: Float,
        scale: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f
    ) {
        if (points.isEmpty() || width <= 0f) return
        val paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.STROKE
            strokeWidth = width * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        if (points.size == 1) {
            paint.style = Paint.Style.FILL
            val point = points.first()
            canvas.drawCircle(
                point.x * scale + panX,
                point.y * scale + panY,
                width * scale / 2f,
                paint
            )
            return
        }
        val path = Path().apply {
            val first = points.first()
            moveTo(first.x * scale + panX, first.y * scale + panY)
            points.drop(1).forEach { point -> lineTo(point.x * scale + panX, point.y * scale + panY) }
        }
        canvas.drawPath(path, paint)
    }

    fun drawMaskedStroke(
        canvas: Canvas,
        stroke: StrokeEntity,
        masks: List<EraserMark>,
        layerAlpha: Float = 1f,
        scale: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f
    ) {
        if (masks.isEmpty()) {
            drawStroke(canvas, stroke, layerAlpha, scale, panX, panY)
            return
        }
        val saved = canvas.saveLayer(null, null)
        drawStroke(canvas, stroke, layerAlpha, scale, panX, panY)
        masks.forEach { clearMask(canvas, it, scale, panX, panY) }
        canvas.restoreToCount(saved)
    }

    /**
     * Draws a logical-pixel bitmap rather than recreating the stroke at the viewport scale.
     * Zooming therefore magnifies the same raster cells (with filtering disabled), exactly like a
     * Paint canvas, while the vector input remains available for editing and undo.
     */
    fun drawRasterStroke(
        canvas: Canvas,
        stroke: StrokeEntity,
        masks: List<EraserMark>,
        layerAlpha: Float = 1f,
        scale: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f,
        useCache: Boolean = false
    ) {
        drawMaskedStroke(canvas, stroke, masks, layerAlpha, scale, panX, panY)
    }

    private fun logicalRasterBounds(stroke: StrokeEntity): RasterBounds? {
        if (stroke.points.isEmpty()) return null
        val maximumPassMultiplier = when (stroke.tool) {
            ToolType.PENCIL -> 1.65f
            ToolType.AIRBRUSH -> 1.75f
            ToolType.CRAYON -> 1.2f
            ToolType.WATERCOLOR_BRUSH -> 1.35f
            ToolType.LASER -> 2.2f
            else -> 1f
        }
        val padding = DrawingEngine.strokeRenderWidth(stroke.tool, stroke.baseWidth) * maximumPassMultiplier / 2f + 2f
        val left = kotlin.math.floor(stroke.points.minOf { it.x } - padding).toInt()
        val top = kotlin.math.floor(stroke.points.minOf { it.y } - padding).toInt()
        val right = kotlin.math.ceil(stroke.points.maxOf { it.x } + padding).toInt().coerceAtLeast(left + 1)
        val bottom = kotlin.math.ceil(stroke.points.maxOf { it.y } + padding).toInt().coerceAtLeast(top + 1)
        return RasterBounds(left, top, right, bottom)
    }
}
