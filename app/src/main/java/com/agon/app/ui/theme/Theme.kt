package com.agon.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DeskDarkScheme = darkColorScheme(
    primary = DeskCyan,
    onPrimary = Color(0xFF00333D),
    primaryContainer = Color(0xFF0E3A46),
    onPrimaryContainer = Color(0xFFBFF0FF),
    secondary = DeskViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2B235E),
    onSecondaryContainer = Color(0xFFE2D9FF),
    tertiary = DeskMint,
    onTertiary = Color(0xFF00332B),
    tertiaryContainer = Color(0xFF0B3B33),
    onTertiaryContainer = Color(0xFFB8FFF1),
    error = DeskRose,
    background = DeskAbyss,
    onBackground = Color(0xFFEAF2FF),
    surface = Color(0xFF0D1530),
    onSurface = Color(0xFFEAF2FF),
    surfaceVariant = Color(0xFF182244),
    onSurfaceVariant = Color(0xFFB9C4DE),
    surfaceContainer = Color(0xFF111B3B),
    surfaceContainerHigh = DeskNavyLight,
    surfaceContainerHighest = Color(0xFF1D2A6B),
    outline = Color(0xFF3A4A7E),
    outlineVariant = Color(0xFF26335C),
)

private val DeskLightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFF0FF),
    onPrimaryContainer = Color(0xFF00333D),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2D9FF),
    onSecondaryContainer = Color(0xFF241A6B),
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB8FFF1),
    onTertiaryContainer = Color(0xFF00332B),
    error = Color(0xFFB4234E),
    background = LightBackground,
    onBackground = Color(0xFF0B1437),
    surface = LightSurface,
    onSurface = Color(0xFF0B1437),
    surfaceVariant = Color(0xFFE3EAF6),
    onSurfaceVariant = Color(0xFF3D4A6E),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEAF0FA),
    surfaceContainerHighest = Color(0xFFDFE7F7),
    outline = Color(0xFF9AA7C7),
    outlineVariant = Color(0xFFC6D0E8),
)

@Composable
fun AgonAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DeskDarkScheme
        else -> DeskLightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
