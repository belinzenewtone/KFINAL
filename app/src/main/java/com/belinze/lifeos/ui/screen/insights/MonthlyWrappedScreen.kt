package com.belinze.lifeos.ui.screen.insights

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.InsightsViewModel
import com.belinze.lifeos.viewmodel.MonthBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// MonthlyWrappedScreen — matches MonthlyWrappedScreen.tsx
//
// Navigated to from InsightsScreen bar taps with an initialMonthOffset.
// Chevrons let the user navigate between months (0 = current, positive = past).
// Uses InsightsViewModel.monthBars to source per-month data.
// ─────────────────────────────────────────────────────────────────────────────

private val MONTH_FMT_LONG  = DateTimeFormatter.ofPattern("MMMM yyyy")
private val MONTH_FMT_SHORT = DateTimeFormatter.ofPattern("MMMM")

@Composable
fun MonthlyWrappedScreen(
    initialMonthOffset: Int              = 0,
    navController:      NavHostController,
    viewModel:          InsightsViewModel = hiltViewModel(),
) {
    val state   by viewModel.uiState.collectAsState()
    var offset  by rememberSaveable { mutableIntStateOf(initialMonthOffset) }

    val maxOffset = (state.monthBars.size - 1).coerceAtLeast(0)

    // Resolve the bar for the selected offset
    val bar: MonthBar? = state.monthBars
        .getOrNull(state.monthBars.size - 1 - offset)

    val monthDate     = LocalDate.now().minusMonths(offset.toLong())
    val monthTitle    = monthDate.format(MONTH_FMT_LONG)
    val monthShort    = monthDate.format(MONTH_FMT_SHORT)

    PageScaffold(
        eyebrow = "Analytics",
        title   = "$monthShort Wrapped",
        onBack  = { navController.popBackStack() },
        actions = {
            // Left = older month (higher offset)
            IconButton(
                onClick  = { if (offset < maxOffset) offset++ },
                enabled  = offset < maxOffset,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            // Right = newer month (lower offset)
            IconButton(
                onClick  = { if (offset > 0) offset-- },
                enabled  = offset > 0,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        },
    ) {
        if (state.isLoading) {
            ShimmerLoadingState(rowCount = 6)
            return@PageScaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // Month label
            Text(monthTitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))

            if (bar == null || (bar.expense == 0.0 && bar.income == 0.0)) {
                FrostCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No spending recorded for $monthShort yet.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(Spacing.lg))
                }
                return@Column
            }

            // ── Hero spend card ────────────────────────────────────────────────
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Text("You spent", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                Spacer(Modifier.height(4.dp))
                Text(formatCurrency(bar.expense),
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary)
                if (bar.income > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("Income: ${formatCurrency(bar.income)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF10B981))
                    val saved = bar.income - bar.expense
                    Text(
                        text  = if (saved >= 0) "Saved: ${formatCurrency(saved)}"
                                else "Over income by: ${formatCurrency(-saved)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (saved >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    )
                }
            }

            // ── Top categories (use current-month data as approximation) ───────
            if (offset == 0 && state.categoryTotals.isNotEmpty()) {
                FrostCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Top Categories", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = Spacing.sm))
                    val medals = listOf("🥇", "🥈", "🥉")
                    state.categoryTotals.take(3).forEachIndexed { i, cat ->
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${medals.getOrElse(i) { "#${i + 1}"}} ${cat.category.replaceFirstChar { it.uppercase() }}",
                                color = MaterialTheme.colorScheme.onBackground)
                            Text(compactCurrency(cat.total), fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }

            // ── Merchant + fee row ─────────────────────────────────────────────
            if (offset == 0) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    state.topMerchants.firstOrNull()?.let { top ->
                        FrostCard(modifier = Modifier.weight(1f)) {
                            Text("Top Merchant", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                            Text(top.merchant, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground)
                            Text(compactCurrency(top.total), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (state.feeTotal > 0) {
                        FrostCard(modifier = Modifier.weight(1f)) {
                            Text("Fees Paid", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                            Text(compactCurrency(state.feeTotal),
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFFF59E0B))
                        }
                    }
                }

                // ── Fuliza warning ─────────────────────────────────────────────
                if (state.pendingReview > 0) {
                    FrostCard(
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, Color(0xFFEF4444).copy(0.40f), MaterialTheme.shapes.medium),
                    ) {
                        Text("⚠️ ${state.pendingReview} transaction${if (state.pendingReview != 1) "s" else ""} pending review",
                            color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                        Text("Visit the Review Queue to categorize them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444).copy(0.70f),
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
