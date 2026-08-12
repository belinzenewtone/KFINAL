package com.belinze.lifeos.ui.screen.tasks

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TaskViewModel

// ─────────────────────────────────────────────────────────────────────────────
// TaskDetailScreen — read-only view of a single task with complete / delete.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TaskDetailScreen(
    taskId:        String,
    navController: NavHostController,
    viewModel:     TaskViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val task    = remember(uiState.tasks, taskId) { uiState.tasks.find { it.id == taskId } }

    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Task") },
            text  = { Text("This task will be removed. Continue?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.softDelete(taskId)
                    navController.popBackStack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }

    PageScaffold(
        eyebrow = "Task",
        title   = task?.title ?: "—",
        onBack  = { navController.popBackStack() },
        actions = {
            if (task != null) {
                IconButton(onClick = { navController.navigate(NavTo.taskForm(taskId)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
        if (task == null) {
            Text("Task not found.", color = MaterialTheme.colorScheme.error)
            return@PageScaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // Status badge
            val isCompleted = task.status == "completed"
            val badgeColor  = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
            val badgeText   = if (isCompleted) "Completed" else "Active"
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(0.12f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = badgeColor)
            }

            Spacer(Modifier.height(4.dp))

            // Detail rows
            if (task.deadline != null) {
                TaskDetailRow(icon = Icons.Filled.CalendarToday, label = "Deadline",
                    value = task.deadline!!.take(16).replace("T", " "))
            }

            val priorityColor = when (task.priority) {
                "high"   -> Color(0xFFEF4444)
                "medium" -> Color(0xFFF59E0B)
                else     -> MaterialTheme.colorScheme.onBackground.copy(0.55f)
            }
            TaskDetailRow(icon = Icons.Filled.Flag, label = "Priority",
                value = task.priority.replaceFirstChar { it.uppercase() },
                valueColor = priorityColor)

            if (task.alarmEnabled) {
                TaskDetailRow(icon = Icons.Filled.Alarm, label = "Alarm", value = "Enabled",
                    valueColor = MaterialTheme.colorScheme.primary)
            }

            if (!task.notes.isNullOrBlank()) {
                TaskDetailRow(icon = Icons.Filled.Notes, label = "Notes", value = task.notes!!)
            }

            Spacer(Modifier.height(Spacing.md))

            // Complete / Reopen
            if (!isCompleted) {
                Button(
                    onClick  = {
                        viewModel.complete(taskId)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        modifier = Modifier.size(18.dp).padding(end = 6.dp))
                    Text("Mark as Complete")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun TaskDetailRow(
    icon:       ImageVector,
    label:      String,
    value:      String,
    valueColor: Color = Color.Unspecified,
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            Text(value, style = MaterialTheme.typography.bodyLarge,
                color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else valueColor)
        }
    }
}
