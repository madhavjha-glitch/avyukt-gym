package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val GymObsidian = Color(0xFF0D0D0D)
val GymDarkSurface = Color(0xFF161616)
val GymCardBg = Color(0xFF1E1E1E)
val GymElevated = Color(0xFF252525)
val GymOrange = Color(0xFFFF6B00)
val GymOrangeLight = Color(0xFFFF8C33)
val GymAmber = Color(0xFFFFAA00)
val GymGold = Color(0xFFFFD700)
val GymRed = Color(0xFFE63946)
val GymGreen = Color(0xFF25D366)
val GymGreenDark = Color(0xFF1DA851)
val GymBlue = Color(0xFF4DABF7)
val GymPurple = Color(0xFFBE4BDB)
val GymTextPrimary = Color(0xFFF5F5F5)
val GymTextMuted = Color(0xFFAAAAAA)
val GymTextSubtle = Color(0xFF888888)
val GymBorder = Color(0x14FFFFFF)

val AvatarPalette = listOf(
    GymOrange,
    GymGreen,
    GymBlue,
    GymGold,
    GymRed,
    GymPurple
)

val GymDarkColorScheme = darkColorScheme(
    primary = GymOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1A00),
    onPrimaryContainer = GymOrangeLight,
    secondary = GymGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D2E1A),
    onSecondaryContainer = GymGreen,
    tertiary = GymBlue,
    onTertiary = Color.White,
    background = GymObsidian,
    onBackground = GymTextPrimary,
    surface = GymDarkSurface,
    onSurface = GymTextPrimary,
    surfaceVariant = GymCardBg,
    onSurfaceVariant = GymTextMuted,
    surfaceContainerHigh = GymElevated,
    outline = GymBorder,
    error = GymRed,
    onError = Color.White
)
