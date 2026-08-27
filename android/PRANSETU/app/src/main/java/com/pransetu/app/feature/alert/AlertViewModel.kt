package com.pransetu.app.feature.alert

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.network.SystemAlert
import com.pransetu.app.core.network.SystemAlertService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * AlertViewModel manages real-time emergency disaster broadcast reception.
 * Triggers a piercing, continuous looping siren and repeating vibration that
 * keeps sounding until the citizen explicitly confirms and acknowledges the alert dialog.
 */
class AlertViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AlertViewModel"
    private val alertService = SystemAlertService()
    
    private val _currentAlert = MutableStateFlow<SystemAlert?>(null)
    val currentAlert: StateFlow<SystemAlert?> = _currentAlert.asStateFlow()

    private var sirenJob: Job? = null
    @Volatile
    private var isSirenActive = false
    private var fallbackRingtone: Ringtone? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            alertService.pollForAlerts(intervalMs = 2500L).collect { alert ->
                Log.d(TAG, "🚨 NEW EMERGENCY SYSTEM ALERT RECEIVED: ${alert.message}")
                _currentAlert.value = alert
                triggerContinuousEmergencySiren()
            }
        }
    }

    /**
     * Synthesizes and continuously plays a high-decibel wailing disaster siren (800Hz - 1200Hz)
     * at maximum alarm volume, along with intense rhythmic vibrations.
     */
    private fun triggerContinuousEmergencySiren() {
        if (isSirenActive) return
        isSirenActive = true

        val context = getApplication<Application>().applicationContext

        // 1. Maximize Alarm Volume
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not force maximum volume", e)
        }

        // 2. Start Infinite Continuous Siren Generator in Background Coroutine
        sirenJob = viewModelScope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            var audioTrack: AudioTrack? = null
            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()

                val durationPerToneMs = 350
                val samplesPerTone = (sampleRate * (durationPerToneMs / 1000.0)).toInt()
                val toneBuffer = ShortArray(samplesPerTone)

                var toggle = false
                while (isActive && isSirenActive) {
                    val freq = if (toggle) 1250.0 else 780.0
                    for (i in 0 until samplesPerTone) {
                        val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                        // Square-modulated wave for high-piercing emergency siren penetration
                        val rawSample = sin(angle)
                        val squareSample = if (rawSample >= 0) 28000 else -28000
                        toneBuffer[i] = squareSample.toShort()
                    }
                    audioTrack.write(toneBuffer, 0, samplesPerTone)
                    toggle = !toggle
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack siren synthesis failed, falling back to system ringtone", e)
                // Fallback to default alarm ringtone
                try {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                    fallbackRingtone = ringtone
                    ringtone.play()
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback ringtone failed", ex)
                }
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}
            }
        }

        // 3. Start Repeating Intense Vibration
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 800, 200, 800, 200, 800, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat continuously from 0
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    /**
     * User clicked "ACKNOWLEDGE & CONFIRM RECEIPT" on the emergency alert dialog.
     * Completely halts the siren sound, cancels vibration, and closes the modal.
     */
    fun dismissAlert() {
        Log.d(TAG, "Emergency alert acknowledged by citizen. Muting siren and dismissing dialog.")
        isSirenActive = false
        sirenJob?.cancel()
        sirenJob = null

        try {
            fallbackRingtone?.stop()
            fallbackRingtone = null
        } catch (_: Exception) {}

        // Stop all vibrations
        val context = getApplication<Application>().applicationContext
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel vibrator", e)
        }

        _currentAlert.value = null
    }
}
