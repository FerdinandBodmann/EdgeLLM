package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ClaudeCoral,
    onPrimary = Color(0xFF1F0E08),
    primaryContainer = ClaudeCoralContainerDark,
    onPrimaryContainer = ClaudeCoralLight,
    secondary = ClaudeAmberWheat,
    onSecondary = Color(0xFF261807),
    secondaryContainer = Color(0xFF382918),
    onSecondaryContainer = Color(0xFFFBE4C8),
    tertiary = ClaudeOliveSage,
    onTertiary = Color(0xFF142413),
    tertiaryContainer = Color(0xFF2B3A2A),
    onTertiaryContainer = Color(0xFFD6E8D5),
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
    secondaryContainer = Color(0xFFFAF0E4),
    onSecondaryContainer = Color(0xFF452E12),
    tertiary = ClaudeOliveSage,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5EEE4),
    onTertiaryContainer = Color(0xFF20331F),
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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
