package com.example.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

enum class AppLanguage(
    val languageTag: String,
    val nativeName: String,
    val flag: String
) {
    ENGLISH("en", "English", "🇬🇧"),
    UKRAINIAN("uk", "Українська", "🇺🇦"),
    DUTCH("nl", "Nederlands", "🇳🇱"),
    GERMAN("de", "Deutsch", "🇩🇪"),
    FRENCH("fr", "Français", "🇫🇷");

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage = entries.firstOrNull {
            it.languageTag.equals(tag, ignoreCase = true)
        } ?: ENGLISH
    }
}

/** Applies the saved locale before Compose reads any resources. */
object AppLocaleManager {
    private const val PREFS_NAME = "localization_preferences"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_LANGUAGE_SELECTED = "language_selected"

    fun currentLanguage(context: Context): AppLanguage {
        val languageTag = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.ENGLISH.languageTag)
        return AppLanguage.fromLanguageTag(languageTag)
    }

    fun hasSelectedLanguage(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LANGUAGE_SELECTED, false)

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.languageTag)
            .putBoolean(KEY_LANGUAGE_SELECTED, true)
            .apply()
    }

    fun wrap(baseContext: Context): Context {
        val locale = Locale.forLanguageTag(currentLanguage(baseContext).languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
        }
        return baseContext.createConfigurationContext(configuration)
    }
}
