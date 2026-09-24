package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.CategorizeViewModel
import com.belinze.lifeos.viewmodel.MerchantGroup
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CATEGORIZE_CATEGORIES = listOf(
    "food", "transport", "utilities", "groceries", "rent", "airtime",
    "entertainment", "health", "education", "shopping", "savings", "investment",
    "housing", "personal_care", "subscriptions", "miscellaneous",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorizeScreen(
    navController: NavHostController,
    viewModel:     CategorizeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    PageScaffold(
        eyebrow = "Finance",
        title = "Categorize",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.x3l),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(Spacing.sm))
                    Text("Loading uncategorized transactions…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@PageScaffold
        }

        if (state.transactions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("All transactions categorized", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Every transaction has a meaningful category. Nice work!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
            return@PageScaffold
        }

        Text(
            "${state.transactions.size} ${if (state.transactions.size == 1) "transaction" else "transactions"} need a category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
        ) {
            items(state.merchantGroups, key = { it.merchant }) { group ->
                Box(modifier = Modifier.animateItem()) {
                    MerchantGroupCard(
                        group       = group,
                        onAssignAll = { cat -> viewModel.assignCategoryForMerchant(group.merchant, cat) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantGroupCard(
    group:       MerchantGroup,
    onAssignAll: (String) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    group.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "${group.count} transaction${if (group.count == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled     = true,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ),
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick  = { pickerOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(50.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Pick a category", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (pickerOpen) {
        CategoryPickerSheet(
            onPick    = { cat -> pickerOpen = false; onAssignAll(cat) },
            onDismiss = { pickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    onPick:    (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(Spacing.lg),
        ) {
            Text("Pick a category", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = Spacing.base))
            CATEGORIZE_CATEGORIES.forEach { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        ) { onPick(cat) }
                        .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(capitalize(cat), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private fun capitalize(value: String): String =
    value.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun formatDateTime(iso: String?): String = try {
    LocalDateTime.parse(iso?.take(19)).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}
