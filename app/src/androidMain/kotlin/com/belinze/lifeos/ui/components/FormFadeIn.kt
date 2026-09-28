package com.belinze.lifeos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

// ─────────────────────────────────────────────────────────────────────────────
// Form fade-in — 1:1 port of src/hooks/useFormFadeIn.ts.
//
// Fades + slides the form content in (opacity 0→1, translateY 8→0, 120 ms)
// once [ready] is true, so fields don't flash empty before edit data loads.
// For new records (ready = true immediately) the fade-in fires on mount.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberFormFadeIn(ready: Boolean = true): Modifier {
    val progress by animateFloatAsState(
        targetValue  = if (ready) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label        = "form_fade_in",
    )
    return Modifier.graphicsLayer {
        alpha        = progress
        translationY = (1f - progress) * 8f
    }
}
