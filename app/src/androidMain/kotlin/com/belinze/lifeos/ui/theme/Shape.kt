package com.belinze.lifeos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Shape tokens — ported 1:1 from src/theme/index.ts borderRadius values.
//
// borderRadius token → dp:
//   sm   →  8 dp
//   md   → 12 dp
//   lg   → 20 dp  (used by GlassCard, FrostCard, FloatingTabBar, etc.)
//   xl   → 24 dp  (used by chips, ShimmerLoadingState rows)
//   2xl  → 32 dp
//   full → 9999 dp (pill shape)
// ─────────────────────────────────────────────────────────────────────────────

// Named shape tokens (use these in components for semantic clarity)
val ShapeSm   = RoundedCornerShape(8.dp)
val ShapeMd   = RoundedCornerShape(12.dp)
val ShapeLg   = RoundedCornerShape(20.dp)
val ShapeXl   = RoundedCornerShape(24.dp)
val Shape2xl  = RoundedCornerShape(32.dp)
val ShapePill = RoundedCornerShape(9999.dp)

// Hero surface: bottom corners only, radius 28 dp (HeroSurface component)
val ShapeHeroBottom = RoundedCornerShape(
    bottomStart = 28.dp,
    bottomEnd   = 28.dp,
)

// Material 3 Shapes — map to the same scale
// (Material 3 uses extra-small → extra-large)
val LifeOsShapes = Shapes(
    extraSmall = ShapeSm,    //  8 dp  — small chips, tooltips
    small      = ShapeMd,    // 12 dp  — cards in dense layouts
    medium     = ShapeLg,    // 20 dp  — GlassCard, FrostCard, main cards
    large      = ShapeXl,    // 24 dp  — FloatingTabBar, sheet handles
    extraLarge = Shape2xl,   // 32 dp  — bottom sheets, dialogs
)
