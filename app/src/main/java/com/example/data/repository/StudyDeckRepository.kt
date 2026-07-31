package com.example.data.repository

import android.content.Context
import com.example.academic.study.ReviewGrade
import com.example.academic.study.SpacedRepetitionScheduler
import com.example.data.db.AppDatabase
import com.example.data.db.StudyDeckDao
import com.example.data.models.FlashcardEntity
import com.example.data.models.StudyDeckEntity
import com.example.data.models.StudyDeckSummary
import com.example.data.models.StudyDeckWithCards
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Local decks and review scheduling; no account or network connection is required. */
class StudyDeckRepository private constructor(
    private val dao: StudyDeckDao
) {
    constructor(context: Context) : this(
        AppDatabase.getDatabase(context.applicationContext).studyDeckDao()
    )

    fun observeDecks(now: Long = System.currentTimeMillis()): Flow<List<StudyDeckSummary>> =
        dao.observeDeckSummaries(now)

    fun observeDeck(deckId: String): Flow<StudyDeckEntity?> = dao.observeDeck(deckId)

    fun observeCards(deckId: String): Flow<List<FlashcardEntity>> = dao.observeCards(deckId)

    fun observeDueCards(
        deckId: String,
        now: Long = System.currentTimeMillis(),
        limit: Int = DEFAULT_REVIEW_LIMIT
    ): Flow<List<FlashcardEntity>> = dao.observeDueCards(
        deckId = deckId,
        now = now,
        limit = limit.coerceIn(1, MAX_REVIEW_LIMIT)
    )

    suspend fun getDeckWithCards(deckId: String): StudyDeckWithCards? = withContext(Dispatchers.IO) {
        dao.getDeckWithCards(deckId)
    }

    suspend fun createDeck(
        title: String,
        description: String = "",
        canvasId: String? = null,
        pageId: String? = null,
        now: Long = System.currentTimeMillis()
    ): StudyDeckEntity = withContext(Dispatchers.IO) {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Deck title cannot be blank" }
        val deck = StudyDeckEntity(
            id = UUID.randomUUID().toString(),
            title = normalizedTitle,
            description = description.trim(),
            canvasId = canvasId,
            pageId = pageId,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertDeck(deck)
        deck
    }

    suspend fun addCard(
        deckId: String,
        prompt: String,
        answer: String,
        hint: String = "",
        tags: List<String> = emptyList(),
        sourceCanvasId: String? = null,
        sourcePageId: String? = null,
        sourceElementIds: List<String> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): FlashcardEntity = withContext(Dispatchers.IO) {
        val normalizedPrompt = prompt.trim()
        val normalizedAnswer = answer.trim()
        require(normalizedPrompt.isNotEmpty()) { "Card prompt cannot be blank" }
        require(normalizedAnswer.isNotEmpty()) { "Card answer cannot be blank" }
        require(dao.getDeck(deckId) != null) { "Deck does not exist" }

        val card = FlashcardEntity(
            id = UUID.randomUUID().toString(),
            deckId = deckId,
            prompt = normalizedPrompt,
            answer = normalizedAnswer,
            hint = hint.trim(),
            tags = tags.map(String::trim).filter(String::isNotEmpty).distinct(),
            sourceCanvasId = sourceCanvasId,
            sourcePageId = sourcePageId,
            sourceElementIds = sourceElementIds.filter(String::isNotBlank).distinct().sorted(),
            position = dao.nextCardPosition(deckId),
            dueAt = now,
            easeFactor = SpacedRepetitionScheduler.DEFAULT_EASE_FACTOR,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertCard(card)
        touchDeck(deckId, now)
        card
    }

    suspend fun reviewCard(
        cardId: String,
        grade: ReviewGrade,
        reviewedAt: Long = System.currentTimeMillis()
    ): FlashcardEntity? = withContext(Dispatchers.IO) {
        val current = dao.getCard(cardId) ?: return@withContext null
        val reviewed = SpacedRepetitionScheduler.schedule(current, grade, reviewedAt)
        dao.updateCard(reviewed)
        touchDeck(reviewed.deckId, reviewedAt)
        reviewed
    }

    suspend fun setCardSuspended(
        cardId: String,
        suspended: Boolean,
        now: Long = System.currentTimeMillis()
    ): FlashcardEntity? = withContext(Dispatchers.IO) {
        val current = dao.getCard(cardId) ?: return@withContext null
        val updated = current.copy(suspended = suspended, updatedAt = now)
        dao.updateCard(updated)
        touchDeck(updated.deckId, now)
        updated
    }

    suspend fun deleteCard(card: FlashcardEntity) = withContext(Dispatchers.IO) {
        dao.deleteCard(card)
        touchDeck(card.deckId, System.currentTimeMillis())
    }

    suspend fun deleteDeck(deck: StudyDeckEntity) = withContext(Dispatchers.IO) {
        dao.deleteDeck(deck)
    }

    private suspend fun touchDeck(deckId: String, now: Long) {
        dao.getDeck(deckId)?.let { dao.updateDeck(it.copy(updatedAt = now)) }
    }

    companion object {
        const val DEFAULT_REVIEW_LIMIT = 50
        const val MAX_REVIEW_LIMIT = 200
    }
}
