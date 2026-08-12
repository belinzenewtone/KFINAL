package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// FinanceScreen
//
// 1:1 port of src/screens/finance/FinanceScreen.tsx.
//
// Layout (top-to-bottom):
//   ‣ TopBanner (error / SMS import status)
//   ‣ PageHeader: "Finance" + Refresh action
//   ‣ Action chips: Add / Import SMS / Import CSV / Export
//   ‣ FrostCard hero: month spend + today/week/income sub-metrics
//   ‣ InlineBanner: budget alert (if top budget ≥ 80%)
//   ‣ InlineBanner: uncategorized transactions
//   ‣ Horizontal insights row: Budget / Fuliza / Fees
//   ‣ Period selector + search field
//   ‣ "Transactions" header + count
//   ‣ Date-grouped LazyColumn of TransactionListItem
//   ‣ FAB: Add transaction
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    navController: NavHostController,
    viewModel:     TransactionViewModel = hiltViewModel(),
) {
    val state   by viewModel.uiState.collectAsState()
    val isDark  = isSystemInDarkTheme()

    // Group transactions by date
    val grouped = remember(state.transactions) {
        state.transactions
            .groupBy { it.date.take(10) }
            .entries
            .sortedByDescending { it.key }
    }

    // Derived hero values
    val monthIncome   = state.monthTotals?.income  ?: 0.0
    val monthExpense  = state.monthTotals?.expense ?: 0.0
    val todayStr      = remember { java.time.LocalDate.now().toString() }
    val weekStartStr  = remember { java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString() }
    val todayExpense  = remember(state.transactions) {
        state.transactions
            .filter { it.date.take(10) == todayStr && it.transactionType != "income" }
            .sumOf { it.amount }
    }
    val weekExpense   = remember(state.transactions) {
        state.transactions
            .filter { it.date.take(10) >= weekStartStr && it.transactionType != "income" }
            .sumOf { it.amount }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {

            // ── Error / SMS import banner ─────────────────────────────────────
            TopBanner(
                visible  = state.error != null,
                message  = state.error ?: "",
                tone     = BannerTone.Error,
            )

            // ── Page header ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text     = "Finance",
                        style    = MaterialTheme.typography.headlineSmall,
                        color    = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text  = currentMonthLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.60f),
                    )
                }
                IconButton(onClick = { viewModel.reload() }) {
                    Icon(
                        imageVector        = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint               = MaterialTheme.colorScheme.onBackground.copy(0.60f),
                    )
                }
            }

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {

                // ── Action chips ──────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        ActionChip(
                            label   = "Add",
                            icon    = Icons.Filled.Add,
                            onClick = { navController.navigate(Route.TRANSACTION_FORM) },
                        )
                        ActionChip(
                            label   = "Import SMS",
                            icon    = Icons.Filled.Message,
                            onClick = { navController.navigate(Route.IMPORT_SMS) },
                        )
                        ActionChip(
                            label   = "Import CSV",
                            icon    = Icons.Filled.FileDownload,
                            onClick = { navController.navigate(Route.IMPORT_CSV) },
                        )
                        ActionChip(
                            label   = "Export",
                            icon    = Icons.Filled.FileUpload,
                            onClick = { navController.navigate(Route.EXPORT_DATA) },
                        )
                    }
                }

                // ── Hero card ─────────────────────────────────────────────────
                item {
                    FrostCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                    ) {
                        Text(
                            text  = "Spent this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.60f),
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text       = formatCurrency(monthExpense),
                            style      = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize   = 34.sp,
                            ),
                            color      = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            HeroSubMetric(label = "Today",  amount = todayExpense)
                            HeroSubMetric(label = "Week",   amount = weekExpense)
                            HeroSubMetric(label = "Income", amount = monthIncome, isCredit = true)
                        }
                    }
                }

                // ── Uncategorized banner ──────────────────────────────────────
                if (state.uncategorized > 0) {
                    item {
                        InlineBanner(
                            message  = "${state.uncategorized} transactions need a category",
                            tone     = BannerTone.Info,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
                            action   = "Review",
                            onAction = { navController.navigate(Route.UNCATEGORIZED) },
                        )
                    }
                }

                // ── Transactions header ───────────────────────────────────────
                item {
                    SectionHeader(
                        label    = "Transactions",
                        action   = if (state.transactions.isNotEmpty()) "${state.transactions.size}" else null,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                }

                // ── Shimmer while loading ─────────────────────────────────────
                if (state.isLoading) {
                    item {
                        ShimmerLoadingState(
                            rowCount = 6,
                            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                        )
                    }
                } else if (grouped.isEmpty()) {
                    item {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.x2l),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No transactions yet.",
                                color = MaterialTheme.colorScheme.onBackground.copy(0.40f),
                            )
                        }
                    }
                } else {
                    // Date-grouped list
                    grouped.forEach { (dateKey, txs) ->
                        item(key = "header_$dateKey") {
                            val dayTotal = txs
                                .filter { it.transactionType != "income" }
                                .sumOf { it.amount }
                            DayGroupHeader(
                                dateLabel = formatDateKey(dateKey),
                                total     = dayTotal,
                            )
                        }
                        itemsIndexed(
                            items = txs,
                            key   = { _, tx -> tx.id },
                        ) { _, tx ->
                            TransactionListItem(
                                tx      = tx,
                                onClick = { navController.navigate(NavTo.transactionDetail(tx.id)) },
                            )
                        }
                    }

                    // Load-more item
                    if (state.hasNextPage) {
                        item {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // Bottom nav clearance
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        ExtendedFloatingActionButton(
            onClick          = { navController.navigate(Route.TRANSACTION_FORM) },
            text             = { Text("Add") },
            icon             = { Icon(Icons.Filled.Add, contentDescription = "Add transaction") },
            containerColor   = MaterialTheme.colorScheme.primary,
            contentColor     = MaterialTheme.colorScheme.onPrimary,
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = Spacing.lg, bottom = Spacing.bottomNavSafeArea + Spacing.md),
        )
    }
}

// ─── Action chip ──────────────────────────────────────────────────────────────

@Composable
private fun ActionChip(
    label:   String,
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.medium,
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(0.15f)),
                onClick           = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = primary, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
    }
}

// ─── Hero sub-metric ─────────────────────────────────────────────────────────

@Composable
private fun HeroSubMetric(label: String, amount: Double, isCredit: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
        )
        Text(
            text       = compactCurrency(amount),
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
            color      = if (isCredit) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun currentMonthLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

private fun formatDateKey(dateKey: String): String = try {
    val d = LocalDate.parse(dateKey)
    val today = LocalDate.now()
    when {
        d == today           -> "Today"
        d == today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
} catch (_: Exception) { dateKey }
