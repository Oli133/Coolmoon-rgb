package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CoolMoonDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianCanvas,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = Ultraviolet,
    onSecondary = Color.White,
    secondaryContainer = UltravioletContainer,
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = PlasmaCoral,
    onTertiary = Color.White,
    tertiaryContainer = PlasmaCoralContainer,
    onTertiaryContainer = Color(0xFFFFD6E4),
    background = ObsidianCanvas,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = ErrorCrimson,
    onError = Color.White
)

private val CoolMoonLightColorScheme = lightColorScheme(
    primary = DaylightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F4F8),
    onPrimaryContainer = Color(0xFF00363D),
    secondary = DaylightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFDBFF),
    onSecondaryContainer = Color(0xFF31005E),
    tertiary = DaylightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF4A001E),
    background = DaylightCanvas,
    onBackground = DaylightTextPrimary,
    surface = DaylightSurface,
    onSurface = DaylightTextPrimary,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = DaylightTextSecondary,
    outline = DaylightOutline,
    outlineVariant = Color(0xFFE0E6F2),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

val CoolMoonShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CoolMoonTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CoolMoonDarkColorScheme else CoolMoonLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoolMoonTypography,
        shapes = CoolMoonShapes,
        content = content
    )
}
