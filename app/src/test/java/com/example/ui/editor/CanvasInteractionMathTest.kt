package com.example.ui.editor

import androidx.compose.ui.geometry.Offset
import com.example.data.models.ChartElementEntity
import com.example.data.models.EraserMark
import com.example.data.models.HslaColor
import com.example.data.models.LayerEntity
import com.example.data.models.PageEntity
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import com.example.data.models.isAttachedToChart
import com.example.data.models.resizeFramePreservingOrigin
import com.example.data.models.squarePixelsPerUnit
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
    fun `every unrotated corner keeps its diagonal corner fixed`() {
        val originalPosition = Offset(100f, 200f)
        val originalSize = Offset(160f, 120f)
        val cases = listOf(
            "TL" to Offset(20f, 10f),
            "TR" to Offset(-20f, 10f),
            "BL" to Offset(20f, -10f),
            "BR" to Offset(-20f, -10f)
        )

        cases.forEach { (corner, delta) ->
            val result = calculateResizeTransform(
                id = "chart",
                type = "CHART",
                originalPosition = originalPosition,
                originalSize = originalSize,
                rotationDegrees = 0f,
                dragDelta = delta,
                corner = corner,
                minimumWidth = 100f,
                minimumHeight = 100f
            )
            when (corner) {
                "TL" -> {
                    assertEquals(originalPosition.x + originalSize.x, result.x + result.width, 0.001f)
                    assertEquals(originalPosition.y + originalSize.y, result.y + result.height, 0.001f)
                }
                "TR" -> {
                    assertEquals(originalPosition.x, result.x, 0.001f)
                    assertEquals(originalPosition.y + originalSize.y, result.y + result.height, 0.001f)
                }
                "BL" -> {
                    assertEquals(originalPosition.x + originalSize.x, result.x + result.width, 0.001f)
                    assertEquals(originalPosition.y, result.y, 0.001f)
                }
                "BR" -> {
                    assertEquals(originalPosition.x, result.x, 0.001f)
                    assertEquals(originalPosition.y, result.y, 0.001f)
                }
            }
        }
    }

    @Test
    fun `legacy chart normalises square cells while resize keeps global origin fixed`() {
        val chart = ChartElementEntity(
            x = 100f,
            y = 200f,
            width = 380f,
            height = 260f,
            pixelsPerUnitX = 19f,
            pixelsPerUnitY = 13f,
            originOffsetX = 190f,
            originOffsetY = 130f
        )
        val resized = chart.resizeFramePreservingOrigin(
            newX = 60f,
            newY = 160f,
            newWidth = 500f,
            newHeight = 340f
        )

        assertEquals(13f, chart.squarePixelsPerUnit(), 0.001f)
        assertEquals(13f, resized.pixelsPerUnitX, 0.001f)
        assertEquals(13f, resized.pixelsPerUnitY, 0.001f)
        assertEquals(chart.x + chart.originOffsetX, resized.x + resized.originOffsetX, 0.001f)
        assertEquals(chart.y + chart.originOffsetY, resized.y + resized.originOffsetY, 0.001f)
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

    @Test
    fun `legacy chart attachment requires every saved point to be inside the graph`() {
        val chart = ChartElementEntity(id = "chart", x = 100f, y = 100f, width = 100f, height = 100f)
        val inside = StrokeEntity(
            id = "inside",
            tool = ToolType.PEN,
            colorHsla = HslaColor.BLACK,
            baseWidth = 2f,
            points = listOf(StrokePoint(110f, 110f), StrokePoint(190f, 190f))
        )
        val grazing = StrokeEntity(
            id = "grazing",
            tool = ToolType.PEN,
            colorHsla = HslaColor.BLACK,
            baseWidth = 2f,
            points = listOf(StrokePoint(50f, 150f), StrokePoint(150f, 150f))
        )

        assertTrue(inside.isAttachedToChart(chart))
        assertTrue(!grazing.isAttachedToChart(chart))
    }

    @Test
    fun `stale layer selection falls back to a visible writable layer`() {
        val page = PageEntity(
            canvasId = "canvas",
            pageIndex = 0,
            activeLayerId = "locked",
            layers = listOf(
                LayerEntity(id = "locked", isLocked = true),
                LayerEntity(id = "writable")
            )
        )

        assertEquals("writable", resolveWritableLayerId(page, "layer-from-another-page"))
    }

    @Test
    fun `legacy global eraser mark is frozen to existing strokes before new ink`() {
        val existing = StrokeEntity(
            id = "existing",
            tool = ToolType.PEN,
            colorHsla = HslaColor.BLACK,
            baseWidth = 20f,
            points = listOf(StrokePoint(8f, 32f), StrokePoint(120f, 32f))
        )
        val legacyMark = EraserMark(
            id = "legacy",
            points = listOf(StrokePoint(64f, 20f), StrokePoint(64f, 44f)),
            width = 2f,
            affectedStrokeIds = emptyList()
        )
        val page = PageEntity(
            canvasId = "canvas",
            pageIndex = 0,
            layers = listOf(LayerEntity(id = "default", strokes = listOf(existing), eraserMarks = listOf(legacyMark)))
        )

        val normalized = normalizeLegacyEraserMarks(page)
        val mark = normalized.layers.single().eraserMarks.single()
        assertEquals(listOf("existing"), mark.affectedStrokeIds)
        assertTrue("future stroke IDs cannot be affected by the legacy mark", "future" !in mark.affectedStrokeIds)
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
