package com.example.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class AudioAndTtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 75)
        } catch (e: Exception) {
            toneGenerator = null
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

    fun playCallDialTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1200)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playCallConnectedTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playCallEndTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stopCallTones() {
        try {
            toneGenerator?.stopTone()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun speakText(text: String, languageCode: String = "hi") {
        if (!isInitialized || tts == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "🔊 Playing Voice: $text", Toast.LENGTH_SHORT).show()
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
        try {
            toneGenerator?.release()
            toneGenerator = null
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
