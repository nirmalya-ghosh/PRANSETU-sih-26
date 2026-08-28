package com.pransetu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pransetu.app.feature.onboarding.OnboardingScreen
import com.pransetu.app.feature.onboarding.OnboardingViewModel
import com.pransetu.app.feature.home.HomeViewModel
import com.pransetu.app.feature.settings.SettingsViewModel
import com.pransetu.app.ui.theme.PRANSETUTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app: PransetuApplication
        get() = applicationContext as PransetuApplication

    private val homeViewModel: HomeViewModel by viewModels {
        AppViewModelFactory(app)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        AppViewModelFactory(app)
    }

    private val onboardingViewModel: OnboardingViewModel by viewModels {
        AppViewModelFactory(app)
    }

    private val sosStatusViewModel: com.pransetu.app.feature.home.SosStatusViewModel by viewModels {
        AppViewModelFactory(app)
    }

    private val alertsViewModel: com.pransetu.app.feature.alerts.AlertsViewModel by viewModels {
        AppViewModelFactory(app)
    }

    private val emergencyContactsViewModel: com.pransetu.app.feature.contacts.EmergencyContactsViewModel by viewModels {
        AppViewModelFactory(app)
    }

    private val sosHistoryViewModel: com.pransetu.app.feature.history.SosHistoryViewModel by viewModels {
        AppViewModelFactory(app)
    }

    private val familyCircleViewModel: com.pransetu.app.feature.family.FamilyCircleViewModel by viewModels {
        AppViewModelFactory(app)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureSystemPermissions()
        
        // Compute start destination ONCE at launch to avoid NavHost recomposition crash
        // If they skipped Auth and set up manual profile, isComplete will be true.
        val isComplete = kotlinx.coroutines.runBlocking { app.userProfileStore.isOnboardingCompleteSync() }
        val startDest = if (isComplete) "main" else "onboarding"
        
        setContent {
            // Track emergency mode: if any active SOS exists, switch to emergency theme
            val sosState by sosStatusViewModel.uiState.collectAsState()
            val isEmergency = sosState.activeSos != null
            
            // Dynamic Language & Locale Propagation
            val settingsState by settingsViewModel.uiState.collectAsState()
            val selectedLang = settingsState.currentLanguage
            val baseActivity = this@MainActivity
            val currentConfig = androidx.compose.ui.platform.LocalConfiguration.current

            val localizedConfig = remember(selectedLang, currentConfig) {
                val locale = java.util.Locale.forLanguageTag(selectedLang)
                java.util.Locale.setDefault(locale)
                
                val config = android.content.res.Configuration(currentConfig).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                
                @Suppress("DEPRECATION")
                baseActivity.resources.updateConfiguration(config, baseActivity.resources.displayMetrics)
                
                config
            }

            val localizedContext = remember(selectedLang) {
                baseActivity.createConfigurationContext(localizedConfig)
            }

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.activity.compose.LocalActivityResultRegistryOwner provides baseActivity,
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedConfig,
                com.pransetu.app.core.localization.LocalAppLanguage provides selectedLang
            ) {
                PRANSETUTheme(isEmergencyMode = isEmergency) {
                    val rootNavController = rememberNavController()
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                    
                    val systemAlertViewModel: com.pransetu.app.feature.alert.AlertViewModel by viewModels {
                        AppViewModelFactory(app)
                    }
                    val currentAlert by systemAlertViewModel.currentAlert.collectAsState()

                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = rootNavController,
                            startDestination = startDest
                        ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            viewModel = onboardingViewModel,
                            onFinishOnboarding = {
                                rootNavController.navigate("main") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main") {
                        MainAppScreen(
                            homeViewModel = homeViewModel,
                            settingsViewModel = settingsViewModel,
                            sosStatusViewModel = sosStatusViewModel,
                            alertsViewModel = alertsViewModel,
                            emergencyContactsViewModel = emergencyContactsViewModel,
                            sosHistoryViewModel = sosHistoryViewModel,
                            familyCircleViewModel = familyCircleViewModel,
                            onLogout = {
                                coroutineScope.launch {
                                    try { app.authRepository.signOut() } catch (_: Exception) {}
                                    try { app.userProfileStore.clearUserProfile() } catch (_: Exception) {}
                                    onboardingViewModel.resetOnboarding()
                                    rootNavController.navigate("onboarding") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }
                
                currentAlert?.let { alert ->
                    com.pransetu.app.feature.alert.SystemAlertDialog(
                        alert = alert,
                        onDismiss = { systemAlertViewModel.dismissAlert() }
                    )
                }
                    }
                }
            }
        }
    }

    private fun ensureSystemPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1010)
        }

        // Request exemption from battery optimizations so background services & sirens are never killed by OEM Doze
        try {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (_: Exception) {}
    }
}