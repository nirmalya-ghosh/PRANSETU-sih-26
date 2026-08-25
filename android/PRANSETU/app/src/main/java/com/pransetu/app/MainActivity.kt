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
import com.pransetu.app.feature.auth.AuthScreen
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
                            },
                            onNavigateToAuth = {
                                rootNavController.navigate("auth")
                            }
                        )
                    }
                    composable("auth") {
                        val authViewModel: com.pransetu.app.feature.auth.AuthViewModel by viewModels {
                            AppViewModelFactory(app)
                        }
                        AuthScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                rootNavController.navigate("onboarding") {
                                    popUpTo("auth") { inclusive = true }
                                }
                                // Signal auth complete back to onboarding
                                onboardingViewModel.handleIntent(
                                    com.pransetu.app.feature.onboarding.OnboardingIntent.AuthComplete
                                )
                            },
                            onBack = { rootNavController.popBackStack() }
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
                                    try { 
                                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                        val googleClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this@MainActivity, gso)
                                        googleClient.signOut()
                                        googleClient.revokeAccess()
                                    } catch (_: Exception) {}
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
            }
        }
    }
}