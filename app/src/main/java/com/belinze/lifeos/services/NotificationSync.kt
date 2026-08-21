package com.belinze.lifeos.services

import android.content.Context
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.dao.PlannerDao
import com.belinze.lifeos.data.db.dao.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationSync — 1:1 port of src/services/notificationSyncService.ts.
 *
 * Reconciles the scheduled notification state with the current database and
 * user settings. Called on app startup (after hydration) and after any
 * task/event/recurring/bill mutation.
 */
@Singleton
class NotificationSync
    @Inject
    constructor(
    @ApplicationContext private val context: Context,
    private val scheduler:   NotificationScheduler,
    private val prefs:       AppPreferences,
    private val taskDao:     TaskDao,
    private val eventDao:    EventDao,
    private val plannerDao:  PlannerDao,
    private val budgetDao:   BudgetDao,
) {
    private fun parseJsonOffsets(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            // Stored as a JSON array of numbers; tolerate leading/trailing brackets.
            val cleaned = raw.trim().removePrefix("[").removeSuffix("]")
            if (cleaned.isBlank()) {
                emptyList()
            } else {
                cleaned.split(",").mapNotNull { it.trim().toIntOrNull() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun syncAll(prefState: com.belinze.lifeos.data.datastore.AppPreferenceState) {
        scheduler.ensureChannels()
        val enabled = prefState.notificationsEnabled

        // ── Daily digest ─────────────────────────────────────────────────
        if (enabled && prefState.notifDailyDigest) {
            scheduler.scheduleDailyDigest("06:30")
        } else {
            scheduler.cancelDailyDigest()
        }

        // ── Tasks ────────────────────────────────────────────────────────
        if (enabled && prefState.notifReminders) {
            val tasks = taskDao.getAll()
            tasks.filter { it.status == "active" && it.deadline != null }
                .forEach { t ->
                    scheduler.scheduleTaskReminders(
                        taskId = t.id,
                        title  = t.title,
                        deadlineIso = t.deadline ?: return@forEach,
                        offsetsMin  = parseJsonOffsets(t.reminderOffsets),
                        alarm       = t.alarmEnabled != 0,
                    )
                }
        }

        // ── Events ───────────────────────────────────────────────────────
        if (enabled && prefState.notifReminders) {
            val events = eventDao.getAll()
            events.filter { it.status != "completed" }.forEach { e ->
                scheduler.scheduleEventReminders(
                    eventId = e.id,
                    title   = e.title,
                    eventDateIso = e.date,
                    offsetsMin   = parseJsonOffsets(e.reminderOffsets),
                    alarm        = e.alarmEnabled != 0,
                    type         = e.type,
                    reminderTimeOfDayMinutes = e.reminderTimeOfDayMinutes,
                )
            }
        }

        // ── Recurring rules ──────────────────────────────────────────────
        if (enabled && prefState.notifRecurringRules) {
            val rules = plannerDao.getAllRules()
            rules.filter { it.enabled != 0 }.forEach { r ->
                r.nextRunAt?.let {
                    scheduler.scheduleRecurringReminder(r.id, r.title, it, r.amount)
                }
            }
        }

        // ── Bills ────────────────────────────────────────────────────────
        if (enabled && prefState.notifReminders) {
            val bills = plannerDao.getAllBills()
            bills.filter { it.isActive != 0 && it.paidStatus != 1 }.forEach { b ->
                b.nextDueDate?.let {
                    scheduler.scheduleBillReminder(b.id, b.title, it, b.amount)
                }
            }
        }
    }

    /** Called after a transaction mutation to re-evaluate budget thresholds. */
    fun evaluateBudgetAlerts() {
        // Budget thresholds are computed by the caller (TransactionViewModel);
        // this hook exists to keep the port symmetric with the RN service.
    }
}
