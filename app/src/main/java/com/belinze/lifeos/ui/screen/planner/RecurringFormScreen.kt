package com.belinze.lifeos.ui.screen.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.DateField
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.PlannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

private val TYPES = listOf("expense", "income", "task")
private val CADENCES = listOf("hourly", "daily", "weekly", "biweekly", "mon_fri", "monthly", "yearly")

private val CADENCE_LABELS = mapOf(
    "hourly"    to "Hourly",
    "daily"     to "Daily",
    "weekly"    to "Weekly",
    "biweekly"  to "Biweekly",
    "mon_fri"   to "Mon–Fri",
    "monthly"   to "Monthly",
    "yearly"    to "Yearly",
)
private val CATEGORIES = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormScreen(
    ruleId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form by viewModel.recurringForm.collectAsState()
    val isEdit = !ruleId.isNullOrEmpty()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // RF-2: success banner state
    var successMsg by remember { mutableStateOf<String?>(null) }
    // RF-3: delete confirmation dialog
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // RF-5: validation error dialog
    var validationError by remember { mutableStateOf<String?>(null) }

    // RF-1: fade-in animation when form data loads
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "formFadeIn",
    )

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
                        viewModel.updateRecurringNextRun(dateStr)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(ruleId) {
        viewModel.openRecurringForm(ruleId?.ifEmpty { null })
    }

    // RF-3: delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete rule?") },
            text  = { Text("This recurring rule will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteRule(form.id.orEmpty())
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    // RF-5: validation error dialog
    if (validationError != null) {
        AlertDialog(
            onDismissRequest = { validationError = null },
            title = { Text("Missing fields") },
            text  = { Text(validationError!!) },
            confirmButton = {
                TextButton(onClick = { validationError = null }) { Text("OK") }
            },
        )
    }

    // RF-2: success banner
    TopBanner(
        visible       = successMsg != null,
        message       = successMsg ?: "",
        tone          = BannerTone.Success,
        onDismiss     = { successMsg = null },
        autoDismissMs = 2000,
    )

    PageScaffold(
        title = if (isEdit) "Edit Recurring Rule" else "Add Recurring Rule",
        onBack = { navController.popBackStack() },
        actions = {
            if (isEdit) {
                // RF-3: show confirmation dialog instead of deleting immediately
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        // RF-1: apply fade-in alpha to entire form content
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { viewModel.updateRecurringName(it) },
                label = { Text("Title") },
                placeholder = { Text("e.g. Netflix subscription") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.amount,
                onValueChange = { viewModel.updateRecurringAmount(it) },
                label = { Text("Amount (optional)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // RF-10: DateField widget for next run date
            DateField(
                label   = "Next run date",
                value   = form.nextRunAt.take(10),
                onClick = { showDatePicker = true },
            )

            // RF-7: type selector → dropdown (matches RN Dropdown component)
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
            ) {
                OutlinedTextField(
                    value = form.type.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                ) {
                    TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.replaceFirstChar { it.uppercase() }) },
                            onClick = { viewModel.updateRecurringType(type); typeExpanded = false },
                        )
                    }
                }
            }

            var cadenceExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = cadenceExpanded,
                onExpandedChange = { cadenceExpanded = it },
            ) {
                OutlinedTextField(
                    value = form.frequency.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cadence") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cadenceExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = cadenceExpanded,
                    onDismissRequest = { cadenceExpanded = false },
                ) {
                    CADENCES.forEach { cadence ->
                        DropdownMenuItem(
                            text = { Text(CADENCE_LABELS[cadence] ?: cadence.replaceFirstChar { it.uppercase() }) },
                            onClick = { viewModel.updateRecurringFrequency(cadence); cadenceExpanded = false },
                        )
                    }
                }
            }

            if (form.type == "expense") {
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = form.category.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.replaceFirstChar { it.uppercase() }) },
                                onClick = { viewModel.updateRecurringCategory(cat); categoryExpanded = false },
                            )
                        }
                    }
                }
            }

            // RF-8: color-changing status button — green when active, surfaceVariant when paused
            Button(
                onClick = { viewModel.updateRecurringEnabled(!form.enabled) },
                modifier = Modifier.fillMaxWidth(),
                colors = if (form.enabled) {
                    ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF22C55E),
                        contentColor   = androidx.compose.ui.graphics.Color.White,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) {
                Text(if (form.enabled) "Active" else "Paused")
            }

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    // RF-5: validation before save
                    if (form.name.isBlank()) {
                        validationError = "Please enter a title for this recurring rule."
                        return@Button
                    }
                    if (form.nextRunAt.isBlank()) {
                        validationError = "Please select a next run date."
                        return@Button
                    }
                    // RF-4: haptic feedback on save
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // RF-2: show success banner then pop
                    viewModel.saveRecurring {
                        successMsg = if (isEdit) "Rule updated" else "Rule added"
                        scope.launch {
                            delay(1200)
                            navController.popBackStack()
                        }
                    }
                },
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            ) {
                // RF-11: show "Saving…" text instead of spinner inside button
                Text(
                    if (form.isSaving) {
                        "Saving…"
                    } else if (isEdit) {
                        "Update Rule"
                    } else {
                        "Add Rule"
                    }
                )
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
