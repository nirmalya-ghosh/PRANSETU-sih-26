package com.pransetu.app.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

enum class ManDownState {
    GUARDING,
    IMPACT_DETECTED,
    COUNTDOWN_ACTIVE,
    TRIGGERED,
    CANCELLED
}

data class ManDownTelemetry(
    val state: ManDownState = ManDownState.GUARDING,
    val countdownSeconds: Int = 0,
    val lastImpactGForce: Float = 0f,
    val isUnderRubble: Boolean = false,
    val ambientLux: Float = 50f
)

/**
 * Autonomous Man-Down, Earthquake Shock & Collapse Detector:
 * 1. Measures high-G impact / violent collapse shocks (>25 m/s²).
 * 2. Monitors for post-impact unconscious stillness.
 * 3. Inspects ambient light to detect entrapment under debris (0 lux).
 * 4. Runs an autonomous 10-second countdown before firing emergency SOS packets.
 */
class ManDownDetector(
    private val context: Context,
    private val onAutonomousSosTriggered: (reason: String, isUnderRubble: Boolean) -> Unit
) : SensorEventListener {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var countdownJob: Job? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lightSensor: Sensor? = null

    private val _telemetry = MutableStateFlow(ManDownTelemetry())
    val telemetry: StateFlow<ManDownTelemetry> = _telemetry.asStateFlow()

    private var lastImpactTime = 0L
    private var postImpactCheckStartTime = 0L
    private var isMonitoringStillness = false
    private var currentLux = 50f

    // Stillness thresholds (m/s²)
    private val impactThreshold = 24.5f // ~2.5G shock
    private val stillThreshold = 1.8f
    private val requiredStillnessDurationMs = 6000L // 6s of complete immobility after shock

    fun start() {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            lightSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            Log.d("ManDownDetector", "Autonomous Man-Down & Collapse Guard active")
        } catch (e: Exception) {
            Log.e("ManDownDetector", "Failed to start Man-Down guard", e)
        }
    }

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
            countdownJob?.cancel()
            countdownJob = null
            _telemetry.value = ManDownTelemetry()
        } catch (e: Exception) {
            Log.e("ManDownDetector", "Error stopping Man-Down guard", e)
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        isMonitoringStillness = false
        _telemetry.value = _telemetry.value.copy(
            state = ManDownState.GUARDING,
            countdownSeconds = 0
        )
    }

    fun simulateTriggerForTest() {
        triggerCountdown(gForce = 3.2f)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                currentLux = event.values[0]
                _telemetry.value = _telemetry.value.copy(
                    ambientLux = currentLux,
                    isUnderRubble = currentLux <= 2.0f
                )
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                val currentTime = System.currentTimeMillis()

                if (_telemetry.value.state == ManDownState.COUNTDOWN_ACTIVE ||
                    _telemetry.value.state == ManDownState.TRIGGERED) {
                    return
                }

                // 1. Detect Violent Shock / Impact
                if (magnitude >= impactThreshold && !isMonitoringStillness) {
                    lastImpactTime = currentTime
                    isMonitoringStillness = true
                    postImpactCheckStartTime = currentTime
                    val gForce = magnitude / 9.81f
                    _telemetry.value = _telemetry.value.copy(
                        state = ManDownState.IMPACT_DETECTED,
                        lastImpactGForce = gForce
                    )
                    Log.i("ManDownDetector", "High-G impact detected: ${gForce}G. Monitoring stillness...")
                    return
                }

                // 2. Monitor Post-Impact Immobility (Unconsciousness / Rubble Entrapment)
                if (isMonitoringStillness) {
                    // Deviation from gravity vector (9.81 m/s²)
                    val deviation = kotlin.math.abs(magnitude - 9.81f)
                    if (deviation > stillThreshold) {
                        // User moved — false alarm / recovered
                        if (currentTime - postImpactCheckStartTime > 1500L) {
                            isMonitoringStillness = false
                            _telemetry.value = _telemetry.value.copy(state = ManDownState.GUARDING)
                        }
                    } else {
                        // Continued stillness
                        if (currentTime - postImpactCheckStartTime >= requiredStillnessDurationMs) {
                            isMonitoringStillness = false
                            triggerCountdown(_telemetry.value.lastImpactGForce)
                        }
                    }
                }
            }
        }
    }

    private fun triggerCountdown(gForce: Float) {
        if (_telemetry.value.state == ManDownState.COUNTDOWN_ACTIVE) return

        val isDebris = currentLux <= 2.0f
        _telemetry.value = _telemetry.value.copy(
            state = ManDownState.COUNTDOWN_ACTIVE,
            countdownSeconds = 10,
            lastImpactGForce = gForce,
            isUnderRubble = isDebris
        )

        countdownJob?.cancel()
        countdownJob = coroutineScope.launch {
            for (sec in 10 downTo 1) {
                if (!isActive) break
                _telemetry.value = _telemetry.value.copy(countdownSeconds = sec)
                delay(1000)
            }

            if (isActive && _telemetry.value.state == ManDownState.COUNTDOWN_ACTIVE) {
                _telemetry.value = _telemetry.value.copy(
                    state = ManDownState.TRIGGERED,
                    countdownSeconds = 0
                )
                val reason = if (isDebris) {
                    "AUTONOMOUS_RUBBLE_ENTRAPMENT_COLLAPSE (${String.format(java.util.Locale.US, "%.1f", gForce)}G)"
                } else {
                    "AUTONOMOUS_MAN_DOWN_FALL_DETECTED (${String.format(java.util.Locale.US, "%.1f", gForce)}G)"
                }
                withContext(Dispatchers.Main) {
                    onAutonomousSosTriggered(reason, isDebris)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
