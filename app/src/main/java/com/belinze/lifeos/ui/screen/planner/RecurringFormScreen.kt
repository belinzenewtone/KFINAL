package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.PlannerViewModel

private val TYPES = listOf("expense", "income", "task")
private val CADENCES = listOf("hourly", "daily", "weekly", "biweekly", "mon_fri", "monthly", "yearly")
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

    LaunchedEffect(ruleId) {
        viewModel.openRecurringForm(ruleId?.ifEmpty { null })
    }

    PageScaffold(
        title = if (isEdit) "Edit Recurring Rule" else "Add Recurring Rule",
        onBack = { navController.popBackStack() },
        actions = {
            if (isEdit) {
                IconButton(onClick = {
                    viewModel.deleteRule(form.id.orEmpty())
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
            OutlinedTextField(
                value = form.nextRunAt.take(10),
                onValueChange = { viewModel.updateRecurringNextRun(it) },
                label = { Text("Next run date") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Type", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TYPES.forEach { type ->
                    FilterChip(
                        selected = form.type == type,
                        onClick = { viewModel.updateRecurringType(type) },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Text("Cadence", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CADENCES.forEach { cadence ->
                    FilterChip(
                        selected = form.frequency == cadence,
                        onClick = { viewModel.updateRecurringFrequency(cadence) },
                        label = { Text(cadence.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            if (form.type == "expense") {
                Text("Category", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = form.category == cat,
                            onClick = { viewModel.updateRecurringCategory(cat) },
                            label = { Text(cat.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.updateRecurringEnabled(!form.enabled) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Status: ${if (form.enabled) "Active" else "Paused"}")
            }

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.saveRecurring { navController.popBackStack() } },
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
                    Text(if (isEdit) "Update Rule" else "Add Rule")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
