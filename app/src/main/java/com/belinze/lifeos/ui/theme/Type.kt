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
 * Full type scale — tightened from the original 1:1 RN port.
 *
 * Changes vs v1:
 *   • bodyLarge  16sp → 15sp  (creates clear 1sp gap to bodyMedium's 14sp)
 *   • bodyMedium 15sp → 14sp  (matches compact Android convention)
 *   • Line heights reduced ~15% across all styles — Android system font has
 *     tighter default metrics than React Native's JS bridge renderer, so the
 *     old heights (1.5× font size) felt loose and "too tall".
 *
 * Token mapping (updated):
 *   headlineLarge  → 30sp / lh 34sp / weight 700 / ls -0.5
 *   headlineMedium → 24sp / lh 28sp / weight 700 / ls -0.4
 *   headlineSmall  → 20sp / lh 24sp / weight 700 / ls -0.3
 *   titleLarge     → 18sp / lh 22sp / weight 600 / ls -0.2
 *   titleMedium    → 16sp / lh 20sp / weight 600 / ls 0
 *   titleSmall     → 14sp / lh 18sp / weight 600
 *   bodyLarge      → 15sp / lh 20sp / weight 400
 *   bodyMedium     → 14sp / lh 18sp / weight 400
 *   bodySmall      → 13sp / lh 16sp / weight 400
 *   labelLarge     → 14sp / lh 18sp / weight 600
 *   labelMedium    → 12sp / lh 16sp / weight 500
 *   labelSmall     → 11sp / lh 14sp / weight 500 / ls +0.08em
 */
val LifeOsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 30.sp,
        lineHeight   = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 24.sp,
        lineHeight   = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 20.sp,
        lineHeight   = 24.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 18.sp,
        lineHeight   = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 15.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 13.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily   = default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 18.sp,
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
