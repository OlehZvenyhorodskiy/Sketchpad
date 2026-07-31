package com.example.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.models.HslaColor
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LassoSelectionTest {

    @Test
    fun `isPointInPolygon identifies point inside polygon`() {
        val polygon = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f)
        )

        assertTrue(isPointInPolygon(Offset(50f, 50f), polygon))
        assertFalse(isPointInPolygon(Offset(150f, 50f), polygon))
        assertFalse(isPointInPolygon(Offset(-10f, 50f), polygon))
    }

    @Test
    fun `lasso selects an element when only part of its bounds intersects`() {
        val lasso = listOf(
            Offset(90f, 90f),
            Offset(140f, 90f),
            Offset(140f, 140f),
            Offset(90f, 140f)
        )

        assertTrue(doesRectIntersectPolygon(Rect(0f, 0f, 120f, 120f), lasso))
        assertFalse(doesRectIntersectPolygon(Rect(0f, 0f, 80f, 80f), lasso))
    }

    @Test
    fun `lasso selects a sparse stroke when it crosses between stored points`() {
        val stroke = StrokeEntity(
            id = "stroke",
            tool = ToolType.PEN,
            colorHsla = HslaColor.BLACK,
            baseWidth = 3f,
            points = listOf(StrokePoint(0f, 50f), StrokePoint(200f, 50f))
        )
        val lasso = listOf(
            Offset(90f, 40f), Offset(110f, 40f),
            Offset(110f, 60f), Offset(90f, 60f)
        )

        assertTrue(doesStrokeIntersectPolygon(stroke, lasso))
    }
}
