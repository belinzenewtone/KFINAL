package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.BudgetEntity
import com.belinze.lifeos.util.Haptics
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// ─────────────────────────────────────────────────────────────────────────────
// BudgetViewModel
//
// Mirrors useBudgetStore from the RN app.
// Loads budgets with their real spend for the current month (from TransactionDao),
// computes percentage used, and exposes form state for add/edit.
// ─────────────────────────────────────────────────────────────────────────────

/** A budget enriched with current-month spend. */
@Immutable
data class BudgetWithSpend(
    val budget:    BudgetEntity,
    val spend:     Double = 0.0,
    val pct:       Float  = 0f,     // spend / limitAmount (can exceed 1.0 when over budget)
    val remaining: Double = 0.0,
)

@Immutable
data class BudgetUiState(
    val isLoading:  Boolean                    = true,
    val budgets:    ImmutableList<BudgetWithSpend> = persistentListOf(),
    val totalLimit: Double               = 0.0,
    val totalSpend: Double               = 0.0,
    val error:      String?              = null,
)

@Immutable
data class BudgetFormState(
    val id:             String?  = null,
    val category:       String   = "",
    val limitAmount:    String   = "",     // raw string input, validated on save
    val period:         String   = "monthly",
    val alertThreshold: String   = "80",
    val isActive:       Boolean  = true,
    val isSaving:       Boolean  = false,
    val error:          String?  = null,
)

@HiltViewModel
class BudgetViewModel
    @Inject
    constructor(
    private val budgetDao:      BudgetDao,
    private val transactionDao: TransactionDao,
) : ViewModel() {
    private val _uiState   = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(BudgetFormState())
    val formState: StateFlow<BudgetFormState> = _formState.asStateFlow()

    private val monthKey    = currentMonthKey()
    private val isoDtFmt    = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val zone        = ZoneId.systemDefault()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val budgets = budgetDao.getAll()

            // Category spend for this month
            val startMs  = monthKeyToStartMillis(monthKey)
            val endMs    = monthKeyToEndMillis(monthKey)
            val startIso = Instant.ofEpochMilli(startMs).atZone(zone).format(isoDtFmt)
            val endIso   = Instant.ofEpochMilli(endMs).atZone(zone).format(isoDtFmt)
            val catSpend = transactionDao.getCategoryTotals(startIso, endIso)
                .associate { it.category to it.total }

            val enriched = budgets.map { b ->
                val spend = catSpend[b.category] ?: 0.0
                val pct   = if (b.limitAmount > 0) (spend / b.limitAmount).toFloat() else 0f
                BudgetWithSpend(
                    budget    = b,
                    spend     = spend,
                    pct       = pct,
                    remaining = b.limitAmount - spend,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading  = false,
                    budgets    = enriched.toImmutableList(),
                    totalLimit = budgets.sumOf { b -> b.limitAmount },
                    totalSpend = enriched.sumOf { b -> b.spend },
                )
            }
        }
    }

    // ─── Form ─────────────────────────────────────────────────────────────────

    fun openForm(budgetId: String? = null) {
        viewModelScope.launch {
            val entity = budgetId?.let { budgetDao.getById(it) }
            _formState.update {
                if (entity == null) {
                    BudgetFormState()
                } else {
                    BudgetFormState(
                    id             = entity.id,
                    category       = entity.category,
                    limitAmount    = entity.limitAmount.toString(),
                    period         = entity.period,
                    alertThreshold = ((entity.alertThreshold ?: 0.8) * 100).toInt().toString(),
                    isActive       = entity.isActive != 0,
                )
                }
            }
        }
    }

    fun updateCategory(v: String) = _formState.update { it.copy(category = v) }

    fun updateLimitAmount(v: String) = _formState.update { it.copy(limitAmount = v) }

    fun updatePeriod(v: String) = _formState.update { it.copy(period = v) }

    fun updateAlertThreshold(v: String) = _formState.update { it.copy(alertThreshold = v) }

    fun updateActive(v: Boolean) = _formState.update { it.copy(isActive = v) }

    // Backward-compatible alias used by BudgetFormScreen
    fun updateLimit(v: String) = updateLimitAmount(v)

    fun saveForm(onSuccess: () -> Unit) {
        val form        = _formState.value
        val limitDouble = form.limitAmount.toDoubleOrNull()
        if (form.category.isBlank()) {
            _formState.update { it.copy(error = "Select a category") }
            return
        }
        if (limitDouble == null || limitDouble <= 0) {
            _formState.update { it.copy(error = "Enter a valid limit") }
            return
        }
        _formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val existing = form.id?.let { budgetDao.getById(it) }
                val entity = (existing ?: BudgetEntity(
                    id           = UUID.randomUUID().toString(),
                    category     = "",
                    limitAmount  = 0.0,
                    period       = "monthly",
                    alertThreshold = null,
                    isActive     = 1,
                    createdAt    = nowIso(),
                    updatedAt    = nowIso(),
                    syncState    = null,
                    recordSource = null,
                    deletedAt    = null,
                    revision     = 1,
                    userId       = null,
                )).copy(
                    category        = form.category,
                    limitAmount     = limitDouble,
                    period          = form.period,
                    alertThreshold  = form.alertThreshold.toDoubleOrNull()?.div(100.0),
                    isActive        = if (form.isActive) 1 else 0,
                    updatedAt       = nowIso(),
                )
                budgetDao.insert(entity)
                load()
                Haptics.light()
                _formState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun softDelete(id: String) {
        viewModelScope.launch {
            budgetDao.softDelete(id, nowIso())
            load()
        }
    }

    fun toggleActive(id: String, active: Boolean) {
        viewModelScope.launch {
            val entity = budgetDao.getById(id) ?: return@launch
            budgetDao.update(entity.copy(isActive = if (active) 1 else 0, updatedAt = nowIso()))
            load()
        }
    }
}
