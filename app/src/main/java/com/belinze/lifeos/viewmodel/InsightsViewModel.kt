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
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// InsightsViewModel
// Mirrors the 2-tab React AnalyticsScreen (Analytics tab + Insights tab).
// ─────────────────────────────────────────────────────────────────────────────

// ── Enums ─────────────────────────────────────────────────────────────────────
enum class InsightsPeriod { ThisMonth, LastMonth, Last3Months, Last6Months, ThisYear }
enum class AnalyticsTab   { Analytics, Insights }
enum class AnalyticsRange { ThisWeek, ThisMonth }
enum class InsightsTrend  { Increasing, Decreasing, Stable }

// ── Data classes shared with screen ──────────────────────────────────────────

/** Month label + spend + income totals — drives the bar chart. */
data class MonthBar(
    val label:      String,   // "Jan"
    val fullLabel:  String,   // "Jan 2026"
    val monthKey:   String,   // "2026-01"
    val monthOffset: Int,     // 0=current, -1=last month, etc.
    val expense:    Double,
    val income:     Double,
    val txCount:    Int,
)

data class CategoryPct(val category: String, val amount: Double, val pct: Double)

data class MonthBreakdownItem(
    val monthKey:      String,
    val label:         String,
    val fullLabel:     String,
    val monthOffset:   Int,
    val expense:       Double,
    val income:        Double,
    val txCount:       Int,
    val topCategories: List<CategoryPct>,
    val delta:         Double?,   // % vs prior month; null for first
)

data class PaydayPulse(
    val postPaydayAvgPerDay: Double,
    val otherDaysAvgPerDay:  Double,
    val incomeEventsCount:   Int,
)

data class SizeBreakdown(
    val microCount:  Int    = 0,
    val mediumCount: Int    = 0,
    val largeCount:  Int    = 0,
    val microTotal:  Double = 0.0,
    val mediumTotal: Double = 0.0,
    val largeTotal:  Double = 0.0,
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

// ── UI State ─────────────────────────────────────────────────────────────────

data class InsightsUiState(
    val isLoading: Boolean = true,

    // Tab & range
    val activeTab:     AnalyticsTab   = AnalyticsTab.Analytics,
    val dateRange:     AnalyticsRange = AnalyticsRange.ThisMonth,
    val nudgeDismissed: Boolean       = false,

    // ── Analytics tab (range-dependent) ─────────────────────────────────────
    val totalSpend:          Double                      = 0.0,
    val totalIncome:         Double                      = 0.0,
    val net:                 Double                      = 0.0,
    val averageTransaction:  Double                      = 0.0,
    val categorySparklines:  List<CategorySparklineItem> = emptyList(),
    val feesData:            AnalyticsFeesData           = AnalyticsFeesData(0.0, null, 0.0, 0),
    val uncategorizedCount:  Int                         = 0,
    val uncategorizedAmount: Double                      = 0.0,
    // SpendingComparisonCard (always current/prev month)
    val currentMonthSpend:   Double                      = 0.0,
    val prevMonthSpend:      Double                      = 0.0,

    // ── Insights tab ─────────────────────────────────────────────────────────
    // Bar chart
    val monthBars:           List<MonthBar>              = emptyList(),
    // Summary tiles
    val avgExpense:          Double                      = 0.0,
    val totalTracked:        Double                      = 0.0,
    // Spending Insights card
    val highestMonth:        MonthBar?                   = null,
    val lowestMonthWithData: MonthBar?                   = null,
    val topCategoryAllTime:  Pair<String, Double>?       = null,  // (category, pct)
    val trend:               InsightsTrend               = InsightsTrend.Stable,
    // History accordion
    val monthBreakdown:      List<MonthBreakdownItem>    = emptyList(),
    // Payday Pulse
    val paydayPulse:         PaydayPulse?                = null,
    // Spend Anatomy
    val sizeBreakdown:       SizeBreakdown               = SizeBreakdown(),

    // Legacy (kept for compatibility)
    val period:         InsightsPeriod      = InsightsPeriod.ThisMonth,
    val currentTotals:  MonthTotals?        = null,
    val previousTotals: MonthTotals?        = null,
    val uncategorized:  Int                 = 0,
    val pendingReview:  Int                 = 0,
    val feeTotal:       Double              = 0.0,
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
    private val mthFmt   = DateTimeFormatter.ofPattern("MMM")       // "Jan"
    private val mthYrFmt = DateTimeFormatter.ofPattern("MMM yyyy")  // "Jan 2026"

    init { load() }

    fun setActiveTab(tab: AnalyticsTab)     { _uiState.update { it.copy(activeTab = tab) } }
    fun setDateRange(range: AnalyticsRange) {
        _uiState.update { it.copy(dateRange = range) }
        loadAnalyticsTab()
    }
    fun dismissNudge()                      { _uiState.update { it.copy(nudgeDismissed = true) } }
    fun setPeriod(period: InsightsPeriod)   {
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
        val range          = _uiState.value.dateRange
        val (start, end)   = rangeToIso(range)
        val curKey         = currentMonthKey()
        val prevKey        = previousMonthKey()
        val (curStart, curEnd)   = isoRange(curKey)
        val (prevStart, prevEnd) = isoRange(prevKey)

        val spend       = transactionDao.getSpendTotalInRange(start, end)
        val income      = transactionDao.getIncomeInRange(start, end)
        val avg         = transactionDao.getAverageTransactionInRange(start, end)
        val catTotals   = transactionDao.getCategoryTotals(start, end)
        val merchants   = transactionDao.getTopMerchants(start, end, 10)
        val feeSummary  = transactionDao.getFeeSummaryInRange(start, end)
        val feeByCat    = transactionDao.getFeeByCategory(start, end)
        val uncatCount  = transactionDao.countUncategorizedInRange(start, end)
        val uncatAmt    = transactionDao.getUncategorizedAmountInRange(start, end)
        val curMonthSpend  = transactionDao.getSpendTotalInRange(curStart, curEnd)
        val prevMonthSpend = transactionDao.getSpendTotalInRange(prevStart, prevEnd)

        // 4 weekly buckets (oldest → newest), last 4 weeks
        val endDay = LocalDate.now(zone)
        val weekBuckets = (3 downTo 0).map { weeksAgo ->
            val wEnd   = endDay.minusWeeks(weeksAgo.toLong())
            val wStart = wEnd.minusWeeks(1).plusDays(1)
            isoDay(wStart) to isoDay(wEnd, true)
        }

        val catWeeklyMap = mutableMapOf<String, MutableList<Double>>()
        weekBuckets.forEachIndexed { i, (ws, we) ->
            val weekCats = transactionDao.getCategoryTotals(ws, we)
            weekCats.forEach { ct ->
                val key = ct.category ?: "uncategorized"
                catWeeklyMap.getOrPut(key) { mutableListOf(0.0, 0.0, 0.0, 0.0) }[i] = ct.total
            }
        }

        val topMerchant = merchants.firstOrNull()?.merchant
        val sparklines = catTotals.map { ct ->
            val cat = ct.category ?: "uncategorized"
            CategorySparklineItem(
                category      = cat,
                color         = categoryColor(cat),
                total         = ct.total,
                pctOfTotal    = if (spend > 0) (ct.total / spend) * 100.0 else 0.0,
                weeklyAmounts = catWeeklyMap[cat] ?: List(4) { 0.0 },
                topMerchant   = topMerchant,
            )
        }.sortedByDescending { it.total }.take(8)

        val feesData = AnalyticsFeesData(
            total       = feeSummary.total,
            topCategory = feeByCat.firstOrNull()?.category,
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
                uncategorizedAmount = uncatAmt,
                currentMonthSpend   = curMonthSpend,
                prevMonthSpend      = prevMonthSpend,
            )
        }
    }

    // ─── Insights tab ─────────────────────────────────────────────────────────

    private suspend fun loadInsightsTabInner() {
        val now          = LocalDate.now(zone)
        // 6-month window: 5 months ago (start of month) → now
        val sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).toString() + "T00:00:00"
        val endIso       = now.toString() + "T23:59:59"
        val curKey       = currentMonthKey()
        val prevKey      = previousMonthKey()

        val monthRows   = transactionDao.getMonthlyTotalsRange(sixMonthsAgo)
        val catRows     = transactionDao.getMonthlyCategoryBreakdown(sixMonthsAgo)
        val incomeDates = transactionDao.getIncomeDates(sixMonthsAgo).map { it.dt }
        val daySpends   = transactionDao.getDaySpends(sixMonthsAgo, endIso)
        val sizeRow     = transactionDao.getSizeBreakdown(sixMonthsAgo)
        val curTotals   = transactionDao.getMonthTotals(curKey)
        val prevTotals  = transactionDao.getMonthTotals(prevKey)
        val (curStart, curEnd) = isoRange(curKey)
        val feeTotal    = transactionDao.getFeeTotal(curStart, curEnd) ?: 0.0
        val uncat       = transactionDao.countUncategorized()
        val pending     = transactionDao.countPendingReview()

        // Build 6-slot array (oldest → newest), filling in missing months with zeros
        val months = (5 downTo 0).map { monthsAgo ->
            val d        = now.minusMonths(monthsAgo.toLong()).withDayOfMonth(1)
            val monthKey = d.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val row      = monthRows.find { it.monthKey == monthKey }
            MonthBar(
                label       = d.atStartOfDay(zone).toInstant().atZone(zone).format(mthFmt),
                fullLabel   = d.atStartOfDay(zone).toInstant().atZone(zone).format(mthYrFmt),
                monthKey    = monthKey,
                monthOffset = -monthsAgo,
                expense     = row?.expense ?: 0.0,
                income      = row?.income  ?: 0.0,
                txCount     = row?.txCount ?: 0,
            )
        }

        // Category breakdown per month
        val breakdown: List<MonthBreakdownItem> = months.mapIndexed { i, m ->
            val monthCats    = catRows.filter { it.monthKey == m.monthKey }
            val topCats      = monthCats.take(5).map { r ->
                CategoryPct(
                    category = r.category ?: "uncategorized",
                    amount   = r.total,
                    pct      = if (m.expense > 0) (r.total / m.expense) * 100.0 else 0.0,
                )
            }
            val prev  = months.getOrNull(i - 1)
            val delta = if (prev != null && prev.expense > 0)
                ((m.expense - prev.expense) / prev.expense) * 100.0
            else null
            MonthBreakdownItem(
                monthKey      = m.monthKey,
                label         = m.label,
                fullLabel     = m.fullLabel,
                monthOffset   = m.monthOffset,
                expense       = m.expense,
                income        = m.income,
                txCount       = m.txCount,
                topCategories = topCats,
                delta         = delta,
            )
        }.reversed()  // newest first for history list

        // Summary stats
        val monthsWithData   = months.filter { it.expense > 0 }
        val totalTracked     = monthsWithData.sumOf { it.expense }
        val avgExpense       = if (monthsWithData.isNotEmpty()) totalTracked / monthsWithData.size else 0.0
        val highestMonth     = monthsWithData.maxByOrNull { it.expense }
            ?: months.last()
        val lowestMonthWithData = if (monthsWithData.size > 1)
            monthsWithData.minByOrNull { it.expense } else null

        // Top category all-time (6-month window)
        val allCatMap = mutableMapOf<String, Double>()
        for (row in catRows) {
            val k = row.category ?: "uncategorized"
            allCatMap[k] = (allCatMap[k] ?: 0.0) + row.total
        }
        val allCatSorted  = allCatMap.entries.sortedByDescending { it.value }
        val grandTotal    = allCatSorted.sumOf { it.value }
        val topCategoryAllTime = allCatSorted.firstOrNull()?.let { (cat, amt) ->
            cat to if (grandTotal > 0) (amt / grandTotal) * 100.0 else 0.0
        }

        // Trend: last 3 months vs prior 3 months
        val last3Avg = months.takeLast(3).map { it.expense }.average()
        val prev3Avg = months.take(3).map { it.expense }.average()
        val trendPct = if (prev3Avg > 0) (last3Avg - prev3Avg) / prev3Avg else 0.0
        val trend    = when {
            trendPct > 0.05  -> InsightsTrend.Increasing
            trendPct < -0.05 -> InsightsTrend.Decreasing
            else             -> InsightsTrend.Stable
        }

        // Payday Pulse
        val dailySpendMap = daySpends.associate { it.day to it.total }
        val paydayPulse: PaydayPulse? = if (incomeDates.size >= 2) {
            val postDaySet = mutableSetOf<String>()
            for (incomeDate in incomeDates) {
                val base = LocalDate.parse(incomeDate)
                for (d in 0 until 7) {
                    postDaySet.add(base.plusDays(d.toLong()).toString())
                }
            }
            var postTotal = 0.0; var postDays = 0
            var otherTotal = 0.0; var otherDays = 0
            for ((day, total) in dailySpendMap) {
                if (postDaySet.contains(day)) { postTotal += total; postDays++ }
                else { otherTotal += total; otherDays++ }
            }
            if (postDays > 0 && otherDays > 0) {
                PaydayPulse(
                    postPaydayAvgPerDay = postTotal / postDays,
                    otherDaysAvgPerDay  = otherTotal / otherDays,
                    incomeEventsCount   = incomeDates.size,
                )
            } else null
        } else null

        // Size breakdown
        val sizeBd = SizeBreakdown(
            microCount  = sizeRow?.microCount  ?: 0,
            mediumCount = sizeRow?.mediumCount ?: 0,
            largeCount  = sizeRow?.largeCount  ?: 0,
            microTotal  = sizeRow?.microTotal  ?: 0.0,
            mediumTotal = sizeRow?.mediumTotal ?: 0.0,
            largeTotal  = sizeRow?.largeTotal  ?: 0.0,
        )

        _uiState.update {
            it.copy(
                isLoading           = false,
                monthBars           = months,
                avgExpense          = avgExpense,
                totalTracked        = totalTracked,
                highestMonth        = highestMonth,
                lowestMonthWithData = lowestMonthWithData,
                topCategoryAllTime  = topCategoryAllTime,
                trend               = trend,
                monthBreakdown      = breakdown,
                paydayPulse         = paydayPulse,
                sizeBreakdown       = sizeBd,
                currentTotals       = curTotals,
                previousTotals      = prevTotals,
                feeTotal            = feeTotal,
                uncategorized       = uncat,
                pendingReview       = pending,
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
                val day = endDate.dayOfWeek.value // 1=Mon…7=Sun
                endDate.minusDays((day - 1).toLong())
            }
            AnalyticsRange.ThisMonth -> endDate.withDayOfMonth(1)
        }
        return isoDay(startDate) to isoDay(endDate, true)
    }

    private fun isoDay(date: LocalDate, endOfDay: Boolean = false): String =
        if (endOfDay) "${date}T23:59:59" else "${date}T00:00:00"

    companion object {
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
            "restaurants"   -> "#F59E0B"
            "snacks"        -> "#F97316"
            else            -> "#6B7280"
        }
    }
}
