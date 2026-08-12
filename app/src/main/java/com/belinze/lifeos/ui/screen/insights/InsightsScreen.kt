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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.InsightsViewModel
import com.belinze.lifeos.viewmodel.MonthBar

// ─────────────────────────────────────────────────────────────────────────────
// InsightsScreen — analytics hub.
//
// Matches InsightsTab.tsx structure:
//   ‣ 6-Month animated bar chart (monthly spend, tappable → MonthlyWrapped)
//   ‣ Summary tiles (avg monthly, total tracked)
//   ‣ Spending Insights card (highest / lowest month, top category, trend)
//   ‣ Category breakdown (top 5 from current month)
//   ‣ Top merchants list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InsightsScreen(
    navController: NavHostController,
    viewModel:     InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        eyebrow = "Analytics",
        title   = "Insights",
        onBack  = { navController.popBackStack() },
    ) {
        if (state.isLoading) {
            ShimmerLoadingState(rowCount = 6)
            return@PageScaffold
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {

            // ── 6-Month bar chart ─────────────────────────────────────────
            if (state.monthBars.isNotEmpty()) {
                FrostCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Monthly Trend",
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                        modifier   = Modifier.padding(bottom = Spacing.md),
                    )
                    MonthlyBarChart(
                        bars          = state.monthBars,
                        onBarTap      = { bar ->
                            val offset = state.monthBars.size - 1 - state.monthBars.indexOf(bar)
                            navController.navigate(NavTo.monthlyWrapped(offset))
                        },
                    )
                }
            }

            // ── Summary tiles ─────────────────────────────────────────────
            val totals = state.monthBars.map { it.expense }
            val avg    = if (totals.isEmpty()) 0.0 else totals.average()
            val total  = totals.sum()
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FrostCard(modifier = Modifier.weight(1f)) {
                    Text("Avg / Month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    Spacer(Modifier.height(4.dp))
                    Text(compactCurrency(avg),
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground)
                }
                FrostCard(modifier = Modifier.weight(1f)) {
                    Text("Total (6 mo)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    Spacer(Modifier.height(4.dp))
                    Text(compactCurrency(total),
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground)
                }
            }

            // ── Spending insights ─────────────────────────────────────────
            val curSpend  = state.currentTotals?.expense  ?: 0.0
            val prevSpend = state.previousTotals?.expense ?: 0.0
            val momPct    = if (prevSpend > 0) ((curSpend - prevSpend) / prevSpend) * 100.0 else 0.0
            val isTrending = momPct <= 0  // spending less = good trend

            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Text("Spending Insights",
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.padding(bottom = Spacing.md))

                InsightRow(
                    label = "This month",
                    value = formatCurrency(curSpend),
                )
                InsightRow(
                    label = "vs Last month",
                    value = "${if (momPct >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", momPct)}%",
                    valueColor = if (momPct <= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    icon  = if (isTrending) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                    iconColor = if (isTrending) Color(0xFF10B981) else Color(0xFFEF4444),
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

            // ── Category breakdown ────────────────────────────────────────
            if (state.categoryTotals.isNotEmpty()) {
                SectionHeader(label = "By Category")
                val maxCat = state.categoryTotals.maxOfOrNull { it.total } ?: 1.0
                state.categoryTotals.take(5).forEach { cat ->
                    CategoryBar(
                        label    = cat.category?.replaceFirstChar { it.uppercase() } ?: "Other",
                        amount   = cat.total,
                        maxAmount = maxCat,
                    )
                }
            }

            // ── Top merchants ─────────────────────────────────────────────
            if (state.topMerchants.isNotEmpty()) {
                SectionHeader(label = "Top Merchants")
                state.topMerchants.take(5).forEachIndexed { i, m ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text("${i + 1}", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.40f),
                                modifier = Modifier.width(20.dp))
                            Text(m.merchant ?: "Unknown", color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(formatCurrency(m.total),
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
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

    // Animate bars in on composition
    val anims = remember(bars.size) { bars.map { Animatable(0f) } }
    LaunchedEffect(bars) {
        anims.forEachIndexed { i, anim ->
            anim.animateTo(
                targetValue = (bars[i].expense / maxVal).toFloat().coerceIn(0.02f, 1f),
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
            val fillFraction = anims.getOrNull(i)?.value ?: 0f
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier            = Modifier
                    .weight(1f)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = { onBarTap(bar) }),
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    val barW   = size.width * 0.55f
                    val barH   = size.height * fillFraction
                    val x      = (size.width - barW) / 2f
                    val y      = size.height - barH
                    drawRoundRect(
                        color        = primary.copy(0.85f),
                        topLeft      = Offset(x, y),
                        size         = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
                Text(bar.label, fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    textAlign = TextAlign.Center)
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
                    modifier = Modifier.height(16.dp).width(16.dp))
            }
            Text(value, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

// ─── Category bar ─────────────────────────────────────────────────────────────

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
