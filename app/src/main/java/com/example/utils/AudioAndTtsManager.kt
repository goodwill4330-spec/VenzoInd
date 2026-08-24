package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class AudioAndTtsManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var toneGenerator: ToneGenerator? = null
    private var defaultRingtone: Ringtone? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var dialToneRunnable: Runnable? = null
    private var isRinging = false

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 85)
        } catch (e: Exception) {
            toneGenerator = null
        }

        try {
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            defaultRingtone = RingtoneManager.getRingtone(context.applicationContext, ringUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                defaultRingtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
        } catch (e: Exception) {
            defaultRingtone = null
        }

        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("hi", "IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.ENGLISH
                    }
                    isInitialized = true
                }
            }
        } catch (e: Exception) {
            isInitialized = false
        }
    }

    /**
     * Starts continuous dial tone while waiting for receiver to pick up
     */
    fun playCallDialTone() {
        stopCallTones()
        dialToneRunnable = object : Runnable {
            override fun run() {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1500)
                } catch (e: Exception) {
                    // Ignore
                }
                mainHandler.postDelayed(this, 3000)
            }
        }
        dialToneRunnable?.let { mainHandler.post(it) }
    }

    /**
     * Rings phone for incoming call
     */
    fun startIncomingRinging() {
        if (isRinging) return
        isRinging = true
        try {
            defaultRingtone?.play()
            triggerVibration(longArrayOf(0, 1000, 800, 1000, 800))
        } catch (e: Exception) {
            // Fallback tone
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 1000)
        }
    }

    /**
     * Connected call chime & configure voice communication audio mode
     */
    fun playCallConnectedTone() {
        stopCallTones()
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Call end disconnect tone & reset audio mode
     */
    fun playCallEndTone() {
        stopCallTones()
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 350)
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = false
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun setSpeakerOn(enabled: Boolean) {
        try {
            audioManager?.isSpeakerphoneOn = enabled
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun setMicrophoneMute(muted: Boolean) {
        try {
            audioManager?.isMicrophoneMute = muted
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playMessageReceivedChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            triggerVibration(longArrayOf(0, 80, 50, 80))
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playMessageSentChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stopCallTones() {
        isRinging = false
        dialToneRunnable?.let { mainHandler.removeCallbacks(it) }
        dialToneRunnable = null
        try {
            toneGenerator?.stopTone()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            if (defaultRingtone?.isPlaying == true) {
                defaultRingtone?.stop()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun triggerVibration(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun speakText(text: String, languageCode: String = "hi") {
        if (!isInitialized || tts == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "🔊 Voice: $text", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            val locale = when (languageCode.lowercase()) {
                "hi", "hindi" -> Locale("hi", "IN")
                "ta", "tamil" -> Locale("ta", "IN")
                "te", "telugu" -> Locale("te", "IN")
                "mr", "marathi" -> Locale("mr", "IN")
                "bn", "bengali" -> Locale("bn", "IN")
                else -> Locale.ENGLISH
            }
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "BharatChatTts")
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "🔊 Voice: $text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun shutdown() {
        stopCallTones()
        try {
            toneGenerator?.release()
            toneGenerator = null
            tts?.stop()
            tts?.shutdown()
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            // Ignore
        }
    }
}
