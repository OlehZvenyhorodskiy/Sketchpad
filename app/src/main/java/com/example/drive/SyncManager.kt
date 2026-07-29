package com.example.drive

import android.content.Context
import com.example.data.models.CanvasEntity
import com.example.data.repository.CanvasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncResult(
    val success: Boolean,
    val localUpdated: Boolean,
    val cloudCopyCreated: Boolean,
    val message: String
)

object SyncManager {

    suspend fun syncCanvas(
        context: Context,
        repository: CanvasRepository,
        canvasId: String,
        cloudLastModifiedMs: Long? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val localCanvas = repository.getCanvasByIdSync(canvasId)
                ?: return@withContext SyncResult(false, localUpdated = false, cloudCopyCreated = false, message = "Локальну канву не знайдено")

            if (cloudLastModifiedMs != null && cloudLastModifiedMs > localCanvas.updatedAt) {
                val cloudCopyTitle = "${localCanvas.title} (Cloud copy)"
                repository.duplicateCanvas(canvasId)
                SyncResult(
                    success = true,
                    localUpdated = false,
                    cloudCopyCreated = true,
                    message = "Збережено хмарну копію: $cloudCopyTitle"
                )
            } else {
                SyncResult(
                    success = true,
                    localUpdated = true,
                    cloudCopyCreated = false,
                    message = "Локальна версія актуальна"
                )
            }
        } catch (e: Exception) {
            SyncResult(false, localUpdated = false, cloudCopyCreated = false, message = "Помилка синхронізації: ${e.message}")
        }
    }
}
