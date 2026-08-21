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
import kotlin.math.abs
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
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isDeviceLevel: Boolean = true,
    val relativeDirectionText: String = "ON TARGET",
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isSensorAvailable: Boolean = true
)

/**
 * Military-grade Tactical Offline Compass & Shelter Bearing Engine:
 * 1. Hardware Fused Rotation Vector (Gyroscope + Accelerometer + Magnetometer) for zero magnetic jitter.
 * 2. Coordinate System Remapping for device tilt/portrait angle compensation.
 * 3. Continuous circular angle interpolation with low-pass deadband filtering (eliminates 0°/360° jump bugs).
 * 4. Line-of-sight bearing & distance calculation to nearest offline disaster shelter.
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
    private val rawRotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var currentHeadingSmoothed = 0f
    private var currentPitchSmoothed = 0f
    private var currentRollSmoothed = 0f
    private var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    // Smoothing factor (lower = smoother/less jitter, higher = faster response)
    private val headingAlpha = 0.12f
    private val tiltAlpha = 0.15f

    // Target Shelter Coordinates (Default to Odisha Coastal Multi-Purpose Shelter)
    private var targetLatitude = 20.3000
    private var targetLongitude = 85.8300
    private var shelterName = "Nearest High-Ground Cyclone Shelter"

    // User GPS location (Defaults to Bhubaneswar coastal coordinates)
    private var userLatitude = 20.2961
    private var userLongitude = 85.8245

    fun start() {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            // 1. Try Hardware Fused Rotation Vector first
            rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

            if (rotationSensor != null) {
                sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            } else {
                // Fallback to discrete accelerometer and magnetometer
                accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
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

        var hasOrientation = false

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rawRotationMatrix, event.values)
            // Remap coordinate system for vertical phone usage (Portrait HUD orientation)
            SensorManager.remapCoordinateSystem(
                rawRotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedRotationMatrix
            )
            SensorManager.getOrientation(remappedRotationMatrix, orientation)
            hasOrientation = true
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Smooth raw gravity vector
                gravity[0] = gravity[0] + 0.2f * (event.values[0] - gravity[0])
                gravity[1] = gravity[1] + 0.2f * (event.values[1] - gravity[1])
                gravity[2] = gravity[2] + 0.2f * (event.values[2] - gravity[2])
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // Smooth raw geomagnetic vector
                geomagnetic[0] = geomagnetic[0] + 0.2f * (event.values[0] - geomagnetic[0])
                geomagnetic[1] = geomagnetic[1] + 0.2f * (event.values[1] - geomagnetic[1])
                geomagnetic[2] = geomagnetic[2] + 0.2f * (event.values[2] - geomagnetic[2])
            }

            if (SensorManager.getRotationMatrix(rawRotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.remapCoordinateSystem(
                    rawRotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedRotationMatrix
                )
                SensorManager.getOrientation(remappedRotationMatrix, orientation)
                hasOrientation = true
            }
        }

        if (hasOrientation) {
            var azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuthDegrees < 0) azimuthDegrees += 360f

            val pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat()

            // Smooth pitch and roll
            currentPitchSmoothed += tiltAlpha * (pitchDegrees - currentPitchSmoothed)
            currentRollSmoothed += tiltAlpha * (rollDegrees - currentRollSmoothed)

            // Continuous circular angle smoothing (prevents 0°/360° flip jitter)
            var diff = (azimuthDegrees - currentHeadingSmoothed + 180f) % 360f - 180f
            if (diff < -180f) diff += 360f

            // Deadband threshold: filter sub-degree micro jitter
            if (abs(diff) > 0.2f) {
                currentHeadingSmoothed = (currentHeadingSmoothed + headingAlpha * diff + 360f) % 360f
            }

            recalculateVector()
        }
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

        val isLevel = abs(currentPitchSmoothed) < 25f && abs(currentRollSmoothed) < 25f

        val relativeText = when {
            relativeAngle < 5f || relativeAngle > 355f -> "🎯 ON TARGET"
            relativeAngle <= 180f -> "▶ ${relativeAngle.toInt()}° RIGHT"
            else -> "◀ ${(360f - relativeAngle).toInt()}° LEFT"
        }

        _compassState.value = TacticalCompassState(
            currentHeadingDegrees = currentHeadingSmoothed,
            targetBearingDegrees = targetBearing,
            relativeArrowAngle = relativeAngle,
            distanceMeters = distance,
            targetShelterName = shelterName,
            cardinalDirection = cardinal,
            pitchDegrees = currentPitchSmoothed,
            rollDegrees = currentRollSmoothed,
            isDeviceLevel = isLevel,
            relativeDirectionText = relativeText,
            sensorAccuracy = currentAccuracy,
            isSensorAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        currentAccuracy = accuracy
    }
}
