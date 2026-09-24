package com.belinze.lifeos.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

private data class FilterEntry(val tab: SearchTab, val label: String, val icon: ImageVector)

private val FILTERS = listOf(
    FilterEntry(SearchTab.All,           "All",        Icons.Outlined.Apps),
    FilterEntry(SearchTab.Transactions,  "Finance",    Icons.Outlined.Wallet),
    FilterEntry(SearchTab.Tasks,         "Tasks",      Icons.Outlined.CheckBox),
    FilterEntry(SearchTab.Events,        "Events",     Icons.Outlined.CalendarMonth),
    FilterEntry(SearchTab.Birthdays,     "Birthdays",  Icons.Outlined.CardGiftcard),
    FilterEntry(SearchTab.Anniversaries, "Anniversary",Icons.Outlined.Favorite),
    FilterEntry(SearchTab.Countdowns,    "Countdown",  Icons.Outlined.Timer),
    FilterEntry(SearchTab.Budgets,       "Budgets",    Icons.Outlined.AccountBalance),
    FilterEntry(SearchTab.Recurring,     "Recurring",  Icons.Outlined.Repeat),
    FilterEntry(SearchTab.Bills,         "Bills",      Icons.Outlined.Receipt),
    FilterEntry(SearchTab.Goals,         "Goals",      Icons.Outlined.Star),
    FilterEntry(SearchTab.Incomes,       "Income",     Icons.Outlined.TrendingUp),
    FilterEntry(SearchTab.Loans,         "Loans",      Icons.Outlined.AccountBalance),
)

private const val MAX_RECENT = 5

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel:     SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var expandedSections by remember {
        mutableStateOf(setOf("transactions","tasks","events","birthdays","anniversaries",
            "countdowns","budgets","recurring","bills","goals","incomes","loans"))
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    PageScaffold(
        title    = "Search",
        onBack   = { navController.popBackStack() },
        scrollable = false,
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.updateQuery(it) },
            placeholder = { Text("Name, ref code, task, event…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.addToRecent(state.query) }),
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm).focusRequester(focusRequester),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(FILTERS, key = { it.tab }) { entry ->
                FilterChip(
                    selected     = state.activeTab == entry.tab,
                    onClick      = { viewModel.setTab(entry.tab) },
                    label        = { Text(entry.label) },
                    leadingIcon  = {
                        Icon(entry.icon, contentDescription = null, modifier = Modifier.size(13.dp))
                    },
                )
            }
        }

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("Searching…", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@PageScaffold
        }

        if (state.query.isBlank()) {
            if (recentSearches.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.x4l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    Text("Search everything", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Search by name, M-Pesa ref code, task, event, birthday and more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Recent",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.clearRecent() }) { Text("Clear") }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(recentSearches) { recent ->
                        SuggestionChip(
                            onClick = {
                                viewModel.updateQuery(recent)
                                viewModel.addToRecent(recent)
                            },
                            label = { Text(recent, style = MaterialTheme.typography.bodySmall) },
                            icon = {
                                Icon(Icons.Outlined.History, null, modifier = Modifier.size(14.dp))
                            },
                        )
                    }
                }
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
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.sm))
                Text("No results", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Try different keywords or adjust filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@PageScaffold
        }

        val query = state.query

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
        ) {
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Transactions) && state.transactions.isNotEmpty()) {
                item { SectionHeader("Transactions", state.transactions.size, expandedSections.contains("transactions")) { expandedSections = if (expandedSections.contains("transactions")) expandedSections - "transactions" else expandedSections + "transactions" } }
                if (expandedSections.contains("transactions")) {
                    items(state.transactions, key = { it.id }) { tx ->
                        SearchRow(
                            title = highlightText(tx.merchant ?: "Unknown", query),
                            subtitle = highlightText("${tx.category ?: "uncategorized"} · ${formatDate(tx.date)}", query),
                            icon = Icons.Outlined.Wallet,
                            iconColor = MaterialTheme.colorScheme.primary,
                            trailing = formatCurrency(tx.amount),
                            trailingColor = if (tx.transactionType == "income") Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                            onClick = { navController.navigate(NavTo.transactionDetail(tx.id)) },
                        )
                    }
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Tasks) && state.tasks.isNotEmpty()) {
                item { SectionHeader("Tasks", state.tasks.size, expandedSections.contains("tasks")) { expandedSections = if (expandedSections.contains("tasks")) expandedSections - "tasks" else expandedSections + "tasks" } }
                if (expandedSections.contains("tasks")) {
                    items(state.tasks, key = { it.id }) { task ->
                        val priorityColor = when (task.priority) {
                            "high"   -> Color(0xFFEF4444)
                            "medium" -> Color(0xFFFBBF24)
                            else     -> Color(0xFF60A5FA)
                        }
                        SearchRow(
                            title = highlightText(task.title, query),
                            subtitle = highlightText(task.deadline?.let { formatDateTime(it) } ?: "", query),
                            icon = Icons.Outlined.Check,
                            iconColor = if (task.status == "completed") Color(0xFF22C55E) else MaterialTheme.colorScheme.outline,
                            trailingDot = if (task.status != "completed") priorityColor else null,
                            onClick = { navController.navigate(NavTo.taskDetail(task.id)) },
                        )
                    }
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Events) && state.events.isNotEmpty()) {
                item { SectionHeader("Events", state.events.size, expandedSections.contains("events")) { expandedSections = if (expandedSections.contains("events")) expandedSections - "events" else expandedSections + "events" } }
                if (expandedSections.contains("events")) {
                    items(state.events, key = { it.id }) { event ->
                        SearchRow(
                            title = highlightText(event.title, query),
                            subtitle = highlightText(
                                formatDateTime(event.date) +
                                    (com.belinze.lifeos.util.formatLocationFirst(event.location)?.let { " · $it" } ?: ""),
                                query,
                            ),
                            icon = Icons.Outlined.CalendarMonth,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                        )
                    }
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Birthdays) && state.birthdays.isNotEmpty()) {
                item { SectionHeader("Birthdays", state.birthdays.size, expandedSections.contains("birthdays")) { expandedSections = if (expandedSections.contains("birthdays")) expandedSections - "birthdays" else expandedSections + "birthdays" } }
                if (expandedSections.contains("birthdays")) {
                    items(state.birthdays, key = { it.id }) { event ->
                        SearchRow(
                            title = highlightText(event.title, query),
                            subtitle = highlightText(formatDateTime(event.date), query),
                            icon = Icons.Outlined.Flag, iconColor = TYPE_COLORS["birthday"]!!,
                            onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                        )
                    }
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Anniversaries) && state.anniversaries.isNotEmpty()) {
                item { SectionHeader("Anniversaries", state.anniversaries.size, expandedSections.contains("anniversaries")) { expandedSections = if (expandedSections.contains("anniversaries")) expandedSections - "anniversaries" else expandedSections + "anniversaries" } }
                if (expandedSections.contains("anniversaries")) {
                    items(state.anniversaries, key = { it.id }) { event ->
                        SearchRow(
                            title = highlightText(event.title, query),
                            subtitle = highlightText(formatDateTime(event.date), query),
                            icon = Icons.Outlined.Flag, iconColor = TYPE_COLORS["anniversary"]!!,
                            onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                        )
                    }
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Countdowns) && state.countdowns.isNotEmpty()) {
                item { SectionHeader("Countdowns", state.countdowns.size, expandedSections.contains("countdowns")) { expandedSections = if (expandedSections.contains("countdowns")) expandedSections - "countdowns" else expandedSections + "countdowns" } }
                if (expandedSections.contains("countdowns")) {
                    items(state.countdowns, key = { it.id }) { event ->
                        SearchRow(
                            title = highlightText(event.title, query),
                            subtitle = highlightText(formatDateTime(event.date), query),
                            icon = Icons.Outlined.Flag, iconColor = TYPE_COLORS["countdown"]!!,
                            onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                        )
                    }
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Budgets) && state.budgets.isNotEmpty()) {
                item { SectionHeader("Budgets", state.budgets.size, expandedSections.contains("budgets")) { expandedSections = if (expandedSections.contains("budgets")) expandedSections - "budgets" else expandedSections + "budgets" } }
                if (expandedSections.contains("budgets")) {
                    items(state.budgets, key = { it.id }) { budget ->
                        SearchRow(
                            title = highlightText(budget.category, query),
                            subtitle = highlightText("Limit ${formatCurrency(budget.limitAmount)}", query),
                            icon = Icons.Outlined.Wallet, iconColor = Color(0xFFA78BFA),
                            onClick = { navController.navigate(NavTo.budgetForm(budget.id)) },
                        )
                    }
                }
            }

            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Recurring) && state.recurring.isNotEmpty()) {
                item { SectionHeader("Recurring", state.recurring.size, expandedSections.contains("recurring")) { expandedSections = if (expandedSections.contains("recurring")) expandedSections - "recurring" else expandedSections + "recurring" } }
                if (expandedSections.contains("recurring")) {
                    items(state.recurring, key = { it.id }) { rule ->
                        SearchRow(
                            title = highlightText(rule.title, query),
                            subtitle = highlightText("${rule.cadence ?: ""}${rule.amount?.let { " · ${formatCurrency(it)}" } ?: ""}", query),
                            icon = Icons.Outlined.Repeat, iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { navController.navigate(NavTo.recurringForm(rule.id)) },
                        )
                    }
                }
            }

            // SE-1: Bills section
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Bills) && state.bills.isNotEmpty()) {
                item { SectionHeader("Bills", state.bills.size, expandedSections.contains("bills")) { expandedSections = if (expandedSections.contains("bills")) expandedSections - "bills" else expandedSections + "bills" } }
                if (expandedSections.contains("bills")) {
                    items(state.bills, key = { it.id }) { bill ->
                        SearchRow(
                            title = highlightText(bill.title, query),
                            subtitle = highlightText(
                                buildString {
                                    bill.cycle?.let { append(it) }
                                    bill.nextDueDate?.let { d ->
                                        if (isNotEmpty()) append(" · ")
                                        append("Due ${formatDate(d)}")
                                    }
                                },
                                query,
                            ),
                            icon = Icons.Outlined.Receipt,
                            iconColor = Color(0xFFF59E0B),
                            trailing = bill.amount?.let { formatCurrency(it) },
                            trailingColor = MaterialTheme.colorScheme.error,
                            onClick = { navController.navigate(NavTo.billForm(bill.id)) },
                        )
                    }
                }
            }

            // SE-2: Goals section
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Goals) && state.goals.isNotEmpty()) {
                item { SectionHeader("Goals", state.goals.size, expandedSections.contains("goals")) { expandedSections = if (expandedSections.contains("goals")) expandedSections - "goals" else expandedSections + "goals" } }
                if (expandedSections.contains("goals")) {
                    items(state.goals, key = { it.id }) { goal ->
                        SearchRow(
                            title = highlightText(goal.title, query),
                            subtitle = highlightText(
                                buildString {
                                    goal.category?.let { append(it) }
                                    goal.deadline?.let { d ->
                                        if (isNotEmpty()) append(" · ")
                                        append("By ${formatDate(d)}")
                                    }
                                },
                                query,
                            ),
                            icon = Icons.Outlined.Star,
                            iconColor = Color(0xFF8B5CF6),
                            trailing = "${formatCurrency(goal.currentValue)} / ${formatCurrency(goal.targetValue)}",
                            onClick = { navController.navigate(NavTo.goalForm(goal.id)) },
                        )
                    }
                }
            }

            // SE-3: Income section
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Incomes) && state.incomes.isNotEmpty()) {
                item { SectionHeader("Income", state.incomes.size, expandedSections.contains("incomes")) { expandedSections = if (expandedSections.contains("incomes")) expandedSections - "incomes" else expandedSections + "incomes" } }
                if (expandedSections.contains("incomes")) {
                    items(state.incomes, key = { it.id }) { income ->
                        SearchRow(
                            title = highlightText(income.source ?: "Income", query),
                            subtitle = highlightText(
                                buildString {
                                    income.date?.let { append(formatDate(it)) }
                                    income.note?.let { n ->
                                        if (isNotEmpty()) append(" · ")
                                        append(n)
                                    }
                                },
                                query,
                            ),
                            icon = Icons.Outlined.TrendingUp,
                            iconColor = Color(0xFF22C55E),
                            trailing = formatCurrency(income.amount),
                            trailingColor = Color(0xFF22C55E),
                            onClick = { navController.navigate(NavTo.incomeForm(income.id)) },
                        )
                    }
                }
            }

            // SE-4: Loans section
            if ((state.activeTab == SearchTab.All || state.activeTab == SearchTab.Loans) && state.loans.isNotEmpty()) {
                item { SectionHeader("Loans", state.loans.size, expandedSections.contains("loans")) { expandedSections = if (expandedSections.contains("loans")) expandedSections - "loans" else expandedSections + "loans" } }
                if (expandedSections.contains("loans")) {
                    items(state.loans, key = { it.id }) { loan ->
                        SearchRow(
                            title = highlightText(loan.drawCode ?: "Loan", query),
                            subtitle = highlightText(
                                buildString {
                                    loan.drawDate?.let { append(formatDate(it)) }
                                    append(" · ${loan.status}")
                                },
                                query,
                            ),
                            icon = Icons.Outlined.AccountBalance,
                            iconColor = Color(0xFFEF4444),
                            trailing = formatCurrency(loan.drawAmountKes),
                            trailingColor = MaterialTheme.colorScheme.error,
                            onClick = { navController.navigate(NavTo.loanForm(loan.id)) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

// SE-6: Highlight matched query text in result items
@Composable
private fun highlightText(text: String, query: String): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    if (query.isBlank() || text.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var cursor = 0
        while (cursor < text.length) {
            val idx = lowerText.indexOf(lowerQuery, cursor)
            if (idx < 0) {
                append(text.substring(cursor))
                break
            }
            if (idx > cursor) append(text.substring(cursor, idx))
            val end = idx + query.length
            withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Bold)) {
                append(text.substring(idx, end))
            }
            cursor = end
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SearchRow(
    title: AnnotatedString,
    subtitle: AnnotatedString,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    trailing: String? = null,
    trailingColor: Color = Color.Unspecified,
    trailingDot: Color? = null,
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
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (trailing != null) {
                Text(trailing, style = MaterialTheme.typography.bodyMedium,
                    color = if (trailingColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else trailingColor,
                    fontWeight = FontWeight.Bold)
            }
            if (trailingDot != null) {
                Box(Modifier.size(10.dp).background(trailingDot, CircleShape))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
        }
    }
}

private fun formatDate(iso: String?): String = try {
    java.time.LocalDate.parse(iso?.take(10)).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
} catch (_: Exception) {
    iso?.take(10) ?: ""
}

private fun formatDateTime(iso: String): String = try {
    LocalDateTime.parse(iso.take(19)).format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
} catch (_: Exception) {
    iso.take(16)
}
