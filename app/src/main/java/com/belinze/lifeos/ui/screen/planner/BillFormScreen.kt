package com.belinze.lifeos.ui.screen.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.Haptics
import com.belinze.lifeos.viewmodel.PlannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

private val CYCLES = listOf("daily", "weekly", "monthly", "yearly", "one_time")
private val CYCLE_LABELS = mapOf(
    "daily"    to "Daily",
    "weekly"   to "Weekly",
    "monthly"  to "Monthly",
    "yearly"   to "Yearly",
    "one_time" to "One-time",
)
private val BILL_SUCCESS = androidx.compose.ui.graphics.Color(0xFF4ADE80)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillFormScreen(
    billId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form by viewModel.billForm.collectAsStateWithLifecycle()
    val isEdit = !billId.isNullOrEmpty()
    val scope = rememberCoroutineScope()
    // CC-2: delete confirmation
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // CC-3: success banner
    var successMsg by remember { mutableStateOf<String?>(null) }
    // CC-4: fade-in entry animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "formFadeIn",
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete bill") },
            text  = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteBill(form.id.orEmpty())
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = form.nextDueDate.takeIf { it.isNotBlank() }?.take(10)?.let {
            runCatching {
                java.time.LocalDate.parse(it)
                    .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        } ?: System.currentTimeMillis(),
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val dateStr = "%04d-%02d-%02d".format(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                        )
                        viewModel.updateBillNextDue(dateStr)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(billId) {
        viewModel.openBillForm(billId?.ifEmpty { null })
    }

    PageScaffold(
        title = if (isEdit) "Edit Bill" else "Add Bill",
        onBack = { navController.popBackStack() },
        topBanner = {
            TopBanner(
                visible       = successMsg != null,
                message       = successMsg ?: "",
                tone          = BannerTone.Success,
                onDismiss     = { successMsg = null },
                autoDismissMs = 2000,
            )
        },
        actions = {
            if (isEdit) {
                // CC-2: confirmation before delete
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        // CC-4: fade-in entry animation
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { viewModel.updateBillName(it) },
                label = { Text("Title") },
                placeholder = { Text("e.g. Rent") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.amount,
                onValueChange = { viewModel.updateBillAmount(it) },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = form.nextDueDate.take(10),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Next due date") },
                    trailingIcon = {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showDatePicker = true },
                )
            }
            OutlinedTextField(
                value = form.notes,
                onValueChange = { viewModel.updateBillNotes(it) },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Notes...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            // Matches BillFormScreen.tsx pillRow: three equal-width pill columns
            // (Cycle / Paid / Status), each a label above a tappable pill.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.base),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                BillPillColumn(
                    label = "Cycle",
                    text  = CYCLE_LABELS[form.frequency] ?: form.frequency.replaceFirstChar { it.uppercase() },
                    selected = true,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        val next = CYCLES[(CYCLES.indexOf(form.frequency).let { if (it < 0) 0 else it } + 1) % CYCLES.size]
                        viewModel.updateBillFrequency(next)
                    },
                    modifier = Modifier.weight(1f),
                )
                BillPillColumn(
                    label = "Paid",
                    text  = if (form.isPaid) "Paid" else "Unpaid",
                    selected = form.isPaid,
                    selectedColor = BILL_SUCCESS,
                    onClick = { viewModel.updateBillPaid(!form.isPaid) },
                    modifier = Modifier.weight(1f),
                )
                BillPillColumn(
                    label = "Status",
                    text  = if (form.isActive) "Active" else "Inactive",
                    selected = form.isActive,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.updateBillActive(!form.isActive) },
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = {
                    Haptics.light()
                    viewModel.saveBill {
                        successMsg = if (isEdit) "Bill updated" else "Bill added"
                        scope.launch {
                            delay(400)
                            navController.popBackStack()
                        }
                    }
                },
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.base),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Saving…")
                } else {
                    Text(if (isEdit) "Update Bill" else "Add Bill")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun BillPillColumn(
    label: String,
    text: String,
    selected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) selectedColor else MaterialTheme.colorScheme.outline
    val contentColor = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(borderColor.copy(alpha = 0x22 / 255f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(),
                    onClick = onClick,
                )
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor, maxLines = 1)
        }
    }
}
