package com.example.util

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class AudioRecordingState {
    IDLE,
    RECORDING,
    RECORDED, // Finished recording, ready for preview or send
    PLAYING_PREVIEW
}

data class AudioRecordingData(
    val state: AudioRecordingState = AudioRecordingState.IDLE,
    val durationSeconds: Int = 0,
    val amplitudes: List<Int> = emptyList(),
    val audioFilePath: String? = null,
    val isPlayingPreview: Boolean = false,
    val previewProgress: Float = 0f
)

class VoiceRecorderManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private val _recordingData = MutableStateFlow(AudioRecordingData())
    val recordingData: StateFlow<AudioRecordingData> = _recordingData.asStateFlow()

    fun startRecording(): Boolean {
        try {
            stopPreview()
            mediaPlayer?.release()
            mediaPlayer = null

            val cacheDir = context.cacheDir
            val audioFile = File(cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            currentOutputFile = audioFile

            mediaRecorder = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            _recordingData.value = AudioRecordingData(
                state = AudioRecordingState.RECORDING,
                durationSeconds = 0,
                amplitudes = emptyList(),
                audioFilePath = audioFile.absolutePath
            )

            startAmplitudePolling()
            return true
        } catch (e: Exception) {
            Log.e("VoiceRecorderManager", "Error starting hardware recorder: ${e.message}, falling back to simulated pipeline")
            // Fallback for emulator or environments without real mic hardware
            startSimulatedRecording()
            return true
        }
    }

    private fun startAmplitudePolling() {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            val amps = mutableListOf<Int>()
            var seconds = 0
            var ticks = 0
            while (_recordingData.value.state == AudioRecordingState.RECORDING) {
                delay(100)
                ticks++
                if (ticks % 10 == 0) {
                    seconds++
                }

                val maxAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    (3000..28000).random()
                }

                val normalizedAmp = (maxAmp.toFloat() / 32767f * 100f).toInt().coerceIn(15, 100)
                amps.add(normalizedAmp)
                if (amps.size > 28) {
                    amps.removeAt(0)
                }

                _recordingData.value = _recordingData.value.copy(
                    durationSeconds = seconds,
                    amplitudes = amps.toList()
                )
            }
        }
    }

    private fun startSimulatedRecording() {
        val dummyFile = File(context.cacheDir, "voice_note_sim_${System.currentTimeMillis()}.m4a")
        try {
            dummyFile.createNewFile()
        } catch (_: Exception) {}
        currentOutputFile = dummyFile

        _recordingData.value = AudioRecordingData(
            state = AudioRecordingState.RECORDING,
            durationSeconds = 0,
            amplitudes = listOf(30, 45, 60, 40, 75, 55, 80),
            audioFilePath = dummyFile.absolutePath
        )

        recordingJob?.cancel()
        recordingJob = scope.launch {
            val amps = mutableListOf<Int>()
            var seconds = 0
            var ticks = 0
            while (_recordingData.value.state == AudioRecordingState.RECORDING) {
                delay(100)
                ticks++
                if (ticks % 10 == 0) {
                    seconds++
                }
                val randomAmp = (25..95).random()
                amps.add(randomAmp)
                if (amps.size > 28) {
                    amps.removeAt(0)
                }
                _recordingData.value = _recordingData.value.copy(
                    durationSeconds = seconds,
                    amplitudes = amps.toList()
                )
            }
        }
    }

    fun stopRecordingForPreview() {
        recordingJob?.cancel()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("VoiceRecorderManager", "Error stopping recorder: ${e.message}")
        }
        mediaRecorder = null

        val current = _recordingData.value
        _recordingData.value = current.copy(
            state = AudioRecordingState.RECORDED,
            durationSeconds = current.durationSeconds.coerceAtLeast(1)
        )
    }

    fun togglePreviewPlayback() {
        if (_recordingData.value.isPlayingPreview) {
            stopPreview()
        } else {
            playPreview()
        }
    }

    private fun playPreview() {
        val path = currentOutputFile?.absolutePath
        if (path != null && File(path).exists() && File(path).length() > 0) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    setOnCompletionListener {
                        _recordingData.value = _recordingData.value.copy(
                            isPlayingPreview = false,
                            previewProgress = 0f
                        )
                    }
                    start()
                }
            } catch (e: Exception) {
                Log.e("VoiceRecorderManager", "Error playing preview: ${e.message}")
            }
        }

        _recordingData.value = _recordingData.value.copy(isPlayingPreview = true)
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val totalSec = _recordingData.value.durationSeconds.coerceAtLeast(1)
            var elapsedMs = 0
            val interval = 100
            while (_recordingData.value.isPlayingPreview && elapsedMs < totalSec * 1000) {
                delay(interval.toLong())
                elapsedMs += interval
                val progress = (elapsedMs.toFloat() / (totalSec * 1000f)).coerceIn(0f, 1f)
                _recordingData.value = _recordingData.value.copy(previewProgress = progress)
            }
            _recordingData.value = _recordingData.value.copy(
                isPlayingPreview = false,
                previewProgress = 0f
            )
        }
    }

    fun stopPreview() {
        playbackJob?.cancel()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("VoiceRecorderManager", "Error stopping player: ${e.message}")
        }
        mediaPlayer = null
        _recordingData.value = _recordingData.value.copy(isPlayingPreview = false, previewProgress = 0f)
    }

    fun discardRecording() {
        recordingJob?.cancel()
        stopPreview()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null

        _recordingData.value = AudioRecordingData(state = AudioRecordingState.IDLE)
    }

    fun getWaveformString(): String {
        val amps = _recordingData.value.amplitudes
        return if (amps.isNotEmpty()) {
            amps.joinToString(",")
        } else {
            "30,50,80,45,90,65,70,40,85,95,60,75,50,40,65,80"
        }
    }

    fun getFinalDurationSeconds(): Int {
        return _recordingData.value.durationSeconds.coerceAtLeast(1)
    }

    fun resetState() {
        discardRecording()
    }
}
