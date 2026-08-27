package com.pransetu.app.core.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Automatically launches the Emergency Broadcast Daemon upon device boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Device booted. Starting PRANSETU Emergency Broadcast Daemon.")
            EmergencyBroadcastDaemonService.startDaemon(context)
        }
    }
}
