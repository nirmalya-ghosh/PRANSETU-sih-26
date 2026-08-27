package com.pransetu.app.feature.tactical

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.PransetuApplication
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
import androidx.compose.ui.tooling.preview.Preview
import com.pransetu.app.ui.theme.PRANSETUTheme
import com.pransetu.app.core.sensor.ManDownTelemetry
import com.pransetu.app.core.sensor.TacticalCompassState
import com.pransetu.app.core.sensor.BarometerReading
import com.pransetu.app.core.hardware.HardwareHealthState
import com.pransetu.app.core.location.PrecisionLocationData

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

    val locationProvider = remember { app?.locationProvider ?: com.pransetu.app.core.location.LocationProvider(context) }
    val liveLocation by locationProvider.liveLocationFlow.collectAsStateWithLifecycle()

    val compassManager = remember { TacticalCompassManager(context) }
    val compassState by compassManager.compassState.collectAsStateWithLifecycle()

    LaunchedEffect(liveLocation) {
        liveLocation?.let {
            compassManager.updateUserLocation(it.latitude, it.longitude)
        }
    }

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

    TacticalTerminalScreenContent(
        myDeviceName = nearbyManager?.myDeviceName ?: "This Device",
        connectedPeers = connectedPeers,
        meshLogs = meshLogs,
        isMeshActive = isMeshActive,
        isBeaconActive = isBeaconActive,
        liveLocation = liveLocation,
        compassState = compassState,
        barometerReading = barometerReading,
        hardwareHealth = hardwareHealth,
        manDownTelemetry = manDownTelemetry,
        onNavigateBack = onNavigateBack,
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
        },
        onToggleBeacon = { beaconManager.toggleBeacon() },
        onCancelManDown = { manDownDetector.cancelCountdown() }
    )
}

@Composable
fun TacticalTerminalScreenContent(
    myDeviceName: String,
    connectedPeers: List<MeshPeerNode>,
    meshLogs: List<MeshRelayLog>,
    isMeshActive: Boolean,
    isBeaconActive: Boolean,
    liveLocation: PrecisionLocationData?,
    compassState: TacticalCompassState,
    barometerReading: BarometerReading,
    hardwareHealth: HardwareHealthState,
    manDownTelemetry: ManDownTelemetry,
    onNavigateBack: () -> Unit,
    onForceFloodSos: () -> Unit,
    onToggleBeacon: () -> Unit,
    onCancelManDown: () -> Unit
) {
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
                myDeviceName = myDeviceName,
                connectedPeers = connectedPeers,
                isMeshActive = isMeshActive,
                latestLog = meshLogs.firstOrNull(),
                onForceFloodSos = onForceFloodSos
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Mesh Terminal Log",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashlightOn,
                                contentDescription = null,
                                tint = if (isBeaconActive) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Optical SOS Beacon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isBeaconActive) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

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
                        onClick = onToggleBeacon,
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

            // 1.5 High-Precision GNSS Multi-Sensor Telemetry Card (1 Hz Continuous Stream)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = if (liveLocation?.isHighPrecision == true) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High-Precision GNSS Telemetry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = if (liveLocation != null) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (liveLocation != null) "1 Hz LIVE GPS" else "ACQUIRING FIX",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = if (liveLocation != null) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Coordinate Readout Capsule
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LIVE COORDINATES",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = liveLocation?.let { "%.6f°, %.6f°".format(it.latitude, it.longitude) } ?: "Searching Satellites...",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = if (liveLocation?.isHighPrecision == true) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = liveLocation?.formatAccuracy() ?: "± --m",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = if (liveLocation?.isHighPrecision == true) Color(0xFF10B981) else Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dual Metric Grid: Altitude + Satellite Constellations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "BAROMETER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", barometerReading.pressureHpa)} hPa",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "SATELLITE LOCK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if ((liveLocation?.satellitesInView ?: 0) > 0)
                                        "${liveLocation?.satellitesUsedInFix ?: 0}/${liveLocation?.satellitesInView ?: 0} Locked"
                                    else
                                        "Multi-GNSS Fused",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    if (liveLocation != null && liveLocation.activeConstellations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active Constellations: ${liveLocation.activeConstellations.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tactical Shelter Compass HUD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = if (compassState.isDeviceLevel) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (compassState.isDeviceLevel) "LEVEL CALIBRATED" else "HOLD LEVEL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = if (compassState.isDeviceLevel) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Hardware-fused line-of-sight magnetic vector to nearest high-ground refuge.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Advanced Tactical Compass Dial
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .background(Color(0xFF0B1120), shape = CircleShape)
                            .border(2.dp, Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background Grid & Dial Graduation Ticks
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.minDimension / 2f

                            // Outer Range Ring
                            drawCircle(
                                color = Color(0xFF1E293B),
                                radius = radius - 8.dp.toPx(),
                                style = Stroke(width = 1.5f)
                            )
                            // Inner Tactical Ring
                            drawCircle(
                                color = Color(0xFF334155).copy(alpha = 0.6f),
                                radius = radius - 32.dp.toPx(),
                                style = Stroke(width = 1f)
                            )
                            // Crosshairs
                            drawLine(
                                color = Color(0xFF334155).copy(alpha = 0.3f),
                                start = Offset(center.x, 16.dp.toPx()),
                                end = Offset(center.x, size.height - 16.dp.toPx()),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF334155).copy(alpha = 0.3f),
                                start = Offset(16.dp.toPx(), center.y),
                                end = Offset(size.width - 16.dp.toPx(), center.y),
                                strokeWidth = 1f
                            )

                            // 360-degree ticks
                            for (degree in 0 until 360 step 15) {
                                val angleRad = Math.toRadians((degree - 90).toDouble())
                                val isMajor = degree % 45 == 0
                                val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                                val startR = radius - 8.dp.toPx()
                                val endR = startR - tickLength

                                val startX = center.x + (startR * Math.cos(angleRad)).toFloat()
                                val startY = center.y + (startR * Math.sin(angleRad)).toFloat()
                                val endX = center.x + (endR * Math.cos(angleRad)).toFloat()
                                val endY = center.y + (endR * Math.sin(angleRad)).toFloat()

                                drawLine(
                                    color = if (isMajor) Color(0xFF64748B) else Color(0xFF334155),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = if (isMajor) 2f else 1f
                                )
                            }
                        }

                        // Rotating Cardinal Points (N, S, E, W, NE, SE, SW, NW)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(-compassState.currentHeadingDegrees),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "N",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                            )
                            Text(
                                "S",
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                            )
                            Text(
                                "E",
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 12.dp)
                            )
                            Text(
                                "W",
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 12.dp)
                            )
                        }

                        // Live Dynamic Shelter Vector Needle
                        Canvas(
                            modifier = Modifier
                                .size(130.dp)
                                .rotate(compassState.targetBearingDegrees - compassState.currentHeadingDegrees)
                        ) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f

                            // North / Shelter Vector Pointer (Emerald Laser Arrow)
                            val pointerPath = Path().apply {
                                moveTo(centerX, 4.dp.toPx())
                                lineTo(centerX + 14.dp.toPx(), centerY)
                                lineTo(centerX, centerY - 8.dp.toPx())
                                lineTo(centerX - 14.dp.toPx(), centerY)
                                close()
                            }
                            drawPath(pointerPath, color = Color(0xFF10B981))

                            // Counter-balance tail
                            val tailPath = Path().apply {
                                moveTo(centerX, size.height - 4.dp.toPx())
                                lineTo(centerX + 10.dp.toPx(), centerY)
                                lineTo(centerX, centerY + 6.dp.toPx())
                                lineTo(centerX - 10.dp.toPx(), centerY)
                                close()
                            }
                            drawPath(tailPath, color = Color(0xFF475569))

                            // Center Pivot Hub
                            drawCircle(color = Color(0xFF0F172A), radius = 10.dp.toPx(), center = Offset(centerX, centerY))
                            drawCircle(color = Color(0xFF10B981), radius = 5.dp.toPx(), center = Offset(centerX, centerY))
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(centerX, centerY))
                        }

                        // Digital HUD Center Readout Capsule
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 70.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${compassState.currentHeadingDegrees.toInt().toString().padStart(3, '0')}° ${compassState.cardinalDirection}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Shelter Telemetry Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target Bearing: ${compassState.targetBearingDegrees.toInt()}°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", compassState.distanceMeters / 1000f)} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = compassState.relativeDirectionText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (compassState.relativeDirectionText.contains("TARGET")) Color(0xFF10B981) else Color(0xFF38BDF8)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Man-Down Guard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Structured Dual Telemetry Metric Boxes (No overlapping text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = "LAST SHOCK FORCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", manDownTelemetry.lastImpactGForce)} G",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (manDownTelemetry.lastImpactGForce > 3.0f) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = "SENSOR STATUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Active Guarding",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Real-Time Barometer Sensor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Air, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Barometer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "LIVE TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${String.format(Locale.US, "%.1f", barometerReading.pressureHpa)} hPa",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Real-Time Atmospheric Pressure",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            text = "Hardware & Thermal Health",
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
                        onClick = onCancelManDown,
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

@Preview(showBackground = true)
@Composable
fun TacticalTerminalScreenPreview() {
    PRANSETUTheme {
        TacticalTerminalScreenContent(
            myDeviceName = "Pixel 7 Pro",
            connectedPeers = listOf(),
            meshLogs = listOf(),
            isMeshActive = true,
            isBeaconActive = false,
            liveLocation = null,
            compassState = TacticalCompassState(),
            barometerReading = BarometerReading(pressureHpa = 1012.5f),
            hardwareHealth = HardwareHealthState(),
            manDownTelemetry = ManDownTelemetry(),
            onNavigateBack = {},
            onForceFloodSos = {},
            onToggleBeacon = {},
            onCancelManDown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TacticalTerminalScreenManDownPreview() {
    PRANSETUTheme {
        TacticalTerminalScreenContent(
            myDeviceName = "Pixel 7 Pro",
            connectedPeers = listOf(),
            meshLogs = listOf(),
            isMeshActive = true,
            isBeaconActive = false,
            liveLocation = null,
            compassState = TacticalCompassState(),
            barometerReading = BarometerReading(pressureHpa = 1008.2f),
            hardwareHealth = HardwareHealthState(),
            manDownTelemetry = ManDownTelemetry(
                state = ManDownState.COUNTDOWN_ACTIVE,
                countdownSeconds = 8,
                lastImpactGForce = 4.5f
            ),
            onNavigateBack = {},
            onForceFloodSos = {},
            onToggleBeacon = {},
            onCancelManDown = {}
        )
    }
}
