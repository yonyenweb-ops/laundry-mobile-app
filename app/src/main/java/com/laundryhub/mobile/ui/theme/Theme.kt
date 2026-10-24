package com.laundryhub.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF2563EB)
val Ink = Color(0xFF172033)
val Muted = Color(0xFF667085)
val Line = Color(0xFFE4E7EC)
val Background = Color(0xFFF4F6FA)
val Green = Color(0xFF15803D)
val GreenSoft = Color(0xFFE8F6EE)
val Red = Color(0xFFB42318)
val RedSoft = Color(0xFFFDECEA)
val Amber = Color(0xFFB54708)
val AmberSoft = Color(0xFFFEF4E6)
val BlueSoft = Color(0xFFEEF4FF)

private val colors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueSoft,
    onPrimaryContainer = Blue,
    secondary = Ink,
    // Selected tab and filter chips use these: soft blue instead of Material's default purple
    secondaryContainer = BlueSoft,
    onSecondaryContainer = Blue,
    // Bottom tab bar, dialogs and menus stay white
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Background,
    surfaceTint = Color.White,
    background = Background,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Background,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Red,
)

@Composable
fun LaundryHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
