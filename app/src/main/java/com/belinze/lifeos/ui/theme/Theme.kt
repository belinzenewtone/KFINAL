package com.belinze.lifeos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Extra app-specific colour tokens that don't map to M3 roles.
// Accessed via LocalAppColors.current inside any composable.
// ─────────────────────────────────────────────────────────────────────────────

data class AppColors(
    // Raw background tokens
    val bgPrimary:   Color,
    val bgSecondary: Color,
    val bgElevated:  Color,
    val bgTertiary:  Color,
    // Text
    val textPrimary:   Color,
    val textSecondary: Color,
    val textTertiary:  Color,
    val textInverse:   Color,
    // Borders
    val border:       Color,
    val borderSubtle: Color,
    // Glass overlays
    val glassWhite:       Color,
    val glassWhiteStrong: Color,
    val glassBlack:       Color,
    // Semantic
    val success: Color,
    val warning: Color,
    val danger:  Color,
    val info:    Color,
)

private val DarkAppColors = AppColors(
    bgPrimary         = BgPrimary,
    bgSecondary       = BgSecondary,
    bgElevated        = BgElevated,
    bgTertiary        = BgTertiary,
    textPrimary       = TextPrimary,
    textSecondary     = TextSecondary,
    textTertiary      = TextTertiary,
    textInverse       = TextInverseDark,
    border            = BorderDark,
    borderSubtle      = BorderSubtleDark,
    glassWhite        = GlassWhite,
    glassWhiteStrong  = GlassWhiteStrong,
    glassBlack        = GlassBlackDark,
    success           = ColorSuccess,
    warning           = ColorWarning,
    danger            = ColorDanger,
    info              = ColorInfo,
)

private val LightAppColors = AppColors(
    bgPrimary         = BgPrimaryLight,
    bgSecondary       = BgSecondaryLight,
    bgElevated        = BgElevatedLight,
    bgTertiary        = BgTertiaryLight,
    textPrimary       = TextPrimaryLight,
    textSecondary     = TextSecondaryLight,
    textTertiary      = TextTertiaryLight,
    textInverse       = TextInverseLight,
    border            = BorderLight,
    borderSubtle      = BorderSubtleLight,
    glassWhite        = GlassWhiteLight,
    glassWhiteStrong  = GlassWhiteStrongLight,
    glassBlack        = GlassBlackLight,
    success           = Color(0xFF1A7F37),
    warning           = Color(0xFF9A6700),
    danger            = Color(0xFFCF222E),
    info              = Color(0xFF0369A1),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ─────────────────────────────────────────────────────────────────────────────
// LifeOsTheme — the root composable. Wraps MaterialTheme with our colour
// scheme, typography, and shapes; also provides [LocalAppColors].
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LifeOsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) LifeOsDarkColorScheme else LifeOsLightColorScheme
    val appColors   = if (darkTheme) DarkAppColors        else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = LifeOsTypography,
            shapes      = LifeOsShapes,
            content     = content,
        )
    }
}

/** Shorthand accessor for extra app colours inside composables. */
val appColors: AppColors
    @Composable get() = LocalAppColors.current
