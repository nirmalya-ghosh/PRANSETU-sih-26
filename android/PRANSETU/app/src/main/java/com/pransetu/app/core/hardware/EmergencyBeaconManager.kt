package com.pransetu.app.core.hardware

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Manages high-intensity multi-modal Emergency Rescue Beacons:
 * 1. Optical SOS: Strobes Camera LED in International Morse Code (••• — — — •••).
 * 2. High-Decibel Acoustic Siren: Synthesizes a 2.5kHz - 4.0kHz piercing tone via AudioTrack.
 * 3. Tactile SOS: Vibrates device in Morse SOS rhythm.
 */
class EmergencyBeaconManager(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var beaconJob: Job? = null

    private val _isBeaconActive = MutableStateFlow(false)
    val isBeaconActive: StateFlow<Boolean> = _isBeaconActive.asStateFlow()

    private val cameraManager: CameraManager? = try {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    } catch (_: Exception) { null }

    private val torchCameraId: String? by lazy {
        try {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (_: Exception) { null }
    }

    private var audioTrack: AudioTrack? = null
    private var isPlayingSiren = false

    fun toggleBeacon() {
        if (_isBeaconActive.value) {
            stopBeacon()
        } else {
            startBeacon()
        }
    }

    fun startBeacon() {
        if (_isBeaconActive.value) return
        _isBeaconActive.value = true

        startAcousticSiren()

        beaconJob = coroutineScope.launch {
            try {
                // Morse Code Timings (ms):
                // Dot: 150ms ON, 150ms OFF
                // Dash: 450ms ON, 150ms OFF
                // Letter gap: 300ms
                // Word gap: 1000ms
                while (isActive) {
                    // S: • • •
                    repeat(3) {
                        setFlashAndHaptic(true, 150)
                        delay(150)
                        setFlashAndHaptic(false, 0)
                        delay(150)
                    }
                    delay(300)

                    // O: — — —
                    repeat(3) {
                        setFlashAndHaptic(true, 450)
                        delay(450)
                        setFlashAndHaptic(false, 0)
                        delay(150)
                    }
                    delay(300)

                    // S: • • •
                    repeat(3) {
                        setFlashAndHaptic(true, 150)
                        delay(150)
                        setFlashAndHaptic(false, 0)
                        delay(150)
                    }
                    delay(1200) // Word pause before repeating SOS
                }
            } finally {
                setTorch(false)
                stopAcousticSiren()
            }
        }
    }

    fun stopBeacon() {
        _isBeaconActive.value = false
        beaconJob?.cancel()
        beaconJob = null
        setTorch(false)
        stopAcousticSiren()
    }

    private fun setFlashAndHaptic(on: Boolean, durationMs: Long) {
        setTorch(on)
        if (on && durationMs > 0) {
            vibrate(durationMs)
        }
    }

    private fun setTorch(enabled: Boolean) {
        try {
            torchCameraId?.let { id ->
                cameraManager?.setTorchMode(id, enabled)
            }
        } catch (e: Exception) {
            Log.e("EmergencyBeacon", "Failed to set torch mode: $enabled", e)
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    private fun startAcousticSiren() {
        if (isPlayingSiren) return
        isPlayingSiren = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufferSize * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                // Generate sweeping sinusoidal waveform (2500 Hz to 3800 Hz warble)
                val buffer = ShortArray(minBufferSize)
                var phase = 0.0
                var time = 0.0

                while (isPlayingSiren && _isBeaconActive.value) {
                    for (i in buffer.indices) {
                        // Modulate frequency between 2500Hz and 3800Hz with 3Hz warble
                        val frequency = 3150.0 + 650.0 * sin(2.0 * Math.PI * 3.0 * time)
                        phase += 2.0 * Math.PI * frequency / sampleRate
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                        buffer[i] = (sin(phase) * 32000.0).toInt().toShort()
                        time += 1.0 / sampleRate
                    }
                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e("EmergencyBeacon", "Error during acoustic siren playback", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                    audioTrack = null
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopAcousticSiren() {
        isPlayingSiren = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
    }
}
