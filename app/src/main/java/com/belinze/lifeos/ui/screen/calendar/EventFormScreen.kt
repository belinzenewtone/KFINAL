package com.belinze.lifeos.ui.screen.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventFormState
import com.belinze.lifeos.viewmodel.EventViewModel

// ─────────────────────────────────────────────────────────────────────────────
// EventFormScreen — 1:1 with TaskEventForm.tsx
//
// Types: task | event | birthday | anniversary | countdown
// ─────────────────────────────────────────────────────────────────────────────

private val EVENT_TYPES = listOf(
    "task"        to "Task",
    "event"       to "Event",
    "birthday"    to "Birthday",
    "anniversary" to "Anniversary",
    "countdown"   to "Countdown",
)

private val EVENT_CATEGORIES = listOf(
    "meeting", "task", "reminder", "goal", "other",
)

private val REPEAT_OPTIONS = listOf(
    "none"    to "No repeat",
    "daily"   to "Daily",
    "weekly"  to "Weekly",
    "monthly" to "Monthly",
    "yearly"  to "Yearly",
)

private val REMINDER_PRESETS = listOf(
    0     to "At time",
    10    to "10 min before",
    30    to "30 min before",
    60    to "1 hour before",
    1440  to "1 day before",
)

private val PRIORITY_OPTIONS = listOf(
    "low"    to "Neutral",
    "medium" to "Important",
    "high"   to "Urgent",
)

private val PRIORITY_COLORS = mapOf(
    "low"    to Color(0xFF7FC8F8),
    "medium" to Color(0xFFF5CB5C),
    "high"   to Color(0xFFF2B8B5),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    eventId:       String?,
    type:          String = "event",
    navController: NavHostController,
    viewModel:     EventViewModel = hiltViewModel(),
) {
    val form   by viewModel.formState.collectAsState()
    val isEdit  = !eventId.isNullOrEmpty()

    LaunchedEffect(eventId, type) {
        viewModel.openForm(eventId?.ifEmpty { null }, type)
    }

    // ─ Modal visibility ─
    var showStartDatePicker    by remember { mutableStateOf(false) }
    var showStartTimePicker    by remember { mutableStateOf(false) }
    var showEndDatePicker      by remember { mutableStateOf(false) }
    var showEndTimePicker      by remember { mutableStateOf(false) }
    var showRepeatDatePicker   by remember { mutableStateOf(false) }
    var showRepeatModal        by remember { mutableStateOf(false) }
    var showReminderModal      by remember { mutableStateOf(false) }
    var showTimezoneModal      by remember { mutableStateOf(false) }
    var showCdReminderTimePicker by remember { mutableStateOf(false) }
    var guestInput             by remember { mutableStateOf("") }

    // Custom reminder entry
    var customReminderValue by remember { mutableStateOf("") }
    var customReminderUnit  by remember { mutableStateOf("minutes") }

    // Timezone search
    var tzSearch by remember { mutableStateOf("") }

    val titleLabel = when (form.type) {
        "birthday"    -> "Person's name"
        "anniversary" -> "Anniversary name"
        "task"        -> "Title"
        else          -> "Event name"
    }

    PageScaffold(
        title      = if (isEdit) "Edit Event" else "New Event",
        onBack     = { navController.popBackStack() },
        scrollable = false,
        actions    = {
            if (isEdit) {
                IconButton(
                    onClick  = { viewModel.softDelete(form.id.orEmpty()); navController.popBackStack() },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Spacer(Modifier.height(Spacing.sm))

            // ─ Type ─
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
            ) {
                OutlinedTextField(
                    value         = EVENT_TYPES.find { it.first == form.type }?.second ?: form.type,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Type") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded         = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                ) {
                    EVENT_TYPES.forEach { (value, label) ->
                        DropdownMenuItem(
                            text    = { Text(label) },
                            onClick = { viewModel.updateType(value); typeExpanded = false },
                        )
                    }
                }
            }

            // ─ Title ─
            OutlinedTextField(
                value         = form.title,
                onValueChange = { viewModel.updateTitle(it) },
                label         = { Text(titleLabel) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            // ─ Description (hidden for birthday / anniversary / countdown) ─
            if (form.type !in setOf("birthday", "anniversary", "countdown")) {
                OutlinedTextField(
                    value         = form.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label         = { Text("Description") },
                    placeholder   = { Text("Add details…") },
                    minLines      = 2,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // TYPE-SPECIFIC SECTIONS
            // ═══════════════════════════════════════════════════════════════

            // ─ TASK ─
            if (form.type == "task") {
                SectionHeader("Date & Time")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DateFieldButton(
                        label    = "Date",
                        value    = form.startDateStr,
                        onClick  = { showStartDatePicker = true },
                        modifier = Modifier.weight(2f),
                    )
                    DateFieldButton(
                        label    = "Time",
                        value    = form.startTimeStr,
                        onClick  = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }

                SectionHeader("Priority")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    PRIORITY_OPTIONS.forEach { (value, label) ->
                        FilterChip(
                            selected = form.importance == value,
                            onClick  = { viewModel.updateImportance(value) },
                            label    = { Text(label) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = (PRIORITY_COLORS[value] ?: MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.25f),
                                selectedLabelColor     = PRIORITY_COLORS[value] ?: MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                ReminderRow(form, onOpenModal = { showReminderModal = true })
                AlarmRow(form.alarmEnabled, { viewModel.updateAlarmEnabled(it) })
            }

            // ─ EVENT ─
            if (form.type == "event") {
                // All-day toggle
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment    = Alignment.CenterVertically,
                ) {
                    Text("All day", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = form.allDay,
                        onCheckedChange = { viewModel.updateAllDay(it) },
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                    )
                }

                // From
                SectionHeader("From")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DateFieldButton("Date", form.startDateStr,
                        { showStartDatePicker = true }, Modifier.weight(2f))
                    if (!form.allDay) {
                        DateFieldButton("Time", form.startTimeStr,
                            { showStartTimePicker = true }, Modifier.weight(1f))
                    }
                }

                // To
                SectionHeader("To")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DateFieldButton(
                        label    = "Date",
                        value    = form.endDateStr ?: "",
                        placeholder = "Not set",
                        onClick  = { showEndDatePicker = true },
                        modifier = Modifier.weight(2f),
                    )
                    if (!form.allDay && form.endDateStr != null) {
                        DateFieldButton("Time", form.endTimeStr,
                            { showEndTimePicker = true }, Modifier.weight(1f))
                    }
                }
                if (form.endDateStr != null) {
                    TextButton(onClick = { viewModel.updateEndDate(null) }) {
                        Text("Clear end date", color = MaterialTheme.colorScheme.error)
                    }
                }

                // Repeat
                RepeatRow(form.repeatRule, { showRepeatModal = true })

                // Guests
                SectionHeader("Guests")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value         = guestInput,
                        onValueChange = { guestInput = it },
                        label         = { Text("Add guest email") },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.addGuest(guestInput.trim())
                            guestInput = ""
                        }),
                    )
                    IconButton(onClick = {
                        viewModel.addGuest(guestInput.trim())
                        guestInput = ""
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add guest")
                    }
                }
                if (form.guests.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        items(form.guests) { g ->
                            InputChip(
                                selected = false,
                                onClick  = {},
                                label    = { Text(g, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    IconButton(
                                        onClick  = { viewModel.removeGuest(g) },
                                        modifier = Modifier.size(18.dp),
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp))
                                    }
                                },
                            )
                        }
                    }
                }

                // Location
                OutlinedTextField(
                    value         = form.location,
                    onValueChange = { viewModel.updateLocation(it) },
                    label         = { Text("Location") },
                    placeholder   = { Text("Add location…") },
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp)) },
                    modifier      = Modifier.fillMaxWidth(),
                )

                // Timezone
                SectionHeader("Timezone")
                OutlinedButton(
                    onClick  = { showTimezoneModal = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(form.timeZoneId, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp))
                }

                // Category (kind)
                SectionHeader("Category")
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded         = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value         = form.kind.replaceFirstChar { it.uppercaseChar() },
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Category") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded         = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        EVENT_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text(cat.replaceFirstChar { it.uppercaseChar() }) },
                                onClick = { viewModel.updateKind(cat); categoryExpanded = false },
                            )
                        }
                    }
                }

                // Priority
                SectionHeader("Priority")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    PRIORITY_OPTIONS.forEach { (value, label) ->
                        FilterChip(
                            selected = form.importance == value,
                            onClick  = { viewModel.updateImportance(value) },
                            label    = { Text(label) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = (PRIORITY_COLORS[value] ?: MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.25f),
                                selectedLabelColor     = PRIORITY_COLORS[value] ?: MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                ReminderRow(form, onOpenModal = { showReminderModal = true })
                AlarmRow(form.alarmEnabled, { viewModel.updateAlarmEnabled(it) })
            }

            // ─ BIRTHDAY ─
            if (form.type == "birthday") {
                SectionHeader("Birthday Date")
                DateFieldButton("Date", form.startDateStr, { showStartDatePicker = true },
                    Modifier.fillMaxWidth())

                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment    = Alignment.CenterVertically,
                ) {
                    Text("Add year", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = form.addYear,
                        onCheckedChange = { viewModel.updateAddYear(it) },
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                    )
                }

                if (form.addYear) {
                    SectionHeader("Repeat until")
                    DateFieldButton("Date", form.repeatEndDate ?: "", { showRepeatDatePicker = true },
                        Modifier.fillMaxWidth(), "Not set")
                }
            }

            // ─ ANNIVERSARY ─
            if (form.type == "anniversary") {
                SectionHeader("Anniversary Date")
                DateFieldButton("Date", form.startDateStr, { showStartDatePicker = true },
                    Modifier.fillMaxWidth())

                SectionHeader("Repeat until")
                DateFieldButton("Date", form.repeatEndDate ?: "", { showRepeatDatePicker = true },
                    Modifier.fillMaxWidth(), "Not set")
            }

            // ─ COUNTDOWN ─
            if (form.type == "countdown") {
                SectionHeader("Countdown to Date")
                DateFieldButton("Date", form.startDateStr, { showStartDatePicker = true },
                    Modifier.fillMaxWidth())

                RepeatRow(form.repeatRule, { showRepeatModal = true })

                SectionHeader("Repeat until")
                DateFieldButton("Date", form.repeatEndDate ?: "", { showRepeatDatePicker = true },
                    Modifier.fillMaxWidth(), "Not set")

                SectionHeader("Remind at")
                DateFieldButton("Time", form.countdownReminderTime, { showCdReminderTimePicker = true },
                    Modifier.fillMaxWidth())

                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment    = Alignment.CenterVertically,
                ) {
                    Text("Remind 3 days before",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = form.remindBefore,
                        onCheckedChange = { viewModel.updateRemindBefore(it) },
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                    )
                }
            }

            // ─ Error ─
            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // ─ Save ─
            Button(
                onClick  = { viewModel.saveForm { navController.popBackStack() } },
                enabled  = !form.isSaving && form.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.xl),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODALS
    // ─────────────────────────────────────────────────────────────────────────

    // Start date picker
    if (showStartDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToMillis(form.startDateStr),
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        viewModel.updateStartDate(millisToIsoDate(ms))
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    // Start time picker
    if (showStartTimePicker) {
        val parts = form.startTimeStr.split(":")
        val state = rememberTimePickerState(
            initialHour   = parts.getOrNull(0)?.toIntOrNull() ?: 8,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour      = true,
        )
        TimePickerModal(
            state      = state,
            onDismiss  = { showStartTimePicker = false },
            onConfirm  = {
                viewModel.updateStartTime("%02d:%02d".format(state.hour, state.minute))
                showStartTimePicker = false
            },
        )
    }

    // End date picker
    if (showEndDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToMillis(form.endDateStr ?: form.startDateStr),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        viewModel.updateEndDate(millisToIsoDate(ms))
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    // End time picker
    if (showEndTimePicker) {
        val parts = form.endTimeStr.split(":")
        val state = rememberTimePickerState(
            initialHour   = parts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour      = true,
        )
        TimePickerModal(
            state      = state,
            onDismiss  = { showEndTimePicker = false },
            onConfirm  = {
                viewModel.updateEndTime("%02d:%02d".format(state.hour, state.minute))
                showEndTimePicker = false
            },
        )
    }

    // Repeat end date picker
    if (showRepeatDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToMillis(form.repeatEndDate ?: form.startDateStr),
        )
        DatePickerDialog(
            onDismissRequest = { showRepeatDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        viewModel.updateRepeatEndDate(millisToIsoDate(ms))
                    }
                    showRepeatDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRepeatDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    // Countdown remind-at time picker
    if (showCdReminderTimePicker) {
        val parts = form.countdownReminderTime.split(":")
        val state = rememberTimePickerState(
            initialHour   = parts.getOrNull(0)?.toIntOrNull() ?: 8,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour      = true,
        )
        TimePickerModal(
            state      = state,
            onDismiss  = { showCdReminderTimePicker = false },
            onConfirm  = {
                viewModel.updateCountdownReminderTime("%02d:%02d".format(state.hour, state.minute))
                showCdReminderTimePicker = false
            },
        )
    }

    // Repeat modal
    if (showRepeatModal) {
        AlertDialog(
            onDismissRequest = { showRepeatModal = false },
            title            = { Text("Repeat") },
            confirmButton    = { TextButton(onClick = { showRepeatModal = false }) { Text("Done") } },
            text = {
                Column {
                    REPEAT_OPTIONS.forEach { (value, label) ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = form.repeatRule == value,
                                onClick  = { viewModel.updateRepeatRule(value) },
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(label, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
        )
    }

    // Reminder modal
    if (showReminderModal) {
        AlertDialog(
            onDismissRequest = { showReminderModal = false },
            title            = { Text("Reminders") },
            confirmButton    = { TextButton(onClick = { showReminderModal = false }) { Text("Done") } },
            text = {
                Column {
                    Text("Quick presets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.sm))

                    REMINDER_PRESETS.forEach { (minutes, label) ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked         = form.reminderOffsets.contains(minutes),
                                onCheckedChange = { viewModel.toggleReminderOffset(minutes) },
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))
                    Text("Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedTextField(
                            value         = customReminderValue,
                            onValueChange = { customReminderValue = it.filter { c -> c.isDigit() } },
                            label         = { Text("Value") },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                        )
                        var expandedUnit by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded        = expandedUnit,
                            onExpandedChange = { expandedUnit = it },
                            modifier        = Modifier.weight(1f),
                        ) {
                            OutlinedTextField(
                                value         = customReminderUnit,
                                onValueChange = {},
                                readOnly      = true,
                                label         = { Text("Unit") },
                                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expandedUnit) },
                                modifier      = Modifier.menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded        = expandedUnit,
                                onDismissRequest = { expandedUnit = false },
                            ) {
                                listOf("minutes", "hours", "days").forEach { unit ->
                                    DropdownMenuItem(
                                        text    = { Text(unit) },
                                        onClick = { customReminderUnit = unit; expandedUnit = false },
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        val v = customReminderValue.toIntOrNull() ?: return@TextButton
                        val minutes = when (customReminderUnit) {
                            "hours" -> v * 60
                            "days"  -> v * 60 * 24
                            else    -> v
                        }
                        viewModel.toggleReminderOffset(minutes)
                        customReminderValue = ""
                    }) { Text("Add custom reminder") }
                }
            },
        )
    }

    // Timezone modal
    if (showTimezoneModal) {
        val allZones = remember {
            java.time.ZoneId.getAvailableZoneIds().sorted()
        }
        val filtered = if (tzSearch.isBlank()) allZones
                       else allZones.filter { it.contains(tzSearch, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showTimezoneModal = false; tzSearch = "" },
            title            = { Text("Select Timezone") },
            confirmButton    = { TextButton(onClick = { showTimezoneModal = false; tzSearch = "" }) { Text("Done") } },
            text = {
                Column {
                    OutlinedTextField(
                        value         = tzSearch,
                        onValueChange = { tzSearch = it },
                        label         = { Text("Search") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        leadingIcon   = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtered.take(100)) { tz ->
                            TextButton(
                                onClick  = { viewModel.updateTimeZoneId(tz); showTimezoneModal = false; tzSearch = "" },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier             = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment    = Alignment.CenterVertically,
                                ) {
                                    Text(tz,
                                        style    = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f))
                                    if (form.timeZoneId == tz) {
                                        Icon(Icons.Filled.Check, null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.bodyMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

@Composable
private fun DateFieldButton(
    label:       String,
    value:       String,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier,
    placeholder: String   = "",
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text  = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RepeatRow(repeatRule: String, onClick: () -> Unit) {
    val repeatLabel = REPEAT_OPTIONS.find { it.first == repeatRule }?.second ?: "No repeat"
    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically,
    ) {
        Text("Repeat", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onClick) {
            Text(repeatLabel)
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ReminderRow(form: EventFormState, onOpenModal: () -> Unit) {
    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically,
    ) {
        Column {
            Text("Reminders", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (form.reminderOffsets.isNotEmpty()) {
                Text(
                    text  = form.reminderOffsets.joinToString(", ") { minutesLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        TextButton(onClick = onOpenModal) {
            Text(if (form.reminderOffsets.isEmpty()) "Add" else "Edit")
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AlarmRow(alarmEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically,
    ) {
        Text("Alarm reminders", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(
            checked = alarmEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(
    state:     TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton    = { TextButton(onClick = onConfirm) { Text("OK") } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state)
            }
        },
    )
}

// ─── Date/time conversion helpers ────────────────────────────────────────────

private fun isoDateToMillis(dateStr: String): Long? {
    return try {
        java.time.LocalDate.parse(dateStr)
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) { null }
}

private fun millisToIsoDate(ms: Long): String {
    return java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
        .toString()   // "YYYY-MM-DD"
}

private fun minutesLabel(minutes: Int): String = when (minutes) {
    0              -> "At time"
    10             -> "10 min"
    30             -> "30 min"
    60             -> "1 hr"
    1440           -> "1 day"
    3 * 24 * 60    -> "3 days"
    else           -> if (minutes < 60) "${minutes}m"
                      else if (minutes < 1440) "${minutes / 60}h"
                      else "${minutes / 1440}d"
}
