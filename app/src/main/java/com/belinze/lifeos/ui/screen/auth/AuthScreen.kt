package com.belinze.lifeos.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// AuthScreen (Phase 3 stub)
//
// Phase 5 will implement the full auth flow matching AuthScreen.tsx:
// PIN setup, profile name entry, SMS permission grant, etc.
//
// For now, a single "Continue" button calls onAuthenticated().
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    modifier:        Modifier = Modifier,
) {
    val isDark  = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0A0B) else Color(0xFFE8EDF3)

    Column(
        modifier             = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(32.dp),
        verticalArrangement  = Arrangement.Center,
        horizontalAlignment  = Alignment.CenterHorizontally,
    ) {
        Text(
            text       = "Set Up Access",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = "Configure your PIN and biometric preferences.\n\nFull auth flow coming in Phase 5.",
            fontSize  = 15.sp,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick  = onAuthenticated,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text       = "Continue",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
