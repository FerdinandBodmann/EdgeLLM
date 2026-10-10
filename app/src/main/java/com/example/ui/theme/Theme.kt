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
    primary = ElectricCyan,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF6EFFF0),
    secondary = SoftBlue,
    onSecondary = Color(0xFF00315D),
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFD2E4FF),
    tertiary = VioletAccent,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkSurfaceHighlight
)

private val LightColorScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F3E8),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4FF),
    onSecondaryContainer = Color(0xFF001A40),
    tertiary = VioletAccent,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightSurfaceHighlight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional brand colors
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
