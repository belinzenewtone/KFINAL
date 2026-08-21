package com.lifeos.sms

/**
 * Hand-calibrated depth-4 decision tree for post-parse result validation.
 *
 * Evaluates raw SMS features (amount, M-Pesa keyword, transaction code, parties,
 * fee, date, Fuliza signal) and returns a structural confidence verdict. When the
 * parser claims HIGH confidence but the tree returns LOW, the result is demoted to
 * REVIEW before DB write. The symmetric case (tree=HIGH, parser=LOW) never promotes —
 * the parser's evidence wins.
 *
 * This is a defence against cases where a regex rule fires on an unusual SMS body
 * that looks like an M-Pesa transaction but lacks the structural signals that real
 * M-Pesa SMS always contain.
 */
internal object TransactionDecisionTree {
    private data class Features(
        val hasAmount: Boolean,
        val hasMpesaKeyword: Boolean,
        val hasTransactionCode: Boolean,
        val hasBothParties: Boolean,
        val hasFee: Boolean,
        val hasDate: Boolean,
        val hasFulizaSignal: Boolean,
    )

    private enum class Verdict { HIGH, MEDIUM, LOW }

    private val TO_RE   = Regex("""\bto\s+[A-Za-z]""",   RegexOption.IGNORE_CASE)
    private val FROM_RE = Regex("""\bfrom\s+[A-Za-z]""", RegexOption.IGNORE_CASE)

    private fun extract(body: String): Features {
        val lower = body.lowercase()
        return Features(
            hasAmount          = SmsParserConfig.AMOUNT_RE.containsMatchIn(body),
            hasMpesaKeyword    = lower.contains("mpesa") || lower.contains("m-pesa"),
            hasTransactionCode = SmsParserConfig.CODE_RE.containsMatchIn(body) ||
                                 SmsParserConfig.CODE_START_RE.containsMatchIn(body),
            hasBothParties     = TO_RE.containsMatchIn(body) || FROM_RE.containsMatchIn(body),
            hasFee             = SmsParserConfig.FEE_RE.containsMatchIn(body),
            hasDate            = SmsParserConfig.DATE_RE.containsMatchIn(body),
            hasFulizaSignal    = lower.contains("fuliza"),
        )
    }

    private fun evaluate(f: Features): Verdict = when {
        !f.hasAmount -> Verdict.LOW
        f.hasMpesaKeyword -> when {
            f.hasTransactionCode -> when {
                f.hasBothParties || f.hasFee || f.hasDate -> Verdict.HIGH
                else -> Verdict.MEDIUM
            }
            f.hasDate -> Verdict.MEDIUM
            else      -> Verdict.LOW
        }
        f.hasFulizaSignal -> when {
            f.hasTransactionCode -> Verdict.MEDIUM
            else                 -> Verdict.LOW
        }
        else -> Verdict.LOW
    }

    /**
     * Returns true when [tx] should be demoted from DIRECT to REVIEW.
     *
     * Only fires on HIGH-confidence parser results that the decision tree rates LOW —
     * an unusual body that matched a rule but lacks the structural signals of real
     * M-Pesa SMS.
     */
    fun shouldDemote(body: String, tx: SmsParser.ParsedTransaction): Boolean {
        if (tx.confidence != SmsParserConfig.Confidence.HIGH) return false
        return evaluate(extract(body)) == Verdict.LOW
    }
}
