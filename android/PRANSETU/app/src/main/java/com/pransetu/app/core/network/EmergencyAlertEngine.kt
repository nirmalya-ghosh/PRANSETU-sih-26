package com.pransetu.app.core.network

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.pransetu.app.core.network.supabase.SupabaseClient
import com.pransetu.app.feature.alert.EmergencyAlertNotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.sin

/**
 * Singleton EmergencyAlertEngine responsible for background and foreground
 * emergency alert monitoring, loud siren synthesis, screen wake-up,
 * and real-time acknowledgment dispatch.
 */
class EmergencyAlertEngine private constructor(private val context: Context) {

    private val TAG = "EmergencyAlertEngine"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val alertService = SystemAlertService(context)
    private val alertStore = EmergencyAlertStore(context)

    private val _activeAlert = MutableStateFlow<SystemAlert?>(null)
    val activeAlert: StateFlow<SystemAlert?> = _activeAlert.asStateFlow()

    @Volatile
    private var isSirenActive = false
    private var sirenJob: Job? = null
    private var currentAudioTrack: AudioTrack? = null
    private var fallbackRingtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        @Volatile
        private var INSTANCE: EmergencyAlertEngine? = null

        fun getInstance(context: Context): EmergencyAlertEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmergencyAlertEngine(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun globalDismiss() {
            INSTANCE?.dismissActiveAlert()
        }
    }

    init {
        startContinuousMonitoring()
    }

    fun startContinuousMonitoring() {
        scope.launch {
            Log.d(TAG, "Emergency Broadcast Daemon: Continuous Background Monitor Started.")
            alertService.pollForAlerts(intervalMs = 2000L).collect { alert ->
                if (alertStore.isAlertAcknowledged(alert.sosId)) {
                    Log.d(TAG, "Skipping alert ${alert.sosId} (already acknowledged on disk).")
                    return@collect
                }

                Log.d(TAG, "🚨 [DAEMON WAKEUP] EMERGENCY BROADCAST DETECTED: ${alert.message}")
                _activeAlert.value = alert

                // 1. Wake up the phone screen & turn display on
                acquireScreenWakeLock()

                // 2. Post heads-up system alert notification
                EmergencyAlertNotificationHelper.showEmergencyNotification(context, alert)

                // 3. Sound the high-frequency loud beeping alarm
                triggerHighFrequencyEmergencyBeep()
            }
        }
    }

    private fun acquireScreenWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock?.release()
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "pransetu:EmergencyAlertWakeLock"
            ).apply {
                acquire(45000L) // Keep awake for up to 45s while ringing
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire full screen wake lock", e)
        }
    }

    private fun triggerHighFrequencyEmergencyBeep() {
        if (isSirenActive) return
        isSirenActive = true

        // Force maximum alarm volume
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not force maximum volume", e)
        }

        sirenJob = scope.launch(Dispatchers.Default) {
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
                Log.e(TAG, "AudioTrack beep error, falling back to system ringtone", e)
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

        // Repeating intense vibration pulse
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

    fun dismissActiveAlert() {
        Log.d(TAG, "Dismissing active alert and muting audio permanently.")
        isSirenActive = false
        stopAudioTrackInternal()
        sirenJob?.cancel()
        sirenJob = null

        try {
            fallbackRingtone?.stop()
            fallbackRingtone = null
        } catch (_: Exception) {}

        try {
            wakeLock?.release()
            wakeLock = null
        } catch (_: Exception) {}

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

        val alert = _activeAlert.value
        if (alert != null) {
            alertStore.markAlertAcknowledged(alert.sosId)
        }

        // Transmit acknowledgment to Supabase
        scope.launch {
            try {
                val profileStore = com.pransetu.app.core.data.local.UserProfileStore(context)
                val phone = profileStore.getUserPhoneSync()
                val name = profileStore.getUserNameSync().ifBlank { "Citizen" }

                val ackPayload = JSONObject().apply {
                    put("event_type", "EMERGENCY_BROADCAST_ACKNOWLEDGED")
                    put("source", "android_app")
                    put("user_id", phone)
                    put("occurred_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date()))
                    put("payload", JSONObject().apply {
                        put("citizen_name", name)
                        put("citizen_phone", phone)
                        put("status", "ACKNOWLEDGED")
                        put("ack_timestamp", System.currentTimeMillis())
                    })
                }

                SupabaseClient.post("realtime_events", ackPayload.toString())
                Log.d(TAG, "✅ Acknowledgment transmitted to Supabase for $name ($phone)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to transmit acknowledgment", e)
            }
        }

        _activeAlert.value = null
    }
}
