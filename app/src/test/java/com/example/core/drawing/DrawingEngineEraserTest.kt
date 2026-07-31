package com.example.core.drawing

import androidx.compose.ui.geometry.Offset
import com.example.data.models.HslaColor
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingEngineEraserTest {

    private fun horizontalStroke(): StrokeEntity = StrokeEntity(
        id = "stroke",
        tool = ToolType.PEN,
        colorHsla = HslaColor.BLACK,
        baseWidth = 4f,
        points = listOf(StrokePoint(0f, 0f), StrokePoint(100f, 0f))
    )

    @Test
    fun `pixel eraser cuts a sparse segment into two rounded chunks`() {
        val chunks = DrawingEngine.eraseStrokeAlongPath(
            horizontalStroke(),
            eraserPoints = listOf(StrokePoint(50f, 0f)),
            radius = 5f
        )

        assertEquals(2, chunks.size)
        assertTrue(chunks[0].points.last().x < 50f)
        assertTrue(chunks[1].points.first().x > 50f)
        assertEquals("stroke", chunks.first().id)
        assertTrue(chunks.all { it.points.size > 2 })
    }

    @Test
    fun `swept eraser cuts between pointer samples`() {
        val chunks = DrawingEngine.eraseStrokeAlongPath(
            horizontalStroke().copy(points = listOf(StrokePoint(0f, 50f), StrokePoint(100f, 50f))),
            eraserPoints = listOf(StrokePoint(50f, 0f), StrokePoint(50f, 100f)),
            radius = 4f
        )

        assertEquals(2, chunks.size)
    }

    @Test
    fun `non intersecting eraser keeps original stroke data`() {
        val original = horizontalStroke()
        val result = DrawingEngine.eraseStrokeAlongPath(
            original,
            eraserPoints = listOf(StrokePoint(50f, 50f)),
            radius = 4f
        )

        assertEquals(listOf(original), result)
    }

    @Test
    fun `object hit test checks the segment not just stored points`() {
        assertTrue(DrawingEngine.isPointInStroke(Offset(50f, 1f), horizontalStroke(), radius = 2f))
    }
}
