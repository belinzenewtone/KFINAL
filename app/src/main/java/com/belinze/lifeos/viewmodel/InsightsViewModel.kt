package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.CategoryTotal
import com.belinze.lifeos.data.db.dao.MerchantTotal
import com.belinze.lifeos.data.db.dao.MonthTotals
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import com.belinze.lifeos.util.previousMonthKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// InsightsViewModel
//
// Mirrors the Analytics / Insights screen store.
// Aggregates spending data from TransactionDao for charts and metrics:
//   ‣ Month-over-month comparison
//   ‣ Category breakdown (pie chart data)
//   ‣ Top merchants
//   ‣ Fee total
//   ‣ Rolling 6-month totals array (for bar chart)
// ─────────────────────────────────────────────────────────────────────────────

enum class InsightsPeriod { ThisMonth, LastMonth, Last3Months, Last6Months, ThisYear }

/** Month label + spend + income totals — drives the bar chart. */
data class MonthBar(
    val label:   String,  // "Jan", "Feb" …
    val monthKey: String,
    val expense: Double,
    val income:  Double,
)

data class InsightsUiState(
    val isLoading:       Boolean             = true,
    val period:          InsightsPeriod      = InsightsPeriod.ThisMonth,
    val currentTotals:   MonthTotals?        = null,
    val previousTotals:  MonthTotals?        = null,
    val categoryTotals:  List<CategoryTotal> = emptyList(),
    val topMerchants:    List<MerchantTotal> = emptyList(),
    val feeTotal:        Double              = 0.0,
    val monthBars:       List<MonthBar>      = emptyList(),
    val uncategorized:   Int                 = 0,
    val pendingReview:   Int                 = 0,
    val error:           String?             = null,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val zone     = ZoneId.systemDefault()
    private val isoDtFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val mthFmt   = DateTimeFormatter.ofPattern("MMM")  // "Jan", "Feb"

    init { load() }

    fun setPeriod(period: InsightsPeriod) {
        _uiState.update { it.copy(period = period) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val curKey  = currentMonthKey()
                val prevKey = previousMonthKey()

                val (curStart, curEnd) = isoRange(curKey)
                val (prevStart, prevEnd) = isoRange(prevKey)

                val curTotals  = transactionDao.getMonthTotals(curKey)
                val prevTotals = transactionDao.getMonthTotals(prevKey)
                val catTotals  = transactionDao.getCategoryTotals(curStart, curEnd)
                val merchants  = transactionDao.getTopMerchants(curStart, curEnd, 10)
                val feeTotal   = transactionDao.getFeeTotal(curStart, curEnd) ?: 0.0
                val uncat      = transactionDao.countUncategorized()
                val pending    = transactionDao.countPendingReview()

                // Rolling 6 months for bar chart
                val bars = (5 downTo 0).map { offset ->
                    val key = monthOffsetKey(offset)
                    val (s, e) = isoRange(key)
                    val totals  = transactionDao.getMonthTotals(key)
                    val label   = Instant.ofEpochMilli(monthKeyToStartMillis(key))
                        .atZone(zone).format(mthFmt)
                    MonthBar(
                        label    = label,
                        monthKey = key,
                        expense  = totals.expense ?: 0.0,
                        income   = totals.income  ?: 0.0,
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading      = false,
                        currentTotals  = curTotals,
                        previousTotals = prevTotals,
                        categoryTotals = catTotals,
                        topMerchants   = merchants,
                        feeTotal       = feeTotal,
                        monthBars      = bars,
                        uncategorized  = uncat,
                        pendingReview  = pending,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun isoRange(monthKey: String): Pair<String, String> {
        val start = Instant.ofEpochMilli(monthKeyToStartMillis(monthKey)).atZone(zone).format(isoDtFmt)
        val end   = Instant.ofEpochMilli(monthKeyToEndMillis(monthKey)).atZone(zone).format(isoDtFmt)
        return start to end
    }

    private fun monthOffsetKey(monthsAgo: Int): String {
        val date = java.time.LocalDate.now(zone).minusMonths(monthsAgo.toLong())
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }
}
