package com.pransetu.app.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.R
import com.pransetu.app.core.localization.LanguageManager
import com.pransetu.app.core.ui.components.PransetuTopAppBar

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToEmergencyContacts: () -> Unit,
    onNavigateToShelters: () -> Unit = {},
    onNavigateToSosHistory: () -> Unit = {},
    onNavigateToFamilyCircle: () -> Unit = {},
    onNavigateToFirstAid: () -> Unit = {},
    onNavigateToSafety: () -> Unit = {},
    onNavigateToTactical: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentLangOption = LanguageManager.supportedLanguages.find {
        it.code.equals(uiState.currentLanguage, ignoreCase = true)
    } ?: LanguageManager.supportedLanguages.first()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PransetuTopAppBar(
                title = stringResource(id = R.string.nav_more)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ==========================================
            // PROFILE CARD (72DP HIGH-LEGIBILITY)
            // ==========================================
            Card(
                onClick = onNavigateToProfile,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.currentLanguage == "or") "ମୋର ପ୍ରୋଫାଇଲ୍" else if (uiState.currentLanguage == "hi") "मेरी प्रोफ़ाइल" else "My Citizen Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.currentLanguage == "or") "ଡାକ୍ତରୀ ସୂଚନା ଏବଂ ପରିଚୟ ସମ୍ପାଦନ କରନ୍ତୁ" else if (uiState.currentLanguage == "hi") "चिकित्सा जानकारी और पहचान विवरण" else "Medical Info & Emergency ID",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // GROUP 1: MY SAFETY & CAREGIVERS
            // ==========================================
            SeniorSectionHeader(title = "My Emergency Circle")
            SeniorMenuCard {
                SeniorMenuItem(
                    icon = Icons.Default.Contacts,
                    title = "Emergency Contacts",
                    subtitle = "Police, Fire, Ambulance & Helplines",
                    iconColor = Color(0xFFD32F2F),
                    onClick = onNavigateToEmergencyContacts
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SeniorMenuItem(
                    icon = Icons.Default.FamilyRestroom,
                    title = "Family Circle",
                    subtitle = "1-tap GPS status & speed dial",
                    iconColor = Color(0xFF00796B),
                    onClick = onNavigateToFamilyCircle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SeniorMenuItem(
                    icon = Icons.Default.History,
                    title = "SOS Dispatch History",
                    subtitle = "Audit logs and delivery receipts",
                    iconColor = Color(0xFF1565C0),
                    onClick = onNavigateToSosHistory
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // GROUP 2: DISASTER PREPAREDNESS & MEDICAL
            // ==========================================
            SeniorSectionHeader(title = "Disaster Preparedness")
            SeniorMenuCard {
                SeniorMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Find Nearest Cyclone Shelters",
                    subtitle = "Verified shelter locations & maps",
                    iconColor = Color(0xFFE65100),
                    onClick = onNavigateToShelters
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SeniorMenuItem(
                    icon = Icons.Default.MedicalServices,
                    title = "First Aid & CPR Guide",
                    subtitle = "Offline emergency medical care steps",
                    iconColor = Color(0xFFC2185B),
                    onClick = onNavigateToFirstAid
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SeniorMenuItem(
                    icon = Icons.Default.Security,
                    title = "Disaster Safety Protocols",
                    subtitle = "What to do before, during & after disaster",
                    iconColor = Color(0xFF00897B),
                    onClick = onNavigateToSafety
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // GROUP 3: ADVANCED & TACTICAL SENSORS
            // ==========================================
            SeniorSectionHeader(title = "Advanced & Sensors")
            SeniorMenuCard {
                SeniorMenuItem(
                    icon = Icons.Default.CellTower,
                    title = "Tactical Mesh Radar & Beacon",
                    subtitle = "Zero-cellular relay, barometer & SOS beacon",
                    iconColor = Color(0xFF0284C7),
                    onClick = onNavigateToTactical
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // GROUP 4: LANGUAGE & LOGOUT
            // ==========================================
            SeniorSectionHeader(title = "Preferences & Account")
            SeniorMenuCard {
                // Language Selector Item
                Box {
                    SeniorMenuItem(
                        icon = Icons.Default.Language,
                        title = "Language / ଭାଷା / भाषा",
                        subtitle = "Current: ${currentLangOption.englishName} (${currentLangOption.nativeName})",
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { languageDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false }
                    ) {
                        LanguageManager.supportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${lang.englishName} (${lang.nativeName})",
                                            fontSize = 16.sp,
                                            fontWeight = if (lang.code.equals(uiState.currentLanguage, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (lang.code.equals(uiState.currentLanguage, ignoreCase = true)) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.handleIntent(SettingsIntent.OnLanguageSelected(lang.code))
                                    languageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Log Out Item
                SeniorMenuItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Sign Out of PRANSETU",
                    subtitle = "Disconnect account on this device",
                    iconColor = Color(0xFFD32F2F),
                    onClick = { showLogoutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Sign Out of PRANSETU",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to sign out? Your offline emergency data and mesh credentials will be locked until you sign back in.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun SeniorSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SeniorMenuCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SeniorMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}
