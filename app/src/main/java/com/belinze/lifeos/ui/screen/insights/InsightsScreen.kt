package com.belinze.lifeos.ui.screen.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.AnalyticsFeesData
import com.belinze.lifeos.viewmodel.AnalyticsRange
import com.belinze.lifeos.viewmodel.AnalyticsTab
import com.belinze.lifeos.viewmodel.CategorySparklineItem
import com.belinze.lifeos.viewmodel.InsightsViewModel
import com.belinze.lifeos.viewmodel.MonthBar

// ─────────────────────────────────────────────────────────────────────────────
// InsightsScreen — 1:1 port of AnalyticsScreen.tsx (2 tabs: Analytics + Insights)
// ─────────────────────────────────────────────────────────────────────────────

private val SUCCESS = Color(0xFF7BC47B)

@Composable
fun InsightsScreen(
    navController: NavHostController,
    viewModel:     InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        eyebrow  = "Analytics",
        title    = "Analytics",
        subtitle = "Productivity and finance trends in one place",
        onBack   = { navController.popBackStack() },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Tab bar ───────────────────────────────────────────────────────
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
                SegmentedButton(
                    selected = state.activeTab == AnalyticsTab.Analytics,
                    onClick  = { viewModel.setActiveTab(AnalyticsTab.Analytics) },
                    shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label    = { Text("Analytics") },
                )
                SegmentedButton(
                    selected = state.activeTab == AnalyticsTab.Insights,
                    onClick  = { viewModel.setActiveTab(AnalyticsTab.Insights) },
                    shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label    = { Text("Insights") },
                )
            }

            if (state.isLoading) {
                ShimmerLoadingState(rowCount = 6)
            } else when (state.activeTab) {

                // ── Analytics tab ─────────────────────────────────────────────
                AnalyticsTab.Analytics -> {

                    // Date range chips
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        listOf(AnalyticsRange.ThisWeek, AnalyticsRange.ThisMonth).forEach { range ->
                            val selected = state.dateRange == range
                            FilterChip(
                                selected = selected,
                                onClick  = { viewModel.setDateRange(range) },
                                label    = {
                                    Text(
                                        text  = if (range == AnalyticsRange.ThisWeek) "This week" else "This month",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }

                    // Uncategorized nudge banner
                    if (state.uncategorizedCount > 0 && !state.nudgeDismissed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.base)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick    = { navController.navigate(com.belinze.lifeos.ui.navigation.Route.CATEGORIZE) },
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Text(
                                text  = "⚠",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text     = "${state.uncategorizedCount} transactions uncategorized — charts may be incomplete",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick  = { viewModel.dismissNudge() },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint     = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Spending comparison (current vs prev month)
                    SpendingComparisonCard(
                        currentMonthSpend = state.currentMonthSpend,
                        prevMonthSpend    = state.prevMonthSpend,
                    )

                    Spacer(Modifier.height(Spacing.base))

                    // Summary cards: Spend / Income / Net / Average
                    AnalyticsSummaryCards(
                        spend   = state.totalSpend,
                        income  = state.totalIncome,
                        net     = state.net,
                        average = state.averageTransaction,
                    )

                    // Category spend cards
                    if (state.categorySparklines.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.base))
                        SectionHeader(label = "Spending by Category")
                        state.categorySparklines.forEach { item ->
                            CategorySpendCard(item = item)
                            Spacer(Modifier.height(Spacing.sm))
                        }
                    }

                    // Fees card
                    if (state.feesData.total > 0) {
                        Spacer(Modifier.height(Spacing.sm))
                        FeesCard(fees = state.feesData)
                    }

                    Spacer(Modifier.height(Spacing.bottomNavSafeArea))
                }

                // ── Insights tab ──────────────────────────────────────────────
                AnalyticsTab.Insights -> {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {

                        // 6-Month bar chart
                        if (state.monthBars.isNotEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Monthly Trend",
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onBackground,
                                    modifier   = Modifier.padding(bottom = Spacing.md),
                                )
                                MonthlyBarChart(
                                    bars     = state.monthBars,
                                    onBarTap = { bar ->
                                        val offset = state.monthBars.size - 1 - state.monthBars.indexOf(bar)
                                        navController.navigate(NavTo.monthlyWrapped(offset))
                                    },
                                )
                            }
                        }

                        // Summary tiles
                        val totals = state.monthBars.map { it.expense }
                        val avg    = if (totals.isEmpty()) 0.0 else totals.average()
                        val total  = totals.sum()
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Avg / Month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    compactCurrency(avg),
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Total (6 mo)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    compactCurrency(total),
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }

                        // Spending insights
                        val curSpend  = state.currentTotals?.expense  ?: 0.0
                        val prevSpend = state.previousTotals?.expense ?: 0.0
                        val momPct    = if (prevSpend > 0) ((curSpend - prevSpend) / prevSpend) * 100.0 else 0.0
                        val isTrending = momPct <= 0

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Spending Insights",
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onBackground,
                                modifier   = Modifier.padding(bottom = Spacing.md),
                            )
                            InsightRow(label = "This month", value = formatCurrency(curSpend))
                            InsightRow(
                                label      = "vs Last month",
                                value      = "${if (momPct >= 0) "+" else ""}${"%.1f".format(momPct)}%",
                                valueColor = if (momPct <= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                icon       = if (isTrending) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                iconColor  = if (isTrending) Color(0xFF10B981) else Color(0xFFEF4444),
                            )
                            if (state.feeTotal > 0) {
                                InsightRow(label = "Service charges", value = formatCurrency(state.feeTotal))
                            }
                            if (state.uncategorized > 0) {
                                InsightRow(
                                    label      = "Uncategorized",
                                    value      = "${state.uncategorized} txns",
                                    valueColor = Color(0xFFF59E0B),
                                )
                            }
                        }

                        // Category breakdown
                        if (state.categoryTotals.isNotEmpty()) {
                            SectionHeader(label = "By Category")
                            val maxCat = state.categoryTotals.maxOfOrNull { it.total } ?: 1.0
                            state.categoryTotals.take(5).forEach { cat ->
                                CategoryBar(
                                    label     = cat.category?.replaceFirstChar { it.uppercase() } ?: "Other",
                                    amount    = cat.total,
                                    maxAmount = maxCat,
                                )
                            }
                        }

                        // Top merchants
                        if (state.topMerchants.isNotEmpty()) {
                            SectionHeader(label = "Top Merchants")
                            state.topMerchants.take(5).forEachIndexed { i, m ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    ) {
                                        Text(
                                            "${i + 1}",
                                            fontSize = 11.sp,
                                            color    = MaterialTheme.colorScheme.onBackground.copy(0.40f),
                                            modifier = Modifier.width(20.dp),
                                        )
                                        Text(
                                            m.merchant ?: "Unknown",
                                            color     = MaterialTheme.colorScheme.onBackground,
                                            style     = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Text(
                                        formatCurrency(m.total),
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(Spacing.bottomNavSafeArea))
                    }
                }
            }
        }
    }
}

// ─── SpendingComparisonCard ───────────────────────────────────────────────────

@Composable
private fun SpendingComparisonCard(
    currentMonthSpend: Double,
    prevMonthSpend:    Double,
) {
    if (currentMonthSpend == 0.0 && prevMonthSpend == 0.0) return

    val maxSpend      = maxOf(currentMonthSpend, prevMonthSpend, 1.0)
    val delta         = currentMonthSpend - prevMonthSpend
    val isOver        = delta > 0
    val currentColor  = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text   = "VS LAST MONTH",
            style  = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
            color  = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.base),
        )

        // Last month bar
        ComparisonRow(
            label    = "Last month",
            ratio    = (prevMonthSpend / maxSpend).toFloat(),
            barColor = MaterialTheme.colorScheme.outline,
            amount   = prevMonthSpend,
            amtColor = MaterialTheme.colorScheme.onSurfaceVariant,
            bold     = false,
        )
        Spacer(Modifier.height(Spacing.sm))
        // This month bar
        ComparisonRow(
            label    = "This month",
            ratio    = (currentMonthSpend / maxSpend).toFloat(),
            barColor = currentColor,
            amount   = currentMonthSpend,
            amtColor = currentColor,
            bold     = true,
        )

        // Delta chip
        if (prevMonthSpend > 0) {
            val chipBg = currentColor.copy(alpha = 0.10f)
            Row(
                modifier = Modifier
                    .padding(top = Spacing.base)
                    .background(chipBg, RoundedCornerShape(50))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = if (isOver) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint     = currentColor,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text  = "${if (isOver) "Spent" else "Saved"} ${formatCurrency(kotlin.math.abs(delta), 0)} ${if (isOver) "more" else "less"} than last month",
                    style = MaterialTheme.typography.bodySmall,
                    color = currentColor,
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label:    String,
    ratio:    Float,
    barColor: Color,
    amount:   Double,
    amtColor: Color,
    bold:     Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text      = label,
            style     = MaterialTheme.typography.bodySmall,
            color     = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier  = Modifier.width(84.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(barColor, RoundedCornerShape(50)),
            )
        }
        Text(
            text       = formatCurrency(amount, 0),
            style      = MaterialTheme.typography.bodySmall,
            color      = amtColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign  = TextAlign.End,
            modifier   = Modifier.width(76.dp),
        )
    }
}

// ─── AnalyticsSummaryCards ────────────────────────────────────────────────────

@Composable
private fun AnalyticsSummaryCards(
    spend:   Double,
    income:  Double,
    net:     Double,
    average: Double,
) {
    val cards = listOf(
        Triple("Spend",   spend,   MaterialTheme.colorScheme.error),
        Triple("Income",  income,  SUCCESS),
        Triple("Net",     net,     if (net >= 0) SUCCESS else MaterialTheme.colorScheme.error),
        Triple("Average", average, MaterialTheme.colorScheme.primary),
    )
    val icons = listOf(
        Icons.Filled.ArrowUpward,
        Icons.Filled.ArrowDownward,
        Icons.Filled.Wallet,
        Icons.Filled.Wallet,
    )
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment     = Alignment.Top,
    ) {
        // 2×2 grid — first column
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            cards.filterIndexed { i, _ -> i % 2 == 0 }.zip(icons.filterIndexed { i, _ -> i % 2 == 0 }).forEach { (card, icon) ->
                val (label, value, color) = card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Text(
                        text     = formatCurrency(kotlin.math.abs(value)),
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                    Text(label, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // Second column
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            cards.filterIndexed { i, _ -> i % 2 == 1 }.zip(icons.filterIndexed { i, _ -> i % 2 == 1 }).forEach { (card, icon) ->
                val (label, value, color) = card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Text(
                        text     = formatCurrency(kotlin.math.abs(value)),
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                    Text(label, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── CategorySpendCard ────────────────────────────────────────────────────────

@Composable
private fun CategorySpendCard(item: CategorySparklineItem) {
    val dotColor = parseHexColor(item.color)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        // Header row: dot + name | amount + pct
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape),
            )
            Text(
                text     = item.category.replaceFirstChar { it.uppercase() },
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = formatCurrency(item.total, 0),
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text  = "${"%.1f".format(item.pctOfTotal)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Bottom row: top merchant | mini sparkline
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (item.topMerchant != null) {
                Text(
                    text     = "Top: ${item.topMerchant}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(end = Spacing.sm),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            MiniSparkline(amounts = item.weeklyAmounts, color = dotColor)
        }
    }
}

@Composable
private fun MiniSparkline(amounts: List<Double>, color: Color) {
    val maxAmt = amounts.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Row(
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(28.dp),
    ) {
        amounts.forEach { amt ->
            val pct = (amt / maxAmt).coerceIn(0.0, 1.0).toFloat()
            val barH = if (amt > 0) (pct * 28).coerceAtLeast(3f) else 1f
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(barH.dp)
                    .background(
                        if (amt > 0) color else color.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

// ─── FeesCard ─────────────────────────────────────────────────────────────────

@Composable
private fun FeesCard(fees: AnalyticsFeesData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.sm),
        ) {
            Icon(
                Icons.Filled.Receipt,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text  = "TRANSACTION FEES",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text     = formatCurrency(fees.total, 0),
            style    = MaterialTheme.typography.headlineSmall,
            color    = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = Spacing.sm),
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom,
        ) {
            if (fees.topCategory != null) {
                Column {
                    Text("Top category", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        fees.topCategory.replaceFirstChar { it.uppercase() },
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Avg fee", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${formatCurrency(fees.avgFee)} · ${fees.txCount} tx",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ─── Monthly bar chart (Canvas) ───────────────────────────────────────────────

@Composable
private fun MonthlyBarChart(
    bars:     List<MonthBar>,
    onBarTap: (MonthBar) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val maxVal  = bars.maxOfOrNull { it.expense } ?: 1.0

    val anims = remember(bars.size) { bars.map { Animatable(0f) } }
    LaunchedEffect(bars) {
        anims.forEachIndexed { i, anim ->
            anim.animateTo(
                targetValue   = (bars[i].expense / maxVal).toFloat().coerceIn(0.02f, 1f),
                animationSpec = tween(durationMillis = 400, delayMillis = i * 60),
            )
        }
    }

    Row(
        modifier              = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.Bottom,
    ) {
        bars.forEachIndexed { i, bar ->
            val fillFraction      = anims.getOrNull(i)?.value ?: 0f
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = { onBarTap(bar) }),
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    val barW = size.width * 0.55f
                    val barH = size.height * fillFraction
                    val x    = (size.width - barW) / 2f
                    val y    = size.height - barH
                    drawRoundRect(
                        color        = primary.copy(0.85f),
                        topLeft      = Offset(x, y),
                        size         = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
                Text(
                    bar.label,
                    fontSize   = 9.sp,
                    color      = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

// ─── Insight row ──────────────────────────────────────────────────────────────

@Composable
private fun InsightRow(
    label:      String,
    value:      String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
    icon:       androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconColor:  Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(0.60f),
            style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconColor,
                    modifier = Modifier.size(16.dp))
            }
            Text(value, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

// ─── Category bar (Insights tab) ─────────────────────────────────────────────

@Composable
private fun CategoryBar(
    label:     String,
    amount:    Double,
    maxAmount: Double,
) {
    val primary = MaterialTheme.colorScheme.primary
    val pct     = (amount / maxAmount).toFloat().coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground)
            Text(compactCurrency(amount), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Box(modifier = Modifier.fillMaxWidth().height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)) {
            Box(modifier = Modifier.fillMaxWidth(pct).height(6.dp)
                .background(primary, MaterialTheme.shapes.extraSmall))
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Parse a "#RRGGBB" hex string to Compose Color. Falls back to gray. */
private fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF6B7280)
}

/** formatCurrency overload without decimals argument going to the utility. */
private fun formatCurrency(amount: Double, decimals: Int): String =
    formatCurrency(amount)  // delegates to util; decimals ignored (util uses 2)
