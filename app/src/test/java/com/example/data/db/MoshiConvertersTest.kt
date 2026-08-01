package com.example.data.db

import com.example.data.models.HslaColor
import com.example.data.models.LayerEntity
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MoshiConvertersTest {

    private val converters = MoshiConverters()

    @Test
    fun `test LayerEntity serialization and deserialization with StrokeEntity`() {
        val stroke = StrokeEntity(
            id = "test-stroke-1",
            tool = ToolType.PEN,
            colorHsla = HslaColor(120f, 0.5f, 0.5f, 1.0f),
            baseWidth = 5f,
            points = listOf(
                StrokePoint(10f, 20f, 0.5f, 0f, 1000L),
                StrokePoint(30f, 40f, 0.8f, 0f, 1005L)
            ),
            snappedToRuler = false,
            startCapRound = true,
            endCapRound = true,
            parentChartId = null
        )

        val layer = LayerEntity(
            id = "test-layer-1",
            name = "Layer 1",
            isVisible = true,
            opacity = 1.0f,
            strokes = listOf(stroke)
        )

        val json = converters.layerListToString(listOf(layer))
        assertNotNull(json)

        val deserialized = converters.stringToLayerList(json)
        assertEquals(1, deserialized.size)
        assertEquals(1, deserialized[0].strokes.size)
        assertEquals("test-stroke-1", deserialized[0].strokes[0].id)
        assertEquals(ToolType.PEN, deserialized[0].strokes[0].tool)
        assertEquals(2, deserialized[0].strokes[0].points.size)
    }

    @Test
    fun `test deserialization of legacy JSON missing startCapRound endCapRound parentChartId`() {
        val legacyJson = """
            [{
                "id": "layer-1",
                "name": "Layer 1",
                "isVisible": true,
                "opacity": 1.0,
                "blendMode": "NORMAL",
                "isLocked": false,
                "strokes": [{
                    "id": "stroke-1",
                    "tool": "PEN",
                    "colorHsla": {"hue": 0.0, "saturation": 0.0, "lightness": 0.0, "alpha": 1.0},
                    "baseWidth": 4.0,
                    "points": [{"x": 10.0, "y": 10.0, "pressure": 0.5, "tilt": 0.0, "timestampMs": 1000}],
                    "snappedToRuler": false
                }],
                "eraserMarks": [],
                "shapes": [],
                "textBlocks": [],
                "images": [],
                "charts": [],
                "codeBlocks": []
            }]
        """.trimIndent()

        val result = converters.stringToLayerList(legacyJson)
        assertEquals("Legacy JSON deserialization must not fail", 1, result.size)
        assertEquals("Legacy stroke must be preserved", 1, result[0].strokes.size)
        assertEquals("stroke-1", result[0].strokes[0].id)
    }
}
