package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.models.CanvasEntity
import com.example.data.models.CanvasReferenceDraft
import com.example.data.models.CanvasReferenceSource
import com.example.data.models.CanvasReferenceTarget
import com.example.data.models.CanvasViewport
import com.example.data.models.PageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CanvasReferenceRepositoryTest {

    @Test
    fun `reference is persisted resolved and removed with its canvas`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val repository = CanvasReferenceRepository(context)
        val canvasId = UUID.randomUUID().toString()
        val sourcePageId = UUID.randomUUID().toString()
        val targetPageId = UUID.randomUUID().toString()

        database.canvasDao().insertCanvas(CanvasEntity(id = canvasId, title = "Physics"))
        database.pageDao().insertPages(
            listOf(
                PageEntity(id = sourcePageId, canvasId = canvasId, pageIndex = 0),
                PageEntity(id = targetPageId, canvasId = canvasId, pageIndex = 1)
            )
        )

        val saved = repository.saveReference(
            CanvasReferenceDraft(
                source = CanvasReferenceSource(canvasId, sourcePageId, setOf("title", "formula")),
                target = CanvasReferenceTarget(
                    canvasId = canvasId,
                    pageId = targetPageId,
                    viewport = CanvasViewport(420f, 180f, 2.5f)
                ),
                label = "Detailed derivation"
            )
        )

        assertEquals(
            saved.id,
            repository.observeReferencesForElement(sourcePageId, "formula").first().single().id
        )
        assertEquals(420f, repository.navigationRequest(saved.id)?.viewport?.centerX ?: 0f, 0.001f)
        assertNotNull(repository.getReference(saved.id))

        database.canvasDao().deleteCanvas(canvasId)

        assertNull(repository.getReference(saved.id))
    }
}
