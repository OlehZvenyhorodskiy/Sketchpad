package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.models.AudioRecordingEntity
import com.example.data.models.BackgroundPattern
import com.example.data.models.CanvasEntity
import com.example.data.models.ChartElementEntity
import com.example.data.models.HslaColor
import com.example.data.models.PageEntity
import com.example.data.models.PageSizePreset
import com.example.data.models.ShapeEntity
import com.example.data.models.ShapeType
import com.example.data.models.StrokeEntity
import com.example.data.models.StrokePoint
import com.example.data.models.TextBlockEntity
import com.example.data.models.ToolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CanvasRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val canvasDao = db.canvasDao()
    private val pageDao = db.pageDao()
    private val audioDao = db.audioDao()

    val allCanvases: Flow<List<CanvasEntity>> = canvasDao.getAllCanvases()

    fun searchCanvases(query: String): Flow<List<CanvasEntity>> {
        return if (query.isBlank()) {
            canvasDao.getAllCanvases()
        } else {
            canvasDao.searchCanvases(query)
        }
    }

    fun getCanvasById(id: String): Flow<CanvasEntity?> = canvasDao.getCanvasById(id)

    suspend fun getCanvasByIdSync(id: String): CanvasEntity? = canvasDao.getCanvasByIdSync(id)

    fun getPagesForCanvas(canvasId: String): Flow<List<PageEntity>> = pageDao.getPagesForCanvas(canvasId)

    suspend fun getPagesForCanvasSync(canvasId: String): List<PageEntity> = pageDao.getPagesForCanvasSync(canvasId)

    fun getRecordingsForCanvas(canvasId: String): Flow<List<AudioRecordingEntity>> =
        audioDao.getRecordingsForCanvas(canvasId)

    suspend fun createNewCanvas(
        title: String = "Нова канва",
        pageSizePreset: PageSizePreset = PageSizePreset.UNLIMITED,
        pattern: BackgroundPattern = BackgroundPattern.BLANK,
        bgColor: Int = 0xFFFFFFFF.toInt()
    ): String = withContext(Dispatchers.IO) {
        val canvasId = UUID.randomUUID().toString()
        val canvas = CanvasEntity(
            id = canvasId,
            title = title,
            pageSizePreset = pageSizePreset,
            backgroundPattern = pattern,
            backgroundColor = bgColor,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val initialPage = PageEntity(
            id = UUID.randomUUID().toString(),
            canvasId = canvasId,
            pageIndex = 0
        )
        canvasDao.insertCanvas(canvas)
        pageDao.insertPage(initialPage)
        return@withContext canvasId
    }

    suspend fun createTemplateCanvas(templateType: String): String = withContext(Dispatchers.IO) {
        val canvasId = UUID.randomUUID().toString()
        var title = "Чистий аркуш"
        var pattern = BackgroundPattern.BLANK
        var shapes = emptyList<ShapeEntity>()
        var charts = emptyList<ChartElementEntity>()
        var textBlocks = emptyList<TextBlockEntity>()

        when (templateType) {
            "GRID_CHART" -> {
                title = "Шаблон з графіком"
                pattern = BackgroundPattern.DOTTED
                val chart = ChartElementEntity(
                    x = 100f,
                    y = 100f,
                    width = 500f,
                    height = 350f,
                    axisRangeX = 10f,
                    axisRangeY = 10f,
                    gridStep = 25f,
                    title = "Функція y = f(x)"
                )
                charts = listOf(chart)
            }
            "LECTURE_NOTES" -> {
                title = "Конспект Корнелла"
                pattern = BackgroundPattern.LINED
                val shape = ShapeEntity(
                    shapeType = ShapeType.SQUARE,
                    x = 40f,
                    y = 40f,
                    width = 180f,
                    height = 700f,
                    fillColor = 0x116366F1,
                    strokeColor = 0xFF6366F1.toInt(),
                    strokeWidth = 2f
                )
                val text = TextBlockEntity(
                    text = "Поля для ключових слів / запитань",
                    x = 50f,
                    y = 60f,
                    width = 160f,
                    height = 60f,
                    fontSize = 14f,
                    color = 0xFF475569.toInt()
                )
                shapes = listOf(shape)
                textBlocks = listOf(text)
            }
        }

        val canvas = CanvasEntity(
            id = canvasId,
            title = title,
            pageSizePreset = PageSizePreset.UNLIMITED,
            backgroundPattern = pattern,
            backgroundColor = 0xFFFFFFFF.toInt(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val page = PageEntity(
            id = UUID.randomUUID().toString(),
            canvasId = canvasId,
            pageIndex = 0,
            shapes = shapes,
            charts = charts,
            textBlocks = textBlocks
        )

        canvasDao.insertCanvas(canvas)
        pageDao.insertPage(page)
        return@withContext canvasId
    }

    suspend fun updateCanvas(canvas: CanvasEntity) = withContext(Dispatchers.IO) {
        canvasDao.updateCanvas(canvas.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updatePage(page: PageEntity) = withContext(Dispatchers.IO) {
        pageDao.updatePage(page)
        val canvas = canvasDao.getCanvasByIdSync(page.canvasId)
        if (canvas != null) {
            canvasDao.updateCanvas(canvas.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun addPage(canvasId: String): PageEntity = withContext(Dispatchers.IO) {
        val existingPages = pageDao.getPagesForCanvasSync(canvasId)
        val nextIndex = existingPages.size
        val newPage = PageEntity(
            id = UUID.randomUUID().toString(),
            canvasId = canvasId,
            pageIndex = nextIndex
        )
        pageDao.insertPage(newPage)
        val canvas = canvasDao.getCanvasByIdSync(canvasId)
        if (canvas != null) {
            canvasDao.updateCanvas(canvas.copy(updatedAt = System.currentTimeMillis()))
        }
        return@withContext newPage
    }

    suspend fun insertPages(pages: List<PageEntity>) = withContext(Dispatchers.IO) {
        pageDao.insertPages(pages)
    }

    suspend fun duplicatePage(pageId: String) = withContext(Dispatchers.IO) {
        val original = pageDao.getPageByIdSync(pageId) ?: return@withContext
        val canvasPages = pageDao.getPagesForCanvasSync(original.canvasId)
        val newPage = original.copy(
            id = UUID.randomUUID().toString(),
            pageIndex = canvasPages.size
        )
        pageDao.insertPage(newPage)
    }

    suspend fun deletePage(page: PageEntity) = withContext(Dispatchers.IO) {
        pageDao.deletePage(page.id)
    }

    suspend fun duplicateCanvas(canvasId: String) = withContext(Dispatchers.IO) {
        val original = canvasDao.getCanvasByIdSync(canvasId) ?: return@withContext
        val pages = pageDao.getPagesForCanvasSync(canvasId)

        val newCanvasId = UUID.randomUUID().toString()
        val newCanvas = original.copy(
            id = newCanvasId,
            title = "${original.title} (копія)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            driveFileId = null
        )

        val newPages = pages.mapIndexed { index, p ->
            p.copy(
                id = UUID.randomUUID().toString(),
                canvasId = newCanvasId,
                pageIndex = index
            )
        }

        canvasDao.insertCanvas(newCanvas)
        pageDao.insertPages(newPages)
    }

    suspend fun deleteCanvas(canvasId: String) = withContext(Dispatchers.IO) {
        canvasDao.deleteCanvas(canvasId)
        pageDao.deletePagesForCanvas(canvasId)
        audioDao.deleteRecordingsForCanvas(canvasId)
    }

    suspend fun saveThumbnail(canvasId: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "thumb_$canvasId.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            }
            val canvas = canvasDao.getCanvasByIdSync(canvasId)
            if (canvas != null) {
                canvasDao.updateCanvas(canvas.copy(thumbnailPath = file.absolutePath))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveAudioRecording(
        canvasId: String,
        tempFilePath: String,
        durationMs: Long
    ): AudioRecordingEntity = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, "audio_${UUID.randomUUID()}.m4a")
        File(tempFilePath).copyTo(targetFile, overwrite = true)
        val recording = AudioRecordingEntity(
            canvasId = canvasId,
            filePath = targetFile.absolutePath,
            durationMs = durationMs
        )
        audioDao.insertRecording(recording)
        return@withContext recording
    }

    suspend fun deleteAudioRecording(recording: AudioRecordingEntity) = withContext(Dispatchers.IO) {
        audioDao.deleteRecording(recording.id)
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveImportedImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, "img_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        return@withContext targetFile.absolutePath
    }
}
