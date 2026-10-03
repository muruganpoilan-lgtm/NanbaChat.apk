package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = VoiceVioletLight,
    onPrimary = Color.White,
    primaryContainer = VoiceVioletDark,
    onPrimaryContainer = Color.White,
    secondary = VoiceCyanAccent,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF80EEFF),
    tertiary = VoiceCoralAccent,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color(0xFFF0EDF6),
    surface = DarkSurface,
    onSurface = Color(0xFFF0EDF6),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = DarkSurfaceCard
)

private val LightColorScheme = lightColorScheme(
    primary = VoiceVioletPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADBFF),
    onPrimaryContainer = Color(0xFF23005B),
    secondary = VoiceTealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7F0FF),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = VoiceCoralAccent,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1B1A22),
    surface = LightSurface,
    onSurface = Color(0xFF1B1A22),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = LightSurfaceCard
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep VoiceClub branded theme consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
