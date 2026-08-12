package com.belinze.lifeos.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.belinze.lifeos.ui.theme.ShapeHeroBottom

// ─────────────────────────────────────────────────────────────────────────────
// HeroSurface
//
// 1:1 port of src/components/layout/HeroSurface.tsx.
//
// Spec:
//  ‣ Top corners: 0dp, Bottom corners: 28dp (ShapeHeroBottom)
//  ‣ Background: gradient from primary (top) → surface/bg (bottom)
//  ‣ Frost overlay on top for subtle texture
//  ‣ Full-width, wraps arbitrary content (balance card, stats row, etc.)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HeroSurface(
    modifier:        Modifier    = Modifier,
    gradientVariant: HeroVariant = HeroVariant.Default,
    content:         @Composable BoxScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()

    val gradient: Brush = when (gradientVariant) {
        HeroVariant.Default -> if (isDark) {
            // Deep navy → near-black, ported from HomeScreen hero gradient
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF0A1628),   // deep navy at top
                    0.55f to Color(0xFF0D1F3C),
                    1.00f to Color(0xFF0A0A0B),   // matches bg_primary
                ),
            )
        } else {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF0369A1),   // primary light
                    0.55f to Color(0xFF075985),
                    1.00f to Color(0xFFE8EDF3),   // bg_primary light
                ),
            )
        }

        HeroVariant.Finance -> if (isDark) {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF0B1A1A),
                    0.55f to Color(0xFF0D1F24),
                    1.00f to Color(0xFF0A0A0B),
                ),
            )
        } else {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF0D9488),
                    1.00f to Color(0xFFE8EDF3),
                ),
            )
        }
    }

    // Frost overlay — subtle shimmer to match RN LinearGradient overlay
    val frostOverlay: Color = if (isDark)
        Color.White.copy(alpha = 0.03f)
    else
        Color.White.copy(alpha = 0.12f)

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
