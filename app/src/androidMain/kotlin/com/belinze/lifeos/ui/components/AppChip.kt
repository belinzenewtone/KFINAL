package com.belinze.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.ShapePill

// ─────────────────────────────────────────────────────────────────────────────
// AppChip
//
// RFINAL builds every chip from react-native-paper's `Chip` with per-call-site
// colours — it never uses Material's `FilterChip`. Three shapes of that pattern
// recur across the app:
//
//   Solid  — selected: accent fill + accent border + selectedContent label
//            default:  surfaceVariant fill + outlineVariant border + muted label
//            (Analytics date range, Export format, Learning category filter,
//             CsvImport column mapping)
//   Soft   — selected: accent @18% fill + accent @40% border + accent bold label
//            default:  surfaceVariant / outlineVariant / muted
//            (task + event type chips)
//   Tinted — always:   accent @9% fill + accent @31% border + accent label
//            (the Learning card "Continue"/"Start" chip)
//
// Material3's FilterChip has no border by default, uses an 8dp radius and an
// unrelated selected palette, so it reads as stock Material next to the rest of
// the app.
// ─────────────────────────────────────────────────────────────────────────────

enum class AppChipStyle { Solid, Soft, Tinted }

@Suppress("LongParameterList")
@Composable
fun AppChip(
    label:             String,
    modifier:          Modifier      = Modifier,
    selected:          Boolean       = false,
    onClick:           (() -> Unit)? = null,
    leadingIcon:       ImageVector?  = null,
    enabled:           Boolean       = true,
    pill:              Boolean       = true,
    style:             AppChipStyle  = AppChipStyle.Solid,
    accent:            Color         = MaterialTheme.colorScheme.primary,
    selectedContent:   Color         = MaterialTheme.colorScheme.onPrimary,
    unselectedContent: Color         = MaterialTheme.colorScheme.onSurfaceVariant,
    contentLabel:      TextStyle     = MaterialTheme.typography.labelLarge,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val container: Color
    val border: Color
    val content: Color
    var weight = FontWeight.Medium

    when (style) {
        AppChipStyle.Tinted -> {
            container = accent.copy(alpha = 0.09f)
            border    = accent.copy(alpha = 0.31f)
            content   = accent
        }
        AppChipStyle.Soft -> {
            container = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
            border    = if (selected) accent.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outlineVariant
            content   = if (selected) accent else muted
            if (selected) weight = FontWeight.Bold
        }
        AppChipStyle.Solid -> {
            container = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant
            border    = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
            content   = if (selected) selectedContent else unselectedContent
        }
    }

    val shape = if (pill) ShapePill else RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .border(1.dp, border, shape)
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .height(32.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint     = if (style == AppChipStyle.Solid && !selected) muted else content,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text       = label,
            style      = contentLabel,
            color      = content,
            fontWeight = weight,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
    }
}
