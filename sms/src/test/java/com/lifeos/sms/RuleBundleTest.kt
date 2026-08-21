package com.lifeos.sms

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Ported from KOTLIN shared/androidUnitTest RuleBundleTest (Phase 2 backport).
 * Robolectric runner required: BundleCompiler uses org.json (android.jar stub on JVM).
 *
 * Verifies that:
 *  1. Valid JSON parses correctly
 *  2. Compiled rules contain the right number of patterns
 *  3. Rules with unknown categories are silently dropped
 *  4. Rules with bad regex patterns are silently dropped (rule kept if some survive)
 *  5. Rules with ZERO valid patterns AND fallbacks are dropped entirely
 *  6. activeRules() returns loaded bundle after loadBundle()
 *  7. clearBundle() reverts to DETECTION_RULES
 *  8. A hot-swapped bundle is actually used by the parser
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RuleBundleTest {
    private val VALID_BUNDLE_JSON = """
        {
          "version": 1,
          "publishedAt": "2026-07-26",
          "rules": [
            {
              "id": "sent_p2p",
              "category": "SENT",
              "description": "Peer-to-peer transfer",
              "patterns": ["(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to"],
              "fallbackPatterns": ["sent to\\s+[A-Z]"],
              "counterpartyPatterns": ["sent to\\s+(.+?)(?:\\s+on\\s|\\.|${'$'})"]
            },
            {
              "id": "received",
              "category": "RECEIVED",
              "description": "Money received",
              "patterns": ["(?:you have\\s+)?received\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+from"],
              "fallbackPatterns": ["received from\\s+[A-Z]"],
              "counterpartyPatterns": ["received\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+from\\s+(.+?)(?:\\s+on|\\.|${'$'})"]
            }
          ]
        }
    """.trimIndent()

    private val BUNDLE_WITH_BAD_ENTRIES_JSON = """
        {
          "version": 2,
          "publishedAt": "2026-07-26",
          "rules": [
            {
              "id": "good_rule",
              "category": "SENT",
              "description": "Good rule",
              "patterns": ["(?:Ksh|KES)\\s?[\\d]+\\s+sent to"],
              "fallbackPatterns": [],
              "counterpartyPatterns": []
            },
            {
              "id": "bad_category",
              "category": "NONEXISTENT_CATEGORY",
              "description": "This rule has an unknown category and must be dropped",
              "patterns": ["some pattern"],
              "fallbackPatterns": [],
              "counterpartyPatterns": []
            },
            {
              "id": "bad_regex",
              "category": "RECEIVED",
              "description": "All patterns are invalid regex — rule must be dropped",
              "patterns": ["[invalid(regex"],
              "fallbackPatterns": ["[also(invalid"],
              "counterpartyPatterns": []
            },
            {
              "id": "partially_bad",
              "category": "DEPOSIT",
              "description": "Some valid, some bad patterns — rule kept with valid ones only",
              "patterns": ["[bad(regex", "deposited\\s+(?:Ksh|KES)"],
              "fallbackPatterns": [],
              "counterpartyPatterns": []
            }
          ]
        }
    """.trimIndent()

    // ── JSON parsing ─────────────────────────────────────────────────────────

    @Test
    fun `fromJson parses valid bundle correctly`() {
        val bundle = BundleCompiler.fromJson(VALID_BUNDLE_JSON)
        assertNotNull(bundle, "fromJson must return non-null for valid JSON")
        assertEquals(1, bundle.version)
        assertEquals("2026-07-26", bundle.publishedAt)
        assertEquals(2, bundle.rules.size)

        val sentRule = bundle.rules.find { it.id == "sent_p2p" }
        assertNotNull(sentRule)
        assertEquals("SENT", sentRule.category)
        assertEquals(1, sentRule.patterns.size)
        assertEquals(1, sentRule.fallbackPatterns.size)
        assertEquals(1, sentRule.counterpartyPatterns.size)
    }

    @Test
    fun `fromJson returns null for malformed JSON`() {
        assertNull(BundleCompiler.fromJson("{invalid json}"),
            "Malformed JSON must return null")
        assertNull(BundleCompiler.fromJson(""),
            "Empty string must return null")
        assertNull(BundleCompiler.fromJson("null"),
            "Bare null must return null")
    }

    // ── Rule compilation ─────────────────────────────────────────────────────

    @Test
    fun `compile produces valid DetectionRule objects`() {
        val bundle = BundleCompiler.fromJson(VALID_BUNDLE_JSON)!!
        val rules  = BundleCompiler.compile(bundle)

        assertEquals(2, rules.size, "Both valid rules must compile")

        val sentRule = rules.find { it.id == "sent_p2p" }
        assertNotNull(sentRule)
        assertEquals(SmsParserConfig.SmsCategory.SENT, sentRule.category)
        assertTrue(sentRule.patterns.isNotEmpty())
        assertTrue(sentRule.fallbackPatterns.isNotEmpty())
        assertTrue(sentRule.counterpartyPatterns.isNotEmpty())
    }

    @Test
    fun `rules with unknown category are silently dropped`() {
        val bundle = BundleCompiler.fromJson(BUNDLE_WITH_BAD_ENTRIES_JSON)!!
        val rules  = BundleCompiler.compile(bundle)

        assertNull(rules.find { it.id == "bad_category" },
            "Rule with unknown category must be dropped")
    }

    @Test
    fun `rules with all invalid regex are silently dropped`() {
        val bundle = BundleCompiler.fromJson(BUNDLE_WITH_BAD_ENTRIES_JSON)!!
        val rules  = BundleCompiler.compile(bundle)

        assertNull(rules.find { it.id == "bad_regex" },
            "Rule with zero valid patterns and fallbacks must be dropped")
    }

    @Test
    fun `rules with some valid and some bad regex are kept with valid patterns only`() {
        val bundle = BundleCompiler.fromJson(BUNDLE_WITH_BAD_ENTRIES_JSON)!!
        val rules  = BundleCompiler.compile(bundle)

        val partial = rules.find { it.id == "partially_bad" }
        assertNotNull(partial, "Rule with at least one valid pattern must be kept")
        assertEquals(1, partial.patterns.size,
            "Only the valid pattern (not the bad regex) must be compiled")
    }

    @Test
    fun `good rule always survives mixed-validity bundle`() {
        val bundle = BundleCompiler.fromJson(BUNDLE_WITH_BAD_ENTRIES_JSON)!!
        val rules  = BundleCompiler.compile(bundle)

        assertNotNull(rules.find { it.id == "good_rule" },
            "Good rule must survive regardless of other bad entries")
    }

    // ── Hot-swap integration ─────────────────────────────────────────────────

    @Test
    fun `loadBundle swaps activeRules and clearBundle reverts to DETECTION_RULES`() {
        val bundle = BundleCompiler.fromJson(VALID_BUNDLE_JSON)!!
        val compiled = BundleCompiler.compile(bundle)

        SmsParserConfig.loadBundle(compiled)
        assertEquals(compiled, SmsParserConfig.activeRules(),
            "activeRules must return the loaded bundle after loadBundle()")

        SmsParserConfig.clearBundle()
        assertSame(SmsParserConfig.DETECTION_RULES, SmsParserConfig.activeRules(),
            "activeRules must return DETECTION_RULES after clearBundle()")
    }

    @Test
    fun `loaded bundle rules are used by the parser`() {
        val bundle = BundleCompiler.fromJson(VALID_BUNDLE_JSON)!!
        val compiled = BundleCompiler.compile(bundle)
        SmsParserConfig.loadBundle(compiled)

        try {
            // A standard SENT SMS — should be routed via the bundle's sent_p2p rule
            val result = SmsParser.parse(
                "SIE8QWE123 Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 26/7/26 at 10:00 AM. New M-PESA balance is Ksh2,500.00.",
            )
            assertTrue(result is SmsParser.SmsParseResult.Success,
                "Parser must succeed with loaded bundle")
            assertEquals(SmsParserConfig.SmsCategory.SENT,
                (result as SmsParser.SmsParseResult.Success).transaction.category,
                "Category must match the loaded bundle's sent_p2p rule")
        } finally {
            SmsParserConfig.clearBundle()
        }
    }
}
