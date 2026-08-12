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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TaskViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val WARNING = Color(0xFFF5CB5C)

@Composable
fun TaskDetailScreen(
    taskId:        String,
    navController: NavHostController,
    viewModel:     TaskViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val task = remember(uiState.tasks, taskId) { uiState.tasks.find { it.id == taskId } }
    var showDelete by remember { mutableStateOf(false) }

    PageScaffold(
        title = "Task",
        onBack = { navController.popBackStack() },
        actions = {
            if (task != null) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        if (task == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@PageScaffold
        }

        val priorityColor = when (task.priority) {
            "high" -> MaterialTheme.colorScheme.error
            "medium" -> WARNING
            else -> MaterialTheme.colorScheme.primary
        }
        val isCompleted = task.status == "completed"

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0x33 / 255f), RoundedCornerShape(9999.dp))
                            .padding(horizontal = Spacing.base, vertical = Spacing.xs),
                    ) {
                        Text(task.priority.uppercase(), style = MaterialTheme.typography.labelSmall,
                            color = priorityColor)
                    }
                    Spacer(Modifier.height(Spacing.base))
                    Text(
                        task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    task.description?.let {
                        Spacer(Modifier.height(Spacing.base))
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    task.deadline?.let {
                        Spacer(Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.size(Spacing.xs))
                            Text(formatDateTime(it), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.complete(task.id) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.base),
            ) {
                Text(if (isCompleted) "Mark as Active" else "Mark as Completed")
            }

            OutlinedButton(
                onClick = { navController.navigate(NavTo.taskForm(task.id)) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.base),
            ) {
                Text("Edit Task", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }

    if (showDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete task") },
            text = { Text("Are you sure?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.softDelete(taskId)
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }
}

private fun formatDateTime(iso: String): String = try {
    LocalDateTime.parse(iso.take(19)).format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
} catch (_: Exception) { iso.take(16) }
