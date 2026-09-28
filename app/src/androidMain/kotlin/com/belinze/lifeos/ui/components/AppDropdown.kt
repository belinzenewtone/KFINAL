package com.belinze.lifeos.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.Shape2xl
import com.belinze.lifeos.ui.theme.ShapeLg
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// AppDropdownField + AppPickerSheet
//
// 1:1 port of src/components/common/Dropdown.tsx (which composes SwipeableSheet).
//
// The reference app does NOT use an anchored Material dropdown menu. It renders:
//   ‣ a boxed trigger — 1dp outlineVariant border, 20dp radius, a label line above
//     the value, the selected option's icon in its own colour, and a chevron;
//   ‣ a bottom sheet — 0.55 black scrim, 32dp top radius, a 40x4 grabber, a
//     titleLarge header with a Close button, and a scrollable option list where
//     the selected row shows a primary check and rows are separated by hairlines.
//
// Material3's ExposedDropdownMenuBox matches none of that, so finance pickers must
// use these two composables instead.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class PickerOption(
    val key:   String,
    val label: String,
    val icon:  ImageVector? = null,
    val tint:  Color        = Color.Unspecified,
)

/** The collapsed trigger field. Does not own its open/closed state. */
@Composable
fun AppDropdownField(
    label:       String,
    valueLabel:  String,
    onClick:     () -> Unit,
    modifier:    Modifier     = Modifier,
    leadingIcon: ImageVector? = null,
    leadingTint: Color        = Color.Unspecified,
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeLg)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeLg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onClick,
                )
                .padding(horizontal = Spacing.base, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint     = if (leadingTint == Color.Unspecified) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        leadingTint
                    },
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text     = valueLabel,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** The bottom-sheet option list. Renders nothing when [visible] is false. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    visible:     Boolean,
    title:       String,
    options:     List<PickerOption>,
    selectedKey: String?,
    onSelect:    (String) -> Unit,
    onDismiss:   () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surfaceVariant,
        scrimColor       = Color.Black.copy(alpha = 0.55f),
        shape            = Shape2xl,
        dragHandle       = { BottomSheetDefaults.DragHandle(width = 40.dp) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            Spacer(Modifier.height(Spacing.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEachIndexed { i, option ->
                    val selected = option.key == selectedKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                            ) {
                                onSelect(option.key)
                                onDismiss()
                            }
                            .padding(vertical = Spacing.base),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (option.icon != null) {
                            Icon(
                                option.icon,
                                contentDescription = null,
                                tint     = if (option.tint == Color.Unspecified) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    option.tint
                                },
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                        }
                        Text(
                            text     = option.label,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (i < options.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
