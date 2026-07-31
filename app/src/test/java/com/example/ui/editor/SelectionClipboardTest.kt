package com.example.ui.editor

import com.example.data.models.ImageElementEntity
import com.example.data.models.LayerEntity
import com.example.data.models.PageEntity
import com.example.data.models.ShapeEntity
import com.example.data.models.ShapeType
import com.example.data.models.HslaColor
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionClipboardTest {

    private val selectedShape = ShapeEntity(
        id = "shape-selected",
        shapeType = ShapeType.SQUARE,
        x = 10f,
        y = 20f,
        width = 100f,
        height = 80f
    )
    private val unselectedImage = ImageElementEntity(
        id = "image-unselected",
        sourceUri = "file:///image.png",
        x = 40f,
        y = 50f
    )
    private val selectedStroke = StrokeEntity(
        id = "stroke-selected",
        tool = ToolType.PEN,
        colorHsla = HslaColor.BLACK,
        baseWidth = 3f,
        points = listOf(StrokePoint(0f, 0f), StrokePoint(20f, 20f))
    )
    private val page = PageEntity(
        canvasId = "canvas",
        pageIndex = 0,
        layers = listOf(
            LayerEntity(
                id = "layer",
                shapes = listOf(selectedShape),
                strokes = listOf(selectedStroke),
                images = listOf(unselectedImage)
            )
        ),
        activeLayerId = "layer"
    )

    @Test
    fun `copy collects only selected elements`() {
        val clipboard = copyElementsFromPage(page, setOf(selectedShape.id))

        assertEquals(1, clipboard.size)
        assertTrue(clipboard.single() is ClipboardElement.Shape)
        assertEquals(selectedShape.id, clipboard.single().id)
    }

    @Test
    fun `delete removes selected elements and keeps the rest`() {
        val updated = deleteElementsFromPage(page, setOf(selectedShape.id))
        val layer = updated.getEffectiveLayers().single()

        assertTrue(layer.shapes.isEmpty())
        assertEquals(listOf(unselectedImage), layer.images)
    }

    @Test
    fun `copy and delete support selected handwritten strokes`() {
        val clipboard = copyElementsFromPage(page, setOf(selectedStroke.id))
        val updated = deleteElementsFromPage(page, setOf(selectedStroke.id))

        assertTrue(clipboard.single() is ClipboardElement.Stroke)
        assertTrue(updated.getEffectiveLayers().single().strokes.isEmpty())
        assertEquals(listOf(selectedShape), updated.getEffectiveLayers().single().shapes)
    }
}
