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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.belinze.lifeos.ui.theme.categoryColor
import com.belinze.lifeos.ui.theme.categoryIcon
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.BudgetViewModel
import com.belinze.lifeos.viewmodel.BudgetWithSpend
import kotlin.math.roundToInt

// Matches BudgetsScreen.tsx's local SEMANTIC constant exactly.
private val SUCCESS = Color(0xFF4ADE80)
private val WARNING = Color(0xFFFBBF24)
private val DANGER = Color(0xFFF87171)

@Composable
fun BudgetsScreen(
    navController: NavHostController,
    viewModel:     BudgetViewModel = hiltViewModel(),
) {
    // Reload whenever the screen resumes (e.g. returning from BudgetFormScreen)
    // so newly added or edited budgets appear immediately.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load()
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var budgetToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (budgetToDelete != null) {
        val (deleteId, deleteCategory) = budgetToDelete!!
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete budget") },
            text  = { Text("Remove $deleteCategory budget?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.softDelete(deleteId)
                    budgetToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) { Text("Cancel") }
            },
        )
    }

    val active       = remember(state.budgets) { state.budgets.filter { it.budget.isActive != 0 } }
    val totalLimit   = remember(active)        { active.sumOf { it.budget.limitAmount } }
    val totalSpend   = remember(active)        { active.sumOf { it.spend } }
    val overCount    = remember(active)        { active.count { it.spend > it.budget.limitAmount } }
    val summaryPct   = remember(totalLimit, totalSpend) {
        if (totalLimit > 0) (totalSpend / totalLimit * 100).roundToInt() else 0
    }
    val summaryColor = if (summaryPct > 100) DANGER else if (summaryPct > 80) WARNING else SUCCESS

    PageScaffold(
        eyebrow = "Spending guardrails",
        title = "Budgets",
        onBack = { navController.popBackStack() },
        scrollable = false, // LazyColumn below provides its own scrolling
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.budgetForm()) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add budget", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Spacing.bottomNavSafeArea),
        ) {
            item {
                GlassCard(
                    variant = com.belinze.lifeos.ui.components.GlassCardVariant.Elevated,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                "This Month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                        append(formatCurrency(totalSpend))
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        ),
                                    ) {
                                        append(" / ${formatCurrency(totalLimit)}")
                                    }
                                },
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(summaryColor.copy(alpha = 0x20 / 255f), MaterialTheme.shapes.large)
                                .padding(horizontal = Spacing.base, vertical = Spacing.xs),
                        ) {
                            Text(
                                if (summaryPct > 100) {
                                    "Over budget"
                                } else if (summaryPct > 80) {
                                    "Nearing limit"
                                } else {
                                    "On track"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = summaryColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.base))
                    BudgetProgressBar(pct = (summaryPct.coerceIn(0, 100)).toFloat() / 100f, color = summaryColor)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        if (overCount > 0) {
                            "$overCount categor${if (overCount > 1) "ies" else "y"} over budget"
                        } else if (state.budgets.isEmpty()) {
                            "No budgets set"
                        } else {
                            "All categories within budget"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.budgets.isNotEmpty()) {
                item {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                }
            }

            if (state.budgets.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.x3l),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Wallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text("No budgets yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Tap + to create a spending guardrail for a category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                }
            }

            items(state.budgets, key = { it.budget.id }) { bws ->
                Box(modifier = Modifier.animateItem()) {
                    BudgetCard(
                        bws = bws,
                        onToggle = { viewModel.toggleActive(bws.budget.id, it) },
                        onEdit = { navController.navigate(NavTo.budgetForm(bws.budget.id)) },
                        onDelete = { budgetToDelete = bws.budget.id to bws.budget.category },
                    )
                }
            }
        }
        } // end PullToRefreshBox
    }
}

@Composable
private fun BudgetCard(
    bws: BudgetWithSpend,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = bws.budget.category
    val color = categoryColor(category)
    val isActive = bws.budget.isActive != 0
    val percent = bws.pct.coerceIn(0f, 100f)
    val isOver = bws.spend > bws.budget.limitAmount
    val isWarning = !isOver && bws.pct > 0.80f
    val statusColor = if (isOver) DANGER else if (isWarning) WARNING else SUCCESS
    val statusLabel = if (isOver) "Over" else if (isWarning) "Close" else "On track"

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm)
            .alpha(if (isActive) 1f else 0.7f),
    ) {
        // Row 1: icon + title | switch | edit | delete
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(color.copy(alpha = 0x20 / 255f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(categoryIcon(category), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor    = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor    = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor  = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
        }

        // Row 2: period · limit | status badge
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                "${bws.budget.period.replaceFirstChar { it.uppercase() }} · ${formatCurrency(bws.budget.limitAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0x20 / 255f), MaterialTheme.shapes.large)
                    .padding(horizontal = Spacing.sm, vertical = 2.dp),
            ) {
                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor,
                    maxLines = 1, softWrap = false)
            }
        }

        // Row 3: progress bar
        BudgetProgressBar(pct = percent.coerceIn(0f, 100f) / 100f, color = statusColor)

        // Row 4: spent | left/over
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${formatCurrency(bws.spend)} spent (${bws.pct.roundToInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (isOver) {
                    "+${formatCurrency(bws.spend - bws.budget.limitAmount)}"
                } else {
                    "${formatCurrency(bws.budget.limitAmount - bws.spend)} left"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isOver) DANGER else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun BudgetProgressBar(pct: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct.coerceIn(0f, 1f))
                .height(8.dp)
                .background(color, CircleShape),
        )
    }
}
