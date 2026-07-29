package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.models.CustomBrushEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBrushDao {
    @Query("SELECT * FROM custom_brushes ORDER BY createdAt DESC")
    fun getAllCustomBrushes(): Flow<List<CustomBrushEntity>>

    @Query("SELECT * FROM custom_brushes ORDER BY createdAt DESC")
    suspend fun getAllCustomBrushesSync(): List<CustomBrushEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomBrush(brush: CustomBrushEntity)

    @Query("DELETE FROM custom_brushes WHERE id = :id")
    suspend fun deleteCustomBrush(id: String)
}
