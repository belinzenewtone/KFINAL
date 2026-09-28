package com.belinze.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.LocalDarkTheme
import com.belinze.lifeos.ui.theme.ShapePill

// ─────────────────────────────────────────────────────────────────────────────
// AppSegmentedControl
//
// 1:1 port of src/components/common/SegmentedControl.tsx.
//
// The reference app does NOT use Material's segmented buttons. It renders one
// bordered pill track (radius full, 3dp inset, 1dp border) holding equal-width
// segments, where the active segment gets a primary-tinted fill AND a
// primary-tinted border, with a primary bold label.
//
// Material3's SingleChoiceSegmentedButtonRow has no shared track, no track
// border, uses secondaryContainer for the active segment, and a different label
// weight — so it reads as stock Material next to the rest of the app.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class SegmentOption(
    val key:   String,
    val label: String,
    val icon:  ImageVector? = null,
)

@Composable
fun AppSegmentedControl(
    options:     List<SegmentOption>,
    selectedKey: String,
    onSelect:    (String) -> Unit,
    modifier:    Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current

    // SegmentedControl.tsx:28-33
    val trackBg     = if (isDark) Color(0xFF0C0F1C) else Color(0xFFE2E8F0)
    val trackBorder = if (isDark) Color(0xFF2A2E42) else Color(0xFFCBD5E1)
    val activeBg     = if (isDark) Color(0x2E57B9FF) else Color(0x1F0369A1)  // 0.18 / 0.12
    val activeBorder = if (isDark) Color(0x6657B9FF) else Color(0x590369A1)  // 0.40 / 0.35

    Row(
        modifier = modifier
            .clip(ShapePill)
            .background(trackBg)
            .border(1.dp, trackBorder, ShapePill)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val active = option.key == selectedKey
            val activeModifier = if (active) {
                Modifier.background(activeBg).border(1.dp, activeBorder, ShapePill)
            } else {
                Modifier.border(1.dp, Color.Transparent, ShapePill)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapePill)
                    .then(activeModifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { onSelect(option.key) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                if (option.icon != null) {
                    Icon(
                        option.icon,
                        contentDescription = null,
                        tint     = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text       = option.label,
                    style      = MaterialTheme.typography.labelMedium,
                    color      = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines   = 1,
                )
            }
        }
    }
}
