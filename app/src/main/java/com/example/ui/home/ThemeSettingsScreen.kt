package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.AppLanguage
import com.example.ui.theme.AppThemeStyle
import com.example.ui.theme.ThemeSpecs

/**
 * Settings screen for selecting visual theme style, accent color, and handedness.
 *
 * Each theme is presented as a preview card showing representative colors
 * and surface style. The accent color can be fine-tuned via an HSL hue slider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    currentThemeOrdinal: Int,
    currentAccentArgb: Int,
    isLeftHanded: Boolean,
    palmRejectionEnabled: Boolean = true,
    currentLanguage: AppLanguage,
    onThemeSelected: (Int) -> Unit,
    onAccentColorChanged: (Int) -> Unit,
    onLeftHandedChanged: (Boolean) -> Unit,
    onPalmRejectionChanged: (Boolean) -> Unit = {},
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    var selectedTheme by remember { mutableIntStateOf(currentThemeOrdinal) }
    var hue by remember {
        val color = Color(currentAccentArgb)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            hsv
        )
        mutableFloatStateOf(hsv[0])
    }
    var leftHanded by remember { mutableStateOf(isLeftHanded) }
    var palmRejection by remember { mutableStateOf(palmRejectionEnabled) }

    val themeEntries = AppThemeStyle.entries.toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_and_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.language_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.language_settings_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppLanguage.entries) { language ->
                    FilterChip(
                        selected = currentLanguage == language,
                        onClick = {
                            if (currentLanguage != language) onLanguageSelected(language)
                        },
                        label = { Text("${language.flag} ${language.nativeName}") }
                    )
                }
            }

            // ──────────────────────────────────────────────
            // Section: Theme Style Picker
            // ──────────────────────────────────────────────
            Text(
                text = stringResource(R.string.theme_style),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(themeEntries) { style ->
                    val ordinal = style.ordinal
                    val spec = ThemeSpecs.forStyle(style, isDark = false)
                    val isSelected = ordinal == selectedTheme

                    ThemePreviewCard(
                        name = themeDisplayName(style),
                        surfaceColor = spec.colorScheme.surface,
                        primaryColor = spec.colorScheme.primary,
                        onSurfaceColor = spec.colorScheme.onSurface,
                        cornerRadius = spec.cornerRadius.value,
                        isSelected = isSelected,
                        onClick = {
                            selectedTheme = ordinal
                            onThemeSelected(ordinal)
                        }
                    )
                }
            }

            // ──────────────────────────────────────────────
            // Section: Accent Color
            // ──────────────────────────────────────────────
            Text(
                text = stringResource(R.string.accent_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val accentColor = Color.hsl(hue, 0.75f, 0.55f)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        val newColor = Color.hsl(it, 0.75f, 0.55f)
                        onAccentColorChanged(newColor.toArgb())
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick accent color presets
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val presets = listOf(
                    Color(0xFF38BDF8) to "Блакитний",
                    Color(0xFFA78BFA) to "Фіолетовий",
                    Color(0xFFF472B6) to "Рожевий",
                    Color(0xFF34D399) to "Зелений",
                    Color(0xFFFBBF24) to "Жовтий",
                    Color(0xFFF87171) to "Червоний"
                )
                presets.forEach { (color, _) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (color.toArgb() == accentColor.toArgb()) 3.dp else 1.dp,
                                color = if (color.toArgb() == accentColor.toArgb())
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable {
                                val hsv = FloatArray(3)
                                android.graphics.Color.RGBToHSV(
                                    (color.red * 255).toInt(),
                                    (color.green * 255).toInt(),
                                    (color.blue * 255).toInt(),
                                    hsv
                                )
                                hue = hsv[0]
                                onAccentColorChanged(color.toArgb())
                            }
                    )
                }
            }

            // ──────────────────────────────────────────────
            // Section: Left-handed Mode
            // ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.left_handed_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.mirror_tool_panels),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = leftHanded,
                    onCheckedChange = {
                        leftHanded = it
                        onLeftHandedChanged(it)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.palm_rejection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.palm_rejection_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = palmRejection,
                    onCheckedChange = {
                        palmRejection = it
                        onPalmRejectionChanged(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    name: String,
    surfaceColor: Color,
    primaryColor: Color,
    onSurfaceColor: Color,
    cornerRadius: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isSelected) primaryColor else Color.Transparent

    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(shape)
            .border(2.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Color preview block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(cornerRadius.dp.coerceAtMost(16.dp)))
                .background(surfaceColor)
                .border(
                    1.dp,
                    onSurfaceColor.copy(alpha = 0.15f),
                    RoundedCornerShape(cornerRadius.dp.coerceAtMost(16.dp))
                )
        ) {
            // Mini accent bar at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(6.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(primaryColor)
            )
            // "Text line" mock
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(onSurfaceColor.copy(alpha = 0.5f))
                )
                Box(
                    Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(onSurfaceColor.copy(alpha = 0.3f))
                )
            }
        }

        // Theme name label
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
        )

        // Selected checkmark
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.selected),
                tint = primaryColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun themeDisplayName(style: AppThemeStyle): String = when (style) {
    AppThemeStyle.SYSTEM_DEFAULT -> stringResource(R.string.theme_system_default)
    AppThemeStyle.PAPER_NOTEBOOK -> stringResource(R.string.theme_paper_notebook)
    AppThemeStyle.NEUMORPHISM -> stringResource(R.string.theme_neumorphism)
    AppThemeStyle.AMOLED_BLACK -> stringResource(R.string.theme_amoled)
    AppThemeStyle.CHALKBOARD -> stringResource(R.string.theme_chalkboard)
    AppThemeStyle.SEPIA_EINK -> stringResource(R.string.theme_sepia)
    AppThemeStyle.MIDNIGHT_INDIGO -> stringResource(R.string.theme_midnight_indigo)
    AppThemeStyle.FOREST_STUDY -> stringResource(R.string.theme_forest_study)
    AppThemeStyle.ROSE_QUARTZ -> stringResource(R.string.theme_rose_quartz)
    AppThemeStyle.HIGH_CONTRAST -> stringResource(R.string.theme_high_contrast)
}
