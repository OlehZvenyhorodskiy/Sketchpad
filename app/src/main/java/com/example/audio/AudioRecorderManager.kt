package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class RecordingStatus {
    object Idle : RecordingStatus()
    data class Recording(val durationMs: Long, val filePath: String) : RecordingStatus()
    data class Playing(val currentPositionMs: Long, val totalDurationMs: Long, val filePath: String) : RecordingStatus()
}

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0L

    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    fun startRecording(): String? {
        try {
            stopPlayback()
            val outputFile = File(context.cacheDir, "temp_rec_${System.currentTimeMillis()}.m4a")
            currentOutputFilePath = outputFile.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentOutputFilePath)
                prepare()
                start()
            }

            recordingStartTimeMs = System.currentTimeMillis()
            _status.value = RecordingStatus.Recording(0L, outputFile.absolutePath)
            return outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
            return null
        }
    }

    fun stopRecording(): Pair<String?, Long> {
        val path = currentOutputFilePath
        val duration = if (recordingStartTimeMs > 0) System.currentTimeMillis() - recordingStartTimeMs else 0L
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            recordingStartTimeMs = 0L
            _status.value = RecordingStatus.Idle
        }
        return Pair(path, duration)
    }

    fun startPlayback(filePath: String, onComplete: () -> Unit = {}) {
        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    _status.value = RecordingStatus.Idle
                    onComplete()
                }
            }
            val total = mediaPlayer?.duration?.toLong() ?: 0L
            _status.value = RecordingStatus.Playing(0L, total, filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            _status.value = RecordingStatus.Idle
        }
    }

    fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _status.value = RecordingStatus.Idle
        }
    }

    fun getRecordingDuration(filePath: String): Long {
        return try {
            val mp = MediaPlayer()
            mp.setDataSource(filePath)
            mp.prepare()
            val duration = mp.duration.toLong()
            mp.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }
}
