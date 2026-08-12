package com.belinze.lifeos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// MetricCard
//
// 1:1 port of src/components/common/MetricCard.tsx.
//
// A FrostCard-wrapped metric tile with:
//  ‣ label      — 11sp/500 above the amount
//  ‣ amount     — headlineMedium (24sp/700)
//  ‣ subtext    — 12sp below the amount (optional)
//  ‣ icon       — optional leading icon tinted with tint colour
//  ‣ tintColor  — overrides icon colour (defaults to primary)
//  ‣ glow       — forwarded to FrostCard (Blue/Teal/None)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MetricCard(
    label:      String,
    amount:     String,
    subtext:    String?       = null,
    icon:       ImageVector?  = null,
    tintColor:  Color?        = null,
    glow:       FrostCardGlow = FrostCardGlow.None,
    onClick:    (() -> Unit)? = null,
    modifier:   Modifier      = Modifier,
) {
    val resolvedTint = tintColor ?: MaterialTheme.colorScheme.primary

    FrostCard(
        glow     = glow,
        modifier = modifier,
    ) {
        // Optional icon + label row
        if (icon != null) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = resolvedTint,
                    modifier           = Modifier.size(16.dp),
                )
                Text(
                    text       = label,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                )
            }
        } else {
            Text(
                text       = label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            )
        }

        Spacer(Modifier.height(Spacing.xs))

        // Amount — headlineMedium 24sp/700
        Text(
            text       = amount,
            style      = MaterialTheme.typography.headlineMedium,   // 24sp, weight 700
            color      = MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier.fillMaxWidth(),
        )

        // Optional subtext
        if (subtext != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text     = subtext,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            )
        }
    }
}
