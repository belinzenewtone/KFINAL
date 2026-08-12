package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.CategoryTotal
import com.belinze.lifeos.data.db.dao.TransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// MonthlyWrappedViewModel — full parity with MonthlyWrappedScreen.tsx
//
// monthOffset: 0 = current month, -1 = last month, -2 = two months ago, …
// ─────────────────────────────────────────────────────────────────────────────

data class TopCategoryRow(
    val rank:     Int,       // 1, 2, 3
    val category: String,
    val total:    Double,
)

data class MonthlyWrappedUiState(
    val isLoading:          Boolean              = true,
    val monthLabel:         String               = "",    // "August" or "August 2024"
    val monthOffset:        Int                  = 0,
    val minMonthOffset:     Int                  = -60,   // nav limit (oldest data)
    val totalSpend:         Double               = 0.0,
    val totalIncome:        Double               = 0.0,
    val txCount:            Int                  = 0,
    val activeDays:         Int                  = 0,
    val totalDaysInMonth:   Int                  = 31,
    val feesTotal:          Double               = 0.0,
    val fulizaTotal:        Double               = 0.0,
    val fulizaCount:        Int                  = 0,
    val topCategories:      List<TopCategoryRow> = emptyList(),
    val topMerchantName:    String               = "",
    val topMerchantSpend:   Double               = 0.0,
    val biggestAmount:      Double               = 0.0,
    val biggestMerchant:    String               = "",
    val hasData:            Boolean              = false,
    val error:              String?              = null,
)

@HiltViewModel
class MonthlyWrappedViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyWrappedUiState())
    val uiState: StateFlow<MonthlyWrappedUiState> = _uiState.asStateFlow()

    private val zone    = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init { loadMonth(0) }

    fun setMonthOffset(offset: Int) {
        if (offset > 0) return              // future not allowed
        loadMonth(offset)
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun loadMonth(offset: Int) {
        _uiState.update { it.copy(isLoading = true, error = null, monthOffset = offset) }
        viewModelScope.launch {
            try {
                val today       = LocalDate.now(zone)
                val targetMonth = YearMonth.from(today).plusMonths(offset.toLong())
                val firstDay    = targetMonth.atDay(1)
                val lastDay     = targetMonth.atEndOfMonth()

                val startStr = firstDay.format(dateFmt)
                val endStr   = lastDay.format(dateFmt) + "T23:59:59"

                val totalSpend   = transactionDao.getSpendTotalInRange(startStr, endStr)
                val totalIncome  = transactionDao.getIncomeTotalInRange(startStr, endStr)
                val txCount      = transactionDao.countSpendTransactions(startStr, endStr)
                val activeDays   = transactionDao.countActiveDays(startStr, endStr)
                val feesTotal    = transactionDao.getFeeTotalInRange(startStr, endStr)
                val fulizaTotal  = transactionDao.getFulizaTotalInRange(startStr, endStr)
                val fulizaCount  = transactionDao.countFulizaTransactions(startStr, endStr)
                val catTotals    = transactionDao.getCategoryTotals(startStr, endStr)
                val topMerchants = transactionDao.getTopMerchants(startStr, endStr, 1)
                val biggestSpend = transactionDao.getBiggestSpend(startStr, endStr)
                val minDateStr   = transactionDao.getMinTransactionDate()

                // Compute how far back we can navigate
                val minOffset = if (minDateStr != null) {
                    val minYm = YearMonth.from(LocalDate.parse(minDateStr.take(10)))
                    val curYm = YearMonth.from(today)
                    minYm.until(curYm, java.time.temporal.ChronoUnit.MONTHS).toInt().let { -it }
                } else -60

                // Top 3 categories
                val top3 = catTotals
                    .filter { !it.category.isNullOrBlank() && it.category != "uncategorized" }
                    .take(3)
                    .mapIndexed { i, c -> TopCategoryRow(i + 1, c.category ?: "Other", c.total) }

                // Month label
                val monthLabel = if (offset > -11) {
                    targetMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                } else {
                    targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH))
                }

                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        monthLabel      = monthLabel,
                        monthOffset     = offset,
                        minMonthOffset  = minOffset,
                        totalSpend      = totalSpend,
                        totalIncome     = totalIncome,
                        txCount         = txCount,
                        activeDays      = activeDays,
                        totalDaysInMonth = lastDay.dayOfMonth,
                        feesTotal       = feesTotal,
                        fulizaTotal     = fulizaTotal,
                        fulizaCount     = fulizaCount,
                        topCategories   = top3,
                        topMerchantName  = topMerchants.firstOrNull()?.merchant ?: "",
                        topMerchantSpend = topMerchants.firstOrNull()?.total ?: 0.0,
                        biggestAmount    = biggestSpend?.amount ?: 0.0,
                        biggestMerchant  = biggestSpend?.merchant ?: "",
                        hasData          = totalSpend > 0.0,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
