package com.belinze.lifeos.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// ─────────────────────────────────────────────────────────────────────────────
// DateUtils
//
// 1:1 port of src/utils/date.ts — all date helpers used in the app.
//
// The app stores timestamps as ISO-8601 strings or epoch-millis longs.
// TimeZone: device local (Nairobi = Africa/Nairobi, EAT UTC+3 by default).
// ─────────────────────────────────────────────────────────────────────────────

private val DEFAULT_ZONE: TimeZone = TimeZone.currentSystemDefault()

// ─── Formatters ─────────────────────────────────────────────────────────────

/** "12 Jan 2024" */
private val FMT_DISPLAY_DATE = LocalDate.Format {
    dayOfMonth(Padding.NONE); char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' ')
    year()
}

/** "12 Jan" (no year, used in lists) */
private val FMT_SHORT_DATE = LocalDate.Format {
    dayOfMonth(Padding.NONE); char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
}

/** "12 Jan 2024, 14:35" */
private val FMT_DISPLAY_DATETIME = LocalDateTime.Format {
    dayOfMonth(Padding.NONE); char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' ')
    year(); chars(", ")
    hour(Padding.ZERO); char(':'); minute(Padding.ZERO)
}

/** "14:35" */
private val FMT_TIME = LocalTime.Format {
    hour(Padding.ZERO); char(':'); minute(Padding.ZERO)
}

/** "Jan 2024" — for month headers in transaction lists */
private val FMT_MONTH_YEAR = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); year()
}

/** "2024-01" — used for groupBy month keys */
private val FMT_MONTH_KEY = LocalDate.Format {
    year(); char('-'); monthNumber(Padding.ZERO)
}

// ─── Epoch ↔ LocalDate ──────────────────────────────────────────────────────

fun epochMillisToLocalDate(epochMillis: Long, zone: TimeZone = DEFAULT_ZONE): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date

fun localDateToEpochMillis(date: LocalDate, zone: TimeZone = DEFAULT_ZONE): Long =
    date.atStartOfDayIn(zone).toEpochMilliseconds()

// ─── ISO string helpers ──────────────────────────────────────────────────────

fun nowIso(zone: TimeZone = DEFAULT_ZONE): String =
    formatInstantAsIsoOffset(Clock.System.now(), zone)

/** Formats an Instant as ISO 8601 with timezone offset, e.g. "2024-01-15T10:30:00+03:00". */
internal fun formatInstantAsIsoOffset(instant: Instant, zone: TimeZone = DEFAULT_ZONE): String {
    val ldt    = instant.toLocalDateTime(zone)
    val utcLdt = instant.toLocalDateTime(TimeZone.UTC)
    var diffSec = (ldt.hour - utcLdt.hour) * 3600L + (ldt.minute - utcLdt.minute) * 60
    if (diffSec > 43200) diffSec -= 86400
    if (diffSec < -43200) diffSec += 86400
    val sign = if (diffSec >= 0) "+" else "-"
    val abs  = kotlin.math.abs(diffSec)
    return "%04d-%02d-%02dT%02d:%02d:%02d%s%02d:%02d".format(
        ldt.year, ldt.monthNumber, ldt.dayOfMonth,
        ldt.hour, ldt.minute, ldt.second,
        sign, abs / 3600, (abs % 3600) / 60,
    )
}

// ─── Display formatters ──────────────────────────────────────────────────────

/**
 * "12 Jan 2024" — from epoch millis.
 * If [showYear] is false returns "12 Jan".
 */
fun formatDate(epochMillis: Long, showYear: Boolean = true): String {
    val date = epochMillisToLocalDate(epochMillis)
    return if (showYear) FMT_DISPLAY_DATE.format(date) else FMT_SHORT_DATE.format(date)
}

/** "12 Jan 2024, 14:35" */
fun formatDateTime(epochMillis: Long): String =
    FMT_DISPLAY_DATETIME.format(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(DEFAULT_ZONE))

/** "14:35" */
fun formatTime(epochMillis: Long): String =
    FMT_TIME.format(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(DEFAULT_ZONE).time)

/** "Jan 2024" */
fun formatMonthYear(epochMillis: Long): String =
    FMT_MONTH_YEAR.format(epochMillisToLocalDate(epochMillis))

/** "2024-01" — stable month key for groupBy and map keys */
fun monthKey(epochMillis: Long): String =
    FMT_MONTH_KEY.format(epochMillisToLocalDate(epochMillis))

// ─── Relative helpers ────────────────────────────────────────────────────────

/**
 * Human-readable relative time matching the RN timeAgo() helper.
 * "Just now", "5m ago", "2h ago", "Yesterday", "12 Jan"
 */
fun timeAgo(epochMillis: Long): String {
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val diffMs    = nowMillis - epochMillis
    val seconds = diffMs / 1_000L
    val minutes = seconds / 60L
    val hours   = minutes / 60L
    return when {
        seconds < 60  -> "Just now"
        minutes < 60  -> "${minutes}m ago"
        hours < 24    -> "${hours}h ago"
        hours < 48    -> "Yesterday"
        else          -> {
            val today  = Clock.System.todayIn(DEFAULT_ZONE)
            val target = epochMillisToLocalDate(epochMillis)
            formatDate(epochMillis, showYear = today.year != target.year)
        }
    }
}

// ─── Period boundaries ───────────────────────────────────────────────────────

/** Start of today (midnight) as epoch millis. */
fun startOfToday(zone: TimeZone = DEFAULT_ZONE): Long =
    Clock.System.todayIn(zone).atStartOfDayIn(zone).toEpochMilliseconds()

/** End of today (23:59:59.999) as epoch millis. */
fun endOfToday(zone: TimeZone = DEFAULT_ZONE): Long {
    val today = Clock.System.todayIn(zone)
    return LocalDateTime(today.year, today.month, today.dayOfMonth, 23, 59, 59, 999_000_000)
        .toInstant(zone).toEpochMilliseconds()
}

/** Start of the current month as epoch millis. */
fun startOfMonth(zone: TimeZone = DEFAULT_ZONE): Long {
    val today = Clock.System.todayIn(zone)
    return LocalDate(today.year, today.month, 1).atStartOfDayIn(zone).toEpochMilliseconds()
}

/** End of the current month as epoch millis. */
fun endOfMonth(zone: TimeZone = DEFAULT_ZONE): Long {
    val today   = Clock.System.todayIn(zone)
    val lastDay = lastDayOfMonth(today)
    return LocalDateTime(lastDay.year, lastDay.month, lastDay.dayOfMonth, 23, 59, 59, 999_000_000)
        .toInstant(zone).toEpochMilliseconds()
}

/** Start of current week (Monday) as epoch millis. */
fun startOfWeek(zone: TimeZone = DEFAULT_ZONE): Long {
    val today  = Clock.System.todayIn(zone)
    val monday = previousOrSameMonday(today)
    return monday.atStartOfDayIn(zone).toEpochMilliseconds()
}

/** "yyyy-MM" key for current month. */
fun currentMonthKey(zone: TimeZone = DEFAULT_ZONE): String =
    FMT_MONTH_KEY.format(Clock.System.todayIn(zone))

/** Previous month key "yyyy-MM". */
fun previousMonthKey(zone: TimeZone = DEFAULT_ZONE): String =
    FMT_MONTH_KEY.format(Clock.System.todayIn(zone).minus(1, DateTimeUnit.MONTH))

/** Return epoch millis for the first day of any "yyyy-MM" month key. */
fun monthKeyToStartMillis(key: String, zone: TimeZone = DEFAULT_ZONE): Long {
    val date = LocalDate.parse("$key-01")
    return date.atStartOfDayIn(zone).toEpochMilliseconds()
}

/** Return epoch millis for the last day of any "yyyy-MM" month key. */
fun monthKeyToEndMillis(key: String, zone: TimeZone = DEFAULT_ZONE): Long {
    val last = lastDayOfMonth(LocalDate.parse("$key-01"))
    return LocalDateTime(last.year, last.month, last.dayOfMonth, 23, 59, 59, 999_000_000)
        .toInstant(zone).toEpochMilliseconds()
}

// ─── Internal date helpers ───────────────────────────────────────────────────

/** Last day of the month that [date] falls in. */
internal fun lastDayOfMonth(date: LocalDate): LocalDate =
    LocalDate(date.year, date.month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

/** Most recent Monday on or before [date] (ISO: Monday = 1). */
internal fun previousOrSameMonday(date: LocalDate): LocalDate {
    val daysBack = (date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber + 7) % 7
    return date.minus(daysBack, DateTimeUnit.DAY)
}

/** "EEE, d MMM" format — shared by AssistantViewModel and task list displays. */
internal val FMT_EEE_D_MMM = LocalDate.Format {
    dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED); chars(", ")
    dayOfMonth(Padding.NONE); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED)
}

/** "d MMM" format — short date display (no year). */
internal val FMT_D_MMM = LocalDate.Format {
    dayOfMonth(Padding.NONE); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED)
}

/** "MMM d" format — used in week-review header and calendar labels. */
internal val FMT_MMM_D = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(Padding.NONE)
}

/** "MMM d, yyyy" format — used in week-review header for Sunday label. */
internal val FMT_MMM_D_YYYY = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' ')
    dayOfMonth(Padding.NONE); chars(", "); year()
}

/** "MMM d, yyyy" format — e.g. for search screen date display. */
internal val FMT_MMM_D_YYYY_FULL = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' ')
    dayOfMonth(Padding.NONE); chars(", "); year()
}
