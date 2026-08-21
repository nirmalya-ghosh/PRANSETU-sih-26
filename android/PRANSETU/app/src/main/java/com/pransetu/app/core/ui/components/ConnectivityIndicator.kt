package com.pransetu.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pransetu.app.core.location.LocationStatus
import com.pransetu.app.core.network.NetworkStatus

@Composable
fun RealtimeConnectivityIndicator(
    networkStatus: NetworkStatus,
    locationStatus: LocationStatus,
    isMeshEnabled: Boolean,
    peerCount: Int,
    isPowerSaveMode: Boolean = false,
    batteryPercent: Int = 100,
    modifier: Modifier = Modifier
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    val isOnline = networkStatus == NetworkStatus.Available
    val isMeshActive = isMeshEnabled && peerCount > 0

    val containerColor = when {
        isOnline -> Color(0xFF065F46) // Emerald dark
        isMeshActive -> Color(0xFF00695C) // Teal dark
        isMeshEnabled -> Color(0xFFE65100) // Amber/Orange
        else -> Color(0xFF37474F) // Gray/Blue dark
    }

    val contentColor = Color.White

    val label = when {
        isOnline -> "📶 4G/Wi-Fi Online"
        isMeshActive -> "🔗 Mesh Relay ($peerCount Peers)"
        isMeshEnabled && isPowerSaveMode -> "⚡ Eco-Mesh Scanning"
        isMeshEnabled -> "📡 Mesh Scanning..."
        else -> "❌ AirGap Offline"
    }

    val icon: ImageVector = when {
        isOnline -> Icons.Default.SignalCellular4Bar
        isMeshActive -> Icons.Default.Hub
        isMeshEnabled -> Icons.Default.CellTower
        else -> Icons.Default.CloudOff
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { showDetailDialog = true },
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOnline || isMeshActive) Color(0xFF64FFDA) else if (isMeshEnabled) Color(0xFFFFD54F) else Color(0xFFEF5350))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (isPowerSaveMode) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Power Save",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showDetailDialog) {
        ConnectivityDetailDialog(
            networkStatus = networkStatus,
            locationStatus = locationStatus,
            isMeshEnabled = isMeshEnabled,
            peerCount = peerCount,
            isPowerSaveMode = isPowerSaveMode,
            batteryPercent = batteryPercent,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
private fun ConnectivityDetailDialog(
    networkStatus: NetworkStatus,
    locationStatus: LocationStatus,
    isMeshEnabled: Boolean,
    peerCount: Int,
    isPowerSaveMode: Boolean,
    batteryPercent: Int,
    onDismiss: () -> Unit
) {
    val isOnline = networkStatus == NetworkStatus.Available

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PRANSETU Network Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TelemetryRow(
                    title = "Internet Gateway",
                    subtitle = if (isOnline) "Direct cloud connection available" else "Degraded/Disconnected (Operating Offline)",
                    status = if (isOnline) "ONLINE" else "OFFLINE",
                    isGood = isOnline,
                    icon = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff
                )

                TelemetryRow(
                    title = "Nearby Mesh Relays",
                    subtitle = if (isMeshEnabled) "$peerCount devices within Bluetooth/Wi-Fi range" else "Mesh transport disabled",
                    status = if (isMeshEnabled && peerCount > 0) "$peerCount PEERS" else if (isMeshEnabled) "DISCOVERING" else "DISABLED",
                    isGood = isMeshEnabled && peerCount > 0,
                    icon = Icons.Default.Hub
                )

                TelemetryRow(
                    title = "GPS Satellite Fix",
                    subtitle = if (locationStatus == LocationStatus.Available) "High-precision GPS available" else "Location searching/denied",
                    status = if (locationStatus == LocationStatus.Available) "LOCKED" else "SEARCHING",
                    isGood = locationStatus == LocationStatus.Available,
                    icon = Icons.Default.LocationOn
                )

                TelemetryRow(
                    title = "Battery & Adaptive Power",
                    subtitle = if (isPowerSaveMode) "Eco-Mesh active: throttled duty cycle to preserve battery" else "Normal battery state ($batteryPercent%)",
                    status = if (isPowerSaveMode) "ECO-MODE" else "$batteryPercent%",
                    isGood = !isPowerSaveMode,
                    icon = Icons.Default.BatteryFull
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TelemetryRow(
    title: String,
    subtitle: String,
    status: String,
    isGood: Boolean,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGood) Color(0xFF10B981) else Color(0xFFEF5350),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                color = (if (isGood) Color(0xFF10B981) else Color(0xFFEF5350)).copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isGood) Color(0xFF10B981) else Color(0xFFEF5350),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
