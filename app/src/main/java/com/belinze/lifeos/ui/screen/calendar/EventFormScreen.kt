package com.belinze.lifeos.ui.screen.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventViewModel

// ─────────────────────────────────────────────────────────────────────────────
// EventFormScreen — matches EventFormScreen.tsx (delegates to TaskEventForm.tsx)
//
// Handles: event | birthday | anniversary | countdown types.
// Task creation is handled by TaskFormScreen (TaskViewModel).
//
// Fields driven by EventViewModel.formState:
//   title, description, date, endDate, allDay, type, repeatRule, location
// ─────────────────────────────────────────────────────────────────────────────

private val EVENT_TYPES = listOf("event", "birthday", "anniversary", "countdown")
private val REPEAT_OPTIONS = listOf("none", "daily", "weekly", "monthly", "yearly")

@Composable
fun EventFormScreen(
    eventId:       String?,
    type:          String   = "event",
    navController: NavHostController,
    viewModel:     EventViewModel = hiltViewModel(),
) {
    val form   by viewModel.formState.collectAsState()
    val isEdit = !eventId.isNullOrEmpty()

    var repeatModalOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.openForm(eventId?.ifEmpty { null }, type)
    }

    // ── Repeat modal ──────────────────────────────────────────────────────────
    if (repeatModalOpen) {
        AlertDialog(
            onDismissRequest = { repeatModalOpen = false },
            title   = { Text("Repeat") },
            text    = {
                Column {
                    REPEAT_OPTIONS.forEach { opt ->
                        val label = opt.replaceFirstChar { it.uppercase() }.let {
                            if (opt == "none") "Never" else it
                        }
                        Row(
                            modifier          = Modifier.fillMaxWidth().height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(label)
                            RadioButton(
                                selected = (form.repeatRule ?: "none") == opt,
                                onClick  = {
                                    viewModel.updateRepeatRule(if (opt == "none") null else opt)
                                    repeatModalOpen = false
                                },
                            )
                        }
                        if (opt != REPEAT_OPTIONS.last()) HorizontalDivider()
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { repeatModalOpen = false }) { Text("Cancel") } },
        )
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "New",
        title   = when (form.type) {
            "birthday"    -> "Birthday"
            "anniversary" -> "Anniversary"
            "countdown"   -> "Countdown"
            else          -> "Event"
        },
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {

            // ── Type chips ────────────────────────────────────────────────────
            Text("Type", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                EVENT_TYPES.forEach { t ->
                    FilterChip(
                        selected = form.type == t,
                        onClick  = { viewModel.updateType(t) },
                        label    = { Text(t.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // ── Title ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = form.title,
                onValueChange = { viewModel.updateTitle(it) },
                label         = {
                    Text(
                        when (form.type) {
                            "birthday"    -> "Person's name"
                            "anniversary" -> "Event name"
                            "countdown"   -> "Countdown name"
                            else          -> "Title"
                        }
                    )
                },
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // ── Description (hidden for single-date types) ────────────────────
            val isSingleDateType = form.type in listOf("birthday", "anniversary", "countdown")
            if (!isSingleDateType) {
                OutlinedTextField(
                    value         = form.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label         = { Text("Description (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2,
                    maxLines      = 4,
                )
            }

            // ── Date / time fields ────────────────────────────────────────────
            if (!isSingleDateType) {
                // All-day toggle
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("All day", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground)
                    Switch(
                        checked         = form.allDay,
                        onCheckedChange = { viewModel.updateAllDay(it) },
                    )
                }
            }

            // Start date
            OutlinedTextField(
                value         = form.date.take(10),
                onValueChange = { d -> viewModel.updateDate(d) },
                label         = { Text(if (isSingleDateType) "Date (YYYY-MM-DD)" else "From (YYYY-MM-DD)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // End date (events only)
            if (!isSingleDateType) {
                OutlinedTextField(
                    value         = form.endDate?.take(10) ?: "",
                    onValueChange = { v -> viewModel.updateEndDate(v.ifBlank { null }) },
                    label         = { Text("To (YYYY-MM-DD, optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                )
            }

            // Repeat button
            val repeatLabel = when (form.repeatRule) {
                null, "none" -> "Never"
                "daily"      -> "Daily"
                "weekly"     -> "Weekly"
                "monthly"    -> "Monthly"
                "yearly"     -> "Yearly"
                else         -> form.repeatRule!!.replaceFirstChar { it.uppercase() }
            }
            TextButton(onClick = { repeatModalOpen = true }) {
                Text("Repeat: $repeatLabel", color = MaterialTheme.colorScheme.primary)
            }

            // Location (events only)
            if (!isSingleDateType) {
                OutlinedTextField(
                    value         = form.location,
                    onValueChange = { viewModel.updateLocation(it) },
                    label         = { Text("Location (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                )
            }

            // ── Error ─────────────────────────────────────────────────────────
            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Save ─────────────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.saveForm { navController.popBackStack() } },
                enabled  = !form.isSaving && form.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update Event" else "Save Event")
                }
            }

            // Delete (edit mode only)
            if (isEdit && form.id != null) {
                TextButton(
                    onClick  = {
                        viewModel.softDelete(form.id!!)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Event", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
