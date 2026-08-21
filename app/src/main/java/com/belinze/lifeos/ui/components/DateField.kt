package com.belinze.lifeos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// DateField
//
// Reusable outlined button that mimics a date/time text field.
// Tapping it triggers an external picker (DatePickerDialog / TimePickerDialog).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DateField(
    label:       String,
    value:       String,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier,
    placeholder: String   = "Pick a date",
) {
    OutlinedButton(
        onClick        = onClick,
        modifier       = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text       = value.ifBlank { placeholder },
                style      = MaterialTheme.typography.bodyMedium,
                color      = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Medium,
            )
        }
    }
}
