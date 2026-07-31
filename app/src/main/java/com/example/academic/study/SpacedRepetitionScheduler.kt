package com.example.academic.study

import com.example.data.models.FlashcardEntity
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewGrade(val quality: Int) {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5)
}

/**
 * A small, deterministic SM-2 variant suitable for completely offline reviews.
 *
 * Failed cards return after ten minutes. Passing cards graduate through day-based intervals, while
 * the classic SM-2 ease formula adapts subsequent intervals to the selected grade.
 */
object SpacedRepetitionScheduler {
    const val MIN_EASE_FACTOR = 1.3
    const val DEFAULT_EASE_FACTOR = 2.5
    const val AGAIN_DELAY_MILLIS = 10 * 60 * 1000L
    const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun schedule(
        card: FlashcardEntity,
        grade: ReviewGrade,
        reviewedAt: Long
    ): FlashcardEntity {
        require(reviewedAt >= 0L) { "Review time cannot be negative" }

        val nextEase = updatedEase(card.easeFactor, grade.quality)
        if (grade == ReviewGrade.AGAIN) {
            return card.copy(
                dueAt = reviewedAt + AGAIN_DELAY_MILLIS,
                intervalDays = 0,
                repetitions = 0,
                easeFactor = nextEase,
                lapses = card.lapses + 1,
                lastReviewedAt = reviewedAt,
                updatedAt = reviewedAt
            )
        }

        val interval = nextIntervalDays(card, grade, nextEase)
        return card.copy(
            dueAt = reviewedAt + interval.toLong() * DAY_MILLIS,
            intervalDays = interval,
            repetitions = card.repetitions + 1,
            easeFactor = nextEase,
            lastReviewedAt = reviewedAt,
            updatedAt = reviewedAt
        )
    }

    private fun updatedEase(current: Double, quality: Int): Double {
        val safeCurrent = current.takeIf(Double::isFinite) ?: DEFAULT_EASE_FACTOR
        val distance = 5 - quality
        val adjustment = 0.1 - distance * (0.08 + distance * 0.02)
        return max(MIN_EASE_FACTOR, safeCurrent + adjustment)
    }

    private fun nextIntervalDays(
        card: FlashcardEntity,
        grade: ReviewGrade,
        ease: Double
    ): Int = when (card.repetitions) {
        0 -> if (grade == ReviewGrade.EASY) 4 else 1
        1 -> when (grade) {
            ReviewGrade.HARD -> 3
            ReviewGrade.GOOD -> 6
            ReviewGrade.EASY -> 9
            ReviewGrade.AGAIN -> error("Again cards do not use a day interval")
        }
        else -> {
            val multiplier = when (grade) {
                ReviewGrade.HARD -> 0.8
                ReviewGrade.GOOD -> 1.0
                ReviewGrade.EASY -> 1.3
                ReviewGrade.AGAIN -> error("Again cards do not use a day interval")
            }
            max(card.intervalDays + 1, (card.intervalDays * ease * multiplier).roundToInt())
        }
    }
}
