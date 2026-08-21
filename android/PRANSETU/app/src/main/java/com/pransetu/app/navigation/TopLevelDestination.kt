package com.pransetu.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.pransetu.app.R

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextResId: Int,
    @StringRes val titleTextResId: Int,
    val route: String,
    val showBadge: Boolean = false
) {
    HOME(
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        iconTextResId = R.string.nav_home,
        titleTextResId = R.string.nav_home,
        route = "home"
    ),
    ALERTS(
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        iconTextResId = R.string.nav_alerts,
        titleTextResId = R.string.nav_alerts,
        route = "alerts",
        showBadge = true
    ),
    TACTICAL(
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        iconTextResId = R.string.nav_tactical,
        titleTextResId = R.string.nav_tactical,
        route = "tactical_terminal"
    ),
    ME(
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        iconTextResId = R.string.nav_more,
        titleTextResId = R.string.nav_more,
        route = "me"
    )
}

