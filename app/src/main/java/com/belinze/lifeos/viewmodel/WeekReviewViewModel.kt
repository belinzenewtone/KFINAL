package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// ─────────────────────────────────────────────────────────────────────────────
// WeekReviewViewModel — full parity with WeekReviewScreen.tsx
//
// Loads per-day and per-week data from Room, computes health score using the
// same formula as the React app, and exposes DOW spend bars with averages.
// ─────────────────────────────────────────────────────────────────────────────

/** 7-day spend bar data for chart rendering. */
@Immutable
data class DayBar(
    val dayOfWeek: Int,      // 1=Mon … 7=Sun (ISO)
    val dateStr:   String,   // "YYYY-MM-DD"
    val amount:    Double,
    val avg:       Double,   // rolling 4-week average for this DOW
    val isFuture:  Boolean,
)

/** Single narrative item for "What Changed?" section. */
@Immutable
data class ChangeItem(
    val icon:      String,   // icon name
    val text:      String,
    val sentiment: String,   // "good" | "warn" | "neutral"
)

@Immutable
data class WeekReviewUiState(
    val isLoading:       Boolean                   = true,
    val weekLabel:       String                    = "",        // "Aug 1 – Aug 7, 2025"
    val greeting:        String                    = "",        // "Good morning, Alex"
    val healthScore:     Int                       = 0,         // 0-100
    val scoreLabel:      String                    = "",        // Excellent|Good|Fair|Needs attention
    val scoreColor:      Long                      = 0xFF22C55E,
    val dayBars:         ImmutableList<DayBar>     = persistentListOf(),
    val changeItems:     ImmutableList<ChangeItem> = persistentListOf(),
    val weekSpend:       Double         = 0.0,
    val topCategory:     String         = "",
    val tasksCompleted:  Int            = 0,
    val tasksPending:    Int            = 0,
    val error:           String?        = null,
)

@HiltViewModel
class WeekReviewViewModel
    @Inject
    constructor(
    private val transactionDao: TransactionDao,
    private val taskDao:        TaskDao,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeekReviewUiState())
    val uiState: StateFlow<WeekReviewUiState> = _uiState.asStateFlow()

    private val zone    = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Exposed for the greeting — reads profile name. */
    val prefState = appPreferences.state.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = com.belinze.lifeos.data.datastore.AppPreferenceState(),
    )

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val today   = LocalDate.now(zone)
                val monThis = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunThis = monThis.plusDays(6)

                val startStr = monThis.format(dateFmt)
                val endStr   = sunThis.format(dateFmt) + "T23:59:59"

                // Previous week
                val monPrev  = monThis.minusWeeks(1)
                val sunPrev  = monPrev.plusDays(6)
                val prevStart = monPrev.format(dateFmt)
                val prevEnd   = sunPrev.format(dateFmt) + "T23:59:59"

                // ─ Current week data ─
                val daySpends    = transactionDao.getDaySpends(startStr, endStr)
                val weekSpend    = transactionDao.getSpendTotalInRange(startStr, endStr)
                val prevSpend    = transactionDao.getSpendTotalInRange(prevStart, prevEnd)
                val topCategory  = transactionDao.getTopCategoryInRange(startStr, endStr) ?: ""
                val uncatCount   = transactionDao.countUncategorizedInRange(startStr, endStr)
                val fulizaCount  = transactionDao.countFulizaInRange(startStr, endStr)

                // ─ Tasks — scope both to the same week for a fair rate ─
                val tasksDone    = taskDao.countCompletedSince(startStr)
                val tasksPending = taskDao.countPendingCreatedSince(startStr)

                // ─ DOW averages (4 prior complete weeks) ─
                val dowAvg = computeDowAverages(monThis)

                // ─ Build day bars ─
                val spendByDay = daySpends.associate { it.day to it.total }
                val dayBars = (0..6).map { i ->
                    val date   = monThis.plusDays(i.toLong())
                    val dow    = date.dayOfWeek.value   // 1=Mon … 7=Sun
                    val dayStr = date.format(dateFmt)
                    DayBar(
                        dayOfWeek = dow,
                        dateStr   = dayStr,
                        amount    = spendByDay[dayStr] ?: 0.0,
                        avg       = dowAvg[dow] ?: 0.0,
                        isFuture  = date.isAfter(today),
                    )
                }

                // ─ Health score (exact React formula) ─
                var score = 50
                // Spending vs last week
                score += when {
                    prevSpend <= 0.0                    -> 0
                    weekSpend < prevSpend               -> 20
                    weekSpend <= prevSpend * 1.2        -> 10
                    weekSpend > prevSpend * 1.5         -> -20
                    else                                -> 0
                }
                // Uncategorized
                score += when {
                    uncatCount == 0  -> 20
                    uncatCount <= 3  -> 10
                    uncatCount > 8   -> -10
                    else             -> 0
                }
                // Fuliza
                score += when {
                    fulizaCount == 0 -> 10
                    fulizaCount > 2  -> -10
                    else             -> 0
                }
                // Tasks
                val totalTasks = tasksDone + tasksPending
                val taskRate   = if (totalTasks > 0) tasksDone.toDouble() / totalTasks else 0.0
                score += when {
                    taskRate >= 0.8  -> 10
                    taskRate >= 0.5  -> 5
                    else             -> 0
                }
                score = score.coerceIn(0, 100)

                val (scoreLabel, scoreColor) = when {
                    score >= 80 -> "Excellent"      to 0xFF22C55EL
                    score >= 60 -> "Good"           to 0xFFF59E0BL
                    score >= 40 -> "Fair"           to 0xFFF97316L
                    else        -> "Needs attention" to 0xFFEF4444L
                }

                // ─ Week label ─
                val monFmt = DateTimeFormatter.ofPattern("MMM d")
                val sunFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
                val weekLabel = "${monThis.format(monFmt)} – ${sunThis.format(sunFmt)}"

                // ─ Greeting ─
                val hourNow  = java.time.ZonedDateTime.now(zone).hour
                val greeting = when {
                    hourNow < 12 -> "Good morning"
                    hourNow < 17 -> "Good afternoon"
                    else         -> "Good evening"
                }

                // ─ What Changed items (up to 3) ─
                val changes = buildChangeItems(weekSpend, prevSpend, uncatCount, fulizaCount, topCategory)

                _uiState.value = WeekReviewUiState(
                    isLoading      = false,
                    weekLabel      = weekLabel,
                    greeting       = greeting,
                    healthScore    = score,
                    scoreLabel     = scoreLabel,
                    scoreColor     = scoreColor,
                    dayBars        = dayBars.toImmutableList(),
                    changeItems    = changes.toImmutableList(),
                    weekSpend      = weekSpend,
                    topCategory    = topCategory,
                    tasksCompleted = tasksDone,
                    tasksPending   = tasksPending,
                )
            } catch (e: Exception) {
                _uiState.value = WeekReviewUiState(isLoading = false, error = e.message)
            }
        }
    }

    // ─── DOW averages ─────────────────────────────────────────────────────────

    /**
     * For each of the 4 prior complete Mon-Sun weeks, fetch per-day spend,
     * then average spend per day-of-week (1=Mon … 7=Sun).
     */
    private suspend fun computeDowAverages(thisMonday: LocalDate): Map<Int, Double> {
        val sums   = mutableMapOf<Int, Double>()   // dow → total spend across 4 weeks
        val counts = mutableMapOf<Int, Int>()

        for (weeksBack in 1..4) {
            val mon   = thisMonday.minusWeeks(weeksBack.toLong())
            val sun   = mon.plusDays(6)
            val start = mon.format(dateFmt)
            val end   = sun.format(dateFmt) + "T23:59:59"
            val days  = transactionDao.getDaySpends(start, end)
            days.forEach { d ->
                val dow = LocalDate.parse(d.day).dayOfWeek.value
                sums[dow]   = (sums[dow] ?: 0.0) + d.total
                counts[dow] = (counts[dow] ?: 0) + 1
            }
        }
        return (1..7).associate { dow ->
            val s = sums[dow] ?: 0.0
            val c = counts[dow] ?: 4           // divide by 4 even if no spend that day
            dow to s / c
        }
    }

    // ─── Narrative items ──────────────────────────────────────────────────────

    private fun buildChangeItems(
        weekSpend: Double,
        prevSpend: Double,
        uncatCount: Int,
        fulizaCount: Int,
        topCategory: String,
    ): List<ChangeItem> {
        val items = mutableListOf<ChangeItem>()

        // 1. Spending change vs last week
        if (prevSpend > 0) {
            val pct = ((weekSpend - prevSpend) / prevSpend * 100).toInt()
            when {
                pct <= -10 -> items += ChangeItem(
                    icon = "trending-down-outline",
                    text = "Spent ${-pct}% less than last week — great job!",
                    sentiment = "good",
                )
                pct >= 20 -> items += ChangeItem(
                    icon = "trending-up-outline",
                    text = "Spent $pct% more than last week",
                    sentiment = "warn",
                )
                else -> items += ChangeItem(
                    icon = "bar-chart-outline",
                    text = "Spending similar to last week",
                    sentiment = "neutral",
                )
            }
        }

        // 2. Uncategorized warning
        if (uncatCount > 0) {
            items += ChangeItem(
                icon = "warning-outline",
                text = "$uncatCount transaction${if (uncatCount > 1) "s" else ""} still uncategorized",
                sentiment = "warn",
            )
        } else {
            items += ChangeItem(
                icon = "checkmark-circle-outline",
                text = "All transactions categorized",
                sentiment = "good",
            )
        }

        // 3. Fuliza or top category
        if (fulizaCount > 0) {
            items += ChangeItem(
                icon = "alert-circle-outline",
                text = "Used Fuliza $fulizaCount time${if (fulizaCount > 1) "s" else ""} this week",
                sentiment = "warn",
            )
        } else if (topCategory.isNotBlank()) {
            items += ChangeItem(
                icon = "bar-chart-outline",
                text = "Biggest spending category: ${topCategory.replaceFirstChar { it.uppercaseChar() }}",
                sentiment = "neutral",
            )
        }

        return items.take(3)
    }
}
