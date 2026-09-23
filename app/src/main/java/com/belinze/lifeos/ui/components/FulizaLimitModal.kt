package com.belinze.lifeos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// FulizaLimitModal — 1:1 port of src/components/settings/FulizaLimitModal.tsx.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulizaLimitModal(
    visible:       Boolean,
    currentLimit:  Double,
    onSave:        (Double) -> Unit,
    onCancel:      () -> Unit,
) {
    if (!visible) return

    var value by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toLong().toString() else "") }
    var isSubmitting by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible, currentLimit) {
        value = if (currentLimit > 0) currentLimit.toLong().toString() else ""
        isSubmitting = false
        focusRequester.requestFocus()
    }

    fun handleSave() {
        if (isSubmitting) return
        isSubmitting = true
        onSave(value.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0)
    }

    fun handleCancel() {
        if (isSubmitting) return
        onCancel()
    }

    ModalBottomSheet(
        onDismissRequest = ::handleCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.base),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = ::handleCancel, enabled = !isSubmitting) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Fuliza Limit",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = ::handleSave, enabled = !isSubmitting) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg)
                    .padding(top = Spacing.xl),
            ) {
                Text(
                    text = "Enter your personal Fuliza limit in KES to improve debt tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xl),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() } },
                    enabled = !isSubmitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleSave() }),
                    placeholder = { Text("0") },
                    singleLine = true,
                    prefix = { Text("KSh ") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        }
    }
}
