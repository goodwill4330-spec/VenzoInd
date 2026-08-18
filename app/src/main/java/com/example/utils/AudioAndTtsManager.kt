package com.example.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class AudioAndTtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
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
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
