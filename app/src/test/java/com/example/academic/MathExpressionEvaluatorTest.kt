package com.example.academic

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

class MathExpressionEvaluatorTest {

    @Test
    fun testQuadraticExpression() {
        // f(x) = x*x + 2*x + 1
        val result = MathExpressionEvaluator.eval("x*x + 2*x + 1", 3.0)
        assertEquals(16.0, result, 0.001)
    }

    @Test
    fun testTrigonometricExpression() {
        // f(x) = sin(2*x)
        val result = MathExpressionEvaluator.eval("sin(2*x)", PI / 4)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun testCosineHalfAngle() {
        // f(x) = cos(x/2)
        val result = MathExpressionEvaluator.eval("cos(x/2)", 0.0)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun testImplicitMultiplication() {
        // f(x) = 2x^2 + 3x + 1
        val result = MathExpressionEvaluator.eval("2x^2 + 3x + 1", 2.0)
        assertEquals(15.0, result, 0.001)
    }
}
