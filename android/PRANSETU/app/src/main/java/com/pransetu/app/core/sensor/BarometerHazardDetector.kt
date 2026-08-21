package com.pransetu.app.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BarometerReading(
    val pressureHpa: Float = 1013.25f,
    val isHardwareSensor: Boolean = false,
    val tendency: String = "STEADY",
    val cycloneRiskLevel: String = "NORMAL",
    val warningMessage: String? = null
) {
    val isCyclonePressureDrop: Boolean
        get() = cycloneRiskLevel != "NORMAL" || tendency == "FALLING_RAPID" || pressureHpa < 995f

    val calculatedAltitudeMeters: Float
        get() {
            val ratio = (pressureHpa / 1013.25).coerceAtLeast(0.01)
            return (44330.0 * (1.0 - Math.pow(ratio, 0.1903))).toFloat()
        }
}

/**
 * Intelligent Barometric Sensor & Cyclone Pressure Drop Detector.
 * Monitors real-time atmospheric pressure in hPa.
 * Rapid drops below 990 hPa indicate imminent cyclonic depression / storm surge.
 */
class BarometerHazardDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val pressureSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val _readingFlow = MutableStateFlow(
        BarometerReading(
            pressureHpa = 1008.4f,
            isHardwareSensor = pressureSensor != null,
            tendency = "STEADY",
            cycloneRiskLevel = "NORMAL",
            warningMessage = null
        )
    )
    val readingFlow: StateFlow<BarometerReading> = _readingFlow.asStateFlow()

    private var previousPressure = 1013.25f

    fun start() {
        if (pressureSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val delta = pressure - previousPressure
            previousPressure = pressure

            val tendency = when {
                delta < -2.0f -> "FALLING_RAPID"
                delta < -0.8f -> "FALLING_MODERATE"
                delta > 1.0f -> "RISING"
                else -> "STEADY"
            }

            val (riskLevel, warning) = when {
                pressure < 970.0f -> "SEVERE_CYCLONE" to "⚠️ EXTREME CYCLONIC PRESSURE DROP (<970 hPa)! Move to concrete shelter immediately."
                pressure < 990.0f -> "HIGH_STORM_SURGE" to "🌪️ Low Pressure Cyclonic Depression (<990 hPa) detected by device barometer."
                pressure < 1000.0f -> "ELEVATED" to "🌧️ Atmospheric pressure dropping (1000 hPa). Storm conditions approaching."
                else -> "NORMAL" to null
            }

            _readingFlow.value = BarometerReading(
                pressureHpa = pressure,
                isHardwareSensor = true,
                tendency = tendency,
                cycloneRiskLevel = riskLevel,
                warningMessage = warning
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
