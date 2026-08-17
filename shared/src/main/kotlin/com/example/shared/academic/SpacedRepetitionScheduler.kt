package com.example.shared.academic

import kotlin.math.max
import kotlin.math.roundToLong

data class CardReviewSchedule(
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Float,
    val nextReviewTimestampMs: Long
)

object SpacedRepetitionScheduler {

    /**
     * SM-2 Spaced Repetition calculation
     * @param rating 0 (Blackout) .. 5 (Perfect recall)
     */
    fun calculateNextReview(
        rating: Int,
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Float,
        currentTimestampMs: Long = System.currentTimeMillis()
    ): CardReviewSchedule {
        val grade = rating.coerceIn(0, 5)
        var repetitions = currentRepetitions
        var intervalDays: Int
        var easeFactor = currentEaseFactor

        if (grade >= 3) {
            when (repetitions) {
                0 -> intervalDays = 1
                1 -> intervalDays = 6
                else -> intervalDays = (currentIntervalDays * easeFactor).roundToLong().toInt()
            }
            repetitions++
        } else {
            repetitions = 0
            intervalDays = 1
        }

        // SM-2 Ease Factor formula: EF' = EF + (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02))
        easeFactor = easeFactor + (0.1f - (5 - grade) * (0.08f + (5 - grade) * 0.02f))
        easeFactor = max(1.3f, easeFactor)

        val nextReviewMs = currentTimestampMs + (intervalDays * 24L * 60L * 60L * 1000L)

        return CardReviewSchedule(
            repetitions = repetitions,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            nextReviewTimestampMs = nextReviewMs
        )
    }
}
