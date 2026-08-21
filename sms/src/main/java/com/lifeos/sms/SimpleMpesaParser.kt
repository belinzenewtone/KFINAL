package com.lifeos.sms

/**
 * Lightweight, fully-independent secondary M-Pesa parser used by [CrossParserVoter].
 *
 * Intentionally shares no code with [SmsParser] — no DetectionRule objects, no
 * multi-stage pipeline, no date parsing. Uses first-Ksh-match for the amount (a
 * deliberate divergence from the primary parser's verb-proximity strategy) so that
 * genuine disagreements between the two produce a REVIEW demotion rather than being
 * silently suppressed by shared logic.
 */
internal object SimpleMpesaParser {
    private val AMOUNT_RE = Regex("""(?:Ksh|KES|KSH)\s?([\d,]+(?:\.\d{1,2})?)""")

    // Ordered: more-specific phrases before general ones to avoid wrong classification.
    private val CATEGORY_KEYWORDS = listOf(
        "has been reversed"         to SmsParserConfig.SmsCategory.REVERSED,
        "reversed"                  to SmsParserConfig.SmsCategory.REVERSED,
        "you have received"         to SmsParserConfig.SmsCategory.RECEIVED,
        "received from"             to SmsParserConfig.SmsCategory.RECEIVED,
        "cash deposit"              to SmsParserConfig.SmsCategory.DEPOSIT,
        "deposited"                 to SmsParserConfig.SmsCategory.DEPOSIT,
        "buy airtime"               to SmsParserConfig.SmsCategory.AIRTIME,
        "for airtime"               to SmsParserConfig.SmsCategory.AIRTIME,
        "airtime purchase"          to SmsParserConfig.SmsCategory.AIRTIME,
        "fuliza m-pesa access fee"  to SmsParserConfig.SmsCategory.FULIZA_CHARGE,
        "fuliza m-pesa loan"        to SmsParserConfig.SmsCategory.LOAN,
        "borrowed from fuliza"      to SmsParserConfig.SmsCategory.LOAN,
        // "buy goods" / "merchant payment" before "paid to" — buy-goods SMS always
        // contain "paid to" but the primary parser distinguishes them by account-ref
        // presence. Without that, "paid to" alone → BUY_GOODS (the more common case).
        "buy goods"                 to SmsParserConfig.SmsCategory.BUY_GOODS,
        "merchant payment"          to SmsParserConfig.SmsCategory.BUY_GOODS,
        "paybill"                   to SmsParserConfig.SmsCategory.PAYBILL,
        "paid to"                   to SmsParserConfig.SmsCategory.BUY_GOODS,
        "withdraw"                  to SmsParserConfig.SmsCategory.WITHDRAW,
        "you have sent"             to SmsParserConfig.SmsCategory.SENT,
        "sent to"                   to SmsParserConfig.SmsCategory.SENT,
    )

    data class Result(val category: SmsParserConfig.SmsCategory, val amount: Double)

    /** Returns null when no M-Pesa signal is present or amount/category cannot be determined. */
    fun parse(body: String): Result? {
        val lower = body.lowercase()
        if (!lower.contains("mpesa") && !lower.contains("m-pesa")) return null
        val amount = AMOUNT_RE.find(body)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?.takeIf { it > 0 } ?: return null
        val category = CATEGORY_KEYWORDS.firstOrNull { lower.contains(it.first) }?.second
            ?: return null
        return Result(category, amount)
    }
}
