package com.pransetu.app.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pransetu.app.MainActivity
import com.pransetu.app.R

/**
 * 24/7 Background Daemon Service for PRANSETU Emergency Broadcasts.
 * Ensures the device is always listening for government disaster broadcasts
 * and rings with high-decibel alarm even when the application is closed.
 */
class EmergencyBroadcastDaemonService : Service() {

    companion object {
        private const val TAG = "BroadcastDaemonService"
        private const val DAEMON_CHANNEL_ID = "pransetu_daemon_service_channel"
        private const val DAEMON_NOTIFICATION_ID = 1001

        fun startDaemon(context: Context) {
            val intent = Intent(context, EmergencyBroadcastDaemonService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not start foreground daemon directly, starting standard service", e)
                try { context.startService(intent) } catch (_: Exception) {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "EmergencyBroadcastDaemonService created.")
        createDaemonChannel()
        startForeground(DAEMON_NOTIFICATION_ID, buildDaemonNotification())
        // Start singleton emergency alert engine
        EmergencyAlertEngine.getInstance(this).startContinuousMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "EmergencyBroadcastDaemonService running in background.")
        EmergencyAlertEngine.getInstance(this).startContinuousMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createDaemonChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DAEMON_CHANNEL_ID,
                "PRANSETU Disaster Lifeline Daemon",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors live emergency broadcasts in the background"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildDaemonNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, DAEMON_CHANNEL_ID)
            .setContentTitle("PRANSETU Emergency Lifeline Active")
            .setContentText("Listening for public disaster broadcasts 24/7")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
}
