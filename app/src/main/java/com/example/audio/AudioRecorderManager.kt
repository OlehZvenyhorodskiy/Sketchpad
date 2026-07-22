package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class RecordingStatus {
    object Idle : RecordingStatus()
    data class Recording(
        val durationMs: Long,
        val filePath: String,
        val amplitudes: List<Float> = emptyList(),
        val currentAmplitude: Float = 0f
    ) : RecordingStatus()
    data class Playing(
        val currentPositionMs: Long,
        val totalDurationMs: Long,
        val filePath: String,
        val isPlaying: Boolean = true
    ) : RecordingStatus()
}

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0L

    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    private fun getAudioNotesDir(): File {
        val dir = File(context.filesDir, "audio_notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun startRecording(canvasId: String = "unknown"): String? {
        try {
            stopPlayback()
            val timestamp = System.currentTimeMillis()
            val outputFile = File(getAudioNotesDir(), "${canvasId}_${timestamp}.m4a")
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
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(currentOutputFilePath)
                prepare()
                start()
            }

            recordingStartTimeMs = System.currentTimeMillis()
            _status.value = RecordingStatus.Recording(0L, outputFile.absolutePath)

            recordingJob?.cancel()
            recordingJob = audioScope.launch {
                val amplitudeHistory = mutableListOf<Float>()
                var smoothedAmp = 0.08f
                while (isActive) {
                    delay(40L)
                    val duration = System.currentTimeMillis() - recordingStartTimeMs
                    val rawAmp = try { mediaRecorder?.maxAmplitude ?: 0 } catch (e: Exception) { 0 }
                    val targetAmp = if (rawAmp > 0) {
                        (Math.log10(1.0 + rawAmp) / Math.log10(32768.0)).toFloat().coerceIn(0.08f, 1.0f)
                    } else 0.08f
                    smoothedAmp = smoothedAmp * 0.3f + targetAmp * 0.7f
                    amplitudeHistory.add(smoothedAmp)
                    if (amplitudeHistory.size > 28) amplitudeHistory.removeAt(0)
                    _status.value = RecordingStatus.Recording(
                        durationMs = duration,
                        filePath = currentOutputFilePath ?: "",
                        amplitudes = amplitudeHistory.toList(),
                        currentAmplitude = smoothedAmp
                    )
                }
            }
            return outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
            return null
        }
    }

    fun stopRecording(): Pair<String?, Long> {
        recordingJob?.cancel()
        recordingJob = null
        val path = currentOutputFilePath
        val duration = if (recordingStartTimeMs > 0) System.currentTimeMillis() - recordingStartTimeMs else 0L
        try {
            mediaRecorder?.apply { stop(); release() }
        } catch (e: Exception) { e.printStackTrace() }
        finally {
            mediaRecorder = null
            recordingStartTimeMs = 0L
            _status.value = RecordingStatus.Idle
        }
        return Pair(path, duration)
    }

    fun startPlayback(filePath: String, startPositionMs: Long = 0L, onComplete: () -> Unit = {}) {
        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                if (startPositionMs > 0) seekTo(startPositionMs.toInt())
                start()
                setOnCompletionListener {
                    playbackJob?.cancel()
                    _status.value = RecordingStatus.Playing(
                        currentPositionMs = duration.toLong(),
                        totalDurationMs = duration.toLong(),
                        filePath = filePath,
                        isPlaying = false
                    )
                    onComplete()
                }
            }
            val total = mediaPlayer?.duration?.toLong() ?: 0L
            startPlaybackTicker(filePath, total)
        } catch (e: Exception) {
            e.printStackTrace()
            _status.value = RecordingStatus.Idle
        }
    }

    private fun startPlaybackTicker(filePath: String, totalDurationMs: Long) {
        playbackJob?.cancel()
        playbackJob = audioScope.launch {
            while (isActive) {
                delay(80L)
                val mp = mediaPlayer ?: continue
                val pos = try { mp.currentPosition.toLong() } catch (e: Exception) { 0L }
                val playing = try { mp.isPlaying } catch (e: Exception) { false }
                _status.value = RecordingStatus.Playing(pos, totalDurationMs, filePath, playing)
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.pause()
                _status.value = RecordingStatus.Playing(
                    mp.currentPosition.toLong(), mp.duration.toLong(),
                    currentOutputFilePath ?: "", false
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun resumePlayback() {
        val current = _status.value
        if (current is RecordingStatus.Playing) {
            mediaPlayer?.let { mp ->
                try { mp.start(); startPlaybackTicker(current.filePath, current.totalDurationMs) }
                catch (e: Exception) { e.printStackTrace() }
            } ?: startPlayback(current.filePath, current.currentPositionMs)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(positionMs.toInt())
                val current = _status.value
                if (current is RecordingStatus.Playing) {
                    _status.value = current.copy(currentPositionMs = positionMs)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try { mediaPlayer?.apply { if (isPlaying) stop(); release() } }
        catch (e: Exception) { e.printStackTrace() }
        finally { mediaPlayer = null; _status.value = RecordingStatus.Idle }
    }

    fun deleteAudioFile(filePath: String) {
        stopPlayback()
        try { File(filePath).takeIf { it.exists() }?.delete() }
        catch (e: Exception) { e.printStackTrace() }
    }

    fun destroy() {
        audioScope.cancel()
        stopPlayback()
        stopRecording()
    }
}
