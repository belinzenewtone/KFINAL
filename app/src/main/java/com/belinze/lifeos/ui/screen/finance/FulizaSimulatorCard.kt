package com.belinze.lifeos.ui.screen.finance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.belinze.lifeos.data.db.entity.FulizaLoanEntity
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.lifeos.sms.FulizaProjection
import java.time.LocalDate

@Composable
fun FulizaSimulatorCard(
    activeLoans: List<FulizaLoanEntity>,
    modifier:    Modifier = Modifier,
) {
    if (activeLoans.isEmpty()) return

    val outstanding = activeLoans.sumOf { (it.drawAmountKes - it.totalRepaidKes).coerceAtLeast(0.0) }
    if (outstanding <= 0.0) return

    var expanded by remember { mutableStateOf(false) }
    var dailyRepaymentText by remember { mutableStateOf("") }

    val schedule by remember(dailyRepaymentText, outstanding) {
        derivedStateOf {
            val repayment = dailyRepaymentText.toDoubleOrNull() ?: 0.0
            if (repayment > 0) {
                FulizaProjection.project(
                    FulizaProjection.Input(
                        outstandingKes    = outstanding,
                        dailyRepaymentKes = repayment,
                    )
                )
            } else null
        }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.sm)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text  = "Fuliza Simulator",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text  = "${formatCurrency(outstanding)} outstanding · ${activeLoans.size} loan${if (activeLoans.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(Spacing.sm))

                    // Daily fee info line (before repayment input)
                    val currentDailyFee = FulizaProjection.project(
                        FulizaProjection.Input(outstanding, 0.01)
                    ).dailyMaintenanceFeeKes
                    Text(
                        text  = "Daily maintenance fee: ${formatCurrency(currentDailyFee)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value         = dailyRepaymentText,
                        onValueChange = { dailyRepaymentText = it.filter { c -> c.isDigit() || c == '.' } },
                        label         = { Text("Daily repayment (KES)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(Spacing.sm))

                    when {
                        schedule == null -> {
                            Text(
                                text  = "Enter a daily repayment amount to see your payoff projection.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        schedule!!.willGrowForever -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text  = "Balance will grow — daily fee exceeds your repayment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        else -> {
                            val days     = schedule!!.estimatedDaysToPayoff
                            val payoffDate = days?.let { LocalDate.now().plusDays(it.toLong()) }
                            val totalFees = schedule!!.schedule.sumOf { it.dailyFeeKes }
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                SimRow("Payoff in",     "${days ?: "?"} days")
                                payoffDate?.let {
                                    SimRow("Payoff date", it.toString())
                                }
                                SimRow("Total extra fees", formatCurrency(totalFees))
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun SimRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color      = Color(0xFF34D399),
        )
    }
}
