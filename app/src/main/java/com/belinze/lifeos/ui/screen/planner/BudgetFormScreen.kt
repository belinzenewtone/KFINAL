package com.belinze.lifeos.ui.screen.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.BudgetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CATEGORIES = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
)
private val PERIODS = listOf("daily", "weekly", "monthly", "yearly")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormScreen(
    budgetId:      String?,
    navController: NavHostController,
    viewModel:     BudgetViewModel = hiltViewModel(),
) {
    val form         by viewModel.formState.collectAsStateWithLifecycle()
    val isEdit        = !budgetId.isNullOrEmpty()
    val scope         = rememberCoroutineScope()

    // CC-4: fade-in on load
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue   = if (contentVisible) 1f else 0f,
        animationSpec = tween(300),
        label         = "contentAlpha",
    )

    // CC-2: delete confirmation
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // CC-3: success banner
    var successMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(budgetId) {
        viewModel.openForm(budgetId?.ifEmpty { null })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            title    = if (isEdit) "Edit Budget" else "Add Budget",
            onBack   = { navController.popBackStack() },
            actions  = {
                if (isEdit) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            scrollable = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .alpha(contentAlpha),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = form.category.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.replaceFirstChar { it.uppercase() }) },
                                onClick = { viewModel.updateCategory(cat); categoryExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = form.limitAmount,
                    onValueChange = { viewModel.updateLimitAmount(it) },
                    label = { Text("Budget Limit") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("KES") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                var periodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = periodExpanded,
                    onExpandedChange = { periodExpanded = it },
                ) {
                    OutlinedTextField(
                        value = form.period.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Period") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(periodExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = periodExpanded,
                        onDismissRequest = { periodExpanded = false },
                    ) {
                        PERIODS.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.replaceFirstChar { it.uppercase() }) },
                                onClick = { viewModel.updatePeriod(period); periodExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = form.alertThreshold,
                    onValueChange = { viewModel.updateAlertThreshold(it) },
                    label = { Text("Alert Threshold (%)") },
                    placeholder = { Text("80") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedButton(
                    onClick = { viewModel.updateActive(!form.isActive) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Active: ${if (form.isActive) "Yes" else "No"}")
                }

                if (form.error != null) {
                    Text(form.error!!, color = MaterialTheme.colorScheme.error)
                }

                // CC-3: show banner then navigate
                Button(
                    onClick = {
                        viewModel.saveForm {
                            scope.launch {
                                successMsg = if (isEdit) "Budget updated" else "Budget added"
                                delay(1200)
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled  = !form.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
                ) {
                    if (form.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (isEdit) "Update Budget" else "Add Budget")
                    }
                }

                Spacer(Modifier.height(Spacing.bottomNavSafeArea))
            }
        }

        // CC-3: success banner (outside PageScaffold so it overlays)
        TopBanner(
            visible   = successMsg != null,
            message   = successMsg ?: "",
            tone      = BannerTone.Success,
            onDismiss = { successMsg = null },
        )
    }

    // CC-2: delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete budget?") },
            text             = { Text("This budget will be permanently removed.") },
            confirmButton    = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.softDelete(form.id.orEmpty())
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
