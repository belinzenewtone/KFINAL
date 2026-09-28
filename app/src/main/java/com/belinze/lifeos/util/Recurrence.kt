package com.belinze.lifeos.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

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

private val zone: TimeZone = TimeZone.currentSystemDefault()

/** Add [months] preserving the anchored day-of-month, clamped to the target month's length. */
private fun addMonthsClamped(instant: Instant, months: Int, anchorDay: Int): Instant {
    val ldt     = instant.toLocalDateTime(zone)
    val shifted = LocalDate(ldt.year, ldt.month, 1).plus(months, DateTimeUnit.MONTH)
    val maxDay  = lastDayOfMonth(shifted).dayOfMonth
    val clamped = LocalDate(shifted.year, shifted.month, minOf(anchorDay, maxDay))
    return LocalDateTime(clamped.year, clamped.month, clamped.dayOfMonth,
        ldt.hour, ldt.minute, ldt.second, ldt.nanosecond).toInstant(zone)
}

/**
 * Advance a recurring rule's `next_run_at` past [nowMs] by its [cadence].
 * Returns null when the timestamp is already in the future (nothing to do),
 * invalid, or the cadence is unknown.
 */
fun advanceCadencePastNow(
    iso: String?,
    cadence: String?,
    nowMs: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
): String? {
    if (iso.isNullOrBlank() || cadence.isNullOrBlank()) return null

    val base: Instant = try {
        when {
            iso.length <= 10 -> LocalDate.parse(iso.take(10)).atStartOfDayIn(zone)
            else -> try {
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse(iso).toInstantUsingOffset()
            } catch (_: Exception) {
                LocalDateTime.parse(iso.take(19)).toInstant(zone)
            }
        }
    } catch (_: Exception) {
        return null
    }

    if (base.toEpochMilliseconds() > nowMs) return null

    val anchorDay = base.toLocalDateTime(zone).dayOfMonth
    var cursor    = base
    var i         = 0
    while (i < 5000 && cursor.toEpochMilliseconds() <= nowMs) {
        cursor = when (cadence) {
            "hourly"   -> cursor + 1.hours
            "daily"    -> cursor.plusDays(1)
            "weekly"   -> cursor.plusDays(7)
            "biweekly" -> cursor.plusDays(14)
            "mon_fri"  -> {
                var next = cursor.plusDays(1)
                while (next.toLocalDateTime(zone).dayOfWeek == DayOfWeek.SATURDAY ||
                    next.toLocalDateTime(zone).dayOfWeek == DayOfWeek.SUNDAY) {
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

    return if (cursor.toEpochMilliseconds() > nowMs) formatInstantAsIsoOffset(cursor, zone) else null
}

private fun Instant.plusDays(n: Int): Instant = plus(n.toLong() * 24, DateTimeUnit.HOUR)
