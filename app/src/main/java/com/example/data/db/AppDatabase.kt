package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.models.AudioRecordingEntity
import com.example.data.models.CanvasEntity
import com.example.data.models.PageEntity

@Database(
    entities = [CanvasEntity::class, PageEntity::class, AudioRecordingEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(MoshiConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun canvasDao(): CanvasDao
    abstract fun pageDao(): PageDao
    abstract fun audioDao(): AudioDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN layers TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE pages ADD COLUMN activeLayerId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE audio_recordings ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sketchpad_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
