package com.example.shared.academic

import kotlin.math.*

object MathExpressionEvaluator {

    fun evaluate(expression: String, xValue: Double = 0.0): Double {
        val clean = expression.replace(" ", "").lowercase()
            .replace("x", "($xValue)")
            .replace("pi", Math.PI.toString())
            .replace("e", Math.E.toString())

        return Parser(clean).parse()
    }

    private class Parser(private val str: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            pos++
            ch = if (pos < str.length) str[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected: $ch")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        x = if (divisor == 0.0) Double.NaN else x / divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return +parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = str.substring(startPos, pos).toDoubleOrNull() ?: 0.0
            } else if (ch in 'a'..'z') {
                while (ch in 'a'..'z') nextChar()
                val func = str.substring(startPos, pos)
                if (eat('(')) {
                    x = parseExpression()
                    eat(')')
                } else {
                    x = parseFactor()
                }
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(x)
                    "cos" -> cos(x)
                    "tan" -> tan(x)
                    "asin" -> asin(x)
                    "acos" -> acos(x)
                    "atan" -> atan(x)
                    "log", "ln" -> ln(x)
                    "log10" -> log10(x)
                    "abs" -> abs(x)
                    "exp" -> exp(x)
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
            } else {
                throw IllegalArgumentException("Unexpected: $ch")
            }

            if (eat('^')) x = x.pow(parseFactor())

            return x
        }
    }
}
