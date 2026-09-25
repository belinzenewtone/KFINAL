package com.belinze.lifeos.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ─────────────────────────────────────────────────────────────────────────────
// Raw design tokens — ported 1:1 from src/theme/index.ts (dark-first)
// and src/theme/paperTheme.ts (MD3 role overrides)
// ─────────────────────────────────────────────────────────────────────────────

// Background surfaces (dark)
val BgPrimary    = Color(0xFF08090E)
val BgSecondary  = Color(0xFF141825)
val BgElevated   = Color(0xFF18202F)
val BgTertiary   = Color(0xFF1B2335)

// Accents
val AccentPrimary   = Color(0xFF57B9FF)
val AccentSecondary = Color(0xFF8B5CF6)
val AccentTertiary  = Color(0xFF5EEAD4)

// Semantic
val ColorSuccess = Color(0xFF4ADE80)
val ColorWarning = Color(0xFFFBBF24)
val ColorDanger  = Color(0xFFF87171)
val ColorInfo    = Color(0xFF60A5FA)

// Text (dark)
val TextPrimary   = Color(0xFFF0F2F8)
val TextSecondary = Color(0xFF9499B0)
val TextTertiary  = Color(0xFF6B7280)
val TextInverseDark = Color(0xFF08090E)

// Borders (dark)
val BorderDark       = Color(0xFF2A2E42)
val BorderSubtleDark = Color(0xFF1E2235)

// Glass overlays (dark)
val GlassWhite       = Color(0x0DFFFFFF) // rgba(255,255,255,0.05)
val GlassWhiteStrong = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)
val GlassBlackDark   = Color(0x47000000) // rgba(0,0,0,0.28)

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
    "housing"       to Color(0xFFF43F5E),
    "personal_care" to Color(0xFFF472B6),
    "subscriptions" to Color(0xFFA78BFA),
    "fuel"          to Color(0xFFF97316),
    "loans"         to Color(0xFFEF4444),
    "insurance"     to Color(0xFF7C3AED),
    "miscellaneous" to Color(0xFF94A3B8),
    "income"        to Color(0xFF34D399),
    "expense"       to Color(0xFFEF4444),
    "transfer"      to Color(0xFF60A5FA),
    "fuliza"        to Color(0xFFFB923C),
    "withdrawal"    to Color(0xFFF87171),
    "uncategorized" to Color(0xFF6B7280),
)

fun categoryColor(category: String): Color =
    CategoryColors[category.lowercase()] ?: Color(0xFF6B7280)

/** Icon per category — mirrors src/constants/index.ts CATEGORY_ICONS. */
private val CategoryIcons: Map<String, ImageVector> by lazy {
    mapOf(
        "food"          to Icons.Outlined.Restaurant,
        "transport"     to Icons.Outlined.DirectionsCar,
        "utilities"     to Icons.Outlined.Bolt,
        "groceries"     to Icons.Outlined.ShoppingCart,
        "rent"          to Icons.Outlined.Home,
        "airtime"       to Icons.Outlined.PhoneAndroid,
        "entertainment" to Icons.Outlined.Movie,
        "health"        to Icons.Outlined.MedicalServices,
        "education"     to Icons.Outlined.School,
        "shopping"      to Icons.Outlined.ShoppingBag,
        "savings"       to Icons.Outlined.Savings,
        "investment"    to Icons.Outlined.TrendingUp,
        "housing"       to Icons.Outlined.Business,
        "personal_care" to Icons.Outlined.AutoAwesome,
        "subscriptions" to Icons.Outlined.Repeat,
        "fuel"          to Icons.Outlined.LocalFireDepartment,
        "loans"         to Icons.Outlined.Payments,
        "insurance"     to Icons.Outlined.VerifiedUser,
        "miscellaneous" to Icons.Outlined.MoreHoriz,
        "uncategorized" to Icons.Outlined.HelpOutline,
        "income"        to Icons.Outlined.ArrowDownward,
        "expense"       to Icons.Outlined.ArrowUpward,
        "transfer"      to Icons.Outlined.SwapHoriz,
        "fuliza"        to Icons.Outlined.Payments,
        "withdrawal"    to Icons.Outlined.ArrowUpward,
    )
}

fun categoryIcon(category: String): ImageVector =
    CategoryIcons[category.lowercase()] ?: Icons.Outlined.HelpOutline

// ─────────────────────────────────────────────────────────────────────────────
// Priority colours
// ─────────────────────────────────────────────────────────────────────────────

val PriorityLow    = Color(0xFF3B82F6)
val PriorityMedium = Color(0xFFF59E0B)
val PriorityHigh   = Color(0xFFEF4444)

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 color schemes — values from src/theme/paperTheme.ts exactly
// ─────────────────────────────────────────────────────────────────────────────

/** Dark scheme — mirrors MD3DarkTheme overrides in paperTheme.ts exactly */
val LifeOsDarkColorScheme = darkColorScheme(
    primary              = Color(0xFF57B9FF),
    onPrimary            = Color(0xFF030B14),
    primaryContainer     = Color(0xFF0F3356),
    onPrimaryContainer   = Color(0xFFBAE0FF),
    secondary            = Color(0xFF9BA3B8),
    onSecondary          = Color(0xFF0B0D14),
    secondaryContainer   = Color(0xFF1A3A60),
    onSecondaryContainer = Color(0xFFBAE0FF),
    tertiary             = Color(0xFF5EEAD4),
    onTertiary           = Color(0xFF050F0D),
    tertiaryContainer    = Color(0xFF0C2D27),
    onTertiaryContainer  = Color(0xFFCCFBF1),
    background           = Color(0xFF08090E),
    onBackground         = Color(0xFFF0F2F8),
    surface              = Color(0xFF0C0E16),
    onSurface            = Color(0xFFF0F2F8),
    surfaceVariant       = Color(0xFF141825),
    onSurfaceVariant     = Color(0xFF9499B0),
    outline              = Color(0xFF2A2E42),
    outlineVariant       = Color(0xFF1E2235),
    error                = Color(0xFFF87171),
    errorContainer       = Color(0xFF331018),
    onError              = Color(0xFF0A0A0B),
    onErrorContainer     = Color(0xFFFECACA),
    surfaceTint          = Color(0xFF57B9FF),
    inverseSurface       = Color(0xFFEEF0F8),
    inverseOnSurface     = Color(0xFF08090E),
    inversePrimary       = Color(0xFF1A6FA8),
    scrim                = Color(0xFF000000),
)

/** Light scheme — mirrors lifeosPaperThemeLight overrides in paperTheme.ts exactly */
val LifeOsLightColorScheme = lightColorScheme(
    primary              = Color(0xFF0369A1),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFBAE6FD),
    onPrimaryContainer   = Color(0xFF082F49),
    secondary            = Color(0xFF64748B),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary             = Color(0xFF0F766E),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFCCFBF1),
    onTertiaryContainer  = Color(0xFF134E4A),
    background           = BgPrimaryLight,           // #E8EDF3
    onBackground         = TextPrimaryLight,         // #0F172A
    surface              = BgSecondaryLight,         // #F8FAFC
    onSurface            = TextPrimaryLight,
    surfaceVariant       = BgElevatedLight,          // #EEF2F7
    onSurfaceVariant     = TextSecondaryLight,       // #475569
    outline              = Color(0xFFCBD5E1),
    outlineVariant       = Color(0xFFE2E8F0),
    error                = Color(0xFFDC2626),
    errorContainer       = Color(0xFFFEE2E2),
    onError              = Color(0xFFFFFFFF),
    onErrorContainer     = Color(0xFF7F1D1D),
    surfaceTint          = Color(0xFF0369A1),
    inverseSurface       = Color(0xFF1E293B),
    inverseOnSurface     = Color(0xFFF8FAFC),
    inversePrimary       = Color(0xFF7DD3FC),
    scrim                = Color(0xFF000000),
)

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 elevation surface tints (from paperTheme.ts elevations)
// ─────────────────────────────────────────────────────────────────────────────

object ElevationColors {
    val level1 = Color(0xFF10131E)
    val level2 = Color(0xFF141825)
    val level3 = Color(0xFF18202F)
    val level4 = Color(0xFF1B2335)
    val level5 = Color(0xFF1F273C)
}
