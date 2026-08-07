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
                colorScheme = if (isDark) {
                    darkColorScheme(
                        primary = accentColor,
                        secondary = Color(0xFFB8C4FF),
                        tertiary = Color(0xFF7ED8D4),
                        background = Color(0xFF0D1117),
                        surface = Color(0xFF111722),
                        surfaceContainer = Color(0xFF171E2B),
                        surfaceContainerHigh = Color(0xFF202938),
                        surfaceContainerHighest = Color(0xFF293445),
                        onBackground = Color(0xFFE6EAF2),
                        onSurface = Color(0xFFE6EAF2),
                        outline = Color(0xFF8490A3),
                        outlineVariant = Color(0xFF3A4658)
                    )
                } else {
                    lightColorScheme(
                        primary = accentColor,
                        secondary = Color(0xFF555BC2),
                        tertiary = Color(0xFF006B68),
                        background = Color(0xFFF6F7FB),
                        surface = Color(0xFFFCFCFF),
                        surfaceContainer = Color(0xFFF0F2F8),
                        surfaceContainerHigh = Color(0xFFE9ECF4),
                        surfaceContainerHighest = Color(0xFFE1E5EF),
                        onBackground = Color(0xFF171B24),
                        onSurface = Color(0xFF171B24),
                        outline = Color(0xFF737B8C),
                        outlineVariant = Color(0xFFC5CAD6)
                    )
                },
                cornerRadius = 22.dp,
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
            AppThemeStyle.MIDNIGHT_INDIGO -> ThemeSpec(
                style = style,
                colorScheme = darkColorScheme(
                    primary = accentColor,
                    secondary = Color(0xFFAFC6FF),
                    tertiary = Color(0xFF9CE7D8),
                    background = Color(0xFF0D1020),
                    surface = Color(0xFF151A32),
                    surfaceContainer = Color(0xFF1C2341),
                    surfaceContainerHigh = Color(0xFF252E52),
                    surfaceContainerHighest = Color(0xFF303B66),
                    onBackground = Color(0xFFF0F3FF),
                    onSurface = Color(0xFFF0F3FF),
                    outline = Color(0xFF9AA7D7),
                    outlineVariant = Color(0xFF424E76)
                ),
                cornerRadius = 16.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = accentColor,
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.FOREST_STUDY -> ThemeSpec(
                style = style,
                colorScheme = lightColorScheme(
                    primary = Color(0xFF166534),
                    secondary = Color(0xFF427A56),
                    tertiary = Color(0xFF8A4B15),
                    background = Color(0xFFF5F8F1),
                    surface = Color(0xFFFBFDF8),
                    surfaceContainer = Color(0xFFE8F0E4),
                    surfaceContainerHigh = Color(0xFFDCE8D8),
                    surfaceContainerHighest = Color(0xFFCFE0CA),
                    onBackground = Color(0xFF172219),
                    onSurface = Color(0xFF172219),
                    outline = Color(0xFF68766A),
                    outlineVariant = Color(0xFFB9C7B7)
                ),
                cornerRadius = 14.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = Color(0xFF166534),
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.ROSE_QUARTZ -> ThemeSpec(
                style = style,
                colorScheme = lightColorScheme(
                    primary = Color(0xFF9D174D),
                    secondary = Color(0xFFA74870),
                    tertiary = Color(0xFF7C3D97),
                    background = Color(0xFFFFF7FA),
                    surface = Color(0xFFFFFBFF),
                    surfaceContainer = Color(0xFFFBEAF0),
                    surfaceContainerHigh = Color(0xFFF4DDE7),
                    surfaceContainerHighest = Color(0xFFECCEDA),
                    onBackground = Color(0xFF2A1820),
                    onSurface = Color(0xFF2A1820),
                    outline = Color(0xFF8C707B),
                    outlineVariant = Color(0xFFDFC0CC)
                ),
                cornerRadius = 24.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = Color(0xFF9D174D),
                borderStyle = BorderStyle.NONE
            )
            AppThemeStyle.HIGH_CONTRAST -> ThemeSpec(
                style = style,
                colorScheme = if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFFFFFF00),
                        secondary = Color(0xFF00FFFF),
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color(0xFF101010),
                        onBackground = Color.White,
                        onSurface = Color.White,
                        outline = Color.White,
                        outlineVariant = Color(0xFFBDBDBD)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF0000CC),
                        secondary = Color(0xFF006666),
                        background = Color.White,
                        surface = Color.White,
                        surfaceContainer = Color(0xFFF3F3F3),
                        onBackground = Color.Black,
                        onSurface = Color.Black,
                        outline = Color.Black,
                        outlineVariant = Color(0xFF444444)
                    )
                },
                cornerRadius = 4.dp,
                surfaceBlurEnabled = false,
                surfaceBlurRadius = 0.dp,
                elevationStyle = ElevationStyle.FLAT,
                accentColor = if (isDark) Color(0xFFFFFF00) else Color(0xFF0000CC),
                borderStyle = BorderStyle.THIN_LIGHT
            )
        }
    }
}
