package com.belinze.lifeos.ui.screen.finance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
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
import com.belinze.lifeos.viewmodel.BudgetViewModel
import com.belinze.lifeos.viewmodel.PlannerViewModel
import com.belinze.lifeos.viewmodel.SmsImportViewModel
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
//   ‣ "Transactions" header + loaded count
//   ‣ Paging 3 LazyColumn of TransactionListItem — date-grouped visually
//     with adaptive corner radius (no wrapping card needed per group)
//   ‣ Load-more spinner while appending next page
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    navController:      NavHostController,
    viewModel:          TransactionViewModel = hiltViewModel(),
    smsImportViewModel: SmsImportViewModel  = hiltViewModel(),
    budgetViewModel:    BudgetViewModel     = hiltViewModel(),
    plannerViewModel:   PlannerViewModel    = hiltViewModel(),
) {
    val state        by viewModel.uiState.collectAsStateWithLifecycle()
    val smsState     by smsImportViewModel.uiState.collectAsStateWithLifecycle()
    val budgetState  by budgetViewModel.uiState.collectAsStateWithLifecycle()
    val plannerState by plannerViewModel.uiState.collectAsStateWithLifecycle()

    // ── Paging 3 — collect once per composition; survives config changes via
    // cachedIn(viewModelScope). refresh() / retry() are called directly on this.
    val pagingItems = viewModel.pagedTransactions.collectAsLazyPagingItems()

    // ── Derived state slices — each only re-triggers its readers when the
    // specific field actually changes (structural equality via data class ==).
    // Paging load-state changes do NOT change these slices, so the hero card,
    // insights row, and filter chips stay composed during long scroll sessions.
    val monthTotals     by remember { derivedStateOf { state.monthTotals } }
    val feeTotal        by remember { derivedStateOf { state.feeTotal } }
    val uncategorized   by remember { derivedStateOf { state.uncategorized } }
    val activeFilters   by remember { derivedStateOf { state.filters } }
    val activeBudgetsDs by remember { derivedStateOf { budgetState.budgets.filter { it.budget.isActive != 0 } } }
    val activeLoansDs   by remember { derivedStateOf { plannerState.loans.filter { it.status == "active" } } }
    val isImporting     by remember { derivedStateOf { smsState.isImporting } }
    val context      = LocalContext.current

    // Reload budgets + transaction metrics whenever Finance resumes (e.g. returning
    // from the Budgets/Categorize screens) so the budget alert, budget card, and
    // uncategorized banner count reflect current data instead of stale values.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            budgetViewModel.load()
            viewModel.refreshMetrics()
        }
    }

    // FI-1: SMS permission check — show banner if READ_SMS not granted
    var smsGranted by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    // BUG-F10: re-check SMS permission on every lifecycle resume (e.g. user grants from OS Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                smsGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val smsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        smsGranted = results[Manifest.permission.READ_SMS] == true ||
                     results[Manifest.permission.RECEIVE_SMS] == true
    }

    // Top budget alert — derived from activeBudgetsDs so it doesn't recompute on scroll
    val alertBudget by remember {
        derivedStateOf {
            activeBudgetsDs.firstOrNull { b ->
                b.pct >= (b.budget.alertThreshold ?: 0.8).toFloat()
            }
        }
    }

    // Hero sub-metrics — pulled from ViewModel metrics (accurate for ALL data,
    // not just the current paging window) so these are never off after filter changes.
    val monthIncome  = monthTotals?.income  ?: 0.0
    val monthExpense = monthTotals?.expense ?: 0.0
    val todayExpense = state.todayExpense
    val weekExpense  = state.weekExpense

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // ── SMS import progress banner (mirrors FinanceScreen.tsx smsBanner) ─
            if (isImporting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        "Importing messages…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            // ── Page header ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text     = "Finance",
                    style    = MaterialTheme.typography.headlineSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Row {
                    IconButton(onClick = { navController.navigate(NavTo.transactionForm()) }) {
                        Icon(
                            imageVector        = Icons.Outlined.Add,
                            contentDescription = "Add transaction",
                            tint               = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {
                        pagingItems.refresh()
                        viewModel.refreshMetrics()
                    }) {
                        Icon(
                            imageVector        = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Hoist scroll states so they are never recreated inside LazyColumn item lambdas
            val actionChipsScrollState    = rememberScrollState()
            val insightsRowScrollState    = rememberScrollState()
            val periodSelectorScrollState = rememberScrollState()
            val listState                 = rememberLazyListState()

            LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── FI-1: SMS permission banner ───────────────────────────────
                if (!smsGranted) {
                    item {
                        val WARNING = Color(0xFFF5CB5C)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs)
                                .clip(MaterialTheme.shapes.medium)
                                .border(1.dp, WARNING, MaterialTheme.shapes.medium)
                                .background(WARNING.copy(alpha = 0.10f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = WARNING.copy(0.2f)),
                                ) {
                                    smsPermLauncher.launch(
                                        arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                    )
                                }
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = WARNING, modifier = Modifier.size(16.dp))
                            Text(
                                "SMS permissions not granted — tap to allow",
                                style = MaterialTheme.typography.bodySmall,
                                color = WARNING,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // ── Action chips ──────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(actionChipsScrollState)
                            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        ActionChip(
                            label   = "Add",
                            icon    = Icons.Outlined.Add,
                            onClick = { navController.navigate(Route.TRANSACTION_FORM) },
                        )
                        ActionChip(
                            label   = "Import SMS",
                            icon    = Icons.Outlined.Message,
                            onClick = { navController.navigate(Route.IMPORT_SMS) },
                        )
                        ActionChip(
                            label   = "Import CSV",
                            icon    = Icons.Outlined.FileDownload,
                            onClick = { navController.navigate(Route.IMPORT_CSV) },
                        )
                        ActionChip(
                            label   = "Export Data",
                            icon    = Icons.Outlined.FileUpload,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text       = formatCurrency(monthExpense),
                            style      = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize   = 34.sp,
                            ),
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            HeroSubMetric(label = "Today",     amount = todayExpense)
                            HeroSubMetric(label = "This week", amount = weekExpense)
                            HeroSubMetric(label = "Income",    amount = monthIncome, isCredit = true)
                        }
                    }
                }

                // ── Budget alert banner (≥ 80% of any budget used) ───────────
                alertBudget?.let { ab ->
                    item {
                        val isOver      = ab.pct >= 1.0f
                        val bgColor     = if (isOver) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                        val accentColor = if (isOver) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs)
                                .clip(MaterialTheme.shapes.large)
                                .border(1.dp, accentColor, MaterialTheme.shapes.large)
                                .background(bgColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = ripple(color = accentColor.copy(0.12f)),
                                ) { navController.navigate(Route.BUDGETS) }
                                .padding(Spacing.base),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint     = accentColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text  = if (isOver) "Over budget" else "Approaching budget",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = accentColor,
                                )
                                val pctText = if (ab.budget.limitAmount > 0) {
                                    "${(ab.pct * 100).toInt()}% of ${formatCurrency(ab.budget.limitAmount, decimals = 0)} ${ab.budget.category} budget used"
                                } else {
                                    "${ab.budget.category.replaceFirstChar { it.uppercase() }} — no limit set"
                                }
                                Text(
                                    text  = pctText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // ── Uncategorized banner ──────────────────────────────────────
                if (uncategorized > 0) {
                    item {
                        InlineBanner(
                            message  = "$uncategorized transactions need a category",
                            tone     = BannerTone.Info,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
                            action   = "Review",
                            onAction = { navController.navigate(Route.CATEGORIZE) },
                        )
                    }
                }

                // ── Insights row (Budget / Fuliza / Fees) — above filters ─────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(insightsRowScrollState)
                            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        val budgetRemaining = activeBudgetsDs.sumOf { it.remaining }
                        InsightCard(
                            label   = "Budget",
                            action  = "View",
                            amount  = budgetRemaining,
                            sub     = "${activeBudgetsDs.size} guardrails",
                            onClick = { navController.navigate(Route.BUDGETS) },
                        )
                        val fulizaOutstanding = activeLoansDs.sumOf { it.drawAmountKes - it.totalRepaidKes }
                        InsightCard(
                            label  = "Fuliza Outstanding",
                            amount = fulizaOutstanding,
                            sub    = "${activeLoansDs.size} open",
                        )
                        if (feeTotal > 0) {
                            InsightCard(
                                label   = "Service Charges",
                                action  = "View",
                                amount  = feeTotal,
                                sub     = "Airtime, Fuliza & subs",
                                onClick = { navController.navigate(Route.FEE_ANALYTICS) },
                            )
                        }
                    }
                }

                // ── Period selector + search ──────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(periodSelectorScrollState)
                            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        listOf("all", "today", "week", "month").forEach { period ->
                            val selected = activeFilters.period == period
                            PeriodChip(
                                label    = period.replaceFirstChar { it.uppercase() },
                                selected = selected,
                                onClick  = { viewModel.setPeriod(period) },
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value         = activeFilters.search,
                        onValueChange = { viewModel.setSearch(it) },
                        placeholder   = { Text("Name, ref code…") },
                        singleLine    = true,
                        modifier      = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
                    )
                }

                // ── Transactions header ───────────────────────────────────────
                item {
                    SectionHeader(
                        label    = "Transactions",
                        action   = pagingItems.itemCount.takeIf { it > 0 }?.toString(),
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                }

                // ── Shimmer — only on the very first load (empty + refreshing) ─
                val isRefreshing = pagingItems.loadState.refresh is LoadState.Loading
                if (isRefreshing && pagingItems.itemCount == 0) {
                    item {
                        ShimmerLoadingState(
                            rowCount = 6,
                            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                        )
                    }
                } else if (!isRefreshing && pagingItems.itemCount == 0) {
                    item {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.x2l),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No transactions yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Paging 3 transaction list ─────────────────────────────────
                //
                // Date grouping is done inline per item:
                //   • isFirstOfDay  → show date header above + open rounded top corners
                //   • isLastOfDay   → close rounded bottom corners + vertical gap after
                //   • peek(index+1) may return null at page boundaries; when the next
                //     page loads the item recomposes with correct corner radius.
                //   • dividers only between same-day items (hidden when isLastOfDay)
                items(
                    count = pagingItems.itemCount,
                    key   = { index -> pagingItems.peek(index)?.id ?: index },
                ) { index ->
                    val tx = pagingItems[index] ?: return@items

                    val prevDate     = if (index > 0) pagingItems.peek(index - 1)?.date?.take(10) else null
                    val currDate     = tx.date?.take(10) ?: ""
                    val nextDate     = pagingItems.peek(index + 1)?.date?.take(10)
                    val isFirstOfDay = prevDate != currDate
                    val isLastOfDay  = nextDate != currDate

                    // Date section header above the first item of each day
                    if (isFirstOfDay) {
                        DayGroupHeader(
                            dateLabel = formatDateKey(currDate),
                            modifier  = Modifier.padding(top = if (index == 0) 0.dp else Spacing.sm),
                        )
                    }

                    // Visual grouping: adaptive corner radius gives each date group
                    // a card-like appearance without a wrapping GlassCard.
                    val topR    = if (isFirstOfDay) 12.dp else 0.dp
                    val bottomR = if (isLastOfDay)  12.dp else 0.dp
                    val shape   = RoundedCornerShape(
                        topStart    = topR, topEnd    = topR,
                        bottomStart = bottomR, bottomEnd = bottomR,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape),
                    ) {
                        TransactionListItem(
                            tx      = tx,
                            onClick = { navController.navigate(NavTo.transactionDetail(tx.id)) },
                        )
                        if (!isLastOfDay) {
                            HorizontalDivider(
                                modifier  = Modifier.padding(horizontal = 12.dp),
                                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                thickness = 1.dp,
                            )
                        }
                    }
                }

                // ── Append (load-more) spinner ────────────────────────────────
                if (pagingItems.loadState.append is LoadState.Loading) {
                    item(key = "load_more") {
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

                // Bottom nav clearance
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }

        // BUG-F9: merge into one TopBanner — error takes priority over import result,
        // so both can never overlap at the same TopCenter position.
        val bannerVisible = state.error != null || smsState.banner != null
        val bannerTone    = if (state.error != null) BannerTone.Error else BannerTone.Success
        val bannerMessage = state.error ?: smsState.banner ?: ""
        TopBanner(
            visible       = bannerVisible,
            message       = bannerMessage,
            tone          = bannerTone,
            onDismiss     = if (state.error == null) ({ smsImportViewModel.clearBanner() }) else null,
            autoDismissMs = if (state.error == null) 3000 else 0,
            modifier      = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars),
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
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ─── Hero sub-metric ─────────────────────────────────────────────────────────

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeroSubMetric(
    label: String,
    amount: Double,
    isCredit: Boolean = false,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text       = formatCurrency(amount, decimals = 0),
            style      = MaterialTheme.typography.titleMedium,
            color      = if (isCredit) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

// ─── Period chip ─────────────────────────────────────────────────────────────

@Composable
private fun PeriodChip(
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label) },
    )
}

// ─── Insight card (Budget / Fuliza / Fees row) ───────────────────────────────

@Composable
private fun InsightCard(
    label:   String,
    amount:  Double,
    sub:     String,
    onClick: (() -> Unit)? = null,
    action:  String? = null,
) {
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(200.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.medium,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                    interactionSource = interactionSource,
                    indication        = ripple(color = primary.copy(0.12f)),
                    onClick           = onClick,
                )
                } else {
                    Modifier
                }
            )
            .padding(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (action != null) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = primary,
                )
            }
        }
        Text(
            text     = formatCurrency(amount, decimals = 0),
            style    = MaterialTheme.typography.headlineSmall,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Text(
            text     = sub,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatDateKey(dateKey: String): String = try {
    val d = LocalDate.parse(dateKey)
    val today = LocalDate.now()
    when {
        d == today               -> "Today"
        d == today.minusDays(1)  -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
} catch (_: Exception) {
    dateKey
}
