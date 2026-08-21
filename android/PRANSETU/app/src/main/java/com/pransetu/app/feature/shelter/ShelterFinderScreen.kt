package com.pransetu.app.feature.shelter

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pransetu.app.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pransetu.app.core.ui.components.PransetuTopAppBar

data class Shelter(
    val name: String,
    val type: String,
    val district: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int,
    val occupancy: Int,
    val hasMedical: Boolean = false,
    val hasFoodWater: Boolean = true,
    val contactPhone: String = "1077"
)

// Pre-cached Odisha Multi-purpose Cyclone & Emergency Shelters
private val ODISHA_SHELTERS = listOf(
    Shelter("Dhamra Port Cyclone Relief Shelter", "Cyclone Shelter", "Bhadrak", 20.7850, 86.9600, 1200, 340, true),
    Shelter("SCB Medical College & Hospital", "Hospital", "Cuttack", 20.4700, 85.8900, 200, 180, true),
    Shelter("Cuttack Town Hall Relief Center", "Government Building", "Cuttack", 20.4625, 85.8830, 500, 312, true),
    Shelter("Ravenshaw University Safe Campus", "School/College", "Cuttack", 20.4580, 85.8825, 800, 450, false),
    Shelter("Bhubaneswar Transit Evacuation Camp", "Transit Hub", "Khordha", 20.2700, 85.8400, 1000, 623, false),
    Shelter("KIIT University Disaster Shelter", "School/College", "Bhubaneswar", 20.3540, 85.8140, 2000, 890, true),
    Shelter("Puri Multipurpose Cyclone Shelter (Near Sea Beach)", "Cyclone Shelter", "Puri", 19.8050, 85.8180, 1500, 1100, true),
    Shelter("Astaranga Coastal High School Shelter", "Cyclone Shelter", "Puri", 19.9800, 86.2700, 600, 220, true),
    Shelter("Paradip Port Emergency Relief Camp", "Cyclone Shelter", "Jagatsinghpur", 20.3164, 86.6085, 900, 520, true),
    Shelter("Chandipur High School Cyclone Safe Home", "Cyclone Shelter", "Balasore", 21.4680, 87.0170, 750, 290, true),
    Shelter("Balasore District Headquarter Hospital", "Hospital", "Balasore", 21.4934, 86.9337, 300, 210, true),
    Shelter("Berhampur Municipal Disaster Center", "Government Building", "Ganjam", 19.3150, 84.7941, 400, 275, false),
    Shelter("Sambalpur Stadium Flood Shelter", "Open Ground / Shelter", "Sambalpur", 21.4669, 83.9812, 3000, 1200, false),
    Shelter("Rourkela Community Center", "Community Center", "Sundargarh", 22.2604, 84.8536, 600, 340, true),
    Shelter("Kendrapara Kendra School Complex", "School/College", "Kendrapara", 20.5020, 86.4200, 450, 190, false)
)

/**
 * Launches Google Maps navigation or search for a shelter with its exact GPS coordinates.
 */
fun openShelterInGoogleMaps(context: Context, shelter: Shelter) {
    try {
        // Preferred: URI with exact latitude, longitude, and label
        val geoUri = Uri.parse("geo:${shelter.lat},${shelter.lon}?q=${shelter.lat},${shelter.lon}(${Uri.encode(shelter.name)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        try {
            // Fallback 1: Universal geo intent for any map application
            val universalUri = Uri.parse("geo:${shelter.lat},${shelter.lon}?q=${shelter.lat},${shelter.lon}(${Uri.encode(shelter.name)})")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, universalUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            // Fallback 2: Browser Google Maps search
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${shelter.lat},${shelter.lon}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
}

@Composable
fun ShelterFinderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTactical: () -> Unit = {}
) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        try {
            val locationProvider = com.pransetu.app.core.location.LocationProvider(context)
            val loc = locationProvider.getLastKnownLocation()
            userLocation = loc
        } catch (_: Exception) {}
    }

    val filterChips = listOf(
        "ALL" to "All Shelters (${ODISHA_SHELTERS.size})",
        "CYCLONE" to "🌪️ Cyclone Shelters",
        "HOSPITAL" to "🏥 Hospitals",
        "SCHOOL" to "🏫 Schools & Colleges",
        "COMMUNITY" to "🏛️ Relief Camps"
    )

    val filteredShelters = ODISHA_SHELTERS.filter { shelter ->
        when (selectedFilter) {
            "CYCLONE" -> shelter.type.contains("Cyclone", ignoreCase = true)
            "HOSPITAL" -> shelter.type.contains("Hospital", ignoreCase = true)
            "SCHOOL" -> shelter.type.contains("School", ignoreCase = true) || shelter.type.contains("University", ignoreCase = true)
            "COMMUNITY" -> shelter.type.contains("Community", ignoreCase = true) || shelter.type.contains("Government", ignoreCase = true)
            else -> true
        }
    }

    val sortedShelters = if (userLocation != null) {
        filteredShelters.sortedBy { shelter ->
            val results = FloatArray(1)
            Location.distanceBetween(userLocation!!.latitude, userLocation!!.longitude, shelter.lat, shelter.lon, results)
            results[0]
        }
    } else {
        filteredShelters
    }

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = stringResource(R.string.shelters_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Location Proximity Notice
            Surface(
                color = if (userLocation != null) Color(0xFF004D40) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (userLocation != null) Icons.Default.LocationOn else Icons.Default.Map,
                        contentDescription = null,
                        tint = if (userLocation != null) Color(0xFF80CBC4) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (userLocation != null)
                            "📍 Sorted by real-time distance from your GPS location"
                        else
                            "🗺️ Showing all pre-cached Odisha Disaster Shelters (Tap any shelter to open Google Maps)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (userLocation != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterChips) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Shelter Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedShelters) { shelter ->
                    ShelterCard(
                        shelter = shelter,
                        userLocation = userLocation,
                        onOpenMap = { openShelterInGoogleMaps(context, shelter) },
                        onNavigateCompass = onNavigateToTactical
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ShelterCard(
    shelter: Shelter,
    userLocation: Location?,
    onOpenMap: () -> Unit,
    onNavigateCompass: () -> Unit = {}
) {
    val distance = if (userLocation != null) {
        val results = FloatArray(1)
        Location.distanceBetween(userLocation.latitude, userLocation.longitude, shelter.lat, shelter.lon, results)
        results[0]
    } else null

    val distanceText = when {
        distance == null -> "Distance unavailable"
        distance < 1000 -> "${distance.toInt()}m away"
        else -> "%.1f km away".format(distance / 1000f)
    }

    val icon: ImageVector = when {
        shelter.type.contains("Hospital") -> Icons.Default.LocalHospital
        shelter.type.contains("School") || shelter.type.contains("University") -> Icons.Default.School
        shelter.type.contains("Cyclone") -> Icons.Default.Shield
        shelter.type.contains("Community") -> Icons.Default.People
        else -> Icons.Default.Home
    }

    val occupancyPercent = (shelter.occupancy.toFloat() / shelter.capacity * 100).toInt()
    val occupancyColor = when {
        occupancyPercent > 90 -> MaterialTheme.colorScheme.error
        occupancyPercent > 70 -> Color(0xFFFBC02D)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenMap),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shelter.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${shelter.type} • ${shelter.district} District",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "GPS: ${String.format("%.4f", shelter.lat)}° N, ${String.format("%.4f", shelter.lon)}° E",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (distance != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Capacity & Medical amenities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Capacity: ${shelter.occupancy}/${shelter.capacity}",
                        style = MaterialTheme.typography.labelMedium,
                        color = occupancyColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " ($occupancyPercent% full)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (shelter.hasMedical) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.shelters_medical_aid),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Side-by-Side Maps and Offline Compass Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MAPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onNavigateCompass,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OFFLINE HUD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
