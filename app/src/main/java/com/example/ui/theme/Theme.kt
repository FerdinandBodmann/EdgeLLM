package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeminiDarkColorScheme = darkColorScheme(
    primary = GeminiWhite,
    onPrimary = GeminiDarkBackground,
    primaryContainer = GeminiDarkSurfaceElevated,
    onPrimaryContainer = GeminiTextPrimary,
    secondary = GeminiBlue,
    onSecondary = GeminiDarkBackground,
    secondaryContainer = GeminiDarkSurfaceHighlight,
    onSecondaryContainer = GeminiLightBlue,
    tertiary = GeminiPurple,
    onTertiary = GeminiDarkBackground,
    background = GeminiDarkBackground,
    onBackground = GeminiTextPrimary,
    surface = GeminiDarkBackground,
    onSurface = GeminiTextPrimary,
    surfaceVariant = GeminiDarkSurface,
    onSurfaceVariant = GeminiTextSecondary,
    outline = GeminiDarkBorder,
    outlineVariant = GeminiDarkDivider
)

// Clean minimalist light scheme if requested, defaulting to Gemini aesthetic
private val GeminiLightColorScheme = lightColorScheme(
    primary = Color(0xFF1F1F1F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F4F9),
    onPrimaryContainer = Color(0xFF1F1F1F),
    secondary = Color(0xFF0B57D0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3FD),
    onSecondaryContainer = Color(0xFF041E49),
    tertiary = Color(0xFF7C5295),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFF0F4F9),
    onSurfaceVariant = Color(0xFF444746),
    outline = Color(0xFFE0E2E0),
    outlineVariant = Color(0xFFF2F2F2)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force true dark mode like Gemini
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) GeminiDarkColorScheme else GeminiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
