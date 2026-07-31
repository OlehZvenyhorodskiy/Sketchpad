package com.example.ui.editor

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CanvasInteractionMathTest {

    @Test
    fun `top left resize keeps opposite corner fixed`() {
        val result = calculateResizeTransform(
            id = "shape",
            type = "SHAPE",
            originalPosition = Offset(0f, 0f),
            originalSize = Offset(100f, 100f),
            rotationDegrees = 0f,
            dragDelta = Offset(20f, 10f),
            corner = "TL",
            minimumWidth = 30f,
            minimumHeight = 30f
        )

        assertEquals(20f, result.x, 0.001f)
        assertEquals(10f, result.y, 0.001f)
        assertEquals(80f, result.width, 0.001f)
        assertEquals(90f, result.height, 0.001f)
        assertEquals(100f, result.x + result.width, 0.001f)
        assertEquals(100f, result.y + result.height, 0.001f)
    }

    @Test
    fun `rotated bottom right resize keeps top left world corner fixed`() {
        val originalPosition = Offset(20f, 40f)
        val originalSize = Offset(120f, 80f)
        val rotation = 35f
        val result = calculateResizeTransform(
            id = "image",
            type = "IMAGE",
            originalPosition = originalPosition,
            originalSize = originalSize,
            rotationDegrees = rotation,
            dragDelta = Offset(30f, 25f),
            corner = "BR",
            minimumWidth = 50f,
            minimumHeight = 50f
        )

        val oldCorner = rotatedCorner(originalPosition, originalSize, rotation, left = true, top = true)
        val newCorner = rotatedCorner(Offset(result.x, result.y), Offset(result.width, result.height), rotation, left = true, top = true)
        assertEquals(oldCorner.x, newCorner.x, 0.001f)
        assertEquals(oldCorner.y, newCorner.y, 0.001f)
    }

    @Test
    fun `axis ticks include every integer without float accumulation gaps`() {
        val ticks = axisTickValues(-10f, 10f, 1f)

        assertEquals(21, ticks.size)
        assertEquals((-10..10).map(Int::toFloat), ticks)
        assertTrue(0f in ticks)
    }

    @Test
    fun `decimal axis ticks stay symmetric and include zero`() {
        val ticks = axisTickValues(-1f, 1f, 0.2f)

        assertEquals(11, ticks.size)
        assertEquals(-1f, ticks.first(), 0.0001f)
        assertEquals(1f, ticks.last(), 0.0001f)
        assertTrue(ticks.any { kotlin.math.abs(it) < 0.0001f })
    }

    private fun rotatedCorner(
        position: Offset,
        size: Offset,
        rotationDegrees: Float,
        left: Boolean,
        top: Boolean
    ): Offset {
        val radians = Math.toRadians(rotationDegrees.toDouble())
        val localX = if (left) -size.x / 2f else size.x / 2f
        val localY = if (top) -size.y / 2f else size.y / 2f
        val center = Offset(position.x + size.x / 2f, position.y + size.y / 2f)
        return Offset(
            center.x + localX * cos(radians).toFloat() - localY * sin(radians).toFloat(),
            center.y + localX * sin(radians).toFloat() + localY * cos(radians).toFloat()
        )
    }
}
