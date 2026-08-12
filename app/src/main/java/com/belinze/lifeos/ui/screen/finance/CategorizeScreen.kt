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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.CategorizeViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CATEGORIZE_CATEGORIES = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
    "housing", "personal_care", "subscriptions", "miscellaneous",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorizeScreen(
    navController: NavHostController,
    viewModel:     CategorizeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    PageScaffold(
        eyebrow = "Finance",
        title = "Categorize",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (state.message != null) {
            TopBanner(
                tone = if (state.isError) BannerTone.Error else BannerTone.Success,
                message = state.message.orEmpty(),
                visible = true,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@PageScaffold
        }

        if (state.transactions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("All transactions categorized", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Every transaction has a meaningful category. Nice work!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
            return@PageScaffold
        }

        Text(
            "${state.transactions.size} ${if (state.transactions.size == 1) "transaction needs" else "transactions need"} a category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.base),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.transactions, key = { it.id }) { tx ->
                TransactionCard(tx = tx, onCategorySelected = { cat ->
                    viewModel.assignCategory(tx.id, cat)
                })
            }
            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionCard(
    tx: TransactionEntity,
    onCategorySelected: (String) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }

    val primaryLabel = if (!tx.description.isNullOrBlank() && tx.description != tx.merchant)
        tx.description else tx.merchant
    val sourceLabel = if (!tx.description.isNullOrBlank() && tx.description != tx.merchant)
        tx.merchant else null

    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                    Text(primaryLabel ?: "", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                    sourceLabel?.let {
                        Text("via $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(formatDateTime(tx.date), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp))
                }
                Text(formatCurrency(tx.amount), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { pickerOpen = true },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
            ) {
                Text("Pick a category…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (pickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { pickerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            ) {
                Text("Pick a category", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.base))
                CATEGORIZE_CATEGORIES.forEach { cat ->
                    TextButton(
                        onClick = {
                            pickerOpen = false
                            onCategorySelected(cat)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            capitalize(cat),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}

private fun capitalize(value: String): String =
    value.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun formatDateTime(iso: String?): String = try {
    LocalDateTime.parse(iso?.take(19)).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
} catch (_: Exception) { iso?.take(10) ?: "" }
