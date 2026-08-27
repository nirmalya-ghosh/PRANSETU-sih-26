package com.pransetu.app.feature.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EmergencyAlertDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == EmergencyAlertNotificationHelper.ACTION_DISMISS_EMERGENCY) {
            Log.d("EmergencyAlertReceiver", "Citizen tapped 'ACKNOWLEDGE & STOP ALARM' on System Notification.")
            EmergencyAlertNotificationHelper.cancelNotification(context)
            AlertViewModel.globalDismiss()
        }
    }
}
