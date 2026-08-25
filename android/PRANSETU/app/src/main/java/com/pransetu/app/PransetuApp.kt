package com.pransetu.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pransetu.app.feature.alerts.AlertsScreen
import com.pransetu.app.feature.alerts.AlertsViewModel
import com.pransetu.app.feature.contacts.EmergencyContactsScreen
import com.pransetu.app.feature.contacts.EmergencyContactsViewModel
import com.pransetu.app.feature.family.FamilyCircleScreen
import com.pransetu.app.feature.family.FamilyCircleViewModel
import com.pransetu.app.feature.firstaid.FirstAidScreen
import com.pransetu.app.feature.history.SosHistoryScreen
import com.pransetu.app.feature.history.SosHistoryViewModel
import com.pransetu.app.feature.home.HomeScreen
import com.pransetu.app.feature.home.HomeViewModel
import com.pransetu.app.feature.home.SosStatusScreen
import com.pransetu.app.feature.home.SosStatusViewModel
import com.pransetu.app.feature.profile.ProfileScreen
import com.pransetu.app.feature.safety.SafetyDetailScreen
import com.pransetu.app.feature.safety.SafetyScreen
import com.pransetu.app.feature.settings.SettingsScreen
import com.pransetu.app.feature.settings.SettingsViewModel
import com.pransetu.app.feature.shelter.ShelterFinderScreen
import com.pransetu.app.feature.tactical.TacticalTerminalScreen
import com.pransetu.app.navigation.TopLevelDestination

@Composable
fun MainAppScreen(
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    sosStatusViewModel: SosStatusViewModel,
    alertsViewModel: AlertsViewModel,
    emergencyContactsViewModel: EmergencyContactsViewModel,
    sosHistoryViewModel: SosHistoryViewModel,
    familyCircleViewModel: FamilyCircleViewModel,
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            PransetuBottomBar(navController = navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSettings = { navController.navigate("shelter_finder") },
                    onNavigateToSosStatus = { navController.navigate("sos_status") },
                    onNavigateToSafety = { navController.navigate("safety") },
                    onNavigateToContacts = { navController.navigate("emergency_contacts") },
                    onNavigateToFamilyCircle = { navController.navigate("family_circle") },
                    onNavigateToFirstAid = { navController.navigate("first_aid_guide") },
                    onNavigateToTactical = { navController.navigate("tactical_terminal") },
                    onLogout = onLogout
                )
            }
            composable("sos_status") {
                SosStatusScreen(
                    viewModel = sosStatusViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(TopLevelDestination.ALERTS.route) {
                AlertsScreen(
                    viewModel = alertsViewModel,
                    onNavigateToShelters = { navController.navigate("shelter_finder") }
                )
            }
            composable("safety") {
                SafetyScreen(
                    onNavigateToDetail = { categoryKey ->
                        if (categoryKey == "first_aid") {
                            navController.navigate("first_aid_guide")
                        } else {
                            navController.navigate("safety_detail/$categoryKey")
                        }
                    }
                )
            }
            composable(
                route = "safety_detail/{categoryKey}",
                arguments = listOf(androidx.navigation.navArgument("categoryKey") {
                    type = androidx.navigation.NavType.StringType
                })
            ) { backStackEntry ->
                val categoryKey = backStackEntry.arguments?.getString("categoryKey") ?: "before"
                SafetyDetailScreen(
                    categoryKey = categoryKey,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(TopLevelDestination.ME.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToEmergencyContacts = { navController.navigate("emergency_contacts") },
                    onNavigateToShelters = { navController.navigate("shelter_finder") },
                    onNavigateToSosHistory = { navController.navigate("sos_history") },
                    onNavigateToFamilyCircle = { navController.navigate("family_circle") },
                    onNavigateToFirstAid = { navController.navigate("first_aid_guide") },
                    onNavigateToSafety = { navController.navigate("safety") },
                    onNavigateToTactical = { navController.navigate("tactical_terminal") },
                    onLogout = onLogout
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToEmergencyContacts = { navController.navigate("emergency_contacts") },
                    onNavigateToShelters = { navController.navigate("shelter_finder") },
                    onNavigateToSosHistory = { navController.navigate("sos_history") },
                    onNavigateToFamilyCircle = { navController.navigate("family_circle") },
                    onNavigateToFirstAid = { navController.navigate("first_aid_guide") },
                    onNavigateToSafety = { navController.navigate("safety") },
                    onNavigateToTactical = { navController.navigate("tactical_terminal") },
                    onLogout = onLogout
                )
            }
            composable("emergency_contacts") {
                EmergencyContactsScreen(
                    viewModel = emergencyContactsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = onLogout
                )
            }
            composable("shelter_finder") {
                ShelterFinderScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTactical = { navController.navigate("tactical_terminal") }
                )
            }
            composable("sos_history") {
                SosHistoryScreen(
                    viewModel = sosHistoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("family_circle") {
                FamilyCircleScreen(
                    viewModel = familyCircleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("first_aid_guide") {
                FirstAidScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("tactical_terminal") {
                TacticalTerminalScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTriggerSos = { reason ->
                        homeViewModel.handleIntent(com.pransetu.app.feature.home.HomeIntent.OnSosClicked(message = reason))
                        navController.navigate("sos_status")
                    }
                )
            }
        }
    }
}

@Composable
private fun PransetuBottomBar(navController: NavHostController) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.height(70.dp)
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            TopLevelDestination.values().forEach { destination ->
                val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        if (destination.showBadge) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.onError, CircleShape)
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = stringResource(destination.iconTextResId),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = stringResource(destination.iconTextResId),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(destination.titleTextResId),
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}
