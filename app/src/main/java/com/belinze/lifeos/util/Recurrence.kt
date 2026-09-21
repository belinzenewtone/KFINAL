package com.belinze.lifeos.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Pure recurrence math — 1:1 port of RFINAL src/utils/recurrence.ts.
 *
 * Advances a recurring rule's next_run_at past "now" by its cadence, so a
 * rule that fired (or should have fired) while the app was closed keeps
 * scheduling its next occurrence instead of going permanently silent.
 *
 * Monthly/yearly arithmetic anchors the original day-of-month and clamps to
 * the target month's length, so "31st monthly" never drifts through short
 * months (Jan 31 → Feb 28 → Mar 31, not Mar 3), and Feb 29 yearly anchors
 * clamp to Feb 28 on non-leap years.
 */

private val ISO_OFFSET: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

private fun daysInMonth(year: Int, month0: Int): Int =
    LocalDate.of(year, month0 + 1, 1).lengthOfMonth()

/** Add [months] preserving the anchored day-of-month, clamped to the target month's length. */
private fun addMonthsClamped(d: ZonedDateTime, months: Long, anchorDay: Int): ZonedDateTime {
    val next = d.plusMonths(months)
    val maxDay = next.toLocalDate().lengthOfMonth()
    return next.withDayOfMonth(minOf(anchorDay, maxDay))
}

/**
 * Advance a recurring rule's `next_run_at` past [nowMs] by its [cadence].
 * Returns null when the timestamp is already in the future (nothing to do),
 * invalid, or the cadence is unknown.
 */
fun advanceCadencePastNow(
    iso: String?,
    cadence: String?,
    nowMs: Long = System.currentTimeMillis(),
): String? {
    if (iso.isNullOrBlank() || cadence.isNullOrBlank()) return null

    val base: ZonedDateTime = try {
        when {
            iso.length <= 10 -> LocalDate.parse(iso.take(10)).atStartOfDay(ZoneId.systemDefault())
            else -> try {
                ZonedDateTime.parse(iso, ISO_OFFSET)
            } catch (_: DateTimeParseException) {
                LocalDateTime.parse(iso.take(19)).atZone(ZoneId.systemDefault())
            }
        }
    } catch (_: Exception) {
        return null
    }

    if (base.toInstant().toEpochMilli() > nowMs) return null

    val anchorDay = base.dayOfMonth
    var cursor = base
    var i = 0
    while (i < 5000 && cursor.toInstant().toEpochMilli() <= nowMs) {
        cursor = when (cadence) {
            "hourly"   -> cursor.plusHours(1)
            "daily"    -> cursor.plusDays(1)
            "weekly"   -> cursor.plusDays(7)
            "biweekly" -> cursor.plusDays(14)
            "mon_fri"  -> {
                var next = cursor.plusDays(1)
                while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                    next = next.plusDays(1)
                }
                next
            }
            "monthly"  -> addMonthsClamped(cursor, 1, anchorDay)
            "yearly"   -> addMonthsClamped(cursor, 12, anchorDay)
            else       -> return null
        }
        i++
    }

    return if (cursor.toInstant().toEpochMilli() > nowMs) cursor.format(ISO_OFFSET) else null
}
