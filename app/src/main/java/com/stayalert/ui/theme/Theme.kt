package com.stayalert.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Tema — DESIGN.md: Material 3 dark, sin modo claro
private val StayAlertDarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = AccentOn,
    primaryContainer = Accent,
    onPrimaryContainer = AccentOn,
    secondary = Accent,
    onSecondary = AccentOn,
    background = SurfaceBase,
    onBackground = InkPrimary,
    surface = SurfaceBase,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = InkSecondary,
    outline = BorderHairline,
    outlineVariant = BorderHairline,
    error = Error,
    onError = SurfaceBase,
    surfaceContainer = SurfaceRaised,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceRaised,
    surfaceContainerLow = SurfaceBase,
    surfaceContainerLowest = SurfaceOverlay
)

@Composable
fun StayAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StayAlertDarkColorScheme,
        typography = StayAlertTypography,
        shapes = StayAlertShapes,
        content = content
    )
}
