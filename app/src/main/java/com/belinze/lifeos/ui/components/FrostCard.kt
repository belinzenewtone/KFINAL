package com.belinze.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
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
// FrostCard
//
// 1:1 port of src/components/common/FrostCard.tsx.
//
// Layer stack (bottom to top):
//   1. LinearGradient  — glow-tinted gradient
//   2. Glow rings      — 3 concentric circles via Canvas at exact pixel offsets
//   3. Frost film      — semi-transparent overlay (rings visible through it)
//   4. Hairline border — 1dp inner stroke
//   5. Content         — Column with 16dp padding
// ─────────────────────────────────────────────────────────────────────────────

enum class FrostCardGlow { Blue, Teal, None }

@Composable
fun FrostCard(
    modifier: Modifier       = Modifier,
    glow:     FrostCardGlow  = FrostCardGlow.None,
    onClick:  (() -> Unit)?  = null,
    content:  @Composable ColumnScope.() -> Unit,
) {
    val isDark  = isSystemInDarkTheme()

    // ── Layer 1: gradient ────────────────────────────────────────────────────
    val gradient: Brush = when (glow) {
        FrostCardGlow.Blue -> if (isDark)
            Brush.linearGradient(colors = listOf(Color(0xFF12304A), Color(0xFF0E1B2E), Color(0xFF101014)), start = Offset.Zero, end = Offset.Infinite)
        else
            Brush.linearGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFF8FAFC)), start = Offset.Zero, end = Offset.Infinite)

        FrostCardGlow.Teal -> if (isDark)
            Brush.linearGradient(colors = listOf(Color(0xFF101014), Color(0xFF0E1F24), Color(0xFF101014)), start = Offset.Zero, end = Offset.Infinite)
        else
            Brush.linearGradient(colors = listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFFF8FAFC)), start = Offset.Zero, end = Offset.Infinite)

        FrostCardGlow.None -> if (isDark)
            Brush.linearGradient(colors = listOf(Color(0xFF101014), Color(0xFF0E1B2E), Color(0xFF101014)), start = Offset.Zero, end = Offset.Infinite)
        else
            Brush.linearGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF), Color(0xFFFFFFFF)), start = Offset.Zero, end = Offset.Infinite)
    }

    // ── Layer 3: frost film ──────────────────────────────────────────────────
    val frostColor: Color = if (isDark)
        Color(0xFF14161C).copy(alpha = 0.45f)
    else
        Color(0xFFFFFFFF).copy(alpha = 0.55f)

    // ── Layer 4: hairline ────────────────────────────────────────────────────
    val hairlineColor: Color = if (isDark)
        Color.White.copy(alpha = 0.10f)
    else
        Color.Black.copy(alpha = 0.07f)

    val clickModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            onClick = onClick,
        )
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeLg)                           // borderRadius.lg = 20dp
            .drawBehind { drawRect(brush = gradient) }  // Layer 1
            .then(clickModifier),
    ) {

        // ── Layer 2: glow rings (Canvas — drawn above gradient, below frost) ──
        if (glow != FrostCardGlow.None) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                when (glow) {
                    FrostCardGlow.Blue -> {
                        // Glow rings anchored to top-right corner
                        // RN spec: (top: -110, right: -80, size 320), (top: -70, right: -45, size 240), (top: -40, right: -20, size 175)
                        // Center.x = w - (-80) - 320/2 = w + 80 - 160 = w - 80 etc.
                        val alphas = if (isDark) listOf(0.10f, 0.12f, 0.14f) else listOf(0.06f, 0.08f, 0.10f)
                        val glowColor = Color(0xFF57B9FF)

                        drawCircle(glowColor.copy(alpha = alphas[0]), radius = 160.dp.toPx(),
                            center = Offset(w - 80.dp.toPx(), 50.dp.toPx()))     // (-110 + 320/2)
                        drawCircle(glowColor.copy(alpha = alphas[1]), radius = 120.dp.toPx(),
                            center = Offset(w - 45.dp.toPx() - 120.dp.toPx() + 120.dp.toPx(), -70.dp.toPx() + 120.dp.toPx()))
                        drawCircle(glowColor.copy(alpha = alphas[2]), radius = 87.5f.dp.toPx(),
                            center = Offset(w - 20.dp.toPx() - 87.5f.dp.toPx() + 87.5f.dp.toPx(), -40.dp.toPx() + 87.5f.dp.toPx()))
                    }

                    FrostCardGlow.Teal -> {
                        // Glow rings anchored to bottom-left corner
                        // RN spec: (bottom: -120, left: -80, size 320), (-80,-45,240), (-50,-20,175)
                        val alphas = if (isDark) listOf(0.07f, 0.09f, 0.11f) else listOf(0.04f, 0.06f, 0.08f)
                        val glowColor = Color(0xFF5EEAD4)

                        drawCircle(glowColor.copy(alpha = alphas[0]), radius = 160.dp.toPx(),
                            center = Offset(-80.dp.toPx() + 160.dp.toPx(), h + 120.dp.toPx() - 160.dp.toPx()))
                        drawCircle(glowColor.copy(alpha = alphas[1]), radius = 120.dp.toPx(),
                            center = Offset(-45.dp.toPx() + 120.dp.toPx(), h + 80.dp.toPx() - 120.dp.toPx()))
                        drawCircle(glowColor.copy(alpha = alphas[2]), radius = 87.5f.dp.toPx(),
                            center = Offset(-20.dp.toPx() + 87.5f.dp.toPx(), h + 50.dp.toPx() - 87.5f.dp.toPx()))
                    }

                    else -> Unit
                }
            }
        }

        // ── Layer 3: frost film (semi-transparent — rings glow through it) ───
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawBehind { drawRect(color = frostColor) }
                .border(1.dp, hairlineColor, ShapeLg),  // Layer 4
        )

        // ── Layer 5: content ─────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content,
        )
    }
}
