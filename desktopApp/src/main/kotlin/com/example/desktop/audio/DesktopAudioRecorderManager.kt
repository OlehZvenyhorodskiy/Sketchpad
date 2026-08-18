package com.example.desktop.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.*
import kotlin.math.abs
import kotlin.math.max

class DesktopAudioRecorderManager {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val currentAmplitudes: StateFlow<List<Float>> = _currentAmplitudes

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress

    private var targetDataLine: TargetDataLine? = null
    private var recordJob: Job? = null
    private var playbackClip: Clip? = null
    private var playbackJob: Job? = null
    private var audioFormat = AudioFormat(44100f, 16, 1, true, false)
    private var recordedBytesStream = ByteArrayOutputStream()

    fun startRecording() {
        if (_isRecording.value) return
        try {
            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
            if (!AudioSystem.isLineSupported(info)) return

            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(audioFormat)
            line.start()
            targetDataLine = line
            recordedBytesStream.reset()
            _isRecording.value = true
            _currentAmplitudes.value = emptyList()

            recordJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(2048)
                while (isActive && _isRecording.value) {
                    val count = line.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        recordedBytesStream.write(buffer, 0, count)
                        // Calculate RMS amplitude
                        var maxSample = 0
                        for (i in 0 until count step 2) {
                            val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
                            maxSample = max(maxSample, abs(sample))
                        }
                        val normalized = (maxSample / 32768f).coerceIn(0.05f, 1.0f)
                        val updated = (_currentAmplitudes.value + normalized).takeLast(60)
                        _currentAmplitudes.value = updated
                    }
                    delay(50)
                }
            }
        } catch (_: Exception) {
            _isRecording.value = false
        }
    }

    fun stopRecording(outputFile: File): Long {
        if (!_isRecording.value) return 0L
        _isRecording.value = false
        recordJob?.cancel()
        targetDataLine?.stop()
        targetDataLine?.close()
        targetDataLine = null

        val audioBytes = recordedBytesStream.toByteArray()
        if (audioBytes.isEmpty()) return 0L

        try {
            outputFile.parentFile?.mkdirs()
            val bais = ByteArrayInputStream(audioBytes)
            val audioInputStream = AudioInputStream(bais, audioFormat, audioBytes.size.toLong() / audioFormat.frameSize)
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile)
        } catch (_: Exception) {}

        val durationMs = (audioBytes.size.toLong() * 1000L) / (audioFormat.sampleRate * audioFormat.frameSize).toLong()
        return durationMs
    }

    fun startPlayback(audioFile: File, onComplete: () -> Unit = {}) {
        stopPlayback()
        if (!audioFile.exists()) return

        try {
            val audioIn = AudioSystem.getAudioInputStream(audioFile)
            val clip = AudioSystem.getClip()
            clip.open(audioIn)
            clip.start()
            playbackClip = clip
            _isPlaying.value = true

            playbackJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && clip.isRunning) {
                    val progress = clip.microsecondPosition.toFloat() / clip.microsecondLength.toFloat()
                    _playbackProgress.value = progress.coerceIn(0f, 1f)
                    delay(50)
                }
                _isPlaying.value = false
                _playbackProgress.value = 0f
                onComplete()
            }
        } catch (_: Exception) {
            _isPlaying.value = false
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackClip?.stop()
        playbackClip?.close()
        playbackClip = null
        _isPlaying.value = false
        _playbackProgress.value = 0f
    }
}
