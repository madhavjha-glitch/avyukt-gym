package com.example.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val GymPaper = Color(0xFFF4F3EF)
val GymSurface = Color(0xFFFFFFFF)
val GymElevated = Color(0xFFECEAE3)
val GymInk = Color(0xFF111111)
val GymInkSecondary = Color(0xFF4A4944)
val GymInkMuted = Color(0xFF75746D)
val GymLine = Color(0xFFDEDCD5)

val GymAccent = Color(0xFFE85D04)
val GymAccentSoft = Color(0x1FE85D04)
val GymGreen = Color(0xFF1E9E55)
val GymRed = Color(0xFFC0392B)
val GymBlue = Color(0xFF1D4ED8)
val GymAmber = Color(0xFFD97706)
val GymPurple = Color(0xFF6D28D9)

val AvatarPalette = listOf(
    GymAccent,
    GymGreen,
    GymBlue,
    GymAmber,
    GymRed,
    GymPurple
)

val GymLightColorScheme = lightColorScheme(
    primary = GymInk,
    onPrimary = GymPaper,
    primaryContainer = GymAccentSoft,
    onPrimaryContainer = GymAccent,
    secondary = GymAccent,
    onSecondary = Color.White,
    secondaryContainer = GymElevated,
    onSecondaryContainer = GymInk,
    tertiary = GymBlue,
    onTertiary = Color.White,
    background = GymPaper,
    onBackground = GymInk,
    surface = GymSurface,
    onSurface = GymInk,
    surfaceVariant = GymElevated,
    onSurfaceVariant = GymInkSecondary,
    surfaceContainerLowest = GymSurface,
    surfaceContainerLow = GymPaper,
    surfaceContainer = GymSurface,
    surfaceContainerHigh = GymElevated,
    surfaceContainerHighest = GymLine,
    outline = GymLine,
    outlineVariant = GymLine,
    error = GymRed,
    onError = Color.White
)
