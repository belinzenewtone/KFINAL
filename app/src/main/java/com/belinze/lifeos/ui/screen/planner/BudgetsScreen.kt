package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.BudgetViewModel
import com.belinze.lifeos.viewmodel.BudgetWithSpend

// ─────────────────────────────────────────────────────────────────────────────
// BudgetsScreen — matches BudgetsScreen.tsx
//
// Layout:
//   ‣ Summary FrostCard (total spend vs total budget + progress bar)
//   ‣ Per-budget cards with progress bar, active toggle, edit/delete
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BudgetsScreen(
    navController: NavHostController,
    viewModel:     BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow   = "Finance",
            title     = "Budgets",
            onBack    = { navController.popBackStack() },
            scrollable = false,
        ) {
            if (state.isLoading) {
                ShimmerLoadingState(rowCount = 4)
                return@PageScaffold
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // ── Summary card ──────────────────────────────────────────
                item {
                    val totalBudget = state.budgets.sumOf { it.budget.limitAmount }   // BudgetEntity.limitAmount
                    val totalSpend  = state.budgets.sumOf { it.spend }
                    val overallPct  = if (totalBudget > 0) (totalSpend / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
                    val overCount   = state.budgets.count { it.pct >= 1.0 }

                    FrostCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
                        Text("This Month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatCurrency(totalSpend)} / ${formatCurrency(totalBudget)}",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        BudgetProgressBar(pct = overallPct)
                        if (overCount > 0) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text("$overCount budget${if (overCount != 1) "s" else ""} over limit",
                                color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ── Budget list ───────────────────────────────────────────
                item { SectionHeader(label = "Categories") }

                if (state.budgets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                            contentAlignment = Alignment.Center) {
                            Text("No budgets yet. Tap + to add one.",
                                color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                        }
                    }
                } else {
                    items(state.budgets, key = { it.budget.id }) { bws ->
                        BudgetCard(
                            bws      = bws,
                            onClick  = { navController.navigate(NavTo.budgetDetail(bws.budget.id)) },
                            onToggle = { active -> viewModel.toggleActive(bws.budget.id, active) },
                            onEdit   = { navController.navigate(NavTo.budgetForm(bws.budget.id)) },
                            onDelete = { viewModel.softDelete(bws.budget.id) },
                        )
                    }
                }

                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }

        // FAB
        ExtendedFloatingActionButton(
            onClick          = { navController.navigate(Route.BUDGET_FORM) },
            text             = { Text("Add Budget") },
            icon             = { Icon(Icons.Filled.Add, contentDescription = null) },
            containerColor   = MaterialTheme.colorScheme.primary,
            contentColor     = MaterialTheme.colorScheme.onPrimary,
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.bottomNavSafeArea + Spacing.md),
        )
    }
}

@Composable
private fun BudgetCard(
    bws:      BudgetWithSpend,
    onClick:  () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val budget = bws.budget
    val pct    = bws.pct.toFloat().coerceIn(0f, 1.5f)

    val statusColor = when {
        pct >= 1.0f  -> Color(0xFFEF4444)
        pct >= 0.80f -> Color(0xFFF59E0B)
        else         -> Color(0xFF10B981)
    }
    val statusLabel = when {
        pct >= 1.0f  -> "Over budget"
        pct >= 0.80f -> "Close to limit"
        else         -> "On track"
    }

    FrostCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .clickable(onClick = onClick),
    ) {
        // Header row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = budget.category.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text  = "${budget.period} · limit ${formatCurrency(budget.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                )
            }
            // Status badge
            Box(
                modifier = Modifier
                    .background(statusColor.copy(0.15f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = statusColor)
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Progress bar
        BudgetProgressBar(pct = pct.coerceIn(0f, 1f), color = statusColor)

        Spacer(Modifier.height(Spacing.sm))

        // Spend vs remaining
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Spent: ${formatCurrency(bws.spend)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            val remaining = budget.limitAmount - bws.spend
            Text(
                text  = if (remaining >= 0) "Left: ${formatCurrency(remaining)}"
                        else "Over: ${formatCurrency(-remaining)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (remaining >= 0) MaterialTheme.colorScheme.onBackground.copy(0.70f) else Color(0xFFEF4444),
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // Actions row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Switch(checked = budget.isActive == 1, onCheckedChange = onToggle)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp),
                        tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun BudgetProgressBar(pct: Float, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier.fillMaxWidth().height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).height(8.dp)
                .background(color, MaterialTheme.shapes.extraSmall),
        )
    }
}
