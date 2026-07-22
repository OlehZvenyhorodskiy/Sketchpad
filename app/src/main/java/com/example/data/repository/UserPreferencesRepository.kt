package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_LAST_TOOL = stringPreferencesKey("last_tool")
        val KEY_PEN_WIDTH = floatPreferencesKey("pen_width")
        val KEY_PEN_OPACITY = floatPreferencesKey("pen_opacity")
        val KEY_COLOR_HUE = floatPreferencesKey("color_hue")
        val KEY_COLOR_SAT = floatPreferencesKey("color_sat")
        val KEY_COLOR_VAL = floatPreferencesKey("color_val")
        val KEY_DRAW_WITH_FINGERS = booleanPreferencesKey("draw_with_fingers")
        val KEY_THEME_MODE = intPreferencesKey("theme_mode") // 0: SYSTEM, 1: LIGHT, 2: DARK
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL]
    }

    val userName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME]
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

    suspend fun setLoggedIn(loggedIn: Boolean, email: String? = null, name: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = loggedIn
            if (email != null) prefs[KEY_USER_EMAIL] = email else prefs.remove(KEY_USER_EMAIL)
            if (name != null) prefs[KEY_USER_NAME] = name else prefs.remove(KEY_USER_NAME)
        }
    }

    suspend fun setDrawWithFingers(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRAW_WITH_FINGERS] = enabled
        }
    }

    suspend fun saveStrokeSettings(width: Float, opacity: Float, tool: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PEN_WIDTH] = width
            prefs[KEY_PEN_OPACITY] = opacity
            if (tool != null) prefs[KEY_LAST_TOOL] = tool
        }
    }
}

