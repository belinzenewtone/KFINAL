package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

@Composable
fun RecurringScreen(
    navController: NavHostController,
    viewModel:     PlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var banner by remember { mutableStateOf<String?>(null) }

    PageScaffold(
        eyebrow = "Automation",
        title = "Recurring",
        subtitle = "Subscriptions and repeating items",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.recurringForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add rule", tint = MaterialTheme.colorScheme.primary)
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
                Icon(Icons.Filled.Repeat, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.base))
                Text("No recurring rules yet", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
                Text("Add a rule to automate subscriptions, bills, or repeating tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.recurringRules, key = { it.id }) { rule ->
                    GlassCard(
                        onClick = { navController.navigate(NavTo.recurringForm(rule.id)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                                Text(rule.title, style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Text(
                                    "${rule.type ?: "expense"} · ${rule.cadence ?: "monthly"} · Next: ${formatDate(rule.nextRunAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                rule.amount?.let {
                                    Text(formatCurrency(it), style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                                Switch(
                                    checked = rule.enabled != 0,
                                    onCheckedChange = { v ->
                                        viewModel.toggleRecurringEnabled(rule.id, v)
                                        banner = "${rule.title} ${if (v) "enabled" else "paused"}"
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.base))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { viewModel.deleteRule(rule.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
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
} catch (_: Exception) { iso?.take(10) ?: "" }
