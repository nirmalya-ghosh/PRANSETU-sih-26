package com.pransetu.app.feature.tactical

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.PransetuApplication
import com.pransetu.app.R
import com.pransetu.app.core.data.repository.SosCanonicalModel
import com.pransetu.app.core.hardware.EmergencyBeaconManager
import com.pransetu.app.core.hardware.HardwareDiagnosticsManager
import com.pransetu.app.core.network.nearby.MeshPeerNode
import com.pransetu.app.core.network.nearby.MeshRelayLog
import com.pransetu.app.core.sensor.BarometerHazardDetector
import com.pransetu.app.core.sensor.ManDownDetector
import com.pransetu.app.core.sensor.ManDownState
import com.pransetu.app.core.sensor.TacticalCompassManager
import com.pransetu.app.core.ui.components.PransetuTopAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TacticalTerminalScreen(
    onNavigateBack: () -> Unit,
    onTriggerSos: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? PransetuApplication
    val nearbyManager = app?.nearbyConnectionsManager

    val beaconManager = remember { EmergencyBeaconManager(context) }
    val isBeaconActive by beaconManager.isBeaconActive.collectAsStateWithLifecycle()

    val compassManager = remember { TacticalCompassManager(context) }
    val compassState by compassManager.compassState.collectAsStateWithLifecycle()

    val barometerDetector = remember { BarometerHazardDetector(context) }
    val barometerReading by barometerDetector.readingFlow.collectAsStateWithLifecycle()

    val hardwareDiagnostics = remember { HardwareDiagnosticsManager(context) }
    val hardwareHealth by hardwareDiagnostics.healthState.collectAsStateWithLifecycle()

    val fallbackPeersFlow = remember { MutableStateFlow<List<MeshPeerNode>>(emptyList()) }
    val fallbackLogsFlow = remember { MutableStateFlow<List<MeshRelayLog>>(emptyList()) }
    val fallbackActiveFlow = remember { MutableStateFlow(true) }

    val connectedPeers by (nearbyManager?.peersFlow ?: fallbackPeersFlow).collectAsStateWithLifecycle()
    val meshLogs by (nearbyManager?.meshLogs ?: fallbackLogsFlow).collectAsStateWithLifecycle()
    val isMeshActive by (nearbyManager?.isMeshActive ?: fallbackActiveFlow).collectAsStateWithLifecycle()

    var showTestBroadcastToast by remember { mutableStateOf<String?>(null) }

    val manDownDetector = remember {
        ManDownDetector(context) { reason, _ ->
            onTriggerSos(reason)
        }
    }
    val manDownTelemetry by manDownDetector.telemetry.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        compassManager.start()
        barometerDetector.start()
        hardwareDiagnostics.start()
        manDownDetector.start()

        onDispose {
            beaconManager.stopBeacon()
            compassManager.stop()
            barometerDetector.stop()
            hardwareDiagnostics.stop()
            manDownDetector.stop()
        }
    }

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = "Tactical Disaster Terminal",
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 0. High-Impact Animated Multi-Hop Flowchart & Interactive Mesh Cockpit
            MeshTransmissionFlowchart(
                myDeviceName = nearbyManager?.myDeviceName ?: "This Device",
                connectedPeers = connectedPeers,
                isMeshActive = isMeshActive,
                latestLog = meshLogs.firstOrNull(),
                onForceFloodSos = {
                    val testSos = SosCanonicalModel(
                        severityCode = 3,
                        peopleCount = 1,
                        medicalRequired = false,
                        message = "🚨 PRANSETU ZERO-CELLULAR MULTI-HOP MESH FLOODING SOS",
                        userName = "Tactical Operator",
                        userPhone = "911-TEST"
                    )
                    nearbyManager?.broadcastOriginSos(testSos)
                    showTestBroadcastToast = "🚨 FORCE BROADCAST TRANSMITTED TO ALL DEVICES!"
                }
            )

            // Real-Time Mesh Event Terminal Log Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Mesh Packet Terminal Log",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${meshLogs.size} Events",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (meshLogs.isEmpty()) {
                                Text(
                                    text = "> [${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] Mesh engine active. Scanning 360° for Bluetooth/Wi-Fi devices...\n> Store-and-forward queue armed.",
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            } else {
                                meshLogs.take(20).forEach { logItem ->
                                    val textColor = when (logItem.eventType) {
                                        "SOS_ORIGINATED" -> Color(0xFFEF4444)
                                        "PACKET_RECEIVED" -> Color(0xFFF59E0B)
                                        "PACKET_FORWARDED" -> Color(0xFF38BDF8)
                                        "GATEWAY_UPLINK", "GATEWAY_SUCCESS" -> Color(0xFF10B981)
                                        "ACK_RECEIVED" -> Color(0xFFA855F7)
                                        "PEER_DISCOVERED", "DEVICE_DISCOVERED" -> Color(0xFF38BDF8)
                                        else -> Color(0xFF94A3B8)
                                    }
                                    Text(
                                        text = "> [${logItem.formattedTime}] ${logItem.message}",
                                        color = textColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. Emergency Optical & Acoustic SOS Beacon Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBeaconActive) Color(0xFFB71C1C) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashlightOn,
                                contentDescription = null,
                                tint = if (isBeaconActive) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Optical & Acoustic SOS Beacon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isBeaconActive) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = if (isBeaconActive) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isBeaconActive) "BEACON ACTIVE" else "READY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isBeaconActive) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Strobes camera LED in Morse SOS (••• — — — •••) and emits a 3.5 kHz piercing siren to guide NDRF rescue boats & night helicopters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBeaconActive) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { beaconManager.toggleBeacon() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBeaconActive) Color.White else MaterialTheme.colorScheme.error,
                            contentColor = if (isBeaconActive) Color(0xFFB71C1C) else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isBeaconActive) Icons.Default.NotificationsActive else Icons.Default.VolumeUp,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBeaconActive) "STOP RESCUE BEACON" else "ACTIVATE 1-TAP RESCUE BEACON",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 2. Offline Tactical Shelter Compass HUD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% Offline Shelter Compass HUD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Direct line-of-sight magnetic vector to nearest cyclone shelter without internet or maps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .background(Color(0xFF0F172A), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF1E293B),
                                radius = size.minDimension / 2f,
                                style = Stroke(width = 4f)
                            )
                            drawCircle(
                                color = Color(0xFF334155),
                                radius = size.minDimension / 2.6f,
                                style = Stroke(width = 1.5f)
                            )
                        }

                        // Rotating Cardinal Bezel
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(-compassState.currentHeadingDegrees),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("N", color = Color(0xFFEF4444), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
                            Text("S", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
                            Text("E", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
                            Text("W", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp))
                        }

                        // Shelter Bearing Needle
                        Canvas(
                            modifier = Modifier
                                .size(110.dp)
                                .rotate(compassState.targetBearingDegrees - compassState.currentHeadingDegrees)
                        ) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f

                            val needlePath = Path().apply {
                                moveTo(centerX, 0f)
                                lineTo(centerX + 16f, centerY)
                                lineTo(centerX - 16f, centerY)
                                close()
                            }
                            drawPath(needlePath, color = Color(0xFF10B981))

                            val bottomNeedlePath = Path().apply {
                                moveTo(centerX, size.height)
                                lineTo(centerX + 12f, centerY)
                                lineTo(centerX - 12f, centerY)
                                close()
                            }
                            drawPath(bottomNeedlePath, color = Color(0xFF64748B))

                            drawCircle(color = Color.White, radius = 6f, center = Offset(centerX, centerY))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = compassState.targetShelterName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Heading: ${compassState.currentHeadingDegrees.toInt()}°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Shelter Distance: ${String.format(Locale.US, "%.1f", compassState.distanceMeters / 1000f)} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Autonomous Man-Down & Debris Entrapment Guard
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Man-Down & Entrapment Guard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = when (manDownTelemetry.state) {
                                ManDownState.GUARDING -> Color(0xFF10B981).copy(alpha = 0.15f)
                                ManDownState.COUNTDOWN_ACTIVE -> Color(0xFFD32F2F).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = manDownTelemetry.state.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (manDownTelemetry.state) {
                                    ManDownState.GUARDING -> Color(0xFF10B981)
                                    ManDownState.COUNTDOWN_ACTIVE -> Color(0xFFD32F2F)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Uses high-frequency accelerometer & optical light sensors. Detects high-G physical impact or building collapse followed by stillness, automatically initiating a 10s countdown to broadcast SOS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Shock Force: ${String.format(Locale.US, "%.1f", manDownTelemetry.lastImpactGForce)}G",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sensor Status: Active Guarding",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Barometric Altimeter & Flood Inundation Sensor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Air, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barometric Altimeter & Flood Sensor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", barometerReading.pressureHpa)} hPa",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Atmospheric Pressure",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", barometerReading.calculatedAltitudeMeters)} m",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00897B)
                            )
                            Text(
                                text = "Estimated Vertical Altitude",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (barometerReading.isCyclonePressureDrop) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFB71C1C).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ RAPID PRESSURE DROP DETECTED — Super Cyclone Proximity Warning",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB71C1C),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // 5. Hardware Diagnostics & Adaptive Duty-Cycle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hardware Health & Thermal Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Battery Level: ${hardwareHealth.batteryLevel}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Temperature: ${hardwareHealth.batteryTemperatureC}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hardwareHealth.batteryTemperatureC > 40f) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Policy: ${hardwareHealth.meshDutyCycleRecommendation}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Man-Down 10-Second Auto-SOS Countdown Modal
        if (manDownTelemetry.state == ManDownState.COUNTDOWN_ACTIVE) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EMERGENCY MAN-DOWN DETECTED",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "A violent impact shock of ${String.format(Locale.US, "%.1f", manDownTelemetry.lastImpactGForce)}G followed by stillness was detected.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        if (manDownTelemetry.isUnderRubble) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Zero ambient light detected (Possible Debris Entrapment)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${manDownTelemetry.countdownSeconds}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F)
                        )
                        Text(
                            text = "Seconds until automatic SOS broadcast",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { manDownDetector.cancelCountdown() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("I AM SAFE / CANCEL SOS", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
