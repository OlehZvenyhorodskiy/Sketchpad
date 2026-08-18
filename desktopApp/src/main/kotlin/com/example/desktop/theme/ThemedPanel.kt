package com.example.desktop.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ThemedPanel(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 6.dp,
    shadowElevation: Dp = 8.dp,
    surfaceAlpha: Float = 0.95f,
    paperRotation: Float = -1.0f,
    content: @Composable BoxScope.() -> Unit
) {
    val spec = LocalThemeSpec.current
    val shape = RoundedCornerShape(spec.cornerRadius)

    when (spec.style) {
        AppThemeStyle.NEUMORPHISM -> {
            Box(
                modifier = modifier
                    .neumorphic(cornerRadius = spec.cornerRadius)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha)),
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

        AppThemeStyle.AMOLED_BLACK -> {
            Surface(
                modifier = modifier.border(1.dp, Color(0xFF262626), shape),
                shape = shape,
                color = Color.Black.copy(alpha = surfaceAlpha),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                content = { Box(content = content) }
            )
        }

        AppThemeStyle.CHALKBOARD -> {
            Surface(
                modifier = modifier.border(1.5.dp, Color(0xFF2D5A49), shape),
                shape = shape,
                color = Color(0xFF142B23).copy(alpha = surfaceAlpha),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                content = { Box(content = content) }
            )
        }

        AppThemeStyle.SEPIA_EINK -> {
            Surface(
                modifier = modifier.border(1.dp, Color(0xFFB8A88A), shape),
                shape = shape,
                color = Color(0xFFEDE4D3).copy(alpha = surfaceAlpha),
                shadowElevation = 2.dp,
                tonalElevation = 1.dp,
                content = { Box(content = content) }
            )
        }

        else -> {
            val effectiveShadow = when (spec.elevationStyle) {
                ElevationStyle.FLAT -> 0.dp
                ElevationStyle.SOFT_SHADOW -> shadowElevation
                ElevationStyle.NEUMORPHIC_DOUBLE_SHADOW -> shadowElevation
            }
            val effectiveTonal = when (spec.elevationStyle) {
                ElevationStyle.FLAT -> 0.dp
                ElevationStyle.SOFT_SHADOW -> tonalElevation
                ElevationStyle.NEUMORPHIC_DOUBLE_SHADOW -> tonalElevation
            }

            Surface(
                modifier = modifier.then(
                    if (spec.borderStyle == BorderStyle.THIN_LIGHT) {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            shape
                        )
                    } else Modifier
                ),
                shape = shape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha),
                shadowElevation = effectiveShadow,
                tonalElevation = effectiveTonal,
                content = { Box(content = content) }
            )
        }
    }
}
