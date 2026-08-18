package com.example.desktop.storage

import com.example.shared.model.CanvasEntity
import com.example.shared.model.PageEntity
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AutosavePayload(
    val canvas: CanvasEntity,
    val pages: List<PageEntity>,
    val timestamp: Long = System.currentTimeMillis()
)

class DesktopAutosaveManager(
    private val getCanvasState: () -> Pair<CanvasEntity, List<PageEntity>>,
    private val onAutosaveCompleted: (Long) -> Unit = {}
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val autosaveDir: File by lazy {
        val appData = System.getenv("APPDATA")
        val baseDir = if (!appData.isNullOrBlank()) {
            File(appData, "Sketchpad/autosave")
        } else {
            File(System.getProperty("user.home"), ".sketchpad/autosave")
        }
        baseDir.mkdirs()
        baseDir
    }

    private val autosaveFile: File by lazy { File(autosaveDir, "current_session.sketchpad") }
    private val backupFile: File by lazy { File(autosaveDir, "current_session.sketchpad.bak") }
    private val tempFile: File by lazy { File(autosaveDir, "current_session.sketchpad.tmp") }

    private var autosaveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startPeriodicAutosave(intervalMs: Long = 30_000L) {
        autosaveJob?.cancel()
        autosaveJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                saveImmediately()
            }
        }
    }

    fun stopAutosave() {
        autosaveJob?.cancel()
    }

    fun saveImmediately(): Boolean {
        return try {
            val (canvas, pages) = getCanvasState()
            val payload = AutosavePayload(canvas = canvas, pages = pages)
            val jsonString = json.encodeToString(payload)

            // 1. Write to temp file
            tempFile.writeText(jsonString)

            // 2. Backup existing file if exists
            if (autosaveFile.exists()) {
                autosaveFile.copyTo(backupFile, overwrite = true)
            }

            // 3. Atomic rename temp -> target
            if (tempFile.renameTo(autosaveFile) || (tempFile.copyTo(autosaveFile, overwrite = true).also { tempFile.delete() }.exists())) {
                onAutosaveCompleted(System.currentTimeMillis())
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun restoreLastSession(): AutosavePayload? {
        return try {
            val target = if (autosaveFile.exists()) autosaveFile else if (backupFile.exists()) backupFile else null
            if (target != null && target.length() > 0) {
                val content = target.readText()
                json.decodeFromString<AutosavePayload>(content)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun clearSession() {
        try {
            autosaveFile.delete()
            backupFile.delete()
            tempFile.delete()
        } catch (_: Exception) {}
    }
}
