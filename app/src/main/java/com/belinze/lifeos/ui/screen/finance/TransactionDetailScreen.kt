package com.belinze.lifeos.ui.screen.finance

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.rememberFormFadeIn
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.ui.theme.categoryColor
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.TransactionViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    navController:  NavHostController,
    viewModel:      TransactionViewModel = hiltViewModel(),
) {
    val selectedTx by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    // With Paging 3 the Finance screen no longer holds a flat list, so we load
    // by ID on entry. Track whether the load has resolved so we don't flash
    // "not found" while the DB query is in-flight.
    var isLoaded by remember { mutableStateOf(false) }
    val tx = selectedTx?.takeIf { it.id == transactionId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
        isLoaded = true
    }
    val context = LocalContext.current

    PageScaffold(
        title = "Transaction",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        when {
            !isLoaded || (tx == null && selectedTx == null) -> {
                // Still loading — show spinner instead of "not found"
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@PageScaffold
            }
            tx == null -> {
                Text("Transaction not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@PageScaffold
            }
        }

        val categoryColor = categoryColor(tx.category ?: "")
        val amountColor = when (tx.transactionType) {
            "income" -> Color(0xFF34D399)
            "expense" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .then(rememberFormFadeIn()),
        ) {
            // Hero card
            GlassCard(modifier = Modifier.padding(bottom = Spacing.base)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(categoryColor.copy(alpha = 0x20 / 255f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            when (tx.transactionType) {
                                "income"   -> Icons.Outlined.ArrowDownward
                                "transfer" -> Icons.Outlined.SwapHoriz
                                else       -> Icons.Outlined.ArrowUpward
                            },
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.height(Spacing.base))
                    Text(
                        tx.merchant ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${tx.category ?: "uncategorized"} · ${tx.transactionType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.base))
                    Text(
                        formatCurrency(tx.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        color = amountColor,
                    )
                }
            }

            // Details card
            GlassCard(modifier = Modifier.padding(bottom = Spacing.base)) {
                DetailRow("Date", tx.date?.let { formatDetailDate(it) } ?: "")
                DetailRow("Status", tx.status)
                tx.mpesaCode?.let { DetailRow("M-Pesa Code", it) }
                if (tx.mpesaCode == null) tx.externalRef?.let { DetailRow("Reference", it) }
                tx.description?.let { DetailRow("Description", it) }
                tx.notes?.let { DetailRow("Notes", it) }
                tx.balanceAfter?.let { DetailRow("Balance After", formatCurrency(it)) }
                tx.fee?.let { DetailRow("Fee", formatCurrency(it)) }
            }

            if (isEditing) {
                InlineEditPanel(
                    viewModel     = viewModel,
                    transactionId = transactionId,
                    onCancel      = { isEditing = false },
                    onSaved       = { isEditing = false },
                )
            } else {
                // Actions row — Share | Delete | Edit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    TextButton(
                        onClick = {
                            val prefix = when (tx.transactionType) {
                                "income" -> "+"; "expense" -> "-"; else -> ""
                            }
                            val msg = "${prefix}${formatCurrency(tx.amount)} ${tx.transactionType} " +
                                "to ${tx.merchant ?: ""} on ${tx.date?.let { formatDetailDate(it) } ?: ""}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                    Button(
                        onClick = {
                            viewModel.openForm(tx.id)
                            isEditing = true
                        },
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("Edit") }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }

    if (showDeleteDialog && tx != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.softDelete(tx.id)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun InlineEditPanel(
    viewModel:     TransactionViewModel,
    transactionId: String,
    onCancel:      () -> Unit,
    onSaved:       () -> Unit,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    GlassCard(modifier = Modifier.padding(bottom = Spacing.base)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TX_TYPES_DETAIL.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = formState.transactionType == type,
                        onClick  = { viewModel.updateFormType(type) },
                        shape    = SegmentedButtonDefaults.itemShape(idx, TX_TYPES_DETAIL.size),
                        label    = { Text(type.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(
                    value = formState.category.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    CATEGORIES_DETAIL.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.replaceFirstChar { it.uppercase() }) },
                            onClick = { viewModel.updateFormCategory(cat); catExpanded = false },
                        )
                    }
                }
            }
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(
                    value = formState.status.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    STATUSES_DETAIL.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.replaceFirstChar { it.uppercase() }) },
                            onClick = { viewModel.updateFormStatus(status); statusExpanded = false },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick  = { viewModel.saveForm { onSaved(); viewModel.loadTransaction(transactionId) } },
                    shape    = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = Spacing.base),
        )
    }
}

private val TX_TYPES_DETAIL   = listOf("expense", "income", "transfer")
private val STATUSES_DETAIL   = listOf("completed", "pending", "failed", "reversed")
private val CATEGORIES_DETAIL = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
    "housing", "personal_care", "subscriptions", "miscellaneous",
    "income", "uncategorized",
)

private fun formatDetailDate(iso: String): String = try {
    LocalDateTime.parse(iso.take(19)).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
} catch (_: Exception) {
    iso.take(16)
}
