package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  themeStyle: AppThemeStyle = AppThemeStyle.SYSTEM_DEFAULT,
  accentColor: Color = Color(0xFF38BDF8),
  content: @Composable () -> Unit,
) {
  val spec = ThemeSpecs.forStyle(themeStyle, darkTheme, accentColor)

  CompositionLocalProvider(LocalThemeSpec provides spec) {
    MaterialTheme(colorScheme = spec.colorScheme, typography = Typography, content = content)
  }
}
