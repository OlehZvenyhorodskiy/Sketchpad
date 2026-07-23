package com.example.academic

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.floor

data class CanvasTileKey(val tileX: Int, val tileY: Int)

class PagedCanvasEngine(
    val tileSizePx: Int = 2048,
    val maxCachedTiles: Int = 16
) {
    private val tileCache = mutableMapOf<CanvasTileKey, Bitmap>()

    fun getVisibleTileKeys(viewportSize: Size, panOffset: Offset, zoomScale: Float): List<CanvasTileKey> {
        val visibleMinX = -panOffset.x / zoomScale
        val visibleMaxX = (viewportSize.width - panOffset.x) / zoomScale
        val visibleMinY = -panOffset.y / zoomScale
        val visibleMaxY = (viewportSize.height - panOffset.y) / zoomScale

        val minTileX = floor(visibleMinX / tileSizePx).toInt()
        val maxTileX = floor(visibleMaxX / tileSizePx).toInt()
        val minTileY = floor(visibleMinY / tileSizePx).toInt()
        val maxTileY = floor(visibleMaxY / tileSizePx).toInt()

        val keys = mutableListOf<CanvasTileKey>()
        for (tx in minTileX..maxTileX) {
            for (ty in minTileY..maxTileY) {
                keys.add(CanvasTileKey(tx, ty))
            }
        }
        return keys
    }

    fun getOrAllocateTile(key: CanvasTileKey): Bitmap {
        return tileCache.getOrPut(key) {
            if (tileCache.size >= maxCachedTiles) {
                // Recycle oldest key
                val oldestKey = tileCache.keys.firstOrNull()
                oldestKey?.let {
                    val bmp = tileCache.remove(it)
                    if (bmp != null && !bmp.isRecycled) {
                        bmp.recycle()
                    }
                }
            }
            Bitmap.createBitmap(tileSizePx / 2, tileSizePx / 2, Bitmap.Config.ARGB_8888)
        }
    }

    fun clearCache() {
        tileCache.values.forEach { bmp ->
            if (!bmp.isRecycled) bmp.recycle()
        }
        tileCache.clear()
    }
}
