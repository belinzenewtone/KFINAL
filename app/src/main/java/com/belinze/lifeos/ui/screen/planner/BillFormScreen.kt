package com.belinze.lifeos.ui.screen.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.belinze.lifeos.viewmodel.PlannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

private val CYCLES = listOf("daily", "weekly", "monthly", "yearly", "one_time")

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

    // CC-3: success banner (outside PageScaffold so it overlays everything)
    TopBanner(
        visible       = successMsg != null,
        message       = successMsg ?: "",
        tone          = BannerTone.Success,
        onDismiss     = { successMsg = null },
        autoDismissMs = 2000,
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete bill?") },
            text  = { Text("This bill will be permanently removed.") },
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
                onClick = {
                    // CC-3: banner + delayed navigation
                    viewModel.saveBill {
                        successMsg = if (isEdit) "Bill updated" else "Bill added"
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
                        "Update Bill"
                    } else {
                        "Add Bill"
                    }
                )
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
