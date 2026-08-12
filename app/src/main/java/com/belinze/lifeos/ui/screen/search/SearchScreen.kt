package com.belinze.lifeos.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.SearchTab
import com.belinze.lifeos.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel:     SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        eyebrow = "Global",
        title   = "Search",
        onBack  = { navController.popBackStack() },
    ) {
        // ── Search field ──────────────────────────────────────────────────
        OutlinedTextField(
            value         = state.query,
            onValueChange = { viewModel.updateQuery(it) },
            modifier      = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
            placeholder   = { Text("Search transactions, tasks, events…") },
            leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine    = true,
        )

        // ── Tab filter ────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier              = Modifier.padding(bottom = Spacing.md),
        ) {
            SearchTab.values().forEach { tab ->
                FilterChip(
                    selected = state.activeTab == tab,
                    onClick  = { viewModel.setTab(tab) },
                    label    = { Text(tab.name) },
                )
            }
        }

        // ── Results ───────────────────────────────────────────────────────
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (state.query.isBlank()) {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                Text("Type to search…", color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
            }
        } else {
            // Transactions
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Transactions) && state.transactions.isNotEmpty()) {
                Text("Transactions", fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    modifier = Modifier.padding(bottom = Spacing.sm))
                state.transactions.forEach { tx ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = interactionSource,
                                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(0.12f)),
                                onClick    = { navController.navigate(NavTo.transactionDetail(tx.id)) })
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.merchant?.ifBlank { null } ?: tx.description?.take(40) ?: "Unknown",
                                fontWeight = FontWeight.Medium,
                                color      = MaterialTheme.colorScheme.onBackground,
                                maxLines   = 1, overflow = TextOverflow.Ellipsis)
                            Text(tx.category ?: "Uncategorized", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.50f))
                        }
                        Text(formatCurrency(tx.amount), fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            // Tasks
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Tasks) && state.tasks.isNotEmpty()) {
                Text("Tasks", fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    modifier = Modifier.padding(bottom = Spacing.sm))
                state.tasks.forEach { task ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = interactionSource,
                                indication = ripple(),
                                onClick    = { navController.navigate(NavTo.taskDetail(task.id)) })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Icon(Icons.Filled.TaskAlt, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(task.title, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            // Events
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Events) && state.events.isNotEmpty()) {
                Text("Events", fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    modifier = Modifier.padding(bottom = Spacing.sm))
                state.events.forEach { event ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = interactionSource,
                                indication = ripple(),
                                onClick    = { navController.navigate(NavTo.eventDetail(event.id)) })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(event.title, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Empty state
            if (state.transactions.isEmpty() && state.tasks.isEmpty() && state.events.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                    Text("No results for \"${state.query}\"",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                }
            }
        }

        Spacer(Modifier.height(Spacing.bottomNavSafeArea))
    }
}
