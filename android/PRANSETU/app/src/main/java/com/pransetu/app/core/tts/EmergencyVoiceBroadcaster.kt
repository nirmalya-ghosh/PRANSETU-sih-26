package com.pransetu.app.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Intelligent Emergency Text-To-Speech (TTS) Voice Engine.
 * Reads cyclone warnings, evacuation instructions, and first-aid steps
 * aloud in native Indian languages (Odia, Hindi, Bengali, Telugu, English).
 */
class EmergencyVoiceBroadcaster(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeakingFlow.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeakingFlow.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeakingFlow.value = false
                    }
                })
            } else {
                Log.w("EmergencyTTS", "TTS Engine initialization failed with code: $status")
            }
        }
    }

    fun speak(text: String, langCode: String = "en") {
        if (!isInitialized || tts == null) return

        val targetLocale = when (langCode.lowercase()) {
            "or" -> Locale("or", "IN")
            "hi" -> Locale("hi", "IN")
            "bn" -> Locale("bn", "IN")
            "te" -> Locale("te", "IN")
            "ta" -> Locale("ta", "IN")
            "mr" -> Locale("mr", "IN")
            "gu" -> Locale("gu", "IN")
            "kn" -> Locale("kn", "IN")
            "ml" -> Locale("ml", "IN")
            "pa" -> Locale("pa", "IN")
            "as" -> Locale("as", "IN")
            "ur" -> Locale("ur", "IN")
            else -> Locale("en", "IN")
        }

        try {
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to Hindi or English if specific regional voice pack is uninstalled on device
                val fallback = tts?.setLanguage(Locale("hi", "IN"))
                if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }
            tts?.setSpeechRate(0.95f) // Slightly slower for clear emergency audibility
            tts?.setPitch(1.0f)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EMERGENCY_TTS_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("EmergencyTTS", "Error during TTS playback", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeakingFlow.value = false
        } catch (_: Exception) {}
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
