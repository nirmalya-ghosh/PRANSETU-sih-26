package com.pransetu.app.core.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryStatus(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val isCriticalBattery: Boolean = false // <= 15% and not charging
)

/**
 * Monitors device battery level and system power-save state.
 * 
 * Used to automatically adapt mesh relay duty-cycle when the device is low on battery,
 * ensuring the citizen's phone stays alive during multi-day disaster blackouts.
 */
class BatteryMonitor(private val context: Context) {

    private val _batteryStatus = MutableStateFlow(getCurrentBatteryStatus())
    val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private var hasNotifiedLowBattery = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED ||
                intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                val status = getCurrentBatteryStatus()
                _batteryStatus.value = status
                Log.d("BatteryMonitor", "Battery updated: $status")

                if (status.isCriticalBattery && !hasNotifiedLowBattery) {
                    hasNotifiedLowBattery = true
                    try {
                        com.pransetu.app.core.network.AppNotificationManager.notifyBatteryPowerSave(
                            this@BatteryMonitor.context,
                            status.percentage
                        )
                    } catch (_: Exception) {}
                } else if (!status.isCriticalBattery) {
                    hasNotifiedLowBattery = false
                }
            }
        }
    }

    private var isRegistered = false

    fun startMonitoring() {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            try {
                context.registerReceiver(receiver, filter)
                isRegistered = true
                _batteryStatus.value = getCurrentBatteryStatus()
            } catch (e: Exception) {
                Log.e("BatteryMonitor", "Failed to register battery receiver", e)
            }
        }
    }

    fun stopMonitoring() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
                isRegistered = false
            } catch (e: Exception) {
                Log.e("BatteryMonitor", "Failed to unregister battery receiver", e)
            }
        }
    }

    private fun getCurrentBatteryStatus(): BatteryStatus {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.registerReceiver(null, intentFilter)
            
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val percentage = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100

            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isSystemPowerSave = powerManager?.isPowerSaveMode == true

            val isCritical = percentage <= 15 && !isCharging

            BatteryStatus(
                percentage = percentage,
                isCharging = isCharging,
                isPowerSaveMode = isSystemPowerSave || isCritical,
                isCriticalBattery = isCritical
            )
        } catch (e: Exception) {
            BatteryStatus()
        }
    }
}
