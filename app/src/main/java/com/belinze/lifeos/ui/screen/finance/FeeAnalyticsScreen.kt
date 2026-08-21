package com.belinze.lifeos.ui.screen.finance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
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
import com.belinze.lifeos.data.db.dao.FeeCategoryTotal
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.FeeAnalyticsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val WARNING = Color(0xFFF5CB5C)

@Composable
fun FeeAnalyticsScreen(
    navController: NavHostController,
    viewModel:     FeeAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    PageScaffold(
        eyebrow = "Finance",
        title = "Service Charges",
        subtitle = "Airtime, Fuliza, withdrawals and subscriptions this month",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (!state.isLoading && state.categories.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No service charges this month", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(Spacing.sm))
                Text("No airtime, Fuliza, withdrawal, or subscription transactions found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
                        Text("This Month's Charges", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(state.totalFees),
                            style = MaterialTheme.typography.headlineLarge,
                            color = WARNING,
                            modifier = Modifier.padding(top = Spacing.xs))
                    }
                }

                if (state.categories.isNotEmpty()) {
                    item {
                        Text("By Category", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm))
                    }
                    item {
                        GlassCard {
                            Column {
                                val maxTotal = state.categories.maxOfOrNull { it.total } ?: 1.0
                                state.categories.forEachIndexed { i, cat ->
                                    FeeBar(cat, maxTotal)
                                    // FA-2: 2dp vertical margin around each category divider
                                    if (i < state.categories.size - 1) {
                                        Spacer(Modifier.height(2.dp))
                                        Divider()
                                        Spacer(Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.transactions.isNotEmpty()) {
                    item {
                        Text("Transactions", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm))
                    }
                    item {
                        GlassCard {
                            Column {
                                state.transactions.forEachIndexed { i, tx ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.merchant ?: "Unknown", style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                            Text(
                                                "${tx.category} · ${formatMonthDay(tx.date)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(formatCurrency(tx.amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium)
                                    }
                                    if (i < state.transactions.size - 1) Divider()
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
private fun FeeBar(cat: FeeCategoryTotal, maxTotal: Double) {
    val ratio = if (maxTotal > 0) (cat.total / maxTotal).toFloat().coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(targetValue = ratio, animationSpec = tween(500), label = "fee_bar")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                cat.category.orEmpty().replaceFirstChar { it.uppercase() }.lowercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(formatCurrency(cat.total), style = MaterialTheme.typography.bodyMedium,
                color = WARNING, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(animated).height(6.dp)
                    .background(WARNING, RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

private fun formatMonthDay(iso: String?): String = try {
    LocalDateTime.parse(iso?.take(19)).format(DateTimeFormatter.ofPattern("MMM d"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}
