package com.belinze.lifeos.ui.screen.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TaskViewModel

private val PRIORITIES = listOf("low", "medium", "high")

@Composable
fun TaskFormScreen(
    taskId:        String?,
    navController: NavHostController,
    viewModel:     TaskViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val isEdit    = !taskId.isNullOrEmpty()

    LaunchedEffect(taskId) {
        viewModel.openForm(taskId?.ifEmpty { null })
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "New",
        title   = "Task",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // Title
            OutlinedTextField(
                value         = formState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label         = { Text("Title") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // Priority
            Text("Priority", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PRIORITIES.forEach { p ->
                    FilterChip(
                        selected = formState.priority == p,
                        onClick  = { viewModel.updatePriority(p) },
                        label    = { Text(p.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // Deadline
            OutlinedTextField(
                value         = formState.deadline ?: "",
                onValueChange = { viewModel.updateDeadline(it.ifBlank { null }) },
                label         = { Text("Deadline (YYYY-MM-DD, optional)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // Notes
            OutlinedTextField(
                value         = formState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label         = { Text("Notes (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4,
            )

            // Alarm toggle
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Enable alarm", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked         = formState.alarmEnabled,
                    onCheckedChange = { viewModel.toggleAlarm(it) },
                )
            }

            // Error
            if (formState.error != null) {
                Text(formState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(Spacing.sm))

            Button(
                onClick  = { viewModel.saveForm { navController.popBackStack() } },
                enabled  = !formState.isSaving && formState.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update Task" else "Save Task")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
