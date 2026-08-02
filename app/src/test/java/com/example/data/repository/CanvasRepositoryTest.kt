package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.models.HslaColor
import com.example.data.models.LayerEntity
import com.example.data.models.PageEntity
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.ToolType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CanvasRepositoryTest {

    @Test
    fun `page update persists strokes in Room database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CanvasRepository(context)

        val canvasId = repository.createNewCanvas("Persistence Test Canvas")
        val initialPages = repository.getPagesForCanvas(canvasId).first()
        assertEquals(1, initialPages.size)

        val initialPage = initialPages.first()
        val stroke = StrokeEntity(
            id = "persisted-stroke-1",
            tool = ToolType.PEN,
            colorHsla = HslaColor.BLACK,
            baseWidth = 4f,
            points = listOf(StrokePoint(10f, 10f), StrokePoint(100f, 100f))
        )

        val updatedPage = initialPage.withAddedStroke(stroke)
        repository.updatePage(updatedPage)

        val persistedPages = repository.getPagesForCanvas(canvasId).first()
        assertEquals(1, persistedPages.size)

        val loadedPage = persistedPages.first()
        val allStrokes = loadedPage.getEffectiveLayers().flatMap { it.strokes }
        assertEquals(1, allStrokes.size)
        assertEquals("persisted-stroke-1", allStrokes.first().id)
    }

    @Test
    fun `createNewCanvas initializes page with default active layer`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CanvasRepository(context)

        val canvasId = repository.createNewCanvas("New Canvas Layer Test")
        val initialPages = repository.getPagesForCanvas(canvasId).first()
        assertEquals(1, initialPages.size)

        val initialPage = initialPages.first()
        assertEquals("default", initialPage.activeLayerId)
        assertEquals(1, initialPage.layers.size)
        assertEquals("default", initialPage.layers.first().id)
    }
}
