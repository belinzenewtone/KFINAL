package com.belinze.lifeos.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.ShapeLg

// ─────────────────────────────────────────────────────────────────────────────
// GlassCard
//
// 1:1 port of src/components/common/GlassCard.tsx.
// All layers are composited in drawBehind — no native blur used (same as RN).
//
// Layer stack (bottom to top):
//   1. LinearGradient  — diagonal, top-left → bottom-right
//   2. Tint overlay    — variant-specific semi-transparent colour
//   3. Frost film      — dark rgba(20,22,28,0.45) / light rgba(248,250,252,0.50)
//   4. Hairline border — 1dp inner stroke
//   5. Content         — Column with 12dp padding
// ─────────────────────────────────────────────────────────────────────────────

enum class GlassCardVariant { Default, Elevated, Accent }

@Composable
fun GlassCard(
    modifier:  Modifier           = Modifier,
    variant:   GlassCardVariant   = GlassCardVariant.Default,
    onClick:   (() -> Unit)?       = null,
    content:   @Composable ColumnScope.() -> Unit,
) {
    val isDark  = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary

    // ── Layer 1: gradient background ─────────────────────────────────────────
    val gradient: Brush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF101014), Color(0xFF0E1B2E), Color(0xFF101014)),
            start  = Offset.Zero,
            end    = Offset.Infinite,
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF), Color(0xFFFFFFFF)),
            start  = Offset.Zero,
            end    = Offset.Infinite,
        )
    }

    // ── Layer 2: tint overlay ─────────────────────────────────────────────────
    // React: accent primary22; elevated rgba(40,47,60,.60) dark / rgba(226,232,240,.60)
    // light; default rgba(30,35,45,.55) dark / rgba(241,245,249,.55) light.
    val tintColor: Color = when (variant) {
        GlassCardVariant.Accent   -> primary.copy(alpha = 0x22 / 255f)
        GlassCardVariant.Elevated -> if (isDark)
            Color(0xFF282F3C).copy(alpha = 0.60f)
        else
            Color(0xFFE2E8F0).copy(alpha = 0.60f)
        GlassCardVariant.Default  -> if (isDark)
            Color(0xFF1E232D).copy(alpha = 0.55f)
        else
            Color(0xFFF1F5F9).copy(alpha = 0.55f)
    }

    // ── Layer 3: frost film ───────────────────────────────────────────────────
    val frostColor: Color = if (isDark)
        Color(0xFF14161C).copy(alpha = 0.45f)
    else
        Color(0xFFF8FAFC).copy(alpha = 0.50f)

    // ── Layer 4: hairline border ──────────────────────────────────────────────
    // React: accent primary33; elevated white 12% dark / black 10% light;
    // default white 8% dark / black 7% light.
    val hairlineColor: Color = when (variant) {
        GlassCardVariant.Accent   -> primary.copy(alpha = 0x33 / 255f)
        GlassCardVariant.Elevated -> if (isDark) Color.White.copy(alpha = 0.12f)
                                     else        Color.Black.copy(alpha = 0.10f)
        GlassCardVariant.Default  -> if (isDark) Color.White.copy(alpha = 0.08f)
                                     else        Color.Black.copy(alpha = 0.07f)
    }

    val clickModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier.clickable(
            interactionSource = interactionSource,
            indication        = ripple(color = primary.copy(alpha = 0.20f)),
            onClick           = onClick,
        )
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeLg)                         // borderRadius.lg = 20dp
            .drawBehind {
                drawRect(brush = gradient)           // Layer 1
                drawRect(color = tintColor)          // Layer 2
                drawRect(color = frostColor)         // Layer 3
            }
            .border(1.dp, hairlineColor, ShapeLg)  // Layer 4
            .then(clickModifier),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),    // internal padding 12dp
            content  = content,
        )
    }
}
