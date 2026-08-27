package com.pransetu.app.feature.alerts

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.R
import com.pransetu.app.core.localization.EmergencyTerminology
import com.pransetu.app.core.sensor.BarometerHazardDetector
import com.pransetu.app.core.tts.EmergencyVoiceBroadcaster
import com.pransetu.app.core.ui.components.PransetuTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.tooling.preview.Preview
import com.pransetu.app.ui.theme.PRANSETUTheme
import com.pransetu.app.core.data.local.AlertEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    onNavigateToShelters: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val voiceBroadcaster = remember { EmergencyVoiceBroadcaster(context) }
    val isSpeaking by voiceBroadcaster.isSpeakingFlow.collectAsStateWithLifecycle()
    val barometerDetector = remember { BarometerHazardDetector(context) }
    val barometerReading by barometerDetector.readingFlow.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        barometerDetector.start()
        onDispose {
            barometerDetector.stop()
            voiceBroadcaster.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshUserLocation()
    }

    AlertsScreenContent(
        uiState = uiState,
        searchQuery = searchQuery,
        isSearchActive = isSearchActive,
        isSpeaking = isSpeaking,
        barometerHpa = barometerReading.pressureHpa,
        barometerTendency = barometerReading.tendency,
        snackbarHostState = snackbarHostState,
        onSearchQueryChange = { searchQuery = it },
        onToggleSearch = { isSearchActive = !isSearchActive },
        onRefresh = {
            viewModel.refreshLiveDisasterFeeds { _, msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        },
        onNavigateToShelters = onNavigateToShelters,
        onSelectFilter = { viewModel.selectFilter(it) },
        onMarkAsRead = { viewModel.markAsRead(it) },
        onSpeak = { textToSpeak ->
            if (isSpeaking) {
                voiceBroadcaster.stop()
            } else {
                voiceBroadcaster.speak(textToSpeak, "en")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreenContent(
    uiState: AlertsUiState,
    searchQuery: String,
    isSearchActive: Boolean,
    isSpeaking: Boolean,
    barometerHpa: Float,
    barometerTendency: String,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToShelters: () -> Unit,
    onSelectFilter: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    val alerts = remember(uiState.alerts, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.alerts
        } else {
            uiState.alerts.filter { item ->
                item.entity.title.contains(searchQuery, ignoreCase = true) ||
                item.entity.locationName.contains(searchQuery, ignoreCase = true) ||
                (item.entity.affectedDistricts?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.entity.bodyKey?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    val rawAlerts = uiState.alerts
    val seismicCount = rawAlerts.count { it.entity.category.equals("EARTHQUAKE", ignoreCase = true) }
    val weatherCount = rawAlerts.count { it.entity.category.equals("WEATHER", ignoreCase = true) }
    val activeCount = rawAlerts.count { it.entity.severity >= 1 }
    val nearMeCount = rawAlerts.count { (it.distanceKm ?: 9999.0) <= 200.0 }

    val filterTabs = listOf(
        "ALL" to "🌐 All Live Feeds (${rawAlerts.size})",
        "SEISMIC" to "⚡ Seismic Grid (${seismicCount})",
        "WEATHER" to "🌪️ Satellite Doppler (${weatherCount})",
        "ACTIVE" to "🚨 Critical / High (${activeCount})",
        "NEAR_ME" to "📍 Near My GPS (${nearMeCount})"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PransetuTopAppBar(
                title = stringResource(R.string.title_alerts),
                actions = {
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Outlined.Search,
                            contentDescription = "Search Alerts",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh Live Feeds",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Search Bar (Animated Visibility)
            item {
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search by district, hazard, or location...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // 1. Executive Meteorological & Radar Telemetry Strip
            item {
                DisasterTelemetryHUD(
                    userLocation = uiState.userLocation,
                    barometerHpa = barometerHpa,
                    tendency = barometerTendency,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh
                )
            }

            // 2. Critical Impact Zone Ribbon (if inside hazard perimeter)
            item {
                AnimatedVisibility(visible = uiState.impactZoneAlertCount > 0) {
                    CriticalImpactRibbon(onNavigateToShelters = onNavigateToShelters)
                }
            }

            // 3. Category Filter Chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(filterTabs) { (key, label) ->
                        val isSelected = uiState.selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectFilter(key) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // 4. Alerts Content List
            if (alerts.isEmpty()) {
                item {
                    EmptyAlertsState(isSearch = searchQuery.isNotBlank())
                }
            } else {
                items(alerts, key = { it.entity.alertId }) { alertItem ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ExecutiveAlertCard(
                            item = alertItem,
                            onClick = {
                                onMarkAsRead(alertItem.entity.alertId)
                            },
                            onNavigateToShelters = onNavigateToShelters,
                            onSpeak = { textToSpeak ->
                                onSpeak(textToSpeak)
                            },
                            isSpeaking = isSpeaking
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlertsScreenPreview() {
    val mockAlerts = listOf(
        AlertItemUi(
            entity = AlertEntity(
                alertId = "1",
                title = "CYCLONE DANA",
                severity = 3,
                timestamp = System.currentTimeMillis() - 1000000,
                source = "IMD Bhubaneswar",
                bodyKey = "Severe Cyclonic Storm 'DANA' is expected to make landfall between Puri and Sagar Island.",
                category = "WEATHER",
                windSpeed = "120 km/h",
                locationName = "Coastal Odisha",
                actionInstruction = "Evacuate to safe shelters immediately."
            ),
            distanceKm = 45.0,
            isUserInImpactZone = true,
            liveTimeAgoFormatted = "🟢 LIVE • 15m ago"
        ),
        AlertItemUi(
            entity = AlertEntity(
                alertId = "2",
                title = "FLASH FLOOD WATCH",
                severity = 2,
                timestamp = System.currentTimeMillis() - 5000000,
                source = "SRC Odisha",
                bodyKey = "Heavy rainfall leading to potential flash floods in low lying areas of Ganjam district.",
                category = "WEATHER",
                rainfall = "200mm",
                locationName = "Ganjam",
                isUpcoming = true,
                expectedImpactTime = System.currentTimeMillis() + 3600000,
                actionInstruction = "Move to higher ground."
            ),
            distanceKm = 120.0,
            isUserInImpactZone = false,
            timeToImpactFormatted = "Strikes in 1h 0m",
            liveTimeAgoFormatted = "🟢 LIVE • 1h ago"
        )
    )

    PRANSETUTheme {
        AlertsScreenContent(
            uiState = AlertsUiState(
                alerts = mockAlerts,
                impactZoneAlertCount = 1,
                selectedFilter = "ALL"
            ),
            searchQuery = "",
            isSearchActive = false,
            isSpeaking = false,
            barometerHpa = 1012.5f,
            barometerTendency = "Steady",
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onToggleSearch = {},
            onRefresh = {},
            onNavigateToShelters = {},
            onSelectFilter = {},
            onMarkAsRead = {},
            onSpeak = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlertsScreenEmptyPreview() {
    PRANSETUTheme {
        AlertsScreenContent(
            uiState = AlertsUiState(),
            searchQuery = "",
            isSearchActive = false,
            isSpeaking = false,
            barometerHpa = 1013.2f,
            barometerTendency = "Rising",
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onToggleSearch = {},
            onRefresh = {},
            onNavigateToShelters = {},
            onSelectFilter = {},
            onMarkAsRead = {},
            onSpeak = {}
        )
    }
}


@Composable
fun DisasterTelemetryHUD(
    userLocation: android.location.Location?,
    barometerHpa: Float,
    tendency: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .clickable { onRefresh() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = radarAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMD DOPPLER & USGS SEISMIC GRID",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRefreshing) "Syncing..." else "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (userLocation != null)
                                "${String.format(Locale.US, "%.3f", userLocation.latitude)}°N, ${String.format(Locale.US, "%.3f", userLocation.longitude)}°E"
                            else
                                "Odisha Coastal Grid",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-Time Sector Lock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", barometerHpa)} hPa",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (barometerHpa < 995f) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tendency,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CriticalImpactRibbon(onNavigateToShelters: () -> Unit) {
    Surface(
        color = Color(0xFFB71C1C),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.CrisisAlert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CRITICAL: INSIDE DISASTER IMPACT ZONE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Evacuate immediately or seek reinforced shelter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            Button(
                onClick = onNavigateToShelters,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SHELTERS", color = Color(0xFFB71C1C), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExecutiveAlertCard(
    item: AlertItemUi,
    onClick: () -> Unit,
    onNavigateToShelters: () -> Unit,
    onSpeak: (String) -> Unit,
    isSpeaking: Boolean
) {
    val alert = item.entity
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val (severityColor, severityLabel) = when (alert.severity) {
        3 -> Color(0xFFD32F2F) to "RED ALERT"
        2 -> Color(0xFFE65100) to "ORANGE WARNING"
        1 -> Color(0xFFFBC02D) to "YELLOW WATCH"
        else -> Color(0xFF0288D1) to "ADVISORY"
    }

    val hazardIcon = when {
        alert.title.contains("CYCLONE", ignoreCase = true) -> Icons.Default.Air
        alert.title.contains("FLOOD", ignoreCase = true) -> Icons.Default.WaterDrop
        alert.title.contains("TSUNAMI", ignoreCase = true) -> Icons.Default.Waves
        alert.title.contains("EARTHQUAKE", ignoreCase = true) -> Icons.Default.Bolt
        alert.title.contains("LIGHTNING", ignoreCase = true) -> Icons.Default.FlashOn
        else -> Icons.Default.Warning
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val timeString = remember(alert.timestamp) { dateFormat.format(Date(alert.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
                isExpanded = !isExpanded
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isUserInImpactZone) 6.dp else 2.dp)
    ) {
        Column {
            // Solid Severity Top Ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(severityColor)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Header: Severity Tag, Landfall Status & Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = severityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = severityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = severityColor,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (alert.isUpcoming) {
                            Surface(
                                color = Color(0xFFF57C00).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.timeToImpactFormatted ?: "Upcoming",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF57C00),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Active Impact",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Text(
                        text = item.liveTimeAgoFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.liveTimeAgoFormatted.contains("LIVE")) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = severityColor.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(hazardIcon, contentDescription = null, tint = severityColor, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = EmergencyTerminology.getEmergencyTypeName(context, alert.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.locationName + (if (item.distanceKm != null) " • ${String.format(Locale.US, "%.0f", item.distanceKm)} km away" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isUserInImpactZone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (item.isUserInImpactZone) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Description Body
                if (!alert.bodyKey.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = alert.bodyKey,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Structured Telemetry Pills
                if (alert.windSpeed != null || alert.rainfall != null || !alert.affectedDistricts.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (alert.windSpeed != null) {
                            TelemetryBadge(icon = Icons.Outlined.Air, label = alert.windSpeed)
                        }
                        if (alert.rainfall != null) {
                            TelemetryBadge(icon = Icons.Outlined.WaterDrop, label = alert.rainfall)
                        }
                    }
                }

                // Citizen Action Directive Callout
                if (!alert.actionInstruction.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFF004D40).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Outlined.Security, contentDescription = null, tint = Color(0xFF00695C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = alert.actionInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF004D40),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Expandable Section (Helplines, SMS Sharing, Source verification)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Source details
                        Text(
                            text = "Authoritative Source: ${alert.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!alert.affectedDistricts.isNullOrBlank()) {
                            Text(
                                text = "Impacted Districts: ${alert.affectedDistricts}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Actions (SMS Share + Helpline Dial)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:")
                                            putExtra("sms_body", "🚨 EMERGENCY ALERT: ${alert.title} in ${alert.locationName}. ${alert.actionInstruction ?: ""}")
                                        }
                                        context.startActivity(smsIntent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SMS ALERT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1077"))
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CALL 1077", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val speechText = "${alert.title}. ${alert.bodyKey ?: ""}. ${alert.actionInstruction ?: ""}"
                            onSpeak(speechText)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Outlined.VolumeUp,
                            contentDescription = "Read Aloud",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSpeaking) "STOP AUDIO" else "LISTEN AUDIO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (alert.severity >= 2) {
                        Button(
                            onClick = onNavigateToShelters,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Outlined.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FIND SHELTER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun EmptyAlertsState(isSearch: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearch) Icons.Outlined.SearchOff else Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (isSearch) "No Matching Alerts" else "All Meteorological Parameters Normal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearch) "Try searching for a different district or hazard keyword." else "No active or forecasted emergency alerts in this sector. Doppler radar indicates clear atmospheric conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
