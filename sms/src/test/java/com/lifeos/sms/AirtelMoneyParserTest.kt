package com.lifeos.sms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Ported from KOTLIN shared/androidUnitTest AirtelMoneyParserTest (Phase 2 backport),
 * adapted to KFINAL's pipeline API (SmsParser.SmsParseResult instead of sealed outcomes).
 */
class AirtelMoneyParserTest {
    private val NOW = 1_753_100_000_000L

    // ═══════════════════════════════════════════════════════════════════════
    // canParse
    // ═══════════════════════════════════════════════════════════════════════

    @Test fun `canParse true for AIRTEL sender`() {
        assertTrue(AirtelMoneyParser.canParse("Airtel Money: You sent KES 100", "AIRTEL"))
    }

    @Test fun `canParse true for body starting with Airtel Money`() {
        assertTrue(AirtelMoneyParser.canParse("Airtel Money: You have sent KES 200", "12345"))
    }

    @Test fun `canParse false for unrelated sender and body`() {
        assertFalse(AirtelMoneyParser.canParse("Your OTP is 123456", "12345"))
    }

    @Test fun `canParse false for MPESA sender`() {
        assertFalse(AirtelMoneyParser.canParse("RC1234 Confirmed Ksh500", "MPESA"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Parsing via ParserPipeline routing
    // ═══════════════════════════════════════════════════════════════════════

    @Test fun `AIRTEL subscription SMS without amount is rejected not parsed as M-Pesa`() {
        val body = "Dear customer you have successfully subscribed to the UnlimitedFun 3hrs plan expiring on 18-07-26 15:10."
        val result = ParserPipeline.process(body, "AIRTEL", NOW)
        assertIs<SmsParser.SmsParseResult.Error>(result)
        assertEquals("no_amount", result.error.reason)
    }

    @Test fun `AIRTEL send SMS parses with amount and counterparty`() {
        val body = "Airtel Money: You sent KES 1,500.00 to JOHN DOE (0712345678) on 15/12/2024 at 2:30 PM. Your new balance is KES 8,500.00. Transaction ID: AIR123456789. Charges: KES 15.00."
        val result = ParserPipeline.process(body, "AIRTEL", NOW)
        val tx = assertIs<SmsParser.SmsParseResult.Success>(result).transaction
        assertEquals(SmsParserConfig.SmsCategory.SENT, tx.category)
        assertEquals(1500.0, tx.amount, 0.001)
        assertEquals("JOHN DOE", tx.counterparty)
        assertEquals(8500.0, tx.balanceAfter!!, 0.001)
        assertEquals(15.0, tx.fee!!, 0.001)
        assertEquals("airtel", tx.institutionId)
    }

    @Test fun `AIRTEL receive SMS parses as RECEIVED with real date`() {
        val body = "Airtel Money: You received KES 1,500.00 from JANE SMITH (0723456789) on 15/12/2024 at 2:30 PM. Your new balance is KES 3,200.00. Transaction ID: AIR987654321."
        val result = ParserPipeline.process(body, "AIRTELMONEY", NOW)
        val tx = assertIs<SmsParser.SmsParseResult.Success>(result).transaction
        assertEquals(SmsParserConfig.SmsCategory.RECEIVED, tx.category)
        assertEquals("JANE SMITH", tx.counterparty)
        // Real transaction date must be extracted from the body — not stamped with receivedAtMs.
        val expected = java.time.LocalDateTime.of(2024, 12, 15, 14, 30)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, tx.date)
    }
}
