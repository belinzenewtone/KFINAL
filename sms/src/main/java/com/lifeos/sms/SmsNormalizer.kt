package com.lifeos.sms

/**
 * Pre-processing pass applied to every SMS body before any regex runs.
 *
 * Fixes two classes of real-world SMS artefacts that cause Stage 0–3 failures:
 *  1. Unicode zero-width characters injected by some Safaricom gateway paths.
 *  2. Word-concatenation artefacts where whitespace is missing between tokens
 *     (e.g. "Confirmed.on 15/01/25", "1,000.00PMWithdraw").
 *
 * Also strips Safaricom promotional tails so they do not interfere with
 * counterparty extraction or date detection (consolidated from SmsParser).
 *
 * All operations are pure string transforms — no I/O, no Regex allocation.
 */
object SmsNormalizer {
    // Zero-width characters sometimes embedded by Safaricom's SMS gateway.
    private val ZERO_WIDTH_RE = Regex("[​‌‍ ]")

    // Concatenation artefacts: missing space between a sentence-ending token and
    // the next word. Order matters — more specific patterns before general ones.
    private val CONCAT_FIXES = listOf(
        // "Confirmed.on" → "Confirmed. on"  (date immediately follows period)
        Regex("""([Cc]onfirmed)\.(on\b)""") to "$1. $2",
        // "1,000.00PMWithdraw" / "1,000.00AMSent" → add space before AM/PM+verb
        Regex("""(\d)((?:AM|PM)(?:Withdraw|Sent|Received|Paid|Bought|Deposit))""", RegexOption.IGNORE_CASE) to "$1 $2",
        // "PMWithdraw" / "AMReceived" at word boundary → "PM Withdraw"
        Regex("""\b(AM|PM)(Withdraw|Sent|Received|Paid|Bought|Deposit)""", RegexOption.IGNORE_CASE) to "$1 $2",
        // "rejected.Ksh" → "rejected. Ksh"  (amount immediately follows period)
        Regex("""\.(Ksh|KES)\b""") to ". $1",
        // "toJohn" / "fromMary" — camelCase junction where lowercase meets uppercase+lower
        Regex("""([a-z])([A-Z][a-z]{2,})""") to "$1 $2",
    )

    // Safaricom promotional tails appended to some SMS bodies. Stripped so they
    // don't interfere with counterparty extraction or DATE_RE matches.
    // Matches from the first occurrence of a promo trigger to end of string.
    private val PROMO_TAIL_RE = Regex(
        """(?:Dial\s+\*\d+#|Download the M-PESA app|Visit\s+\w+\.|Get M-PESA|use M-PESA|Free\s+M-PESA|\*234#|\*334#|\*100#|www\.\S+).*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /**
     * Normalize [body] for parsing. Returns a clean string with:
     *  - Zero-width characters removed
     *  - Concatenation artefacts repaired
     *  - Promotional tails stripped
     *  - Runs of whitespace collapsed to a single space
     */
    fun normalize(body: String): String {
        var s = ZERO_WIDTH_RE.replace(body, "")
        for ((pattern, replacement) in CONCAT_FIXES) {
            s = pattern.replace(s, replacement)
        }
        s = PROMO_TAIL_RE.replace(s, "").trim()
        // Collapse any whitespace runs introduced by the replacements above.
        return SmsParserConfig.WS_RE.replace(s, " ").trim()
    }
}
