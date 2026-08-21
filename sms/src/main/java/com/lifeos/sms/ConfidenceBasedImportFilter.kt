package com.lifeos.sms

/**
 * Confidence-based filtering for M-Pesa import decisions.
 *
 * Strategy:
 * - HIGH confidence: import immediately to realtime feed
 * - MEDIUM confidence: defer to batch reconciliation (manual review)
 * - LOW confidence: quarantine for later inspection
 *
 * This keeps the realtime feed accurate without false-positive clutter.
 */
object ConfidenceBasedImportFilter {
    /**
     * Determine whether to import a transaction in realtime or defer to batch.
     */
    fun evaluateImportStrategy(confidence: SmsParserConfig.Confidence): ImportDecision =
        when (confidence) {
            SmsParserConfig.Confidence.HIGH   -> ImportDecision.IMPORT_REALTIME
            SmsParserConfig.Confidence.MEDIUM -> ImportDecision.DEFER_TO_BATCH
            SmsParserConfig.Confidence.LOW    -> ImportDecision.QUARANTINE_FOR_REVIEW
        }

    /** Decision about when/how to import a transaction. */
    enum class ImportDecision {
        /** Import immediately to the realtime transaction feed. */
        IMPORT_REALTIME,

        /** Defer to batch reconciliation; hidden from the realtime feed until verified. */
        DEFER_TO_BATCH,

        /** Quarantine for manual review; hidden until explicitly approved. */
        QUARANTINE_FOR_REVIEW,
    }

    fun shouldShowInRealtimeFeed(decision: ImportDecision): Boolean =
        decision == ImportDecision.IMPORT_REALTIME

    fun shouldIncludeInBatchReconciliation(decision: ImportDecision): Boolean =
        decision == ImportDecision.DEFER_TO_BATCH

    fun requiresManualReview(decision: ImportDecision): Boolean =
        decision == ImportDecision.QUARANTINE_FOR_REVIEW
}
