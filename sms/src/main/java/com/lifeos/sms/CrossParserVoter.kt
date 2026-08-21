package com.lifeos.sms

/**
 * Cross-parser voting layer for M-Pesa transactions.
 *
 * After [SmsParser] produces a DIRECT result, [SimpleMpesaParser] independently
 * re-parses the same body. If the two parsers disagree on category or amount by
 * more than [AMOUNT_THRESHOLD], the result is demoted to REVIEW so a human can
 * confirm before the transaction is auto-inserted into the ledger.
 *
 * The fallback result is never written — it is used purely as a disagreement signal.
 */
internal object CrossParserVoter {
    private const val AMOUNT_THRESHOLD = 0.05  // 5% relative tolerance

    /**
     * Apply the cross-vote to [tx]. Returns [tx] unchanged if already at REVIEW
     * or QUARANTINE, or if [SimpleMpesaParser] cannot parse the body. Otherwise
     * demotes DIRECT → REVIEW on category or amount disagreement.
     */
    fun applyVote(tx: SmsParser.ParsedTransaction, rawSms: String): SmsParser.ParsedTransaction {
        if (tx.parseRoute != SmsParser.ParseRoute.DIRECT) return tx

        val fallback = SimpleMpesaParser.parse(rawSms) ?: return tx

        val categoryAgree = tx.category == fallback.category
        val amountAgree = if (fallback.amount > 0 && tx.amount > 0) {
            Math.abs(tx.amount - fallback.amount) / tx.amount <= AMOUNT_THRESHOLD
        } else {
            true
        }

        return if (categoryAgree && amountAgree) {
            tx
        } else {
            tx.copy(parseRoute = SmsParser.ParseRoute.REVIEW)
        }
    }
}
