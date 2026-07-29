package com.example.data.models

data class ColorPalette(
    val id: String,
    val name: String,
    val colors: List<HslaColor>
) {
    companion object {
        val DEFAULT_PALETTES = listOf(
            ColorPalette(
                id = "classic",
                name = "Класична",
                colors = listOf(
                    HslaColor(0f, 0f, 0f, 1f),
                    HslaColor(0f, 0f, 1f, 1f),
                    HslaColor(0f, 0.85f, 0.5f, 1f),
                    HslaColor(120f, 0.75f, 0.4f, 1f),
                    HslaColor(210f, 0.9f, 0.55f, 1f),
                    HslaColor(50f, 0.95f, 0.5f, 1f)
                )
            ),
            ColorPalette(
                id = "pastel",
                name = "Пастель",
                colors = listOf(
                    HslaColor(350f, 0.6f, 0.85f, 1f),
                    HslaColor(160f, 0.5f, 0.8f, 1f),
                    HslaColor(200f, 0.6f, 0.85f, 1f),
                    HslaColor(45f, 0.7f, 0.85f, 1f),
                    HslaColor(280f, 0.5f, 0.85f, 1f)
                )
            )
        )
    }
}
