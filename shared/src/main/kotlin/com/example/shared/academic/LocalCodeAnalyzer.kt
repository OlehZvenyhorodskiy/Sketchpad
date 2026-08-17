package com.example.shared.academic

import com.example.shared.model.CodeLanguage

data class CodeAnalysisResult(
    val isValid: Boolean,
    val diagnostics: List<String>,
    val estimatedOutput: String
)

object LocalCodeAnalyzer {

    fun analyze(source: String, language: CodeLanguage): CodeAnalysisResult {
        val diagnostics = mutableListOf<String>()
        val lines = source.lines()

        when (language) {
            CodeLanguage.PYTHON -> {
                var openParens = 0
                var openBrackets = 0
                var openBraces = 0

                lines.forEachIndexed { idx, line ->
                    val lineNum = idx + 1
                    openParens += line.count { it == '(' } - line.count { it == ')' }
                    openBrackets += line.count { it == '[' } - line.count { it == ']' }
                    openBraces += line.count { it == '{' } - line.count { it == '}' }

                    val trimmed = line.trim()
                    if ((trimmed.startsWith("def ") || trimmed.startsWith("if ") ||
                                trimmed.startsWith("for ") || trimmed.startsWith("while ") ||
                                trimmed.startsWith("class ")) && !trimmed.endsWith(":") && !trimmed.contains("#")
                    ) {
                        diagnostics.add("Line $lineNum: Missing colon ':' at end of block header")
                    }
                }

                if (openParens != 0) diagnostics.add("Unbalanced parentheses ()")
                if (openBrackets != 0) diagnostics.add("Unbalanced square brackets []")
                if (openBraces != 0) diagnostics.add("Unbalanced curly braces {}")

                val simulatedOutput = simulatePythonOutput(source)
                return CodeAnalysisResult(
                    isValid = diagnostics.isEmpty(),
                    diagnostics = diagnostics,
                    estimatedOutput = simulatedOutput
                )
            }
            CodeLanguage.C, CodeLanguage.CPP -> {
                var openBraces = 0
                lines.forEachIndexed { idx, line ->
                    val lineNum = idx + 1
                    openBraces += line.count { it == '{' } - line.count { it == '}' }
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("//") &&
                        !trimmed.endsWith(";") && !trimmed.endsWith("{") && !trimmed.endsWith("}") &&
                        !trimmed.startsWith("/*") && !trimmed.endsWith("*/")
                    ) {
                        diagnostics.add("Line $lineNum: Possible missing semicolon ';'")
                    }
                }
                if (openBraces != 0) diagnostics.add("Unbalanced curly braces {}")
                return CodeAnalysisResult(
                    isValid = diagnostics.isEmpty(),
                    diagnostics = diagnostics,
                    estimatedOutput = "Binary compiled successfully (0 errors, ${diagnostics.size} warnings)"
                )
            }
        }
    }

    private fun simulatePythonOutput(source: String): String {
        val output = StringBuilder()
        val printRegex = Regex("""print\s*\(\s*["'](.*?)["']\s*\)""")
        printRegex.findAll(source).forEach { match ->
            output.appendLine(match.groupValues[1])
        }
        return output.toString().ifBlank { "Executed with exit code 0" }
    }
}
