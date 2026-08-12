package com.belinze.lifeos.ui.screen.planner

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel

// ─────────────────────────────────────────────────────────────────────────────
// LoansScreen — matches LoansScreen.tsx (Fuliza / loans)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoansScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow = "Planner",
            title   = "Loans / Fuliza",
            onBack  = { navController.popBackStack() },
            scrollable = false,
        ) {
            // Summary
            if (state.totalActiveLoans > 0) {
                FrostCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
                    Text("Outstanding", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    Text(formatCurrency(state.totalActiveLoans),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            }

            if (state.loans.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                    contentAlignment = Alignment.Center) {
                    Text("No loans yet.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.loans, key = { it.id }) { loan ->
                        val outstanding = loan.drawAmountKes - loan.totalRepaidKes
                        FrostCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text       = "Draw ${loan.drawCode ?: loan.id.take(6)}",
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text  = loan.status,
                                        fontSize = 10.sp,
                                        color = if (loan.status == "active") Color(0xFFF59E0B)
                                                else MaterialTheme.colorScheme.onBackground.copy(0.45f),
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Draw: ${formatCurrency(loan.drawAmountKes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
                                    Text("Owing: ${formatCurrency(outstanding)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (outstanding > 0) MaterialTheme.colorScheme.onBackground.copy(0.70f)
                                                else Color(0xFF10B981))
                                }
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    IconButton(onClick = { navController.navigate(NavTo.loanForm(loan.id)) },
                                        modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit",
                                            modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.deleteLoan(loan.id) },
                                        modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete",
                                            modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                }
            }
        }
    }
}
