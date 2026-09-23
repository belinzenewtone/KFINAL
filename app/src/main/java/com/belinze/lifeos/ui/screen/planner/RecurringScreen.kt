package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CADENCE_LABELS = mapOf(
    "hourly"   to "Hourly",
    "daily"    to "Daily",
    "weekly"   to "Weekly",
    "biweekly" to "Biweekly",
    "mon_fri"  to "Mon–Fri",
    "monthly"  to "Monthly",
    "yearly"   to "Yearly",
)

@Composable
fun RecurringScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var banner by remember { mutableStateOf<String?>(null) }

    // BUG #23: re-load on resume so rules added/edited in RecurringFormScreen
    // are visible immediately when the user navigates back to this screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadAll()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // RC-1: delete confirmation dialog
    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (pendingDelete != null) {
        val (deleteId, deleteTitle) = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete rule") },
            text  = { Text("Remove $deleteTitle?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    // RC-2: trigger exit animation then delete from DB after animation settles
                    viewModel.deleteRule(deleteId)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    PageScaffold(
        eyebrow = "Automation",
        title = "Recurring",
        subtitle = "Subscriptions and repeating items",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.recurringForm()) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add rule", tint = MaterialTheme.colorScheme.primary)
            }
        },
        topBanner = {
            TopBanner(
                visible = banner != null,
                message = banner ?: "",
                tone = BannerTone.Success,
                onDismiss = { banner = null },
                autoDismissMs = 2000,
            )
        },
    ) {
        if (state.recurringRules.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Repeat, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No recurring rules yet", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Add a rule to automate subscriptions, bills, or repeating tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
            ) {
                items(state.recurringRules, key = { it.id }) { rule ->
                    val enabled = rule.enabled != 0
                    // RC-2: animateItem animates item removal with fade+shrink automatically
                    GlassCard(
                        onClick = { navController.navigate(NavTo.recurringForm(rule.id)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm)
                            .alpha(if (enabled) 1f else 0.45f)
                            .animateItem(fadeInSpec = null, fadeOutSpec = null),
                    ) {
                        // Row 1: title | amount
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                rule.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f).padding(end = Spacing.sm),
                            )
                            rule.amount?.let {
                                Text(formatCurrency(it), style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Row 2: cadence | next run
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                CADENCE_LABELS[rule.cadence] ?: rule.cadence ?: "Monthly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Next: ${formatDate(rule.nextRunAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Row 3: delete | toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // RC-1: show confirmation before deleting
                            IconButton(
                                onClick = { pendingDelete = rule.id to rule.title },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                            // RC-8: match RN toggle visual — white thumb on primary track
                            Switch(
                                checked = enabled,
                                onCheckedChange = { v ->
                                    viewModel.toggleRecurringEnabled(rule.id, v)
                                    banner = "${rule.title} ${if (v) "enabled" else "paused"}"
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor    = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor    = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor  = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }
    }
}

private fun formatDate(iso: String?): String = try {
    LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}
