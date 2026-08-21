package com.belinze.lifeos.services

import com.belinze.lifeos.data.datastore.AppPreferenceState
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BudgetAlertService — 1:1 port of src/services/budgetAlertService.ts.
 *
 * Evaluates each active budget against current-month category spend and fires a
 * notification when the spend crosses the alert thresholds:
 *   ‣ global 3-level thresholds (high 90 / medium 75 / low 50 by default)
 *   ‣ per-budget custom `alert_threshold` (fires independently, keyed `custom`)
 *
 * Dedup is keyed `category|level|yearMonth` and persisted in DataStore, so a
 * single category only notifies once per month per level.
 */
@Singleton
class BudgetAlertService
    @Inject
    constructor(
    private val budgetDao:  BudgetDao,
    private val transactionDao: TransactionDao,
    private val prefs:      AppPreferences,
    private val scheduler:  NotificationScheduler,
) {
    private val zone = ZoneId.systemDefault()
    private val isoDtFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /** Evaluate a single budget's spend and fire if a threshold is crossed. */
    suspend fun checkBudgetThresholds(state: AppPreferenceState, category: String) {
        if (!shouldCheck(state)) return
        if (category.isBlank()) return

        val budget = budgetDao.getAll().firstOrNull { it.category.equals(category, ignoreCase = true) }
            ?: return
        if (budget.isActive != 1) return

        val spent = categorySpend(category)
        evaluateBudgetThreshold(state, budget, spent)
    }

    /** Evaluate every active budget (used on cold start / after SMS import). */
    suspend fun checkAllBudgetThresholds(state: AppPreferenceState) {
        if (!shouldCheck(state)) return
        val budgets = budgetDao.getAll().filter { it.isActive == 1 }
        val catSpend = categorySpendMap()
        budgets.forEach { budget ->
            val spent = catSpend[budget.category.lowercase()] ?: 0.0
            evaluateBudgetThreshold(state, budget, spent)
        }
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    private fun shouldCheck(state: AppPreferenceState): Boolean =
        state.notificationsEnabled && state.budgetThresholdAlerts && state.notifBudgetAlerts

    private suspend fun evaluateBudgetThreshold(
        state: AppPreferenceState,
        budget: com.belinze.lifeos.data.db.entity.BudgetEntity,
        spent: Double,
    ) {
        val yearMonth = currentYearMonth()
        val pct = if (budget.limitAmount > 0) Math.round((spent / budget.limitAmount) * 100).toInt() else 0
        val category = budget.category

        // ── Per-budget custom threshold ──────────────────────────────────
        val customPct = budget.alertThreshold
        if (customPct != null && customPct > 0 && pct >= customPct) {
            val customKey = "$category|custom|$yearMonth"
            if (state.firedBudgetAlerts[customKey] == null) {
                scheduler.postBudgetAlert(category, pct)
                prefs.markBudgetAlertFired(customKey, Instant.now().toString())
            }
        }

        // ── Global 3-level alert ─────────────────────────────────────────
        var highestLevel: String? = null
        val thresholds = mapOf(
            "high"   to state.alertThresholdHigh,
            "medium" to state.alertThresholdMedium,
            "low"    to state.alertThresholdLow,
        )
        for (level in listOf("high", "medium", "low")) {
            if (pct >= (thresholds[level] ?: 0)) { highestLevel = level; break }
        }
        if (highestLevel == null) return

        val highestKey = "$category|$highestLevel|$yearMonth"
        if (state.firedBudgetAlerts[highestKey] != null) return

        scheduler.postBudgetAlert(category, pct)

        // Mark this level and all lower levels as fired (once per month).
        val highestThreshold = thresholds[highestLevel] ?: 0
        for (level in listOf("high", "medium", "low")) {
            if ((thresholds[level] ?: 0) <= highestThreshold) {
                prefs.markBudgetAlertFired("$category|$level|$yearMonth", Instant.now().toString())
            }
        }
    }

    private suspend fun categorySpend(category: String): Double =
        categorySpendMap()[category.lowercase()] ?: 0.0

    private suspend fun categorySpendMap(): Map<String, Double> {
        val key = currentMonthKey()
        val startIso = Instant.ofEpochMilli(monthKeyToStartMillis(key))
            .atZone(zone).format(isoDtFmt)
        val endIso = Instant.ofEpochMilli(monthKeyToEndMillis(key))
            .atZone(zone).format(isoDtFmt)
        return transactionDao.getCategoryTotals(startIso, endIso)
            .associate { (it.category ?: "").lowercase() to it.total }
    }

    private fun currentYearMonth(): String {
        val now = java.time.LocalDate.now()
        return "${now.year}-${String.format(java.util.Locale.US, "%02d", now.monthValue)}"
    }
}
