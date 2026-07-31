package com.example.data.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(
    tableName = "study_decks",
    foreignKeys = [
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["canvasId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("canvasId"), Index("pageId"), Index("updatedAt")]
)
data class StudyDeckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val canvasId: String? = null,
    val pageId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = StudyDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceCanvasId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourcePageId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("deckId"),
        Index("sourceCanvasId"),
        Index("sourcePageId"),
        Index(value = ["deckId", "dueAt"])
    ]
)
data class FlashcardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val prompt: String,
    val answer: String,
    val hint: String = "",
    val tags: List<String> = emptyList(),
    val sourceCanvasId: String? = null,
    val sourcePageId: String? = null,
    val sourceElementIds: List<String> = emptyList(),
    val position: Int = 0,
    val dueAt: Long = 0L,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5,
    val lapses: Int = 0,
    val lastReviewedAt: Long? = null,
    val suspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

data class StudyDeckWithCards(
    @Embedded val deck: StudyDeckEntity,
    @Relation(parentColumn = "id", entityColumn = "deckId")
    val cards: List<FlashcardEntity>
)

data class StudyDeckSummary(
    @Embedded val deck: StudyDeckEntity,
    @ColumnInfo(name = "cardCount") val cardCount: Int,
    @ColumnInfo(name = "dueCount") val dueCount: Int
)
