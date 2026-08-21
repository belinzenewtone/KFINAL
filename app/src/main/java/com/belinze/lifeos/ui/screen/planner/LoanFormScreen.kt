package com.belinze.lifeos.ui.screen.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

private val LOAN_STATUSES = listOf("active", "repaid", "defaulted")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormScreen(
    loanId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form by viewModel.loanForm.collectAsStateWithLifecycle()
    val isEdit = !loanId.isNullOrEmpty()
    val scope = rememberCoroutineScope()
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

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = form.drawDate.takeIf { it.isNotBlank() }?.take(10)?.let {
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
                        viewModel.updateLoanDrawDate(dateStr)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(loanId) {
        viewModel.openLoanForm(loanId?.ifEmpty { null })
    }

    // CC-2: delete confirmation
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { androidx.compose.material3.Text("Delete loan?") },
            text  = { androidx.compose.material3.Text("This loan record will be permanently removed.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteLoan(form.id.orEmpty())
                    navController.popBackStack()
                }) { androidx.compose.material3.Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            },
        )
    }

    PageScaffold(
        title = if (isEdit) "Edit Loan" else "Add Loan",
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
                value = form.drawCode,
                onValueChange = { viewModel.updateLoanDrawCode(it) },
                label = { Text("Loan name / code") },
                placeholder = { Text("e.g. Fuliza draw") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.drawAmountKes,
                onValueChange = { viewModel.updateLoanDrawAmount(it) },
                label = { Text("Draw amount") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = form.drawDate.take(10),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Draw date") },
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

            // LF-1: Total repaid (read-only)
            if (isEdit) {
                OutlinedTextField(
                    value = formatCurrency(form.totalRepaidKes),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Total repaid") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // LF-2: Last repayment date (read-only)
            val lastRepayDate = form.lastRepaymentDate
            if (isEdit && lastRepayDate != null) {
                OutlinedTextField(
                    value = lastRepayDate.take(10),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Last repayment date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text("Status", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LOAN_STATUSES.forEach { status ->
                    FilterChip(
                        selected = form.status == status,
                        onClick = { viewModel.updateLoanStatus(status) },
                        label = { Text(status.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    // CC-3: banner + delayed navigation
                    viewModel.saveLoan {
                        successMsg = if (isEdit) "Loan updated" else "Loan added"
                        scope.launch {
                            delay(1200)
                            navController.popBackStack()
                        }
                    }
                },
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            ) {
                Text(
                    if (form.isSaving) {
                        "Saving…"
                    } else if (isEdit) {
                        "Update Loan"
                    } else {
                        "Add Loan"
                    }
                )
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
