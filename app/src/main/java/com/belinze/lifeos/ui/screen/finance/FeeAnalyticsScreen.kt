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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.FeeAnalyticsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// FeeAnalyticsScreen — 1:1 port of FeeAnalyticsScreen.tsx
//
// Shows total service charges for the month, a per-category bar breakdown,
// and the underlying fee transactions.
// ─────────────────────────────────────────────────────────────────────────────

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
        title   = "Service Charges",
        onBack  = { navController.popBackStack() },
        scrollable = false,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Total ─────────────────────────────────────────────────────
            item {
                FrostCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
                    Text("Total this month", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    Text(formatCurrency(state.totalFees),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = WARNING)
                }
            }

            // ── Category bars ─────────────────────────────────────────────
            item {
                Text("By category", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = Spacing.sm))
            }
            if (state.categories.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                        contentAlignment = Alignment.Center) {
                        Text("No service charges this month.",
                            color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                    }
                }
            } else {
                items(state.categories, key = { it.category ?: "other" }) { cat ->
                    FeeBar(cat, state.categories.first().total)
                }
            }

            // ── Fee transactions ──────────────────────────────────────────
            if (state.transactions.isNotEmpty()) {
                item {
                    Text("Recent charges", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm))
                }
                items(state.transactions, key = { it.id }) { tx ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.merchant ?: "Fee", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground)
                            Text(tx.date?.take(10) ?: "", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.45f))
                        }
                        Text("-${formatCurrency(tx.fee ?: 0.0)}", fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

@Composable
private fun FeeBar(cat: FeeCategoryTotal, maxTotal: Double) {
    val ratio = if (maxTotal > 0) (cat.total / maxTotal).toFloat().coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(targetValue = ratio, animationSpec = tween(500), label = "fee_bar")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(cat.category?.replaceFirstChar { it.uppercase() } ?: "Other",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground)
            Text(formatCurrency(cat.total), style = MaterialTheme.typography.bodyMedium,
                color = WARNING, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(animated).height(8.dp)
                    .background(WARNING, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}
