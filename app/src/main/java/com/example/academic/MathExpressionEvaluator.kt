package com.example.academic

import kotlin.math.*

object MathExpressionEvaluator {
    fun eval(expression: String, x: Double): Double {
        return try {
            val tokens = tokenize(expression, x)
            val rpn = toRpn(tokens)
            evaluateRpn(rpn)
        } catch (e: Exception) {
            0.0
        }
    }

    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Op(val symbol: Char, val precedence: Int, val rightAssociative: Boolean = false) : Token()
        data class Fn(val name: String) : Token()
        object LParen : Token()
        object RParen : Token()
    }

    private fun tokenize(expr: String, x: Double): List<Token> {
        val clean = expr.lowercase().replace(" ", "")
        val result = mutableListOf<Token>()
        var i = 0
        var prevToken: Token? = null

        while (i < clean.length) {
            val c = clean[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < clean.length && (clean[i].isDigit() || clean[i] == '.')) {
                        i++
                    }
                    val num = clean.substring(start, i).toDoubleOrNull() ?: 0.0
                    checkImplicitMul(result, prevToken)
                    val t = Token.Num(num)
                    result.add(t)
                    prevToken = t
                }
                c == 'x' -> {
                    checkImplicitMul(result, prevToken)
                    val t = Token.Num(x)
                    result.add(t)
                    prevToken = t
                    i++
                }
                c == 'p' && clean.startsWith("pi", i) -> {
                    checkImplicitMul(result, prevToken)
                    val t = Token.Num(PI)
                    result.add(t)
                    prevToken = t
                    i += 2
                }
                c == 'e' && (i + 1 == clean.length || !clean[i + 1].isLetter()) -> {
                    checkImplicitMul(result, prevToken)
                    val t = Token.Num(E)
                    result.add(t)
                    prevToken = t
                    i++
                }
                c.isLetter() -> {
                    val start = i
                    while (i < clean.length && clean[i].isLetter()) {
                        i++
                    }
                    val name = clean.substring(start, i)
                    checkImplicitMul(result, prevToken)
                    val t = Token.Fn(name)
                    result.add(t)
                    prevToken = t
                }
                c == '+' || c == '-' -> {
                    if (prevToken == null || prevToken is Token.Op || prevToken is Token.LParen) {
                        if (c == '-') {
                            result.add(Token.Num(0.0))
                            val t = Token.Op('-', 1)
                            result.add(t)
                            prevToken = t
                        }
                    } else {
                        val t = Token.Op(c, 1)
                        result.add(t)
                        prevToken = t
                    }
                    i++
                }
                c == '*' || c == '/' || c == '%' -> {
                    val t = Token.Op(c, 2)
                    result.add(t)
                    prevToken = t
                    i++
                }
                c == '^' -> {
                    val t = Token.Op('^', 3, rightAssociative = true)
                    result.add(t)
                    prevToken = t
                    i++
                }
                c == '(' -> {
                    checkImplicitMul(result, prevToken)
                    val t = Token.LParen
                    result.add(t)
                    prevToken = t
                    i++
                }
                c == ')' -> {
                    val t = Token.RParen
                    result.add(t)
                    prevToken = t
                    i++
                }
                else -> i++
            }
        }
        return result
    }

    private fun checkImplicitMul(result: MutableList<Token>, prevToken: Token?) {
        if (prevToken is Token.Num || prevToken is Token.RParen) {
            result.add(Token.Op('*', 2))
        }
    }

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = java.util.ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Num -> output.add(token)
                is Token.Fn -> stack.push(token)
                is Token.Op -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.peek()
                        if (top is Token.Op && ((!token.rightAssociative && token.precedence <= top.precedence) ||
                                                (token.rightAssociative && token.precedence < top.precedence))) {
                            output.add(stack.pop())
                        } else if (top is Token.Fn) {
                            output.add(stack.pop())
                        } else {
                            break
                        }
                    }
                    stack.push(token)
                }
                is Token.LParen -> stack.push(token)
                is Token.RParen -> {
                    while (stack.isNotEmpty() && stack.peek() !is Token.LParen) {
                        output.add(stack.pop())
                    }
                    if (stack.isNotEmpty() && stack.peek() is Token.LParen) {
                        stack.pop()
                    }
                    if (stack.isNotEmpty() && stack.peek() is Token.Fn) {
                        output.add(stack.pop())
                    }
                }
            }
        }
        while (stack.isNotEmpty()) {
            val top = stack.pop()
            if (top !is Token.LParen && top !is Token.RParen) {
                output.add(top)
            }
        }
        return output
    }

    private fun evaluateRpn(rpn: List<Token>): Double {
        val stack = java.util.ArrayDeque<Double>()

        for (token in rpn) {
            when (token) {
                is Token.Num -> stack.push(token.value)
                is Token.Op -> {
                    val b = if (stack.isNotEmpty()) stack.pop() else 0.0
                    val a = if (stack.isNotEmpty()) stack.pop() else 0.0
                    val res = when (token.symbol) {
                        '+' -> a + b
                        '-' -> a - b
                        '*' -> a * b
                        '/' -> if (b != 0.0) a / b else 0.0
                        '%' -> a % b
                        '^' -> a.pow(b)
                        else -> 0.0
                    }
                    stack.push(res)
                }
                is Token.Fn -> {
                    val a = if (stack.isNotEmpty()) stack.pop() else 0.0
                    val res = when (token.name) {
                        "sin" -> sin(a)
                        "cos" -> cos(a)
                        "tan" -> tan(a)
                        "asin" -> asin(a)
                        "acos" -> acos(a)
                        "atan" -> atan(a)
                        "sqrt" -> if (a >= 0) sqrt(a) else 0.0
                        "abs" -> abs(a)
                        "ln" -> if (a > 0) ln(a) else 0.0
                        "log" -> if (a > 0) log10(a) else 0.0
                        "exp" -> exp(a)
                        else -> a
                    }
                    stack.push(res)
                }
                else -> {}
            }
        }
        return if (stack.isNotEmpty()) stack.pop() else 0.0
    }
}
