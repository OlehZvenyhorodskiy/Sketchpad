package com.example.academic

import com.example.data.models.StrokeEntity

data class TimestampedStroke(
    val strokeId: String,
    val audioTimestampMs: Long,
    val previewLabel: String
)

object SmartLectureRecorder {

    fun generateTimeline(strokes: List<StrokeEntity>, recordingStartTimeMs: Long): List<TimestampedStroke> {
        return strokes.mapIndexed { idx, st ->
            val strokeTime = st.points.firstOrNull()?.timestampMs ?: System.currentTimeMillis()
            val relativeMs = (strokeTime - recordingStartTimeMs).coerceAtLeast(0L)
            val seconds = relativeMs / 1000
            val mins = seconds / 60
            val secs = seconds % 60
            val timeStr = String.format("%02d:%02d", mins, secs)

            TimestampedStroke(
                strokeId = st.id,
                audioTimestampMs = relativeMs,
                previewLabel = "Штрих #${idx + 1} ($timeStr)"
            )
        }
    }
}
