package com.belinze.lifeos.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Raw design tokens — ported 1:1 from src/theme/index.ts (dark-first)
// and src/theme/paperTheme.ts (MD3 role overrides)
// ─────────────────────────────────────────────────────────────────────────────

// Background surfaces (dark)
val BgPrimary    = Color(0xFF0B0E14)
val BgSecondary  = Color(0xFF1A1E26)
val BgElevated   = Color(0xFF1E232D)
val BgTertiary   = Color(0xFF232A36)

// Accents
val AccentPrimary   = Color(0xFF4DB8FF)
val AccentSecondary = Color(0xFF8B5CF6)
val AccentTertiary  = Color(0xFF38BDF8)

// Semantic
val ColorSuccess = Color(0xFF34D399)
val ColorWarning = Color(0xFFF59E0B)
val ColorDanger  = Color(0xFFFF6B6B)
val ColorInfo    = Color(0xFF4DB8FF)

// Text (dark)
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9CA3AF)
val TextTertiary  = Color(0xFF6B7280)
val TextInverseDark = Color(0xFF0B0E14)

// Borders (dark)
val BorderDark       = Color(0xFF2A303C)
val BorderSubtleDark = Color(0xFF1F2937)

// Glass overlays (dark)
val GlassWhite       = Color(0x0FFFFFFF) // rgba(255,255,255,0.06)
val GlassWhiteStrong = Color(0x1EFFFFFF) // rgba(255,255,255,0.12)
val GlassBlackDark   = Color(0x3D000000) // rgba(0,0,0,0.24)

// Background surfaces (light)
val BgPrimaryLight   = Color(0xFFE8EDF3)
val BgSecondaryLight = Color(0xFFF8FAFC)
val BgElevatedLight  = Color(0xFFEEF2F7)
val BgTertiaryLight  = Color(0xFFE2E8F0)

// Text (light)
val TextPrimaryLight   = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextTertiaryLight  = Color(0xFF94A3B8)
val TextInverseLight   = Color(0xFFFFFFFF)

// Borders (light)
val BorderLight       = Color(0xFFE2E8F0)
val BorderSubtleLight = Color(0xFFF1F5F9)

// Glass overlays (light)
val GlassWhiteLight       = Color(0xA3FFFFFF) // rgba(255,255,255,0.64)
val GlassWhiteStrongLight = Color(0xD6FFFFFF) // rgba(255,255,255,0.84)
val GlassBlackLight       = Color(0x0F000000) // rgba(0,0,0,0.06)

// ─────────────────────────────────────────────────────────────────────────────
// Category palette — 14 entries, used for chart colours and transaction icons
// ─────────────────────────────────────────────────────────────────────────────

val CategoryColors: Map<String, Color> = mapOf(
    "food"          to Color(0xFFF59E0B),
    "transport"     to Color(0xFF3B82F6),
    "utilities"     to Color(0xFF8B5CF6),
    "groceries"     to Color(0xFF10B981),
    "rent"          to Color(0xFFEF4444),
    "airtime"       to Color(0xFF06B6D4),
    "entertainment" to Color(0xFFEC4899),
    "health"        to Color(0xFFF97316),
    "education"     to Color(0xFF6366F1),
    "shopping"      to Color(0xFFD946EF),
    "savings"       to Color(0xFF22C55E),
    "investment"    to Color(0xFF14B8A6),
    "income"        to Color(0xFF34D399),
    "uncategorized" to Color(0xFF6B7280),
)

fun categoryColor(category: String): Color =
    CategoryColors[category.lowercase()] ?: Color(0xFF6B7280)

// ─────────────────────────────────────────────────────────────────────────────
// Priority colours
// ─────────────────────────────────────────────────────────────────────────────

val PriorityLow    = Color(0xFF3B82F6)
val PriorityMedium = Color(0xFFF59E0B)
val PriorityHigh   = Color(0xFFEF4444)

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 color schemes — values from src/theme/paperTheme.ts exactly
// ─────────────────────────────────────────────────────────────────────────────

/** Dark scheme — mirrors MD3DarkTheme overrides in paperTheme.ts */
val LifeOsDarkColorScheme = darkColorScheme(
    primary              = Color(0xFF57B9FF),
    onPrimary            = Color(0xFF0A0A0B),
    primaryContainer     = Color(0xFF0F2A40),
    onPrimaryContainer   = Color(0xFFBFE3FF),
    secondary            = AccentSecondary,          // #8B5CF6
    onSecondary          = Color(0xFF0A0A0B),
    secondaryContainer   = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary             = AccentTertiary,           // #38BDF8
    onTertiary           = Color(0xFF0A0A0B),
    tertiaryContainer    = Color(0xFF082030),
    onTertiaryContainer  = Color(0xFFB3E8FD),
    background           = Color(0xFF0A0A0B),
    onBackground         = Color(0xFFF4F4F5),
    surface              = Color(0xFF0A0A0B),
    onSurface            = Color(0xFFF4F4F5),
    surfaceVariant       = Color(0xFF161618),
    onSurfaceVariant     = Color(0xFFA1A1AA),
    outline              = Color(0xFF2E2E33),
    outlineVariant       = Color(0xFF222226),
    error                = Color(0xFFF87171),
    errorContainer       = Color(0xFF3A1214),
    onError              = Color(0xFFFFFFFF),
    onErrorContainer     = Color(0xFFFCA5A5),
    surfaceTint          = Color(0xFF57B9FF),
    inverseSurface       = Color(0xFFF4F4F5),
    inverseOnSurface     = Color(0xFF0A0A0B),
    inversePrimary       = Color(0xFF0369A1),
    scrim                = Color(0xFF000000),
)

/** Light scheme — mirrors lifeosPaperThemeLight overrides */
val LifeOsLightColorScheme = lightColorScheme(
    primary              = Color(0xFF0369A1),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFDBEAFE),
    onPrimaryContainer   = Color(0xFF0369A1),
    secondary            = Color(0xFF7C3AED),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary             = Color(0xFF0EA5E9),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFE0F2FE),
    onTertiaryContainer  = Color(0xFF0369A1),
    background           = BgPrimaryLight,           // #E8EDF3
    onBackground         = TextPrimaryLight,         // #0F172A
    surface              = BgSecondaryLight,         // #F8FAFC
    onSurface            = TextPrimaryLight,
    surfaceVariant       = BgElevatedLight,          // #EEF2F7
    onSurfaceVariant     = TextSecondaryLight,       // #475569
    // outline: used for input borders, switch thumb, decorative strokes.
    // Was #E2E8F0 (barely visible). Now slate-500 for proper contrast on light bg.
    outline              = Color(0xFF64748B),
    // outlineVariant: subtle dividers. Was #F1F5F9 (invisible). Now slate-200.
    outlineVariant       = Color(0xFFCBD5E1),
    error                = Color(0xFFCF222E),
    errorContainer       = Color(0xFFFFE4E6),
    onError              = Color(0xFFFFFFFF),
    onErrorContainer     = Color(0xFF7F1D1D),
    surfaceTint          = Color(0xFF0369A1),
    inverseSurface       = Color(0xFF0A0A0B),
    inverseOnSurface     = Color(0xFFF4F4F5),
    inversePrimary       = Color(0xFF57B9FF),
    scrim                = Color(0xFF000000),
)

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 elevation surface tints (from paperTheme.ts elevations)
// ─────────────────────────────────────────────────────────────────────────────

object ElevationColors {
    val level1 = Color(0xFF131315)
    val level2 = Color(0xFF161618)
    val level3 = Color(0xFF1A1A1D)
    val level4 = Color(0xFF1D1D20)
    val level5 = Color(0xFF212124)
}
