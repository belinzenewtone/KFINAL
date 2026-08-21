package com.belinze.lifeos.ui.screen.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

private val PRIORITIES = listOf(
    "low"    to "Neutral",
    "medium" to "Important",
    "high"   to "Urgent",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    taskId:        String?,
    navController: NavHostController,
    viewModel:     TaskViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isEdit     = !taskId.isNullOrEmpty()
    val scope      = rememberCoroutineScope()

    // CC-4: fade-in on load
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue   = if (contentVisible) 1f else 0f,
        animationSpec = tween(300),
        label         = "contentAlpha",
    )

    // CC-2: delete confirmation
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // CC-3: success banner
    var successMsg by remember { mutableStateOf<String?>(null) }

    var showDeadlinePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }

    // BUG-CAL7: state is inside the conditional so it re-initialises to the
    // stored deadline each time the dialog opens, not once at screen creation.
    if (showDeadlinePicker) {
        val deadlinePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.deadline?.take(10)?.let {
                runCatching {
                    java.time.LocalDate.parse(it)
                        .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                }.getOrNull()
            } ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDeadlinePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeadlinePicker = false
                    deadlinePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val dateStr = "%04d-%02d-%02d".format(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                        )
                        val existingTime = formState.deadline?.take(16)?.substring(11)?.ifBlank { null }
                        viewModel.updateDeadline(
                            if (existingTime != null) "${dateStr}T$existingTime" else dateStr,
                        )
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = deadlinePickerState) }
    }

    if (showDeadlineTimePicker) {
        val parts = formState.deadline?.take(16)?.substring(11)?.split(":") ?: listOf("08", "00")
        val timeState = rememberTimePickerState(
            initialHour   = parts.getOrNull(0)?.toIntOrNull() ?: 8,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour      = true,
        )
        AlertDialog(
            onDismissRequest = { showDeadlineTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeadlineTimePicker = false
                    val date = formState.deadline?.take(10) ?: ""
                    viewModel.updateDeadline(
                        if (date.isBlank()) {
                            null
                        } else {
                            "${date}T%02d:%02d".format(timeState.hour, timeState.minute)
                        },
                    )
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlineTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timeState)
                }
            },
        )
    }

    LaunchedEffect(taskId) {
        viewModel.openForm(taskId?.ifEmpty { null })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            title    = if (isEdit) "Edit Task" else "New Task",
            onBack   = { navController.popBackStack() },
            actions  = {
                if (isEdit) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            scrollable = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .alpha(contentAlpha),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                OutlinedTextField(
                    value = formState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Pay electricity bill") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = formState.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Description") },
                    placeholder = { Text("Add details...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Deadline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.lg),
                )
                // Date and Time side-by-side — 3:2 ratio gives Time enough width for
                // label + value + icon without wrapping.
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Box(modifier = Modifier.weight(3f)) {
                        OutlinedTextField(
                            value = formState.deadline?.take(10) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { showDeadlinePicker = true },
                        )
                    }
                    Box(modifier = Modifier.weight(2f)) {
                        OutlinedTextField(
                            value = formState.deadline?.take(16)?.let {
                                if (it.length > 10) it.substring(11) else ""
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = formState.deadline != null, // BUG-CAL6: only tappable after a date is set
                            label = { Text("Time") },
                            placeholder = { Text("08:00") },
                            trailingIcon = {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick time",
                                    tint = if (formState.deadline != null) MaterialTheme.colorScheme.onSurfaceVariant
                                           else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (formState.deadline != null) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { showDeadlineTimePicker = true },
                            )
                        }
                    }
                }

                Text(
                    "Priority",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.lg),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PRIORITIES.forEach { (value, label) ->
                        FilterChip(
                            selected = formState.priority == value,
                            onClick  = { viewModel.updatePriority(value) },
                            label    = { Text(label) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Alarm reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = formState.alarmEnabled,
                        onCheckedChange = { viewModel.toggleAlarm(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor    = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor    = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor  = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }

                if (formState.error != null) {
                    Text(formState.error!!, color = MaterialTheme.colorScheme.error)
                }

                // CC-3: show banner then navigate
                Button(
                    onClick  = {
                        viewModel.saveForm {
                            scope.launch {
                                successMsg = if (isEdit) "Task updated" else "Task added"
                                delay(1200)
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled  = !formState.isSaving && formState.title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Save")
                    }
                }

                Spacer(Modifier.height(Spacing.bottomNavSafeArea))
            }
        }

        // CC-3: success banner
        TopBanner(
            visible   = successMsg != null,
            message   = successMsg ?: "",
            tone      = BannerTone.Success,
            onDismiss = { successMsg = null },
        )
    }

    // CC-2: delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete task?") },
            text             = { Text("This task will be permanently removed.") },
            confirmButton    = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.softDelete(formState.id.orEmpty())
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
