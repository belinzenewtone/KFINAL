package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

private val SUCCESS = Color(0xFF7BC47B)

@Composable
fun GoalsScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var banner by remember { mutableStateOf<String?>(null) }
    var logGoalId by remember { mutableStateOf<String?>(null) }
    var logAmount by remember { mutableStateOf("") }

    val activeGoals = state.goals.filter { it.status == "active" }

    PageScaffold(
        eyebrow = "Personal Growth",
        title = "Goals",
        subtitle = "${activeGoals.size} active goal${if (activeGoals.size == 1) "" else "s"}",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.goalForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add goal", tint = MaterialTheme.colorScheme.primary)
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
                Text("No goals yet", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Set a goal to start tracking your progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onEdit = { navController.navigate(NavTo.goalForm(goal.id)) },
                        onLogProgress = { logGoalId = goal.id; logAmount = "" },
                        onComplete = {
                            viewModel.markGoalComplete(goal.id)
                            banner = "${goal.title} marked as complete"
                        },
                        onDelete = { viewModel.deleteGoal(goal.id) },
                    )
                }
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
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
                        viewModel.logGoalProgress(goal.id, logAmount.toDoubleOrNull() ?: 0.0)
                        banner = "Progress logged"
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
    val percent = if (goal.targetValue > 0)
        (goal.currentValue / goal.targetValue * 100).toInt().coerceIn(0, 100) else 0
    val isCompleted = goal.status == "completed"

    GlassCard(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                Text(goal.title, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                goal.category?.let {
                    Text(it.replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text("${formatCurrency(goal.currentValue)} / ${formatCurrency(goal.targetValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                goal.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                goal.deadline?.let {
                    Box(
                        modifier = Modifier
                            .padding(top = Spacing.xs)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9999.dp))
                            .padding(horizontal = Spacing.sm, vertical = 2.dp),
                    ) {
                        Text("Due ${formatDate(it)}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("$percent%", style = MaterialTheme.typography.titleMedium, color = SUCCESS)
        }
        Spacer(Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(percent / 100f).height(6.dp)
                    .background(SUCCESS, CircleShape),
            )
        }
        Spacer(Modifier.height(Spacing.base))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            if (!isCompleted) {
                TextButton(onClick = onLogProgress) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Log Progress", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onComplete) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = SUCCESS, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Mark Complete", color = SUCCESS)
                }
            }
            TextButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
} catch (_: Exception) { iso.take(10) }
