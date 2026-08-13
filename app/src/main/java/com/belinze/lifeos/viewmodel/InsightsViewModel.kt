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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// InsightsViewModel
//
// Mirrors the React AnalyticsScreen (2-tab: Analytics + Insights).
// ─────────────────────────────────────────────────────────────────────────────

enum class InsightsPeriod { ThisMonth, LastMonth, Last3Months, Last6Months, ThisYear }
enum class AnalyticsTab   { Analytics, Insights }
enum class AnalyticsRange { ThisWeek, ThisMonth }

/** Month label + spend + income totals — drives the bar chart. */
data class MonthBar(
    val label:    String,
    val monthKey: String,
    val expense:  Double,
    val income:   Double,
)

data class CategorySparklineItem(
    val category:      String,
    val color:         String,
    val total:         Double,
    val pctOfTotal:    Double,
    val weeklyAmounts: List<Double>,  // 4 values oldest → newest
    val topMerchant:   String?,
)

data class AnalyticsFeesData(
    val total:       Double,
    val topCategory: String?,
    val avgFee:      Double,
    val txCount:     Int,
)

data class InsightsUiState(
    val isLoading: Boolean = true,

    // ── Tab & range ──────────────────────────────────────────────────────────
    val activeTab:    AnalyticsTab   = AnalyticsTab.Analytics,
    val dateRange:    AnalyticsRange = AnalyticsRange.ThisMonth,
    val nudgeDismissed: Boolean      = false,

    // ── Analytics tab (range-dependent) ─────────────────────────────────────
    val totalSpend:          Double                     = 0.0,
    val totalIncome:         Double                     = 0.0,
    val net:                 Double                     = 0.0,
    val averageTransaction:  Double                     = 0.0,
    val categorySparklines:  List<CategorySparklineItem> = emptyList(),
    val feesData:            AnalyticsFeesData          = AnalyticsFeesData(0.0, null, 0.0, 0),
    val uncategorizedCount:  Int                        = 0,
    val uncategorizedAmount: Double                     = 0.0,

    // ── SpendingComparisonCard (always current/prev month) ───────────────────
    val currentMonthSpend: Double = 0.0,
    val prevMonthSpend:    Double = 0.0,

    // ── Insights tab (month-fixed) ───────────────────────────────────────────
    val period:         InsightsPeriod      = InsightsPeriod.ThisMonth,
    val currentTotals:  MonthTotals?        = null,
    val previousTotals: MonthTotals?        = null,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val topMerchants:   List<MerchantTotal> = emptyList(),
    val feeTotal:       Double              = 0.0,
    val monthBars:      List<MonthBar>      = emptyList(),
    val uncategorized:  Int                 = 0,
    val pendingReview:  Int                 = 0,
    val error:          String?             = null,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val zone     = ZoneId.systemDefault()
    private val isoDtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val mthFmt   = DateTimeFormatter.ofPattern("MMM")  // "Jan", "Feb"

    init { load() }

    fun setActiveTab(tab: AnalyticsTab)      { _uiState.update { it.copy(activeTab = tab) } }
    fun setDateRange(range: AnalyticsRange)  {
        _uiState.update { it.copy(dateRange = range) }
        loadAnalyticsTab()
    }
    fun dismissNudge()                       { _uiState.update { it.copy(nudgeDismissed = true) } }
    fun setPeriod(period: InsightsPeriod)    {
        _uiState.update { it.copy(period = period) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                loadInsightsTabInner()
                loadAnalyticsTabInner()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Analytics tab ────────────────────────────────────────────────────────

    private fun loadAnalyticsTab() {
        viewModelScope.launch {
            try { loadAnalyticsTabInner() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    private suspend fun loadAnalyticsTabInner() {
        val range            = _uiState.value.dateRange
        val (start, end)     = rangeToIso(range)
        val curKey           = currentMonthKey()
        val prevKey          = previousMonthKey()
        val (curStart, curEnd)   = isoRange(curKey)
        val (prevStart, prevEnd) = isoRange(prevKey)

        val spend            = transactionDao.getSpendTotalInRange(start, end)
        val income           = transactionDao.getIncomeInRange(start, end)
        val avg              = transactionDao.getAverageTransactionInRange(start, end)
        val catTotals        = transactionDao.getCategoryTotals(start, end)
        val merchants        = transactionDao.getTopMerchants(start, end, 10)
        val feeSummary       = transactionDao.getFeeSummaryInRange(start, end)
        val feeByCategory    = transactionDao.getFeeByCategory(start, end)
        val uncatCount       = transactionDao.countUncategorizedInRange(start, end)
        val uncatAmount      = transactionDao.getUncategorizedAmountInRange(start, end)
        val curMonthSpend    = transactionDao.getSpendTotalInRange(curStart, curEnd)
        val prevMonthSpend   = transactionDao.getSpendTotalInRange(prevStart, prevEnd)

        // 4 weekly buckets (oldest → newest), last 4 weeks from end of range
        val endDay  = LocalDate.now(zone)
        val weekBuckets: List<Pair<String, String>> = (3 downTo 0).map { weeksAgo ->
            val wEnd   = endDay.minusWeeks(weeksAgo.toLong())
            val wStart = wEnd.minusWeeks(1).plusDays(1)
            isoDay(wStart) to isoDay(wEnd)
        }

        // Weekly amounts per category
        val catWeeklyMap = mutableMapOf<String, MutableList<Double>>()
        weekBuckets.forEach { (ws, we) ->
            val weekCats = transactionDao.getCategoryTotals(ws, we)
            weekCats.forEach { ct ->
                val key = ct.category ?: "uncategorized"
                catWeeklyMap.getOrPut(key) { mutableListOf(0.0, 0.0, 0.0, 0.0) }
            }
        }
        weekBuckets.forEachIndexed { i, (ws, we) ->
            val weekCats = transactionDao.getCategoryTotals(ws, we)
            weekCats.forEach { ct ->
                val key = ct.category ?: "uncategorized"
                catWeeklyMap.getOrPut(key) { mutableListOf(0.0, 0.0, 0.0, 0.0) }[i] = ct.total
            }
        }

        // Top merchant per category (approximation: top global merchant per category)
        val topMerchantByCategory: Map<String, String?> = catTotals.associate { ct ->
            val cat = ct.category ?: "uncategorized"
            cat to merchants.firstOrNull()?.merchant
        }

        val sparklines = catTotals.map { ct ->
            val cat     = ct.category ?: "uncategorized"
            val weekAmt = catWeeklyMap[cat] ?: List(4) { 0.0 }
            CategorySparklineItem(
                category      = cat,
                color         = categoryColor(cat),
                total         = ct.total,
                pctOfTotal    = if (spend > 0) (ct.total / spend) * 100.0 else 0.0,
                weeklyAmounts = weekAmt,
                topMerchant   = topMerchantByCategory[cat],
            )
        }.sortedByDescending { it.total }.take(8)

        val topFeeCategory = feeByCategory.firstOrNull()?.category
        val feesData = AnalyticsFeesData(
            total       = feeSummary.total,
            topCategory = topFeeCategory,
            avgFee      = feeSummary.avgFee,
            txCount     = feeSummary.txCount,
        )

        _uiState.update {
            it.copy(
                isLoading           = false,
                totalSpend          = spend,
                totalIncome         = income,
                net                 = income - spend,
                averageTransaction  = avg,
                categorySparklines  = sparklines,
                feesData            = feesData,
                uncategorizedCount  = uncatCount,
                uncategorizedAmount = uncatAmount,
                currentMonthSpend   = curMonthSpend,
                prevMonthSpend      = prevMonthSpend,
            )
        }
    }

    // ─── Insights tab ─────────────────────────────────────────────────────────

    private suspend fun loadInsightsTabInner() {
        val curKey  = currentMonthKey()
        val prevKey = previousMonthKey()
        val (curStart, curEnd)   = isoRange(curKey)
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
            val totals = transactionDao.getMonthTotals(key)
            val label  = Instant.ofEpochMilli(monthKeyToStartMillis(key))
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
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun isoRange(monthKey: String): Pair<String, String> {
        val start = Instant.ofEpochMilli(monthKeyToStartMillis(monthKey)).atZone(zone)
            .toLocalDateTime().format(isoDtFmt)
        val end   = Instant.ofEpochMilli(monthKeyToEndMillis(monthKey)).atZone(zone)
            .toLocalDateTime().format(isoDtFmt)
        return start to end
    }

    private fun rangeToIso(range: AnalyticsRange): Pair<String, String> {
        val endDate   = LocalDate.now(zone)
        val startDate = when (range) {
            AnalyticsRange.ThisWeek  -> {
                val day = endDate.dayOfWeek.value // 1=Mon … 7=Sun
                endDate.minusDays((day - 1).toLong())
            }
            AnalyticsRange.ThisMonth -> endDate.withDayOfMonth(1)
        }
        return isoDay(startDate) to isoDay(endDate, endOfDay = true)
    }

    private fun isoDay(date: LocalDate, endOfDay: Boolean = false): String =
        if (endOfDay) "${date}T23:59:59" else "${date}T00:00:00"

    private fun monthOffsetKey(monthsAgo: Int): String =
        LocalDate.now(zone).minusMonths(monthsAgo.toLong())
            .format(DateTimeFormatter.ofPattern("yyyy-MM"))

    companion object {
        // Mirrors analyticsService.ts categoryColors
        fun categoryColor(category: String): String = when (category.lowercase()) {
            "food"          -> "#F59E0B"
            "transport"     -> "#3B82F6"
            "utilities"     -> "#8B5CF6"
            "groceries"     -> "#10B981"
            "rent"          -> "#EF4444"
            "airtime"       -> "#06B6D4"
            "entertainment" -> "#EC4899"
            "health"        -> "#F97316"
            "education"     -> "#6366F1"
            "shopping"      -> "#D946EF"
            "savings"       -> "#22C55E"
            "investment"    -> "#14B8A6"
            "income"        -> "#34D399"
            else            -> "#6B7280"
        }
    }
}
