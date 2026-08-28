package com.pransetu.app.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pransetu.app.MainActivity

object AppNotificationManager {

    const val CHANNEL_EMERGENCY = "pransetu_emergency_broadcast_channel"
    const val CHANNEL_SOS = "pransetu_sos_channel"
    const val CHANNEL_MESH = "pransetu_mesh_relay_channel"
    const val CHANNEL_SYSTEM = "pransetu_system_events_channel"

    private const val NOTIFICATION_ID_SOS = 1001
    private const val NOTIFICATION_ID_SOS_DELIVERED = 1002
    private const val NOTIFICATION_ID_MESH = 1003
    private const val NOTIFICATION_ID_NETWORK = 1004
    private const val NOTIFICATION_ID_BATTERY = 1005
    private const val NOTIFICATION_ID_SAFE = 1006

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Emergency Disaster Broadcast Channel (Max Priority / Siren)
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "🚨 Critical Disaster Broadcasts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority emergency disaster sirens and evacuation warnings"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 200, 800, 200, 800)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // 2. SOS Signal & Delivery Channel
            val sosChannel = NotificationChannel(
                CHANNEL_SOS,
                "🆘 SOS Distress & Rescue Tracking",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active SOS transmission status, multi-hop relay progress, and rescue dispatches"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // 3. Mesh Relay & Peer Discovery Channel
            val meshChannel = NotificationChannel(
                CHANNEL_MESH,
                "📡 Autonomous Mesh Relay Network",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Zero-cellular Bluetooth/Wi-Fi mesh packet forwarding and peer discovery"
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // 4. System, Cellular & Battery Alerts Channel
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "⚡ System & Connectivity Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Network status changes, offline storage synchronization, and battery alerts"
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            nm.createNotificationChannels(listOf(emergencyChannel, sosChannel, meshChannel, systemChannel))
        }
    }

    private fun getAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Triggered when citizen initiates an emergency SOS.
     */
    fun notifySosTriggered(context: Context, isOnline: Boolean, message: String) {
        initChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (isOnline) "🚨 SOS Active - Direct EOC Uplink" else "🚨 SOS Active - Mesh Broadcasting"
        val detail = if (isOnline) {
            "Transmitting distress coordinates directly to State Emergency Operations Centre (SEOC)."
        } else {
            "Zero-cellular mode: Broadcasting encrypted SOS packets across Bluetooth/Wi-Fi peer mesh."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SOS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$detail\n\nNotes: \"$message\""))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_SOS, notification)
    }

    /**
     * Triggered when SOS is successfully delivered / acknowledged.
     */
    fun notifySosDelivered(context: Context, hopCount: Int, recipient: String = "Command Center (EOC)") {
        initChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val hopInfo = if (hopCount <= 1) "Direct Uplink" else "$hopCount Hops across Mesh"
        val notification = NotificationCompat.Builder(context, CHANNEL_SOS)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("✅ SOS Delivered & Confirmed")
            .setContentText("Your distress beacon reached $recipient ($hopInfo). Rescue team assigned.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Your distress signal has been verified by the State Disaster Management Authority ($recipient).\n\nRoute: $hopInfo\nStatus: RESCUE EN ROUTE"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_SOS_DELIVERED, notification)
    }

    /**
     * Triggered when this device forwards / relays an SOS packet for a nearby victim.
     */
    fun notifyMeshRelayed(context: Context, originDevice: String, hopCount: Int) {
        initChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_MESH)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("📡 Mesh Relay: Packet Forwarded")
            .setContentText("Relayed emergency distress packet from $originDevice (Hop #$hopCount).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_MESH, notification)
    }

    /**
     * Triggered when network connectivity status changes (Lost / Restored).
     */
    fun notifyNetworkStatus(context: Context, isConnected: Boolean) {
        initChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (isConnected) "🌐 Cellular / Internet Restored" else "⚠️ Cellular Disrupted - Mesh Armed"
        val body = if (isConnected) {
            "Direct EOC uplink active. Synced all pending local offline distress logs."
        } else {
            "No cellular service detected. Zero-cellular Bluetooth/Wi-Fi Direct mesh is actively listening for nearby beacons."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(if (isConnected) android.R.drawable.stat_sys_data_bluetooth else android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_NETWORK, notification)
    }

    /**
     * Triggered when battery drops below 15% (Power Saving Mesh Duty-Cycle activated).
     */
    fun notifyBatteryPowerSave(context: Context, percentage: Int) {
        initChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val body = "Battery at $percentage%. PRANSETU adapted Bluetooth mesh scanning duty-cycle to conserve power for emergency SOS."
        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("🔋 Emergency Battery Saver Mode")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_BATTERY, notification)
    }

    /**
     * Triggered when citizen registers or records a safety check-in.
     */
    fun notifySafetyConfirmed(context: Context, location: String) {
        initChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val body = "Your safe status has been logged at $location and shared with Disaster Command & Family."
        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("🟢 Safe Check-in Recorded")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_SAFE, notification)
    }
}
