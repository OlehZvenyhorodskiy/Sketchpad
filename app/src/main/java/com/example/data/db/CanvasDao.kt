package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.AudioRecordingEntity
import com.example.data.models.CanvasEntity
import com.example.data.models.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasDao {
    @Query("SELECT * FROM canvases ORDER BY updatedAt DESC")
    fun getAllCanvases(): Flow<List<CanvasEntity>>

    @Query("SELECT * FROM canvases WHERE id = :id")
    fun getCanvasById(id: String): Flow<CanvasEntity?>

    @Query("SELECT * FROM canvases WHERE id = :id")
    suspend fun getCanvasByIdSync(id: String): CanvasEntity?

    @Query("SELECT * FROM canvases WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchCanvases(query: String): Flow<List<CanvasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvas(canvas: CanvasEntity)

    @Update
    suspend fun updateCanvas(canvas: CanvasEntity)

    @Query("DELETE FROM canvases WHERE id = :id")
    suspend fun deleteCanvas(id: String)
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE canvasId = :canvasId ORDER BY pageIndex ASC")
    fun getPagesForCanvas(canvasId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE canvasId = :canvasId ORDER BY pageIndex ASC")
    suspend fun getPagesForCanvasSync(canvasId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getPageByIdSync(id: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deletePage(id: String)

    @Query("DELETE FROM pages WHERE canvasId = :canvasId")
    suspend fun deletePagesForCanvas(canvasId: String)
}

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio_recordings WHERE canvasId = :canvasId ORDER BY recordedAt DESC")
    fun getRecordingsForCanvas(canvasId: String): Flow<List<AudioRecordingEntity>>

    @Query("SELECT * FROM audio_recordings WHERE canvasId = :canvasId ORDER BY recordedAt DESC")
    suspend fun getRecordingsForCanvasSync(canvasId: String): List<AudioRecordingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: AudioRecordingEntity)

    @Query("DELETE FROM audio_recordings WHERE id = :id")
    suspend fun deleteRecording(id: String)

    @Query("DELETE FROM audio_recordings WHERE canvasId = :canvasId")
    suspend fun deleteRecordingsForCanvas(canvasId: String)
}
