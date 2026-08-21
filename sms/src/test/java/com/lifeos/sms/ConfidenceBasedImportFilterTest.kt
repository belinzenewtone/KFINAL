package com.lifeos.sms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ported from KOTLIN shared/androidUnitTest ConfidenceBasedImportFilterTest (Phase 2 backport).
 */
class ConfidenceBasedImportFilterTest {
    // ── evaluateImportStrategy ───────────────────────────────────────────────

    @Test fun `HIGH confidence maps to IMPORT_REALTIME`() {
        assertEquals(
            ConfidenceBasedImportFilter.ImportDecision.IMPORT_REALTIME,
            ConfidenceBasedImportFilter.evaluateImportStrategy(SmsParserConfig.Confidence.HIGH),
        )
    }

    @Test fun `MEDIUM confidence maps to DEFER_TO_BATCH`() {
        assertEquals(
            ConfidenceBasedImportFilter.ImportDecision.DEFER_TO_BATCH,
            ConfidenceBasedImportFilter.evaluateImportStrategy(SmsParserConfig.Confidence.MEDIUM),
        )
    }

    @Test fun `LOW confidence maps to QUARANTINE_FOR_REVIEW`() {
        assertEquals(
            ConfidenceBasedImportFilter.ImportDecision.QUARANTINE_FOR_REVIEW,
            ConfidenceBasedImportFilter.evaluateImportStrategy(SmsParserConfig.Confidence.LOW),
        )
    }

    // ── shouldShowInRealtimeFeed ─────────────────────────────────────────────

    @Test fun `realtime feed shows IMPORT_REALTIME`() {
        assertTrue(
            ConfidenceBasedImportFilter.shouldShowInRealtimeFeed(
                ConfidenceBasedImportFilter.ImportDecision.IMPORT_REALTIME)
        )
    }

    @Test fun `realtime feed hides DEFER_TO_BATCH`() {
        assertFalse(
            ConfidenceBasedImportFilter.shouldShowInRealtimeFeed(
                ConfidenceBasedImportFilter.ImportDecision.DEFER_TO_BATCH)
        )
    }

    @Test fun `realtime feed hides QUARANTINE_FOR_REVIEW`() {
        assertFalse(
            ConfidenceBasedImportFilter.shouldShowInRealtimeFeed(
                ConfidenceBasedImportFilter.ImportDecision.QUARANTINE_FOR_REVIEW)
        )
    }

    // ── shouldIncludeInBatchReconciliation ───────────────────────────────────

    @Test fun `batch includes DEFER_TO_BATCH`() {
        assertTrue(
            ConfidenceBasedImportFilter.shouldIncludeInBatchReconciliation(
                ConfidenceBasedImportFilter.ImportDecision.DEFER_TO_BATCH)
        )
    }

    @Test fun `batch excludes IMPORT_REALTIME`() {
        assertFalse(
            ConfidenceBasedImportFilter.shouldIncludeInBatchReconciliation(
                ConfidenceBasedImportFilter.ImportDecision.IMPORT_REALTIME)
        )
    }

    @Test fun `batch excludes QUARANTINE_FOR_REVIEW`() {
        assertFalse(
            ConfidenceBasedImportFilter.shouldIncludeInBatchReconciliation(
                ConfidenceBasedImportFilter.ImportDecision.QUARANTINE_FOR_REVIEW)
        )
    }

    // ── requiresManualReview ─────────────────────────────────────────────────

    @Test fun `manual review required for QUARANTINE_FOR_REVIEW`() {
        assertTrue(
            ConfidenceBasedImportFilter.requiresManualReview(
                ConfidenceBasedImportFilter.ImportDecision.QUARANTINE_FOR_REVIEW)
        )
    }

    @Test fun `no manual review for IMPORT_REALTIME`() {
        assertFalse(
            ConfidenceBasedImportFilter.requiresManualReview(
                ConfidenceBasedImportFilter.ImportDecision.IMPORT_REALTIME)
        )
    }

    @Test fun `no manual review for DEFER_TO_BATCH`() {
        assertFalse(
            ConfidenceBasedImportFilter.requiresManualReview(
                ConfidenceBasedImportFilter.ImportDecision.DEFER_TO_BATCH)
        )
    }

    // ── Round-trip consistency ───────────────────────────────────────────────

    @Test fun `decisions are mutually exclusive across all confidences`() {
        for (conf in SmsParserConfig.Confidence.entries) {
            val d = ConfidenceBasedImportFilter.evaluateImportStrategy(conf)
            val flags = listOf(
                ConfidenceBasedImportFilter.shouldShowInRealtimeFeed(d),
                ConfidenceBasedImportFilter.shouldIncludeInBatchReconciliation(d),
                ConfidenceBasedImportFilter.requiresManualReview(d),
            )
            assertEquals(1, flags.count { it },
                "Exactly one bucket must own decision $d (from $conf)")
        }
    }
}
