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
// GoalFormScreen — matches GoalFormScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GoalFormScreen(
    goalId:        String?,
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val form   by viewModel.goalForm.collectAsState()
    val isEdit = !goalId.isNullOrEmpty()

    LaunchedEffect(goalId) {
        viewModel.openGoalForm(goalId?.ifEmpty { null })
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "Add",
        title   = "Goal",
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
                onValueChange = { viewModel.updateGoalName(it) },
                label         = { Text("Goal name") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            Text("Target amount", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value           = form.targetAmount,
                onValueChange   = { viewModel.updateGoalTarget(it) },
                label           = { Text("Target (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix          = { Text("KES ") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
            )

            Text("Saved so far", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value           = form.savedAmount,
                onValueChange   = { viewModel.updateGoalSaved(it) },
                label           = { Text("Saved (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix          = { Text("KES ") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
            )

            Text("Target date", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            OutlinedTextField(
                value         = form.deadline ?: "",
                onValueChange = { viewModel.updateGoalDeadline(it.ifBlank { null }) },
                label         = { Text("YYYY-MM-DD (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick  = { viewModel.saveGoal { navController.popBackStack() } },
                enabled  = !form.isSaving && form.name.isNotBlank() && form.targetAmount.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update Goal" else "Save Goal")
                }
            }

            if (isEdit && form.id != null) {
                TextButton(
                    onClick  = {
                        viewModel.deleteGoal(form.id!!)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Goal", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
