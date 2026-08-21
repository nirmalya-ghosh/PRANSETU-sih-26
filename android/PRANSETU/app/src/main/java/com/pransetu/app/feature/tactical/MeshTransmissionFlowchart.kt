package com.pransetu.app.feature.tactical

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pransetu.app.core.network.nearby.MeshPeerNode
import com.pransetu.app.core.network.nearby.MeshRelayLog

enum class TransmissionStage {
    IDLE_SCANNING,
    ORIGIN_BROADCASTING,
    PEER_RELAYING,
    GATEWAY_UPLINKING,
    EOC_DELIVERED
}

/**
 * High-impact, military-grade Animated Multi-Hop Flowchart & Interactive Topology Cockpit.
 * 
 * Visualizes the entire zero-cellular transmission lifecycle:
 * [Origin Phone] ➔ (Bluetooth/Wi-Fi Direct) ➔ [Discovered In-Range Peers] ➔ [Internet Gateway] ➔ [OSDMA / EOC Server]
 */
@Composable
fun MeshTransmissionFlowchart(
    myDeviceName: String,
    connectedPeers: List<MeshPeerNode>,
    isMeshActive: Boolean,
    latestLog: MeshRelayLog?,
    onForceFloodSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_flow_anim")

    // Pulsing animations
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Flowing laser particle offset (0f to 1f)
    val flowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow_progress"
    )

    val currentStage = when (latestLog?.eventType) {
        "SOS_ORIGINATED" -> TransmissionStage.ORIGIN_BROADCASTING
        "PACKET_RECEIVED", "PACKET_FORWARDED" -> TransmissionStage.PEER_RELAYING
        "GATEWAY_UPLINK" -> TransmissionStage.GATEWAY_UPLINKING
        "GATEWAY_SUCCESS", "ACK_RECEIVED" -> TransmissionStage.EOC_DELIVERED
        else -> if (connectedPeers.isNotEmpty()) TransmissionStage.PEER_RELAYING else TransmissionStage.IDLE_SCANNING
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Dark Tactical HUD background
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: Title + Live Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMeshActive) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ZERO-CELLULAR MULTI-HOP FLOWCHART",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${connectedPeers.size} PEERS IN RANGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (connectedPeers.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle
            Text(
                text = "Live store-and-forward packet flow across in-range devices until reaching an internet gateway to OSDMA / EOC:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- ANIMATED FLOWCHART PIPELINE (Vertical Stack) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Node 1: Origin Device
                FlowchartNodeCard(
                    title = "1. ORIGIN DEVICE (YOU)",
                    subtitle = myDeviceName,
                    badgeText = "SOS TRANSMITTER",
                    badgeColor = Color(0xFFEF4444),
                    icon = Icons.Default.PhoneAndroid,
                    iconTint = Color(0xFFEF4444),
                    isActive = currentStage == TransmissionStage.ORIGIN_BROADCASTING || isMeshActive,
                    pulseScale = if (currentStage == TransmissionStage.ORIGIN_BROADCASTING) pulseScale else 1f
                )

                // Animated Flow Arrow 1 -> 2
                AnimatedFlowConnector(
                    label = "Bluetooth LE & Wi-Fi Direct (P2P Cluster)",
                    progress = flowProgress,
                    isActive = isMeshActive
                )

                // Node 2: Discovered Peers
                val peerDisplayName = if (connectedPeers.isEmpty()) {
                    "Searching 360° for nearby phones..."
                } else {
                    connectedPeers.joinToString(", ") { it.deviceName }
                }

                FlowchartNodeCard(
                    title = "2. DISCOVERED IN-RANGE PEERS",
                    subtitle = peerDisplayName,
                    badgeText = if (connectedPeers.isNotEmpty()) "${connectedPeers.size} FORWARDING NODES" else "SCANNING...",
                    badgeColor = if (connectedPeers.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF64748B),
                    icon = Icons.Default.Sensors,
                    iconTint = Color(0xFF38BDF8),
                    isActive = connectedPeers.isNotEmpty(),
                    pulseScale = if (currentStage == TransmissionStage.PEER_RELAYING) pulseScale else 1f
                )

                // Animated Flow Arrow 2 -> 3
                AnimatedFlowConnector(
                    label = "Store-and-Forward Flooding (TTL - 1, Hop + 1)",
                    progress = flowProgress,
                    isActive = connectedPeers.isNotEmpty()
                )

                // Node 3: Internet Gateway Node
                FlowchartNodeCard(
                    title = "3. INTERNET-CAPABLE GATEWAY NODE",
                    subtitle = "Identifies first peer with 4G/5G Cellular or Wi-Fi coverage",
                    badgeText = if (currentStage >= TransmissionStage.GATEWAY_UPLINKING) "GATEWAY DETECTED" else "AUTO-DETECTING",
                    badgeColor = Color(0xFFF59E0B),
                    icon = Icons.Default.CellTower,
                    iconTint = Color(0xFFF59E0B),
                    isActive = currentStage >= TransmissionStage.GATEWAY_UPLINKING,
                    pulseScale = if (currentStage == TransmissionStage.GATEWAY_UPLINKING) pulseScale else 1f
                )

                // Animated Flow Arrow 3 -> 4
                AnimatedFlowConnector(
                    label = "Encrypted TLS Uplink to Disaster Server",
                    progress = flowProgress,
                    isActive = currentStage >= TransmissionStage.GATEWAY_UPLINKING
                )

                // Node 4: OSDMA / EOC Command Platform
                FlowchartNodeCard(
                    title = "4. OSDMA / EOC DISASTER COMMAND",
                    subtitle = "Odisha State Disaster Management Authority • EOC Live Dashboard",
                    badgeText = if (currentStage == TransmissionStage.EOC_DELIVERED) "DELIVERED & ACK'D" else "READY FOR INGESTION",
                    badgeColor = Color(0xFF10B981),
                    icon = Icons.Default.CloudDone,
                    iconTint = Color(0xFF10B981),
                    isActive = currentStage == TransmissionStage.EOC_DELIVERED,
                    pulseScale = if (currentStage == TransmissionStage.EOC_DELIVERED) pulseScale else 1f
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- TRUE DEVICE ROSTER SECTION ---
            Text(
                text = "TRUE DISCOVERED DEVICE NAMES IN RANGE:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (connectedPeers.isEmpty()) {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Force scanning nearby Bluetooth & Wi-Fi devices...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    connectedPeers.forEach { peer ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothConnected,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = peer.deviceName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Endpoint: ${peer.endpointId} • Cluster Connected",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "READY TO RELAY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FORCE MULTI-DEVICE MESH FLOODING SOS BUTTON ---
            Button(
                onClick = onForceFloodSos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FORCE MULTI-DEVICE MESH FLOODING SOS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun FlowchartNodeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconTint: Color,
    isActive: Boolean,
    pulseScale: Float
) {
    Surface(
        color = if (isActive) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) badgeColor.copy(alpha = 0.8f) else Color(0xFF334155),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isActive) Color.White else Color(0xFF94A3B8)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) Color(0xFFE2E8F0) else Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedFlowConnector(
    label: String,
    progress: Float,
    isActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = size.width / 2f
                val startY = 0f
                val endY = size.height

                // Base connector line
                drawLine(
                    color = if (isActive) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFF334155),
                    start = Offset(startX, startY),
                    end = Offset(startX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Animated glowing laser packet moving down
                if (isActive) {
                    val laserY = startY + (endY - startY) * progress
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 4.5f,
                        center = Offset(startX, laserY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = Offset(startX, laserY)
                    )
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Color(0xFF38BDF8) else Color(0xFF64748B),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
