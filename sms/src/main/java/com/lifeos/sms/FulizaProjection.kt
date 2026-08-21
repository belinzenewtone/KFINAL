package com.lifeos.sms

/**
 * Fuliza M-PESA payoff projection engine.
 *
 * Given an outstanding balance and the user's daily repayment capacity,
 * simulates day-by-day Fuliza payoff using Safaricom's published fee tiers.
 *
 * Fee tiers source: Safaricom Fuliza M-PESA published schedule (2024).
 * Two fee types apply:
 *  - Access fee  — one-time charge on the draw amount (already paid; included for display).
 *  - Daily maintenance fee — accrues every day until the loan is fully repaid.
 */
object FulizaProjection {
    data class Input(
        /** Current outstanding Fuliza balance in KES (principal + fees already added). */
        val outstandingKes: Double,
        /** Amount the user plans to repay per day in KES. 0 or negative → willGrowForever. */
        val dailyRepaymentKes: Double,
    )

    data class DayEntry(
        val day: Int,
        val balanceKes: Double,
        val dailyFeeKes: Double,
        val repaymentKes: Double,
    )

    data class FulizaSchedule(
        /** One-time access fee for the ORIGINAL draw amount (informational only). */
        val accessFeeKes: Double,
        /** Daily maintenance fee on the CURRENT outstanding balance. */
        val dailyMaintenanceFeeKes: Double,
        /**
         * Number of days until the balance reaches zero given [Input.dailyRepaymentKes].
         * Null when [willGrowForever] is true or the outstanding balance is already zero.
         */
        val estimatedDaysToPayoff: Int?,
        /** Day-by-day schedule up to the payoff day (capped at 365 entries). */
        val schedule: List<DayEntry>,
        /**
         * True when the daily maintenance fee meets or exceeds the daily repayment —
         * the balance will never reach zero at the current repayment rate.
         */
        val willGrowForever: Boolean,
    )

    // ── Safaricom Fuliza fee tiers (2024) ─────────────────────────────────────

    private data class Tier(val maxKes: Double, val accessFeeKes: Double, val dailyFeeKes: Double)

    private val TIERS = listOf(
        Tier(100.0,   2.0,  1.0),
        Tier(500.0,   5.0,  3.0),
        Tier(1_000.0,  10.0,  5.0),
        Tier(1_500.0,  15.0,  7.0),
        Tier(2_500.0,  20.0, 10.0),
        Tier(5_000.0,  30.0, 15.0),
        Tier(10_000.0,  60.0, 25.0),
        Tier(50_000.0, 100.0, 40.0),
        Tier(Double.MAX_VALUE, 150.0, 60.0),
    )

    private fun accessFee(amountKes: Double): Double =
        TIERS.first { amountKes <= it.maxKes }.accessFeeKes

    private fun dailyFee(balanceKes: Double): Double =
        TIERS.first { balanceKes <= it.maxKes }.dailyFeeKes

    /**
     * Project the payoff schedule for [input].
     *
     * Simulation runs day-by-day for up to 365 days:
     *  1. Accrue daily maintenance fee (re-tiered on the current balance).
     *  2. Apply the day's repayment.
     *  3. If balance ≤ 0 → payoff reached.
     *
     * The schedule list is truncated at the payoff day or 365, whichever comes first.
     */
    fun project(input: Input): FulizaSchedule {
        val outstanding = input.outstandingKes.coerceAtLeast(0.0)
        val currentDailyFee = dailyFee(outstanding)
        val accessFeeKes = accessFee(outstanding)

        if (outstanding <= 0.0) {
            return FulizaSchedule(
                accessFeeKes = accessFeeKes,
                dailyMaintenanceFeeKes = 0.0,
                estimatedDaysToPayoff = 0,
                schedule = emptyList(),
                willGrowForever = false,
            )
        }

        val willGrowForever = input.dailyRepaymentKes <= currentDailyFee
        if (willGrowForever) {
            return FulizaSchedule(
                accessFeeKes = accessFeeKes,
                dailyMaintenanceFeeKes = currentDailyFee,
                estimatedDaysToPayoff = null,
                schedule = emptyList(),
                willGrowForever = true,
            )
        }

        val schedule = mutableListOf<DayEntry>()
        var balance = outstanding
        var daysToPayoff: Int? = null

        for (day in 1..365) {
            val fee = dailyFee(balance)
            balance += fee
            val repayment = minOf(input.dailyRepaymentKes, balance)
            balance -= repayment
            schedule.add(DayEntry(day = day, balanceKes = balance, dailyFeeKes = fee, repaymentKes = repayment))
            if (balance <= 0.0) {
                daysToPayoff = day
                break
            }
        }

        return FulizaSchedule(
            accessFeeKes = accessFeeKes,
            dailyMaintenanceFeeKes = currentDailyFee,
            estimatedDaysToPayoff = daysToPayoff,
            schedule = schedule,
            willGrowForever = false,
        )
    }
}
