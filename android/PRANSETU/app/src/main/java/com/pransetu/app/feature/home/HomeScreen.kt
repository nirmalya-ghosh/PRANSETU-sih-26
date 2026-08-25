package com.pransetu.app.feature.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.pransetu.app.core.ui.components.AppHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.core.ai.VoiceDistressParser
import com.pransetu.app.core.hardware.EmergencyBeaconManager
import com.pransetu.app.core.network.NetworkStatus
import com.pransetu.app.core.sensor.BarometerHazardDetector
import com.pransetu.app.core.sensor.ManDownDetector
import com.pransetu.app.core.sensor.ManDownState
import com.pransetu.app.core.sensor.ShakeDetector
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSosStatus: () -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToFamilyCircle: () -> Unit = {},
    onNavigateToFirstAid: () -> Unit = {},
    onNavigateToTactical: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Background Sensors & Hardware Daemons (Quiet Lifeline)
    val beaconManager = remember { EmergencyBeaconManager(context) }
    val manDownDetector = remember {
        ManDownDetector(context) { reason, _ ->
            viewModel.handleIntent(HomeIntent.OnSosClicked(message = reason))
            onNavigateToSosStatus()
        }
    }
    val manDownTelemetry by manDownDetector.telemetry.collectAsStateWithLifecycle()
    val barometerDetector = remember { BarometerHazardDetector(context) }

    var shakeTriggered by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val shakeDetector = ShakeDetector(
            context = context,
            requiredShakeCount = 3,
            onShakeDetected = { shakeTriggered = true }
        )
        shakeDetector.start()
        barometerDetector.start()
        manDownDetector.start()
        onDispose {
            shakeDetector.stop()
            barometerDetector.stop()
            manDownDetector.stop()
            beaconManager.stopBeacon()
        }
    }

    // Permission Launcher for Mesh & Sensors
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        viewModel.handleIntent(HomeIntent.ToggleMesh(allGranted))
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    // Voice Emergency State & Dialog
    var transcribedText by remember { mutableStateOf<String?>(null) }
    var showVoiceConfirmDialog by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = data?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                transcribedText = spoken
                showVoiceConfirmDialog = true
            }
        }
    }

    // SOS Instant Trigger Function (Zero Delay, 100% Reliable)
    val triggerSosInstant: (String?) -> Unit = { reason ->
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Activity.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Activity.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(200L, 255)
                )
            }
        } catch (_: Exception) {}

        viewModel.handleIntent(HomeIntent.OnSosClicked(reason))
        onNavigateToSosStatus()
    }

    LaunchedEffect(shakeTriggered) {
        if (shakeTriggered) {
            shakeTriggered = false
            triggerSosInstant("Motion Shake SOS")
        }
    }

    // Breathing pulse animations for the Tactical SOS Halo
    val infiniteTransition = rememberInfiniteTransition(label = "SosHalo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "HaloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "HaloAlpha"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // ==========================================
                // ZONE A: HEADER
                // ==========================================
                val isConnected = uiState.networkStatus == NetworkStatus.Available
                val isMeshOn = uiState.isMeshEnabled && uiState.peerCount > 0
                val statusText = when {
                    isConnected -> "Online"
                    isMeshOn -> "Mesh Active"
                    else -> "Offline"
                }
                
                AppHeader(
                    connectionStatus = statusText,
                    isMeshOnly = isMeshOn,
                    language = when (uiState.selectedLanguage) {
                        "or" -> "ଓଡ଼ିଆ"
                        "hi" -> "हिन्दी"
                        else -> "English"
                    },
                    onLanguageClick = {
                        val nextLang = when (uiState.selectedLanguage) {
                            "en" -> "or"
                            "or" -> "hi"
                            else -> "en"
                        }
                        viewModel.handleIntent(HomeIntent.SetLanguage(nextLang))
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // ==========================================
                // ZONE B: TACTICAL INSTANT SOS BUTTON
                // ==========================================
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Main Tactical SOS Button (Professional size)
                    Surface(
                        onClick = { triggerSosInstant(null) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "SOS",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (uiState.selectedLanguage) {
                                        "or" -> "ସାହାଯ୍ୟ ପାଇଁ ଦବାନ୍ତୁ"
                                        "hi" -> "मदद के लिए दबाएं"
                                        else -> "TAP TO BROADCAST"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ==========================================
                // ZONE C: THREE LARGE HIGH-CONTRAST ACTION CARDS
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Voice Emergency (Speak for help)
                    SeniorActionCard(
                        icon = Icons.Default.Mic,
                        title = when (uiState.selectedLanguage) {
                            "or" -> "କହି ସାହାଯ୍ୟ ମାଗନ୍ତୁ"
                            "hi" -> "बोलकर मदद मांगें"
                            else -> "Speak Your Emergency"
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    when (uiState.selectedLanguage) {
                                        "or" -> "ଆପଣଙ୍କ ସମସ୍ୟା କୁହନ୍ତୁ..."
                                        "hi" -> "अपनी समस्या बताएं..."
                                        else -> "Speak your emergency clearly..."
                                    }
                                )
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (_: Exception) {
                                viewModel.handleIntent(
                                    HomeIntent.OnSosClicked("Voice SOS Triggered")
                                )
                                onNavigateToSosStatus()
                            }
                        }
                    )

                    // Card 2: Family & Caregivers (Direct Call)
                    SeniorActionCard(
                        icon = Icons.Default.FamilyRestroom,
                        title = when (uiState.selectedLanguage) {
                            "or" -> "ପରିବାର ଏବଂ ସମ୍ପର୍କୀୟ"
                            "hi" -> "परिवार और देखभालकर्ता"
                            else -> "Family & Caregivers"
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToFamilyCircle
                    )

                    // Card 3: Find Shelters Near Me
                    SeniorActionCard(
                        icon = Icons.Default.LocationOn,
                        title = when (uiState.selectedLanguage) {
                            "or" -> "ନିକଟସ୍ଥ ଆଶ୍ରୟସ୍ଥଳ ଖୋଜନ୍ତୁ"
                            "hi" -> "निकटतम सुरक्षित आश्रय"
                            else -> "Find Shelters Near Me"
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigateToSettings() } // Routes to Shelter Finder or Tools
                    )
                }
            }
        }

        // ==========================================
        // VOICE SOS CONFIRMATION DIALOG (ZERO JARGON)
        // ==========================================
        if (showVoiceConfirmDialog && transcribedText != null) {
            AlertDialog(
                onDismissRequest = { showVoiceConfirmDialog = false },
                title = {
                    Text(
                        text = when (uiState.selectedLanguage) {
                            "or" -> "ଆପଣ ଏହା କହିଛନ୍ତି କି?"
                            "hi" -> "क्या आपने यह कहा?"
                            else -> "Did we hear you correctly?"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\"$transcribedText\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (uiState.selectedLanguage) {
                                "or" -> "ଏହି ସୂଚନା ତୁରନ୍ତ ଉଦ୍ଧାରକାରୀ ଦଳକୁ ପଠାଯିବ।"
                                "hi" -> "यह जानकारी तुरंत बचाव दल को भेजी जाएगी।"
                                else -> "This emergency description will be sent to rescue teams with your location."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showVoiceConfirmDialog = false
                            val parsed = VoiceDistressParser.parse(transcribedText!!)
                            viewModel.handleIntent(
                                HomeIntent.OnSosClicked(message = parsed.structuredSummary)
                            )
                            onNavigateToSosStatus()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = when (uiState.selectedLanguage) {
                                "or" -> "ହଁ, ଏବେ ସାହାଯ୍ୟ ପଠାନ୍ତୁ"
                                "hi" -> "हां, तुरंत मदद भेजें"
                                else -> "YES, SEND SOS NOW"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showVoiceConfirmDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = when (uiState.selectedLanguage) {
                                "or" -> "ପୁଣି ଚେଷ୍ଟା କରନ୍ତୁ / ବାତିଲ"
                                "hi" -> "पुनः प्रयास करें / रद्द"
                                else -> "Try Again / Cancel"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        // ==========================================
        // AUTONOMOUS MAN-DOWN ALARM MODAL
        // ==========================================
        if (manDownTelemetry.state == ManDownState.COUNTDOWN_ACTIVE) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FALL / IMPACT DETECTED",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F),
                            fontSize = 20.sp
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "A sudden fall or impact was detected. Sending automatic rescue signal in:",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${manDownTelemetry.countdownSeconds}",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD32F2F)
                        )
                        Text(
                            text = "Seconds remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { manDownDetector.cancelCountdown() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "I AM SAFE (CANCEL)",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SeniorActionCard(
    icon: ImageVector,
    title: String,
    containerColor: Color,
    contentColor: Color = Color.White,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}
