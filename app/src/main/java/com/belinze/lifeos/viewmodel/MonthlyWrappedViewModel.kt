package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.util.lastDayOfMonth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// ─────────────────────────────────────────────────────────────────────────────
// MonthlyWrappedViewModel — full parity with MonthlyWrappedScreen.tsx
//
// monthOffset: 0 = current month, -1 = last month, -2 = two months ago, …
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class TopCategoryRow(
    val rank:     Int,       // 1, 2, 3
    val category: String,
    val total:    Double,
)

@Immutable
data class MonthlyWrappedUiState(
    val isLoading:          Boolean                      = true,
    val monthLabel:         String                       = "",    // "August" or "August 2024"
    val monthOffset:        Int                          = 0,
    val minMonthOffset:     Int                          = -24,   // nav limit (oldest data)
    val totalSpend:         Double                       = 0.0,
    val totalIncome:        Double                       = 0.0,
    val txCount:            Int                          = 0,
    val activeDays:         Int                          = 0,
    val totalDaysInMonth:   Int                          = 31,
    val feesTotal:          Double                       = 0.0,
    val fulizaTotal:        Double                       = 0.0,
    val fulizaCount:        Int                          = 0,
    val topCategories:      ImmutableList<TopCategoryRow> = persistentListOf(),
    val topMerchantName:    String               = "",
    val topMerchantSpend:   Double               = 0.0,
    val biggestAmount:      Double               = 0.0,
    val biggestMerchant:    String               = "",
    val hasData:            Boolean              = false,
    val error:              String?              = null,
)

class MonthlyWrappedViewModel
constructor(
    private val transactionDao: TransactionDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MonthlyWrappedUiState())
    val uiState: StateFlow<MonthlyWrappedUiState> = _uiState.asStateFlow()

    private val zone = TimeZone.currentSystemDefault()

    // Screen's LaunchedEffect calls setMonthOffset(initialMonthOffset) — that is
    // the single load trigger. No init load to avoid flashing the wrong month.

    fun setMonthOffset(offset: Int) {
        if (offset > 0) return              // future not allowed
        loadMonth(offset)
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun loadMonth(offset: Int) {
        _uiState.update { it.copy(isLoading = true, error = null, monthOffset = offset) }
        viewModelScope.launch {
            try {
                val today    = Clock.System.todayIn(zone)
                // firstDay of target month (using offset from current month)
                val firstDay = LocalDate(today.year, today.month, 1).plus(offset, DateTimeUnit.MONTH)
                val lastDay  = lastDayOfMonth(firstDay)

                val startStr = firstDay.toString() + "T00:00:00"
                val endStr   = lastDay.toString() + "T23:59:59"

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
                    val minFirst = LocalDate(
                        LocalDate.parse(minDateStr.take(10)).year,
                        LocalDate.parse(minDateStr.take(10)).month,
                        1,
                    )
                    val curFirst = LocalDate(today.year, today.month, 1)
                    val diff = (curFirst.year - minFirst.year) * 12 + (curFirst.monthNumber - minFirst.monthNumber)
                    -diff
                } else {
                    -24
                }

                // Top 3 categories — React takes the raw category groups with no
                // filtering, so an "uncategorized"/null group can appear here too.
                val top3 = catTotals
                    .take(3)
                    .mapIndexed { i, c -> TopCategoryRow(i + 1, c.category ?: "", c.total) }

                // React shows the year only once the month is 12+ months back
                // (monthOffset < -11), so -11 and above stay month-only.
                val monthLabel = if (offset >= -11) {
                    MonthNames.ENGLISH_FULL.names[firstDay.monthNumber - 1]
                } else {
                    "${MonthNames.ENGLISH_FULL.names[firstDay.monthNumber - 1]} ${firstDay.year}"
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
                        topCategories   = top3.toImmutableList(),
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
