package com.belinze.lifeos.ui.screen.finance

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButtonDefaults
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
import com.belinze.lifeos.ui.components.AppDropdownField
import com.belinze.lifeos.ui.components.AppPickerSheet
import com.belinze.lifeos.ui.components.AppSegmentedControl
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PickerOption
import com.belinze.lifeos.ui.components.SegmentOption
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.ui.theme.categoryColor
import com.belinze.lifeos.ui.theme.categoryIcon
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
    TransactionDetailDialog(
        transactionId = transactionId,
        onDismiss     = { navController.popBackStack() },
        viewModel     = viewModel,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// TransactionDetailDialog
//
// Full-screen dark-scrim overlay with a centred card — 1:1 with RFINAL's
// modal overlay (Stack navigation with semi-transparent background).
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("CyclomaticComplexMethod")
@Composable
fun TransactionDetailDialog(
    transactionId: String,
    onDismiss:     () -> Unit,
    viewModel:     TransactionViewModel,
) {
    val selectedTx by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val tx = selectedTx?.takeIf { it.id == transactionId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(transactionId) { viewModel.loadTransaction(transactionId) }

    // Ordinary screen content, deliberately NOT a Dialog. Compose Navigation already
    // replaces the previous destination, so the Dialog never produced RFINAL's
    // "previous screen visible behind" effect — it only hosted a second window, and
    // nesting a ModalBottomSheet (the Category/Status pickers below) inside a Dialog
    // is fragile. Same visual result, one less window.
    Box(modifier = Modifier.fillMaxSize()) {
        // BoxWithConstraints so the card can cap itself against the REAL available
        // height instead of a hard-coded 520dp: it grows to fit its content (so the
        // edit panel extends naturally, like RFINAL) and only starts scrolling once
        // it would otherwise run off the screen.
        BoxWithConstraints(
            modifier         = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val maxCardHeight = maxHeight * 0.9f
            Surface(
                modifier  = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxCardHeight)
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = {},
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                when {
                    tx == null -> {
                        Box(
                            modifier         = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    }
                    else -> {
                        val categoryKey = tx.category?.takeIf { it.isNotBlank() } ?: (tx.transactionType ?: "expense")
                        val catColor = categoryColor(categoryKey)
                        val amountColor = when (tx.transactionType) {
                            "income" -> Color(0xFF34D399)
                            "expense" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Column(
                            // Content-sized up to the cap above; this scroll only engages
                            // for genuinely tall content, so tapping Edit extends the card
                            // rather than cramming the panel into a fixed-height box.
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                        ) {
                            // Header block — icon chip + Merchant/Category/Amount label-value rows
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(40.dp)
                                        .background(catColor.copy(alpha = 0x20 / 255f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        categoryIcon(categoryKey),
                                        contentDescription = null,
                                        tint     = catColor,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailRow("Merchant", tx.merchant ?: "")
                                    DetailRow("Category", categoryKey.replaceFirstChar { it.uppercase() })
                                    DetailRow("Amount", formatCurrency(tx.amount), valueColor = amountColor)
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                color    = MaterialTheme.colorScheme.outlineVariant,
                            )

                            // Details — bare rows after divider, matching RFINAL's modal layout
                            DetailRow("Date", tx.date?.let { formatDetailDate(it) } ?: "")
                            tx.mpesaCode?.let { DetailRow("M-Pesa Code", it) }
                            if (tx.mpesaCode == null) tx.externalRef?.let { DetailRow("Reference", it) }
                            tx.description?.let { DetailRow("Description", it) }
                            tx.notes?.let { DetailRow("Notes", it) }
                            tx.balanceAfter?.let { DetailRow("Balance After", formatCurrency(it)) }
                            tx.fee?.let { DetailRow("Fee", formatCurrency(it)) }

                            if (isEditing) {
                                InlineEditPanel(
                                    viewModel     = viewModel,
                                    transactionId = transactionId,
                                    onCancel      = { isEditing = false },
                                    onSaved       = { isEditing = false },
                                )
                            } else {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    TextButton(
                                        onClick  = {
                                            val prefix = when (tx.transactionType) {
                                                "income"  -> "+"
                                                "expense" -> "-"
                                                else      -> ""
                                            }
                                            val msg = "${prefix}${formatCurrency(tx.amount)} ${tx.transactionType} " +
                                                "to ${tx.merchant ?: ""} on ${tx.date?.let { formatDetailDate(it) } ?: ""}"
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, msg)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share"))
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share")
                                    }
                                    TextButton(
                                        onClick  = { showDeleteDialog = true },
                                        colors   = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
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
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && tx != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete transaction") },
            text    = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.softDelete(tx.id)
                    showDeleteDialog = false
                    onDismiss()
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
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AppSegmentedControl(
                options     = TX_TYPES_DETAIL.map {
                    SegmentOption(key = it, label = it.replaceFirstChar { c -> c.uppercase() })
                },
                selectedKey = formState.transactionType,
                onSelect    = { viewModel.updateFormType(it) },
                modifier    = Modifier.fillMaxWidth(),
            )
            var catSheetOpen by remember { mutableStateOf(false) }
            AppDropdownField(
                label       = "Category",
                valueLabel  = formState.category.replaceFirstChar { it.uppercase() },
                onClick     = { catSheetOpen = true },
                modifier    = Modifier.fillMaxWidth(),
                leadingIcon = categoryIcon(formState.category),
                leadingTint = categoryColor(formState.category),
            )
            var statusSheetOpen by remember { mutableStateOf(false) }
            AppDropdownField(
                label      = "Status",
                valueLabel = formState.status.replaceFirstChar { it.uppercase() },
                onClick    = { statusSheetOpen = true },
                modifier   = Modifier.fillMaxWidth(),
            )
            // RN opens a SwipeableSheet option list for both pickers.
            AppPickerSheet(
                visible     = catSheetOpen,
                title       = "Category",
                options     = CATEGORIES_DETAIL.map { cat ->
                    PickerOption(
                        key   = cat,
                        label = cat.replaceFirstChar { it.uppercase() },
                        icon  = categoryIcon(cat),
                        tint  = categoryColor(cat),
                    )
                },
                selectedKey = formState.category,
                onSelect    = { viewModel.updateFormCategory(it) },
                onDismiss   = { catSheetOpen = false },
            )
            AppPickerSheet(
                visible     = statusSheetOpen,
                title       = "Status",
                options     = STATUSES_DETAIL.map { st ->
                    PickerOption(key = st, label = st.replaceFirstChar { it.uppercase() })
                },
                selectedKey = formState.status,
                onSelect    = { viewModel.updateFormStatus(it) },
                onDismiss   = { statusSheetOpen = false },
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                // RFINAL keeps "Cancel" on a single line (compact text button, flex 1).
                // A weighted 1:3 split squeezes it below its intrinsic width on narrow
                // screens, so the label breaks and renders vertically. Let Cancel size to
                // its content and give Save the remaining space instead.
                TextButton(
                    onClick        = onCancel,
                    // RFINAL: a flat text button whose label is onSurfaceVariant (not
                    // primary), at the theme's 16dp roundness — not an M3 pill.
                    shape          = RoundedCornerShape(16.dp),
                    colors         = TextButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                ) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
                Button(
                    onClick  = {
                        viewModel.saveForm { onSaved(); viewModel.loadTransaction(transactionId) }
                    },
                    enabled  = !formState.isSaving,
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
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
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
    }
}

private val TX_TYPES_DETAIL   = listOf("expense", "income", "transfer")
private val STATUSES_DETAIL   = listOf("completed", "pending", "failed", "reversed")
private val CATEGORIES_DETAIL = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
    "housing", "personal_care", "subscriptions", "fuel", "loans", "insurance",
    "miscellaneous", "income", "uncategorized",
)

private fun formatDetailDate(iso: String): String = try {
    LocalDateTime.parse(iso.take(19)).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
} catch (_: Exception) {
    iso.take(16)
}
