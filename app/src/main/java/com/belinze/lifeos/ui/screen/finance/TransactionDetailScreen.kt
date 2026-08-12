package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.TransactionViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TransactionDetailScreen(
    transactionId:  String,
    navController:  NavHostController,
    viewModel:      TransactionViewModel = hiltViewModel(),
) {
    val state  by viewModel.uiState.collectAsState()
    val tx     = state.transactions.firstOrNull { it.id == transactionId }

    var showDeleteDialog by remember { mutableStateOf(false) }

    PageScaffold(
        eyebrow  = "Transaction",
        title    = tx?.merchant?.ifBlank { "Detail" } ?: "Detail",
        onBack   = { navController.popBackStack() },
        actions  = {
            if (tx != null) {
                Icon(
                    imageVector        = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    modifier           = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(0.70f),
                )
            }
            if (tx != null) {
                Icon(
                    imageVector        = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    modifier           = Modifier.size(22.dp),
                    tint               = Color(0xFFEF4444),
                )
            }
        },
    ) {
        if (tx == null) {
            Text("Transaction not found.", color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            return@PageScaffold
        }

        val isCredit = tx.transactionType == "income"

        // ── Amount hero ───────────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Type icon
            Box(
                modifier         = Modifier
                    .size(56.dp)
                    .background(if (isCredit) Color(0xFF10B981).copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = if (isCredit) Icons.Filled.ArrowDownward
                                        else if (tx.transactionType == "transfer") Icons.Filled.SwapHoriz
                                        else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint               = if (isCredit) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(24.dp),
                )
            }
            Text(
                text       = "${if (isCredit) "+" else "-"}${formatCurrency(tx.amount)}",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color      = if (isCredit) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground,
            )
        }

        // ── Details list ─────────────────────────────────────────────────
        listOf(
            "Merchant"    to (tx.merchant.ifBlank { "—" }),
            "Category"    to (tx.category.ifBlank { "Uncategorized" }),
            "Type"        to tx.transactionType.replaceFirstChar { it.uppercase() },
            "Status"      to tx.status.replaceFirstChar { it.uppercase() },
            "Date"        to parseTxDate(tx.date),
            "Reference"   to (tx.reference?.ifBlank { "—" } ?: "—"),
            "Notes"       to (tx.notes?.ifBlank { "—" } ?: "—"),
        ).forEach { (label, value) ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    style = MaterialTheme.typography.bodyMedium)
                Text(value, color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f),
                thickness = 0.5.dp,
            )
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    if (showDeleteDialog && tx != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete transaction?") },
            text             = { Text("This cannot be undone.") },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.softDelete(tx.id)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("Delete", color = Color(0xFFEF4444)) }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

private fun parseTxDate(iso: String?): String = try {
    val dt = LocalDateTime.parse(iso?.take(19) ?: return "—")
    dt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
} catch (_: Exception) { iso?.take(16) ?: "—" }
