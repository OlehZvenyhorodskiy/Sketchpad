package com.example.brush.recognizer

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class VectorShapeRecognizerTest {

    @Test
    fun testRecognizeRectangle() {
        val points = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f),
            Offset(0f, 0f)
        )

        val result = VectorShapeRecognizer.recognizeShape(points)
        assertEquals(RecognizedShape.RECTANGLE, result)
    }

    @Test
    fun testRecognizeLine() {
        val points = listOf(
            Offset(0f, 0f),
            Offset(25f, 25f),
            Offset(50f, 50f),
            Offset(75f, 75f),
            Offset(100f, 100f)
        )

        val result = VectorShapeRecognizer.recognizeShape(points)
        assertEquals(RecognizedShape.LINE, result)
    }
}
