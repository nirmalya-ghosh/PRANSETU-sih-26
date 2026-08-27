package com.pransetu.app.core.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistent storage for acknowledged and dismissed emergency alert IDs.
 * Stored in SharedPreferences so that app restarts, process recreation,
 * or background polling will NEVER re-trigger a siren for an alert the user already dismissed.
 */
class EmergencyAlertStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pransetu_emergency_alerts", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACKNOWLEDGED_PREFIX = "ack_alert_"
        private const val KEY_LAST_STAND_DOWN_TIME = "last_stand_down_time"
        private const val KEY_LAST_ACKNOWLEDGED_TIME = "last_acknowledged_time"
    }

    fun isAlertAcknowledged(alertId: String): Boolean {
        if (alertId.isBlank()) return false
        return prefs.getBoolean(KEY_ACKNOWLEDGED_PREFIX + alertId, false)
    }

    fun markAlertAcknowledged(alertId: String) {
        if (alertId.isBlank()) return
        prefs.edit()
            .putBoolean(KEY_ACKNOWLEDGED_PREFIX + alertId, true)
            .putLong(KEY_LAST_ACKNOWLEDGED_TIME, System.currentTimeMillis())
            .apply()
    }

    fun recordStandDownTime(timestampMillis: Long) {
        prefs.edit()
            .putLong(KEY_LAST_STAND_DOWN_TIME, timestampMillis)
            .apply()
    }

    fun getLastStandDownTime(): Long {
        return prefs.getLong(KEY_LAST_STAND_DOWN_TIME, 0L)
    }
}
