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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.belinze.lifeos.ui.navigation.NavTo
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

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
        isLoaded = true
    }
    val context = LocalContext.current

    PageScaffold(
        title = "Transaction",
        onBack = { navController.popBackStack() },
        actions = {
            if (tx != null) {
                IconButton(onClick = {
                    val prefix = when (tx.transactionType) {
                        "income" -> "+"
                        "expense" -> "-"
                        else -> ""
                    }
                    val message = "${prefix}${formatCurrency(tx.amount)} ${tx.transactionType} to " +
                        "${tx.merchant ?: ""} on ${tx.date?.take(19) ?: ""}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                }
            }
        },
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
                .verticalScroll(rememberScrollState()),
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

            Button(
                onClick = { navController.navigate(NavTo.transactionForm(tx.id)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Edit Transaction")
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

private fun formatDetailDate(iso: String): String = try {
    LocalDateTime.parse(iso.take(19)).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
} catch (_: Exception) {
    iso.take(16)
}
