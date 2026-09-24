package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.GoalEntity
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Matches GoalsScreen.tsx's local SEMANTIC constant exactly.
private val SUCCESS = Color(0xFF4ADE80)

@Composable
fun GoalsScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var banner by remember { mutableStateOf<String?>(null) }
    var logGoalId by remember { mutableStateOf<String?>(null) }
    var logAmount by remember { mutableStateOf("") }
    var goalToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (goalToDelete != null) {
        val (deleteId, deleteTitle) = goalToDelete!!
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete goal") },
            text  = { Text("Remove $deleteTitle?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGoal(deleteId)
                    goalToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text("Cancel") }
            },
        )
    }

    val activeGoals = remember(state.goals) { state.goals.filter { it.status == "active" } }

    PageScaffold(
        eyebrow = "Personal Growth",
        title = "Goals",
        subtitle = "${activeGoals.size} active goal${if (activeGoals.size == 1) "" else "s"}",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.goalForm()) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add goal", tint = MaterialTheme.colorScheme.primary)
            }
        },
        topBanner = {
            TopBanner(
                visible = banner != null,
                message = banner ?: "",
                tone = BannerTone.Success,
                onDismiss = { banner = null },
                autoDismissMs = 2500,
            )
        },
    ) {
        if (state.goals.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(Spacing.base))
                Text("No goals yet", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Set a goal to start tracking your progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    Box(modifier = Modifier.animateItem()) {
                        GoalCard(
                            goal = goal,
                            onEdit = { navController.navigate(NavTo.goalForm(goal.id)) },
                            onLogProgress = { logGoalId = goal.id; logAmount = "" },
                            onComplete = {
                                viewModel.markGoalComplete(goal.id)
                                banner = "${goal.title} marked as complete"
                            },
                            onDelete = { goalToDelete = goal.id to goal.title },
                        )
                    }
                }
            }
        }
    }

    if (logGoalId != null) {
        val goal = state.goals.firstOrNull { it.id == logGoalId }
        if (goal != null) {
            AlertDialog(
                onDismissRequest = { logGoalId = null },
                title = { Text("Log progress") },
                text = {
                    Column {
                        Text("Add to ${goal.title}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedTextField(
                            value = logAmount,
                            onValueChange = { logAmount = it },
                            placeholder = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val delta = logAmount.toDoubleOrNull() ?: 0.0
                        if (delta > 0) {
                            val next = (goal.currentValue + delta).coerceAtMost(goal.targetValue)
                            val reached = next >= goal.targetValue
                            viewModel.logGoalProgress(goal.id, delta)
                            banner = if (reached) {
                                "Goal reached: ${goal.title} 🎉"
                            } else {
                                "Logged ${formatCurrency(delta)} · ${goal.title}"
                            }
                        } else {
                            banner = "Enter a positive amount"
                        }
                        logGoalId = null
                        logAmount = ""
                    }) { Text("Log") }
                },
                dismissButton = {
                    TextButton(onClick = { logGoalId = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    onEdit: () -> Unit,
    onLogProgress: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val percent = if (goal.targetValue > 0) {
        (goal.currentValue / goal.targetValue * 100).coerceIn(0.0, 100.0)
    } else {
        0.0
    }
    val isCompleted = goal.status == "completed"
    val accentColor = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else SUCCESS

    GlassCard(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
        // Row 1: title
        Text(
            goal.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )

        // Row 2: current / target | percent%
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${formatCurrency(goal.currentValue)} / ${formatCurrency(goal.targetValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("${percent.toInt()}%", style = MaterialTheme.typography.titleSmall, color = accentColor)
        }

        // Row 3: deadline chip | actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (goal.deadline != null) {
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(9999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9999.dp))
                        .padding(horizontal = Spacing.sm, vertical = 2.dp),
                ) {
                    Text("Due ${formatDate(goal.deadline!!)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            if (!isCompleted) {
                IconButton(onClick = onLogProgress, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Log progress",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onComplete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Mark complete",
                        tint = SUCCESS, modifier = Modifier.size(22.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }

        // Row 5: progress bar
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth((percent / 100.0).toFloat()).height(6.dp)
                    .background(accentColor, CircleShape),
            )
        }
    }
}

private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
} catch (_: Exception) {
    iso.take(10)
}
