package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.AppDropdownField
import com.belinze.lifeos.ui.components.AppPickerSheet
import com.belinze.lifeos.ui.components.AppSegmentedControl
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.PickerOption
import com.belinze.lifeos.ui.components.SegmentOption
import com.belinze.lifeos.ui.components.rememberFormFadeIn
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.ui.theme.categoryColor
import com.belinze.lifeos.ui.theme.categoryIcon
import com.belinze.lifeos.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

private val TX_TYPES = listOf("expense", "income", "transfer")
private val STATUSES = listOf("completed", "pending", "failed", "reversed")

// Unified category list — mirrors RN's CATEGORY_COLORS key order exactly.
private val CATEGORIES = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
    "housing", "personal_care", "subscriptions", "fuel", "loans", "insurance",
    "miscellaneous", "uncategorized", "income",
)

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: String?,
    navController: NavHostController,
    viewModel:     TransactionViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isEdit = !transactionId.isNullOrEmpty()
    val scope  = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text("Delete transaction") },
            text    = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.softDelete(transactionId.orEmpty())
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    LaunchedEffect(transactionId) {
        viewModel.openForm(transactionId?.ifEmpty { null })
    }

    Box(modifier = Modifier.fillMaxSize()) {
    PageScaffold(
        title = if (isEdit) "Edit Transaction" else "Add Transaction",
        onBack = { navController.popBackStack() },
        actions = {
            if (isEdit) {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .then(rememberFormFadeIn()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // RFINAL uses its own pill track (SegmentedControl.tsx) here, not M3
            // segments, and fires no haptic when the type changes.
            AppSegmentedControl(
                options     = TX_TYPES.map {
                    SegmentOption(key = it, label = it.replaceFirstChar { c -> c.uppercase() })
                },
                selectedKey = formState.transactionType,
                onSelect    = { viewModel.updateFormType(it) },
                modifier    = Modifier.fillMaxWidth(),
            )

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
                onValueChange = { viewModel.updateFormMpesaCode(it.uppercase()) },
                label = { Text("M-Pesa code (optional)") },
                placeholder = { Text("e.g. TAB5CDE12F") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )

            var categorySheetOpen by remember { mutableStateOf(false) }
            AppDropdownField(
                label       = "Category",
                valueLabel  = formState.category.replaceFirstChar { it.uppercase() },
                onClick     = { categorySheetOpen = true },
                modifier    = Modifier.fillMaxWidth(),
                leadingIcon = categoryIcon(formState.category),
                leadingTint = categoryColor(formState.category),
            )

            var statusSheetOpen by remember { mutableStateOf(false) }
            AppDropdownField(
                label      = "Status",
                valueLabel = formState.status.replaceFirstChar { it.uppercase() },
                onClick    = { statusSheetOpen = true },
                modifier   = Modifier.fillMaxWidth(),
            )

            // RN opens a SwipeableSheet option list for both pickers; there is no
            // anchored Material dropdown in the reference app.
            AppPickerSheet(
                visible     = categorySheetOpen,
                title       = "Category",
                options     = CATEGORIES.map { cat ->
                    PickerOption(
                        key   = cat,
                        label = cat.replaceFirstChar { it.uppercase() },
                        icon  = categoryIcon(cat),
                        tint  = categoryColor(cat),
                    )
                },
                selectedKey = formState.category,
                onSelect    = { viewModel.updateFormCategory(it) },
                onDismiss   = { categorySheetOpen = false },
            )
            AppPickerSheet(
                visible     = statusSheetOpen,
                title       = "Status",
                options     = STATUSES.map { st ->
                    PickerOption(key = st, label = st.replaceFirstChar { it.uppercase() })
                },
                selectedKey = formState.status,
                onSelect    = { viewModel.updateFormStatus(it) },
                onDismiss   = { statusSheetOpen = false },
            )

            if (formState.error != null) {
                Text(formState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick  = {
                    viewModel.saveForm {
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isEdit) "Transaction updated" else "Transaction saved")
                            navController.popBackStack()
                        }
                    }
                },
                enabled  = !formState.isSaving,
                shape    = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Saving...")
                } else {
                    Text(if (isEdit) "Update" else "Save")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg),
    )
    } // Box
}
