package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasNamingTest {
    @Test
    fun `new canvases use the first available numeric suffix`() {
        assertEquals(
            "New canvas 3",
            nextAvailableCanvasTitle(
                baseTitle = "New canvas",
                existingTitles = listOf("New canvas", "New canvas 1", "New canvas 2", "Physics")
            )
        )
    }

    @Test
    fun `numbering fills gaps without renaming custom notes`() {
        assertEquals(
            "New canvas 2",
            nextAvailableCanvasTitle("New canvas", listOf("New canvas 1", "New canvas 3"))
        )
    }
}
