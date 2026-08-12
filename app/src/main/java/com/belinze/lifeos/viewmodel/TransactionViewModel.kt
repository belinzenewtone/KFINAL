package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.CategoryTotal
import com.belinze.lifeos.data.db.dao.MerchantTotal
import com.belinze.lifeos.data.db.dao.MonthTotals
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import com.belinze.lifeos.util.nowIso
import com.lifeos.sms.SmsEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// TransactionViewModel
//
// Mirrors useTransactionStore / useFinanceStore from the RN app.
// Manages the paginated, filtered transaction list (Finance screen), month totals,
// category totals, merchant totals, and the add/edit form state.
// ─────────────────────────────────────────────────────────────────────────────

/** Active filter parameters — mirrors the Finance screen filter drawer state. */
data class TransactionFilters(
    val search:    String  = "",
    val category:  String  = "all",
    val type:      String? = null,    // "expense" | "receive" | "transfer" | "fuliza" | null
    val status:    String? = null,    // "completed" | "review" | null
    val startDate: String? = null,
    val endDate:   String? = null,
)

data class TransactionUiState(
    val isLoading:       Boolean                  = true,
    val transactions:    List<TransactionEntity>  = emptyList(),
    val hasNextPage:     Boolean                  = true,
    val filters:         TransactionFilters       = TransactionFilters(),
    val monthTotals:     MonthTotals?             = null,
    val categoryTotals:  List<CategoryTotal>      = emptyList(),
    val merchantTotals:  List<MerchantTotal>      = emptyList(),
    val pendingReview:   Int                      = 0,
    val uncategorized:   Int                      = 0,
    val feeTotal:        Double                   = 0.0,
    val error:           String?                  = null,
)

data class TransactionFormState(
    val id:              String?  = null,      // null = new
    val merchant:        String   = "",
    val amount:          String   = "",        // raw input
    val category:        String   = "uncategorized",
    val transactionType: String   = "expense",
    val date:            String   = nowIso(),
    val notes:           String   = "",
    val isSaving:        Boolean  = false,
    val error:           String?  = null,
)

private const val PAGE_SIZE = 30

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val dao: TransactionDao,
) : ViewModel() {

    private val _uiState   = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    // Track how many pages are loaded (for infinite scroll)
    private var currentPage = 0

    // Current month analytics key
    private val monthKey = currentMonthKey()

    init {
        // Observe search/filter changes with 300ms debounce, reload list
        _uiState
            .debounce(300)
            .distinctUntilChanged { old, new -> old.filters == new.filters }
            .onEach { reload() }
            .launchIn(viewModelScope)

        loadMetrics()

        // Refresh automatically when the SMS parser inserts a new transaction.
        // SmsProcessWorker writes directly to the Room-shared DB file and then
        // emits on SmsEventBus — we reload the current page and metrics so the
        // Finance screen shows the new row without the user having to pull-to-refresh.
        viewModelScope.launch {
            SmsEventBus.newTransaction.collect {
                reload()
                loadMetrics()
            }
        }
    }

    // ─── List loading ─────────────────────────────────────────────────────────

    fun reload() {
        currentPage = 0
        _uiState.update { it.copy(isLoading = true, transactions = emptyList(), hasNextPage = true) }
        fetchPage()
    }

    fun loadNextPage() {
        if (!_uiState.value.hasNextPage || _uiState.value.isLoading) return
        fetchPage()
    }

    private fun fetchPage() {
        val filters = _uiState.value.filters
        val offset  = currentPage * PAGE_SIZE

        viewModelScope.launch {
            val rows = dao.getFiltered(
                search    = filters.search,
                category  = filters.category,
                type      = filters.type,
                status    = filters.status,
                startDate = filters.startDate,
                endDate   = filters.endDate,
                limit     = PAGE_SIZE,
                offset    = offset,
            )
            currentPage++
            _uiState.update { state ->
                state.copy(
                    isLoading    = false,
                    transactions = state.transactions + rows,
                    hasNextPage  = rows.size == PAGE_SIZE,
                )
            }
        }
    }

    // ─── Filters ──────────────────────────────────────────────────────────────

    fun setSearch(q: String) =
        _uiState.update { it.copy(filters = it.filters.copy(search = q)) }

    fun setCategory(cat: String) {
        _uiState.update { it.copy(filters = it.filters.copy(category = cat)) }
        reload()
    }

    fun setType(type: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(type = type)) }
        reload()
    }

    fun setDateRange(start: String?, end: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(startDate = start, endDate = end)) }
        reload()
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = TransactionFilters()) }
        reload()
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    private fun loadMetrics() {
        viewModelScope.launch {
            val startMs = monthKeyToStartMillis(monthKey)
            val endMs   = monthKeyToEndMillis(monthKey)
            val startIso = java.time.Instant.ofEpochMilli(startMs)
                .atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endIso   = java.time.Instant.ofEpochMilli(endMs)
                .atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            val totals      = dao.getMonthTotals(monthKey)
            val catTotals   = dao.getCategoryTotals(startIso, endIso)
            val merchantTop = dao.getTopMerchants(startIso, endIso, 10)
            val feeTotal    = dao.getFeeTotal(startIso, endIso) ?: 0.0
            val pending     = dao.countPendingReview()
            val uncat       = dao.countUncategorized()

            _uiState.update {
                it.copy(
                    monthTotals     = totals,
                    categoryTotals  = catTotals,
                    merchantTotals  = merchantTop,
                    feeTotal        = feeTotal,
                    pendingReview   = pending,
                    uncategorized   = uncat,
                )
            }
        }
    }

    fun refreshMetrics() = loadMetrics()

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
                        transactionType = entity.transactionType,
                        date            = entity.date,
                        notes           = entity.notes ?: "",
                    )
                }
            }
        }
    }

    fun updateFormMerchant(v: String) = _formState.update { it.copy(merchant = v) }
    fun updateFormAmount(v: String)   = _formState.update { it.copy(amount = v) }
    fun updateFormCategory(v: String) = _formState.update { it.copy(category = v) }
    fun updateFormType(v: String)     = _formState.update { it.copy(transactionType = v) }
    fun updateFormDate(v: String)     = _formState.update { it.copy(date = v) }
    fun updateFormNotes(v: String)    = _formState.update { it.copy(notes = v) }

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
                val entity = (existing ?: TransactionEntity(id = UUID.randomUUID().toString())).copy(
                    merchant        = form.merchant.ifBlank { null },
                    amount          = amt,
                    category        = form.category,
                    transactionType = form.transactionType,
                    date            = form.date,
                    notes           = form.notes.ifBlank { null },
                    status          = "completed",
                    updatedAt       = nowIso(),
                )
                dao.insert(entity)
                reload()
                refreshMetrics()
                _formState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun softDelete(id: String) {
        viewModelScope.launch {
            dao.softDelete(id, nowIso())
            reload()
            refreshMetrics()
        }
    }

    fun updateCategory(id: String, category: String) {
        viewModelScope.launch {
            val entity = dao.getById(id) ?: return@launch
            dao.update(entity.copy(category = category, updatedAt = nowIso()))
            reload()
            refreshMetrics()
        }
    }
}
