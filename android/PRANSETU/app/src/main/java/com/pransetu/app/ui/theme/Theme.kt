package com.pransetu.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PransetuBlueLight,
    onPrimary = Color(0xFF003355),
    secondary = PransetuTealLight,
    onSecondary = Color(0xFF003333),
    error = EmergencyRedLight,
    onError = Color(0xFF410002),
    background = DarkBackground,
    onBackground = Color(0xFFE3E3E3),
    surface = DarkSurface,
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C7C5)
)

private val LightColorScheme = lightColorScheme(
    primary = PransetuBlue,
    onPrimary = Color.White,
    secondary = PransetuTeal,
    onSecondary = Color.White,
    error = EmergencyRed,
    onError = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = LightSurface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF42474E)
)

/** High-contrast emergency theme when SOS is active */
private val EmergencyColorScheme = darkColorScheme(
    primary = Color(0xFFFF1744),
    onPrimary = Color.White,
    secondary = Color(0xFFFF6E40),
    onSecondary = Color.White,
    error = Color(0xFFFF1744),
    onError = Color.White,
    background = Color(0xFF1A0000),
    onBackground = Color(0xFFFFCDD2),
    surface = Color(0xFF2D0000),
    onSurface = Color(0xFFFFCDD2),
    surfaceVariant = Color(0xFF4A0000),
    onSurfaceVariant = Color(0xFFFFAB91)
)

@Composable
fun PRANSETUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isEmergencyMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isEmergencyMode -> EmergencyColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}