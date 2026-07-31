package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.models.FlashcardEntity
import com.example.data.models.StudyDeckEntity
import com.example.data.models.StudyDeckSummary
import com.example.data.models.StudyDeckWithCards
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDeckDao {
    @Query(
        """
        SELECT d.*,
            (SELECT COUNT(*) FROM flashcards c WHERE c.deckId = d.id) AS cardCount,
            (SELECT COUNT(*) FROM flashcards c
                WHERE c.deckId = d.id AND c.suspended = 0 AND c.dueAt <= :now) AS dueCount
        FROM study_decks d
        ORDER BY d.updatedAt DESC
        """
    )
    fun observeDeckSummaries(now: Long): Flow<List<StudyDeckSummary>>

    @Query("SELECT * FROM study_decks WHERE id = :deckId LIMIT 1")
    fun observeDeck(deckId: String): Flow<StudyDeckEntity?>

    @Query("SELECT * FROM study_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeck(deckId: String): StudyDeckEntity?

    @Transaction
    @Query("SELECT * FROM study_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckWithCards(deckId: String): StudyDeckWithCards?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeck(deck: StudyDeckEntity)

    @Update
    suspend fun updateDeck(deck: StudyDeckEntity)

    @Delete
    suspend fun deleteDeck(deck: StudyDeckEntity)

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY position, createdAt")
    fun observeCards(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY position, createdAt")
    suspend fun getCards(deckId: String): List<FlashcardEntity>

    @Query(
        """
        SELECT * FROM flashcards
        WHERE deckId = :deckId AND suspended = 0 AND dueAt <= :now
        ORDER BY dueAt, position, createdAt
        LIMIT :limit
        """
    )
    fun observeDueCards(deckId: String, now: Long, limit: Int): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE id = :cardId LIMIT 1")
    suspend fun getCard(cardId: String): FlashcardEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM flashcards WHERE deckId = :deckId")
    suspend fun nextCardPosition(deckId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: FlashcardEntity)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteCards(deckId: String)
}
