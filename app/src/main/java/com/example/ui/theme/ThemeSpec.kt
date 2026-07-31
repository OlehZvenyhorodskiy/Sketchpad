package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ElevationStyle {
    FLAT,
    SOFT_SHADOW,
    NEUMORPHIC_DOUBLE_SHADOW
}

enum class BorderStyle {
    NONE,
    THIN_LIGHT,
    THIN_GLOW
}

data class ThemeSpec(
    val style: AppThemeStyle,
    val colorScheme: ColorScheme,
    val cornerRadius: Dp,
    val surfaceBlurEnabled: Boolean,
    val surfaceBlurRadius: Dp,
    val elevationStyle: ElevationStyle,
    val accentColor: Color,
    val borderStyle: BorderStyle
)

val LocalThemeSpec = compositionLocalOf {
    ThemeSpecs.forStyle(AppThemeStyle.SYSTEM_DEFAULT, isDark = false, accentColor = Color(0xFF38BDF8))
}

object ThemeSpecs {
    fun forStyle(style: AppThemeStyle, isDark: Boolean, accentColor: Color = Color(0xFF38BDF8)): ThemeSpec {
        return when (style) {
            AppThemeStyle.SYSTEM_DEFAULT -> ThemeSpec(
                style = style,
                colorScheme = if (isDark) darkColorScheme(primary = accentColor) else lightColorScheme(primary = accentColor),
                cornerRadius = 16.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = accentColor,
                borderStyle = BorderStyle.NONE
            )
            AppThemeStyle.PAPER_NOTEBOOK -> ThemeSpec(
                style = style,
                colorScheme = lightColorScheme(
                    primary = Color(0xFF8C6D46),
                    secondary = Color(0xFFA67C52),
                    surface = Color(0xFFF5F1E8),
                    surfaceContainer = Color(0xFFEFEAD8),
                    background = Color(0xFFF5F1E8),
                    onBackground = Color(0xFF3D3122),
                    onSurface = Color(0xFF3D3122)
                ),
                cornerRadius = 8.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = Color(0xFF8C6D46),
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.NEUMORPHISM -> ThemeSpec(
                style = style,
                colorScheme = if (isDark) darkColorScheme(
                    primary = accentColor,
                    surface = Color(0xFF242830),
                    surfaceContainer = Color(0xFF1F232B),
                    background = Color(0xFF242830)
                ) else lightColorScheme(
                    primary = accentColor,
                    surface = Color(0xFFE0E5EC),
                    surfaceContainer = Color(0xFFD1D9E6),
                    background = Color(0xFFE0E5EC)
                ),
                cornerRadius = 18.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.NEUMORPHIC_DOUBLE_SHADOW,
                accentColor = accentColor,
                borderStyle = BorderStyle.NONE
            )
            AppThemeStyle.AMOLED_BLACK -> ThemeSpec(
                style = style,
                colorScheme = darkColorScheme(
                    primary = accentColor,
                    surface = Color(0xFF000000),
                    surfaceContainer = Color(0xFF0A0A0A),
                    background = Color(0xFF000000),
                    onBackground = Color.White,
                    onSurface = Color.White
                ),
                cornerRadius = 12.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.FLAT,
                accentColor = accentColor,
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.CHALKBOARD -> ThemeSpec(
                style = style,
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFACC15),
                    secondary = Color(0xFF4ADE80),
                    surface = Color(0xFF1B3A2F),
                    surfaceContainer = Color(0xFF142B23),
                    background = Color(0xFF1B3A2F),
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFF1F5F9)
                ),
                cornerRadius = 4.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.FLAT,
                accentColor = Color(0xFFFACC15),
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.SEPIA_EINK -> ThemeSpec(
                style = style,
                colorScheme = lightColorScheme(
                    primary = Color(0xFF4A3F35),
                    secondary = Color(0xFF6B5B4D),
                    surface = Color(0xFFEDE4D3),
                    surfaceContainer = Color(0xFFE2D6C1),
                    background = Color(0xFFEDE4D3),
                    onBackground = Color(0xFF332B25),
                    onSurface = Color(0xFF332B25)
                ),
                cornerRadius = 6.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.FLAT,
                accentColor = Color(0xFF4A3F35),
                borderStyle = BorderStyle.THIN_LIGHT
            )
        }
    }
}
