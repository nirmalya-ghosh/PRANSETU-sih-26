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
 * AlertViewModel handles high-frequency, high-decibel disaster emergency beeps
 * and government-style heads-up alert notifications across the system.
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

    companion object {
        private var instance: AlertViewModel? = null

        fun globalDismiss() {
            instance?.dismissAlert()
        }
    }

    init {
        instance = this
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            alertService.pollForAlerts(intervalMs = 2500L).collect { alert ->
                Log.d(TAG, "🚨 NEW EMERGENCY DISASTER ALERT: ${alert.message}")
                _currentAlert.value = alert
                
                val context = getApplication<Application>().applicationContext
                // 1. Post real system heads-up notification banner
                EmergencyAlertNotificationHelper.showEmergencyNotification(context, alert)
                
                // 2. Sound the loud, high-frequency emergency beeping siren
                triggerHighFrequencyEmergencyBeep()
            }
        }
    }

    /**
     * Synthesizes and continuously plays the authentic high-frequency Emergency Alert System (EAS)
     * dual-frequency piercing staccato beeps (853Hz + 960Hz & 1450Hz rapid pulses) at max volume.
     */
    private fun triggerHighFrequencyEmergencyBeep() {
        if (isSirenActive) return
        isSirenActive = true

        val context = getApplication<Application>().applicationContext

        // 1. Force Max Alarm Volume
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not force maximum volume", e)
        }

        // 2. High-Frequency Pulsing Emergency Beep Generator
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

                // Classic EAS / National Warning Beep: 400ms High-Pitch Beep + 100ms Silence in Rapid Succession
                val beepDurationMs = 380
                val silenceDurationMs = 120
                val beepSamples = (sampleRate * (beepDurationMs / 1000.0)).toInt()
                val silenceSamples = (sampleRate * (silenceDurationMs / 1000.0)).toInt()

                val beepBuffer = ShortArray(beepSamples)
                val silenceBuffer = ShortArray(silenceSamples)

                // High frequencies: 960Hz & 1400Hz emergency alert frequencies
                var freqToggle = false

                while (isActive && isSirenActive) {
                    val f1 = if (freqToggle) 960.0 else 853.0
                    val f2 = if (freqToggle) 1400.0 else 1200.0

                    for (i in 0 until beepSamples) {
                        val angle1 = 2.0 * Math.PI * i / (sampleRate / f1)
                        val angle2 = 2.0 * Math.PI * i / (sampleRate / f2)
                        // Superimpose dual emergency alert frequencies
                        val sampleVal = (0.5 * sin(angle1) + 0.5 * sin(angle2))
                        // High piercing square modulation
                        val modulated = if (sampleVal >= 0) 29000 else -29000
                        beepBuffer[i] = modulated.toShort()
                    }

                    audioTrack.write(beepBuffer, 0, beepSamples)
                    audioTrack.write(silenceBuffer, 0, silenceSamples)
                    freqToggle = !freqToggle
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack beep synthesis failed, falling back to system ringtone", e)
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

        // 3. Repeating Intense Emergency Vibration Pulse
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 500, 150, 500, 150, 500, 300)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
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
     * Halts beeping sound, cancels notification, and resets alert state.
     */
    fun dismissAlert() {
        Log.d(TAG, "Dismissing emergency alert and stopping beeping alarm.")
        isSirenActive = false
        sirenJob?.cancel()
        sirenJob = null

        try {
            fallbackRingtone?.stop()
            fallbackRingtone = null
        } catch (_: Exception) {}

        val context = getApplication<Application>().applicationContext
        EmergencyAlertNotificationHelper.cancelNotification(context)

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

    override fun onCleared() {
        super.onCleared()
        if (instance == this) {
            instance = null
        }
    }
}
