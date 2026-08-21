package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.dao.MonthTotals
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.services.BudgetAlertService
import com.belinze.lifeos.services.NotificationScheduler
import com.belinze.lifeos.util.Haptics
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import com.belinze.lifeos.util.nowIso
import com.lifeos.sms.SmsEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// TransactionViewModel
//
// Mirrors useTransactionStore / useFinanceStore from the RN app.
// Manages the Paging 3 transaction list (Finance screen), month totals,
// category totals, merchant totals, and the add/edit form state.
//
// Paging 3 replaces the old manual currentPage / loadNextPage / fetchPage
// approach. flatMapLatest on filters creates a new Pager on every filter
// change; Room's InvalidationTracker auto-invalidates the PagingSource on any
// table write — no explicit reload() calls are needed anywhere.
// ─────────────────────────────────────────────────────────────────────────────

/** Active filter parameters — mirrors the Finance screen filter drawer state. */
@Immutable
data class TransactionFilters(
    val search:    String  = "",
    val category:  String  = "all",
    val type:      String? = null,    // "expense" | "receive" | "transfer" | "fuliza" | null
    val status:    String? = null,    // "completed" | "review" | null
    val period:    String  = "all",   // "all" | "today" | "week" | "month"
    val startDate: String? = null,
    val endDate:   String? = null,
)

@Immutable
data class TransactionUiState(
    val filters:       TransactionFilters = TransactionFilters(),
    val monthTotals:   MonthTotals?       = null,
    val uncategorized: Int                = 0,
    val feeTotal:      Double             = 0.0,
    val todayExpense:  Double             = 0.0,
    val weekExpense:   Double             = 0.0,
    val error:         String?            = null,
)

@Immutable
data class TransactionFormState(
    val id:              String?  = null,      // null = new
    val merchant:        String   = "",
    val amount:          String   = "",        // raw input
    val category:        String   = "uncategorized",
    val transactionType: String   = "expense",
    val date:            String   = nowIso(),
    val description:     String   = "",
    val notes:           String   = "",
    val fee:             String   = "",
    val balanceAfter:    String   = "",
    val mpesaCode:       String   = "",
    val status:          String   = "completed",
    val isSaving:        Boolean  = false,
    val error:           String?  = null,
)

private const val PAGE_SIZE = 50

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel
    @Inject
    constructor(
    private val dao: TransactionDao,
    private val budgetAlertService: BudgetAlertService,
    private val prefs: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {
    private val _uiState   = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    // Separate flow for the detail screen — updating this never triggers a
    // recomposition on FinanceScreen (which only collects uiState), so the
    // navigation animation stays smooth.
    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransaction: StateFlow<TransactionEntity?> = _selectedTransaction.asStateFlow()

    // Current month analytics key
    private val monthKey = currentMonthKey()

    // ─── Paging 3 ─────────────────────────────────────────────────────────────
    //
    // flatMapLatest on _uiState.filters creates a new Pager every time the
    // filters change. The 300 ms debounce prevents a flood of Pager creations
    // during fast typing in the search field.
    //
    // Room's InvalidationTracker invalidates the current PagingSource on every
    // write to the `transactions` table, so insert / update / softDelete all
    // trigger an automatic list refresh — no explicit reload() needed.
    //
    // cachedIn(viewModelScope) retains the in-memory page cache across
    // screen recompositions and config changes.
    val pagedTransactions: Flow<PagingData<TransactionEntity>> = _uiState
        .map { it.filters }
        .distinctUntilChanged()
        .debounce(300)
        .flatMapLatest { filters ->
            Pager(
                config = PagingConfig(
                    pageSize           = PAGE_SIZE,
                    enablePlaceholders = false,
                    prefetchDistance   = PAGE_SIZE / 2,
                ),
            ) {
                dao.getFilteredPaged(
                    search    = filters.search,
                    category  = filters.category,
                    type      = filters.type,
                    status    = filters.status,
                    startDate = filters.startDate,
                    endDate   = filters.endDate,
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    init {
        loadMetrics()

        // Refresh analytics automatically when the SMS parser inserts a new
        // transaction. Room's InvalidationTracker already handles list refresh —
        // only the metric numbers (hero card, insights row) need a manual nudge.
        viewModelScope.launch {
            SmsEventBus.newTransaction.collect { event ->
                loadMetrics()
                // Heads-up notification for the auto-imported transaction, gated by
                // the user's notification preferences. Skip Fuliza fee/charge notices —
                // they are service debits, not user-initiated transactions (mirrors RN).
                runCatching {
                    val state = prefs.state.first()
                    if (state.notificationsEnabled && state.notifTxAlerts && !event.isFuliza && event.mpesaCode.isNotBlank()) {
                        notificationScheduler.postTransactionAlert(
                            mpesaCode = event.mpesaCode,
                            amount    = event.amount,
                            merchant  = event.merchant,
                            type      = event.transactionType,
                        )
                    }
                }
                // Re-evaluate budget thresholds — spending just changed.
                runCatching {
                    val state = prefs.state.first()
                    budgetAlertService.checkAllBudgetThresholds(state)
                }
            }
        }
    }

    // ─── Filters ──────────────────────────────────────────────────────────────
    // Each setter updates _uiState.filters; the pagedTransactions flatMapLatest
    // reacts automatically — no explicit reload() call needed.

    fun setSearch(q: String) =
        _uiState.update { it.copy(filters = it.filters.copy(search = q)) }

    fun setPeriod(period: String) {
        val now = java.time.LocalDate.now()
        val (start, end) = when (period) {
            "today" -> now.toString() to now.toString()
            "week"  -> now.with(java.time.DayOfWeek.MONDAY).toString() to now.toString()
            "month" -> now.withDayOfMonth(1).toString() to now.toString()
            else    -> null to null
        }
        _uiState.update {
            it.copy(filters = it.filters.copy(period = period, startDate = start, endDate = end))
        }
    }

    fun setCategory(cat: String) {
        _uiState.update { it.copy(filters = it.filters.copy(category = cat)) }
    }

    fun setType(type: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(type = type)) }
    }

    fun setDateRange(start: String?, end: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(startDate = start, endDate = end)) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = TransactionFilters()) }
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    private fun loadMetrics() {
        viewModelScope.launch {
            val startMs  = monthKeyToStartMillis(monthKey)
            val endMs    = monthKeyToEndMillis(monthKey)
            val startIso = java.time.Instant.ofEpochMilli(startMs)
                .atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endIso   = java.time.Instant.ofEpochMilli(endMs)
                .atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            val totals   = dao.getMonthTotals(monthKey)
            val feeTotal = dao.getFeeTotal(startIso, endIso) ?: 0.0
            val uncat    = dao.countUncategorized()

            // Today / week spend for the Finance hero card sub-metrics
            val today         = java.time.LocalDate.now()
            val weekStart     = today.with(java.time.DayOfWeek.MONDAY).toString()
            val weekStartIso  = "${weekStart}T00:00:00"
            val todayStartIso = "${today}T00:00:00"
            val todayEndIso   = "${today}T23:59:59"
            val todayExpense  = dao.getSpendTotalInRange(todayStartIso, todayEndIso)
            val weekExpense   = dao.getSpendTotalInRange(weekStartIso, todayEndIso)

            _uiState.update {
                it.copy(
                    monthTotals  = totals,
                    feeTotal     = feeTotal,
                    uncategorized = uncat,
                    todayExpense  = todayExpense,
                    weekExpense   = weekExpense,
                )
            }
        }
    }

    fun refreshMetrics() = loadMetrics()

    // ─── Detail ───────────────────────────────────────────────────────────────

    /** Load a single transaction by ID — used by TransactionDetailScreen when
     *  the transaction is not present in the current paginated list (e.g. opened
     *  from Search). Falls back gracefully: if already in the paged list the
     *  screen uses that copy without waiting for this call. */
    fun loadTransaction(id: String) {
        viewModelScope.launch {
            _selectedTransaction.value = dao.getById(id)
        }
    }

    // ─── Form ─────────────────────────────────────────────────────────────────

    fun openForm(transactionId: String? = null) {
        viewModelScope.launch {
            val entity = transactionId?.let { dao.getById(it) }
            _formState.update {
                if (entity == null) {
                    TransactionFormState()
                } else {
                    TransactionFormState(
                        id              = entity.id,
                        merchant        = entity.merchant ?: "",
                        amount          = entity.amount.toString(),
                        category        = entity.category ?: "uncategorized",
                        transactionType = entity.transactionType ?: "expense",
                        date            = entity.date ?: nowIso(),
                        description     = entity.description ?: "",
                        notes           = entity.notes ?: "",
                        fee             = entity.fee?.toString() ?: "",
                        balanceAfter    = entity.balanceAfter?.toString() ?: "",
                        mpesaCode       = entity.mpesaCode ?: "",
                        status          = entity.status,
                    )
                }
            }
        }
    }

    fun updateFormMerchant(v: String) = _formState.update { it.copy(merchant = v) }

    fun updateFormAmount(v: String) = _formState.update { it.copy(amount = v) }

    fun updateFormCategory(v: String) = _formState.update { it.copy(category = v) }

    fun updateFormType(v: String) = _formState.update { it.copy(transactionType = v) }

    fun updateFormDate(v: String) = _formState.update { it.copy(date = v) }

    fun updateFormDescription(v: String) = _formState.update { it.copy(description = v) }

    fun updateFormNotes(v: String) = _formState.update { it.copy(notes = v) }

    fun updateFormFee(v: String) = _formState.update { it.copy(fee = v) }

    fun updateFormBalanceAfter(v: String) = _formState.update { it.copy(balanceAfter = v) }

    fun updateFormMpesaCode(v: String) = _formState.update { it.copy(mpesaCode = v.uppercase().take(12)) }

    fun updateFormStatus(v: String) = _formState.update { it.copy(status = v) }

    fun saveForm(onSuccess: () -> Unit) {
        val form = _formState.value
        val amt  = form.amount.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            _formState.update { it.copy(error = "Enter a valid amount") }
            return
        }
        _formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val existing = form.id?.let { dao.getById(it) }
                val entity = (existing ?: TransactionEntity(
                    id              = UUID.randomUUID().toString(),
                    amount          = 0.0,
                    merchant        = null,
                    category        = "uncategorized",
                    date            = nowIso(),
                    source          = "manual",
                    transactionType = "expense",
                    status          = "completed",
                    createdAt       = nowIso(),
                    updatedAt       = nowIso(),
                )).copy(
                    merchant        = form.merchant.trim().ifBlank { null },
                    amount          = amt,
                    category        = form.category,
                    transactionType = form.transactionType,
                    date            = form.date,
                    description     = form.description.trim().ifBlank { null },
                    notes           = form.notes.trim().ifBlank { null },
                    fee             = form.fee.trim().toDoubleOrNull(),
                    balanceAfter    = form.balanceAfter.trim().toDoubleOrNull(),
                    mpesaCode       = form.mpesaCode.trim().ifBlank { null },
                    status          = form.status,
                    updatedAt       = nowIso(),
                )
                dao.insert(entity)
                // Room's InvalidationTracker auto-invalidates the PagingSource on
                // this write — list refreshes without an explicit reload() call.
                refreshMetrics()
                Haptics.success()
                _formState.update { it.copy(isSaving = false) }
                onSuccess()
                // Fire budget alerts for the transaction's category (mirrors RN).
                runCatching {
                    val state = prefs.state.first()
                    budgetAlertService.checkBudgetThresholds(state, form.category)
                }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun softDelete(id: String) {
        viewModelScope.launch {
            dao.softDelete(id, nowIso())
            Haptics.warning()
            // Room's InvalidationTracker auto-invalidates the PagingSource.
            refreshMetrics()
        }
    }

    fun updateCategory(id: String, category: String) {
        viewModelScope.launch {
            val entity = dao.getById(id) ?: return@launch
            dao.update(entity.copy(category = category, updatedAt = nowIso()))
            Haptics.light()
            // Room's InvalidationTracker auto-invalidates the PagingSource.
            refreshMetrics()
        }
    }
}
