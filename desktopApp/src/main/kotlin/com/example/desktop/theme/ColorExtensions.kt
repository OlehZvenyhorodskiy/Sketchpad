package com.example.desktop.theme

import androidx.compose.ui.graphics.Color
import com.example.shared.model.HslaColor

fun HslaColor.toColor(): Color {
    return Color.hsl(
        hue = hue.coerceIn(0f, 360f),
        saturation = saturation.coerceIn(0f, 1f),
        lightness = lightness.coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}
