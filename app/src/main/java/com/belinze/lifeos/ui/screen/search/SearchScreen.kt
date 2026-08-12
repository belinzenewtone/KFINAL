package com.belinze.lifeos.ui.screen.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.SearchTab
import com.belinze.lifeos.viewmodel.SearchViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val TYPE_COLORS = mapOf(
    "birthday" to Color(0xFFFF69B4),
    "anniversary" to Color(0xFFFF6B6B),
    "countdown" to Color(0xFFFFA726),
)

private val FILTERS = listOf(
    SearchTab.All to "All",
    SearchTab.Transactions to "Finance",
    SearchTab.Tasks to "Tasks",
    SearchTab.Events to "Events",
    SearchTab.Birthdays to "Birthdays",
    SearchTab.Anniversaries to "Anniversary",
    SearchTab.Countdowns to "Countdown",
    SearchTab.Budgets to "Budgets",
    SearchTab.Recurring to "Recurring",
)

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel:     SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title = "Search",
        onBack = { navController.popBackStack() },
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.updateQuery(it) },
            placeholder = { Text("Name, ref code, task, event…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(FILTERS) { (tab, label) ->
                FilterChip(
                    selected = state.activeTab == tab,
                    onClick = { viewModel.setTab(tab) },
                    label = { Text(label) },
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            return@PageScaffold
        }

        if (state.query.isBlank()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.x4l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.sm))
                Text("Search everything", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Search by name, M-Pesa ref code, task, event, birthday and more.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@PageScaffold
        }

        val hasAny = state.transactions.isNotEmpty() || state.tasks.isNotEmpty() ||
            state.events.isNotEmpty() || state.birthdays.isNotEmpty() ||
            state.anniversaries.isNotEmpty() || state.countdowns.isNotEmpty() ||
            state.budgets.isNotEmpty() || state.recurring.isNotEmpty() ||
            state.bills.isNotEmpty() || state.goals.isNotEmpty() ||
            state.incomes.isNotEmpty() || state.loans.isNotEmpty()

        if (!hasAny) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.x4l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No results", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Try different keywords or adjust filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@PageScaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Transactions) && state.transactions.isNotEmpty()) {
                item { SectionHeader("Transactions", state.transactions.size) }
                items(state.transactions, key = { it.id }) { tx ->
                    SearchRow(
                        title = tx.merchant ?: "Unknown",
                        subtitle = "${tx.category ?: "uncategorized"} · ${formatDate(tx.date)}",
                        icon = Icons.Filled.Wallet,
                        iconColor = MaterialTheme.colorScheme.primary,
                        trailing = formatCurrency(tx.amount),
                        trailingColor = if (tx.transactionType == "income") Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                        onClick = { navController.navigate(NavTo.transactionDetail(tx.id)) },
                    )
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Tasks) && state.tasks.isNotEmpty()) {
                item { SectionHeader("Tasks", state.tasks.size) }
                items(state.tasks, key = { it.id }) { task ->
                    SearchRow(
                        title = task.title,
                        subtitle = task.deadline?.let { formatDateTime(it) } ?: "",
                        icon = Icons.Filled.Check,
                        iconColor = if (task.status == "completed") Color(0xFF22C55E) else MaterialTheme.colorScheme.outline,
                        onClick = { navController.navigate(NavTo.taskDetail(task.id)) },
                    )
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Events) && state.events.isNotEmpty()) {
                item { SectionHeader("Events", state.events.size) }
                items(state.events, key = { it.id }) { event ->
                    SearchRow(
                        title = event.title,
                        subtitle = formatDateTime(event.date) + (event.location?.let { " · $it" } ?: ""),
                        icon = Icons.Filled.CalendarMonth,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                    )
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Birthdays) && state.birthdays.isNotEmpty()) {
                item { SectionHeader("Birthdays", state.birthdays.size) }
                items(state.birthdays, key = { it.id }) { event ->
                    SearchRow(
                        title = event.title, subtitle = formatDateTime(event.date),
                        icon = Icons.Filled.Flag, iconColor = TYPE_COLORS["birthday"]!!,
                        onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                    )
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Anniversaries) && state.anniversaries.isNotEmpty()) {
                item { SectionHeader("Anniversaries", state.anniversaries.size) }
                items(state.anniversaries, key = { it.id }) { event ->
                    SearchRow(
                        title = event.title, subtitle = formatDateTime(event.date),
                        icon = Icons.Filled.Flag, iconColor = TYPE_COLORS["anniversary"]!!,
                        onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                    )
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Countdowns) && state.countdowns.isNotEmpty()) {
                item { SectionHeader("Countdowns", state.countdowns.size) }
                items(state.countdowns, key = { it.id }) { event ->
                    SearchRow(
                        title = event.title, subtitle = formatDateTime(event.date),
                        icon = Icons.Filled.Flag, iconColor = TYPE_COLORS["countdown"]!!,
                        onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                    )
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Budgets) && state.budgets.isNotEmpty()) {
                item { SectionHeader("Budgets", state.budgets.size) }
                items(state.budgets, key = { it.id }) { budget ->
                    SearchRow(
                        title = budget.category, subtitle = "Limit ${formatCurrency(budget.limitAmount)}",
                        icon = Icons.Filled.Wallet, iconColor = Color(0xFFA78BFA),
                        onClick = { navController.navigate(NavTo.budgetForm(budget.id)) },
                    )
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Recurring) && state.recurring.isNotEmpty()) {
                item { SectionHeader("Recurring", state.recurring.size) }
                items(state.recurring, key = { it.id }) { rule ->
                    SearchRow(
                        title = rule.title,
                        subtitle = "${rule.cadence ?: ""}${rule.amount?.let { " · ${formatCurrency(it)}" } ?: ""}",
                        icon = Icons.Filled.Repeat, iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { navController.navigate(NavTo.recurringForm(rule.id)) },
                    )
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    trailing: String? = null,
    trailingColor: Color = Color.Unspecified,
) {
    GlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(iconColor.copy(alpha = 0x20 / 255f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (trailing != null) {
                Text(trailing, style = MaterialTheme.typography.bodyMedium,
                    color = if (trailingColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else trailingColor,
                    fontWeight = FontWeight.Bold)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
        }
    }
}

private fun formatDate(iso: String?): String = try {
    java.time.LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
} catch (_: Exception) { iso?.take(10) ?: "" }

private fun formatDateTime(iso: String): String = try {
    LocalDateTime.parse(iso.take(19)).format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
} catch (_: Exception) { iso.take(16) }
