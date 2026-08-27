package com.pransetu.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ProfessionalBlue,
    onPrimary = White,
    secondary = ProfessionalBlue,
    onSecondary = White,
    error = EmergencyRed,
    onError = White,
    background = DarkBackground,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SecondaryText,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ProfessionalBlue,
    onPrimary = White,
    secondary = ProfessionalBlue,
    onSecondary = White,
    error = EmergencyRed,
    onError = White,
    background = LightBackground,
    onBackground = DarkNavy,
    surface = LightSurface,
    onSurface = DarkNavy,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = SecondaryText,
    outline = DarkBorder
)

@Composable
fun PRANSETUTheme(
    darkTheme: Boolean = true, // Forced dark theme
    isEmergencyMode: Boolean = false, // Kept for compatibility but we no longer tint the whole app red
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}