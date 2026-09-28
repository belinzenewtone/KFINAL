package com.belinze.lifeos.ui.screen.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// LoadingScreen
//
// Shown while DataStore is hydrating (first composable frame).
// Mirrors LoadingScreen.tsx: pulsing primary dot on dark background.
//
// Phase 8 will add the exact gradient/lottie from the RN version.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0A0B) else Color(0xFFE8EDF3)

    // Pulse animation — scale 0.8 ↔ 1.1 over 900ms, repeat reverse
    val infinite = rememberInfiniteTransition(label = "loading_pulse")
    val scale by infinite.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading_scale",
    )

    Box(
        modifier         = modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Pulsing primary circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text       = "LifeOS",
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
        }
    }
}
