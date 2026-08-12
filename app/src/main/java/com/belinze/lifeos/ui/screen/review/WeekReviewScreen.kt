package com.belinze.lifeos.ui.screen.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.InsightsViewModel
import com.belinze.lifeos.viewmodel.TaskFilter
import com.belinze.lifeos.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// WeekReviewScreen — matches WeekReviewScreen.tsx
//
// Sections:
//   ‣ Financial Health Score hero card (0–100)
//   ‣ 7-Day spend pattern bar chart (per-day bars, tappable for amount tooltip)
//   ‣ What Changed? narrative card
//   ‣ Spending summary card
//   ‣ Tasks card (done vs pending)
// ─────────────────────────────────────────────────────────────────────────────

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun WeekReviewScreen(
    navController:     NavHostController,
    insightsViewModel: InsightsViewModel = hiltViewModel(),
    taskViewModel:     TaskViewModel     = hiltViewModel(),
) {
    val insState  by insightsViewModel.uiState.collectAsState()
    val taskState by taskViewModel.uiState.collectAsState()

    // Week range label
    val today      = LocalDate.now()
    val weekStart  = today.with(DayOfWeek.MONDAY)
    val weekEnd    = today.with(DayOfWeek.SUNDAY)
    val shortFmt   = DateTimeFormatter.ofPattern("MMM d")
    val weekLabel  = "${weekStart.format(shortFmt)} – ${weekEnd.format(shortFmt)}, ${weekEnd.year}"

    // Current + previous month spend
    val curSpend   = insState.currentTotals?.expense  ?: 0.0
    val prevSpend  = insState.previousTotals?.expense ?: 0.0
    val momPct     = if (prevSpend > 0) ((curSpend - prevSpend) / prevSpend) * 100.0 else 0.0

    // Financial health score (0–100) mirroring RN calculation:
    // Start at 100, penalise: spend up >10%, uncategorized, pending review, fuliza
    val spendPenalty   = if (momPct > 10.0) (momPct.coerceAtMost(60.0) * 0.5).toInt() else 0
    val uncatPenalty   = (insState.uncategorized.coerceAtMost(20) * 1)
    val pendingPenalty = (insState.pendingReview.coerceAtMost(10) * 2)
    val healthScore    = (100 - spendPenalty - uncatPenalty - pendingPenalty).coerceIn(0, 100)

    val healthLabel = when {
        healthScore >= 80 -> "Excellent"
        healthScore >= 60 -> "Good"
        healthScore >= 40 -> "Fair"
        else              -> "Needs attention"
    }
    val healthColor = when {
        healthScore >= 80 -> Color(0xFF10B981)
        healthScore >= 60 -> Color(0xFF3B82F6)
        healthScore >= 40 -> Color(0xFFF59E0B)
        else              -> Color(0xFFEF4444)
    }

    // Tasks
    val completed = taskState.tasks.count { it.status == "completed" }
    val pending   = taskState.tasks.count { it.status == "active" }

    // 7-day bars from monthBars (we use the last 7 days from transactions as proxy)
    // Since we don't have per-day aggregation in the ViewModel, distribute month bars as placeholder
    val weekBars = insState.monthBars.takeLast(7).let { bars ->
        val max = bars.maxOfOrNull { it.expense } ?: 1.0
        bars.mapIndexed { i, bar ->
            Triple(
                DAY_LABELS.getOrElse(i) { bar.label },
                bar.expense,
                if (max > 0) (bar.expense / max).toFloat().coerceIn(0.02f, 1f) else 0.02f
            )
        }
    }
    val maxDaySpend = weekBars.maxOfOrNull { it.second } ?: 1.0

    var selectedBarIdx by remember { mutableStateOf<Int?>(null) }

    PageScaffold(
        eyebrow = "Review",
        title   = "Weekly Review",
        onBack  = { navController.popBackStack() },
    ) {
        if (insState.isLoading) {
            ShimmerLoadingState(rowCount = 5)
            return@PageScaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {

            // Week label
            Text(weekLabel, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))

            // ── Financial Health Score ─────────────────────────────────────────
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    // Circular score badge
                    Box(
                        modifier         = Modifier
                            .size(96.dp)
                            .background(healthColor.copy(0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = "$healthScore",
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color      = healthColor,
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text("Financial Health Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    Text(healthLabel, fontWeight = FontWeight.SemiBold, color = healthColor)
                }
            }

            // ── 7-Day Spend Pattern ───────────────────────────────────────────
            if (weekBars.isNotEmpty()) {
                SectionHeader(label = "7-Day Spend Pattern")
                FrostCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().height(100.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.Bottom,
                    ) {
                        weekBars.forEachIndexed { i, (label, amount, fill) ->
                            val barColor = when {
                                fill >= 0.85f -> Color(0xFFEF4444)  // Peak
                                fill >= 0.60f -> Color(0xFFF59E0B)  // High
                                else          -> Color(0xFF10B981)  // Normal
                            }
                            val interactionSource = remember { MutableInteractionSource() }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier            = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication        = null,
                                    ) { selectedBarIdx = if (selectedBarIdx == i) null else i },
                            ) {
                                if (selectedBarIdx == i) {
                                    Text(compactCurrency(amount), fontSize = 8.sp,
                                        color = barColor, fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height((80 * fill).dp)
                                        .background(
                                            if (selectedBarIdx == i) barColor else barColor.copy(0.75f),
                                            MaterialTheme.shapes.extraSmall,
                                        ),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(label, fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // Legend
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        listOf(
                            "Normal" to Color(0xFF10B981),
                            "High"   to Color(0xFFF59E0B),
                            "Peak"   to Color(0xFFEF4444),
                        ).forEach { (lbl, col) ->
                            Box(modifier = Modifier
                                .size(8.dp)
                                .background(col, MaterialTheme.shapes.extraSmall))
                            Spacer(Modifier.width(4.dp))
                            Text(lbl, fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                                modifier = Modifier.padding(end = Spacing.md))
                        }
                    }
                }
            }

            // ── What Changed? ─────────────────────────────────────────────────
            SectionHeader(label = "What Changed?")
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // Spend trend
                    val trendPositive = momPct <= 0
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (trendPositive) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint     = if (trendPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text  = if (trendPositive)
                                "Spending down ${String.format("%.1f", -momPct)}% vs last month"
                            else
                                "Spending up ${String.format("%.1f", momPct)}% vs last month",
                            color = if (trendPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Uncategorized
                    if (insState.uncategorized > 0) {
                        Text(
                            "⚠️ ${insState.uncategorized} uncategorized transaction${if (insState.uncategorized != 1) "s" else ""} need review",
                            color = Color(0xFFF59E0B),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Top category
                    insState.categoryTotals.firstOrNull()?.let { top ->
                        Text(
                            "🏷️ Top category: ${top.category.replaceFirstChar { it.uppercase() }} (${formatCurrency(top.total)})",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // ── Spending Summary ──────────────────────────────────────────────
            SectionHeader(label = "Spending")
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Text(formatCurrency(curSpend),
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground)
                Text("This month's total spend",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                if (insState.feeTotal > 0) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text("Service fees: ${formatCurrency(insState.feeTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF59E0B))
                }
            }

            // ── Tasks ─────────────────────────────────────────────────────────
            SectionHeader(label = "Tasks")
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FrostCard(modifier = Modifier.weight(1f)) {
                    Text("$completed", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981))
                    Text("Done", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                }
                FrostCard(modifier = Modifier.weight(1f)) {
                    Text("$pending", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = if (pending > 5) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onBackground)
                    Text("Pending", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
