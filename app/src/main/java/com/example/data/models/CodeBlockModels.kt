package com.example.data.models

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class CodeLanguage(val displayName: String) {
    PYTHON("Python"),
    C("C"),
    CPP("C++")
}

@JsonClass(generateAdapter = true)
data class CodeBlockEntity(
    val id: String = UUID.randomUUID().toString(),
    val language: CodeLanguage = CodeLanguage.PYTHON,
    val source: String,
    val consoleOutput: String = "",
    val diagnostics: List<String> = emptyList(),
    val x: Float,
    val y: Float,
    val width: Float = 520f,
    val height: Float = 320f,
    val rotation: Float = 0f,
    val lastRunAt: Long? = null
)
