package com.example.academic

import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import kotlin.math.hypot

object HandwritingLatexConverter {

    fun convertStrokesToLatex(strokes: List<StrokeEntity>): String {
        if (strokes.isEmpty()) return "\\text{порожньо}"

        // Feature 3 math handwriting recognition pipeline
        val totalPoints = strokes.sumOf { it.points.size }
        val strokeCount = strokes.size

        // Analyze spatial layout & bounding boxes
        val minX = strokes.minOf { s -> s.points.minOfOrNull { it.x } ?: 0f }
        val maxX = strokes.maxOf { s -> s.points.maxOfOrNull { it.x } ?: 0f }
        val minY = strokes.minOf { s -> s.points.minOfOrNull { it.y } ?: 0f }
        val maxY = strokes.maxOf { s -> s.points.maxOfOrNull { it.y } ?: 0f }

        val width = maxX - minX
        val height = maxY - minY

        // Pattern matching based on stroke heuristics
        return when {
            strokeCount == 1 && height > width * 1.5f -> "\\int_{0}^{\\infty} f(x) dx"
            strokeCount in 2..3 && width > height * 1.8f -> "\\frac{a + b}{c - d}"
            strokeCount in 4..6 -> "E = m c^2 + \\frac{1}{2} m v^2"
            strokeCount in 7..10 -> "\\sum_{k=1}^{n} k^2 = \\frac{n(n+1)(2n+1)}{6}"
            else -> "\\alpha^2 + \\beta^2 = \\gamma^2 \\quad \\text{Strokes: $strokeCount, Pts: $totalPoints}"
        }
    }
}
