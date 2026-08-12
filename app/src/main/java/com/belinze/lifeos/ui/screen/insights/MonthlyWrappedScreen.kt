package com.belinze.lifeos.ui.screen.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.ShapeLg
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.MonthlyWrappedViewModel
import com.belinze.lifeos.viewmodel.TopCategoryRow

// ─────────────────────────────────────────────────────────────────────────────
// MonthlyWrappedScreen — 1:1 with MonthlyWrappedScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

private val RANK_COLORS = listOf(
    Color(0xFFF59E0B), // 1st — gold
    Color(0xFF9CA3AF), // 2nd — silver
    Color(0xFFB45309), // 3rd — bronze
)
private val COLOR_FULIZA  = Color(0xFFF97316)
private val COLOR_SUCCESS = Color(0xFF22C55E)
private val COLOR_DANGER  = Color(0xFFEF4444)

@Composable
fun MonthlyWrappedScreen(
    initialMonthOffset: Int = 0,
    navController:      NavHostController,
    viewModel:          MonthlyWrappedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.setMonthOffset(initialMonthOffset) }

    PageScaffold(
        onBack     = { navController.popBackStack() },
        scrollable = false,
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(Spacing.base))
                        Text("Crunching the numbers…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(Spacing.screenHorizontal),
                    contentAlignment = Alignment.Center) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
            !state.hasData -> {
                Box(Modifier.fillMaxSize().padding(Spacing.screenHorizontal),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No spending this month",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.base),
                ) {
                    // ─ Month navigation header ─
                    item {
                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment    = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick  = { viewModel.setMonthOffset(state.monthOffset - 1) },
                                enabled  = state.monthOffset > state.minMonthOffset,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Older month",
                                    modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text       = "${state.monthLabel} Wrapped",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(
                                onClick  = { viewModel.setMonthOffset(state.monthOffset + 1) },
                                enabled  = state.monthOffset < 0,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Newer month",
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // ─ Hero ─
                    item {
                        GlassCard {
                            Column(
                                modifier            = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("You spent",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text       = formatCurrency(state.totalSpend),
                                    fontSize   = 44.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 50.sp,
                                )
                                Text(
                                    text  = "this month · ${state.txCount} transaction${if (state.txCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // ─ Top Categories ─
                    if (state.topCategories.isNotEmpty()) {
                        item {
                            GlassCard {
                                Text("Top Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(Spacing.sm))
                                state.topCategories.forEachIndexed { i, row ->
                                    if (i > 0) HorizontalDivider(
                                        modifier = Modifier.padding(vertical = Spacing.xs),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    )
                                    CategoryRow(row)
                                }
                            }
                        }
                    }

                    // ─ Top Merchant + Biggest Spend (half-cards) ─
                    item {
                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            // Top merchant
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Text("Top Merchant",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text       = state.topMerchantName.ifBlank { "—" },
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1,
                                )
                                Text(
                                    text  = formatCurrency(state.topMerchantSpend),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // Biggest spend
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Text("Biggest Spend",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text       = formatCurrency(state.biggestAmount),
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text     = state.biggestMerchant.ifBlank { "—" },
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    // ─ Active Days + Fees (half-cards) ─
                    item {
                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Text("Active Days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text       = "${state.activeDays} of ${state.totalDaysInMonth} days",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (state.feesTotal > 0.0) {
                                GlassCard(modifier = Modifier.weight(1f)) {
                                    Text("Fees Paid",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(Spacing.xs))
                                    Text(
                                        text       = formatCurrency(state.feesTotal),
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // ─ Fuliza warning (conditional) ─
                    if (state.fulizaCount > 0) {
                        item {
                            LeftAccentCard(color = MaterialTheme.colorScheme.error) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = "${state.fulizaCount} times · ${formatCurrency(state.fulizaTotal)}",
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = COLOR_FULIZA,
                                        )
                                        Text(
                                            text  = "Fuliza loans affect your balance. Consider paying back early.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─ Savings verdict (conditional, only if income > 0) ─
                    if (state.totalIncome > 0.0) {
                        item {
                            val saved = state.totalIncome - state.totalSpend
                            val isSaving = saved >= 0
                            LeftAccentCard(
                                color = if (isSaving) COLOR_SUCCESS else COLOR_DANGER,
                            ) {
                                Text(
                                    text  = if (isSaving) "Saved this month" else "Spent over income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text       = formatCurrency(kotlin.math.abs(saved)),
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (isSaving) COLOR_SUCCESS else COLOR_DANGER,
                                )
                                Text(
                                    text  = "Income ${formatCurrency(state.totalIncome)} · Spend ${formatCurrency(state.totalSpend)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                }
            }
        }
    }
}

// ─── Category row ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(row: TopCategoryRow) {
    val rankColor = RANK_COLORS.getOrNull(row.rank - 1) ?: MaterialTheme.colorScheme.onSurfaceVariant
    val rankLabel = when (row.rank) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${row.rank}th" }
    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = rankLabel,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color      = rankColor,
                modifier   = Modifier.width(28.dp),
            )
            Text(
                text  = row.category.replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text       = formatCurrency(row.total),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─── Left accent card helper ──────────────────────────────────────────────────
// Draws a 3dp accent strip on the left edge of a GlassCard, clipped to ShapeLg.

@Composable
private fun LeftAccentCard(
    color:   Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLg)
            .drawBehind {
                // Accent bar on the left — clipped by the outer ShapeLg clip
                drawRect(
                    color = color,
                    size  = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height),
                )
            },
    ) {
        // Offset the GlassCard so it starts where the accent bar ends
        Box(modifier = Modifier.padding(start = 3.dp)) {
            GlassCard(content = content)
        }
    }
}
