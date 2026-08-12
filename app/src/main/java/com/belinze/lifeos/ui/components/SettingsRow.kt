package com.belinze.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// SettingsRow — 1:1 port of src/components/settings/SettingsRow.tsx.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsRow(
    icon:            ImageVector?  = null,
    iconColor:       Color?        = null,
    label:           String,
    subtitle:        String?       = null,
    value:           String?       = null,
    onPress:         (() -> Unit)? = null,
    toggle:          Boolean       = false,
    toggleValue:     Boolean       = false,
    onToggleChange:  ((Boolean) -> Unit)? = null,
    destructive:     Boolean       = false,
    showChevron:     Boolean       = false,
    disabled:        Boolean       = false,
    isLast:          Boolean       = false,
    modifier:        Modifier      = Modifier,
) {
    val resolvedIconColor = iconColor ?: MaterialTheme.colorScheme.primary
    val labelColor = if (destructive) MaterialTheme.colorScheme.error
                     else MaterialTheme.colorScheme.onSurface
    val interactionSource = remember { MutableInteractionSource() }

    val row = @Composable {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (disabled) Modifier
                    else if (onPress != null && !toggle) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = resolvedIconColor.copy(alpha = 0.2f)),
                        onClick = onPress,
                    ) else Modifier
                )
                .padding(vertical = Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                resolvedIconColor.copy(alpha = 0x20 / 255f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = resolvedIconColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = labelColor,
                        maxLines = 1,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (toggle) {
                    Switch(
                        checked = toggleValue,
                        onCheckedChange = onToggleChange ?: {},
                        enabled = !disabled,
                        colors = SwitchDefaults.colors(
                            // Explicit unchecked colors for both themes.
                            // Light: outline was #E2E8F0 (invisible on #E8EDF3 bg).
                            // Dark: surfaceVariant is #161618 (invisible on dark bg).
                            // onSurface.copy(0.12f) gives a visible subtle track in both.
                            uncheckedThumbColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                    )
                } else if (showChevron) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }

    row()
}
