package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.Haptics
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Matches BillsScreen.tsx's local SEMANTIC constant exactly.
private val SUCCESS = Color(0xFF4ADE80)
private val WARNING = Color(0xFFFBBF24)
private val DANGER = Color(0xFFF87171)

@Composable
fun BillsScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var banner by remember { mutableStateOf<String?>(null) }
    var billToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (billToDelete != null) {
        val (deleteId, deleteTitle) = billToDelete!!
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("Delete bill") },
            text  = { Text("Remove $deleteTitle?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBill(deleteId)
                    billToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) { Text("Cancel") }
            },
        )
    }

    val activeBills = remember(state.bills) { state.bills.filter { it.isActive != 0 } }

    PageScaffold(
        eyebrow = "Recurring Obligations",
        title = "Bills",
        subtitle = "${activeBills.size} active bill${if (activeBills.size == 1) "" else "s"}",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.billForm()) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add bill", tint = MaterialTheme.colorScheme.primary)
            }
        },
        topBanner = {
            TopBanner(
                visible = banner != null,
                message = banner ?: "",
                tone = BannerTone.Success,
                onDismiss = { banner = null },
                autoDismissMs = 2500,
            )
        },
    ) {
        if (state.bills.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Receipt, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No bills yet", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Add a recurring obligation to track due dates and payments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
            ) {
                items(state.bills, key = { it.id }) { bill ->
                    Box(modifier = Modifier.animateItem()) {
                        BillCard(
                            bill = bill,
                            onEdit = { navController.navigate(NavTo.billForm(bill.id)) },
                            onTogglePaid = {
                                if (bill.paidStatus == 0) Haptics.success() else Haptics.light()
                                viewModel.toggleBillPaid(bill.id)
                                banner = "${bill.title} marked as ${if (bill.paidStatus == 0) "paid" else "unpaid"}"
                            },
                            onDelete = { billToDelete = bill.id to bill.title },
                        )
                    }
                }
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
        bill.nextDueDate!!.take(10) < java.time.LocalDate.now().toString()
    val dueColor = if (isOverdue) DANGER else MaterialTheme.colorScheme.onSurfaceVariant

    GlassCard(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
        // Row 1: title | amount
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                bill.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(end = Spacing.sm),
            )
            Text(formatCurrency(bill.amount ?: 0.0), style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
        }

        // Row 2: chips | actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.weight(1f).padding(end = Spacing.sm),
            ) {
                bill.nextDueDate?.let {
                    BillChip("Due ${formatDate(it)}", dueColor)
                }
                BillChip(
                    formatCycleLabel(bill.cycle),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    borderColor = MaterialTheme.colorScheme.outline,
                    background = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            IconButton(onClick = onTogglePaid, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (paid) Icons.Outlined.Cancel else Icons.Outlined.CheckCircle,
                    contentDescription = if (paid) "Mark unpaid" else "Mark paid",
                    tint = if (paid) WARNING else SUCCESS,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BillChip(
    text: String,
    color: Color,
    borderColor: Color = color,
    background: Color = color.copy(alpha = 0x14 / 255f),
) {
    Row(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(9999.dp))
            .background(background, RoundedCornerShape(9999.dp))
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun formatCycleLabel(cycle: String?): String = when (cycle) {
    "one_time"  -> "One-time"
    "weekly"    -> "Weekly"
    "biweekly"  -> "Biweekly"
    "monthly"   -> "Monthly"
    "quarterly" -> "Quarterly"
    "yearly"    -> "Yearly"
    else        -> cycle?.replaceFirstChar { it.uppercase() } ?: ""
}

private fun formatDate(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}
