package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
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
        topBanner = {
            if (state.message != null) {
                // CZ-1: auto-dismiss after 1500 ms to match RN behaviour
                TopBanner(
                    tone          = if (state.isError) BannerTone.Error else BannerTone.Success,
                    message       = state.message.orEmpty(),
                    visible       = true,
                    onDismiss     = { viewModel.clearMessage() },
                    autoDismissMs = 1500,
                )
            }
        },
    ) {
        // Loading state — modest top padding so it doesn't look pushed down.
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
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
                Icon(Icons.Outlined.CheckCircle, contentDescription = null,
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

        // Compact summary header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "${state.transactions.size} ${if (state.transactions.size == 1) "transaction" else "transactions"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Tap a card to assign a category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatCurrency(state.transactions.sumOf { it.amount }),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

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

    val primaryLabel = if (!tx.description.isNullOrBlank() && tx.description != tx.merchant) {
        tx.description
    } else {
        tx.merchant
    }
    val sourceLabel = if (!tx.description.isNullOrBlank() && tx.description != tx.merchant) {
        tx.merchant
    } else {
        null
    }

    val isIncome     = tx.transactionType == "income"
    val isTransfer   = tx.transactionType == "transfer"
    val typeIcon     = when {
        isIncome   -> Icons.Outlined.ArrowUpward
        isTransfer -> Icons.Outlined.SwapHoriz
        else       -> Icons.Outlined.ArrowDownward
    }
    val typeColor    = when {
        isIncome   -> Color(0xFF22C55E)
        isTransfer -> MaterialTheme.colorScheme.tertiary
        else       -> MaterialTheme.colorScheme.error
    }
    val amountColor  = typeColor

    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Transaction-type icon badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(typeColor.copy(alpha = 0.13f), MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        typeIcon,
                        contentDescription = null,
                        tint     = typeColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        primaryLabel ?: "",
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        fontWeight = FontWeight.Medium,
                    )
                    sourceLabel?.let {
                        Text(
                            "via $it",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        formatDateTime(tx.date),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    "${if (isIncome) "+" else "-"}${formatCurrency(tx.amount)}",
                    style      = MaterialTheme.typography.bodyLarge,
                    color      = amountColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick  = { pickerOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    "Assign category",
                    modifier   = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (pickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { pickerOpen = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            // CZ-3: cap at ~70% of screen height to match RN Modal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(Spacing.lg),
            ) {
                Text("Pick a category", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.sm))
                Text("Group this transaction under a category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.base))
                CATEGORIZE_CATEGORIES.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = androidx.compose.material3.ripple(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                ),
                            ) {
                                pickerOpen = false
                                onCategorySelected(cat)
                            }
                            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0x20 / 255f),
                                    MaterialTheme.shapes.medium,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                capitalize(cat).take(1),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            capitalize(cat),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
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
} catch (_: Exception) {
    iso?.take(10) ?: ""
}
