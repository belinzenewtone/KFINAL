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
// LoanFormScreen — matches LoanFormScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoanFormScreen(
    loanId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form   by viewModel.loanForm.collectAsState()
    val isEdit = !loanId.isNullOrEmpty()

    LaunchedEffect(loanId) {
        viewModel.openLoanForm(loanId?.ifEmpty { null })
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "Add",
        title   = "Loan",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Text("Draw code", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value         = form.drawCode,
                onValueChange = { viewModel.updateLoanDrawCode(it) },
                label         = { Text("Fuliza draw code (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            Text("Draw amount", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value           = form.drawAmountKes,
                onValueChange   = { viewModel.updateLoanDrawAmount(it) },
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
                onClick  = {
                    // Save the loan — reuse goal-style insert via the planner DAO path.
                    viewModel.saveLoan { navController.popBackStack() }
                },
                enabled  = !form.isSaving && form.drawAmountKes.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update Loan" else "Save Loan")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
