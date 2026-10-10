package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ClaudeCoral,
    onPrimary = Color(0xFF2A150D),
    primaryContainer = ClaudeCoralContainerDark,
    onPrimaryContainer = ClaudeCoralLight,
    secondary = ClaudeAmberWheat,
    onSecondary = Color(0xFF291C0E),
    secondaryContainer = Color(0xFF382C1D),
    onSecondaryContainer = Color(0xFFF7DEB8),
    tertiary = ClaudeOliveSage,
    background = ClaudeDarkBackground,
    onBackground = ClaudeDarkTextPrimary,
    surface = ClaudeDarkSurface,
    onSurface = ClaudeDarkTextPrimary,
    surfaceVariant = ClaudeDarkSurfaceElevated,
    onSurfaceVariant = ClaudeDarkTextSecondary,
    outline = ClaudeDarkBorder,
    outlineVariant = ClaudeDarkSurfaceHighlight
)

private val LightColorScheme = lightColorScheme(
    primary = ClaudeCoral,
    onPrimary = Color.White,
    primaryContainer = ClaudeCoralContainerLight,
    onPrimaryContainer = ClaudeCoralDark,
    secondary = ClaudeAmberWheat,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8EDE0),
    onSecondaryContainer = Color(0xFF422E15),
    tertiary = ClaudeOliveSage,
    background = ClaudeLightBackground,
    onBackground = ClaudeLightTextPrimary,
    surface = ClaudeLightSurface,
    onSurface = ClaudeLightTextPrimary,
    surfaceVariant = ClaudeLightSurfaceElevated,
    onSurfaceVariant = ClaudeLightTextSecondary,
    outline = ClaudeLightBorder,
    outlineVariant = ClaudeLightSurfaceHighlight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional Claude branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
