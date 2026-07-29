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
import com.example.data.models.CustomBrushEntity
import com.example.data.models.PageEntity

@Database(
    entities = [CanvasEntity::class, PageEntity::class, AudioRecordingEntity::class, CustomBrushEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(MoshiConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun canvasDao(): CanvasDao
    abstract fun pageDao(): PageDao
    abstract fun audioDao(): AudioDao
    abstract fun customBrushDao(): CustomBrushDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pages ADD COLUMN layers TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE pages ADD COLUMN activeLayerId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE audio_recordings ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_brushes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `profileJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
