package com.lifeos.sms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ported from KOTLIN shared/androidUnitTest InstitutionDetectorTest (Phase 2 backport).
 */
class InstitutionDetectorTest {
    // ═══════════════════════════════════════════════════════════════════════
    // Tier 1: exact sender-ID match
    // ═══════════════════════════════════════════════════════════════════════

    @Test fun `detect MPESA sender`() {
        val d = InstitutionDetector.detect("MPESA", "some body")
        assertNotNull(d)
        assertEquals("mpesa", d.institutionId)
        assertEquals(1, d.tier)
    }

    @Test fun `detect NCBALOOP sender`() {
        val d = InstitutionDetector.detect("NCBALOOP", "some body")
        assertNotNull(d)
        assertEquals("ncba", d.institutionId)
        assertEquals(1, d.tier)
    }

    @Test fun `detect StanChart sender`() {
        val d = InstitutionDetector.detect("StanChart", "transaction details")
        assertNotNull(d)
        assertEquals("stanchart", d.institutionId)
        assertEquals(1, d.tier)
    }

    @Test fun `detect EquityBank sender`() {
        val d = InstitutionDetector.detect("EquityBank", "payment confirmed")
        assertNotNull(d)
        assertEquals("equity", d.institutionId)
        assertEquals(1, d.tier)
    }

    @Test fun `detect KCB sender`() {
        val d = InstitutionDetector.detect("KCB", "sent to M-PESA")
        assertNotNull(d)
        assertEquals("kcb", d.institutionId)
        assertEquals(1, d.tier)
    }

    @Test fun `detect NCBA_BANK sender`() {
        val d = InstitutionDetector.detect("NCBA_BANK", "service restored")
        assertNotNull(d)
        assertEquals("ncba", d.institutionId)
        assertEquals(1, d.tier)
    }

    @Test fun `detect AIRTEL sender`() {
        val d = InstitutionDetector.detect("AIRTEL", "subscribed")
        assertNotNull(d)
        assertEquals("airtel", d.institutionId)
        assertEquals(1, d.tier)
    }

    @Test fun `detect sender case-insensitive`() {
        val d = InstitutionDetector.detect("mpesa", "sent Ksh200")
        assertNotNull(d)
        assertEquals("mpesa", d.institutionId)
    }

    @Test fun `detect sender with whitespace trimmed`() {
        val d = InstitutionDetector.detect("  MPESA  ", "sent Ksh200")
        assertNotNull(d)
        assertEquals("mpesa", d.institutionId)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Unknown sender returns null
    // ═══════════════════════════════════════════════════════════════════════

    @Test fun `detect returns null for unknown sender and non-financial body`() {
        assertNull(InstitutionDetector.detect("12345", "Hello how are you"))
    }

    @Test fun `detect returns null for JTL sender`() {
        val d = InstitutionDetector.detect("JTL", "Dear Belinze, your last call was 0 hr 5 mins")
        assertNull(d)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // isFinancialSms
    // ═══════════════════════════════════════════════════════════════════════

    @Test fun `isFinancialSms true for known sender`() {
        assertTrue(InstitutionDetector.isFinancialSms("MPESA", "anything"))
    }

    @Test fun `isFinancialSms true for bank sender`() {
        assertTrue(InstitutionDetector.isFinancialSms("EquityBank", "payment"))
    }

    @Test fun `isFinancialSms false for unknown non-financial SMS`() {
        assertFalse(InstitutionDetector.isFinancialSms("JTL", "your last call was 5 mins"))
    }

    @Test fun `isFinancialSms false for promo SMS from Airtel_Home`() {
        assertFalse(InstitutionDetector.isFinancialSms(
            "Airtel_Home",
            "Your 5G Unlimited 15Mbps plan of KSh 1,999 will expire",
        ))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tier 1 banks coverage
    // ═══════════════════════════════════════════════════════════════════════

    @Test fun `detect CoopBank sender`() {
        val d = InstitutionDetector.detect("Co-opBank", "body")
        assertNotNull(d)
        assertEquals("coopbank", d.institutionId)
    }

    @Test fun `detect ABSA sender`() {
        val d = InstitutionDetector.detect("ABSA", "body")
        assertNotNull(d)
        assertEquals("absa", d.institutionId)
    }

    @Test fun `detect DTB sender`() {
        val d = InstitutionDetector.detect("DTB", "body")
        assertNotNull(d)
        assertEquals("dtb", d.institutionId)
    }

    @Test fun `detect FamilyBank sender`() {
        val d = InstitutionDetector.detect("FamilyBank", "body")
        assertNotNull(d)
        assertEquals("family", d.institutionId)
    }

    @Test fun `detect IMBANK sender maps to im`() {
        val d = InstitutionDetector.detect("IMBANK", "body")
        assertNotNull(d)
        assertEquals("im", d.institutionId)
    }

    @Test fun `detect IMBANK-NEWS sender maps to im`() {
        val d = InstitutionDetector.detect("IMBANK-NEWS", "body")
        assertNotNull(d)
        assertEquals("im", d.institutionId)
    }

    @Test fun `detect Stanbic sender`() {
        val d = InstitutionDetector.detect("Stanbic", "body")
        assertNotNull(d)
        assertEquals("stanbic", d.institutionId)
    }

    @Test fun `detect PesaLink sender`() {
        val d = InstitutionDetector.detect("PesaLink", "body")
        assertNotNull(d)
        assertEquals("pesalink", d.institutionId)
    }
}
