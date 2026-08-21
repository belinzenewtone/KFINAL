package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.BudgetViewModel
import com.belinze.lifeos.viewmodel.BudgetWithSpend

// ─────────────────────────────────────────────────────────────────────────────
// BudgetDetailScreen — per-budget detail (spend vs limit, edit/delete)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BudgetDetailScreen(
    budgetId:       String,
    navController:  NavHostController,
    viewModel:      BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bws: BudgetWithSpend? = state.budgets.firstOrNull { it.budget.id == budgetId }

    PageScaffold(
        eyebrow = "Budget",
        title   = bws?.budget?.category?.replaceFirstChar { it.uppercase() } ?: "Budget",
        onBack  = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (bws == null) {
            Text("Budget not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@PageScaffold
        }

        val budget = bws.budget
        val pct = bws.pct
        val statusColor = when {
            pct >= 1f   -> Color(0xFFEF4444)
            pct >= 0.8f -> Color(0xFFF59E0B)
            else        -> Color(0xFF10B981)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // ── Summary ───────────────────────────────────────────────────
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("${formatCurrency(bws.spend)} of ${formatCurrency(budget.limitAmount)}",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    com.belinze.lifeos.ui.screen.planner.BudgetProgressBar(
                        pct = pct.coerceIn(0f, 1f),
                        color = statusColor,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${(pct * 100).toInt()}% used",
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor, fontWeight = FontWeight.Medium)
                        Text("Left: ${formatCurrency(bws.remaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Details ───────────────────────────────────────────────────
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DetailRow("Category", budget.category.replaceFirstChar { it.uppercase() })
                    DetailRow("Period", budget.period.replaceFirstChar { it.uppercase() })
                    DetailRow("Limit", formatCurrency(budget.limitAmount))
                    DetailRow("Status", if (budget.isActive == 1) "Active" else "Paused")
                }
            }

            // ── Actions ───────────────────────────────────────────────────
            TextButton(
                onClick  = { navController.navigate(NavTo.budgetForm(budget.id)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Edit Budget", color = MaterialTheme.colorScheme.primary)
            }
            TextButton(
                onClick  = {
                    viewModel.softDelete(budget.id)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete Budget", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
