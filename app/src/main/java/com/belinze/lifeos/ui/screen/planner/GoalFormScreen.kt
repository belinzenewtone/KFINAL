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

private val GOAL_STATUSES = listOf("active", "completed", "archived")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(
    goalId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form by viewModel.goalForm.collectAsStateWithLifecycle()
    val isEdit = !goalId.isNullOrEmpty()
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

    TopBanner(
        visible       = successMsg != null,
        message       = successMsg ?: "",
        tone          = BannerTone.Success,
        onDismiss     = { successMsg = null },
        autoDismissMs = 2000,
    )

    var showDeadlinePicker by remember { mutableStateOf(false) }
    val deadlinePickerState = rememberDatePickerState(
        initialSelectedDateMillis = form.deadline?.takeIf { it.isNotBlank() }?.take(10)?.let {
            runCatching {
                java.time.LocalDate.parse(it)
                    .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        } ?: System.currentTimeMillis(),
    )

    if (showDeadlinePicker) {
        DatePickerDialog(
            onDismissRequest = { showDeadlinePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeadlinePicker = false
                    deadlinePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val dateStr = "%04d-%02d-%02d".format(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                        )
                        viewModel.updateGoalDeadline(dateStr)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = deadlinePickerState) }
    }

    LaunchedEffect(goalId) {
        viewModel.openGoalForm(goalId?.ifEmpty { null })
    }

    // CC-2: delete confirmation
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { androidx.compose.material3.Text("Delete goal?") },
            text  = { androidx.compose.material3.Text("This goal will be permanently removed.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteGoal(form.id.orEmpty())
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
        title = if (isEdit) "Edit Goal" else "Add Goal",
        onBack = { navController.popBackStack() },
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
                value = form.name,
                onValueChange = { viewModel.updateGoalName(it) },
                label = { Text("Title") },
                placeholder = { Text("e.g. Emergency fund") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = { viewModel.updateGoalDescription(it) },
                label = { Text("Description (optional)") },
                placeholder = { Text("Notes...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.targetAmount,
                onValueChange = { viewModel.updateGoalTarget(it) },
                label = { Text("Target value") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.savedAmount,
                onValueChange = { viewModel.updateGoalSaved(it) },
                label = { Text("Current value") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.unit,
                onValueChange = { viewModel.updateGoalUnit(it) },
                label = { Text("Unit (optional)") },
                placeholder = { Text("e.g. KES, km") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = form.deadline?.take(10) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deadline (optional)") },
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
                        ) { showDeadlinePicker = true },
                )
            }

            Text("Status", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GOAL_STATUSES.forEach { status ->
                    FilterChip(
                        selected = form.status == status,
                        onClick = { viewModel.updateGoalStatus(status) },
                        label = { Text(status.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    // CC-3: success banner + delayed navigation
                    viewModel.saveGoal {
                        successMsg = if (isEdit) "Goal updated" else "Goal added"
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
                        "Update Goal"
                    } else {
                        "Add Goal"
                    }
                )
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
