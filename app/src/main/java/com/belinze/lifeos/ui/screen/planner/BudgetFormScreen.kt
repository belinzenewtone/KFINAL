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
import com.belinze.lifeos.viewmodel.BudgetViewModel

// ─────────────────────────────────────────────────────────────────────────────
// BudgetFormScreen — matches BudgetFormScreen.tsx
//
// Fields: category, limit (KES), period (monthly/weekly/yearly), notes
// ─────────────────────────────────────────────────────────────────────────────

private val CATEGORIES = listOf(
    "food", "transport", "entertainment", "utilities", "health",
    "shopping", "education", "housing", "savings", "personal", "other",
)

private val PERIODS = listOf("monthly", "weekly", "yearly")

@Composable
fun BudgetFormScreen(
    budgetId:      String?,
    navController: NavHostController,
    viewModel:     BudgetViewModel = hiltViewModel(),
) {
    val form   by viewModel.formState.collectAsState()
    val isEdit = !budgetId.isNullOrEmpty()

    LaunchedEffect(budgetId) {
        viewModel.openForm(budgetId?.ifEmpty { null })
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "Add",
        title   = "Budget",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {

            // ── Category ──────────────────────────────────────────────────────
            Text("Category", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            // Scrollable chip row
            Column {
                val rows = CATEGORIES.chunked(4)
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = form.category == cat,
                                onClick  = { viewModel.updateCategory(cat) },
                                label    = { Text(cat.replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                }
            }

            // ── Limit ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = form.limitAmount,
                onValueChange = { viewModel.updateLimitAmount(it) },
                label         = { Text("Budget limit (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                prefix        = { Text("KES ") },
            )

            // ── Period ────────────────────────────────────────────────────────
            Text("Period", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PERIODS.forEach { p ->
                    FilterChip(
                        selected = form.period == p,
                        onClick  = { viewModel.updatePeriod(p) },
                        label    = { Text(p.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Save ──────────────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.saveForm { navController.popBackStack() } },
                enabled  = !form.isSaving && form.category.isNotBlank() && form.limitAmount.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update Budget" else "Save Budget")
                }
            }

            // ── Delete (edit only) ────────────────────────────────────────────
            if (isEdit && form.id != null) {
                TextButton(
                    onClick  = {
                        viewModel.softDelete(form.id!!)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Budget", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
