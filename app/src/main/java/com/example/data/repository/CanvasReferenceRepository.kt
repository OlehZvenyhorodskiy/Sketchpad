package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.CanvasDao
import com.example.data.db.CanvasReferenceDao
import com.example.data.db.PageDao
import com.example.data.models.CanvasReferenceDestination
import com.example.data.models.CanvasReferenceDestinationPage
import com.example.data.models.CanvasReferenceDraft
import com.example.data.models.CanvasReferenceEntity
import com.example.data.models.CanvasReferenceNavigationRequest
import com.example.data.models.CanvasReferenceSource
import com.example.data.models.CanvasReferenceTarget
import com.example.data.models.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Local Mini-Obsidian graph for links between precise locations in sketchpads. */
class CanvasReferenceRepository private constructor(
    private val referenceDao: CanvasReferenceDao,
    private val canvasDao: CanvasDao,
    private val pageDao: PageDao
) {
    constructor(context: Context) : this(
        database = AppDatabase.getDatabase(context.applicationContext)
    )

    private constructor(database: AppDatabase) : this(
        referenceDao = database.canvasReferenceDao(),
        canvasDao = database.canvasDao(),
        pageDao = database.pageDao()
    )

    fun observeOutgoingFromPage(pageId: String): Flow<List<CanvasReferenceEntity>> =
        referenceDao.observeOutgoingFromPage(pageId)

    fun observeIncomingToPage(pageId: String): Flow<List<CanvasReferenceEntity>> =
        referenceDao.observeIncomingToPage(pageId)

    fun observeOutgoingFromCanvas(canvasId: String): Flow<List<CanvasReferenceEntity>> =
        referenceDao.observeOutgoingFromCanvas(canvasId)

    fun observeIncomingToCanvas(canvasId: String): Flow<List<CanvasReferenceEntity>> =
        referenceDao.observeIncomingToCanvas(canvasId)

    fun observeReferencesForElement(
        sourcePageId: String,
        sourceElementId: String
    ): Flow<List<CanvasReferenceEntity>> = referenceDao
        .observeOutgoingFromPage(sourcePageId)
        .map { references -> references.filter { it.referencesElement(sourceElementId) } }

    fun observeReferenceForSelection(
        sourcePageId: String,
        sourceElementIds: Set<String>
    ): Flow<CanvasReferenceEntity?> = referenceDao
        .observeOutgoingFromPage(sourcePageId)
        .map { references ->
            references.firstOrNull { it.hasExactSourceSelection(sourceElementIds) }
        }

    /**
     * Creates a link or updates the existing link attached to the exact same source selection.
     * Set [replaceExistingSelection] to false when multiple links from one selection are desired.
     */
    suspend fun saveReference(
        draft: CanvasReferenceDraft,
        replaceExistingSelection: Boolean = true
    ): CanvasReferenceEntity = withContext(Dispatchers.IO) {
        validateDestination(draft.source, draft.target)

        val now = System.currentTimeMillis()
        val existing = if (replaceExistingSelection) {
            referenceDao.getOutgoingFromPage(draft.source.pageId)
                .firstOrNull { it.hasExactSourceSelection(draft.source.elementIds) }
        } else {
            null
        }
        val reference = CanvasReferenceEntity.fromDraft(
            draft = draft,
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        referenceDao.upsert(reference)
        reference
    }

    suspend fun updateTarget(
        referenceId: String,
        target: CanvasReferenceTarget
    ): CanvasReferenceEntity? = withContext(Dispatchers.IO) {
        val current = referenceDao.getById(referenceId) ?: return@withContext null
        val source = CanvasReferenceSource(
            canvasId = current.sourceCanvasId,
            pageId = current.sourcePageId,
            elementIds = current.sourceElementIds.toSet()
        )
        validateDestination(source, target)
        val viewport = target.viewport.normalized()
        val updated = current.copy(
            targetCanvasId = target.canvasId,
            targetPageId = target.pageId,
            targetCenterX = viewport.centerX,
            targetCenterY = viewport.centerY,
            targetZoom = viewport.zoom,
            targetElementIds = target.canonicalElementIds(),
            updatedAt = System.currentTimeMillis()
        )
        referenceDao.upsert(updated)
        updated
    }

    suspend fun updateLabel(referenceId: String, label: String): CanvasReferenceEntity? =
        withContext(Dispatchers.IO) {
            val current = referenceDao.getById(referenceId) ?: return@withContext null
            val updated = current.copy(
                label = label.trim(),
                updatedAt = System.currentTimeMillis()
            )
            referenceDao.upsert(updated)
            updated
        }

    suspend fun getReference(referenceId: String): CanvasReferenceEntity? =
        withContext(Dispatchers.IO) { referenceDao.getById(referenceId) }

    suspend fun navigationRequest(referenceId: String): CanvasReferenceNavigationRequest? =
        withContext(Dispatchers.IO) { referenceDao.getById(referenceId)?.toNavigationRequest() }

    suspend fun deleteReference(referenceId: String) = withContext(Dispatchers.IO) {
        referenceDao.deleteById(referenceId)
    }

    suspend fun deleteReferencesForElements(sourcePageId: String, elementIds: Set<String>) =
        withContext(Dispatchers.IO) {
            if (elementIds.isEmpty()) return@withContext
            val stale = referenceDao.getOutgoingFromPage(sourcePageId).filter { reference ->
                reference.sourceElementIds.any(elementIds::contains)
            }
            if (stale.isNotEmpty()) referenceDao.deleteAll(stale)
        }

    /** Removes references whose complete source selection no longer exists on [page]. */
    suspend fun pruneMissingSourceElements(page: PageEntity) = withContext(Dispatchers.IO) {
        val liveIds = page.getEffectiveLayers().flatMap { layer ->
            buildList {
                addAll(layer.strokes.map { it.id })
                addAll(layer.shapes.map { it.id })
                addAll(layer.textBlocks.map { it.id })
                addAll(layer.images.map { it.id })
                addAll(layer.charts.map { it.id })
                addAll(layer.codeBlocks.map { it.id })
            }
        }.toSet()
        val stale = referenceDao.getOutgoingFromPage(page.id).filter { reference ->
            reference.sourceElementIds.any { it !in liveIds }
        }
        if (stale.isNotEmpty()) referenceDao.deleteAll(stale)
    }

    /** Returns canvases with their pages for the destination picker. */
    suspend fun listDestinations(
        query: String = "",
        excludePageId: String? = null
    ): List<CanvasReferenceDestination> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        canvasDao.getAllCanvasesSync()
            .filter { normalizedQuery.isEmpty() || it.title.contains(normalizedQuery, ignoreCase = true) }
            .map { canvas ->
                val pages = pageDao.getPagesForCanvasSync(canvas.id)
                    .filterNot { it.id == excludePageId }
                    .sortedBy { it.pageIndex }
                    .map { page ->
                        CanvasReferenceDestinationPage(
                            canvasId = canvas.id,
                            canvasTitle = canvas.title,
                            pageId = page.id,
                            pageIndex = page.pageIndex
                        )
                    }
                CanvasReferenceDestination(
                    canvasId = canvas.id,
                    canvasTitle = canvas.title,
                    pages = pages
                )
            }
            .filter { it.pages.isNotEmpty() }
    }

    private suspend fun validateDestination(
        source: CanvasReferenceSource,
        target: CanvasReferenceTarget
    ) {
        require(canvasDao.getCanvasByIdSync(source.canvasId) != null) {
            "Source canvas does not exist"
        }
        require(pageDao.getPageByIdSync(source.pageId)?.canvasId == source.canvasId) {
            "Source page does not belong to the source canvas"
        }
        require(canvasDao.getCanvasByIdSync(target.canvasId) != null) {
            "Target canvas does not exist"
        }
        require(pageDao.getPageByIdSync(target.pageId)?.canvasId == target.canvasId) {
            "Target page does not belong to the target canvas"
        }
    }
}
