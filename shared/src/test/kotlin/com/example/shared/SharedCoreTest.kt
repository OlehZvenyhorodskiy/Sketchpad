package com.example.shared

import com.example.shared.academic.FunctionPlotterEngine
import com.example.shared.academic.MathExpressionEvaluator
import com.example.shared.academic.SpacedRepetitionScheduler
import com.example.shared.core.DrawingMath
import com.example.shared.model.HslaColor
import com.example.shared.model.StrokeEntity
import com.example.shared.model.StrokePoint
import com.example.shared.model.SymmetryMode
import com.example.shared.model.ToolType
import com.example.shared.protocol.SketchLinkPacket
import com.example.shared.protocol.SketchLinkPacketType
import org.junit.Assert.*
import org.junit.Test

class SharedCoreTest {

    @Test
    fun testHslaColorConversion() {
        val red = HslaColor(0f, 1f, 0.5f, 1f)
        val argb = red.toArgbInt()
        assertEquals(0xFFFF0000.toInt(), argb)

        val reconstructed = HslaColor.fromArgb(argb)
        assertEquals(0f, reconstructed.hue, 1f)
        assertEquals(1f, reconstructed.saturation, 0.05f)
        assertEquals(0.5f, reconstructed.lightness, 0.05f)
    }

    @Test
    fun testCatmullRomInterpolation() {
        val points = listOf(
            StrokePoint(0f, 0f),
            StrokePoint(10f, 10f),
            StrokePoint(20f, 0f),
            StrokePoint(30f, 10f)
        )
        val smooth = DrawingMath.interpolateCatmullRom(points, segmentsPerPoint = 4)
        assertTrue(smooth.size > points.size)
        assertEquals(0f, smooth.first().x, 0.001f)
    }

    @Test
    fun testSymmetryGeneration() {
        val stroke = StrokeEntity(
            tool = ToolType.PEN,
            colorHsla = HslaColor.BLACK,
            baseWidth = 2f,
            points = listOf(StrokePoint(100f, 100f), StrokePoint(150f, 150f))
        )
        val quad = DrawingMath.generateSymmetricStrokes(stroke, SymmetryMode.QUAD, centerX = 200f, centerY = 200f)
        assertEquals(4, quad.size)
        assertEquals(300f, quad[1].points[0].x, 0.001f) // vertical mirror x = 2*200 - 100 = 300
    }

    @Test
    fun testMathEvaluator() {
        val result = MathExpressionEvaluator.evaluate("2 + 3 * 4")
        assertEquals(14.0, result, 0.001)

        val sinVal = MathExpressionEvaluator.evaluate("sin(0)")
        assertEquals(0.0, sinVal, 0.001)
    }

    @Test
    fun testFunctionPlotter() {
        val points = FunctionPlotterEngine.generatePlotData("sin(x)", xMin = -3.14, xMax = 3.14, samples = 50)
        assertEquals(50, points.size)
    }

    @Test
    fun testSpacedRepetition() {
        val schedule1 = SpacedRepetitionScheduler.calculateNextReview(rating = 4, currentRepetitions = 0, currentIntervalDays = 1, currentEaseFactor = 2.5f)
        assertEquals(1, schedule1.repetitions)
        assertEquals(1, schedule1.intervalDays)

        val schedule2 = SpacedRepetitionScheduler.calculateNextReview(rating = 5, currentRepetitions = 1, currentIntervalDays = 1, currentEaseFactor = schedule1.easeFactor)
        assertEquals(2, schedule2.repetitions)
        assertEquals(6, schedule2.intervalDays)
    }

    @Test
    fun testSketchLinkSerialization() {
        val packet = SketchLinkPacket(
            type = SketchLinkPacketType.HANDSHAKE_REQUEST,
            pin = "123456"
        )
        val json = packet.toJson()
        val decoded = SketchLinkPacket.fromJson(json)
        assertNotNull(decoded)
        assertEquals(SketchLinkPacketType.HANDSHAKE_REQUEST, decoded?.type)
        assertEquals("123456", decoded?.pin)
    }
}
