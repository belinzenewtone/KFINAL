package com.belinze.lifeos.ui.screen.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventFormState
import com.belinze.lifeos.viewmodel.EventViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// EventFormScreen — redesigned to match reference CalendarAddScreen
//
// Navigation: AnimatedContent sub-pages (FORM | REPEAT | REMINDERS | TIMEZONE)
// Visual: FormCard / FormPickerRow / FormNavRow / FormToggleRow primitives
// ─────────────────────────────────────────────────────────────────────────────

private enum class AddPage { FORM, REPEAT, REMINDERS, TIMEZONE }

private val EVENT_TYPES = listOf(
    "task"        to "Task",
    "event"       to "Event",
    "birthday"    to "Birthday",
    "anniversary" to "Anniversary",
    "countdown"   to "Countdown",
)

private val REPEAT_OPTIONS = listOf(
    "none"    to "Never",
    "daily"   to "Daily",
    "weekly"  to "Weekly",
    "monthly" to "Monthly",
    "yearly"  to "Yearly",
)

private val REMINDER_PRESETS = listOf(
    0     to "When event starts",
    5     to "5 minutes before",
    10    to "10 minutes before",
    15    to "15 minutes before",
    30    to "30 minutes before",
    60    to "1 hour before",
    1440  to "1 day before",
    2880  to "2 days before",
    10080 to "1 week before",
)

private val PRIORITY_OPTIONS = listOf(
    "low"    to "Neutral",
    "medium" to "Important",
    "high"   to "Urgent",
)

private val PRIORITY_COLORS = mapOf(
    "low"    to Color(0xFF4B9EF4),
    "medium" to Color(0xFFF5CB5C),
    "high"   to Color(0xFFE57373),
)

private val EVENT_CATEGORIES = listOf(
    "meeting" to "Work",
    "task"    to "Personal",
    "goal"    to "Health",
    "other"   to "Finance",
    "reminder" to "Other",
)

private val CUSTOM_UNITS = listOf("minute", "hour", "day")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    eventId:       String?,
    type:          String = "event",
    navController: NavHostController,
    viewModel:     EventViewModel = hiltViewModel(),
) {
    val form  by viewModel.formState.collectAsStateWithLifecycle()
    val isEdit = !eventId.isNullOrEmpty()
    val scope  = rememberCoroutineScope()

    // Fade-in on load
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue   = if (contentVisible) 1f else 0f,
        animationSpec = tween(300),
        label         = "contentAlpha",
    )

    LaunchedEffect(eventId, type) {
        viewModel.openForm(eventId?.ifEmpty { null }, type)
    }

    // Sub-page state
    var currentPage by remember { mutableStateOf(AddPage.FORM) }
    if (currentPage != AddPage.FORM) { BackHandler { currentPage = AddPage.FORM } }

    // Picker modal state
    var showStartDatePicker    by remember { mutableStateOf(false) }
    var showStartTimePicker    by remember { mutableStateOf(false) }
    var showEndDatePicker      by remember { mutableStateOf(false) }
    var showEndTimePicker      by remember { mutableStateOf(false) }
    var showRepeatDatePicker   by remember { mutableStateOf(false) }
    var showCdTimePicker       by remember { mutableStateOf(false) }
    var showDeleteConfirm      by remember { mutableStateOf(false) }
    var guestInput             by remember { mutableStateOf("") }
    var tzSearch               by remember { mutableStateOf("") }
    var successMsg             by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == AddPage.FORM) {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { -it / 4 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 4 })
                } else {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 4 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { -it / 4 })
                }
            },
            label = "addPage",
            modifier = Modifier.alpha(contentAlpha),
        ) { page ->
            when (page) {
                AddPage.FORM ->
                    FormPage(
                        form               = form,
                        isEdit             = isEdit,
                        viewModel          = viewModel,
                        guestInput         = guestInput,
                        onGuestInputChange = { guestInput = it },
                        onDismiss          = { navController.popBackStack() },
                        onDeleteConfirm    = { showDeleteConfirm = true },
                        onOpenStartDate    = { showStartDatePicker = true },
                        onOpenStartTime    = { showStartTimePicker = true },
                        onOpenEndDate      = { showEndDatePicker = true },
                        onOpenEndTime      = { showEndTimePicker = true },
                        onOpenRepeat       = { currentPage = AddPage.REPEAT },
                        onOpenReminders    = { currentPage = AddPage.REMINDERS },
                        onOpenTimezone     = { currentPage = AddPage.TIMEZONE },
                        onOpenRepeatEnd    = { showRepeatDatePicker = true },
                        onOpenCdTime       = { showCdTimePicker = true },
                        onSave = {
                            viewModel.saveForm {
                                scope.launch {
                                    successMsg = if (isEdit) "${typeLabel(form.type)} updated" else "${typeLabel(form.type)} added"
                                    delay(1200)
                                    navController.popBackStack()
                                }
                            }
                        },
                    )

                AddPage.REPEAT ->
                    RepeatPickerPage(
                        selected = form.repeatRule,
                        onSelect = { viewModel.updateRepeatRule(it); currentPage = AddPage.FORM },
                        onBack   = { currentPage = AddPage.FORM },
                    )

                AddPage.REMINDERS ->
                    RemindersPickerPage(
                        form      = form,
                        viewModel = viewModel,
                        onBack    = { currentPage = AddPage.FORM },
                    )

                AddPage.TIMEZONE ->
                    TimezonePickerPage(
                        selected      = form.timeZoneId,
                        query         = tzSearch,
                        onQueryChange = { tzSearch = it },
                        onSelect      = { viewModel.updateTimeZoneId(it); tzSearch = ""; currentPage = AddPage.FORM },
                        onBack        = { tzSearch = ""; currentPage = AddPage.FORM },
                    )
            }
        }

        TopBanner(
            visible   = successMsg != null,
            message   = successMsg ?: "",
            tone      = BannerTone.Success,
            onDismiss = { successMsg = null },
        )
    }

    // ── Material3 date/time pickers ───────────────────────────────────────────

    if (showStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoDateToMillis(form.startDateStr))
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.updateStartDate(millisToIsoDate(it)) }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showStartTimePicker) {
        val parts = form.startTimeStr.split(":")
        val state = rememberTimePickerState(parts.getOrNull(0)?.toIntOrNull() ?: 8, parts.getOrNull(1)?.toIntOrNull() ?: 0, false)
        TimePickerModal(state, { showStartTimePicker = false }) {
            viewModel.updateStartTime("%02d:%02d".format(state.hour, state.minute))
            showStartTimePicker = false
        }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoDateToMillis(form.endDateStr ?: form.startDateStr))
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.updateEndDate(millisToIsoDate(it)) }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showEndTimePicker) {
        val parts = form.endTimeStr.split(":")
        val state = rememberTimePickerState(parts.getOrNull(0)?.toIntOrNull() ?: 9, parts.getOrNull(1)?.toIntOrNull() ?: 0, false)
        TimePickerModal(state, { showEndTimePicker = false }) {
            viewModel.updateEndTime("%02d:%02d".format(state.hour, state.minute))
            showEndTimePicker = false
        }
    }

    if (showRepeatDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoDateToMillis(form.repeatEndDate ?: form.startDateStr))
        DatePickerDialog(
            onDismissRequest = { showRepeatDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.updateRepeatEndDate(millisToIsoDate(it)) }
                    showRepeatDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showRepeatDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showCdTimePicker) {
        val parts = form.countdownReminderTime.split(":")
        val state = rememberTimePickerState(parts.getOrNull(0)?.toIntOrNull() ?: 8, parts.getOrNull(1)?.toIntOrNull() ?: 0, false)
        TimePickerModal(state, { showCdTimePicker = false }) {
            viewModel.updateCountdownReminderTime("%02d:%02d".format(state.hour, state.minute))
            showCdTimePicker = false
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete event?") },
            text             = { Text("This event will be permanently removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    form.id?.takeIf { it.isNotEmpty() }?.let { viewModel.softDelete(it) }
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FormPage — the main form
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormPage(
    form:               EventFormState,
    isEdit:             Boolean,
    viewModel:          EventViewModel,
    guestInput:         String,
    onGuestInputChange: (String) -> Unit,
    onDismiss:          () -> Unit,
    onDeleteConfirm:    () -> Unit,
    onOpenStartDate:    () -> Unit,
    onOpenStartTime:    () -> Unit,
    onOpenEndDate:      () -> Unit,
    onOpenEndTime:      () -> Unit,
    onOpenRepeat:       () -> Unit,
    onOpenReminders:    () -> Unit,
    onOpenTimezone:     () -> Unit,
    onOpenRepeatEnd:    () -> Unit,
    onOpenCdTime:       () -> Unit,
    onSave:             () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text      = if (isEdit) "Edit ${typeLabel(form.type)}" else "New ${typeLabel(form.type)}",
                style     = MaterialTheme.typography.titleMedium,
                color     = MaterialTheme.colorScheme.onSurface,
                modifier  = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (isEdit) {
                IconButton(onClick = onDeleteConfirm) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
            TextButton(onClick = onSave, enabled = !form.isSaving && form.title.isNotBlank()) {
                if (form.isSaving) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Tab strip ─────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EVENT_TYPES.forEach { (value, label) ->
                val selected = form.type == value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .clickable { viewModel.updateType(value) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.labelMedium,
                        color      = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(4.dp))

        // ── Form content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (form.type) {
                "task" -> TaskFormContent(
                    form            = form,
                    viewModel       = viewModel,
                    onOpenStartDate = onOpenStartDate,
                    onOpenStartTime = onOpenStartTime,
                    onOpenReminders = onOpenReminders,
                )
                "event" -> EventFormContent(
                    form            = form,
                    viewModel       = viewModel,
                    guestInput      = guestInput,
                    onGuestInput    = onGuestInputChange,
                    onOpenStartDate = onOpenStartDate,
                    onOpenStartTime = onOpenStartTime,
                    onOpenEndDate   = onOpenEndDate,
                    onOpenEndTime   = onOpenEndTime,
                    onOpenRepeat    = onOpenRepeat,
                    onOpenReminders = onOpenReminders,
                    onOpenTimezone  = onOpenTimezone,
                )
                "birthday", "anniversary" -> BirthdayFormContent(
                    form            = form,
                    isAnniversary   = form.type == "anniversary",
                    viewModel       = viewModel,
                    onOpenDate      = onOpenStartDate,
                    onOpenRepeatEnd = onOpenRepeatEnd,
                    onOpenReminders = onOpenReminders,
                )
                "countdown" -> CountdownFormContent(
                    form         = form,
                    viewModel    = viewModel,
                    onOpenDate   = onOpenStartDate,
                    onOpenRepeat = onOpenRepeat,
                    onOpenTime   = onOpenCdTime,
                    onOpenRepeatEnd = onOpenRepeatEnd,
                )
            }

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Type-specific form content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TaskFormContent(
    form:            EventFormState,
    viewModel:       EventViewModel,
    onOpenStartDate: () -> Unit,
    onOpenStartTime: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    FormTextField(
        value         = form.title,
        onValueChange = { viewModel.updateTitle(it) },
        label         = "Task title",
    )
    FormTextField(
        value         = form.description,
        onValueChange = { viewModel.updateDescription(it) },
        label         = "Description (optional)",
        maxLines      = 3,
    )

    // Priority
    FormSectionLabel("Priority")
    PriorityRow(selected = form.importance) { viewModel.updateImportance(it) }

    // Deadline
    FormSectionLabel("Deadline")
    FormCard {
        FormPickerRow(
            icon    = Icons.Outlined.CalendarMonth,
            label   = "Date",
            value   = formatDisplayDate(form.startDateStr),
            onClick = onOpenStartDate,
        )
        HorizontalDivider(Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        FormPickerRow(
            icon    = Icons.Outlined.AccessTime,
            label   = "Time",
            value   = formatDisplayTime(form.startTimeStr),
            onClick = onOpenStartTime,
        )
    }

    // Reminders
    val remindersLabel = if (form.reminderOffsets.isEmpty()) {
        "None"
    } else {
        "${form.reminderOffsets.size} reminder${if (form.reminderOffsets.size > 1) "s" else ""}"
    }
    FormNavRow(
        icon    = Icons.Outlined.Notifications,
        label   = "Reminders",
        value   = remindersLabel,
        onClick = onOpenReminders,
    )

    FormToggleRow(
        icon           = Icons.Outlined.NotificationsActive,
        label          = "Alarm reminders",
        checked        = form.alarmEnabled,
        onCheckedChange = { viewModel.updateAlarmEnabled(it) },
    )
}

@Composable
private fun EventFormContent(
    form:            EventFormState,
    viewModel:       EventViewModel,
    guestInput:      String,
    onGuestInput:    (String) -> Unit,
    onOpenStartDate: () -> Unit,
    onOpenStartTime: () -> Unit,
    onOpenEndDate:   () -> Unit,
    onOpenEndTime:   () -> Unit,
    onOpenRepeat:    () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenTimezone:  () -> Unit,
) {
    FormTextField(
        value         = form.title,
        onValueChange = { viewModel.updateTitle(it) },
        label         = "Event title",
    )

    // All day toggle
    FormToggleRow(
        label           = "All day",
        checked         = form.allDay,
        onCheckedChange = { viewModel.updateAllDay(it) },
    )

    // When block
    FormSectionLabel("When")
    FormCard {
        FormPickerRow(
            icon    = Icons.Outlined.CalendarMonth,
            label   = "From",
            value   = formatDisplayDate(form.startDateStr, if (form.allDay) "EEE, MMM d" else "EEE, MMM d  ") +
                      if (!form.allDay) formatDisplayTime(form.startTimeStr) else "",
            onClick = onOpenStartDate,
        )
        if (!form.allDay) {
            HorizontalDivider(Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            FormPickerRow(
                icon    = Icons.Outlined.AccessTime,
                label   = "Start time",
                value   = formatDisplayTime(form.startTimeStr),
                onClick = onOpenStartTime,
            )
        }
        HorizontalDivider(Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        FormPickerRow(
            icon    = Icons.Outlined.CalendarMonth,
            label   = "To",
            value   = form.endDateStr?.let {
                formatDisplayDate(it, if (form.allDay) "EEE, MMM d" else "EEE, MMM d  ") +
                    if (!form.allDay) formatDisplayTime(form.endTimeStr) else ""
            } ?: "Set end date",
            onClick = onOpenEndDate,
        )
        if (!form.allDay && form.endDateStr != null) {
            HorizontalDivider(Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            FormPickerRow(
                icon    = Icons.Outlined.AccessTime,
                label   = "End time",
                value   = formatDisplayTime(form.endTimeStr),
                onClick = onOpenEndTime,
            )
        }
        if (form.endDateStr != null) {
            HorizontalDivider(Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            TextButton(
                onClick  = { viewModel.updateEndDate(null) },
                modifier = Modifier.padding(start = 36.dp),
            ) {
                Text("Clear end date", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    // Repeat
    val repeatLabel = REPEAT_OPTIONS.find { it.first == form.repeatRule }?.second ?: "Never"
    FormNavRow(Icons.Outlined.Repeat, "Repeat", repeatLabel, onOpenRepeat)

    // Reminders
    val remindersLabel = if (form.reminderOffsets.isEmpty()) {
        "None"
    } else {
        "${form.reminderOffsets.size} reminder${if (form.reminderOffsets.size > 1) "s" else ""}"
    }
    FormNavRow(Icons.Outlined.Notifications, "Reminders", remindersLabel, onOpenReminders)

    // Alarm reminders
    FormToggleRow(
        icon            = Icons.Outlined.NotificationsActive,
        label           = "Alarm reminders",
        checked         = form.alarmEnabled,
        onCheckedChange = { viewModel.updateAlarmEnabled(it) },
    )

    // Guests
    GuestsSection(
        guests          = form.guests,
        inputText       = guestInput,
        onInputChange   = onGuestInput,
        onAdd           = { viewModel.addGuest(guestInput.trim()); onGuestInput("") },
        onRemove        = { viewModel.removeGuest(it) },
    )

    // Location
    FormTextField(
        value         = form.location,
        onValueChange = { viewModel.updateLocation(it) },
        label         = "Location",
        icon          = Icons.Outlined.LocationOn,
    )

    // Time zone (hidden for all-day)
    if (!form.allDay) {
        FormCard {
            FormPickerRow(
                icon    = Icons.Outlined.Public,
                label   = "Time zone",
                value   = form.timeZoneId.ifBlank { java.util.TimeZone.getDefault().id },
                onClick = onOpenTimezone,
            )
        }
    }

    // Category
    FormSectionLabel("Category")
    Row(
        modifier              = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EVENT_CATEGORIES.forEach { (value, label) ->
            val selected = form.kind == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .clickable { viewModel.updateKind(value) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }

    // Priority
    FormSectionLabel("Priority")
    PriorityRow(selected = form.importance) { viewModel.updateImportance(it) }

    // Description (at the bottom, matching reference)
    FormTextField(
        value         = form.description,
        onValueChange = { viewModel.updateDescription(it) },
        label         = "Description (optional)",
        maxLines      = 4,
    )
}

@Composable
private fun BirthdayFormContent(
    form:            EventFormState,
    isAnniversary:   Boolean,
    viewModel:       EventViewModel,
    onOpenDate:      () -> Unit,
    onOpenRepeatEnd: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    val nameLabel = if (isAnniversary) "Anniversary name" else "Person's name"
    val dateLabel = if (isAnniversary) "Date" else "Birthday"

    FormTextField(
        value         = form.title,
        onValueChange = { viewModel.updateTitle(it) },
        label         = nameLabel,
    )

    FormSectionLabel(dateLabel)
    FormCard {
        FormPickerRow(
            icon    = Icons.Outlined.CalendarMonth,
            label   = if (!isAnniversary && !form.addYear) "Month and day" else "Date",
            value   = formatDisplayDate(
                form.startDateStr,
                if (isAnniversary || form.addYear) "MMMM d, yyyy" else "MMMM d",
            ),
            onClick = onOpenDate,
        )
    }

    if (!isAnniversary) {
        FormToggleRow(
            label           = "Add year",
            checked         = form.addYear,
            onCheckedChange = { viewModel.updateAddYear(it) },
        )
    }

    // "Repeat until" only makes sense for Birthday after the user enables "Add year".
    // Anniversary inherently repeats yearly but doesn't expose an end-date row in the reference UI.
    if (!isAnniversary && form.addYear) {
        FormNavRow(
            icon    = Icons.Outlined.CalendarMonth,
            label   = "Repeat until",
            value   = form.repeatEndDate?.let { formatDisplayDate(it, "MMM d, yyyy") } ?: "Not set",
            onClick = onOpenRepeatEnd,
        )
    }

    val remindersLabel = if (form.reminderOffsets.isEmpty()) {
        "None"
    } else {
        "${form.reminderOffsets.size} reminder${if (form.reminderOffsets.size > 1) "s" else ""}"
    }
    FormNavRow(Icons.Outlined.Notifications, "Reminders", remindersLabel, onOpenReminders)

    FormToggleRow(
        icon            = Icons.Outlined.NotificationsActive,
        label           = "Alarm reminders",
        checked         = form.alarmEnabled,
        onCheckedChange = { viewModel.updateAlarmEnabled(it) },
    )
}

@Composable
private fun CountdownFormContent(
    form:         EventFormState,
    viewModel:    EventViewModel,
    onOpenDate:   () -> Unit,
    onOpenRepeat: () -> Unit,
    onOpenTime:   () -> Unit,
    onOpenRepeatEnd: () -> Unit,
) {
    FormTextField(
        value         = form.title,
        onValueChange = { viewModel.updateTitle(it) },
        label         = "Event name",
    )

    FormSectionLabel("When")
    FormPickerRow(
        icon    = Icons.Outlined.CalendarMonth,
        label   = "Date",
        value   = formatDisplayDate(form.startDateStr, "EEE, MMM d, yyyy"),
        onClick = onOpenDate,
        card    = true,
    )

    val repeatLabel = REPEAT_OPTIONS.find { it.first == form.repeatRule }?.second ?: "Never"
    FormNavRow(Icons.Outlined.Repeat, "Repeat", repeatLabel, onOpenRepeat)

    if (form.repeatRule != "none") {
        FormNavRow(
            icon    = Icons.Outlined.CalendarMonth,
            label   = "Repeat until",
            value   = form.repeatEndDate?.let { formatDisplayDate(it, "MMM d, yyyy") } ?: "Not set",
            onClick = onOpenRepeatEnd,
        )
    }

    FormSectionLabel("Remind me at")
    FormPickerRow(
        icon    = Icons.Outlined.AccessTime,
        label   = "Time",
        value   = form.countdownReminderTime,
        onClick = onOpenTime,
        card    = true,
    )

    FormToggleRow(
        label           = "Remind 3 days before",
        checked         = form.remindBefore,
        onCheckedChange = { viewModel.updateRemindBefore(it) },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-pages: Repeat / Reminders / Timezone
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RepeatPickerPage(
    selected: String,
    onSelect: (String) -> Unit,
    onBack:   () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubPageTopBar("Repeat", onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            REPEAT_OPTIONS.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(value) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (value == selected) {
                        Icon(Icons.Outlined.Check, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun RemindersPickerPage(
    form:      EventFormState,
    viewModel: EventViewModel,
    onBack:    () -> Unit,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SubPageTopBar("Reminders", onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(4.dp))

            REMINDER_PRESETS.forEach { (minutes, label) ->
                val selected = form.reminderOffsets.contains(minutes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleReminderOffset(minutes) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (selected) {
                        Icon(Icons.Outlined.Check, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            // Custom offsets not in presets
            val presetMinutes = REMINDER_PRESETS.map { it.first }.toSet()
            form.reminderOffsets.filter { it !in presetMinutes }.forEach { offset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleReminderOffset(offset) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(minutesLabel(offset), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.Outlined.Check, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            // Custom row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomDialog = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("Custom", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Outlined.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showCustomDialog) {
        CustomReminderDialog(
            onConfirm = { minutes -> viewModel.toggleReminderOffset(minutes); showCustomDialog = false },
            onDismiss = { showCustomDialog = false },
        )
    }
}

@Composable
private fun TimezonePickerPage(
    selected:      String,
    query:         String,
    onQueryChange: (String) -> Unit,
    onSelect:      (String) -> Unit,
    onBack:        () -> Unit,
) {
    val allZones = remember { java.util.TimeZone.getAvailableIDs().sorted() }
    val filtered = remember(query) {
        if (query.isBlank()) allZones else allZones.filter { it.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubPageTopBar("Time zone", onBack)
        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            placeholder   = { Text("Search time zones") },
            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine    = true,
            shape         = RoundedCornerShape(10.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered.size) { idx ->
                val tz         = filtered[idx]
                val isSelected = tz == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(tz) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = tz,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(Icons.Outlined.Check, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable UI primitives
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubPageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun FormTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    maxLines:      Int          = 1,
    icon:          ImageVector? = null,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        maxLines      = maxLines,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        leadingIcon   = if (icon != null) {
            ({
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
        })
        } else {
            null
        },
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor  = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor   = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        content()
    }
}

@Composable
private fun FormPickerRow(
    icon:    ImageVector,
    label:   String,
    value:   String,
    onClick: () -> Unit,
    card:    Boolean = false,
) {
    val modifier = if (card) {
        Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FormNavRow(
    icon:    ImageVector,
    label:   String,
    value:   String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Outlined.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FormToggleRow(
    label:           String,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon:            ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor    = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor  = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun PriorityRow(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PRIORITY_OPTIONS.forEach { (value, label) ->
            val isSelected = value == selected
            val color      = PRIORITY_COLORS[value] ?: MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) color else color.copy(alpha = 0.15f))
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSelected) Color.White else color,
                )
            }
        }
    }
}

@Composable
private fun GuestsSection(
    guests:        List<String>,
    inputText:     String,
    onInputChange: (String) -> Unit,
    onAdd:         () -> Unit,
    onRemove:      (String) -> Unit,
) {
    FormSectionLabel("Guests")
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value         = inputText,
            onValueChange = onInputChange,
            label         = { Text("Add guest (name or email)") },
            singleLine    = true,
            modifier      = Modifier.weight(1f),
            shape         = RoundedCornerShape(10.dp),
            leadingIcon   = {
                Icon(Icons.Outlined.People, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onAdd() }),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor  = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        val canAdd = inputText.isNotBlank()
        IconButton(
            onClick  = onAdd,
            enabled  = canAdd,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                .background(
                    if (canAdd) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                ),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add guest",
                tint = if (canAdd) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp))
        }
    }

    if (guests.isNotEmpty()) {
        Row(
            modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            guests.forEach { guest ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(guest, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Box(
                            modifier = Modifier.size(18.dp).clip(CircleShape)
                                .clickable { onRemove(guest) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove $guest",
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Custom reminder wheel dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomReminderDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var unitIndex  by remember { mutableStateOf(0) }
    val unit        = CUSTOM_UNITS[unitIndex]
    val valueItems  = remember(unit) {
        when (unit) {
            "hour" -> (1..23).map { it.toString() }
            "day"  -> (1..30).map { it.toString() }
            else   -> (1..59).map { it.toString() }
        }
    }
    var valueIndex by remember(unit) { mutableStateOf(0) }
    val currentValue = valueItems.getOrNull(valueIndex)?.toIntOrNull() ?: 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Custom reminder", style = MaterialTheme.typography.titleMedium) },
        text = {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                WheelPicker(valueItems, valueIndex, { valueIndex = it }, Modifier.width(80.dp))
                Spacer(Modifier.width(16.dp))
                WheelPicker(
                    items              = CUSTOM_UNITS.map { u -> u + if (currentValue > 1) "s" else "" },
                    selectedIndex      = unitIndex,
                    onSelectedIndexChange = { unitIndex = it },
                    modifier           = Modifier.width(104.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val minutes = when (unit) {
                    "hour" -> currentValue * 60
                    "day"  -> currentValue * 60 * 24
                    else   -> currentValue
                }
                onConfirm(minutes)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun WheelPicker(
    items:                List<String>,
    selectedIndex:        Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier:             Modifier = Modifier,
) {
    val itemHeightDp = 48.dp
    val listState    = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(selectedIndex) {
        if (listState.firstVisibleItemIndex != selectedIndex) {
            listState.scrollToItem(selectedIndex)
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val idx = listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
            if (idx != selectedIndex) onSelectedIndexChange(idx)
        }
    }

    Box(modifier = modifier.height(itemHeightDp * 3)) {
        LazyColumn(
            state         = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            modifier      = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Spacer(Modifier.height(itemHeightDp).fillMaxWidth()) }
            items(items.size) { idx ->
                val isSel = listState.firstVisibleItemIndex == idx
                Box(Modifier.height(itemHeightDp).fillMaxWidth(), Alignment.Center) {
                    Text(
                        text       = items[idx],
                        style      = if (isSel) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color      = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                    )
                }
            }
            item { Spacer(Modifier.height(itemHeightDp).fillMaxWidth()) }
        }
        HorizontalDivider(Modifier.align(Alignment.TopCenter).padding(top = itemHeightDp),
            color = MaterialTheme.colorScheme.outlineVariant)
        HorizontalDivider(Modifier.align(Alignment.TopCenter).padding(top = itemHeightDp * 2),
            color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Time picker dialog (Material3 TimePicker in an AlertDialog)
// ─────────────────────────────────────────────────────────────────────────────

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
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDisplayDate(isoDate: String, pattern: String = "EEE, MMM d, yyyy"): String =
    try {
        java.time.LocalDate.parse(isoDate)
            .format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    } catch (_: Exception) {
        isoDate.ifBlank { "Not set" }
    }

private fun formatDisplayTime(time24: String): String =
    try {
        val parts = time24.split(":")
        val h = parts[0].toInt(); val m = parts[1].toInt()
        val ampm = if (h < 12) "AM" else "PM"
        val dh   = if (h == 0) 12 else if (h > 12) h - 12 else h
        "%d:%02d %s".format(dh, m, ampm)
    } catch (_: Exception) {
        time24
    }

private fun isoDateToMillis(dateStr: String): Long? =
    try {
        java.time.LocalDate.parse(dateStr)
            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }

private fun millisToIsoDate(ms: Long): String =
    java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate().toString()

private fun minutesLabel(minutes: Int): String = when {
    minutes % (60 * 24 * 7) == 0 -> { val w = minutes / (60 * 24 * 7); "$w week${if (w > 1) "s" else ""} before" }
    minutes % (60 * 24) == 0 -> { val d = minutes / (60 * 24); "$d day${if (d > 1) "s" else ""} before" }
    minutes % 60 == 0        -> { val h = minutes / 60; "$h hour${if (h > 1) "s" else ""} before" }
    else                     -> "$minutes min before"
}

private fun typeLabel(type: String): String = when (type) {
    "task"        -> "Task"
    "birthday"    -> "Birthday"
    "anniversary" -> "Anniversary"
    "countdown"   -> "Countdown"
    else          -> "Event"
}
