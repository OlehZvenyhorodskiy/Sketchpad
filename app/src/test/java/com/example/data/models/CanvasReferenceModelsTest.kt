package com.example.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasReferenceModelsTest {

    @Test
    fun `viewport transform round trips across screen space`() {
        val viewport = CanvasViewport.fromCanvasTransform(
            panX = -320f,
            panY = 140f,
            zoom = 2f,
            viewportWidthPx = 1_200f,
            viewportHeightPx = 800f
        )

        assertEquals(-320f, viewport.panX(1_200f), 0.001f)
        assertEquals(140f, viewport.panY(800f), 0.001f)
        assertEquals(2f, viewport.zoom, 0.001f)
    }

    @Test
    fun `reference factory canonicalizes group and navigation metadata`() {
        val draft = CanvasReferenceDraft(
            source = CanvasReferenceSource(
                canvasId = "source-canvas",
                pageId = "source-page",
                elementIds = linkedSetOf(" text-2 ", "shape-1", "shape-1")
            ),
            target = CanvasReferenceTarget(
                canvasId = "target-canvas",
                pageId = "target-page",
                viewport = CanvasViewport(120f, 340f, 99f),
                elementIds = setOf("target-shape")
            ),
            label = "  Thermodynamics  ",
            transitionDurationMillis = 99_000,
            highlightDurationMillis = -5
        )

        val reference = CanvasReferenceEntity.fromDraft(
            draft = draft,
            id = "reference-id",
            createdAt = 10L,
            updatedAt = 20L
        )
        val navigation = reference.toNavigationRequest()

        assertEquals(listOf("shape-1", "text-2"), reference.sourceElementIds)
        assertTrue(reference.hasExactSourceSelection(setOf("text-2", "shape-1")))
        assertEquals("Thermodynamics", reference.label)
        assertEquals(CanvasViewport.MAX_ZOOM, navigation.viewport.zoom, 0.001f)
        assertEquals(CanvasReferenceEntity.MAX_TRANSITION_DURATION_MS, navigation.transitionDurationMillis)
        assertEquals(0, navigation.highlightDurationMillis)
        assertEquals(setOf("target-shape"), navigation.targetElementIds)
    }

    @Test
    fun `invalid viewport values fall back to safe values`() {
        val viewport = CanvasViewport(Float.NaN, Float.POSITIVE_INFINITY, Float.NaN).normalized()

        assertEquals(0f, viewport.centerX, 0f)
        assertEquals(0f, viewport.centerY, 0f)
        assertEquals(CanvasViewport.DEFAULT_ZOOM, viewport.zoom, 0f)
    }

    @Test
    fun `capture session keeps source group while navigating to target page`() {
        val source = CanvasReferenceSource("canvas-a", "page-a", setOf("formula", "caption"))
        val destination = CanvasReferenceDestinationPage(
            canvasId = "canvas-b",
            canvasTitle = "Thermodynamics",
            pageId = "page-b",
            pageIndex = 3
        )
        val session = CanvasReferenceCaptureSession(source, destination, label = "Entropy")

        val draft = session.createDraft(
            viewport = CanvasViewport(800f, 250f, 1.75f),
            targetElementIds = setOf("derivation")
        )

        assertEquals(source, draft.source)
        assertEquals("page-b", draft.target.pageId)
        assertEquals(setOf("derivation"), draft.target.elementIds)
        assertEquals("Entropy", draft.label)
    }
}
