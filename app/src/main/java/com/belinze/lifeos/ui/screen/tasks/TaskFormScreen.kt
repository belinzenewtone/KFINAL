package com.belinze.lifeos.ui.screen.tasks

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                OutlinedTextField(
                    value = formState.deadline?.take(10) ?: "",
                    onValueChange = { viewModel.updateDeadline(it.ifBlank { null }) },
                    label = { Text("Date") },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = formState.deadline?.take(16)?.substring(11) ?: "",
                    onValueChange = { time ->
                        val date = formState.deadline?.take(10) ?: ""
                        viewModel.updateDeadline(if (time.isBlank()) date else "$date" + "T" + time)
                    },
                    label = { Text("Time") },
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
