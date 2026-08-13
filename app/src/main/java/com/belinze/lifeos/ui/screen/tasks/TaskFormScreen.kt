package com.belinze.lifeos.ui.screen.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.TimeZone
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TaskViewModel

private val PRIORITIES = listOf(
    "low" to "Neutral",
    "medium" to "Important",
    "high" to "Urgent",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    taskId:        String?,
    navController: NavHostController,
    viewModel:     TaskViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val isEdit = !taskId.isNullOrEmpty()

    var showDeadlinePicker by remember { mutableStateOf(false) }
    val deadlinePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showDeadlinePicker) {
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
                        // Preserve existing time if already set, otherwise default to T00:00
                        val existingTime = formState.deadline?.take(16)?.substring(11)?.ifBlank { null }
                        viewModel.updateDeadline(if (existingTime != null) "${dateStr}T${existingTime}" else dateStr)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = deadlinePickerState) }
    }

    LaunchedEffect(taskId) {
        viewModel.openForm(taskId?.ifEmpty { null })
    }

    PageScaffold(
        title = if (isEdit) "Edit Task" else "New Task",
        onBack = { navController.popBackStack() },
        actions = {
            if (isEdit) {
                IconButton(onClick = {
                    viewModel.softDelete(formState.id.orEmpty())
                    navController.popBackStack()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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

            Text("Deadline", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
                Box(modifier = Modifier.weight(2f)) {
                    OutlinedTextField(
                        value = formState.deadline?.take(10) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                OutlinedTextField(
                    value = formState.deadline?.take(16)?.let { if (it.length > 10) it.substring(11) else "" } ?: "",
                    onValueChange = { time ->
                        val date = formState.deadline?.take(10) ?: ""
                        viewModel.updateDeadline(if (time.isBlank()) date.ifBlank { null } else "${date}T${time}")
                    },
                    label = { Text("Time") },
                    placeholder = { Text("HH:mm") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Text("Priority", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PRIORITIES.forEach { (value, label) ->
                    FilterChip(
                        selected = formState.priority == value,
                        onClick = { viewModel.updatePriority(value) },
                        label = { Text(label) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Alarm reminders", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(
                    checked = formState.alarmEnabled,
                    onCheckedChange = { viewModel.toggleAlarm(it) },
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    ),
                )
            }

            if (formState.error != null) {
                Text(formState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.saveForm { navController.popBackStack() } },
                enabled = !formState.isSaving && formState.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Save")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
