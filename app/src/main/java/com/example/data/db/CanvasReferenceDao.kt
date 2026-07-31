package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.models.CanvasReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasReferenceDao {
    @Query("SELECT * FROM canvas_references WHERE sourcePageId = :pageId ORDER BY updatedAt DESC")
    fun observeOutgoingFromPage(pageId: String): Flow<List<CanvasReferenceEntity>>

    @Query("SELECT * FROM canvas_references WHERE targetPageId = :pageId ORDER BY updatedAt DESC")
    fun observeIncomingToPage(pageId: String): Flow<List<CanvasReferenceEntity>>

    @Query("SELECT * FROM canvas_references WHERE sourceCanvasId = :canvasId ORDER BY updatedAt DESC")
    fun observeOutgoingFromCanvas(canvasId: String): Flow<List<CanvasReferenceEntity>>

    @Query("SELECT * FROM canvas_references WHERE targetCanvasId = :canvasId ORDER BY updatedAt DESC")
    fun observeIncomingToCanvas(canvasId: String): Flow<List<CanvasReferenceEntity>>

    @Query("SELECT * FROM canvas_references WHERE sourcePageId = :pageId ORDER BY updatedAt DESC")
    suspend fun getOutgoingFromPage(pageId: String): List<CanvasReferenceEntity>

    @Query("SELECT * FROM canvas_references WHERE targetPageId = :pageId ORDER BY updatedAt DESC")
    suspend fun getIncomingToPage(pageId: String): List<CanvasReferenceEntity>

    @Query("SELECT * FROM canvas_references WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CanvasReferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reference: CanvasReferenceEntity)

    @Delete
    suspend fun delete(reference: CanvasReferenceEntity)

    @Delete
    suspend fun deleteAll(references: List<CanvasReferenceEntity>)

    @Query("DELETE FROM canvas_references WHERE id = :id")
    suspend fun deleteById(id: String)
}
