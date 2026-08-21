package com.belinze.lifeos.ui.screen.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.AnalyticsFeesData
import com.belinze.lifeos.viewmodel.AnalyticsRange
import com.belinze.lifeos.viewmodel.AnalyticsTab
import com.belinze.lifeos.viewmodel.CategorySparklineItem
import com.belinze.lifeos.viewmodel.InsightsTrend
import com.belinze.lifeos.viewmodel.InsightsViewModel
import com.belinze.lifeos.viewmodel.MonthBar
import com.belinze.lifeos.viewmodel.PaydayPulse
import com.belinze.lifeos.viewmodel.SizeBreakdown
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// InsightsScreen — 1:1 port of AnalyticsScreen.tsx (tab bar) +
//   InsightsTab.tsx (Insights content) + AnalyticsScreen.tsx Analytics content
// ─────────────────────────────────────────────────────────────────────────────

private val GOOD    = Color(0xFF22C55E)
private val BAD     = Color(0xFFEF4444)
private val SUCCESS = Color(0xFF7BC47B)

@Composable
fun InsightsScreen(
    navController: NavHostController,
    viewModel:     InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Accordion state lives in the screen (pure UI state)
    var expandedMonthKey    by remember { mutableStateOf<String?>(null) }
    // Show-more toggles for category sparklines and month history
    var showAllCategories   by remember { mutableStateOf(false) }
    var showAllMonths       by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.bottomNavSafeArea),
        ) {
            // ── Tab bar ───────────────────────────────────────────────────────
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.base),
            ) {
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

            when (state.activeTab) {
                // ── Analytics tab ─────────────────────────────────────────────
                AnalyticsTab.Analytics -> {
                    // Date range chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.base),
                    ) {
                        listOf(AnalyticsRange.ThisWeek, AnalyticsRange.ThisMonth).forEach { range ->
                            val selected = state.dateRange == range
                            FilterChip(
                                selected = selected,
                                onClick  = { viewModel.setDateRange(range) },
                                label    = {
                                    Text(
                                        if (range == AnalyticsRange.ThisWeek) "This week" else "This month",
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

                    // Uncategorized nudge
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
                                ) { navController.navigate(Route.CATEGORIZE) }
                                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingFlat,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text     = "${state.uncategorizedCount} transactions (${formatCurrency(state.uncategorizedAmount, decimals = 0)}) uncategorized — charts may be incomplete",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick  = { viewModel.dismissNudge() },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Dismiss",
                                    tint     = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Spending comparison card
                    SpendingComparisonCard(
                        currentMonthSpend = state.currentMonthSpend,
                        prevMonthSpend    = state.prevMonthSpend,
                    )

                    // Summary 2x2 grid
                    if (state.totalSpend > 0 || state.totalIncome > 0) {
                        Spacer(Modifier.height(Spacing.base))
                        AnalyticsSummaryCards(
                            spend   = state.totalSpend,
                            income  = state.totalIncome,
                            net     = state.net,
                            average = state.averageTransaction,
                        )
                    }

                    // Category spend cards — first 5, expand via Show more
                    if (state.categorySparklines.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.base))
                        Text(
                            "SPENDING BY CATEGORY",
                            fontSize      = 12.sp,
                            fontWeight    = FontWeight.SemiBold,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            modifier      = Modifier.padding(vertical = Spacing.sm),
                        )
                        val visibleCategories = if (showAllCategories) {
                            state.categorySparklines
                        } else {
                            state.categorySparklines.take(5)
                        }
                        visibleCategories.forEach { item ->
                            CategorySpendCard(item = item)
                            Spacer(Modifier.height(Spacing.sm))
                        }
                        if (state.categorySparklines.size > 5) {
                            TextButton(
                                onClick  = { showAllCategories = !showAllCategories },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text(
                                    if (showAllCategories) {
                                        "Show less"
                                    } else {
                                        "Show ${state.categorySparklines.size - 5} more"
                                    },
                                )
                            }
                        }
                    }

                    // Fees card
                    if (state.feesData.total > 0) {
                        Spacer(Modifier.height(Spacing.sm))
                        FeesCard(fees = state.feesData)
                    }
                }

                // ── Insights tab ──────────────────────────────────────────────
                AnalyticsTab.Insights -> {
                    val months      = state.monthBars
                    val hasData     = months.any { it.expense > 0 }
                    val avgExpense  = state.avgExpense

                    if (!hasData && !state.isLoading) {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No spending history yet — add transactions to unlock patterns.",
                                style     = MaterialTheme.typography.bodyLarge,
                                color     = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }
                    } else {
                        // 1. 6-Month Bar Chart
                        if (months.isNotEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "MONTHLY TREND",
                                    style         = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier      = Modifier.padding(bottom = Spacing.base),
                                )
                                InsightsBarChart(
                                    months    = months,
                                    avgExpense = avgExpense,
                                    onBarTap  = { bar ->
                                        if (bar.txCount > 0) {
                                            navController.navigate(NavTo.monthlyWrapped(bar.monthOffset))
                                        }
                                    },
                                )
                                // Legend footer
                                Row(
                                    modifier              = Modifier.padding(top = Spacing.sm),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                                    verticalAlignment     = Alignment.CenterVertically,
                                ) {
                                    LegendDot(color = GOOD, label = "Below avg")
                                    LegendDot(color = BAD,  label = "Above avg")
                                    Text(
                                        "Tap → Wrapped",
                                        fontSize  = 10.sp,
                                        color     = MaterialTheme.colorScheme.outline,
                                        fontStyle = FontStyle.Italic,
                                    )
                                }
                            }

                            Spacer(Modifier.height(Spacing.base))
                        }

                        // 2. Summary tiles (2 side by side)
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                        ) {
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Outlined.BarChart,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    "Average Monthly",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.xs),
                                )
                                Text(
                                    formatCurrency(state.avgExpense, decimals = 0),
                                    style      = MaterialTheme.typography.titleMedium,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.padding(top = 2.dp),
                                )
                            }
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Outlined.Wallet,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    "Total Tracked",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.xs),
                                )
                                Text(
                                    formatCurrency(state.totalTracked, decimals = 0),
                                    style      = MaterialTheme.typography.titleMedium,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.padding(top = 2.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(Spacing.base))

                        // 3. Spending Insights card
                        val trendColor = when (state.trend) {
                            InsightsTrend.Increasing -> BAD
                            InsightsTrend.Decreasing -> GOOD
                            InsightsTrend.Stable     -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val trendIcon = when (state.trend) {
                            InsightsTrend.Increasing -> Icons.AutoMirrored.Filled.TrendingUp
                            InsightsTrend.Decreasing -> Icons.AutoMirrored.Filled.TrendingDown
                            InsightsTrend.Stable     -> Icons.AutoMirrored.Filled.TrendingFlat
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            // Card header
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                modifier = Modifier.padding(bottom = Spacing.base),
                            ) {
                                IconBox(
                                    icon     = Icons.Outlined.Lightbulb,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    bgColor  = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                                )
                                Text(
                                    "Spending Insights",
                                    style      = MaterialTheme.typography.titleSmall,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            // Highest month (pressable)
                            state.highestMonth?.let { highest ->
                                InsightRow(
                                    icon      = Icons.Outlined.BarChart,
                                    iconTint  = BAD,
                                    bgColor   = BAD.copy(0.13f),
                                    label     = "Highest Month",
                                    value     = "${highest.fullLabel} · ${formatCurrency(highest.expense, decimals = 0)}",
                                    pressable = true,
                                    onClick   = { navController.navigate(NavTo.monthlyWrapped(highest.monthOffset)) },
                                )
                            }

                            // Lowest month (pressable, if exists)
                            state.lowestMonthWithData?.let { lowest ->
                                HorizontalDivider()
                                InsightRow(
                                    icon      = Icons.AutoMirrored.Filled.TrendingDown,
                                    iconTint  = GOOD,
                                    bgColor   = GOOD.copy(0.13f),
                                    label     = "Lowest Month",
                                    value     = "${lowest.fullLabel} · ${formatCurrency(lowest.expense, decimals = 0)}",
                                    pressable = true,
                                    onClick   = { navController.navigate(NavTo.monthlyWrapped(lowest.monthOffset)) },
                                )
                            }

                            // Top category
                            state.topCategoryAllTime?.let { (cat, pct) ->
                                HorizontalDivider()
                                val catColor = parseHexColor(InsightsViewModel.categoryColor(cat))
                                InsightRow(
                                    icon      = Icons.Outlined.LocalOffer,
                                    iconTint  = catColor,
                                    bgColor   = catColor.copy(0.13f),
                                    label     = "Top Category",
                                    value     = "${cat.replaceFirstChar { it.uppercase() }} · ${"%.1f".format(pct)}%",
                                    pressable = false,
                                )
                            }

                            // Trend
                            HorizontalDivider()
                            InsightRow(
                                icon       = trendIcon,
                                iconTint   = trendColor,
                                bgColor    = trendColor.copy(0.13f),
                                label      = "Trend",
                                value      = state.trend.name.replaceFirstChar { it.uppercase() },
                                valueColor = trendColor,
                                pressable  = false,
                            )
                        }

                        Spacer(Modifier.height(Spacing.base))

                        // 4. History Accordion — first 3 months, expand via Show more
                        Text(
                            "History",
                            style      = MaterialTheme.typography.titleSmall,
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(horizontal = 2.dp, vertical = Spacing.sm),
                        )

                        val visibleMonths = if (showAllMonths) {
                            state.monthBreakdown
                        } else {
                            state.monthBreakdown.take(3)
                        }
                        visibleMonths.forEach { m ->
                            val isExpanded = expandedMonthKey == m.monthKey
                            val deltaColor = when {
                                m.delta == null || m.delta == 0.0 -> MaterialTheme.colorScheme.outlineVariant
                                m.delta > 0                       -> BAD
                                else                              -> GOOD
                            }
                            val deltaIcon = when {
                                m.delta == null || m.delta == 0.0 -> Icons.AutoMirrored.Filled.TrendingFlat
                                m.delta > 0                       -> Icons.AutoMirrored.Filled.TrendingUp
                                else                              -> Icons.AutoMirrored.Filled.TrendingDown
                            }

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                // Header row (always visible, pressable)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            expandedMonthKey = if (isExpanded) null else m.monthKey
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                ) {
                                    // Trend icon box (36x36)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(deltaColor.copy(0.13f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            deltaIcon,
                                            contentDescription = null,
                                            tint     = deltaColor,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                        ) {
                                            Text(
                                                m.fullLabel,
                                                style      = MaterialTheme.typography.titleSmall,
                                                color      = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            if (m.delta != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(deltaColor.copy(0.13f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        "${if (m.delta > 0) "+" else ""}${"%.1f".format(m.delta)}%",
                                                        fontSize   = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color      = deltaColor,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                formatCurrency(m.expense, decimals = 0),
                                                style      = MaterialTheme.typography.titleSmall,
                                                color      = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Text(
                                            "${m.txCount} transaction${if (m.txCount != 1) "s" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }

                                // Expanded content
                                AnimatedVisibility(
                                    visible = isExpanded && m.topCategories.isNotEmpty(),
                                    enter   = expandVertically(),
                                    exit    = shrinkVertically(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = Spacing.base),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant),
                                        )
                                        Spacer(Modifier.height(Spacing.base))
                                        Text(
                                            "TOP CATEGORIES",
                                            fontSize      = 10.sp,
                                            fontWeight    = FontWeight.SemiBold,
                                            color         = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.5.sp,
                                            modifier      = Modifier.padding(bottom = Spacing.sm),
                                        )
                                        m.topCategories.forEach { cat ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = Spacing.sm),
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                            ) {
                                                Text(
                                                    cat.category.replaceFirstChar { it.uppercase() },
                                                    style    = MaterialTheme.typography.bodySmall,
                                                    color    = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.width(110.dp),
                                                )
                                                val catColor = parseHexColor(InsightsViewModel.categoryColor(cat.category))
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(6.dp)
                                                        .background(
                                                            Color.White.copy(alpha = 0.08f),
                                                            RoundedCornerShape(3.dp),
                                                        ),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(minOf(cat.pct / 100f, 1.0).toFloat())
                                                            .height(6.dp)
                                                            .background(catColor, RoundedCornerShape(3.dp)),
                                                    )
                                                }
                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    modifier = Modifier.width(72.dp),
                                                ) {
                                                    Text(
                                                        formatCurrency(cat.amount, decimals = 0),
                                                        style      = MaterialTheme.typography.bodySmall,
                                                        color      = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    Text(
                                                        "${cat.pct.toInt()}%",
                                                        fontSize = 9.sp,
                                                        color    = MaterialTheme.colorScheme.outline,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(Spacing.base))
                        }
                        if (state.monthBreakdown.size > 3) {
                            TextButton(
                                onClick  = { showAllMonths = !showAllMonths },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (showAllMonths) {
                                        "Show less"
                                    } else {
                                        "Show ${state.monthBreakdown.size - 3} more months"
                                    },
                                )
                            }
                            Spacer(Modifier.height(Spacing.sm))
                        }

                        // 5. Payday Pulse
                        state.paydayPulse?.let { pulse ->
                            PaydayPulseCard(pulse = pulse)
                            Spacer(Modifier.height(Spacing.base))
                        }

                        // 6. Spend Anatomy
                        val sb = state.sizeBreakdown
                        val totalCount = sb.microCount + sb.mediumCount + sb.largeCount
                        if (totalCount > 0) {
                            SpendAnatomyCard(sb = sb, totalCount = totalCount)
                        }
                    }
                }
            }
        }
    }
}

// ─── Analytics tab components ─────────────────────────────────────────────────

@Composable
private fun SpendingComparisonCard(
    currentMonthSpend: Double,
    prevMonthSpend:    Double,
) {
    if (currentMonthSpend == 0.0 && prevMonthSpend == 0.0) return
    val maxSpend     = maxOf(currentMonthSpend, prevMonthSpend, 1.0)
    val delta        = currentMonthSpend - prevMonthSpend
    val isOver       = delta > 0
    val currentColor = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "VS LAST MONTH",
            style    = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.base),
        )
        ComparisonRow(
            label    = "Last month",
            ratio    = (prevMonthSpend / maxSpend).toFloat(),
            barColor = MaterialTheme.colorScheme.outline,
            amount   = prevMonthSpend,
            amtColor = MaterialTheme.colorScheme.onSurfaceVariant,
            bold     = false,
        )
        Spacer(Modifier.height(Spacing.sm))
        ComparisonRow(
            label    = "This month",
            ratio    = (currentMonthSpend / maxSpend).toFloat(),
            barColor = currentColor,
            amount   = currentMonthSpend,
            amtColor = currentColor,
            bold     = true,
        )
        if (prevMonthSpend > 0) {
            Row(
                modifier = Modifier
                    .padding(top = Spacing.base)
                    .background(currentColor.copy(alpha = 0.10f), RoundedCornerShape(50.dp))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    if (isOver) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint     = currentColor,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    "${if (isOver) "Spent" else "Saved"} ${formatCurrency(kotlin.math.abs(delta), decimals = 0)} ${if (isOver) "more" else "less"} than last month",
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
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style    = MaterialTheme.typography.bodySmall,
            color    = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(barColor, RoundedCornerShape(50.dp)),
            )
        }
        Text(
            formatCurrency(amount, decimals = 0),
            style      = MaterialTheme.typography.bodySmall,
            color      = amtColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign  = TextAlign.End,
            modifier   = Modifier.width(76.dp),
        )
    }
}

@Composable
private fun AnalyticsSummaryCards(
    spend:   Double,
    income:  Double,
    net:     Double,
    average: Double,
) {
    val items = listOf(
        Triple("Spend",   spend,   MaterialTheme.colorScheme.error),
        Triple("Income",  income,  SUCCESS),
        Triple("Net",     net,     if (net >= 0) SUCCESS else MaterialTheme.colorScheme.error),
        Triple("Average", average, MaterialTheme.colorScheme.primary),
    )
    val icons = listOf(
        Icons.Outlined.ArrowUpward,
        Icons.Outlined.ArrowDownward,
        Icons.Outlined.Wallet,
        Icons.Outlined.BarChart,
    )
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment     = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items.filterIndexed { i, _ -> i % 2 == 0 }.forEachIndexed { idx, triple ->
                val icon = icons[idx * 2]
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Icon(icon, contentDescription = null,
                        tint = triple.third, modifier = Modifier.size(18.dp))
                    Text(
                        formatCurrency(kotlin.math.abs(triple.second), decimals = 0),
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                    Text(triple.first, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items.filterIndexed { i, _ -> i % 2 == 1 }.forEachIndexed { idx, triple ->
                val icon = icons[idx * 2 + 1]
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Icon(icon, contentDescription = null,
                        tint = triple.third, modifier = Modifier.size(18.dp))
                    Text(
                        formatCurrency(kotlin.math.abs(triple.second), decimals = 0),
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                    Text(triple.first, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CategorySpendCard(item: CategorySparklineItem) {
    val dotColor = parseHexColor(item.color)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
            Text(
                item.category.replaceFirstChar { it.uppercase() },
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(item.total, decimals = 0),
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${"%.1f".format(item.pctOfTotal)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (item.topMerchant != null) {
                Text(
                    "Top: ${item.topMerchant}",
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
            val frac = (amt / maxAmt).coerceIn(0.0, 1.0).toFloat()
            val h    = if (amt > 0) (frac * 28).coerceAtLeast(3f) else 1f
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(h.dp)
                    .background(
                        if (amt > 0) color else color.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@Composable
private fun FeesCard(fees: AnalyticsFeesData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.sm),
        ) {
            Icon(Icons.Outlined.Receipt, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
            Text(
                "TRANSACTION FEES",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatCurrency(fees.total, decimals = 0),
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
                    Text(fees.topCategory.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Avg fee", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatCurrency(fees.avgFee, decimals = 0)} · ${fees.txCount} tx",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ─── Insights tab components ──────────────────────────────────────────────────

@Composable
private fun InsightsBarChart(
    months:    List<MonthBar>,
    avgExpense: Double,
    onBarTap:  (MonthBar) -> Unit,
) {
    val maxVal    = months.maxOfOrNull { it.expense }?.takeIf { it > 0.0 } ?: 1.0
    val BAR_MAX_H = 90.dp

    // One Animatable per bar
    val anims = remember(months.size) { months.map { Animatable(0f) } }
    LaunchedEffect(months) {
        anims.forEach { it.snapTo(0f) }
        // All bars animate simultaneously — mirrors RN's parallel Animated.timing calls
        kotlinx.coroutines.coroutineScope {
            months.forEachIndexed { i, m ->
                launch {
                    anims[i].animateTo(
                        targetValue   = (m.expense / maxVal).toFloat().coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 400),
                    )
                }
            }
        }
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .height(BAR_MAX_H + 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.Bottom,
    ) {
        months.forEachIndexed { i, bar ->
            val isCurrent  = bar.monthOffset == 0
            val barColor   = when {
                bar.expense == 0.0        -> MaterialTheme.colorScheme.outlineVariant
                bar.expense <= avgExpense -> GOOD
                else                      -> BAD
            }
            val fillFraction    = anims.getOrNull(i)?.value ?: 0f
            val interactionSource = remember { MutableInteractionSource() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .let {
                        // BUG-13: zero-spend bars should not be tappable
                        if (bar.expense > 0.0) it.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onBarTap(bar) }
                        else it
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(BAR_MAX_H),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (fillFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fillFraction)
                                .background(
                                    barColor.copy(alpha = if (isCurrent) 0.65f else 1f),
                                    RoundedCornerShape(4.dp),
                                ),
                        )
                    }
                }
                Text(
                    bar.label,
                    fontSize   = 10.sp,
                    lineHeight = 14.sp,
                    color      = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IconBox(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    bgColor:  Color,
    size:     Int = 28,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(bgColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun InsightRow(
    icon:       androidx.compose.ui.graphics.vector.ImageVector,
    iconTint:   Color,
    bgColor:    Color,
    label:      String,
    value:      String,
    pressable:  Boolean,
    onClick:    () -> Unit = {},
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (pressable) {
                    Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                } else {
                    Modifier
                }
            )
            .padding(vertical = Spacing.sm),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        IconBox(icon = icon, iconTint = iconTint, bgColor = bgColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = valueColor)
        }
        if (pressable) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun PaydayPulseCard(pulse: PaydayPulse) {
    val postHigher = pulse.postPaydayAvgPerDay > pulse.otherDaysAvgPerDay
    val maxVal     = maxOf(pulse.postPaydayAvgPerDay, pulse.otherDaysAvgPerDay, 1.0)
    val diffPct    = if (pulse.otherDaysAvgPerDay > 0) {
        kotlin.math.abs((pulse.postPaydayAvgPerDay - pulse.otherDaysAvgPerDay) / pulse.otherDaysAvgPerDay) * 100.0
    } else {
        0.0
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.base),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFF59E0B).copy(0.13f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.ElectricBolt, contentDescription = null,
                    tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
            }
            Text("Payday Pulse", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        Text(
            "${pulse.incomeEventsCount} income event${if (pulse.incomeEventsCount != 1) "s" else ""} · avg daily spend",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        Text(
            if (postHigher) {
                "You spend ${"%.0f".format(diffPct)}% more in the 7 days after income arrives"
            } else {
                "You spend ${"%.0f".format(diffPct)}% less right after income — disciplined!"
            },
            style      = MaterialTheme.typography.bodyMedium,
            color      = if (postHigher) BAD else GOOD,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = Spacing.base),
        )
        listOf(
            Triple("Post-income (7 days)", pulse.postPaydayAvgPerDay, if (postHigher) BAD else GOOD),
            Triple("Other days",           pulse.otherDaysAvgPerDay,  MaterialTheme.colorScheme.primary),
        ).forEach { (label, value, color) ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatCurrency(value, decimals = 0)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((value / maxVal).toFloat().coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(color, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendAnatomyCard(sb: SizeBreakdown, totalCount: Int) {
    val totalAmt = sb.microTotal + sb.mediumTotal + sb.largeTotal
    val tiers = listOf(
        Triple("Micro",  "< KSh 500",  Triple(sb.microCount,  sb.microTotal,  GOOD)),
        Triple("Medium", "KSh 500–2k", Triple(sb.mediumCount, sb.mediumTotal, Color(0xFFF59E0B))),
        Triple("Large",  "> KSh 2k",   Triple(sb.largeCount,  sb.largeTotal,  BAD)),
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.base),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF8B5CF6).copy(0.13f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Layers, contentDescription = null,
                    tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
            }
            Text("Spend Anatomy", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        Text(
            "How your $totalCount transactions break down by size",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.base),
        )
        tiers.forEach { (label, range, data) ->
            val (count, total, color) = data
            val pctOfCount = if (totalCount > 0) (count / totalCount.toFloat()) * 100f else 0f
            val pctOfAmt   = if (totalAmt > 0) (total / totalAmt * 100).toInt() else 0
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                        Text(
                            "$label ($range)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        "$count txns · $pctOfAmt% of spend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((pctOfCount / 100f).coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(color, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF6B7280)
}
