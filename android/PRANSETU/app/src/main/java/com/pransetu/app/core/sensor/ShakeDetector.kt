package com.pransetu.app.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Detects shake gestures using the accelerometer.
 * 
 * When the device is shaken [requiredShakeCount] times within [shakeWindowMs],
 * the [onShakeDetected] callback is invoked.
 * 
 * This enables "Shake-to-SOS" — injured/trapped users can trigger SOS
 * without looking at the screen.
 */
class ShakeDetector(
    private val context: Context,
    private val requiredShakeCount: Int = 3,
    private val shakeThreshold: Float = 15f,
    private val shakeWindowMs: Long = 2000L,
    private val cooldownMs: Long = 5000L,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    
    private var shakeTimestamps = mutableListOf<Long>()
    private var lastShakeTime = 0L
    private var lastTriggerTime = 0L

    fun start() {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                sensorManager?.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
                )
                Log.d("ShakeDetector", "Shake detection started")
            } else {
                Log.w("ShakeDetector", "No accelerometer available")
            }
        } catch (e: Exception) {
            Log.e("ShakeDetector", "Failed to start shake detection", e)
        }
    }

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
            Log.d("ShakeDetector", "Shake detection stopped")
        } catch (e: Exception) {
            Log.e("ShakeDetector", "Failed to stop shake detection", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate acceleration magnitude minus gravity
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

        if (acceleration > shakeThreshold) {
            val now = System.currentTimeMillis()
            
            // Debounce: ignore shakes within 300ms of each other
            if (now - lastShakeTime < 300) return
            lastShakeTime = now

            // Add this shake to the window
            shakeTimestamps.add(now)

            // Remove old shakes outside the window
            shakeTimestamps.removeAll { now - it > shakeWindowMs }

            Log.d("ShakeDetector", "Shake detected! Count in window: ${shakeTimestamps.size}")

            // Check if we've hit the required count
            if (shakeTimestamps.size >= requiredShakeCount) {
                // Cooldown: don't re-trigger within cooldownMs
                if (now - lastTriggerTime > cooldownMs) {
                    lastTriggerTime = now
                    shakeTimestamps.clear()
                    Log.d("ShakeDetector", "SHAKE-TO-SOS TRIGGERED!")
                    onShakeDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
