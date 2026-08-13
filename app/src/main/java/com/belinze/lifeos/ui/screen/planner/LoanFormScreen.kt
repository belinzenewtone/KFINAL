package com.belinze.lifeos.ui.screen.planner

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.PlannerViewModel

private val LOAN_STATUSES = listOf("active", "repaid", "defaulted")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormScreen(
    loanId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form by viewModel.loanForm.collectAsState()
    val isEdit = !loanId.isNullOrEmpty()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

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

    PageScaffold(
        title = if (isEdit) "Edit Loan" else "Add Loan",
        onBack = { navController.popBackStack() },
        actions = {
            if (isEdit) {
                IconButton(onClick = {
                    viewModel.deleteLoan(form.id.orEmpty())
                    navController.popBackStack()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date",
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
                onClick = { viewModel.saveLoan { navController.popBackStack() } },
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (isEdit) "Update Loan" else "Add Loan")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
