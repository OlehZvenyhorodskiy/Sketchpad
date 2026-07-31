package com.example.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A device-independent canvas position.
 *
 * The center is stored in world coordinates instead of as a screen-space pan offset. This makes
 * a link land at the same content after rotation or when it is opened on a differently sized
 * display.
 */
data class CanvasViewport(
    val centerX: Float,
    val centerY: Float,
    val zoom: Float
) {
    fun normalized(): CanvasViewport = copy(
        centerX = centerX.takeIf(Float::isFinite) ?: 0f,
        centerY = centerY.takeIf(Float::isFinite) ?: 0f,
        zoom = zoom.takeIf(Float::isFinite)?.coerceIn(MIN_ZOOM, MAX_ZOOM) ?: DEFAULT_ZOOM
    )

    fun panX(viewportWidthPx: Float): Float {
        val safe = normalized()
        return viewportWidthPx / 2f - safe.centerX * safe.zoom
    }

    fun panY(viewportHeightPx: Float): Float {
        val safe = normalized()
        return viewportHeightPx / 2f - safe.centerY * safe.zoom
    }

    companion object {
        const val MIN_ZOOM = 0.25f
        const val MAX_ZOOM = 8f
        const val DEFAULT_ZOOM = 1f

        /** Converts the editor's `world * zoom + pan` transform into a stable world viewport. */
        fun fromCanvasTransform(
            panX: Float,
            panY: Float,
            zoom: Float,
            viewportWidthPx: Float,
            viewportHeightPx: Float
        ): CanvasViewport {
            val safeZoom = zoom.takeIf { it.isFinite() && it > 0f }
                ?.coerceIn(MIN_ZOOM, MAX_ZOOM)
                ?: DEFAULT_ZOOM
            return CanvasViewport(
                centerX = (viewportWidthPx / 2f - panX) / safeZoom,
                centerY = (viewportHeightPx / 2f - panY) / safeZoom,
                zoom = safeZoom
            ).normalized()
        }
    }
}

data class CanvasReferenceSource(
    val canvasId: String,
    val pageId: String,
    val elementIds: Set<String>
) {
    init {
        require(canvasId.isNotBlank()) { "Source canvas id cannot be blank" }
        require(pageId.isNotBlank()) { "Source page id cannot be blank" }
        require(elementIds.any { it.isNotBlank() }) { "At least one source element is required" }
    }

    fun canonicalElementIds(): List<String> = elementIds.canonicalElementIds()
}

data class CanvasReferenceTarget(
    val canvasId: String,
    val pageId: String,
    val viewport: CanvasViewport,
    val elementIds: Set<String> = emptySet()
) {
    init {
        require(canvasId.isNotBlank()) { "Target canvas id cannot be blank" }
        require(pageId.isNotBlank()) { "Target page id cannot be blank" }
    }

    fun canonicalElementIds(): List<String> = elementIds.canonicalElementIds()
}

data class CanvasReferenceDraft(
    val source: CanvasReferenceSource,
    val target: CanvasReferenceTarget,
    val label: String = "",
    val transitionDurationMillis: Int = CanvasReferenceEntity.DEFAULT_TRANSITION_DURATION_MS,
    val highlightDurationMillis: Int = CanvasReferenceEntity.DEFAULT_HIGHLIGHT_DURATION_MS
)

/**
 * Hoist this while the editor navigates from the source selection to the destination page. It
 * keeps the source group intact until the user frames and confirms the target viewport.
 */
data class CanvasReferenceCaptureSession(
    val source: CanvasReferenceSource,
    val destination: CanvasReferenceDestinationPage,
    val label: String = "",
    val transitionDurationMillis: Int = CanvasReferenceEntity.DEFAULT_TRANSITION_DURATION_MS,
    val highlightDurationMillis: Int = CanvasReferenceEntity.DEFAULT_HIGHLIGHT_DURATION_MS
) {
    fun createDraft(
        viewport: CanvasViewport,
        targetElementIds: Set<String> = emptySet()
    ): CanvasReferenceDraft = CanvasReferenceDraft(
        source = source,
        target = CanvasReferenceTarget(
            canvasId = destination.canvasId,
            pageId = destination.pageId,
            viewport = viewport,
            elementIds = targetElementIds
        ),
        label = label,
        transitionDurationMillis = transitionDurationMillis,
        highlightDurationMillis = highlightDurationMillis
    )
}

@Entity(
    tableName = "canvas_references",
    foreignKeys = [
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceCanvasId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourcePageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetCanvasId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetPageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceCanvasId"]),
        Index(value = ["sourcePageId"]),
        Index(value = ["targetCanvasId"]),
        Index(value = ["targetPageId"])
    ]
)
data class CanvasReferenceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceCanvasId: String,
    val sourcePageId: String,
    val sourceElementIds: List<String>,
    val label: String = "",
    val targetCanvasId: String,
    val targetPageId: String,
    val targetCenterX: Float,
    val targetCenterY: Float,
    val targetZoom: Float,
    val targetElementIds: List<String> = emptyList(),
    val transitionDurationMillis: Int = DEFAULT_TRANSITION_DURATION_MS,
    val highlightDurationMillis: Int = DEFAULT_HIGHLIGHT_DURATION_MS,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val targetViewport: CanvasViewport
        get() = CanvasViewport(targetCenterX, targetCenterY, targetZoom).normalized()

    fun referencesElement(elementId: String): Boolean = elementId in sourceElementIds

    fun hasExactSourceSelection(elementIds: Set<String>): Boolean =
        sourceElementIds == elementIds.canonicalElementIds()

    fun toNavigationRequest(): CanvasReferenceNavigationRequest = CanvasReferenceNavigationRequest(
        referenceId = id,
        canvasId = targetCanvasId,
        pageId = targetPageId,
        viewport = targetViewport,
        targetElementIds = targetElementIds.toSet(),
        transitionDurationMillis = transitionDurationMillis.coerceIn(0, MAX_TRANSITION_DURATION_MS),
        highlightDurationMillis = highlightDurationMillis.coerceIn(0, MAX_HIGHLIGHT_DURATION_MS)
    )

    companion object {
        const val DEFAULT_TRANSITION_DURATION_MS = 550
        const val DEFAULT_HIGHLIGHT_DURATION_MS = 1_200
        const val MAX_TRANSITION_DURATION_MS = 5_000
        const val MAX_HIGHLIGHT_DURATION_MS = 10_000

        fun fromDraft(
            draft: CanvasReferenceDraft,
            id: String = UUID.randomUUID().toString(),
            createdAt: Long = System.currentTimeMillis(),
            updatedAt: Long = createdAt
        ): CanvasReferenceEntity {
            val viewport = draft.target.viewport.normalized()
            return CanvasReferenceEntity(
                id = id,
                sourceCanvasId = draft.source.canvasId,
                sourcePageId = draft.source.pageId,
                sourceElementIds = draft.source.canonicalElementIds(),
                label = draft.label.trim(),
                targetCanvasId = draft.target.canvasId,
                targetPageId = draft.target.pageId,
                targetCenterX = viewport.centerX,
                targetCenterY = viewport.centerY,
                targetZoom = viewport.zoom,
                targetElementIds = draft.target.canonicalElementIds(),
                transitionDurationMillis = draft.transitionDurationMillis
                    .coerceIn(0, MAX_TRANSITION_DURATION_MS),
                highlightDurationMillis = draft.highlightDurationMillis
                    .coerceIn(0, MAX_HIGHLIGHT_DURATION_MS),
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}

data class CanvasReferenceNavigationRequest(
    val referenceId: String,
    val canvasId: String,
    val pageId: String,
    val viewport: CanvasViewport,
    val targetElementIds: Set<String>,
    val transitionDurationMillis: Int,
    val highlightDurationMillis: Int
)

data class CanvasReferenceDestination(
    val canvasId: String,
    val canvasTitle: String,
    val pages: List<CanvasReferenceDestinationPage>
)

data class CanvasReferenceDestinationPage(
    val canvasId: String,
    val canvasTitle: String,
    val pageId: String,
    val pageIndex: Int
)

private fun Iterable<String>.canonicalElementIds(): List<String> = asSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
    .sorted()
    .toList()
