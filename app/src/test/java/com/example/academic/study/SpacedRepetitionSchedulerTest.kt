package com.example.academic.study

import com.example.data.models.FlashcardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {
    private val reviewedAt = 1_700_000_000_000L

    @Test
    fun `good first review schedules one day without changing ease`() {
        val result = SpacedRepetitionScheduler.schedule(card(), ReviewGrade.GOOD, reviewedAt)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(reviewedAt + SpacedRepetitionScheduler.DAY_MILLIS, result.dueAt)
        assertEquals(2.5, result.easeFactor, 0.0001)
        assertEquals(reviewedAt, result.lastReviewedAt)
    }

    @Test
    fun `easy first review graduates directly to four days`() {
        val result = SpacedRepetitionScheduler.schedule(card(), ReviewGrade.EASY, reviewedAt)

        assertEquals(4, result.intervalDays)
        assertEquals(2.6, result.easeFactor, 0.0001)
        assertEquals(reviewedAt + 4 * SpacedRepetitionScheduler.DAY_MILLIS, result.dueAt)
    }

    @Test
    fun `hard second review uses short interval and lowers ease`() {
        val result = SpacedRepetitionScheduler.schedule(
            card(repetitions = 1, intervalDays = 1),
            ReviewGrade.HARD,
            reviewedAt
        )

        assertEquals(2, result.repetitions)
        assertEquals(3, result.intervalDays)
        assertEquals(2.36, result.easeFactor, 0.0001)
    }

    @Test
    fun `mature good review multiplies interval by ease`() {
        val result = SpacedRepetitionScheduler.schedule(
            card(repetitions = 5, intervalDays = 10, easeFactor = 2.4),
            ReviewGrade.GOOD,
            reviewedAt
        )

        assertEquals(24, result.intervalDays)
        assertEquals(reviewedAt + 24 * SpacedRepetitionScheduler.DAY_MILLIS, result.dueAt)
    }

    @Test
    fun `again resets repetitions and returns card in ten minutes`() {
        val result = SpacedRepetitionScheduler.schedule(
            card(repetitions = 4, intervalDays = 20, lapses = 2),
            ReviewGrade.AGAIN,
            reviewedAt
        )

        assertEquals(0, result.repetitions)
        assertEquals(0, result.intervalDays)
        assertEquals(3, result.lapses)
        assertEquals(reviewedAt + SpacedRepetitionScheduler.AGAIN_DELAY_MILLIS, result.dueAt)
    }

    @Test
    fun `ease never drops below SM2 floor`() {
        var scheduled = card(easeFactor = 1.31)
        repeat(10) {
            scheduled = SpacedRepetitionScheduler.schedule(
                scheduled,
                ReviewGrade.AGAIN,
                reviewedAt + it
            )
        }

        assertEquals(SpacedRepetitionScheduler.MIN_EASE_FACTOR, scheduled.easeFactor, 0.0001)
    }

    @Test
    fun `same inputs always produce same schedule`() {
        val input = card(repetitions = 3, intervalDays = 8, easeFactor = 2.2)

        val first = SpacedRepetitionScheduler.schedule(input, ReviewGrade.EASY, reviewedAt)
        val second = SpacedRepetitionScheduler.schedule(input, ReviewGrade.EASY, reviewedAt)

        assertEquals(first, second)
        assertTrue(first.intervalDays > input.intervalDays)
    }

    private fun card(
        repetitions: Int = 0,
        intervalDays: Int = 0,
        easeFactor: Double = 2.5,
        lapses: Int = 0
    ) = FlashcardEntity(
        id = "card",
        deckId = "deck",
        prompt = "Question",
        answer = "Answer",
        repetitions = repetitions,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        lapses = lapses,
        createdAt = 1L,
        updatedAt = 1L
    )
}
