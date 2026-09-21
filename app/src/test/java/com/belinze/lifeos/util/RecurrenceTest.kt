package com.belinze.lifeos.util

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Port of RFINAL src/utils/__tests__/recurrence.test.ts — advanceCadencePastNow
 * cases, using a fixed "now" for determinism: 2026-07-03T12:00:00Z (Friday).
 */
class RecurrenceTest {
    private val NOW: Long = Instant.parse("2026-07-03T12:00:00Z").toEpochMilli()

    private fun assertIso(expected: String, actual: String?) {
        assertNotNull(actual)
        assertEquals(
            Instant.parse(expected).toEpochMilli(),
            Instant.parse(actual).toEpochMilli(),
        )
    }

    @Test
    fun `returns null when already in the future`() {
        assertNull(advanceCadencePastNow("2026-12-01T09:00:00Z", "monthly", NOW))
    }

    @Test
    fun `returns null for invalid iso or unknown cadence`() {
        assertNull(advanceCadencePastNow("garbage", "daily", NOW))
        assertNull(advanceCadencePastNow("2026-01-01T09:00:00Z", "fortnightly", NOW))
    }

    @Test
    fun `advances hourly to the next future hour slot`() {
        assertIso("2026-07-03T12:30:00Z", advanceCadencePastNow("2026-07-03T09:30:00Z", "hourly", NOW))
    }

    @Test
    fun `advances daily preserving time of day`() {
        assertIso("2026-07-04T05:00:00Z", advanceCadencePastNow("2026-06-28T05:00:00Z", "daily", NOW))
    }

    @Test
    fun `advances weekly preserving weekday`() {
        assertIso("2026-07-07T18:00:00Z", advanceCadencePastNow("2026-06-02T18:00:00Z", "weekly", NOW))
    }

    @Test
    fun `advances biweekly in 14-day steps from the anchor`() {
        assertIso("2026-07-03T18:00:00Z", advanceCadencePastNow("2026-06-05T18:00:00Z", "biweekly", NOW))
    }

    @Test
    fun `advances mon_fri to the next weekday only`() {
        assertIso("2026-07-06T09:00:00Z", advanceCadencePastNow("2026-07-03T09:00:00Z", "mon_fri", NOW))
    }

    @Test
    fun `advances monthly with 31st clamping`() {
        assertIso("2026-07-31T08:00:00Z", advanceCadencePastNow("2026-05-31T08:00:00Z", "monthly", NOW))
    }

    @Test
    fun `advances yearly`() {
        assertIso("2027-07-01T08:00:00Z", advanceCadencePastNow("2025-07-01T08:00:00Z", "yearly", NOW))
    }
}
