package com.belinze.lifeos.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// FormatUtils
//
// 1:1 port of src/utils/format.ts — currency and number formatting.
//
// All monetary values in the app are KES (Kenyan Shilling).
// RFINAL formats with Intl.NumberFormat('en-KE', {style:'currency', currency:'KES',
// currencyDisplay:'symbol'}), whose CLDR symbol for KES is "Ksh" — so the reference
// app renders "Ksh 1,234.50". This helper must emit the same prefix or every
// currency value in the app diverges from the reference.
// Negative amounts show: "-Ksh 1,234.50"
// ─────────────────────────────────────────────────────────────────────────────

private val KES_FORMAT: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed        = true
    }
}

private val KES_FORMAT_NO_DECIMALS: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
        isGroupingUsed        = true
    }
}

/**
 * Format a monetary amount in KES.
 *
 * @param amount        The amount to format (may be negative).
 * @param compact       When true, omits fractional cents for large whole numbers.
 * @param showCurrency  When true (default), prepends "KES ".
 * @return "KES 1,234.50" / "-KES 1,234.50" / "1,234.50"
 */
fun formatCurrency(
    amount:       Double,
    showCurrency: Boolean = true,
    compact:      Boolean = false,
    decimals:     Int     = -1,   // -1 = default (2 decimals); 0 = whole number
): String {
    val absAmount = Math.abs(amount)
    val sign      = if (amount < 0) "-" else ""

    val formatted = if (compact && absAmount >= 1_000_000.0) {
        // Compact: "1.2M"
        val millions = absAmount / 1_000_000.0
        "${String.format(Locale.US, "%.1f", millions)}M"
    } else if (compact && absAmount >= 1_000.0 && absAmount % 1.0 == 0.0) {
        KES_FORMAT_NO_DECIMALS.format(absAmount.toLong())
    } else if (decimals == 0) {
        KES_FORMAT_NO_DECIMALS.format(absAmount.toLong())
    } else {
        KES_FORMAT.format(absAmount)
    }

    return if (showCurrency) "${sign}Ksh $formatted" else "$sign$formatted"
}

/** Overload accepting Long (integer amounts stored as cents × 100 / 100). */
fun formatCurrency(
    amount:       Long,
    showCurrency: Boolean = true,
    compact:      Boolean = false,
    decimals:     Int     = -1,
): String = formatCurrency(amount.toDouble(), showCurrency, compact, decimals)

/** Overload accepting BigDecimal for precision-sensitive calcs. */
fun formatCurrency(
    amount:       BigDecimal,
    showCurrency: Boolean = true,
    compact:      Boolean = false,
    decimals:     Int     = -1,
): String = formatCurrency(amount.toDouble(), showCurrency, compact, decimals)

/**
 * Format a plain number with comma thousands separator.
 * Used for counts, percentages, etc.
 */
fun formatNumber(amount: Double, decimals: Int = 0): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
        isGroupingUsed        = true
    }
    return fmt.format(amount)
}

/**
 * Compact KES shorthand: "KES 1.2M", "KES 34K", "KES 999".
 * Matches the RN compactCurrency() helper.
 */
fun compactCurrency(amount: Double): String {
    val abs = Math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        abs >= 1_000_000 -> "${sign}Ksh ${String.format(Locale.US, "%.1f", abs / 1_000_000)}M"
        abs >= 1_000     -> "${sign}Ksh ${String.format(Locale.US, "%.1f", abs / 1_000)}K"
        else             -> formatCurrency(amount)
    }
}

/**
 * Return a sign-aware string: "+KES 1,234" for positive deltas.
 * Used in budget/goal progress lines.
 */
fun formatDelta(amount: Double): String {
    val prefix = if (amount >= 0) "+" else ""
    return "$prefix${formatCurrency(amount)}"
}

/**
 * Event location is stored as a JSON array of strings (multi-location support);
 * falls back to a legacy plain-string location. Renders as a comma-joined list.
 */
fun formatLocation(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).map { arr.getString(it) }.joinToString(", ").ifBlank { null }
    } catch (_: Exception) {
        raw
    }
}

/** Compact variant for single-line list rows: shows only the first location. */
fun formatLocationFirst(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val arr = org.json.JSONArray(raw)
        if (arr.length() > 0) arr.getString(0) else null
    } catch (_: Exception) {
        raw
    }
}
