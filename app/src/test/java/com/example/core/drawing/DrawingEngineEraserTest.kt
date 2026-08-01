package com.example.core.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.example.data.models.EraserMark
import com.example.data.models.HslaColor
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DrawingEngineEraserTest {

    private fun horizontalStroke(): StrokeEntity = StrokeEntity(
        id = "stroke",
        tool = ToolType.PEN,
        colorHsla = HslaColor.BLACK,
        baseWidth = 4f,
        points = listOf(StrokePoint(0f, 0f), StrokePoint(100f, 0f))
    )

    @Test
    fun `two pixel raster mask cuts inside every thick brush without splitting its stroke`() {
        val tools = listOf(
            ToolType.PEN,
            ToolType.PENCIL,
            ToolType.INK_PEN,
            ToolType.FOUNTAIN_PEN,
            ToolType.MARKER,
            ToolType.AIRBRUSH,
            ToolType.CRAYON,
            ToolType.WATERCOLOR_BRUSH,
            ToolType.LASER
        )
        tools.forEach { tool ->
            val stroke = horizontalStroke().copy(tool = tool, baseWidth = 20f, points = listOf(
                StrokePoint(8f, 32f), StrokePoint(120f, 32f)
            ))
            val mark = EraserMark(
                points = listOf(StrokePoint(28f, 32f), StrokePoint(100f, 32f)),
                width = 2f,
                affectedStrokeIds = listOf(stroke.id)
            )
            val bitmap = Bitmap.createBitmap(128, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            RasterStrokeCompositor.drawRasterStroke(canvas, stroke, listOf(mark))

            assertEquals("$tool must clear the 2 px centre corridor", 0, Color.alpha(bitmap.getPixel(64, 32)))
            assertTrue("$tool must retain ink above the erased corridor", Color.alpha(bitmap.getPixel(64, 25)) > 0)
            assertEquals("$tool must preserve the source stroke id", "stroke", stroke.id)
            assertEquals("$tool must preserve source control points", 2, stroke.points.size)
        }
    }

    @Test
    fun `new ink remains visible over an older erased corridor`() {
        val bitmap = Bitmap.createBitmap(128, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val oldStroke = horizontalStroke().copy(baseWidth = 20f, points = listOf(StrokePoint(8f, 32f), StrokePoint(120f, 32f)))
        val mask = EraserMark(
            points = listOf(StrokePoint(28f, 32f), StrokePoint(100f, 32f)),
            width = 2f,
            affectedStrokeIds = listOf(oldStroke.id)
        )
        RasterStrokeCompositor.drawRasterStroke(canvas, oldStroke, listOf(mask))
        val newerStroke = oldStroke.copy(
            id = "newer",
            colorHsla = HslaColor(0f, 1f, 0.5f, 1f),
            baseWidth = 2f
        )
        RasterStrokeCompositor.drawRasterStroke(canvas, newerStroke, emptyList())

        assertTrue(Color.alpha(bitmap.getPixel(64, 32)) > 0)
        assertTrue("newer red stroke must not be cleared by an older mark", Color.red(bitmap.getPixel(64, 32)) > 0)
    }

    @Test
    fun `fast swept eraser hit is retained even when neither sampled endpoint touches ink`() {
        val stroke = horizontalStroke().copy(points = listOf(StrokePoint(8f, 32f), StrokePoint(120f, 32f)))

        assertTrue(
            DrawingEngine.doesEraserPathAffectStroke(
                eraserPoints = listOf(StrokePoint(64f, 0f), StrokePoint(64f, 64f)),
                eraserWidth = 2f,
                stroke = stroke
            )
        )
    }

    @Test
    fun `two pixel eraser can target the visible outer edge of a wide brush`() {
        val airbrush = horizontalStroke().copy(
            tool = ToolType.AIRBRUSH,
            baseWidth = 4f,
            points = listOf(StrokePoint(8f, 32f), StrokePoint(120f, 32f))
        )

        assertTrue(
            DrawingEngine.doesEraserPathAffectStroke(
                eraserPoints = listOf(StrokePoint(40f, 42f), StrokePoint(90f, 42f)),
                eraserWidth = 2f,
                stroke = airbrush
            )
        )
    }

    @Test
    fun `object hit test checks the segment not just stored points`() {
        assertTrue(DrawingEngine.isPointInStroke(Offset(50f, 1f), horizontalStroke(), radius = 2f))
    }

    @Test
    fun `ruler catches a fast stylus crossing at its first physical edge`() {
        val ruler = RulerState(
            isVisible = true,
            center = Offset(100f, 100f),
            angleRad = 0f,
            length = 200f,
            width = 40f
        )

        val contact = ruler.edgeContact(
            previousPoint = Offset(100f, 180f),
            currentPoint = Offset(100f, 20f),
            contactZone = 4f
        )

        assertTrue(contact != null)
        assertEquals(120f, contact!!.first.y, 0.01f)
    }

    @Test
    fun `eraser control size maps to one exact preview and mask diameter`() {
        assertEquals(8f, DrawingEngine.eraserDiameter(8f), 0.001f)
        assertEquals(1f, DrawingEngine.eraserDiameter(1f), 0.001f)
    }
}
