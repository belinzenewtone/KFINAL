package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.belinze.lifeos.util.Haptics
import com.belinze.lifeos.viewmodel.TransactionViewModel

private val TX_TYPES = listOf("expense", "income", "transfer")
private val STATUSES = listOf("completed", "pending", "failed", "reversed")
private val CATEGORIES = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
    "income", "uncategorized",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    transactionId: String?,
    navController: NavHostController,
    viewModel:     TransactionViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val isEdit = !transactionId.isNullOrEmpty()

    LaunchedEffect(transactionId) {
        viewModel.openForm(transactionId?.ifEmpty { null })
    }

    PageScaffold(
        title = if (isEdit) "Edit Transaction" else "Add Transaction",
        onBack = { navController.popBackStack() },
        actions = {
            if (isEdit) {
                TextButton(onClick = {
                    viewModel.softDelete(transactionId.orEmpty())
                    navController.popBackStack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TX_TYPES.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = formState.transactionType == type,
                        onClick = {
                            viewModel.updateFormType(type)
                            Haptics.light()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = TX_TYPES.size),
                        label = { Text(type.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            OutlinedTextField(
                value = formState.amount,
                onValueChange = { viewModel.updateFormAmount(it) },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = formState.merchant,
                onValueChange = { viewModel.updateFormMerchant(it) },
                label = { Text("Merchant / Counterparty") },
                placeholder = { Text("e.g. Java House") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateFormDescription(it) },
                label = { Text("Description (optional)") },
                placeholder = { Text("Short summary of the transaction") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.updateFormNotes(it) },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Longer note, ref numbers, receipt info…") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = formState.fee,
                    onValueChange = { viewModel.updateFormFee(it) },
                    label = { Text("Fee (optional)") },
                    placeholder = { Text("e.g. 33") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = formState.balanceAfter,
                    onValueChange = { viewModel.updateFormBalanceAfter(it) },
                    label = { Text("Balance after (optional)") },
                    placeholder = { Text("Account balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = formState.mpesaCode,
                onValueChange = { viewModel.updateFormMpesaCode(it) },
                label = { Text("M-Pesa code (optional)") },
                placeholder = { Text("e.g. TAB5CDE12F") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CATEGORIES.forEach { cat ->
                    androidx.compose.material3.FilterChip(
                        selected = formState.category == cat,
                        onClick = {
                            viewModel.updateFormCategory(cat)
                            Haptics.light()
                        },
                        label = { Text(cat.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Text(
                "Status",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                STATUSES.forEachIndexed { idx, status ->
                    SegmentedButton(
                        selected = formState.status == status,
                        onClick = { viewModel.updateFormStatus(status) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = STATUSES.size),
                        label = { Text(status.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            if (formState.error != null) {
                Text(formState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.saveForm { navController.popBackStack() } },
                enabled = !formState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (isEdit) "Update" else "Save")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
