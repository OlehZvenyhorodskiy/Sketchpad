package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import com.example.ui.theme.AppThemeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
        val KEY_LAST_TOOL = stringPreferencesKey("last_tool")
        val KEY_PEN_WIDTH = floatPreferencesKey("pen_width")
        val KEY_PEN_OPACITY = floatPreferencesKey("pen_opacity")
        val KEY_COLOR_HUE = floatPreferencesKey("color_hue")
        val KEY_COLOR_SAT = floatPreferencesKey("color_sat")
        val KEY_COLOR_VAL = floatPreferencesKey("color_val")
        val KEY_DRAW_WITH_FINGERS = booleanPreferencesKey("draw_with_fingers")
        val KEY_THEME_MODE = intPreferencesKey("theme_mode") // 0: SYSTEM, 1: LIGHT, 2: DARK
        val KEY_THEME_STYLE = intPreferencesKey("theme_style") // Legacy ordinal from pre-name storage.
        val KEY_THEME_STYLE_V2 = stringPreferencesKey("theme_style_v2")
        val KEY_ACCENT_COLOR = intPreferencesKey("accent_color") // ARGB int
        val KEY_LEFT_HANDED_MODE = booleanPreferencesKey("left_handed_mode")
        val KEY_SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val KEY_CURSOR_SHAPE = intPreferencesKey("cursor_shape") // 0: CIRCLE, 1: RING, 2: CROSSHAIR
        val KEY_CURSOR_SIZE = floatPreferencesKey("cursor_size") // dp
        val KEY_CURSOR_FOLLOWS_ACCENT = booleanPreferencesKey("cursor_follows_accent")
        val KEY_SAVED_PALETTES = stringPreferencesKey("saved_palettes")
    }

    val themeStyle: Flow<Int> = context.dataStore.data.map { prefs ->
        val savedName = prefs[KEY_THEME_STYLE_V2]
        if (savedName != null) {
            try {
                AppThemeStyle.valueOf(savedName).ordinal
            } catch (_: IllegalArgumentException) {
                AppThemeStyle.SYSTEM_DEFAULT.ordinal
            }
        } else {
            legacyThemeStyleOrdinal(prefs[KEY_THEME_STYLE] ?: AppThemeStyle.SYSTEM_DEFAULT.ordinal)
        }
    }

    val accentColor: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT_COLOR] ?: 0xFF38BDF8.toInt()
    }

    val leftHandedMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LEFT_HANDED_MODE] ?: false
    }

    val cursorShape: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_SHAPE] ?: 0
    }

    val cursorSize: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_SIZE] ?: 12f
    }

    val cursorFollowsAccent: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_FOLLOWS_ACCENT] ?: true
    }

    val savedPalettesJson: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_PALETTES]
    }

    suspend fun setCursorSettings(shape: Int, size: Float, followsAccent: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_SHAPE] = shape
            prefs[KEY_CURSOR_SIZE] = size
            prefs[KEY_CURSOR_FOLLOWS_ACCENT] = followsAccent
        }
    }

    suspend fun setSavedPalettesJson(json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_PALETTES] = json
        }
    }

    suspend fun setThemeStyle(styleOrdinal: Int) {
        context.dataStore.edit { prefs ->
            val style = AppThemeStyle.entries.getOrElse(styleOrdinal) { AppThemeStyle.SYSTEM_DEFAULT }
            prefs[KEY_THEME_STYLE_V2] = style.name
        }
    }

    suspend fun setAccentColor(colorInt: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = colorInt
        }
    }

    suspend fun setLeftHandedMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LEFT_HANDED_MODE] = enabled
        }
    }

    val selectedProvider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_PROVIDER] ?: "GEMINI"
    }

    suspend fun setSelectedProvider(provider: String) {
        setSelectedProviderSync(provider)
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_PROVIDER] = provider
        }
    }

    fun getSelectedProviderIdSync(): String {
        return try {
            val prefs = context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
            prefs.getString("selected_provider", "GEMINI") ?: "GEMINI"
        } catch (_: Exception) {
            "GEMINI"
        }
    }

    fun setSelectedProviderSync(providerId: String) {
        try {
            context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
                .edit().putString("selected_provider", providerId).apply()
        } catch (_: Exception) {}
    }

    fun saveEncryptedApiKey(apiKey: String) {
        com.example.ai.GeminiAssistantService.saveApiKey(context, apiKey)
    }

    fun getEncryptedApiKey(): String {
        return com.example.ai.GeminiAssistantService.getApiKey(context)
    }

    val userName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME]
    }

    val userAvatar: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_AVATAR]
    }

    val drawWithFingers: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DRAW_WITH_FINGERS] ?: true
    }

    val penWidth: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_PEN_WIDTH] ?: 4f
    }

    val penOpacity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_PEN_OPACITY] ?: 1f
    }

    val lastTool: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_TOOL]
    }

    suspend fun ensureLocalProfile() {
        context.dataStore.edit { prefs ->
            if (prefs[KEY_USER_NAME].isNullOrBlank()) {
                val suffix = java.util.UUID.randomUUID().toString().take(4).uppercase()
                prefs[KEY_USER_NAME] = context.getString(R.string.generated_student_name, suffix)
            }
            if (prefs[KEY_USER_AVATAR].isNullOrBlank()) {
                prefs[KEY_USER_AVATAR] = listOf("🎓", "📚", "🧠", "✏️", "🦉", "🚀").random()
            }
        }
    }

    suspend fun setLocalProfile(name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name.trim().ifBlank { context.getString(R.string.student) }
            prefs[KEY_USER_AVATAR] = avatar.ifBlank { "🎓" }
        }
    }

    suspend fun setDrawWithFingers(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRAW_WITH_FINGERS] = enabled
        }
    }

    fun getEraserModeSync(): com.example.data.models.EraserMode {
        return try {
            val prefs = context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
            val name = prefs.getString("eraser_mode", com.example.data.models.EraserMode.PIXEL.name)
            com.example.data.models.EraserMode.valueOf(name ?: com.example.data.models.EraserMode.PIXEL.name)
        } catch (_: Exception) {
            com.example.data.models.EraserMode.PIXEL
        }
    }

    fun setEraserModeSync(mode: com.example.data.models.EraserMode) {
        try {
            context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
                .edit().putString("eraser_mode", mode.name).apply()
        } catch (_: Exception) {}
    }

    fun hasExplicitProviderChoice(): Boolean {
        return try {
            val prefs = context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
            prefs.getBoolean("has_explicit_provider_choice", false)
        } catch (_: Exception) { false }
    }

    fun setHasExplicitProviderChoice(hasChoice: Boolean) {
        try {
            context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
                .edit().putBoolean("has_explicit_provider_choice", hasChoice).apply()
        } catch (_: Exception) {}
    }

    fun saveApiKeyForProvider(providerId: String, key: String) {
        com.example.ai.GeminiAssistantService.saveApiKeyForProvider(context, providerId, key)
    }

    fun getApiKeyForProvider(providerId: String): String {
        return com.example.ai.GeminiAssistantService.getApiKeyForProvider(context, providerId)
    }

    fun getCustomEndpoint(providerId: String): String {
        return try {
            context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
                .getString("custom_endpoint_${providerId.lowercase()}", "") ?: ""
        } catch (_: Exception) { "" }
    }

    fun saveCustomEndpoint(providerId: String, endpoint: String) {
        try {
            context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
                .edit().putString("custom_endpoint_${providerId.lowercase()}", endpoint).apply()
        } catch (_: Exception) {}
    }

    fun getCustomModel(providerId: String): String {
        return try {
            context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
                .getString("custom_model_${providerId.lowercase()}", "") ?: ""
        } catch (_: Exception) { "" }
    }

    fun saveCustomModel(providerId: String, model: String) {
        try {
            context.getSharedPreferences("user_prefs_sync", Context.MODE_PRIVATE)
                .edit().putString("custom_model_${providerId.lowercase()}", model).apply()
        } catch (_: Exception) {}
    }

    suspend fun saveStrokeSettings(width: Float, opacity: Float, tool: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PEN_WIDTH] = width
            prefs[KEY_PEN_OPACITY] = opacity
            if (tool != null) prefs[KEY_LAST_TOOL] = tool
        }
    }
}

private fun legacyThemeStyleOrdinal(legacyOrdinal: Int): Int = when (legacyOrdinal) {
    0, 1 -> AppThemeStyle.SYSTEM_DEFAULT.ordinal
    in 2..6 -> legacyOrdinal - 1
    else -> AppThemeStyle.SYSTEM_DEFAULT.ordinal
}

