package com.example.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.example.data.models.LayerEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType

/**
 * Кешує відрендерені шари у Bitmap.
 * Перемальовує лише при зміні вмісту шару.
 * Active stroke рендериться поверх кешу без кешування.
 */
class CanvasRenderCache {

    private val layerBitmaps = mutableMapOf<String, Bitmap>()
    private val layerDirtyFlags = mutableMapOf<String, Boolean>()

    fun invalidateLayer(layerId: String) {
        layerDirtyFlags[layerId] = true
    }

    fun invalidateAll() {
        layerDirtyFlags.keys.forEach { layerDirtyFlags[it] = true }
    }

    fun getOrCreateLayerBitmap(
        layer: LayerEntity,
        width: Int,
        height: Int,
        scale: Float,
        panX: Float,
        panY: Float,
        renderStroke: (Canvas, List<StrokePoint>, Float, ToolType, Int) -> Unit
    ): Bitmap {
        val isDirty = layerDirtyFlags[layer.id] ?: true
        val existing = layerBitmaps[layer.id]

        if (!isDirty && existing != null && existing.width == width && existing.height == height) {
            return existing
        }

        // Створюємо або перестворюємо bitmap
        val bitmap = if (existing != null && existing.width == width && existing.height == height) {
            existing.apply { eraseColor(0) }
        } else {
            existing?.recycle()
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        val layerAlpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()

        // Рендеримо всі strokes шару
        layer.strokes.forEach { stroke ->
            val scaledPoints = stroke.points.map { p ->
                StrokePoint(
                    x = p.x * scale + panX,
                    y = p.y * scale + panY,
                    pressure = p.pressure,
                    tilt = p.tilt
                )
            }
            renderStroke(canvas, scaledPoints, scale, stroke.tool, layerAlpha)
        }

        layerBitmaps[layer.id] = bitmap
        layerDirtyFlags[layer.id] = false
        return bitmap
    }

    fun removeLayer(layerId: String) {
        layerBitmaps.remove(layerId)?.recycle()
        layerDirtyFlags.remove(layerId)
    }

    fun clear() {
        layerBitmaps.values.forEach { it.recycle() }
        layerBitmaps.clear()
        layerDirtyFlags.clear()
    }
}
