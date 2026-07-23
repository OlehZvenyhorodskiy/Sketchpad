package com.example.academic

import androidx.compose.ui.geometry.Offset
import com.example.data.models.StrokePoint
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

data class PlottedFunctionResult(
    val latexFormula: String,
    val curvePoints: List<StrokePoint>,
    val rSquared: Float
)

object FunctionPlotterEngine {

    fun fitFunctionFromStrokes(points: List<StrokePoint>): PlottedFunctionResult? {
        if (points.size < 6) return null

        val sorted = points.sortedBy { it.x }
        val n = sorted.size

        val xs = DoubleArray(n)
        val ys = DoubleArray(n)
        for (i in 0 until n) {
            xs[i] = sorted[i].x.toDouble()
            ys[i] = sorted[i].y.toDouble()
        }

        val minX = sorted.first().x
        val maxX = sorted.last().x
        if (maxX - minX < 30f) return null

        // Attempt Quadratic fit: y = a*x^2 + b*x + c
        val quadCoeffs = polyfit2(xs, ys)
        val a = quadCoeffs[2]
        val b = quadCoeffs[1]
        val c = quadCoeffs[0]

        // Calculate R^2 coefficient of determination
        val meanY = ys.average()
        var ssTot = 0.0
        var ssRes = 0.0
        for (i in 0 until n) {
            val yPred = a * xs[i].pow(2.0) + b * xs[i] + c
            ssRes += (ys[i] - yPred).pow(2.0)
            ssTot += (ys[i] - meanY).pow(2.0)
        }
        val r2 = if (ssTot > 0) (1.0 - (ssRes / ssTot)).toFloat().coerceIn(0f, 1f) else 0.8f

        val latexStr = if (abs(a) < 0.0001) {
            val mStr = String.format("%.2f", b)
            val cStr = String.format("%.2f", c)
            "y = ${mStr}x + $cStr"
        } else {
            val aStr = String.format("%.4f", a)
            val bStr = String.format("%.2f", b)
            val cStr = String.format("%.2f", c)
            "y = ${aStr}x^2 + ${bStr}x + $cStr"
        }

        // Generate smooth curve points across domain
        val curvePoints = mutableListOf<StrokePoint>()
        val numSteps = 100
        val step = (maxX - minX) / numSteps
        val now = System.currentTimeMillis()

        for (i in 0..numSteps) {
            val currX = minX + i * step
            val currY = (a * currX.toDouble().pow(2.0) + b * currX.toDouble() + c).toFloat()
            curvePoints.add(
                StrokePoint(
                    x = currX,
                    y = currY,
                    pressure = 0.6f,
                    tilt = 0f,
                    timestampMs = now + i * 10
                )
            )
        }

        return PlottedFunctionResult(
            latexFormula = latexStr,
            curvePoints = curvePoints,
            rSquared = r2
        )
    }

    private fun polyfit2(x: DoubleArray, y: DoubleArray): DoubleArray {
        val n = x.size
        var s0 = n.toDouble()
        var s1 = 0.0
        var s2 = 0.0
        var s3 = 0.0
        var s4 = 0.0
        var t0 = 0.0
        var t1 = 0.0
        var t2 = 0.0

        for (i in 0 until n) {
            val xi = x[i]
            val yi = y[i]
            val xi2 = xi * xi
            val xi3 = xi2 * xi
            val xi4 = xi3 * xi

            s1 += xi
            s2 += xi2
            s3 += xi3
            s4 += xi4

            t0 += yi
            t1 += xi * yi
            t2 += xi2 * yi
        }

        // Solve 3x3 system for [c, b, a]^T
        val det = s0 * (s2 * s4 - s3 * s3) - s1 * (s1 * s4 - s2 * s3) + s2 * (s1 * s3 - s2 * s2)
        if (abs(det) < 1e-12) {
            // Linear fallback
            val b = (n * t1 - s1 * t0) / (n * s2 - s1 * s1 + 1e-6)
            val c = (t0 - b * s1) / n
            return doubleArrayOf(c, b, 0.0)
        }

        val c = (t0 * (s2 * s4 - s3 * s3) - s1 * (t1 * s4 - t2 * s3) + s2 * (t1 * s3 - t2 * s2)) / det
        val b = (s0 * (t1 * s4 - t2 * s3) - t0 * (s1 * s4 - s2 * s3) + s2 * (s1 * t2 - t1 * s2)) / det
        val a = (s0 * (s2 * t2 - s3 * t1) - s1 * (s1 * t2 - s2 * t1) + t0 * (s1 * s3 - s2 * s2)) / det

        return doubleArrayOf(c, b, a)
    }
}
