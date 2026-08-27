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
import com.pransetu.app.core.network.EmergencyAlertStore
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
 * and government-style heads-up alert notifications.
 *
 * CRITICAL RULE: Once acknowledged by the citizen, the alert ID is persisted to disk,
 * and audio/vibration are permanently and immediately silenced.
 */
class AlertViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AlertViewModel"
    private val alertService = SystemAlertService(application.applicationContext)
    private val alertStore = EmergencyAlertStore(application.applicationContext)
    
    private val _currentAlert = MutableStateFlow<SystemAlert?>(null)
    val currentAlert: StateFlow<SystemAlert?> = _currentAlert.asStateFlow()

    private var sirenJob: Job? = null
    @Volatile
    private var isSirenActive = false
    private var currentAudioTrack: AudioTrack? = null
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
            alertService.pollForAlerts(intervalMs = 2000L).collect { alert ->
                if (alertStore.isAlertAcknowledged(alert.sosId)) {
                    Log.d(TAG, "Skipping alert ${alert.sosId} because citizen already acknowledged it.")
                    return@collect
                }

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
     * dual-frequency piercing staccato beeps (853Hz + 960Hz & 1400Hz rapid pulses) at max volume.
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

            try {
                val track = AudioTrack.Builder()
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

                currentAudioTrack = track
                track.play()

                val beepDurationMs = 380
                val silenceDurationMs = 120
                val beepSamples = (sampleRate * (beepDurationMs / 1000.0)).toInt()
                val silenceSamples = (sampleRate * (silenceDurationMs / 1000.0)).toInt()

                val beepBuffer = ShortArray(beepSamples)
                val silenceBuffer = ShortArray(silenceSamples)

                var freqToggle = false

                while (isActive && isSirenActive) {
                    val f1 = if (freqToggle) 960.0 else 853.0
                    val f2 = if (freqToggle) 1400.0 else 1200.0

                    for (i in 0 until beepSamples) {
                        val angle1 = 2.0 * Math.PI * i / (sampleRate / f1)
                        val angle2 = 2.0 * Math.PI * i / (sampleRate / f2)
                        val sampleVal = (0.5 * sin(angle1) + 0.5 * sin(angle2))
                        val modulated = if (sampleVal >= 0) 29000 else -29000
                        beepBuffer[i] = modulated.toShort()
                    }

                    if (!isSirenActive) break
                    track.write(beepBuffer, 0, beepSamples)
                    if (!isSirenActive) break
                    track.write(silenceBuffer, 0, silenceSamples)
                    freqToggle = !freqToggle
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack beep synthesis error", e)
                try {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                    fallbackRingtone = ringtone
                    if (isSirenActive) {
                        ringtone.play()
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback ringtone failed", ex)
                }
            } finally {
                stopAudioTrackInternal()
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

    private fun stopAudioTrackInternal() {
        try {
            currentAudioTrack?.pause()
            currentAudioTrack?.flush()
            currentAudioTrack?.stop()
            currentAudioTrack?.release()
        } catch (_: Exception) {}
        currentAudioTrack = null
    }

    /**
     * Halts beeping sound immediately, cancels notification, silences all vibrations,
     * permanently records acknowledgment in SharedPreferences, and transmits telemetry.
     */
    fun dismissAlert() {
        Log.d(TAG, "Citizen acknowledged alert. Instantly stopping audio and muting alarm permanently.")
        
        // 1. Immediately silence audio flag and stop AudioTrack
        isSirenActive = false
        stopAudioTrackInternal()
        sirenJob?.cancel()
        sirenJob = null

        // 2. Stop fallback ringtone if active
        try {
            fallbackRingtone?.stop()
            fallbackRingtone = null
        } catch (_: Exception) {}

        // 3. Cancel notification
        val context = getApplication<Application>().applicationContext
        EmergencyAlertNotificationHelper.cancelNotification(context)

        // 4. Cancel all vibration
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

        // 5. Persist acknowledgment to disk so it NEVER plays sound again
        val alert = _currentAlert.value
        if (alert != null) {
            alertStore.markAlertAcknowledged(alert.sosId)
        }

        // 6. Transmit citizen acknowledgment to Supabase Event Bus
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>() as? com.pransetu.app.PransetuApplication
                val phone = app?.userProfileStore?.getUserPhoneSync() ?: ""
                val name = app?.userProfileStore?.getUserNameSync() ?: "Citizen"

                val ackPayload = org.json.JSONObject().apply {
                    put("event_type", "EMERGENCY_BROADCAST_ACKNOWLEDGED")
                    put("source", "android_app")
                    put("user_id", phone)
                    put("occurred_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date()))
                    put("payload", org.json.JSONObject().apply {
                        put("citizen_name", name)
                        put("citizen_phone", phone)
                        put("status", "ACKNOWLEDGED")
                        put("ack_timestamp", System.currentTimeMillis())
                    })
                }

                com.pransetu.app.core.network.supabase.SupabaseClient.post("realtime_events", ackPayload.toString())
                Log.d(TAG, "✅ Transmitted EMERGENCY_BROADCAST_ACKNOWLEDGED for citizen $name ($phone)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send acknowledgment to Supabase", e)
            }
        }

        _currentAlert.value = null
    }

    override fun onCleared() {
        super.onCleared()
        isSirenActive = false
        stopAudioTrackInternal()
        sirenJob?.cancel()
        if (instance == this) {
            instance = null
        }
    }
}
