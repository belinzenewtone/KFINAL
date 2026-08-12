package com.belinze.lifeos.ui.screen.finance

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
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.MerchantDetailViewModel

// ─────────────────────────────────────────────────────────────────────────────
// MerchantDetailScreen — 1:1 port of MerchantDetailScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MerchantDetailScreen(
    merchant:       String,
    navController:  NavHostController,
    viewModel:      MerchantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(merchant) { viewModel.load(merchant) }

    PageScaffold(
        eyebrow   = "Merchant",
        title     = merchant,
        subtitle  = "${state.transactions.size} transaction${if (state.transactions.size != 1) "s" else ""}",
        onBack    = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
            }
            return@PageScaffold
        }

        if (state.transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                contentAlignment = Alignment.Center) {
                Text("No transactions with this merchant.",
                    color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
            }
            return@PageScaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Stats card ────────────────────────────────────────────────
            item {
                val s = state.stats
                if (s != null) {
                    FrostCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatColumn("Total", formatCurrency(s.totalSpend))
                            StatColumn("Avg", formatCurrency(s.avgAmount))
                            StatColumn("Peak day", s.peakDay?.take(10) ?: "—")
                        }
                    }
                }
            }

            items(state.transactions, key = { it.id }) { tx ->
                val isCredit = tx.transactionType == "income"
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = tx.date?.take(10) ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                        )
                        Text(
                            text  = tx.category?.replaceFirstChar { it.uppercase() } ?: "Uncategorized",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                        )
                    }
                    Text(
                        text       = "${if (isCredit) "+" else "-"}${formatCurrency(tx.amount)}",
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isCredit) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
    }
}
