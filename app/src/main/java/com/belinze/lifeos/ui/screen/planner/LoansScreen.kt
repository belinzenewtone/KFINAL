package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.FulizaLoanEntity
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Matches LoansScreen.tsx's local SEMANTIC/STATUS_COLOR constants exactly.
private val SUCCESS = Color(0xFF4ADE80)
private val WARNING = Color(0xFFFBBF24)
private val DANGER = Color(0xFFF87171)

private val LOAN_STATUS_COLOR = mapOf(
    "active"    to WARNING,
    "repaid"    to SUCCESS,
    "defaulted" to DANGER,
)
private val LOAN_STATUS_LABEL = mapOf(
    "active"    to "Active",
    "repaid"    to "Repaid",
    "defaulted" to "Defaulted",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Reload whenever the screen resumes so status/repayment changes made in the
    // loan form (or by the Fuliza SMS pipeline) are reflected on return, instead
    // of showing the stale snapshot captured on first composition.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadAll()
        }
    }

    var payLoanId by remember { mutableStateOf<String?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var banner by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(banner) {
        banner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            banner = null
        }
    }

    val openLoans      = remember(state.loans) { state.loans.filter { it.status == "active" } }
    val closedLoans    = remember(state.loans) { state.loans.filter { it.status != "active" }.take(10) }
    val netOutstanding = remember(openLoans)   { openLoans.sumOf { it.drawAmountKes - it.totalRepaidKes } }

    Box(modifier = Modifier.fillMaxSize()) {
    PageScaffold(
        eyebrow = "Finance Tools",
        title = "Loans & Fuliza",
        subtitle = "Track outstanding draws and repayment history",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.loanForm()) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add loan", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        if (state.loans.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Payments, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No Fuliza history yet", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Import M-Pesa messages from Finance to track Fuliza draws and repayments automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
            ) {
                item {
                    GlassCard(
                        variant = com.belinze.lifeos.ui.components.GlassCardVariant.Elevated,
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
                    ) {
                        Text("Net Outstanding", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(formatCurrency(netOutstanding),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (netOutstanding > 0) WARNING else SUCCESS,
                            modifier = Modifier.padding(top = Spacing.xs))
                        Text(
                            if (netOutstanding <= 0) {
                                "All Fuliza draws are fully repaid."
                            } else {
                                "${openLoans.size} open draw${if (openLoans.size == 1) "" else "s"}. Pay to avoid daily interest."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                if (openLoans.isNotEmpty()) {
                    item {
                        Text("Open Draws", style = MaterialTheme.typography.labelLarge,
                            color = WARNING, modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm))
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
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant))
                        Spacer(Modifier.height(Spacing.base))
                        Text("Repaid", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.sm))
                    }
                    items(closedLoans, key = { it.id }) { loan ->
                        LoanCard(loan = loan, onEdit = { navController.navigate(NavTo.loanForm(loan.id)) })
                    }
                }

            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg),
    )
    } // Box

    if (payLoanId != null) {
        val loan = state.loans.firstOrNull { it.id == payLoanId }
        if (loan != null) {
            ModalBottomSheet(
                onDismissRequest = { payLoanId = null },
                sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                ) {
                    Text("Log repayment", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "Outstanding: ${formatCurrency(loan.drawAmountKes - loan.totalRepaidKes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value           = payAmount,
                        onValueChange   = { payAmount = it },
                        placeholder     = { Text("Amount repaid") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { payLoanId = null }) { Text("Cancel") }
                        TextButton(onClick = {
                            val delta = payAmount.toDoubleOrNull() ?: 0.0
                            if (delta > 0) {
                                val outstanding = (loan.drawAmountKes - loan.totalRepaidKes).coerceAtLeast(0.0)
                                val applied = delta.coerceAtMost(outstanding)
                                val fullyPaid = (loan.totalRepaidKes + applied) >= loan.drawAmountKes - 0.005
                                viewModel.logRepayment(loan.id, delta) { ok ->
                                    if (ok) {
                                        banner = if (fullyPaid) {
                                            "Loan fully repaid 🎉"
                                        } else {
                                            "Logged ${formatCurrency(applied)} repayment"
                                        }
                                    }
                                    payLoanId = null
                                    payAmount = ""
                                }
                            } else {
                                banner = "Enter a positive repayment amount"
                                payLoanId = null
                                payAmount = ""
                            }
                        }) { Text("Log") }
                    }
                    Spacer(Modifier.height(Spacing.xl))
                }
            }
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
    val outstanding = (loan.drawAmountKes - loan.totalRepaidKes).coerceAtLeast(0.0)
    val isActive = loan.status == "active"
    val percent = if (loan.drawAmountKes > 0) {
        (loan.totalRepaidKes / loan.drawAmountKes * 100).coerceIn(0.0, 100.0)
    } else {
        0.0
    }
    val statusColor = LOAN_STATUS_COLOR[loan.status] ?: WARNING
    val statusLabel = LOAN_STATUS_LABEL[loan.status] ?: loan.status.replaceFirstChar { it.uppercase() }

    GlassCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
    ) {
        // Row 1: draw label | amount
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                loan.drawCode?.takeIf { it.isNotBlank() } ?: "Draw · ${formatDateShort(loan.drawDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(end = Spacing.sm),
            )
            Text(
                if (isActive) formatCurrency(outstanding) else formatCurrency(loan.drawAmountKes),
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
            )
        }

        // Row 2: draw date | status badge
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatDate(loan.drawDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0x20 / 255f), MaterialTheme.shapes.large)
                    .padding(horizontal = Spacing.sm, vertical = 2.dp),
            ) {
                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
        }

        // Row 3: progress bar
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth((percent / 100.0).toFloat()).height(6.dp)
                    .background(statusColor, androidx.compose.foundation.shape.CircleShape),
            )
        }
        Spacer(Modifier.height(Spacing.sm))

        // Row 4: repaid info | actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (loan.status) {
                    "repaid" -> {
                        val when_ = loan.lastRepaymentDate?.let { " · ${formatDateShort(it)}" } ?: ""
                        "Fully repaid$when_"
                    }
                    "defaulted" -> "Outstanding ${formatCurrency(outstanding)} unpaid"
                    else -> "Repaid ${formatCurrency(loan.totalRepaidKes)} · ${percent.toInt()}%"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (isActive) {
                onLogRepayment?.let {
                    IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Log repayment",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                onMarkRepaid?.let {
                    IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Mark repaid",
                            tint = SUCCESS, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun formatDate(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}

private fun formatDateShort(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("MMM d"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}
