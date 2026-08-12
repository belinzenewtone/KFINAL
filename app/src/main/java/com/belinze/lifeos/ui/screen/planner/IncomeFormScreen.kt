package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.belinze.lifeos.viewmodel.PlannerViewModel

// ─────────────────────────────────────────────────────────────────────────────
// IncomeFormScreen — matches IncomeFormScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IncomeFormScreen(
    incomeId:      String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form   by viewModel.incomeForm.collectAsState()
    val isEdit = !incomeId.isNullOrEmpty()

    LaunchedEffect(incomeId) {
        viewModel.openIncomeForm(incomeId?.ifEmpty { null })
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "Add",
        title   = "Income",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Text("Source", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value         = form.source,
                onValueChange = { viewModel.updateIncomeSource(it) },
                label         = { Text("Income source") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            Text("Amount", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value           = form.amount,
                onValueChange   = { viewModel.updateIncomeAmount(it) },
                label           = { Text("Amount (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix          = { Text("KES ") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
            )

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick  = { viewModel.saveIncome { navController.popBackStack() } },
                enabled  = !form.isSaving && form.source.isNotBlank() && form.amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update Income" else "Save Income")
                }
            }

            if (isEdit && form.id != null) {
                TextButton(
                    onClick  = {
                        viewModel.deleteIncome(form.id!!)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Income", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
