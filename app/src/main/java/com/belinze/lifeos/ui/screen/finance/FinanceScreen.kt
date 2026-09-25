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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.belinze.lifeos.ui.components.AppDropdownField
import com.belinze.lifeos.ui.components.AppPickerSheet
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.PickerOption
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.BudgetViewModel
import com.belinze.lifeos.viewmodel.PlannerViewModel
import com.belinze.lifeos.viewmodel.SmsImportViewModel
import com.belinze.lifeos.viewmodel.TransactionViewModel

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

@Suppress("CyclomaticComplexMethod", "LongMethod")
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
    val uncategorized       by remember { derivedStateOf { state.uncategorized } }
    val uncategorizedAmount by remember { derivedStateOf { state.uncategorizedAmount } }
    val activeFilters   by remember { derivedStateOf { state.filters } }
    val activeBudgetsDs by remember { derivedStateOf { budgetState.budgets.filter { it.budget.isActive != 0 } } }
    val activeLoansDs   by remember { derivedStateOf { plannerState.loans.filter { it.status == "active" } } }
    val isImporting     by remember { derivedStateOf { smsState.isImporting } }
    val context      = LocalContext.current
    var showImportSmsSheet     by remember { mutableStateOf(false) }
    var showImportCsvSheet     by remember { mutableStateOf(false) }

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

    // Top budget alert — mirrors RN: prefer a budget that's crossed its alert
    // threshold, else fall back to the single highest-usage active budget so a
    // card is shown whenever at least one active budget exists (RN always
    // shows one here, not only once a threshold is crossed).
    val alertBudget by remember {
        derivedStateOf {
            activeBudgetsDs.firstOrNull { b ->
                b.pct >= (b.budget.alertThreshold ?: 0.8).toFloat()
            } ?: activeBudgetsDs.maxByOrNull { it.pct }
        }
    }

    // Hero sub-metrics — pulled from ViewModel metrics (accurate for ALL data,
    // not just the current paging window) so these are never off after filter changes.
    val monthIncome  = monthTotals?.income  ?: 0.0
    val monthExpense = monthTotals?.expense ?: 0.0
    val todayExpense = state.todayExpense
    val weekExpense  = state.weekExpense

    Box(modifier = Modifier.fillMaxSize()) {
        // Declared at screen level because the period picker sheet is rendered as a
        // sibling of the Column below, not inside it.
        var periodExpanded by remember { mutableStateOf(false) }
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
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text     = "Finance",
                    style    = MaterialTheme.typography.headlineSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                IconButton(onClick = {
                    pagingItems.refresh()
                    viewModel.refreshMetrics()
                    budgetViewModel.load()
                }) {
                    Icon(
                        imageVector        = Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint               = MaterialTheme.colorScheme.onSurface,
                        modifier           = Modifier.size(22.dp),
                    )
                }
            }

            // Hoist scroll states so they are never recreated inside LazyColumn item lambdas
            val actionChipsScrollState = rememberScrollState()
            val listState              = rememberLazyListState()

            // RFINAL has no pull-to-refresh here — only the header refresh button.
            LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── Action chips ──────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(actionChipsScrollState)
                            .padding(horizontal = Spacing.screenHorizontal, vertical = 0.dp)
                            .padding(bottom = 14.dp),
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
                            onClick = { showImportSmsSheet = true },
                        )
                        ActionChip(
                            label   = "Import CSV",
                            icon    = Icons.Outlined.Description,
                            onClick = { showImportCsvSheet = true },
                        )
                        ActionChip(
                            label   = "Export",
                            icon    = Icons.Outlined.FileDownload,
                            onClick = { navController.navigate(Route.EXPORT_DATA) },
                        )
                    }
                }

                // ── Hero card ─────────────────────────────────────────────────
                item {
                    FrostCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start  = Spacing.screenHorizontal,
                                end    = Spacing.screenHorizontal,
                                bottom = 14.dp,
                            ),
                    ) {
                        Text(
                            text  = "Spent this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text  = formatCurrency(monthExpense, decimals = 0),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                            Color(0xFFFEF9C3)
                        }
                        val accentColor = if (isOver) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color(0xFFF5CB5C)
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
                                val pctText = "${ab.budget.category.replaceFirstChar { it.uppercase() }} is ${(ab.pct * 100).toInt()}% used"
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
                            message  = "$uncategorized uncategorized transaction${if (uncategorized == 1) "" else "s"} · ${formatCurrency(uncategorizedAmount, decimals = 0)} missing from charts",
                            tone     = BannerTone.Info,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
                            action   = "Fix",
                            onAction = { navController.navigate(Route.CATEGORIZE) },
                        )
                    }
                }

                // ── Insights row (Budget / Fuliza / Fees) — above filters ─────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start  = Spacing.screenHorizontal,
                                end    = Spacing.screenHorizontal,
                                top    = Spacing.sm,
                                bottom = Spacing.base,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        val totalMonthBudget = activeBudgetsDs.sumOf { it.budget.limitAmount }
                        InsightCard(
                            modifier = Modifier.weight(1f),
                            icon     = Icons.Outlined.AccountBalanceWallet,
                            label    = "Budget",
                            action   = "View all",
                            amount   = totalMonthBudget,
                            sub      = "${activeBudgetsDs.size} active budget${if (activeBudgetsDs.size != 1) "s" else ""}",
                            onClick  = { navController.navigate(Route.BUDGETS) },
                        )
                        val fulizaOutstanding = activeLoansDs.sumOf { it.drawAmountKes - it.totalRepaidKes }
                        InsightCard(
                            modifier = Modifier.weight(1f),
                            icon     = Icons.Outlined.TrendingUp,
                            label    = "Fuliza",
                            amount   = fulizaOutstanding,
                            sub      = if (activeLoansDs.isEmpty()) "No open loans" else "${activeLoansDs.size} loan${if (activeLoansDs.size != 1) "s" else ""} outstanding",
                        )
                        if (feeTotal > 0) {
                            InsightCard(
                                modifier = Modifier.weight(1f),
                                icon     = Icons.Outlined.Receipt,
                                label    = "Charges",
                                amount   = feeTotal,
                                sub      = "Airtime, Fuliza & subs",
                            )
                        }
                    }
                }

                // ── Period selector + search ──────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                    ) {
                        AppDropdownField(
                            label      = "Period",
                            valueLabel = activeFilters.period.replaceFirstChar { it.uppercase() },
                            onClick    = { periodExpanded = true },
                            modifier   = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    FinanceSearchField(
                        value         = activeFilters.search,
                        onValueChange = { viewModel.setSearch(it) },
                        placeholder   = "Name, ref code…",
                        modifier      = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal)
                            .padding(bottom = 14.dp),
                    )
                }

                // ── FI-1: SMS permission banner — between search and list ────────
                if (!smsGranted) {
                    item {
                        val SMS_WARNING = Color(0xFFF5CB5C)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs)
                                .clip(MaterialTheme.shapes.medium)
                                .border(1.dp, SMS_WARNING, MaterialTheme.shapes.medium)
                                .background(SMS_WARNING.copy(alpha = 0.10f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = SMS_WARNING.copy(0.2f)),
                                ) {
                                    smsPermLauncher.launch(
                                        arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                    )
                                }
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = SMS_WARNING, modifier = Modifier.size(16.dp))
                            Text(
                                "SMS permissions not granted — tap to allow",
                                style = MaterialTheme.typography.bodySmall,
                                color = SMS_WARNING,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // ── Transactions header ───────────────────────────────────────
                // RFINAL: titleMedium "Transactions" + a bodyMedium count, with a 12dp
                // horizontal gutter and 8dp vertical margins. SectionHeader was wrong
                // here — it uppercases its label, and it only draws the action when
                // onAction is non-null, which is why the count never appeared.
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal)
                            .padding(top = Spacing.sm, bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text     = "Transactions",
                            style    = MaterialTheme.typography.titleMedium,
                            color    = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text     = pagingItems.itemCount.toString(),
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                // RFINAL renders no loading skeleton — only the empty state.
                if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh !is LoadState.Loading) {
                    item {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No transactions found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Paging 3 transaction list ─────────────────────────────────
                //
                // Each transaction is its own bordered card (RFINAL parity).
                // Date grouping: peek(index-1) to show a date header above the
                // first item of each day. peek(index+1) is NOT used so there's
                // no bounds-check hazard and no adaptive corner logic needed.
                items(
                    count = pagingItems.itemCount,
                    key   = { index -> pagingItems.peek(index)?.id ?: index },
                ) { index ->
                    val tx = pagingItems[index] ?: return@items

                    val prevDate     = if (index > 0) pagingItems.peek(index - 1)?.date?.take(10) else null
                    val currDate     = tx.date?.take(10) ?: ""
                    val isFirstOfDay = prevDate != currDate

                    if (isFirstOfDay) {
                        DayGroupHeader(
                            dateLabel = formatRelativeDay(currDate),
                            modifier  = Modifier.padding(top = if (index == 0) 0.dp else Spacing.sm),
                        )
                    }

                    // RFINAL pushes the detail screen; there is no in-place dialog.
                    // TransactionListItem draws its own bordered card (as in RFINAL),
                    // so no wrapper container is needed here.
                    TransactionListItem(
                        tx      = tx,
                        onClick = {
                            navController.navigate(
                                Route.TRANSACTION_DETAIL.replace("{transactionId}", tx.id)
                            )
                        },
                    )
                }

                // RFINAL renders no load-more spinner; Paging 3 appends silently.

                // Bottom nav clearance
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }

        // FAB — Add transaction (mirrors RFINAL's bottom-right FAB on FinanceScreen)
        FloatingActionButton(
            onClick        = { navController.navigate(Route.TRANSACTION_FORM) },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.lg),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Add transaction",
                tint     = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        // RFINAL shows import results inline in the scroll flow, never as a
        // floating toast, so no TopBanner is rendered here.

        // Period picker — hoisted out of the LazyColumn so scrolling can't dispose
        // the sheet while it is open (mirrors RN's Dropdown + SwipeableSheet).
        AppPickerSheet(
            visible     = periodExpanded,
            title       = "Period",
            options     = listOf("all", "today", "week", "month").map {
                PickerOption(key = it, label = it.replaceFirstChar { c -> c.uppercase() })
            },
            selectedKey = activeFilters.period,
            onSelect    = { viewModel.setPeriod(it) },
            onDismiss   = { periodExpanded = false },
        )

        if (showImportSmsSheet) {
            ImportSmsSheet(
                onDismiss = { showImportSmsSheet = false },
                viewModel = smsImportViewModel,
            )
        }
        if (showImportCsvSheet) {
            ImportCsvSheet(
                onDismiss     = { showImportCsvSheet = false },
                navController = navController,
            )
        }
    }
}

// ─── Search field — pill-shaped, mirrors RN's SearchField (44dp height) ───────

@Composable
private fun FinanceSearchField(
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String,
    modifier:      Modifier = Modifier,
) {
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint     = onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value          = value,
                onValueChange  = onValueChange,
                singleLine     = true,
                textStyle      = MaterialTheme.typography.bodyMedium.copy(color = onSurface),
                cursorBrush    = SolidColor(MaterialTheme.colorScheme.primary),
                modifier       = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant,
                )
            }
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Outlined.Cancel,
                contentDescription = "Clear",
                tint     = onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onValueChange("") },
            )
        }
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
    val onSurface         = MaterialTheme.colorScheme.onSurface
    val outlineVariant    = MaterialTheme.colorScheme.outlineVariant
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.medium,
            )
            .border(1.dp, outlineVariant, MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(0.15f)),
                onClick           = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = onSurface, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = onSurface)
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
            text  = formatCurrency(amount, decimals = 0),
            style = MaterialTheme.typography.titleSmall,
            color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ─── Insight card (Budget / Fuliza / Fees row) ───────────────────────────────

@Composable
private fun InsightCard(
    label:    String,
    amount:   Double,
    sub:      String,
    modifier: Modifier = Modifier,
    icon:     androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick:  (() -> Unit)? = null,
    action:   String? = null,
) {
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text     = formatCurrency(amount, decimals = 0),
            style    = MaterialTheme.typography.titleMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Text(
            text     = sub,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (action != null) {
            Text(
                text     = "$action →",
                style    = MaterialTheme.typography.labelSmall,
                color    = primary,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
// Date-group labels are formatted via formatRelativeDay() in TransactionListItem.kt
// (mirrors RN's formatRelativeDay(): Today/Tomorrow/Yesterday/weekday/"dd MMM").

// ─── Import CSV sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportCsvSheet(
    onDismiss:     () -> Unit,
    navController: NavHostController,
) {
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onDismiss()
            val name = uri.lastPathSegment ?: "import.csv"
            navController.navigate(NavTo.csvImport(uri.toString(), name))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.x2l),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                "Import from CSV",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Columns: date, amount, type, category, description.\nFormats: yyyy-MM-dd · dd/MM/yyyy · MM/dd/yyyy",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
            Button(
                onClick  = { fileLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Choose File")
            }
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
