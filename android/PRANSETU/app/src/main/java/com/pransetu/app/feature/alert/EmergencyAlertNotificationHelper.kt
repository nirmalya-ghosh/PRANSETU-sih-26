package com.pransetu.app.feature.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pransetu.app.MainActivity
import com.pransetu.app.R
import com.pransetu.app.core.network.SystemAlert

object EmergencyAlertNotificationHelper {

    const val CHANNEL_ID = "pransetu_emergency_broadcast_channel"
    const val NOTIFICATION_ID = 911911
    const val ACTION_DISMISS_EMERGENCY = "com.pransetu.app.ACTION_DISMISS_EMERGENCY"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Emergency Disaster Broadcasts"
            val descriptionText = "Critical public safety disaster warnings and high-frequency emergency alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 200, 800, 200, 800)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showEmergencyNotification(context: Context, alert: SystemAlert) {
        createNotificationChannel(context)

        // Intent to open app directly to the alert
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to dismiss & silence directly from notification bar
        val dismissIntent = Intent(context, EmergencyAlertDismissReceiver::class.java).apply {
            action = ACTION_DISMISS_EMERGENCY
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val severityLabel = when (alert.severityCode) {
            5 -> "🚨 EMERGENCY ALERT: IMMEDIATE EVACUATION"
            4 -> "⚠️ DISASTER WARNING: TAKE SHELTER"
            else -> "📢 PUBLIC SAFETY ADVISORY"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(severityLabel)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "ACKNOWLEDGE & STOP ALARM",
                dismissPendingIntent
            )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
