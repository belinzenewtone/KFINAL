package com.belinze.lifeos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Typography — ported 1:1 from src/theme/paperTheme.ts font overrides
// and src/theme/index.ts typography tokens.
//
// System font family is used (same as RN's default on Android).
// Custom fonts can be added via @Font annotations in a future iteration.
// ─────────────────────────────────────────────────────────────────────────────

private val default = FontFamily.Default

/**
 * Full type scale matching the RN app's paperTheme.ts exactly.
 *
 * Token mapping:
 *   headlineLarge  → 30sp / weight 700 / ls -0.5
 *   headlineMedium → 24sp / weight 700 / ls -0.4
 *   headlineSmall  → 20sp / weight 700 / ls -0.3
 *   titleLarge     → 18sp / weight 600 / ls -0.2
 *   titleMedium    → 16sp / weight 600 / ls 0
 *   titleSmall     → 14sp / weight 600
 *   bodyLarge      → 16sp / weight 400
 *   bodyMedium     → 15sp / weight 400
 *   bodySmall      → 13sp / weight 400
 *   labelLarge     → 14sp / weight 600 (matches typography.sizes.sm)
 *   labelMedium    → 12sp / weight 500
 *   labelSmall     → 11sp / weight 500 / ls +0.08em
 */
val LifeOsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 30.sp,
        lineHeight   = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 24.sp,
        lineHeight   = 30.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 20.sp,
        lineHeight   = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 18.sp,
        lineHeight   = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 15.sp,
        lineHeight   = 21.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.5.sp,
    ),
    displayLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 57.sp,
        lineHeight   = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 45.sp,
        lineHeight   = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 36.sp,
        lineHeight   = 44.sp,
        letterSpacing = 0.sp,
    ),
)
