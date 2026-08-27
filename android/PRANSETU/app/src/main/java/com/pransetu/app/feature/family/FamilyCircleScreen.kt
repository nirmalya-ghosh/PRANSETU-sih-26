package com.pransetu.app.feature.family

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.core.data.local.FamilyMemberEntity
import com.pransetu.app.core.data.local.FamilySafetyStatus
import com.pransetu.app.core.ui.components.PransetuTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyCircleScreen(
    viewModel: FamilyCircleViewModel,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissFeedback()
        }
    }

    val selfMember = members.firstOrNull { it.isSelf }
    val nonSelfMembers = members.filter { !it.isSelf }

    val safeCount = members.count { it.status == FamilySafetyStatus.SAFE.name }
    val unknownCount = members.count { it.status == FamilySafetyStatus.UNKNOWN.name }
    val dangerCount = members.count { it.status == FamilySafetyStatus.IN_DANGER.name || it.status == FamilySafetyStatus.NEEDS_HELP.name }

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = com.pransetu.app.core.localization.tr("Family Check-In Circle"),
                canNavigateBack = canNavigateBack,
                navigateUp = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF060B13),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Family Member", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Member", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. HERO SECTION: ONE-TOUCH SAFETY BEACON & "I AM SAFE" BUTTON
            item {
                SafetyBeaconHeroCard(
                    uiState = uiState,
                    onBroadcastSafe = { viewModel.markSelfSafe() }
                )
            }

            // 2. STATUS OVERVIEW METRIC CHIPS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusOverviewMetricCard(
                        label = "Safe",
                        count = safeCount,
                        color = Color(0xFF10B981),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                    StatusOverviewMetricCard(
                        label = "Unknown",
                        count = unknownCount,
                        color = Color(0xFF94A3B8),
                        icon = Icons.Default.HelpOutline,
                        modifier = Modifier.weight(1f)
                    )
                    StatusOverviewMetricCard(
                        label = "Distress",
                        count = dangerCount,
                        color = Color(0xFFEF4444),
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. SECTION HEADER WITH ADD MEMBER QUICK ACTION
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Family & Loved Ones (${members.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF1F5F9),
                        letterSpacing = 0.3.sp
                    )

                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showAddDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add New",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // 4. SELF MEMBER CARD (YOU)
            if (selfMember != null) {
                item {
                    SelfMemberCard(
                        member = selfMember,
                        uiState = uiState
                    )
                }
            }

            // 5. OTHER REGISTERED FAMILY MEMBERS
            if (nonSelfMembers.isNotEmpty()) {
                items(nonSelfMembers, key = { it.id }) { member ->
                    FamilyMemberCard(
                        member = member,
                        onStatusChange = { newStatus -> viewModel.updateStatus(member.id, newStatus) },
                        onDelete = { viewModel.deleteMember(member.id) },
                        onCall = {
                            val cleanNumber = member.phoneNumber.replace(Regex("[^0-9+]"), "")
                            if (cleanNumber.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        },
                        onSendCheckInSms = {
                            val cleanNumber = member.phoneNumber.replace(Regex("[^0-9+]"), "")
                            if (cleanNumber.isNotBlank()) {
                                try {
                                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        context.getSystemService(SmsManager::class.java)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        SmsManager.getDefault()
                                    }
                                    val reqText = "PRANSETU Public Safety Notice: Status check requested by your emergency contact. Please reply with your current safety condition and location."
                                    smsManager.sendTextMessage(cleanNumber, null, reqText, null, null)
                                    Toast.makeText(context, "Safety Status Request: SMS dispatched to ${member.name}.", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "SMS Dispatch Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            } else {
                // EMPTY STATE CARD
                item {
                    EmptyFamilyCircleCard(onAddClick = { showAddDialog = true })
                }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }

    if (showAddDialog) {
        AddFamilyMemberDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, relationship, phone ->
                viewModel.addFamilyMember(name, relationship, phone)
                showAddDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// HERO COMPONENT: ONE-TOUCH SAFETY BEACON & "I AM SAFE" BUTTON
// -----------------------------------------------------------------------------

@Composable
private fun SafetyBeaconHeroCard(
    uiState: FamilyCircleUiState,
    onBroadcastSafe: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "One-Touch Safety Beacon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F5F9)
                            )
                            Text(
                                text = "Zero-Cellular Mesh + Direct SMS Broadcast",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Live Battery Chip for Self
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.selfBattery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = if (uiState.selfBattery.percentage <= 15) Color(0xFFEF4444) else Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.selfBattery.percentage}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFF1F5F9),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Sensor Telemetry Strip
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val loc = uiState.liveLocation
                            val locText = if (loc != null) {
                                "GPS: %.4f°, %.4f° (±%.1fm)".format(loc.latitude, loc.longitude, loc.accuracyMeters)
                            } else {
                                "High-Precision 1s GNSS Active"
                            }
                            Text(
                                text = locText,
                                fontSize = 11.5.sp,
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RE-IMAGINED "I AM SAFE" BUTTON (HIGH-TACTILE CYBER-EMERALD PILL)
                Button(
                    onClick = onBroadcastSafe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .scale(pulseScale),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "I AM SAFE — BROADCAST CHECK-IN",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.4.sp
                                )
                                Text(
                                    text = "Dispatches Instant Mesh Ping + SMS to Family",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// STATUS OVERVIEW METRICS
// -----------------------------------------------------------------------------

@Composable
private fun StatusOverviewMetricCard(
    label: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// SELF USER CARD (YOU)
// -----------------------------------------------------------------------------

@Composable
private fun SelfMemberCard(
    member: FamilyMemberEntity,
    uiState: FamilyCircleUiState
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val lastPing = dateFormat.format(Date(member.lastCheckedInAt))
    val currentBattery = uiState.selfBattery.percentage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with YOU badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF2563EB),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Self • ${member.phoneNumber}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "SAFE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-Time Telemetry Row: GPS & Live Battery
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    val loc = uiState.liveLocation
                    val locText = if (loc != null) {
                        "GPS: %.4f°, %.4f°".format(loc.latitude, loc.longitude)
                    } else {
                        member.lastLocationName
                    }
                    Text(
                        text = locText,
                        fontSize = 11.5.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Actual Live Battery Percentage
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.selfBattery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = if (currentBattery <= 15) Color(0xFFEF4444) else if (currentBattery <= 40) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$currentBattery%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBattery <= 15) Color(0xFFEF4444) else Color(0xFFF1F5F9),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last Live Check-in: $lastPing",
                fontSize = 10.5.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// OTHER FAMILY MEMBERS CARDS (WITH REAL TELEMETRY & CALL/SMS ACTIONS)
// -----------------------------------------------------------------------------

@Composable
private fun FamilyMemberCard(
    member: FamilyMemberEntity,
    onStatusChange: (FamilySafetyStatus) -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onSendCheckInSms: () -> Unit
) {
    val statusColor = when (member.status) {
        FamilySafetyStatus.SAFE.name -> Color(0xFF10B981)
        FamilySafetyStatus.IN_DANGER.name -> Color(0xFFEF4444)
        FamilySafetyStatus.NEEDS_HELP.name -> Color(0xFFF59E0B)
        else -> Color(0xFF94A3B8)
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val lastPing = dateFormat.format(Date(member.lastCheckedInAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Relationship Initials
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.take(2).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = member.relationship,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = member.phoneNumber,
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Safety Status Pill
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = member.status.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location & Actual Battery Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1320), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = member.lastLocationName,
                        fontSize = 11.5.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Actual Battery percentage from Mesh / Telemetry
                if (member.batteryPercent != null) {
                    val batt = member.batteryPercent
                    val battColor = if (batt <= 15) Color(0xFFEF4444) else if (batt <= 40) Color(0xFFF59E0B) else Color(0xFF10B981)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = battColor, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$batt%",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = battColor
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryUnknown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Awaiting Sync",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last Telemetry Sync: $lastPing",
                fontSize = 10.5.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: Call, SMS, Status Override, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direct Phone Call
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // Check-in SMS Ping
                OutlinedButton(
                    onClick = onSendCheckInSms,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ask Status", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // Status Override (Safe)
                IconButton(
                    onClick = { onStatusChange(FamilySafetyStatus.SAFE) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Mark Safe", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                }

                // Delete member
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// EMPTY STATE (WHEN NO FAMILY MEMBERS ADDED YET)
// -----------------------------------------------------------------------------

@Composable
private fun EmptyFamilyCircleCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No Family Members Added Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Add your parents, spouse, or siblings to monitor their real-time safety status, verified GPS coordinates, and phone battery levels during emergencies.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Family Member", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ADD FAMILY MEMBER DIALOG
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFamilyMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRelationship by remember { mutableStateOf("Father") }
    var phone by remember { mutableStateOf("") }
    val relationshipOptions = listOf("Father", "Mother", "Spouse", "Sister", "Brother", "Child", "Relative", "Neighbor", "Friend")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Family Member", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Enter your family member's details to track their status and device battery level:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name (e.g. Maa, Priya)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Relationship Chips
                Column {
                    Text("Relationship", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(relationshipOptions) { rel ->
                            val isSelected = selectedRelationship == rel
                            Surface(
                                color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { selectedRelationship = rel }
                            ) {
                                Text(
                                    text = rel,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (+91 ...)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        val formattedPhone = if (!phone.startsWith("+") && !phone.startsWith("0")) "+91 $phone" else phone
                        onAdd(name.trim(), selectedRelationship, formattedPhone.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add to Circle", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
