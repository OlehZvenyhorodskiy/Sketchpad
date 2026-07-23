package com.example.academic

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.example.data.models.StrokeEntity

object WatercolorBleedEngine {

    fun applyWatercolorBleed(bitmap: Bitmap, diffusionCoeff: Float = 0.15f, iterations: Int = 3): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        val tempPixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        System.arraycopy(pixels, 0, tempPixels, 0, pixels.size)

        for (iter in 0 until iterations) {
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val idx = y * width + x
                    val origCol = tempPixels[idx]
                    val aOrig = Color.alpha(origCol)
                    if (aOrig < 5) continue

                    val top = tempPixels[(y - 1) * width + x]
                    val bot = tempPixels[(y + 1) * width + x]
                    val left = tempPixels[y * width + (x - 1)]
                    val right = tempPixels[y * width + (x + 1)]

                    val avgR = (Color.red(top) + Color.red(bot) + Color.red(left) + Color.red(right)) / 4
                    val avgG = (Color.green(top) + Color.green(bot) + Color.green(left) + Color.green(right)) / 4
                    val avgB = (Color.blue(top) + Color.blue(bot) + Color.blue(left) + Color.blue(right)) / 4
                    val avgA = (Color.alpha(top) + Color.alpha(bot) + Color.alpha(left) + Color.alpha(right)) / 4

                    val rNew = (Color.red(origCol) * (1f - diffusionCoeff) + avgR * diffusionCoeff).toInt().coerceIn(0, 255)
                    val gNew = (Color.green(origCol) * (1f - diffusionCoeff) + avgG * diffusionCoeff).toInt().coerceIn(0, 255)
                    val bNew = (Color.blue(origCol) * (1f - diffusionCoeff) + avgB * diffusionCoeff).toInt().coerceIn(0, 255)
                    val aNew = (aOrig * (1f - diffusionCoeff * 0.5f) + avgA * diffusionCoeff * 0.5f).toInt().coerceIn(0, 255)

                    pixels[idx] = Color.argb(aNew, rNew, gNew, bNew)
                }
            }
            System.arraycopy(pixels, 0, tempPixels, 0, pixels.size)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
