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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.TaskEntity
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TaskViewModel

private val SUCCESS = Color(0xFF7BC47B)
private val WARNING = Color(0xFFF5CB5C)
private const val COMPLETED_LIMIT = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavHostController,
    viewModel:     TaskViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var completedExpanded by rememberSaveable { mutableStateOf(false) }

    val filtered = if (query.isBlank()) state.tasks
    else state.tasks.filter {
        it.title.contains(query, ignoreCase = true) ||
            (it.description?.contains(query, ignoreCase = true) == true)
    }
    val active = filtered.filter { it.status == "active" }
    val urgent = active.filter { it.priority == "high" }
    val important = active.filter { it.priority == "medium" }
    val other = active.filter { it.priority == "low" }
    val completed = filtered.filter { it.status == "completed" }.take(COMPLETED_LIMIT)
    val openCount = state.tasks.count { it.status == "active" }
    val completedCount = state.tasks.count { it.status == "completed" }

    PageScaffold(
        title = "Tasks",
        subtitle = "$openCount open · $completedCount completed",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(Route.TASK_FORM) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search tasks") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.lg),
        )

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.x4l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No tasks found", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Spacing.bottomNavSafeArea),
            ) {
                if (urgent.isNotEmpty()) {
                    item { PrioritySectionHeader("Urgent", MaterialTheme.colorScheme.error, urgent.size) }
                    items(urgent, key = { it.id }) { task ->
                        TaskCard(task, MaterialTheme.colorScheme.error, viewModel)
                    }
                }
                if (important.isNotEmpty()) {
                    item { PrioritySectionHeader("Important", WARNING, important.size) }
                    items(important, key = { it.id }) { task ->
                        TaskCard(task, WARNING, viewModel)
                    }
                }
                if (other.isNotEmpty()) {
                    item { PrioritySectionHeader("Other", MaterialTheme.colorScheme.primary, other.size) }
                    items(other, key = { it.id }) { task ->
                        TaskCard(task, MaterialTheme.colorScheme.primary, viewModel)
                    }
                }
                if (completed.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { completedExpanded = !completedExpanded }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Box(
                                modifier = Modifier.size(width = 4.dp, height = 18.dp)
                                    .background(MaterialTheme.colorScheme.outline, CircleShape),
                            )
                            Text("Completed", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f))
                            Text("${completed.size}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(
                                if (completedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    if (completedExpanded) {
                        items(completed, key = { it.id }) { task ->
                            TaskCard(task, MaterialTheme.colorScheme.outline, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrioritySectionHeader(title: String, color: Color, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier.size(width = 4.dp, height = 18.dp).background(color, CircleShape),
        )
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    color: Color,
    viewModel: TaskViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val isTimerActive = state.activeTimerTaskId == task.id
    val displaySeconds = viewModel.displaySeconds(task)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { newValue ->
            when (newValue) {
                SwipeToDismissBoxValue.StartToEnd -> viewModel.complete(task.id)
                SwipeToDismissBoxValue.EndToStart -> viewModel.softDelete(task.id)
                SwipeToDismissBoxValue.Settled -> {}
            }
            true
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> SUCCESS
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipe_bg",
            )
            Box(
                modifier = Modifier.fillMaxSize().background(bgColor, MaterialTheme.shapes.large)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Filled.Check else Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier.size(width = 4.dp, height = 40.dp)
                        .background(if (task.status == "completed") MaterialTheme.colorScheme.outline else color, CircleShape),
                )
                IconButton(
                    onClick = { viewModel.complete(task.id) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        if (task.status == "completed") Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.status == "completed") SUCCESS else color,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (task.status == "completed")
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.status == "completed") TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    task.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        if (isTimerActive) Icons.Filled.Stop else Icons.Filled.Timer,
                        contentDescription = null,
                        tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { viewModel.toggleTimer(task.id) },
                    )
                    if (isTimerActive || displaySeconds > 0) {
                        Text(
                            viewModel.formatTimer(displaySeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
