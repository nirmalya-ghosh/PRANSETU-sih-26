package com.pransetu.app.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class TacticalCompassState(
    val currentHeadingDegrees: Float = 0f,
    val targetBearingDegrees: Float = 0f,
    val relativeArrowAngle: Float = 0f,
    val distanceMeters: Float = 0f,
    val targetShelterName: String = "Designated Cyclone Shelter",
    val cardinalDirection: String = "N",
    val isSensorAvailable: Boolean = true
)

/**
 * Tactical Offline Compass & Shelter Bearing Engine:
 * 1. Computes real-time azimuth heading (0° - 360°) using device Magnetometer / Rotation Vector.
 * 2. Calculates true line-of-sight bearing vector to the nearest safe shelter without internet or maps.
 * 3. Provides smooth low-pass filtered rotational telemetry for tactical HUD dials.
 */
class TacticalCompassManager(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val _compassState = MutableStateFlow(TacticalCompassState())
    val compassState: StateFlow<TacticalCompassState> = _compassState.asStateFlow()

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var currentHeadingSmoothed = 0f
    private val alpha = 0.15f // Low-pass filter smoothing coefficient

    // Target Shelter Coordinates (Default to Odisha Coastal Multi-Purpose Shelter)
    private var targetLatitude = 20.3000
    private var targetLongitude = 85.8300
    private var shelterName = "Nearest High-Ground Cyclone Shelter"

    // User GPS location
    private var userLatitude = 20.2961
    private var userLongitude = 85.8245

    fun start() {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            if (rotationSensor != null) {
                sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
                magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            }
        } catch (e: Exception) {
            Log.e("TacticalCompass", "Failed to start compass sensors", e)
            _compassState.value = _compassState.value.copy(isSensorAvailable = false)
        }
    }

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.e("TacticalCompass", "Error stopping compass", e)
        }
    }

    fun updateTargetShelter(latitude: Double, longitude: Double, name: String) {
        targetLatitude = latitude
        targetLongitude = longitude
        shelterName = name
        recalculateVector()
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        recalculateVector()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var azimuthRadians = 0f

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuthRadians = orientation[0]
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, 3)
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientation)
                azimuthRadians = orientation[0]
            }
        }

        var azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        if (azimuthDegrees < 0) azimuthDegrees += 360f

        // Apply low-pass smoothing
        currentHeadingSmoothed = currentHeadingSmoothed + alpha * (azimuthDegrees - currentHeadingSmoothed)
        recalculateVector()
    }

    private fun recalculateVector() {
        val userLoc = Location("User").apply {
            latitude = userLatitude
            longitude = userLongitude
        }
        val shelterLoc = Location("Shelter").apply {
            latitude = targetLatitude
            longitude = targetLongitude
        }

        val distance = userLoc.distanceTo(shelterLoc)
        var targetBearing = userLoc.bearingTo(shelterLoc)
        if (targetBearing < 0) targetBearing += 360f

        val relativeAngle = (targetBearing - currentHeadingSmoothed + 360f) % 360f

        val cardinal = when (currentHeadingSmoothed) {
            in 337.5..360.0, in 0.0..22.5 -> "N"
            in 22.5..67.5 -> "NE"
            in 67.5..112.5 -> "E"
            in 112.5..157.5 -> "SE"
            in 157.5..202.5 -> "S"
            in 202.5..247.5 -> "SW"
            in 247.5..292.5 -> "W"
            in 292.5..337.5 -> "NW"
            else -> "N"
        }

        _compassState.value = TacticalCompassState(
            currentHeadingDegrees = currentHeadingSmoothed,
            targetBearingDegrees = targetBearing,
            relativeArrowAngle = relativeAngle,
            distanceMeters = distance,
            targetShelterName = shelterName,
            cardinalDirection = cardinal,
            isSensorAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
