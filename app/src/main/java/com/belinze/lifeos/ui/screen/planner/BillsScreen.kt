package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
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
private val DANGER = Color(0xFFFF6B6B)

@Composable
fun BillsScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var banner by remember { mutableStateOf<String?>(null) }

    val activeBills = state.bills.filter { it.isActive != 0 }

    PageScaffold(
        eyebrow = "Recurring Obligations",
        title = "Bills",
        subtitle = "${activeBills.size} active bill${if (activeBills.size == 1) "" else "s"}",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.billForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add bill", tint = MaterialTheme.colorScheme.primary)
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

        if (state.bills.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Receipt, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No bills yet", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Add a recurring obligation to track due dates and payments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.bills, key = { it.id }) { bill ->
                    BillCard(
                        bill = bill,
                        onEdit = { navController.navigate(NavTo.billForm(bill.id)) },
                        onTogglePaid = {
                            viewModel.toggleBillPaid(bill.id)
                            banner = "${bill.title} marked as ${if (bill.paidStatus == 0) "paid" else "unpaid"}"
                        },
                        onDelete = { viewModel.deleteBill(bill.id) },
                    )
                }
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }
    }
}

@Composable
private fun BillCard(
    bill: com.belinze.lifeos.data.db.entity.BillEntity,
    onEdit: () -> Unit,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit,
) {
    val paid = bill.paidStatus != 0
    val isOverdue = !paid && bill.nextDueDate != null &&
        bill.nextDueDate!! < java.time.LocalDateTime.now().toString()
    val dueColor = if (isOverdue) DANGER else MaterialTheme.colorScheme.onSurfaceVariant

    GlassCard(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                Text(bill.title, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.padding(top = Spacing.xs),
                ) {
                    bill.nextDueDate?.let {
                        BillChip("Due ${formatDate(it)}", dueColor)
                    }
                    BillChip(bill.cycle ?: "", MaterialTheme.colorScheme.onSurfaceVariant)
                }
                bill.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                bill.amount?.let {
                    Text(formatCurrency(it), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                Text(if (paid) "Paid" else "Unpaid", style = MaterialTheme.typography.bodySmall,
                    color = if (paid) SUCCESS else WARNING)
            }
        }
        Spacer(Modifier.height(Spacing.base))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            TextButton(onClick = onTogglePaid) {
                Icon(
                    if (paid) Icons.Filled.CheckCircle else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (paid) WARNING else SUCCESS,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(if (paid) "Mark Unpaid" else "Mark Paid", color = if (paid) WARNING else SUCCESS)
            }
            TextButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun BillChip(text: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0x14 / 255f), RoundedCornerShape(9999.dp))
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun formatDate(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
} catch (_: Exception) { iso?.take(10) ?: "" }
