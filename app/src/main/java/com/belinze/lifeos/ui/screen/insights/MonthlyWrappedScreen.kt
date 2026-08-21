package com.belinze.lifeos.ui.screen.insights

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.theme.AppBarDimens
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
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) { viewModel.setMonthOffset(initialMonthOffset) }

    // Page gradient — matches PageScaffold's background brush
    val bgGradient: Brush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0A0B), Color(0xFF0D1117), Color(0xFF0A0A0B)),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE8EDF3), Color(0xFFDDE4EE), Color(0xFFE8EDF3)),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(bgGradient) }
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // ── Single combined header row ─────────────────────────────────────────
        // React layout: [back ←] [‹ month] [title — flex, centered] [month ›]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppBarDimens.height)
                .padding(horizontal = AppBarDimens.horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // System back button
            IconButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.size(AppBarDimens.backBtnSize),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint     = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(AppBarDimens.iconSize),
                )
            }

            // Older month (chevron, not a back arrow)
            IconButton(
                onClick  = { viewModel.setMonthOffset(state.monthOffset - 1) },
                enabled  = state.monthOffset > state.minMonthOffset,
                modifier = Modifier.size(AppBarDimens.backBtnSize),
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Older month",
                    tint     = if (state.monthOffset > state.minMonthOffset) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(22.dp),
                )
            }

            // Centered title
            Text(
                text      = "${state.monthLabel} Wrapped",
                style     = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                modifier  = Modifier.weight(1f),
            )

            // Newer month
            IconButton(
                onClick  = { viewModel.setMonthOffset(state.monthOffset + 1) },
                enabled  = state.monthOffset < 0,
                modifier = Modifier.size(AppBarDimens.backBtnSize),
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowRight,
                    contentDescription = "Newer month",
                    tint     = if (state.monthOffset < 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // ── Content ──────────────────────────────────────────────────────────
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(Spacing.base))
                        Text(
                            "Crunching the numbers…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            state.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(Spacing.screenHorizontal),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.error!!,
                        color     = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            !state.hasData -> {
                Box(
                    Modifier.fillMaxSize().padding(Spacing.screenHorizontal),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No spending recorded for ${state.monthLabel} yet.",
                            style     = MaterialTheme.typography.bodyLarge,
                            color     = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.screenHorizontal),
                    contentPadding      = PaddingValues(top = Spacing.sm, bottom = Spacing.bottomNavSafeArea),
                    verticalArrangement = Arrangement.spacedBy(Spacing.base),
                ) {
                    // ─ Hero ─
                    item {
                        GlassCard {
                            Column(
                                modifier            = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "You spent",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text       = formatCurrency(state.totalSpend),
                                    fontSize   = 44.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 50.sp,
                                    color      = MaterialTheme.colorScheme.primary,
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
                                Text(
                                    "TOP CATEGORIES",
                                    style         = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier      = Modifier.padding(bottom = Spacing.base),
                                )
                                state.topCategories.forEachIndexed { i, row ->
                                    if (i > 0) {
                                        HorizontalDivider(
                                        modifier  = Modifier.padding(vertical = Spacing.xs),
                                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    )
                                    }
                                    CategoryRow(row)
                                }
                            }
                        }
                    }

                    // ─ Top Merchant + Biggest Spend (half-cards) ─
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            // Top merchant
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Text(
                                    "TOP MERCHANT",
                                    style    = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = Spacing.sm),
                                )
                                Text(
                                    text       = state.topMerchantName.ifBlank { "—" },
                                    style      = MaterialTheme.typography.titleMedium,
                                    color      = MaterialTheme.colorScheme.onSurface,
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
                                Text(
                                    "BIGGEST SPEND",
                                    style    = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = Spacing.sm),
                                )
                                Text(
                                    text       = formatCurrency(state.biggestAmount),
                                    style      = MaterialTheme.typography.titleMedium,
                                    color      = MaterialTheme.colorScheme.onSurface,
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
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Text(
                                    "ACTIVE DAYS",
                                    style    = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = Spacing.sm),
                                )
                                Text(
                                    text       = "${state.activeDays}",
                                    style      = MaterialTheme.typography.headlineSmall,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text  = "of ${state.totalDaysInMonth} days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.feesTotal > 0.0) {
                                GlassCard(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "FEES PAID",
                                        style    = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = Spacing.sm),
                                    )
                                    Text(
                                        text       = formatCurrency(state.feesTotal),
                                        style      = MaterialTheme.typography.headlineSmall,
                                        color      = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text  = "M-Pesa charges",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                Text(
                                    "FULIZA USED",
                                    style    = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                    color    = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = Spacing.sm),
                                )
                                Text(
                                    text       = "${state.fulizaCount} time${if (state.fulizaCount != 1) "s" else ""} · ${formatCurrency(state.fulizaTotal)}",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = COLOR_FULIZA,
                                )
                                Text(
                                    text  = "Try to keep this below 3 times per month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // ─ Savings verdict (conditional, only if income > 0) ─
                    if (state.totalIncome > 0.0) {
                        item {
                            val saved    = state.totalIncome - state.totalSpend
                            val isSaving = saved >= 0
                            LeftAccentCard(color = if (isSaving) COLOR_SUCCESS else COLOR_DANGER) {
                                Text(
                                    text  = if (isSaving) "SAVED THIS MONTH" else "SPENT OVER INCOME",
                                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = Spacing.sm),
                                )
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
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text       = rankLabel,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = rankColor,
                modifier   = Modifier.width(28.dp),
            )
            Text(
                text  = row.category.replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text       = formatCurrency(row.total),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ─── Left accent card helper ──────────────────────────────────────────────────

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
                drawRect(
                    color = color,
                    size  = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height),
                )
            },
    ) {
        Box(modifier = Modifier.padding(start = 3.dp)) {
            GlassCard(content = content)
        }
    }
}
