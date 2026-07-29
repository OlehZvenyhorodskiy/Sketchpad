package com.example.data.storage

import android.content.Context
import com.example.brush.BrushProfile
import com.example.data.models.ColorPalette
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@JsonClass(generateAdapter = true)
data class AppSettingsExport(
    val themeStyleOrdinal: Int,
    val accentColorArgb: Int,
    val leftHandedMode: Boolean,
    val customBrushes: List<BrushProfile>,
    val colorPalettes: List<ColorPalette>
)

object SettingsBackupManager {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(AppSettingsExport::class.java)

    suspend fun exportSettings(
        context: Context,
        themeStyleOrdinal: Int,
        accentColorArgb: Int,
        leftHandedMode: Boolean,
        customBrushes: List<BrushProfile>,
        colorPalettes: List<ColorPalette>
    ): String = withContext(Dispatchers.IO) {
        val export = AppSettingsExport(
            themeStyleOrdinal = themeStyleOrdinal,
            accentColorArgb = accentColorArgb,
            leftHandedMode = leftHandedMode,
            customBrushes = customBrushes,
            colorPalettes = colorPalettes
        )
        val json = adapter.toJson(export)
        val file = File(context.filesDir, ".sketchpad-settings.json")
        file.writeText(json)
        file.absolutePath
    }

    suspend fun importSettings(jsonString: String): AppSettingsExport? = withContext(Dispatchers.IO) {
        try {
            adapter.fromJson(jsonString)
        } catch (_: Exception) {
            null
        }
    }
}
