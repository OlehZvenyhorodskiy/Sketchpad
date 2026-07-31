package com.example.ai

/**
 * Single source of truth for AI model names used across the app.
 * Update this object when new model versions are released.
 *
 * Prevents model name duplication between AiProvider.kt, HandwritingOcrService.kt,
 * and other consumers.
 */
object AiModelDefaults {
    // ── Google Gemini ──────────────────────────────────────────
    const val GEMINI_DEFAULT = "gemini-3.5-flash"
    val GEMINI_MODELS = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
    )

    // ── OpenAI ─────────────────────────────────────────────────
    const val OPENAI_DEFAULT = "gpt-4o-mini"
    val OPENAI_MODELS = listOf(
        "gpt-4.1",
        "gpt-4.1-mini",
        "gpt-4.1-nano",
        "gpt-4o",
        "gpt-4o-mini",
        "o3",
        "o3-mini",
        "o4-mini",
    )

    // ── Anthropic Claude ───────────────────────────────────────
    const val ANTHROPIC_DEFAULT = "claude-sonnet-4-20250514"
    val ANTHROPIC_MODELS = listOf(
        "claude-sonnet-4-20250514",
        "claude-opus-4-20250514",
        "claude-3-5-haiku-20241022",
    )

    // ── DeepSeek ───────────────────────────────────────────────
    const val DEEPSEEK_DEFAULT = "deepseek-chat"
    val DEEPSEEK_MODELS = listOf(
        "deepseek-chat",
        "deepseek-reasoner",
    )

    // ── OCR (used by HandwritingOcrService) ────────────────────
    const val OCR_MODEL = GEMINI_DEFAULT
}
