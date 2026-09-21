package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.CategorizeViewModel
import com.belinze.lifeos.viewmodel.MerchantGroup
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    PageScaffold(
        eyebrow = "Finance",
        title = "Categorize",
        onBack = { navController.popBackStack() },
        scrollable = false,
        topBanner = {
            if (state.message != null) {
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

        // Summary + view toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
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
                    if (state.groupByMerchant) "${state.merchantGroups.size} merchants" else "Tap a card to assign a category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected  = state.groupByMerchant,
                    onClick   = { viewModel.setGroupByMerchant(true) },
                    label     = { Text("By Merchant", style = MaterialTheme.typography.labelMedium) },
                )
                FilterChip(
                    selected  = !state.groupByMerchant,
                    onClick   = { viewModel.setGroupByMerchant(false) },
                    label     = { Text("All", style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
        ) {
            if (state.groupByMerchant) {
                items(state.merchantGroups, key = { it.merchant }) { group ->
                    MerchantGroupCard(
                        group           = group,
                        onAssignAll     = { cat -> viewModel.assignCategoryForMerchant(group.merchant, cat) },
                        onAssignOne     = { id, cat -> viewModel.assignCategory(id, cat) },
                    )
                }
            } else {
                items(state.transactions, key = { it.id }) { tx ->
                    TransactionCard(tx = tx, onCategorySelected = { cat ->
                        viewModel.assignCategory(tx.id, cat)
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantGroupCard(
    group:       MerchantGroup,
    onAssignAll: (String) -> Unit,
    onAssignOne: (String, String) -> Unit,
) {
    var expanded      by remember { mutableStateOf(false) }
    var pickerOpen    by remember { mutableStateOf(false) }
    var pickerTarget  by remember { mutableStateOf<String?>(null) } // null = all, else tx id

    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${group.count} transaction${if (group.count != 1) "s" else ""} · ${formatCurrency(group.transactions.sumOf { it.amount })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick  = { pickerTarget = null; pickerOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    "Assign all ${group.count} as…",
                    modifier   = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            if (expanded) {
                Spacer(Modifier.height(Spacing.sm))
                group.transactions.forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tx.description?.takeIf { it.isNotBlank() && it != tx.merchant } ?: tx.merchant ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                formatDateTime(tx.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            formatCurrency(tx.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(onClick = { pickerTarget = tx.id; pickerOpen = true }) {
                            Text("Tag", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        val target = pickerTarget
        CategoryPickerSheet(
            title   = if (target == null) "Assign all ${group.count} as…" else "Assign transaction",
            onPick  = { cat ->
                pickerOpen = false
                if (target == null) onAssignAll(cat) else onAssignOne(target, cat)
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionCard(
    tx: TransactionEntity,
    onCategorySelected: (String) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    var assignedCat by remember { mutableStateOf<String?>(null) }

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

    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(typeColor.copy(alpha = 0.13f), MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
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
                        Text("via $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(formatDateTime(tx.date), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    "${if (isIncome) "+" else if (isTransfer) "" else "-"}${formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick  = { pickerOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(assignedCat?.let { capitalize(it) } ?: "Assign category",
                    modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (pickerOpen) {
        CategoryPickerSheet(
            title     = "Pick a category",
            onPick    = { cat -> assignedCat = cat; pickerOpen = false; onCategorySelected(cat) },
            onDismiss = { pickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    title:     String,
    onPick:    (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(Spacing.lg),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = Spacing.base))
            CATEGORIZE_CATEGORIES.forEach { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        ) { onPick(cat) }
                        .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0x20 / 255f), MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(capitalize(cat).take(1), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(capitalize(cat), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
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
