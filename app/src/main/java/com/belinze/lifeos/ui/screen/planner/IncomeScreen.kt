package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SUCCESS = Color(0xFF4ADE80)

private val FREQUENCY_LABELS = mapOf(
    "once"    to "One-time",
    "daily"   to "Daily",
    "weekly"  to "Weekly",
    "monthly" to "Monthly",
    "yearly"  to "Yearly",
)

@Composable
fun IncomeScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadAll()
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var incomeToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (incomeToDelete != null) {
        val (deleteId, deleteSource) = incomeToDelete!!
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text("Delete income") },
            text  = { Text("Remove $deleteSource?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteIncome(deleteId)
                    incomeToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) { Text("Cancel") }
            },
        )
    }

    val incomes = state.income
    val activeIncomes = incomes.filter { it.isActive != 0 }
    val totalIncome = activeIncomes.sumOf { it.amount }
    val pausedCount = incomes.size - activeIncomes.size

    PageScaffold(
        title = "Income",
        subtitle = "${incomes.size} entr${if (incomes.size == 1) "y" else "ies"} tracked",
        onBack = { navController.popBackStack() },
        scrollable = false, // LazyColumn below provides its own scrolling
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.incomeForm()) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add income", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
        ) {
            if (incomes.isNotEmpty()) {
                item {
                    GlassCard(
                        variant = com.belinze.lifeos.ui.components.GlassCardVariant.Elevated,
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                    ) {
                        Text("Total Income", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalIncome),
                            style = MaterialTheme.typography.titleLarge,
                            color = SUCCESS,
                            modifier = Modifier.padding(top = Spacing.xs))
                        Text(
                            "${activeIncomes.size} active source${if (activeIncomes.size != 1) "s" else ""}" +
                                if (pausedCount > 0) " · $pausedCount paused" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            if (incomes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.x3l),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.AttachMoney, contentDescription = null,
                                tint = SUCCESS, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text("No income yet", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Track your salary, side hustles, and other income sources.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                }
            } else {
                items(incomes, key = { it.id }) { income ->
                    val isActive = income.isActive != 0
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm)
                            .alpha(if (isActive) 1f else 0.45f)
                            .animateItem(),
                        onClick = { navController.navigate(NavTo.incomeForm(income.id)) },
                    ) {
                        // Row 1: source | amount
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                income.source ?: "Income",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Text(formatCurrency(income.amount), style = MaterialTheme.typography.titleMedium, color = SUCCESS)
                        }

                        // Row 2: frequency (only when set)
                        income.frequency?.let { freq ->
                            Text(
                                "Frequency: ${FREQUENCY_LABELS[freq] ?: freq}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Spacing.xs),
                            )
                        }

                        // Row 3: date · note | switch + delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatDate(income.date) + (income.note?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = isActive,
                                onCheckedChange = { viewModel.setIncomeActive(income.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor    = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor    = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor  = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                            Spacer(Modifier.width(28.dp))
                            IconButton(
                                onClick = { incomeToDelete = income.id to (income.source ?: "this income") },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}
