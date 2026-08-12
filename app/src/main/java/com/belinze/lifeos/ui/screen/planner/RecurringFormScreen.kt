package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.PlannerViewModel

// ─────────────────────────────────────────────────────────────────────────────
// RecurringFormScreen — matches RecurringFormScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

private val FREQUENCIES = listOf("daily", "weekly", "monthly", "yearly")

private val CATEGORIES = listOf(
    "food", "transport", "entertainment", "utilities", "health",
    "shopping", "education", "housing", "savings", "personal", "other",
)

@Composable
fun RecurringFormScreen(
    ruleId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form   by viewModel.recurringForm.collectAsState()
    val isEdit = !ruleId.isNullOrEmpty()

    LaunchedEffect(ruleId) {
        viewModel.openRecurringForm(ruleId?.ifEmpty { null })
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "Add",
        title   = "Recurring",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Text("Name", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value         = form.name,
                onValueChange = { viewModel.updateRecurringName(it) },
                label         = { Text("Rule name") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            Text("Amount", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value           = form.amount,
                onValueChange   = { viewModel.updateRecurringAmount(it) },
                label           = { Text("Amount (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix          = { Text("KES ") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
            )

            Text("Frequency", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FREQUENCIES.forEach { f ->
                    FilterChip(
                        selected = form.frequency == f,
                        onClick  = { viewModel.updateRecurringFrequency(f) },
                        label    = { Text(f.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Text("Category", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CATEGORIES.take(6).forEach { cat ->
                    FilterChip(
                        selected = form.category == cat,
                        onClick  = { viewModel.updateRecurringCategory(cat) },
                        label    = { Text(cat.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Enabled", fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
                Switch(checked = form.enabled, onCheckedChange = { viewModel.updateRecurringEnabled(it) })
            }

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick  = { viewModel.saveRecurring { navController.popBackStack() } },
                enabled  = !form.isSaving && form.name.isNotBlank() && form.amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update Rule" else "Save Rule")
                }
            }

            if (isEdit && form.id != null) {
                TextButton(
                    onClick  = {
                        viewModel.deleteRule(form.id!!)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Rule", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
