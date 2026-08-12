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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
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
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SUCCESS = Color(0xFF7BC47B)

@Composable
fun IncomeScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val incomes = state.income
    val totalIncome = incomes.sumOf { it.amount }

    PageScaffold(
        title = "Income",
        subtitle = "${incomes.size} entr${if (incomes.size == 1) "y" else "ies"} tracked",
        onBack = { navController.popBackStack() },
        scrollable = false, // LazyColumn below provides its own scrolling
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.incomeForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add income", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (incomes.isNotEmpty()) {
                item {
                    GlassCard(
                        variant = com.belinze.lifeos.ui.components.GlassCardVariant.Elevated,
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
                    ) {
                        Text("Total Income", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalIncome),
                            style = MaterialTheme.typography.headlineMedium,
                            color = SUCCESS,
                            modifier = Modifier.padding(top = Spacing.xs))
                        Text("${incomes.size} source${if (incomes.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            if (incomes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.x3l),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.AttachMoney, contentDescription = null,
                                tint = SUCCESS, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(Spacing.base))
                        Text("No income yet", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(Spacing.xs))
                        Text("Track your salary, side hustles, and other income sources.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(incomes, key = { it.id }) { income ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                                Text(income.source ?: "Income", style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Text(
                                    formatDate(income.date) + if (income.isRecurring != 0) " · ${income.frequency ?: ""}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                income.note?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatCurrency(income.amount),
                                    style = MaterialTheme.typography.titleMedium, color = SUCCESS)
                                IconButton(onClick = { viewModel.deleteIncome(income.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

private fun formatDate(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
} catch (_: Exception) { iso?.take(10) ?: "" }
