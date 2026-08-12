package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.FulizaLoanEntity
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SUCCESS = Color(0xFF7BC47B)
private val WARNING = Color(0xFFF5CB5C)

@Composable
fun LoansScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var payLoanId by remember { mutableStateOf<String?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var banner by remember { mutableStateOf<String?>(null) }

    val openLoans = state.loans.filter { it.status == "active" }
    val closedLoans = state.loans.filter { it.status != "active" }.take(10)
    val netOutstanding = openLoans.sumOf { it.drawAmountKes - it.totalRepaidKes }

    PageScaffold(
        eyebrow = "Finance Tools",
        title = "Loans & Fuliza",
        subtitle = "Track outstanding draws and repayment history",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.loanForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add loan", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        TopBanner(
            visible = banner != null,
            message = banner ?: "",
            tone = BannerTone.Success,
            onDismiss = { banner = null },
            autoDismissMs = 2500,
        )

        if (state.loans.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No Fuliza history yet", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Import M-Pesa messages from Finance to track Fuliza draws and repayments automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    GlassCard(
                        variant = com.belinze.lifeos.ui.components.GlassCardVariant.Elevated,
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
                    ) {
                        Text("Net Outstanding", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(formatCurrency(netOutstanding),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (netOutstanding > 0) WARNING else SUCCESS,
                            modifier = Modifier.padding(top = Spacing.xs))
                        Text(
                            if (netOutstanding <= 0) "All Fuliza draws are fully repaid."
                            else "${openLoans.size} open draw${if (openLoans.size == 1) "" else "s"}. Pay to avoid daily interest.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                if (openLoans.isNotEmpty()) {
                    item {
                        Text("Open Draws", style = MaterialTheme.typography.labelLarge,
                            color = WARNING, modifier = Modifier.padding(bottom = Spacing.sm))
                    }
                    items(openLoans, key = { it.id }) { loan ->
                        LoanCard(
                            loan = loan,
                            onEdit = { navController.navigate(NavTo.loanForm(loan.id)) },
                            onLogRepayment = { payLoanId = loan.id; payAmount = "" },
                            onMarkRepaid = { viewModel.markRepaid(loan.id); banner = "Loan marked as repaid" },
                        )
                    }
                }

                if (closedLoans.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(Spacing.base))
                        Text("Repaid", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.sm))
                    }
                    items(closedLoans, key = { it.id }) { loan ->
                        LoanCard(loan = loan, onEdit = { navController.navigate(NavTo.loanForm(loan.id)) })
                    }
                }

                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }
    }

    if (payLoanId != null) {
        val loan = state.loans.firstOrNull { it.id == payLoanId }
        if (loan != null) {
            AlertDialog(
                onDismissRequest = { payLoanId = null },
                title = { Text("Log repayment") },
                text = {
                    Column {
                        Text(
                            "Outstanding: ${formatCurrency(loan.drawAmountKes - loan.totalRepaidKes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedTextField(
                            value = payAmount,
                            onValueChange = { payAmount = it },
                            placeholder = { Text("Amount repaid") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.logRepayment(loan.id, payAmount.toDoubleOrNull() ?: 0.0) { ok ->
                            if (ok) banner = "Logged repayment"
                            payLoanId = null
                            payAmount = ""
                        }
                    }) {
                        Text("Log")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { payLoanId = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun LoanCard(
    loan: FulizaLoanEntity,
    onEdit: () -> Unit,
    onLogRepayment: (() -> Unit)? = null,
    onMarkRepaid: (() -> Unit)? = null,
) {
    val outstanding = loan.drawAmountKes - loan.totalRepaidKes
    val isClosed = loan.status != "active"

    GlassCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                Text("Draw: ${formatCurrency(loan.drawAmountKes)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(formatDate(loan.drawDate), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isClosed) "Repaid" else formatCurrency(outstanding),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isClosed) SUCCESS else WARNING,
                )
                Text(loan.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (loan.totalRepaidKes > 0) {
            Text("Repaid: ${formatCurrency(loan.totalRepaidKes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm))
        }
        if (!isClosed && (onLogRepayment != null || onMarkRepaid != null)) {
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                onLogRepayment?.let {
                    TextButton(onClick = it) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Log Repayment", color = MaterialTheme.colorScheme.primary)
                    }
                }
                onMarkRepaid?.let {
                    TextButton(onClick = it) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null,
                            tint = SUCCESS, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Mark Repaid", color = SUCCESS)
                    }
                }
            }
        }
    }
}

private fun formatDate(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
} catch (_: Exception) { iso?.take(10) ?: "" }
