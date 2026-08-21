package com.pransetu.app.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HardwareHealthState(
    val batteryLevel: Int = 100,
    val batteryTemperatureC: Float = 28.0f,
    val isCharging: Boolean = false,
    val thermalStatus: String = "OPTIMAL",
    val meshDutyCycleRecommendation: String = "HIGH_PERFORMANCE_MESH"
)

/**
 * Monitors hardware battery and thermal state for disaster survival duty-cycling.
 */
class HardwareDiagnosticsManager(private val context: Context) {

    private val _healthState = MutableStateFlow(HardwareHealthState())
    val healthState: StateFlow<HardwareHealthState> = _healthState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val tempC = tempTenths / 10.0f

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val thermal = when {
                    tempC >= 45.0f -> "CRITICAL_OVERHEAT"
                    tempC >= 39.0f -> "ELEVATED_TEMP"
                    tempC <= 0.0f -> "SUB_FREEZING"
                    else -> "OPTIMAL_TEMP"
                }

                val dutyCycle = when {
                    batteryPct <= 15 -> "EXTREME_ECO_MESH (60s Burst)"
                    batteryPct <= 30 -> "BALANCED_DISASTER_MESH (20s Burst)"
                    else -> "CONTINUOUS_TACTICAL_MESH"
                }

                _healthState.value = HardwareHealthState(
                    batteryLevel = batteryPct,
                    batteryTemperatureC = tempC,
                    isCharging = isCharging,
                    thermalStatus = thermal,
                    meshDutyCycleRecommendation = dutyCycle
                )
            }
        }
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }
}
