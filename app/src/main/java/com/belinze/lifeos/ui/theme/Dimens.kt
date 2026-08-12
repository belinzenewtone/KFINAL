package com.belinze.lifeos.ui.theme

import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Spacing tokens — ported 1:1 from src/theme/index.ts spacing values.
// ─────────────────────────────────────────────────────────────────────────────

object Spacing {
    /** 12 dp — horizontal screen padding used on all scrollable screens */
    val screenHorizontal = 12.dp

    val xs   = 4.dp    // extra-small
    val sm   = 8.dp    // small
    val md   = 12.dp   // medium (same as screenHorizontal)
    val base = 14.dp   // base (used for padding inside cards, section gaps)
    val lg   = 16.dp   // large
    val xl   = 24.dp   // extra-large
    val x2l  = 28.dp   // 2xl
    val x3l  = 36.dp   // 3xl
    val x4l  = 48.dp   // 4xl

    /**
     * 100 dp — bottom content padding on every scrollable screen.
     * Ensures content is not obscured by the floating tab bar (58 dp height
     * + its bottom offset from the safe area).
     */
    val bottomNavSafeArea = 100.dp
}

// ─────────────────────────────────────────────────────────────────────────────
// FloatingTabBar dimensions (Phase 2 — referenced here for easy lookup)
// ─────────────────────────────────────────────────────────────────────────────

object TabBarDimens {
    val height       = 58.dp
    val borderRadius = 24.dp      // ShapeXl
    val sideInset    = 12.dp      // Spacing.screenHorizontal
    val iconSize     = 24.dp
    val labelSize    = 10          // sp (Spacing.xs - 2 in the RN version = 10 sp)
}

// ─────────────────────────────────────────────────────────────────────────────
// PageScaffold / AppBar dimensions
// ─────────────────────────────────────────────────────────────────────────────

object AppBarDimens {
    val height          = 52.dp
    val horizontalPad   = 8.dp    // screenHorizontal - 4
    val backBtnSize     = 36.dp
    val iconSize        = 20.dp
}
