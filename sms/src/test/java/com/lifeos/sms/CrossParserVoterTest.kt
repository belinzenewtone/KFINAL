package com.lifeos.sms

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from KOTLIN shared/androidUnitTest CrossParserVoterTest (Phase 2 backport).
 *
 * Covers the voting outcomes:
 *  1. Agreement on category + amount → DIRECT kept
 *  2. Category disagreement          → DIRECT demoted to REVIEW
 *  3. Amount disagreement (> 5%)     → DIRECT demoted to REVIEW
 *  4. Already REVIEW                 → not demoted further
 *  5. SimpleMpesaParser returns null → original result unchanged
 */
class CrossParserVoterTest {
    private val RECEIVED_AT_MS = 1_750_000_000_000L

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun tx(
        category: SmsParserConfig.SmsCategory,
        amount: Double,
        route: SmsParser.ParseRoute = SmsParser.ParseRoute.DIRECT,
    ) = SmsParser.ParsedTransaction(
        mpesaCode    = "SIE8QWE123",
        amount       = amount,
        category     = category,
        confidence   = SmsParserConfig.Confidence.HIGH,
        counterparty = "JOHN DOE",
        description  = "Test",
        balanceAfter = null,
        fee          = null,
        date         = RECEIVED_AT_MS,
        rawSms       = "",
        parseRoute   = route,
    )

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `category and amount agree — DIRECT kept`() {
        val sms = "SIE8QWE123 Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 26/7/26 at 10:00 AM. New M-PESA balance is Ksh2,500.00."
        val transaction = tx(SmsParserConfig.SmsCategory.SENT, 500.0)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.DIRECT, result.parseRoute,
            "Route should stay DIRECT when both parsers agree")
    }

    @Test
    fun `category disagreement — DIRECT demoted to REVIEW`() {
        // SimpleMpesaParser classifies as RECEIVED ("you have received")
        // but we pass a transaction with category=SENT → disagreement
        val sms = "SIE8QWE123 Confirmed. You have received Ksh500.00 from MARY JANE 0712345678 on 26/7/26. New M-PESA balance is Ksh800.00."
        val transaction = tx(SmsParserConfig.SmsCategory.SENT, 500.0)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.REVIEW, result.parseRoute,
            "Route should be demoted when category disagrees")
    }

    @Test
    fun `amount disagrees by more than 5 percent — DIRECT demoted`() {
        val sms = "SIE8QWE123 Confirmed. Ksh1000.00 sent to JOHN DOE on 26/7/26. New M-PESA balance is Ksh3,000.00."
        val transaction = tx(SmsParserConfig.SmsCategory.SENT, 2000.0)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.REVIEW, result.parseRoute,
            "Route should be demoted when amounts differ > 5%")
    }

    @Test
    fun `amount agrees within 5 percent threshold — DIRECT kept`() {
        val sms = "SIE8QWE123 Confirmed. Ksh500.00 sent to JOHN DOE on 26/7/26. New M-PESA balance is Ksh2,500.00."
        val transaction = tx(SmsParserConfig.SmsCategory.SENT, 501.0)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.DIRECT, result.parseRoute,
            "Sub-5% amount difference should not demote")
    }

    @Test
    fun `already REVIEW — not demoted further`() {
        val sms = "SIE8QWE123 Confirmed. You have received Ksh500.00 from JOHN DOE on 26/7/26."
        val transaction = tx(SmsParserConfig.SmsCategory.SENT, 500.0,
            route = SmsParser.ParseRoute.REVIEW)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.REVIEW, result.parseRoute,
            "REVIEW should not be changed by the voter")
    }

    @Test
    fun `already QUARANTINE — not touched`() {
        val sms = "Random SMS without M-Pesa signal"
        val transaction = tx(SmsParserConfig.SmsCategory.UNKNOWN, 100.0,
            route = SmsParser.ParseRoute.QUARANTINE)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.QUARANTINE, result.parseRoute,
            "QUARANTINE should not be changed by the voter")
    }

    @Test
    fun `SimpleMpesaParser returns null — original result unchanged`() {
        val sms = "Your bank account 00123 has been credited with KES 500."
        val transaction = tx(SmsParserConfig.SmsCategory.RECEIVED, 500.0)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.DIRECT, result.parseRoute,
            "Null fallback result should not demote")
        assertEquals(transaction.amount, result.amount,
            "Transaction should be identical when fallback is null")
    }

    @Test
    fun `Fuliza repayment — categories agree — DIRECT kept`() {
        val sms = "SIE8QWE123 Confirmed. Ksh250.00 from your M-PESA has been used to fully pay your outstanding Fuliza M-PESA. New M-PESA balance is Ksh3,000.00."
        val transaction = tx(SmsParserConfig.SmsCategory.LOAN, 250.0)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.DIRECT, result.parseRoute,
            "Fuliza repayment with agreeing parsers should stay DIRECT")
    }

    @Test
    fun `airtime — categories agree — DIRECT kept`() {
        val sms = "SIE8QWE123 Confirmed. You bought Ksh50.00 of airtime on 26/7/26 at 9:00 AM. New M-PESA balance is Ksh1,450.00."
        val transaction = tx(SmsParserConfig.SmsCategory.AIRTIME, 50.0)

        val result = CrossParserVoter.applyVote(transaction, sms)

        assertEquals(SmsParser.ParseRoute.DIRECT, result.parseRoute,
            "Airtime with agreeing parsers should stay DIRECT")
    }
}
