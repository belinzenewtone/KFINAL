package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.TransactionViewModel

/**
 * A summary card showing all-time transaction history with a given counterparty.
 * Displayed on TransactionDetailScreen between the hero card and the details card.
 */
@Composable
fun CounterpartyCard(
    stats:    TransactionViewModel.CounterpartyStats,
    modifier: Modifier = Modifier,
) {
    if (stats.count <= 1) return   // Only interesting when there's a transaction history

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text       = "Counterparty history",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = stats.merchant,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CounterpartyMetric(
                    label = "Transactions",
                    value = "${stats.count}",
                )
                CounterpartyMetric(
                    label = "Total",
                    value = formatCurrency(stats.totalAmount),
                )
                CounterpartyMetric(
                    label = "Average",
                    value = formatCurrency(stats.avgAmount),
                )
            }
        }
    }
}

@Composable
private fun CounterpartyMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF60A5FA),
        )
    }
}
