package com.example.ui.editor

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LassoSelectionTest {

    private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
        var inside = false
        val n = polygon.size
        var j = n - 1
        for (i in 0 until n) {
            val yi = polygon[i].y; val yj = polygon[j].y
            val xi = polygon[i].x; val xj = polygon[j].x
            if ((yi > point.y) != (yj > point.y) &&
                point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

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
}
