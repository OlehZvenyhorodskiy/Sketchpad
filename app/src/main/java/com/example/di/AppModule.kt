package com.example.di

import android.content.Context
import com.example.audio.AudioRecorderManager
import com.example.data.db.AppDatabase
import com.example.data.db.AudioDao
import com.example.data.db.CanvasDao
import com.example.data.repository.CanvasRepository
import com.example.data.repository.CollaborationRepository

/**
 * Модуль залежностей (Dependency Injection) для проєкту Sketchpad.
 */
object AppModule {

    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var canvasRepository: CanvasRepository? = null

    @Volatile
    private var audioRecorderManager: AudioRecorderManager? = null

    @Volatile
    private var collaborationRepository: CollaborationRepository? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: AppDatabase.getDatabase(context).also { database = it }
        }
    }

    fun provideCanvasDao(context: Context): CanvasDao = provideDatabase(context).canvasDao()

    fun provideAudioDao(context: Context): AudioDao = provideDatabase(context).audioDao()

    fun provideAudioRecorderManager(context: Context): AudioRecorderManager {
        return audioRecorderManager ?: synchronized(this) {
            audioRecorderManager ?: AudioRecorderManager(context.applicationContext).also { audioRecorderManager = it }
        }
    }

    fun provideCanvasRepository(context: Context): CanvasRepository {
        return canvasRepository ?: synchronized(this) {
            canvasRepository ?: CanvasRepository(context.applicationContext).also { canvasRepository = it }
        }
    }

    fun provideCollaborationRepository(): CollaborationRepository {
        return collaborationRepository ?: synchronized(this) {
            collaborationRepository ?: CollaborationRepository().also { collaborationRepository = it }
        }
    }
}
