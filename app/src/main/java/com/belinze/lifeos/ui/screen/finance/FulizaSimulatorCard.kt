package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// FulizaSimulatorCard — 1:1 port of components/finance/FulizaSimulatorCard.tsx
// and utils/fulizaProjection.ts (self-contained: the tariff tables here match
// the React source exactly and are intentionally NOT shared with
// com.lifeos.sms.FulizaProjection, whose tier tables are unrelated numbers
// used elsewhere for live SMS fee prediction).
// ─────────────────────────────────────────────────────────────────────────────

private const val MIN_DAILY = 10
private const val MAX_DAILY = 5_000
private const val STEP = 50

private data class FeeTier(val maxAmount: Double, val fee: Double)

private val ACCESS_FEE_TIERS = listOf(
    FeeTier(100.0, 2.0),
    FeeTier(500.0, 5.0),
    FeeTier(1_000.0, 10.0),
    FeeTier(1_500.0, 15.0),
    FeeTier(2_500.0, 25.0),
    FeeTier(5_000.0, 45.0),
    FeeTier(7_500.0, 60.0),
    FeeTier(10_000.0, 75.0),
    FeeTier(15_000.0, 100.0),
    FeeTier(20_000.0, 125.0),
    FeeTier(30_000.0, 150.0),
    FeeTier(Double.MAX_VALUE, 200.0),
)

private val DAILY_FEE_TIERS = listOf(
    FeeTier(100.0, 2.0),
    FeeTier(500.0, 5.0),
    FeeTier(1_000.0, 10.0),
    FeeTier(1_500.0, 15.0),
    FeeTier(2_500.0, 20.0),
    FeeTier(5_000.0, 30.0),
    FeeTier(7_500.0, 45.0),
    FeeTier(10_000.0, 55.0),
    FeeTier(15_000.0, 60.0),
    FeeTier(20_000.0, 65.0),
    FeeTier(30_000.0, 70.0),
    FeeTier(Double.MAX_VALUE, 75.0),
)

private fun lookupFee(tiers: List<FeeTier>, principal: Double): Double =
    tiers.firstOrNull { principal <= it.maxAmount }?.fee ?: tiers.last().fee

private fun accessFeeForAmount(principalKes: Double) = lookupFee(ACCESS_FEE_TIERS, principalKes)

private fun dailyFeeForAmount(principalKes: Double) = lookupFee(DAILY_FEE_TIERS, principalKes)

private data class FulizaProjectionResult(
    val accessFeeKes: Double,
    val dailyFeeKes: Double,
    val totalOwedKes: Double,
    val totalInterestKes: Double,
    val daysElapsed: Int,
    val estimatedDaysToPayoff: Int?,
    val estimatedPayoffDate: LocalDate?,
)

/** Direct port of utils/fulizaProjection.ts projectFuliza(). */
private fun projectFuliza(
    principalKes: Double,
    alreadyRepaidKes: Double,
    drawDate: LocalDate,
    asOf: LocalDate,
    dailyRepaymentKes: Double,
): FulizaProjectionResult {
    val daysElapsed = (asOf.toEpochDay() - drawDate.toEpochDay()).toInt().coerceAtLeast(0)

    val accessFeeKes = accessFeeForAmount(principalKes)
    val dailyFeeKes = dailyFeeForAmount(principalKes)

    val totalInterestKes = accessFeeKes + dailyFeeKes * daysElapsed
    val totalOwedKes = (principalKes + totalInterestKes - alreadyRepaidKes).coerceAtLeast(0.0)

    var outstanding = totalOwedKes
    var estimatedDaysToPayoff: Int? = null

    if (dailyRepaymentKes > dailyFeeKes && outstanding > 0) {
        val netRepayment = dailyRepaymentKes - dailyFeeKes
        val maxSimDays = minOf(365, ceil(outstanding / netRepayment).toInt() + 1)
        for (d in 1..maxSimDays) {
            outstanding = (outstanding + dailyFeeKes - dailyRepaymentKes).coerceAtLeast(0.0)
            if (outstanding == 0.0) {
                estimatedDaysToPayoff = d
                break
            }
        }
    }

    val estimatedPayoffDate = estimatedDaysToPayoff?.let { asOf.plusDays(it.toLong()) }

    return FulizaProjectionResult(
        accessFeeKes = accessFeeKes,
        dailyFeeKes = dailyFeeKes,
        totalOwedKes = totalOwedKes,
        totalInterestKes = totalInterestKes,
        daysElapsed = daysElapsed,
        estimatedDaysToPayoff = estimatedDaysToPayoff,
        estimatedPayoffDate = estimatedPayoffDate,
    )
}

private fun formatKes(amount: Double): String = "Ksh " + String.format(Locale.US, "%,.2f", amount)

@Composable
fun FulizaSimulatorCard(
    principalKes: Double,
    totalRepaidKes: Double,
    drawDateIso: String,
    asOfDateIso: String? = null,
    modifier: Modifier = Modifier,
) {
    val drawDate = remember(drawDateIso) {
        runCatching { LocalDate.parse(drawDateIso.take(10)) }.getOrDefault(LocalDate.now())
    }
    val asOf = remember(asOfDateIso) {
        asOfDateIso?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: LocalDate.now()
    }

    var dailyRepayment by remember(principalKes) {
        // Default the slider to 2x the daily fee so the payoff projection is
        // realistic immediately instead of showing "grows forever".
        mutableIntStateOf(
            MIN_DAILY.times(2).coerceAtLeast(ceil(principalKes / 30).toInt()).coerceIn(MIN_DAILY, MAX_DAILY),
        )
    }

    val projection = remember(principalKes, totalRepaidKes, drawDate, asOf, dailyRepayment) {
        projectFuliza(principalKes, totalRepaidKes, drawDate, asOf, dailyRepayment.toDouble())
    }

    val repaidFraction = if (principalKes > 0) {
        (totalRepaidKes / (principalKes + projection.totalInterestKes)).coerceIn(0.0, 1.0)
    } else {
        0.0
    }

    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val accentColor = if (projection.totalOwedKes == 0.0) primary else error

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0x20 / 255f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("FULIZA SIMULATOR", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatKes(projection.totalOwedKes)} owed", style = MaterialTheme.typography.titleMedium, color = accentColor)
                }
                Text("Day ${projection.daysElapsed}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Repayment progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                        .background(accentColor.copy(alpha = 0x20 / 255f), RoundedCornerShape(3.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(repaidFraction.toFloat().coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(primary, RoundedCornerShape(3.dp)),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Repaid ${formatKes(totalRepaidKes)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(repaidFraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Fee breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, outlineVariant, RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeeCell("Access fee", formatKes(projection.accessFeeKes), Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(outlineVariant))
                FeeCell("Daily fee", "${formatKes(projection.dailyFeeKes)}/day", Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(outlineVariant))
                FeeCell("Interest so far", formatKes(projection.totalInterestKes), Modifier.weight(1f))
            }

            // Repayment rate control
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily repayment", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatKes(dailyRepayment.toDouble()), style = MaterialTheme.typography.labelMedium, color = primary)
                }
                Slider(
                    value = dailyRepayment.toFloat(),
                    onValueChange = { raw ->
                        val stepped = (raw / STEP).roundToInt() * STEP
                        dailyRepayment = stepped.coerceIn(MIN_DAILY, MAX_DAILY)
                    },
                    valueRange = MIN_DAILY.toFloat()..MAX_DAILY.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = primary,
                        activeTrackColor = primary,
                        inactiveTrackColor = primary.copy(alpha = 0x30 / 255f),
                    ),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatKes(MIN_DAILY.toDouble()), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatKes(MAX_DAILY.toDouble()), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = { dailyRepayment = (dailyRepayment - STEP).coerceAtLeast(MIN_DAILY) },
                        enabled = dailyRepayment > MIN_DAILY,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { dailyRepayment = (dailyRepayment + STEP).coerceAtMost(MAX_DAILY) },
                        enabled = dailyRepayment < MAX_DAILY,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Payoff estimate
            val days = projection.estimatedDaysToPayoff
            if (days != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, primary.copy(alpha = 0x30 / 255f), RoundedCornerShape(8.dp))
                        .background(primary.copy(alpha = 0x15 / 255f), RoundedCornerShape(8.dp))
                        .padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                    Text(
                        "Paid off in $days day${if (days != 1) "s" else ""} · ${
                            projection.estimatedPayoffDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accentColor.copy(alpha = 0x25 / 255f), RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0x10 / 255f), RoundedCornerShape(8.dp))
                        .padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Text(
                        if (dailyRepayment <= projection.dailyFeeKes) {
                            "Repayment below daily fee — loan grows indefinitely"
                        } else {
                            "Increase daily repayment to see payoff estimate"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeeCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Spacing.xs), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}
