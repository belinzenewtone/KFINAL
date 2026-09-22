package com.belinze.lifeos.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.belinze.lifeos.ui.theme.LocalDarkTheme
import com.belinze.lifeos.ui.theme.ShapeHeroBottom

// ─────────────────────────────────────────────────────────────────────────────
// HeroSurface
//
// 1:1 port of src/components/layout/HeroSurface.tsx.
//
// Spec:
//  ‣ Top corners: 0dp, Bottom corners: 28dp (ShapeHeroBottom)
//  ‣ Background: theme-adaptive gradient — primary (top) → background (bottom)
//  ‣ Frost overlay on top for subtle texture
//  ‣ Full-width, wraps arbitrary content (balance card, stats row, etc.)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HeroSurface(
    modifier:        Modifier    = Modifier,
    gradientVariant: HeroVariant = HeroVariant.Default,
    content:         @Composable BoxScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = LocalDarkTheme.current

    val gradient: Brush = when (gradientVariant) {
        HeroVariant.Default -> Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to cs.primary,
                0.55f to cs.primary.copy(alpha = if (isDark) 0.55f else 0.60f),
                1.00f to cs.background,
            ),
        )

        HeroVariant.Finance -> Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to cs.tertiary,
                1.00f to cs.background,
            ),
        )
    }

    // Frost overlay — subtle shimmer to match RN LinearGradient overlay
    val frostOverlay: Color = if (isDark) {
        Color.White.copy(alpha = 0.03f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeHeroBottom)
            .drawBehind {
                drawRect(brush = gradient)
                drawRect(color = frostOverlay)
            },
        content = content,
    )
}

enum class HeroVariant { Default, Finance }
