package com.belinze.lifeos.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.LocalDarkTheme
import com.belinze.lifeos.ui.theme.Motion
import com.belinze.lifeos.ui.theme.ShapeMd
import com.belinze.lifeos.ui.theme.ShapeSm
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// ShimmerLoadingState
//
// 1:1 port of src/components/common/ShimmerLoadingState.tsx.
//
// Spec:
//  ‣ InfiniteTransition: alpha 0.35 ↔ 0.85, duration 900ms (shimmerTween)
//  ‣ RepeatMode.Reverse — pulses back and forth
//  ‣ Shimmer color: onSurface-derived with interpolated alpha
//  ‣ Multiple rows of placeholder skeletons matching the screen content
// ─────────────────────────────────────────────────────────────────────────────

/** Single shimmering rectangle placeholder. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height:   Dp       = 16.dp,
    width:    Dp?      = null,         // null = fillMaxWidth
    alpha:    Float    = 0.5f,
) {
    val isDark = LocalDarkTheme.current
    val baseColor = if (isDark) Color(0xFF2E2E33) else Color(0xFFCBD5E1)

    val boxMod = if (width != null) modifier.width(width) else modifier.fillMaxWidth()

    Box(
        modifier = boxMod
            .height(height)
            .clip(ShapeSm)
            .background(baseColor.copy(alpha = alpha)),
    )
}

/** Full shimmer loading state — card rows pulsing together. */
@Composable
fun ShimmerLoadingState(
    modifier:  Modifier = Modifier,
    rowCount:  Int      = 4,
    showHero:  Boolean  = false,
) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val alpha by infinite.animateFloat(
        initialValue   = 0.35f,
        targetValue    = 0.85f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(durationMillis = Motion.shimmer),  // 900ms
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )

    Column(
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Optional hero placeholder (e.g. balance card)
        if (showHero) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(ShapeMd)
                    .background(
                        if (LocalDarkTheme.current) {
                            Color(0xFF161618).copy(alpha = alpha)
                        } else {
                            Color(0xFFCBD5E1).copy(alpha = alpha)
                        }
                    ),
            )
            Spacer(Modifier.height(Spacing.md))
        }

        // Row skeletons
        repeat(rowCount) { index ->
            ShimmerRow(alpha = alpha, wide = index % 2 == 0)
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

/** One shimmer row — two stacked rectangle placeholders. */
@Composable
private fun ShimmerRow(
    alpha: Float,
    wide:  Boolean = true,
) {
    val isDark = LocalDarkTheme.current
    val baseColor = if (isDark) Color(0xFF2E2E33) else Color(0xFFCBD5E1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Title line
        Box(
            modifier = Modifier
                .fillMaxWidth(if (wide) 0.75f else 0.55f)
                .height(14.dp)
                .clip(ShapeSm)
                .background(baseColor.copy(alpha = alpha)),
        )
        // Subtitle line
        Box(
            modifier = Modifier
                .fillMaxWidth(if (wide) 0.50f else 0.38f)
                .height(11.dp)
                .clip(ShapeSm)
                .background(baseColor.copy(alpha = alpha * 0.70f)),
        )
    }
}
