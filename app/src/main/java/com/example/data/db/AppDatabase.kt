package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.models.AudioRecordingEntity
import com.example.data.models.CanvasEntity
import com.example.data.models.PageEntity

@Database(
    entities = [CanvasEntity::class, PageEntity::class, AudioRecordingEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(MoshiConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun canvasDao(): CanvasDao
    abstract fun pageDao(): PageDao
    abstract fun audioDao(): AudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mecanvas_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
