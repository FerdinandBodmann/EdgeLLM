package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Authentic Claude Warm Dark Theme Palette
// Signature warm espresso background, graphite cards, terracotta coral accent, and soft ivory cream text
val ClaudeDarkBackground = Color(0xFF1E1D1B)        // Warm roasted espresso canvas
val ClaudeDarkSurface = Color(0xFF262522)           // Claude graphite surface card
val ClaudeDarkSurfaceElevated = Color(0xFF2E2C28)   // Elevated input pill / dock
val ClaudeDarkSurfaceHighlight = Color(0xFF383530)  // Active chip pill & hover
val ClaudeDarkBorder = Color(0xFF3E3B35)            // Subtle warm hairline border
val ClaudeDarkDivider = Color(0xFF33302B)

// Claude Text Hierarchy (Soft Ivory Cream & Warm Sand Greys)
val ClaudeTextPrimary = Color(0xFFFAF7F2)           // Soft warm ivory text
val ClaudeTextSecondary = Color(0xFFC4BCB3)         // Warm sand text
val ClaudeTextTertiary = Color(0xFF8B847B)          // Muted warm slate / metadata text

// Claude Iconic Accents
val ClaudeCoral = Color(0xFFD97757)                 // Signature Anthropic Claude Terracotta Coral
val ClaudeCoralDark = Color(0xFFBF6243)
val ClaudeCoralLight = Color(0xFFECA389)
val ClaudeCoralContainerDark = Color(0xFF3D2A24)
val ClaudeCoralContainerLight = Color(0xFF5A392F)

val ClaudeAmberWheat = Color(0xFFE8B67D)            // Warm amber / sunburst tone
val ClaudeOliveSage = Color(0xFFA3B19B)             // Soft botanical sage
val ClaudeWarmSlate = Color(0xFF948D84)

// Functional Status
val WarmSuccessGreen = Color(0xFF7CB88B)
val WarmWarningAmber = Color(0xFFE8B67D)
val WarmErrorRed = Color(0xFFE06D53)

// Containers
val ClaudeUserBubble = Color(0xFF2B2925)            // Warm user chat bubble
val ClaudeCodeBlockBackground = Color(0xFF161514)   // Deep obsidian code fence

// Gemini Aliases (mapped directly to Claude Warm Dark theme so entire app transforms to Claude)
val GeminiDarkBackground = ClaudeDarkBackground
val GeminiDarkSurface = ClaudeDarkSurface
val GeminiDarkSurfaceElevated = ClaudeDarkSurfaceElevated
val GeminiDarkSurfaceHighlight = ClaudeDarkSurfaceHighlight
val GeminiDarkBorder = ClaudeDarkBorder
val GeminiDarkDivider = ClaudeDarkDivider

val GeminiTextPrimary = ClaudeTextPrimary
val GeminiTextSecondary = ClaudeTextSecondary
val GeminiTextTertiary = ClaudeTextTertiary

val GeminiBlue = ClaudeCoral                        // Terracotta replaces cold blue
val GeminiBlueContainer = ClaudeCoralContainerDark
val GeminiLightBlue = ClaudeCoralLight
val GeminiPurple = ClaudeAmberWheat
val GeminiWhite = ClaudeTextPrimary                 // Soft warm cream replaces harsh pure white

val GeminiSuccessGreen = WarmSuccessGreen
val GeminiWarningAmber = WarmWarningAmber
val GeminiErrorRed = WarmErrorRed

val GeminiUserBubble = ClaudeUserBubble
val GeminiCodeBlockBackground = ClaudeCodeBlockBackground

val ClaudeDarkTextPrimary = ClaudeTextPrimary
val ClaudeDarkTextSecondary = ClaudeTextSecondary
val ClaudeDarkTextTertiary = ClaudeTextTertiary

val ClaudeLightBackground = ClaudeDarkBackground
val ClaudeLightSurface = ClaudeDarkSurface
val ClaudeLightSurfaceElevated = ClaudeDarkSurfaceElevated
val ClaudeLightSurfaceHighlight = ClaudeDarkSurfaceHighlight
val ClaudeLightBorder = ClaudeDarkBorder
val ClaudeLightTextPrimary = ClaudeTextPrimary
val ClaudeLightTextSecondary = ClaudeTextSecondary
val ClaudeLightTextTertiary = ClaudeTextTertiary
