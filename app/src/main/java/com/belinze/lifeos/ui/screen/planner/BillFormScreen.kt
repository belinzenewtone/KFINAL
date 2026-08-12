package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.TimeZone
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.PlannerViewModel

private val CYCLES = listOf("daily", "weekly", "monthly", "yearly", "one_time")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillFormScreen(
    billId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form by viewModel.billForm.collectAsState()
    val isEdit = !billId.isNullOrEmpty()

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
        actions = {
            if (isEdit) {
                IconButton(onClick = {
                    viewModel.deleteBill(form.id.orEmpty())
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
            OutlinedTextField(
                value = form.notes,
                onValueChange = { viewModel.updateBillNotes(it) },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Notes...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            var cycleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = cycleExpanded,
                onExpandedChange = { cycleExpanded = it },
            ) {
                OutlinedTextField(
                    value = form.frequency.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cycle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cycleExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = cycleExpanded,
                    onDismissRequest = { cycleExpanded = false },
                ) {
                    CYCLES.forEach { cycle ->
                        DropdownMenuItem(
                            text = { Text(cycle.replaceFirstChar { it.uppercase() }) },
                            onClick = { viewModel.updateBillFrequency(cycle); cycleExpanded = false },
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.updateBillPaid(!form.isPaid) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Paid: ${if (form.isPaid) "Yes" else "No"}")
            }
            OutlinedButton(
                onClick = { viewModel.updateBillActive(!form.isActive) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Active: ${if (form.isActive) "Yes" else "No"}")
            }

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.saveBill { navController.popBackStack() } },
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
                    Text(if (isEdit) "Update Bill" else "Add Bill")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
