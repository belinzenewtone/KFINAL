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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel

// ─────────────────────────────────────────────────────────────────────────────
// GoalsScreen — matches GoalsScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GoalsScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow = "Planner",
            title   = "Goals",
            onBack  = { navController.popBackStack() },
            scrollable = false,
        ) {
            if (state.goals.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                    contentAlignment = Alignment.Center) {
                    Text("No goals yet. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.goals, key = { it.id }) { goal ->
                        val pct = if (goal.targetValue > 0)
                            (goal.currentValue / goal.targetValue).toFloat().coerceIn(0f, 1f) else 0f
                        val statusColor = when {
                            pct >= 1f  -> Color(0xFF10B981)
                            pct >= 0.6f -> Color(0xFFF59E0B)
                            else       -> MaterialTheme.colorScheme.primary
                        }
                        FrostCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = goal.title,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text  = goal.deadline?.let { "Target ${it.take(10)}" } ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                                    )
                                }
                                Text(
                                    text  = "${(pct * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                )
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            // Progress bar
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)) {
                                Box(modifier = Modifier.fillMaxWidth(pct).height(8.dp)
                                    .background(statusColor, MaterialTheme.shapes.extraSmall))
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${formatCurrency(goal.currentValue)} saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
                                Text("of ${formatCurrency(goal.targetValue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
                            }
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(onClick = { navController.navigate(NavTo.goalForm(goal.id)) },
                                    modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit",
                                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteGoal(goal.id) },
                                    modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                                        modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick        = { navController.navigate(NavTo.goalForm()) },
            text           = { Text("Add Goal") },
            icon           = { Icon(Icons.Filled.Add, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.bottomNavSafeArea + Spacing.md),
        )
    }
}
