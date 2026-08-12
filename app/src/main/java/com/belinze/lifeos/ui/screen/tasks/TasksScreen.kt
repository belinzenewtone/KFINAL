package com.belinze.lifeos.ui.screen.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.TaskEntity
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TaskFilter
import com.belinze.lifeos.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavHostController,
    viewModel:     TaskViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow    = "Manage",
            title      = "Tasks",
            onBack     = { navController.popBackStack() },
            scrollable = false,   // LazyColumn owns scrolling
        ) {
            // ── Filter chips ──────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TaskFilter.values().forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick  = { viewModel.setFilter(filter) },
                        label    = { Text(filter.name) },
                    )
                }
            }

            // ── Task list with swipe-to-delete ────────────────────────────
            if (state.tasks.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(Spacing.x2l),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No tasks. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.40f),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.tasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { newValue ->
                                if (newValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.softDelete(task.id)
                                    true
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state              = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent  = {
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                        Color(0xFFEF4444) else Color.Transparent,
                                    label = "swipe_bg",
                                )
                                Box(
                                    modifier         = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(
                                        imageVector        = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint               = Color.White,
                                    )
                                }
                            },
                        ) {
                            TaskItem(
                                task     = task,
                                onToggle = { viewModel.complete(task.id) },
                                onDelete = { viewModel.softDelete(task.id) },
                                onClick  = { navController.navigate(NavTo.taskDetail(task.id)) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                }
            }
        }

        // FAB
        ExtendedFloatingActionButton(
            onClick          = { navController.navigate(Route.TASK_FORM) },
            text             = { Text("Add Task") },
            icon             = { Icon(Icons.Filled.Add, contentDescription = null) },
            containerColor   = MaterialTheme.colorScheme.primary,
            contentColor     = MaterialTheme.colorScheme.onPrimary,
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.lg),
        )
    }
}

@Composable
private fun TaskItem(
    task:     TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick:  () -> Unit,
) {
    val isDone            = task.status == "completed"
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = ripple(color = primary.copy(0.12f)), onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Completion toggle
        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector        = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isDone) "Completed" else "Pending",
                tint               = if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground.copy(0.40f),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text            = task.title,
                fontWeight      = FontWeight.Medium,
                color           = if (isDone) MaterialTheme.colorScheme.onBackground.copy(0.40f) else MaterialTheme.colorScheme.onBackground,
                textDecoration  = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                maxLines        = 2,
                overflow        = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Priority indicator
                val priorityColor = when (task.priority) {
                    "high"   -> Color(0xFFEF4444)
                    "medium" -> Color(0xFFF59E0B)
                    else     -> MaterialTheme.colorScheme.onBackground.copy(0.35f)
                }
                Text(
                    text  = task.priority?.replaceFirstChar { it.uppercase() } ?: "Low",
                    fontSize = 11.sp,
                    color = priorityColor,
                )
                if (task.deadline != null) {
                    Text("·", color = MaterialTheme.colorScheme.onBackground.copy(0.35f), fontSize = 11.sp)
                    Text(
                        text  = task.deadline.take(10),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                    )
                }
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector        = Icons.Filled.Delete,
                contentDescription = "Delete",
                tint               = MaterialTheme.colorScheme.onBackground.copy(0.30f),
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}
