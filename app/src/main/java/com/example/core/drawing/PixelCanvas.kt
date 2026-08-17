package com.example.core.drawing

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.models.StrokePoint
import java.util.ArrayDeque

/**
 * Растрова модель полотна PixelCanvas.
 * Представляє полотно у вигляді піксельного буфера (RGBA/ARGB).
 * Підтримує точні піксельні операції: Flood Fill (заливка), піпетку, стирання з прозорістю
 * та швидкий рендеринг у Bitmap.
 */
class PixelCanvas(
    val width: Int = 1920,
    val height: Int = 1080
) {
    private val pixelData: IntArray = IntArray(width * height) { Color.TRANSPARENT }
    private var cachedBitmap: Bitmap? = null
    private var isDirty: Boolean = true

    fun setPixel(x: Int, y: Int, color: Int) {
        if (x in 0 until width && y in 0 until height) {
            val index = y * width + x
            if (pixelData[index] != color) {
                pixelData[index] = color
                isDirty = true
            }
        }
    }

    fun getPixel(x: Int, y: Int): Int {
        return if (x in 0 until width && y in 0 until height) {
            pixelData[y * width + x]
        } else {
            Color.TRANSPARENT
        }
    }

    /**
     * Алгоритм Flood Fill (ітеративне заповнення зв'язаної області з використанням черги).
     */
    fun floodFill(startX: Int, startY: Int, fillColor: Int): Boolean {
        if (startX !in 0 until width || startY !in 0 until height) return false
        val targetColor = getPixel(startX, startY)
        if (targetColor == fillColor) return false

        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startX, startY))
        setPixel(startX, startY, fillColor)

        while (queue.isNotEmpty()) {
            val (x, y) = queue.poll() ?: continue

            // 4-зв'язані сусіди
            val neighbors = arrayOf(
                Pair(x + 1, y),
                Pair(x - 1, y),
                Pair(x, y + 1),
                Pair(x, y - 1)
            )

            for (n in neighbors) {
                val nx = n.first
                val ny = n.second
                if (nx in 0 until width && ny in 0 until height) {
                    if (getPixel(nx, ny) == targetColor) {
                        setPixel(nx, ny, fillColor)
                        queue.add(n)
                    }
                }
            }
        }
        isDirty = true
        return true
    }

    /**
     * Малювання штриха у вигляді растрових піксельних кіл уздовж ліній.
     */
    fun drawPixelStroke(points: List<StrokePoint>, color: Int, radius: Float) {
        if (points.isEmpty()) return
        val radInt = radius.coerceAtLeast(1f).toInt()
        val radSq = radius * radius

        for (i in points.indices) {
            val p1 = points[i]
            val p0 = if (i > 0) points[i - 1] else p1
            val distance = Math.hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
            val steps = Math.max(1, (distance / (radius * 0.5f)).toInt())

            for (step in 0..steps) {
                val t = step.toFloat() / steps
                val cx = (p0.x + (p1.x - p0.x) * t).toInt()
                val cy = (p0.y + (p1.y - p0.y) * t).toInt()

                for (dx in -radInt..radInt) {
                    for (dy in -radInt..radInt) {
                        if (dx * dx + dy * dy <= radSq) {
                            setPixel(cx + dx, cy + dy, color)
                        }
                    }
                }
            }
        }
        isDirty = true
    }

    /**
     * Точне стирання пікселів (запис прозорого кольору Color.TRANSPARENT).
     */
    fun erasePixelArea(centerX: Int, centerY: Int, radius: Float) {
        val radInt = radius.coerceAtLeast(1f).toInt()
        val radSq = radius * radius
        for (dx in -radInt..radInt) {
            for (dy in -radInt..radInt) {
                if (dx * dx + dy * dy <= radSq) {
                    setPixel(centerX + dx, centerY + dy, Color.TRANSPARENT)
                }
            }
        }
        isDirty = true
    }

    /**
     * Очищення всього растрового полотна.
     */
    fun clear() {
        pixelData.fill(Color.TRANSPARENT)
        isDirty = true
    }

    /**
     * Конвертація растрових даних у Bitmap для швидкового рендерингу.
     */
    fun getBitmap(): Bitmap {
        var bmp = cachedBitmap
        if (bmp == null || bmp.width != width || bmp.height != height) {
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            cachedBitmap = bmp
            isDirty = true
        }
        if (isDirty) {
            bmp.setPixels(pixelData, 0, width, 0, 0, width, height)
            isDirty = false
        }
        return bmp
    }
}
