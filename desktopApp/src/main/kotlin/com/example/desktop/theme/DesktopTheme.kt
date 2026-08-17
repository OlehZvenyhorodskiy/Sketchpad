package com.example.desktop.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppThemeStyle {
    SYSTEM_DEFAULT,
    PAPER_NOTEBOOK,
    NEUMORPHISM,
    AMOLED_BLACK,
    CHALKBOARD,
    SEPIA_EINK,
    MIDNIGHT_INDIGO,
    FOREST_STUDY,
    ROSE_QUARTZ,
    HIGH_CONTRAST
}

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
    val elevationStyle: ElevationStyle,
    val accentColor: Color,
    val borderStyle: BorderStyle
)

val LocalThemeSpec = compositionLocalOf {
    DesktopThemeSpecs.forStyle(AppThemeStyle.SYSTEM_DEFAULT, isDark = true, accentColor = Color(0xFF38BDF8))
}

object DesktopThemeSpecs {
    fun forStyle(style: AppThemeStyle, isDark: Boolean, accentColor: Color = Color(0xFF38BDF8)): ThemeSpec {
        return when (style) {
            AppThemeStyle.SYSTEM_DEFAULT -> ThemeSpec(
                style = style,
                colorScheme = if (isDark) {
                    darkColorScheme(
                        primary = accentColor,
                        secondary = Color(0xFFB8C4FF),
                        tertiary = Color(0xFF7ED8D4),
                        background = Color(0xFF0F172A),
                        surface = Color(0xFF1E293B),
                        surfaceContainer = Color(0xFF334155),
                        onBackground = Color(0xFFF8FAFC),
                        onSurface = Color(0xFFF8FAFC)
                    )
                } else {
                    lightColorScheme(
                        primary = accentColor,
                        secondary = Color(0xFF555BC2),
                        background = Color(0xFFF8FAFC),
                        surface = Color(0xFFFFFFFF),
                        surfaceContainer = Color(0xFFF1F5F9),
                        onBackground = Color(0xFF0F172A),
                        onSurface = Color(0xFF0F172A)
                    )
                },
                cornerRadius = 14.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = accentColor,
                borderStyle = BorderStyle.THIN_LIGHT
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
                    background = Color(0xFF242830),
                    onSurface = Color(0xFFE2E8F0)
                ) else lightColorScheme(
                    primary = accentColor,
                    surface = Color(0xFFE0E5EC),
                    surfaceContainer = Color(0xFFD1D9E6),
                    background = Color(0xFFE0E5EC),
                    onSurface = Color(0xFF1E293B)
                ),
                cornerRadius = 16.dp,
                elevationStyle = ElevationStyle.NEUMORPHIC_DOUBLE_SHADOW,
                accentColor = accentColor,
                borderStyle = BorderStyle.NONE
            )
            AppThemeStyle.AMOLED_BLACK -> ThemeSpec(
                style = style,
                colorScheme = darkColorScheme(
                    primary = accentColor,
                    surface = Color(0xFF000000),
                    surfaceContainer = Color(0xFF121212),
                    background = Color(0xFF000000),
                    onBackground = Color.White,
                    onSurface = Color.White
                ),
                cornerRadius = 10.dp,
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
                cornerRadius = 6.dp,
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
                elevationStyle = ElevationStyle.FLAT,
                accentColor = Color(0xFF4A3F35),
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.MIDNIGHT_INDIGO -> ThemeSpec(
                style = style,
                colorScheme = darkColorScheme(
                    primary = accentColor,
                    secondary = Color(0xFFAFC6FF),
                    background = Color(0xFF0D1020),
                    surface = Color(0xFF151A32),
                    surfaceContainer = Color(0xFF1C2341),
                    onBackground = Color(0xFFF0F3FF),
                    onSurface = Color(0xFFF0F3FF)
                ),
                cornerRadius = 14.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = accentColor,
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.FOREST_STUDY -> ThemeSpec(
                style = style,
                colorScheme = lightColorScheme(
                    primary = Color(0xFF166534),
                    secondary = Color(0xFF427A56),
                    background = Color(0xFFF5F8F1),
                    surface = Color(0xFFFBFDF8),
                    surfaceContainer = Color(0xFFE8F0E4),
                    onBackground = Color(0xFF172219),
                    onSurface = Color(0xFF172219)
                ),
                cornerRadius = 12.dp,
                elevationStyle = ElevationStyle.SOFT_SHADOW,
                accentColor = Color(0xFF166534),
                borderStyle = BorderStyle.THIN_LIGHT
            )
            AppThemeStyle.ROSE_QUARTZ -> ThemeSpec(
                style = style,
                colorScheme = lightColorScheme(
                    primary = Color(0xFF9D174D),
                    secondary = Color(0xFFA74870),
                    background = Color(0xFFFFF7FA),
                    surface = Color(0xFFFFFBFF),
                    surfaceContainer = Color(0xFFFBEAF0),
                    onBackground = Color(0xFF2A1820),
                    onSurface = Color(0xFF2A1820)
                ),
                cornerRadius = 16.dp,
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
                        surfaceContainer = Color(0xFF181818),
                        onBackground = Color.White,
                        onSurface = Color.White
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF0000CC),
                        secondary = Color(0xFF006666),
                        background = Color.White,
                        surface = Color.White,
                        surfaceContainer = Color(0xFFF0F0F0),
                        onBackground = Color.Black,
                        onSurface = Color.Black
                    )
                },
                cornerRadius = 4.dp,
                elevationStyle = ElevationStyle.FLAT,
                accentColor = if (isDark) Color(0xFFFFFF00) else Color(0xFF0000CC),
                borderStyle = BorderStyle.THIN_LIGHT
            )
        }
    }
}

@Composable
fun SketchpadDesktopTheme(
    style: AppThemeStyle = AppThemeStyle.SYSTEM_DEFAULT,
    isDark: Boolean = true,
    accentColor: Color = Color(0xFF38BDF8),
    content: @Composable () -> Unit
) {
    val spec = DesktopThemeSpecs.forStyle(style, isDark, accentColor)
    CompositionLocalProvider(LocalThemeSpec provides spec) {
        MaterialTheme(
            colorScheme = spec.colorScheme,
            content = content
        )
    }
}
