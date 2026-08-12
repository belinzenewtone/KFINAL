package com.belinze.lifeos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// SectionHeader
//
// 1:1 port of src/components/common/SectionHeader.tsx.
//
// Spec:
//  ‣ Label: 12sp, weight 600, uppercase letter-spacing 0.5sp, onBackground.copy(0.55)
//  ‣ Action: 12sp, weight 500, primary colour
//  ‣ Row layout: label left, action right, vertical padding 4dp top + 8dp bottom
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    label:      String,
    action:     String?      = null,
    onAction:   (() -> Unit)? = null,
    modifier:   Modifier     = Modifier,
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs, bottom = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text          = label.uppercase(),
            fontSize      = 12.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            letterSpacing = 0.5.sp,
        )

        if (action != null && onAction != null) {
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text       = action,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.primary,
                modifier   = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication        = null,
                    onClick           = onAction,
                ),
            )
        }
    }
}
