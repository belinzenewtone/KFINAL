package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.MerchantDetailViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MerchantDetailScreen(
    merchant:       String,
    navController:  NavHostController,
    viewModel:      MerchantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(merchant) { viewModel.load(merchant) }

    PageScaffold(
        eyebrow = "Merchant",
        title = merchant,
        subtitle = "${state.transactions.size} transaction${if (state.transactions.size != 1) "s" else ""}",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (state.transactions.isEmpty() && !state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Receipt, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No transactions", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.sm))
                Text("No transactions found for this merchant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    val s = state.stats
                    if (s != null) {
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    StatColumn("Total Spend", formatCurrency(s.totalSpend))
                                    StatDivider()
                                    StatColumn("Transactions", "${s.txCount}")
                                    StatDivider()
                                    StatColumn("Avg. Amount", formatCurrency(s.avgAmount))
                                }
                                if (s.activeDays > 0) {
                                    Spacer(Modifier.height(Spacing.base))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant))
                                    Spacer(Modifier.height(Spacing.base))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        StatColumn("Active days", "${s.activeDays}")
                                        StatDivider()
                                        StatColumn("Avg per day", formatCurrency(s.avgPerDay))
                                        StatDivider()
                                        StatColumn("Peak day", s.peakDay?.let { formatPeakDay(it) } ?: "—")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Transaction History", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm))
                }

                item {
                    GlassCard {
                        Column {
                            state.transactions.forEachIndexed { i, tx ->
                                val isIncome = tx.transactionType?.uppercase() in setOf("RECEIVED", "DEPOSIT", "INCOME")
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            tx.category.orEmpty().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            "${formatDate(tx.date)} · ${formatTime(tx.date)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        "${if (isIncome) "+" else ""}${formatCurrency(tx.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isIncome) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                if (i < state.transactions.size - 1) {
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant))
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatDivider() {
    Box(modifier = Modifier.size(width = 1.dp, height = 36.dp)
        .background(MaterialTheme.colorScheme.outlineVariant))
}

private fun formatDate(iso: String?): String = try {
    LocalDateTime.parse(iso?.take(19)).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
} catch (_: Exception) { iso?.take(10) ?: "" }

private fun formatTime(iso: String?): String = try {
    LocalDateTime.parse(iso?.take(19)).format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: Exception) { "" }

private fun formatPeakDay(day: String): String = try {
    java.time.LocalDate.parse(day).format(DateTimeFormatter.ofPattern("MMM d"))
} catch (_: Exception) { day }
