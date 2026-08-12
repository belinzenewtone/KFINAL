package com.belinze.lifeos.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// ─────────────────────────────────────────────────────────────────────────────
// Motion tokens — ported 1:1 from src/theme/index.ts motion values.
// ─────────────────────────────────────────────────────────────────────────────

object Motion {
    /** 100 ms — navigation slide, tab pill opacity fade */
    const val fast        = 100
    /** 180 ms — standard transitions, banner exit */
    const val standard    = 180
    /** 260 ms — slow reveals */
    const val slow        = 260
    /** 900 ms — shimmer pulse (alpha 0.35 ↔ 0.85, reversed) */
    const val shimmer     = 900
    /** 220 ms — TopBanner enter (slide + fade) */
    const val bannerEnter = 220
    /** 180 ms — TopBanner exit (= standard) */
    const val bannerExit  = 180
}

// ─────────────────────────────────────────────────────────────────────────────
// Named AnimationSpec helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Standard "fast" tween — 100 ms, no easing. Used for nav slide and tab pill. */
fun <T> fastTween(): TweenSpec<T> = tween(durationMillis = Motion.fast)

/** Standard "default" tween — 180 ms. */
fun <T> standardTween(): TweenSpec<T> = tween(durationMillis = Motion.standard)

/** Slow tween — 260 ms. */
fun <T> slowTween(): TweenSpec<T> = tween(durationMillis = Motion.slow)

/**
 * Tab-bar scale spring.
 *
 * Derived from the RN Animated.spring config: friction=7, tension=90.
 *   dampingRatio ≈ friction / (2 * sqrt(tension)) = 7 / (2 * sqrt(90)) ≈ 0.37
 *   stiffness    ≈ tension = 90 (in Compose units, scaled to ~375f for visual match)
 *
 * Fine-tune in Phase 8 with side-by-side 120 fps comparison.
 */
fun <T> tabScaleSpring(): SpringSpec<T> = spring(
    dampingRatio = 0.43f,
    stiffness    = 375f,
)

/**
 * Shimmer pulse tween — 900 ms per direction.
 * Used in ShimmerLoadingState (opacity 0.35 ↔ 0.85, reversed).
 */
fun <T> shimmerTween(): TweenSpec<T> = tween(durationMillis = 900)

/**
 * TopBanner enter tween — 220 ms.
 * Drives both fadeIn and slideInVertically.
 */
fun <T> bannerEnterTween(): TweenSpec<T> = tween(durationMillis = 220)

/**
 * TopBanner exit tween — 180 ms.
 */
fun <T> bannerExitTween(): TweenSpec<T> = tween(durationMillis = Motion.standard)
