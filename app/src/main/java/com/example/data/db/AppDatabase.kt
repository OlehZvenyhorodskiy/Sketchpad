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
import com.example.data.models.CanvasReferenceEntity
import com.example.data.models.CustomBrushEntity
import com.example.data.models.FlashcardEntity
import com.example.data.models.PageEntity
import com.example.data.models.StudyDeckEntity

@Database(
    entities = [
        CanvasEntity::class,
        PageEntity::class,
        AudioRecordingEntity::class,
        CustomBrushEntity::class,
        CanvasReferenceEntity::class,
        StudyDeckEntity::class,
        FlashcardEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(MoshiConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun canvasDao(): CanvasDao
    abstract fun pageDao(): PageDao
    abstract fun audioDao(): AudioDao
    abstract fun customBrushDao(): CustomBrushDao
    abstract fun canvasReferenceDao(): CanvasReferenceDao
    abstract fun studyDeckDao(): StudyDeckDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `canvas_references` (
                        `id` TEXT NOT NULL,
                        `sourceCanvasId` TEXT NOT NULL,
                        `sourcePageId` TEXT NOT NULL,
                        `sourceElementIds` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `targetCanvasId` TEXT NOT NULL,
                        `targetPageId` TEXT NOT NULL,
                        `targetCenterX` REAL NOT NULL,
                        `targetCenterY` REAL NOT NULL,
                        `targetZoom` REAL NOT NULL,
                        `targetElementIds` TEXT NOT NULL,
                        `transitionDurationMillis` INTEGER NOT NULL,
                        `highlightDurationMillis` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sourceCanvasId`) REFERENCES `canvases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`sourcePageId`) REFERENCES `pages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`targetCanvasId`) REFERENCES `canvases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`targetPageId`) REFERENCES `pages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_canvas_references_sourceCanvasId` ON `canvas_references` (`sourceCanvasId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_canvas_references_sourcePageId` ON `canvas_references` (`sourcePageId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_canvas_references_targetCanvasId` ON `canvas_references` (`targetCanvasId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_canvas_references_targetPageId` ON `canvas_references` (`targetPageId`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_decks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `canvasId` TEXT,
                        `pageId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`canvasId`) REFERENCES `canvases`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`pageId`) REFERENCES `pages`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id` TEXT NOT NULL,
                        `deckId` TEXT NOT NULL,
                        `prompt` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `hint` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `sourceCanvasId` TEXT,
                        `sourcePageId` TEXT,
                        `sourceElementIds` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `dueAt` INTEGER NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `repetitions` INTEGER NOT NULL,
                        `easeFactor` REAL NOT NULL,
                        `lapses` INTEGER NOT NULL,
                        `lastReviewedAt` INTEGER,
                        `suspended` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`deckId`) REFERENCES `study_decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`sourceCanvasId`) REFERENCES `canvases`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`sourcePageId`) REFERENCES `pages`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_decks_canvasId` ON `study_decks` (`canvasId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_decks_pageId` ON `study_decks` (`pageId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_decks_updatedAt` ON `study_decks` (`updatedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_deckId` ON `flashcards` (`deckId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_sourceCanvasId` ON `flashcards` (`sourceCanvasId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_sourcePageId` ON `flashcards` (`sourcePageId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_deckId_dueAt` ON `flashcards` (`deckId`, `dueAt`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE canvases ADD COLUMN folderName TEXT DEFAULT NULL")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
