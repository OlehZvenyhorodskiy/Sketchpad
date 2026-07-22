package com.example.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.CanvasEntity
import com.example.data.models.ImageElementEntity
import com.example.data.models.PageEntity
import com.example.data.repository.CanvasRepository
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val repository: CanvasRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val userEmail: StateFlow<String?> = userPrefsRepository.userEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val userName: StateFlow<String?> = userPrefsRepository.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val isLoggedIn: StateFlow<Boolean> = userPrefsRepository.isLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun logout() {
        viewModelScope.launch {
            userPrefsRepository.setLoggedIn(false)
        }
    }

    val canvases: StateFlow<List<CanvasEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            repository.searchCanvases(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun createNewCanvas(
        title: String = "Нова канва",
        pageSizePreset: com.example.data.models.PageSizePreset = com.example.data.models.PageSizePreset.UNLIMITED,
        pattern: com.example.data.models.BackgroundPattern = com.example.data.models.BackgroundPattern.DOTTED,
        bgColor: Int = 0xFF121212.toInt(),
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createNewCanvas(
                title = title,
                pageSizePreset = pageSizePreset,
                pattern = pattern,
                bgColor = bgColor
            )
            onCreated(id)
        }
    }

    fun createTemplateCanvas(templateType: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = repository.createTemplateCanvas(templateType)
            onCreated(id)
        }
    }

    fun renameCanvas(canvas: CanvasEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateCanvas(canvas.copy(title = newTitle))
        }
    }

    fun duplicateCanvas(canvasId: String) {
        viewModelScope.launch {
            repository.duplicateCanvas(canvasId)
        }
    }

    fun deleteCanvas(canvasId: String) {
        viewModelScope.launch {
            repository.deleteCanvas(canvasId)
        }
    }

    fun importPdfOrImage(context: Context, uri: Uri, onCreated: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().lowercase().contains(".pdf")
            val title = if (isPdf) "Імпортована лекція (PDF)" else "Імпортоване фото"
            val canvasId = repository.createNewCanvas(title = title)

            if (isPdf) {
                val pages = mutableListOf<PageEntity>()
                try {
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        val pdfRenderer = PdfRenderer(pfd)
                        val pageCount = pdfRenderer.pageCount
                        for (i in 0 until pageCount) {
                            val pdfPage = pdfRenderer.openPage(i)
                            val w = pdfPage.width * 2
                            val h = pdfPage.height * 2
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pdfPage.close()

                            val imgFile = File(context.filesDir, "pdf_page_${canvasId}_$i.png")
                            FileOutputStream(imgFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                            }

                            val imageEntity = ImageElementEntity(
                                id = UUID.randomUUID().toString(),
                                sourceUri = imgFile.absolutePath,
                                x = 0f,
                                y = 0f,
                                width = pdfPage.width.toFloat(),
                                height = pdfPage.height.toFloat()
                            )

                            pages.add(
                                PageEntity(
                                    id = UUID.randomUUID().toString(),
                                    canvasId = canvasId,
                                    pageIndex = i,
                                    images = listOf(imageEntity)
                                )
                            )
                        }
                        pdfRenderer.close()
                        pfd.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (pages.isNotEmpty()) {
                    repository.insertPages(pages)
                }
            } else {
                val imagePath = repository.saveImportedImage(uri)
                val pages = repository.getPagesForCanvasSync(canvasId)
                if (pages.isNotEmpty()) {
                    val updatedPage = pages.first().copy(
                        images = listOf(
                            ImageElementEntity(
                                id = UUID.randomUUID().toString(),
                                sourceUri = imagePath,
                                x = 50f,
                                y = 50f,
                                width = 600f,
                                height = 800f
                            )
                        )
                    )
                    repository.updatePage(updatedPage)
                }
            }

            withContext(Dispatchers.Main) {
                onCreated(canvasId)
            }
        }
    }
}
