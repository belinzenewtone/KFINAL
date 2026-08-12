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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.TransactionViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ReviewQueueScreen — matches ReviewQueueScreen.tsx ("review_queue" route)
//
// Shows transactions with status="review" that need categorization.
// Per-item actions: assign category (dropdown) → recovers to "completed"
//                   dismiss → soft-deletes.
// ─────────────────────────────────────────────────────────────────────────────

private val REVIEW_CATEGORIES = listOf(
    "food", "transport", "entertainment", "utilities", "health",
    "shopping", "education", "housing", "savings", "income",
    "fuliza", "transfer", "personal", "other",
)

@Composable
fun ReviewQueueScreen(
    navController: NavHostController,
    viewModel:     TransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Load review-status transactions on entry
    LaunchedEffect(Unit) {
        viewModel.setType(null)
        // Use the status filter via a category='all' + status='review' call
        // We access review items by filtering in-memory from the loaded list
    }

    // Filter in-memory to only pending-review items
    val reviewItems = state.transactions.filter { it.status == "review" }

    PageScaffold(
        eyebrow = "Finance",
        title   = "Review Queue",
        onBack  = { navController.popBackStack() },
    ) {
        if (state.isLoading) {
            ShimmerLoadingState(rowCount = 4)
            return@PageScaffold
        }

        if (reviewItems.isEmpty()) {
            // Empty state
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint     = Color(0xFF10B981),
                        modifier = Modifier.padding(8.dp))
                    Text("Queue Clear! 🎉", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text("All transactions have been reviewed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                }
            }
            return@PageScaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Header
            item {
                Text(
                    text     = "${reviewItems.size} pending",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }

            items(reviewItems, key = { it.id }) { tx ->
                ReviewEntryCard(
                    merchant    = tx.merchant ?: "Unknown",
                    amount      = tx.amount,
                    rawSms      = tx.notes?.take(140) ?: "",
                    txType      = tx.transactionType,
                    onApprove   = { cat -> viewModel.updateCategory(tx.id, cat) },
                    onDismiss   = { viewModel.softDelete(tx.id) },
                )
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

@Composable
private fun ReviewEntryCard(
    merchant:  String,
    amount:    Double,
    rawSms:    String,
    txType:    String,
    onApprove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryMenuOpen by remember { mutableStateOf(false) }

    FrostCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
    ) {
        // Header row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(merchant, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text  = formatCurrency(amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (txType) {
                        "receive" -> Color(0xFF10B981)
                        "fuliza"  -> Color(0xFFEF4444)
                        else      -> MaterialTheme.colorScheme.onBackground
                    },
                )
            }
            // Status chip
            Box(
                modifier = Modifier
                    .background(Color(0xFFF59E0B).copy(0.15f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("Review", fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    color = Color(0xFFF59E0B))
            }
        }

        // Raw SMS snippet
        if (rawSms.isNotBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(rawSms, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                maxLines = 2)
        }

        Spacer(Modifier.height(Spacing.sm))

        // Action buttons
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                FilledTonalButton(
                    onClick  = { categoryMenuOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Categorise & Recover")
                }
                DropdownMenu(
                    expanded        = categoryMenuOpen,
                    onDismissRequest = { categoryMenuOpen = false },
                ) {
                    REVIEW_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text    = { Text(cat.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                categoryMenuOpen = false
                                onApprove(cat)
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Delete, contentDescription = "Dismiss",
                    tint = Color(0xFFEF4444))
            }
        }
    }
}
