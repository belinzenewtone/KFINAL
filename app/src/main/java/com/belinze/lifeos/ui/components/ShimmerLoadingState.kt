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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.Motion
import com.belinze.lifeos.ui.theme.ShapeXl
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// ShimmerLoadingState
//
// 1:1 port of src/components/common/ShimmerLoadingState.tsx.
//
// Spec:
//  ‣ Simple full-width rectangle placeholders (surfaceVariant), no icon/text
//  ‣ InfiniteTransition: alpha 0.35 ↔ 0.85, duration 900ms (shimmer)
//  ‣ RepeatMode.Reverse — pulses back and forth
//  ‣ rowHeight 72dp, borderRadius.xl (ShapeXl), base spacing between rows
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ShimmerLoadingState(
    modifier:  Modifier = Modifier,
    rowCount:  Int      = 3,
    rowHeight: Dp       = 72.dp,
) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val alpha by infinite.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = Motion.shimmer),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier           = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        repeat(rowCount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(ShapeXl)
                    .background(baseColor.copy(alpha = alpha)),
            )
        }
    }
}
