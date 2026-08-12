package com.belinze.lifeos.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// DateUtils
//
// 1:1 port of src/utils/date.ts — all date helpers used in the app.
//
// The app stores timestamps as ISO-8601 strings or epoch-millis longs.
// TimeZone: device local (Nairobi = Africa/Nairobi, EAT UTC+3 by default).
// ─────────────────────────────────────────────────────────────────────────────

private val DEFAULT_ZONE: ZoneId = ZoneId.systemDefault()

// ─── Formatters ─────────────────────────────────────────────────────────────

/** "12 Jan 2024" */
private val FMT_DISPLAY_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** "12 Jan" (no year, used in lists) */
private val FMT_SHORT_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/** "12 Jan 2024, 14:35" */
private val FMT_DISPLAY_DATETIME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

/** "14:35" */
private val FMT_TIME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** "Jan 2024" — for month headers in transaction lists */
private val FMT_MONTH_YEAR: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

/** "2024-01" — used for groupBy month keys */
private val FMT_MONTH_KEY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)

/** ISO 8601 full — "2024-01-12T14:35:00+03:00" */
private val FMT_ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

// ─── Epoch ↔ LocalDate / ZonedDateTime ──────────────────────────────────────

fun epochMillisToLocalDate(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

fun epochMillisToZdt(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): ZonedDateTime =
    Instant.ofEpochMilli(epochMillis).atZone(zone)

fun localDateToEpochMillis(date: LocalDate, zone: ZoneId = DEFAULT_ZONE): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

// ─── ISO string ↔ ZonedDateTime ─────────────────────────────────────────────

fun isoToZdt(iso: String): ZonedDateTime =
    ZonedDateTime.parse(iso, FMT_ISO)

fun zdtToIso(zdt: ZonedDateTime): String = zdt.format(FMT_ISO)

fun nowIso(zone: ZoneId = DEFAULT_ZONE): String =
    ZonedDateTime.now(zone).format(FMT_ISO)

// ─── Display formatters ──────────────────────────────────────────────────────

/**
 * "12 Jan 2024" — from epoch millis.
 * If [showYear] is false returns "12 Jan".
 */
fun formatDate(epochMillis: Long, showYear: Boolean = true): String {
    val ldt = epochMillisToLocalDate(epochMillis)
    return ldt.format(if (showYear) FMT_DISPLAY_DATE else FMT_SHORT_DATE)
}

/** "12 Jan 2024, 14:35" */
fun formatDateTime(epochMillis: Long): String =
    epochMillisToZdt(epochMillis).format(FMT_DISPLAY_DATETIME)

/** "14:35" */
fun formatTime(epochMillis: Long): String =
    epochMillisToZdt(epochMillis).format(FMT_TIME)

/** "Jan 2024" */
fun formatMonthYear(epochMillis: Long): String =
    epochMillisToLocalDate(epochMillis).format(FMT_MONTH_YEAR)

/** "2024-01" — stable month key for groupBy and map keys */
fun monthKey(epochMillis: Long): String =
    epochMillisToLocalDate(epochMillis).format(FMT_MONTH_KEY)

// ─── Relative helpers ────────────────────────────────────────────────────────

/**
 * Human-readable relative time matching the RN timeAgo() helper.
 * "Just now", "5m ago", "2h ago", "Yesterday", "12 Jan"
 */
fun timeAgo(epochMillis: Long): String {
    val nowMillis = System.currentTimeMillis()
    val diffMs    = nowMillis - epochMillis

    val seconds = diffMs / 1_000L
    val minutes = seconds / 60L
    val hours   = minutes / 60L

    return when {
        seconds < 60  -> "Just now"
        minutes < 60  -> "${minutes}m ago"
        hours < 24    -> "${hours}h ago"
        hours < 48    -> "Yesterday"
        else          -> formatDate(epochMillis, showYear = LocalDate.now().year != epochMillisToLocalDate(epochMillis).year)
    }
}

// ─── Period boundaries ───────────────────────────────────────────────────────

/** Start of today (midnight) as epoch millis. */
fun startOfToday(zone: ZoneId = DEFAULT_ZONE): Long =
    LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

/** End of today (23:59:59.999) as epoch millis. */
fun endOfToday(zone: ZoneId = DEFAULT_ZONE): Long =
    LocalDate.now(zone).atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

/** Start of the current month as epoch millis. */
fun startOfMonth(zone: ZoneId = DEFAULT_ZONE): Long {
    val now = LocalDate.now(zone)
    return now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
}

/** End of the current month as epoch millis. */
fun endOfMonth(zone: ZoneId = DEFAULT_ZONE): Long {
    val now = LocalDate.now(zone)
    return now.with(TemporalAdjusters.lastDayOfMonth())
        .atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
}

/** Start of current week (Monday) as epoch millis. */
fun startOfWeek(zone: ZoneId = DEFAULT_ZONE): Long {
    val now = LocalDate.now(zone)
    return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone).toInstant().toEpochMilli()
}

/** "yyyy-MM" key for current month. */
fun currentMonthKey(zone: ZoneId = DEFAULT_ZONE): String =
    LocalDate.now(zone).format(FMT_MONTH_KEY)

/** Previous month key "yyyy-MM". */
fun previousMonthKey(zone: ZoneId = DEFAULT_ZONE): String =
    LocalDate.now(zone).minusMonths(1).format(FMT_MONTH_KEY)

/** Return epoch millis for the first day of any "yyyy-MM" month key. */
fun monthKeyToStartMillis(key: String, zone: ZoneId = DEFAULT_ZONE): Long {
    val date = LocalDate.parse("$key-01")
    return date.atStartOfDay(zone).toInstant().toEpochMilli()
}

/** Return epoch millis for the last day of any "yyyy-MM" month key. */
fun monthKeyToEndMillis(key: String, zone: ZoneId = DEFAULT_ZONE): Long {
    val date = LocalDate.parse("$key-01").with(TemporalAdjusters.lastDayOfMonth())
    return date.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
}
