package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Theme-aware panel wrapper. Automatically applies the correct surface style
 * based on the current [AppThemeStyle] from [LocalThemeSpec].
 *
 * Replaces boilerplate Surface() calls across all floating panels so that
 * theme changes propagate uniformly without copy-paste in each component.
 */
@Composable
fun ThemedPanel(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 6.dp,
    shadowElevation: Dp = 8.dp,
    surfaceAlpha: Float = 0.95f,
    paperRotation: Float = -1.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val spec = LocalThemeSpec.current
    val shape = RoundedCornerShape(spec.cornerRadius)

    when (spec.style) {
        AppThemeStyle.IOS_GLASS -> {
            GlassSurface(
                modifier = modifier,
                cornerRadius = spec.cornerRadius,
                content = content
            )
        }

        AppThemeStyle.NEUMORPHISM -> {
            Box(
                modifier = modifier
                    .neumorphic(cornerRadius = spec.cornerRadius)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface),
                content = content
            )
        }

        AppThemeStyle.PAPER_NOTEBOOK -> {
            Surface(
                modifier = modifier.rotate(paperRotation),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = surfaceAlpha),
                shadowElevation = shadowElevation,
                tonalElevation = tonalElevation,
                content = { Box(content = content) }
            )
        }

        else -> {
            val borderMod = when (spec.borderStyle) {
                BorderStyle.THIN_LIGHT -> Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape
                )
                BorderStyle.THIN_GLOW -> Modifier.border(
                    1.dp,
                    spec.accentColor.copy(alpha = 0.25f),
                    shape
                )
                BorderStyle.NONE -> Modifier
            }

            Surface(
                modifier = modifier.then(borderMod),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = surfaceAlpha),
                shadowElevation = shadowElevation,
                tonalElevation = tonalElevation,
                content = { Box(content = content) }
            )
        }
    }
}
