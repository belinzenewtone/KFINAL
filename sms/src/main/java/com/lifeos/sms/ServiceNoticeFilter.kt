package com.lifeos.sms

/**
 * Pre-parse filters that reject SMS messages which look like transactions but aren't.
 *
 * These run as Stage 0 in [SmsParser] before any regex/rule detection, so they never
 * produce a phantom transaction in the ledger.
 *
 * Port of KOTLIN's parser/ServiceNoticeFilter.kt with additional M-Pesa patterns sourced
 * from field reports. Each pattern is kept narrow to avoid false positives.
 */
internal object ServiceNoticeFilter {
    /**
     * Returns true when the body is a *failed* transaction notice — one that contains the
     * word "Failed" or a standard failure phrase.  These often have a transaction code and
     * an amount, so the main parser assigns them a synthetic code and imports them as phantom
     * debit/credit rows.  Reject them here instead.
     *
     * Patterns (from KOTLIN ServiceNoticeFilter.kt:14-21 + field additions):
     *   "Failed."                     — Safaricom standard prefix
     *   "wrong pin" / "wrong PIN"     — auth failure
     *   "has not joined M-PESA"       — unregistered recipient
     *   "insufficient funds" + "failed" — explicit dual-signal
     *   "transaction could not be completed"
     *   "could not be processed"
     *   "unable to complete"
     *   "service temporarily unavailable" (when preceded by a code — still a failure)
     */
    private val FAILED_TX_RE = Regex(
        """(?i)(?:^Failed\b|""" +
        """you have entered the wrong\s+(?:pin|m-?pesa\s+pin)|""" +
        """the number you are trying to pay has not (?:joined|registered)|""" +
        """(?:insufficient funds?|account balance is insufficient)""" +
            """\b.{0,80}(?:failed|could not be|was not completed)|""" +
        """transaction could not be completed|""" +
        """could not be processed|""" +
        """unable to complete (?:the\s+)?(?:transaction|payment|transfer)|""" +
        """your (?:transaction|payment|transfer) (?:has )?failed)""",
    )

    /**
     * Returns true when the body is definitively a failed transaction that should not be
     * imported into the ledger.
     */
    fun isFailedTransaction(body: String): Boolean = FAILED_TX_RE.containsMatchIn(body)
}
