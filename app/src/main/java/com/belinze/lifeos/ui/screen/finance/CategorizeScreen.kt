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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.CategorizeViewModel

// ─────────────────────────────────────────────────────────────────────────────
// CategorizeScreen — 1:1 port of CategorizeScreen.tsx
//
// Groups uncategorized transactions by merchant; tapping a category assigns it
// to every transaction in that merchant group.
// ─────────────────────────────────────────────────────────────────────────────

private val CATEGORIES = listOf(
    "food", "transport", "entertainment", "utilities", "health",
    "shopping", "education", "housing", "savings", "personal", "other",
)

@Composable
fun CategorizeScreen(
    navController: NavHostController,
    viewModel:     CategorizeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    PageScaffold(
        eyebrow = "Finance",
        title   = "Categorize",
        onBack  = { navController.popBackStack() },
    ) {
        // Success/error banner
        if (state.message != null) {
            TopBanner(
                tone      = if (state.isError) BannerTone.Error else BannerTone.Success,
                message   = state.message ?: "",
                visible   = true,
                onDismiss = { viewModel.clearMessage() },
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(Spacing.md))
                    Text("Loading uncategorized transactions…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                }
            }
            return@PageScaffold
        }

        if (state.groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                contentAlignment = Alignment.Center) {
                Text("All caught up — nothing to categorize.",
                    color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
            }
            return@PageScaffold
        }

        val totalCount = state.groups.sumOf { it.transactionCount }
        Text(
            text  = "$totalCount transaction${if (totalCount != 1) "s" else ""} need a category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
            modifier = Modifier.padding(bottom = Spacing.md),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.groups, key = { it.merchant }) { group ->
                MerchantGroupCard(
                    group     = group,
                    onAssign  = { cat -> viewModel.assignCategory(group.merchant, cat) },
                )
            }
            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

@Composable
private fun MerchantGroupCard(
    group:    CategorizeViewModel.MerchantGroup,
    onAssign: (String) -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    FrostCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.merchant, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text  = "${group.transactionCount} txns · ${formatCurrency(group.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    )
                }
            }

            // Category chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CATEGORIES.take(6).forEach { cat ->
                    FilterChip(
                        selected = selected == cat,
                        onClick  = {
                            selected = cat
                            onAssign(cat)
                        },
                        label    = { Text(cat.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                    )
                }
            }
        }
    }
}
