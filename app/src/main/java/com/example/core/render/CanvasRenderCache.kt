package com.example.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import com.example.data.models.LayerEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType

/**
 * Кешує відрендерені шари у Bitmap з обмежуванням пам'яті LRU і безпечною очисткою.
 */
class CanvasRenderCache {

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()

    private val lruCache = object : LruCache<String, Bitmap>(maxMemoryKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

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
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)

        val isDirty = layerDirtyFlags[layer.id] ?: true
        val existing = lruCache.get(layer.id)?.takeIf { !it.isRecycled }

        if (!isDirty && existing != null && existing.width == safeWidth && existing.height == safeHeight) {
            return existing
        }

        val bitmap = if (existing != null && existing.width == safeWidth && existing.height == safeHeight) {
            existing.apply { eraseColor(0) }
        } else {
            Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        val layerAlpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()

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

        lruCache.put(layer.id, bitmap)
        layerDirtyFlags[layer.id] = false
        return bitmap
    }

    fun removeLayer(layerId: String) {
        lruCache.remove(layerId)?.let {
            if (!it.isRecycled) it.recycle()
        }
        layerDirtyFlags.remove(layerId)
    }

    fun clear() {
        lruCache.evictAll()
        layerDirtyFlags.clear()
    }
}
