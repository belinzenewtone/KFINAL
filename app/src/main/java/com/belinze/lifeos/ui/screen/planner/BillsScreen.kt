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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
// BillsScreen — matches BillsScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BillsScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow = "Planner",
            title   = "Bills",
            onBack  = { navController.popBackStack() },
            scrollable = false,
        ) {
            if (state.bills.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                    contentAlignment = Alignment.Center) {
                    Text("No bills yet. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.bills, key = { it.id }) { bill ->
                        val paid = bill.paidStatus != 0
                        FrostCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = bill.title,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text  = "${bill.cycle ?: "monthly"} · due ${bill.nextDueDate?.take(10) ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text  = bill.amount?.let { formatCurrency(it) } ?: "—",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text  = if (paid) "Paid" else "Due",
                                        fontSize = 10.sp,
                                        color = if (paid) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    )
                                }
                            }
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick  = { viewModel.toggleBillPaid(bill.id) },
                                    modifier = Modifier.padding(end = Spacing.sm),
                                ) {
                                    Text(
                                        if (paid) "Mark unpaid" else "Mark paid",
                                        color = if (paid) MaterialTheme.colorScheme.onBackground.copy(0.55f)
                                                else Color(0xFF10B981),
                                    )
                                }
                                IconButton(onClick = { navController.navigate(NavTo.billForm(bill.id)) },
                                    modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit",
                                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteBill(bill.id) },
                                    modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                                        modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick        = { navController.navigate(NavTo.billForm()) },
            text           = { Text("Add Bill") },
            icon           = { Icon(Icons.Filled.Add, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.bottomNavSafeArea + Spacing.md),
        )
    }
}
