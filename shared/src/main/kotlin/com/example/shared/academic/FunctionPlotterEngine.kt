package com.example.shared.academic

data class FunctionPlotPoint(val x: Double, val y: Double)

object FunctionPlotterEngine {

    fun generatePlotData(
        formula: String,
        xMin: Double = -10.0,
        xMax: Double = 10.0,
        samples: Int = 200
    ): List<FunctionPlotPoint> {
        val points = mutableListOf<FunctionPlotPoint>()
        if (xMax <= xMin || samples < 2) return points

        val step = (xMax - xMin) / (samples - 1)
        for (i in 0 until samples) {
            val x = xMin + i * step
            try {
                val y = MathExpressionEvaluator.evaluate(formula, x)
                if (!y.isNaN() && !y.isInfinite() && kotlin.math.abs(y) < 1e6) {
                    points.add(FunctionPlotPoint(x, y))
                }
            } catch (_: Exception) {}
        }
        return points
    }
}
