package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.TransactionViewModel

private val TX_TYPES = listOf("expense", "income", "transfer", "fuliza")
private val CATEGORIES = listOf(
    "food", "transport", "utility", "entertainment", "health",
    "clothing", "education", "business", "savings", "loan", "other",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    transactionId: String?,
    navController: NavHostController,
    viewModel:     TransactionViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val isEdit    = !transactionId.isNullOrEmpty()

    LaunchedEffect(transactionId) {
        viewModel.openForm(transactionId?.ifEmpty { null })
    }

    PageScaffold(
        eyebrow = if (isEdit) "Edit" else "Add",
        title   = "Transaction",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {

            // ── Type selector ─────────────────────────────────────────────
            Text("Type", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TX_TYPES.forEach { type ->
                    FilterChip(
                        selected = formState.transactionType == type,
                        onClick  = { viewModel.updateFormType(type) },
                        label    = { Text(type.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // ── Amount ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = formState.amount,
                onValueChange = { viewModel.updateFormAmount(it) },
                label         = { Text("Amount (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // ── Merchant / Description ────────────────────────────────────
            OutlinedTextField(
                value         = formState.merchant,
                onValueChange = { viewModel.updateFormMerchant(it) },
                label         = { Text("Merchant / Description") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // ── Category ──────────────────────────────────────────────────
            Text("Category", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = formState.category == cat,
                        onClick  = { viewModel.updateFormCategory(cat) },
                        label    = { Text(cat.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // ── Notes ─────────────────────────────────────────────────────
            OutlinedTextField(
                value         = formState.notes,
                onValueChange = { viewModel.updateFormNotes(it) },
                label         = { Text("Notes (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4,
            )

            // ── Error ─────────────────────────────────────────────────────
            if (formState.error != null) {
                Text(formState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Save button ───────────────────────────────────────────────
            Button(
                onClick  = { viewModel.saveForm { navController.popBackStack() } },
                enabled  = !formState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Update" else "Save Transaction")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
