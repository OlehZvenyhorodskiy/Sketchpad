package com.example.academic.code

import com.example.data.models.CodeLanguage
import java.util.Locale
import kotlin.math.pow

data class CodeDiagnostic(
    val line: Int,
    val message: String,
    val severity: Severity = Severity.ERROR
) {
    enum class Severity { INFO, WARNING, ERROR }
}

data class CodeRunResult(
    val output: String,
    val diagnostics: List<CodeDiagnostic>,
    val executedStatements: Int
) {
    val isSuccess: Boolean
        get() = diagnostics.none { it.severity == CodeDiagnostic.Severity.ERROR }
}

/**
 * A deliberately small, deterministic interpreter for study snippets.
 *
 * It never executes native code and never accesses files, the network, or Android APIs. The
 * supported subset is intentionally clear: scalar assignments, arithmetic, string values and
 * Python `print`, C `printf`, or C++ `cout`. Unsupported control flow is reported instead of being
 * silently ignored, which makes the component useful as a first-pass code checker on a canvas.
 */
object LocalCodeAnalyzer {
    private const val MAX_SOURCE_LENGTH = 20_000
    private const val MAX_STATEMENTS = 500
    private const val MAX_OUTPUT_LENGTH = 20_000

    fun run(source: String, language: CodeLanguage): CodeRunResult {
        if (source.length > MAX_SOURCE_LENGTH) {
            return CodeRunResult(
                output = "",
                diagnostics = listOf(CodeDiagnostic(1, "Code block is too large (maximum $MAX_SOURCE_LENGTH characters).")),
                executedStatements = 0
            )
        }

        val environment = linkedMapOf<String, Value>()
        val output = StringBuilder()
        val diagnostics = mutableListOf<CodeDiagnostic>()
        var executed = 0

        val statements = normaliseStatements(source, language)
        for ((lineNumber, rawStatement) in statements) {
            if (executed >= MAX_STATEMENTS) {
                diagnostics += CodeDiagnostic(lineNumber, "Execution limit reached.")
                break
            }

            val statement = rawStatement.trim()
            if (statement.isBlank() || isBoilerplate(statement, language)) continue

            if (looksLikeUnsupportedControlFlow(statement)) {
                diagnostics += CodeDiagnostic(
                    lineNumber,
                    "Loops, functions and conditional blocks are not supported by the local study runner yet.",
                    CodeDiagnostic.Severity.WARNING
                )
                continue
            }

            try {
                when {
                    language == CodeLanguage.PYTHON && statement.startsWith("print") -> {
                        val body = callBody(statement, "print")
                        appendLineCapped(output, splitArguments(body).joinToString(" ") {
                            ExpressionParser(it, environment).parse().display()
                        })
                    }

                    language == CodeLanguage.C && statement.startsWith("printf") -> {
                        val body = callBody(statement, "printf")
                        appendCapped(output, evaluatePrintf(body, environment))
                    }

                    language == CodeLanguage.CPP && statement.contains("cout") -> {
                        val coutBody = statement.substringAfter("cout").trim().removePrefix("<<").trim()
                        val pieces = splitOutsideStrings(coutBody, "<<")
                        pieces.forEach { piece ->
                            val token = piece.trim()
                            if (token == "endl" || token == "std::endl") {
                                appendLineCapped(output, "")
                            } else if (token.isNotEmpty()) {
                                appendCapped(output, ExpressionParser(token, environment).parse().display())
                            }
                        }
                    }

                    assignmentParts(statement, language) != null -> {
                        val (name, expression) = assignmentParts(statement, language)!!
                        require(name.matches(Regex("[A-Za-z_]\\w*"))) { "Invalid variable name '$name'." }
                        environment[name] = ExpressionParser(expression, environment).parse()
                    }

                    statement == "pass" || statement.startsWith("return") -> Unit

                    else -> {
                        ExpressionParser(statement, environment).parse()
                    }
                }
                executed++
            } catch (error: IllegalArgumentException) {
                diagnostics += CodeDiagnostic(lineNumber, error.message ?: "Invalid statement.")
            } catch (_: ArithmeticException) {
                diagnostics += CodeDiagnostic(lineNumber, "Arithmetic error.")
            }
        }

        return CodeRunResult(
            output = output.toString().trimEnd('\n'),
            diagnostics = diagnostics,
            executedStatements = executed
        )
    }

    private fun normaliseStatements(source: String, language: CodeLanguage): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        source.lines().forEachIndexed { index, originalLine ->
            val withoutComment = stripComment(originalLine, if (language == CodeLanguage.PYTHON) "#" else "//")
            if (language == CodeLanguage.PYTHON) {
                result += (index + 1) to withoutComment.trim()
            } else {
                splitOutsideStrings(withoutComment, ";").forEach { statement ->
                    result += (index + 1) to statement.trim()
                }
            }
        }
        return result
    }

    private fun stripComment(line: String, marker: String): String {
        var quote: Char? = null
        var escaped = false
        var index = 0
        while (index <= line.length - marker.length) {
            val char = line[index]
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '\'' || char == '"') {
                quote = if (quote == char) null else if (quote == null) char else quote
            } else if (quote == null && line.startsWith(marker, index)) {
                return line.substring(0, index)
            }
            index++
        }
        return line
    }

    private fun isBoilerplate(statement: String, language: CodeLanguage): Boolean {
        val compact = statement.replace(" ", "")
        return statement == "{" || statement == "}" ||
            compact.matches(Regex("intmain\\((?:void)?\\)\\{?")) ||
            statement.startsWith("#include") ||
            (language == CodeLanguage.CPP && statement.startsWith("using namespace"))
    }

    private fun looksLikeUnsupportedControlFlow(statement: String): Boolean {
        val keyword = statement.substringBefore('(').substringBefore(' ').trim()
        return keyword in setOf("if", "else", "for", "while", "do", "switch", "def", "class", "try", "catch") ||
            statement.endsWith(":")
    }

    private fun callBody(statement: String, function: String): String {
        val prefix = statement.indexOf(function) + function.length
        val open = statement.indexOf('(', prefix)
        val close = statement.lastIndexOf(')')
        require(open >= 0 && close > open) { "Expected $function(...)." }
        return statement.substring(open + 1, close)
    }

    private fun assignmentParts(statement: String, language: CodeLanguage): Pair<String, String>? {
        val withoutDeclaration = if (language == CodeLanguage.PYTHON) {
            statement
        } else {
            statement.replaceFirst(
                Regex("^(?:const\\s+)?(?:unsigned\\s+|signed\\s+)?(?:int|long|float|double|char|string|std::string|auto|bool)\\s+"),
                ""
            )
        }

        var quote: Char? = null
        var depth = 0
        withoutDeclaration.forEachIndexed { index, char ->
            if ((char == '\'' || char == '"') && (index == 0 || withoutDeclaration[index - 1] != '\\')) {
                quote = if (quote == char) null else if (quote == null) char else quote
            } else if (quote == null) {
                if (char == '(') depth++
                if (char == ')') depth--
                if (char == '=' && depth == 0) {
                    val previous = withoutDeclaration.getOrNull(index - 1)
                    val next = withoutDeclaration.getOrNull(index + 1)
                    if (previous !in listOf('=', '!', '<', '>') && next != '=') {
                        return withoutDeclaration.substring(0, index).trim() to
                            withoutDeclaration.substring(index + 1).trim()
                    }
                }
            }
        }
        return null
    }

    private fun evaluatePrintf(body: String, environment: Map<String, Value>): String {
        val arguments = splitArguments(body)
        require(arguments.isNotEmpty()) { "printf needs a format string." }
        val formatValue = ExpressionParser(arguments.first(), environment).parse()
        require(formatValue is Value.Text) { "The first printf argument must be a string." }
        val values = arguments.drop(1).map { ExpressionParser(it, environment).parse() }
        var valueIndex = 0
        val regex = Regex("%(?:\\.(\\d+))?([dfs%])")
        return regex.replace(formatValue.value) { match ->
            if (match.groupValues[2] == "%") return@replace "%"
            require(valueIndex < values.size) { "Not enough values for printf format." }
            val value = values[valueIndex++]
            when (match.groupValues[2]) {
                "d" -> value.asNumber().toLong().toString()
                "f" -> {
                    val precision = match.groupValues[1].toIntOrNull() ?: 6
                    String.format(Locale.US, "%.${precision}f", value.asNumber())
                }
                else -> value.display()
            }
        }.replace("\\n", "\n").replace("\\t", "\t")
    }

    private fun splitArguments(value: String): List<String> = splitOutsideStrings(value, ",")

    private fun splitOutsideStrings(value: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var quote: Char? = null
        var escaped = false
        var depth = 0
        var start = 0
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '\'' || char == '"') {
                quote = if (quote == char) null else if (quote == null) char else quote
            } else if (quote == null) {
                if (char == '(') depth++
                if (char == ')') depth--
                if (depth == 0 && value.startsWith(delimiter, index)) {
                    result += value.substring(start, index)
                    index += delimiter.length
                    start = index
                    continue
                }
            }
            index++
        }
        result += value.substring(start)
        return result.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun appendCapped(output: StringBuilder, text: String) {
        if (output.length >= MAX_OUTPUT_LENGTH) return
        output.append(text.take(MAX_OUTPUT_LENGTH - output.length))
    }

    private fun appendLineCapped(output: StringBuilder, text: String) {
        appendCapped(output, text)
        appendCapped(output, "\n")
    }

    private sealed interface Value {
        fun display(): String
        fun asNumber(): Double = throw IllegalArgumentException("Expected a number.")

        data class Number(val value: Double) : Value {
            override fun display(): String = if (value.isFinite() && value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                value.toString()
            }

            override fun asNumber(): Double = value
        }

        data class Text(val value: String) : Value {
            override fun display(): String = value
        }
    }

    private class ExpressionParser(
        private val source: String,
        private val environment: Map<String, Value>
    ) {
        private var index = 0

        fun parse(): Value {
            val result = parseAdditive()
            skipWhitespace()
            require(index == source.length) { "Unexpected token near '${source.substring(index).take(12)}'." }
            return result
        }

        private fun parseAdditive(): Value {
            var left = parseMultiplicative()
            while (true) {
                skipWhitespace()
                left = when {
                    consume('+') -> plus(left, parseMultiplicative())
                    consume('-') -> Value.Number(left.asNumber() - parseMultiplicative().asNumber())
                    else -> return left
                }
            }
        }

        private fun parseMultiplicative(): Value {
            var left = parsePower()
            while (true) {
                skipWhitespace()
                left = when {
                    consume('*') -> Value.Number(left.asNumber() * parsePower().asNumber())
                    consume('/') -> {
                        val divisor = parsePower().asNumber()
                        require(divisor != 0.0) { "Division by zero." }
                        Value.Number(left.asNumber() / divisor)
                    }
                    consume('%') -> {
                        val divisor = parsePower().asNumber()
                        require(divisor != 0.0) { "Division by zero." }
                        Value.Number(left.asNumber() % divisor)
                    }
                    else -> return left
                }
            }
        }

        private fun parsePower(): Value {
            val left = parseUnary()
            skipWhitespace()
            return if (consume('^')) {
                Value.Number(left.asNumber().pow(parsePower().asNumber()))
            } else left
        }

        private fun parseUnary(): Value {
            skipWhitespace()
            return when {
                consume('+') -> Value.Number(parseUnary().asNumber())
                consume('-') -> Value.Number(-parseUnary().asNumber())
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Value {
            skipWhitespace()
            require(index < source.length) { "Expected an expression." }
            val char = source[index]
            if (char == '\'' || char == '"') return parseString(char)
            if (char.isDigit() || char == '.') return parseNumber()
            if (char.isLetter() || char == '_') return parseIdentifier()
            if (consume('(')) {
                val nested = parseAdditive()
                skipWhitespace()
                require(consume(')')) { "Missing closing parenthesis." }
                return nested
            }
            throw IllegalArgumentException("Unexpected character '$char'.")
        }

        private fun parseString(quote: Char): Value.Text {
            index++
            val result = StringBuilder()
            var escaped = false
            while (index < source.length) {
                val char = source[index++]
                if (escaped) {
                    result.append(
                        when (char) {
                            'n' -> '\n'
                            't' -> '\t'
                            else -> char
                        }
                    )
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == quote) {
                    return Value.Text(result.toString())
                } else {
                    result.append(char)
                }
            }
            throw IllegalArgumentException("Unterminated string.")
        }

        private fun parseNumber(): Value.Number {
            val start = index
            while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
            val number = source.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number.")
            return Value.Number(number)
        }

        private fun parseIdentifier(): Value {
            val start = index
            while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
            val name = source.substring(start, index)
            return when (name) {
                "true", "True" -> Value.Number(1.0)
                "false", "False" -> Value.Number(0.0)
                else -> environment[name] ?: throw IllegalArgumentException("Unknown variable '$name'.")
            }
        }

        private fun plus(left: Value, right: Value): Value = when {
            left is Value.Text || right is Value.Text -> Value.Text(left.display() + right.display())
            else -> Value.Number(left.asNumber() + right.asNumber())
        }

        private fun consume(expected: Char): Boolean {
            if (index < source.length && source[index] == expected) {
                index++
                return true
            }
            return false
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }
    }
}
