package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.dao.AssistantDao
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.AssistantMessageEntity
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import com.belinze.lifeos.util.nowIso
import com.belinze.lifeos.util.previousMonthKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// AssistantViewModel — Phase 6: full keyword router
//
// 100 % local / offline — no Anthropic API calls at runtime.
// Mirrors AssistantEngine.ts intent dispatch:
//   greeting | help | spend | category | merchant | compare | income |
//   topspend | savings | budget | task | event | search | date | week
//
// Each intent runs a targeted SQLite query and formats a human-readable reply.
// ─────────────────────────────────────────────────────────────────────────────

/** Displayed message (combines DB entity + in-flight state). */
@Immutable
data class ChatMessage(
    val id:          String,
    val role:        String,   // "user" | "assistant"
    val content:     String,
    val isStreaming: Boolean = false,
    val createdAt:   String  = nowIso(),
    // AS-1: structured action chips derived from response content
    val actions:     ImmutableList<String> = persistentListOf(),
)

@Immutable
data class AssistantUiState(
    val isLoading:      Boolean          = false,
    val messages:       ImmutableList<ChatMessage> = persistentListOf(),
    val inputText:      String           = "",
    val conversationId: String          = DEFAULT_CONVERSATION_ID,
    val error:          String?          = null,
    val isOnline:       Boolean          = true,   // local engine is always ready
)

private const val DEFAULT_CONVERSATION_ID = "main"

// ─── Known categories and merchants (mirrors AssistantEngine.ts lists) ────────

private val KNOWN_CATEGORIES = setOf(
    "food", "transport", "fuel", "utilities", "health", "shopping", "entertainment",
    "housing", "education", "personal", "savings", "fees", "mpesa", "income",
    "fuliza", "transfer", "other",
)

private val KNOWN_MERCHANTS = setOf(
    "naivas", "quickmart", "carrefour", "java", "kfc", "kenchic", "equity", "kcb", "coop",
    "safaricom", "airtel", "starbucks", "uber", "bolt", "netflix", "spotify",
)

// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class AssistantViewModel
    @Inject
    constructor(
    private val assistantDao:   AssistantDao,
    private val transactionDao: TransactionDao,
    private val taskDao:        TaskDao,
    private val budgetDao:      BudgetDao,
    private val eventDao:       EventDao,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    /** Mirrors React `settings.assistantQuickSuggestions`. */
    val quickSuggestionsEnabled: StateFlow<Boolean> =
        appPreferences.state
            .map { it.assistantQuickSuggestions }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true,
            )

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val zone      = ZoneId.systemDefault()
    private val isoDtFmt  = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFmt   = DateTimeFormatter.ofPattern("MMM d")
    private val monthKey  = currentMonthKey()
    private val prevKey   = previousMonthKey()

    init {
        assistantDao.observeConversation(DEFAULT_CONVERSATION_ID)
            .onEach { entities ->
                _uiState.update { s ->
                    s.copy(messages = entities.map { it.toChatMessage() }.toImmutableList())
                }
            }
            .launchIn(viewModelScope)
    }

    // ─── Input ────────────────────────────────────────────────────────────────

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val input = _uiState.value.inputText.trim()
        if (input.isEmpty() || _uiState.value.isLoading) return

        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        viewModelScope.launch {
            // Persist user message
            assistantDao.insert(AssistantMessageEntity(
                id             = UUID.randomUUID().toString(),
                conversationId = DEFAULT_CONVERSATION_ID,
                role           = "user",
                content        = input,
                createdAt      = nowIso(),
            ))

            // Run local engine
            val answer = runEngine(input)

            // Persist assistant reply
            assistantDao.insert(AssistantMessageEntity(
                id             = UUID.randomUUID().toString(),
                conversationId = DEFAULT_CONVERSATION_ID,
                role           = "assistant",
                content        = answer,
                createdAt      = nowIso(),
            ))

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ─── Conversation management ──────────────────────────────────────────────

    fun clearConversation() {
        viewModelScope.launch {
            assistantDao.clearConversation(DEFAULT_CONVERSATION_ID, nowIso())
            _uiState.update { it.copy(messages = persistentListOf()) }
        }
    }

    // ─── Local engine — full keyword router ──────────────────────────────────

    private suspend fun runEngine(query: String): String {
        val q     = query.lowercase().trim()
        val today = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)

        // ── Greeting ──────────────────────────────────────────────────────────
        if (q.containsAny("hello", "hi", "hey", "sup", "yo", "good morning",
                           "good afternoon", "good evening", "howdy")) {
            return "👋 Hey there! Here's what I can help you with:\n\n" +
                "• \"How much did I spend this month?\"\n" +
                "• \"What are my active tasks?\"\n" +
                "• \"What's my budget status?\"\n" +
                "• \"Compare this month to last month\"\n" +
                "• \"What events do I have coming up?\"\n" +
                "• \"Show my top spending categories\"\n\n" +
                "All answers come from your local data — no internet needed. 🔒"
        }

        // ── Help ──────────────────────────────────────────────────────────────
        if (q.containsAny("help", "what can you do", "commands", "tutorial")) {
            return "📚 I understand these query types:\n\n" +
                "💰 Spending: spend, total, expense, how much\n" +
                "📅 Time ranges: today, yesterday, this week, this month\n" +
                "🏷️ Categories: food, transport, entertainment …\n" +
                "🏪 Merchants: naivas, java, equity …\n" +
                "📊 Compare: last month, trend, month over month\n" +
                "💵 Income: earned, received, salary\n" +
                "🔝 Top: biggest, most, highest\n" +
                "💾 Savings: saved, net, balance\n" +
                "📋 Budget: limit, over budget, remaining\n" +
                "✅ Tasks: due, overdue, todos\n" +
                "📆 Events: calendar, upcoming, schedule\n" +
                "🔍 Search: find, look for, transactions with"
        }

        // ── Week-spend ────────────────────────────────────────────────────────
        if (q.containsAny("this week", "this wk", "weekly") ||
            (q.containsAny("spend", "spent", "total") && q.containsAny("week"))) {
            return queryWeek()
        }

        // ── Today ─────────────────────────────────────────────────────────────
        if ((q.containsAny("spend", "spent", "total", "how much") && q.contains("today")) ||
            q == "today") {
            return queryDate(today)
        }

        // ── Yesterday ─────────────────────────────────────────────────────────
        if (q.contains("yesterday")) {
            val yesterday = LocalDate.now(zone).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            return queryDate(yesterday)
        }

        // ── Compare / MoM ────────────────────────────────────────────────────
        if (q.containsAny("compare", "vs", "versus", "last month", "previous month",
                           "month over month", "trend", "changed")) {
            return queryCompare()
        }

        // ── Savings / balance ─────────────────────────────────────────────────
        if (q.containsAny("save", "saving", "savings", "saved", "net", "balance", "profit")) {
            return querySavings()
        }

        // ── Income ────────────────────────────────────────────────────────────
        if (q.containsAny("income", "earn", "earned", "received", "receive", "salary")) {
            return queryIncome()
        }

        // ── Top spend ─────────────────────────────────────────────────────────
        if (q.containsAny("top", "highest", "most", "biggest", "largest",
                           "where did i spend", "breakdown")) {
            return queryTopSpend()
        }

        // ── Budget ────────────────────────────────────────────────────────────
        if (q.containsAny("budget", "limit", "overspend", "over budget", "remaining budget")) {
            return queryBudgets()
        }

        // ── Tasks ─────────────────────────────────────────────────────────────
        if (q.containsAny("task", "tasks", "todo", "to-do", "reminder", "due",
                           "deadline", "overdue", "pending task")) {
            return queryTasks()
        }

        // ── Events ────────────────────────────────────────────────────────────
        if (q.containsAny("event", "events", "calendar", "meeting", "appointment",
                           "schedule", "upcoming")) {
            return queryEvents()
        }

        // ── Category intent ───────────────────────────────────────────────────
        val extractedCat = extractCategory(q)
        if (extractedCat != null || q.containsAny("category", "categor")) {
            return queryCategory(extractedCat)
        }

        // ── Merchant intent ───────────────────────────────────────────────────
        val extractedMerchant = extractMerchant(q)
        if (extractedMerchant != null ||
            q.containsAny("merchant", "shop", "store", "vendor", "bought from", "paid to")) {
            return queryMerchant(extractedMerchant)
        }

        // ── Search / find ─────────────────────────────────────────────────────
        if (q.containsAny("find", "search", "look for", "show me", "where is",
                           "transactions with")) {
            // Extract search term after keywords
            val searchTerm = q
                .removePrefix("find").removePrefix("search").removePrefix("look for")
                .removePrefix("show me").removePrefix("where is").trim()
            return querySearch(searchTerm.ifBlank { q })
        }

        // ── Explicit date (YYYY-MM-DD) ────────────────────────────────────────
        val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")
        val dateMatch = dateRegex.find(q)
        if (dateMatch != null) {
            return queryDate(dateMatch.value)
        }

        // ── Generic spend (fallback) ──────────────────────────────────────────
        if (q.containsAny("spend", "spent", "total", "expense", "cost", "pay",
                           "paid", "much", "money")) {
            return queryMonthSpend()
        }

        // ── Default ───────────────────────────────────────────────────────────
        return "I can help with spending totals, tasks, budgets, and events. " +
               "Try asking: \"How much did I spend this month?\" or \"What tasks are due?\"\n\n" +
               "Say \"help\" for a full list of query types."
    }

    // ─── Intent handlers ──────────────────────────────────────────────────────

    private suspend fun queryMonthSpend(): String {
        val (startIso, endIso) = isoRange(monthKey)
        val totals   = transactionDao.getMonthTotals(monthKey)
        val expense  = totals.expense ?: 0.0
        val income   = totals.income  ?: 0.0
        val catTops  = transactionDao.getCategoryTotals(startIso, endIso).take(3)

        val catLines = catTops.joinToString("\n") {
            "  • ${it.category?.replaceFirstChar { c -> c.uppercase() } ?: "Other"}: ${kes(it.total)}"
        }
        return buildString {
            append("📊 This month so far:\n")
            append("• Spent: ${kes(expense)}\n")
            append("• Received: ${kes(income)}\n")
            if (catLines.isNotBlank()) {
                append("\nTop categories:\n$catLines")
            }
        }
    }

    private suspend fun queryWeek(): String {
        val weekStart = LocalDate.now(zone).with(DayOfWeek.MONDAY)
            .atStartOfDay(zone).format(isoDtFmt)
        val today     = LocalDate.now(zone).atTime(23, 59, 59)
            .atZone(zone).format(isoDtFmt)

        val rows     = transactionDao.getFiltered(
            search    = "",
            category  = "all",
            type      = null,
            status    = null,
            startDate = weekStart,
            endDate   = today,
            limit     = 200,
            offset    = 0,
        )
        val weekExpense = rows.filter {
            it.transactionType in listOf("expense", "fuliza")
        }.sumOf { it.amount }
        val weekIncome = rows.filter { it.transactionType == "receive" }.sumOf { it.amount }

        val catTotals = rows
            .filter { it.transactionType in listOf("expense", "fuliza") }
            .groupBy { it.category ?: "other" }
            .map { (cat, txList) -> cat to txList.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(3)

        val catLines = catTotals.joinToString("\n") { (cat, amt) ->
            "  • ${cat.replaceFirstChar { it.uppercase() }}: ${kes(amt)}"
        }

        return buildString {
            append("📅 This week:\n")
            append("• Spent: ${kes(weekExpense)}\n")
            append("• Received: ${kes(weekIncome)}\n")
            if (catLines.isNotBlank()) append("\nTop categories:\n$catLines")
        }
    }

    private suspend fun queryDate(date: String): String {
        val start = "${date}T00:00:00"
        val end   = "${date}T23:59:59"
        val rows  = transactionDao.getFiltered(
            search    = "",
            category  = "all",
            type      = null,
            status    = null,
            startDate = start,
            endDate   = end,
            limit     = 30,
            offset    = 0,
        )
        if (rows.isEmpty()) return "No transactions found on $date."

        val total = rows.filter { it.transactionType in listOf("expense", "fuliza") }
            .sumOf { it.amount }
        val lines = rows.take(5).joinToString("\n") { tx ->
            val sign = if (tx.transactionType == "receive") "+" else "-"
            "  $sign${kes(tx.amount)} — ${tx.merchant ?: "Unknown"}"
        }
        return "📅 On $date:\n• Total spent: ${kes(total)}\n\nTransactions:\n$lines" +
               if (rows.size > 5) "\n  … and ${rows.size - 5} more" else ""
    }

    private suspend fun queryCompare(): String {
        val curTotals  = transactionDao.getMonthTotals(monthKey)
        val prevTotals = transactionDao.getMonthTotals(prevKey)
        val curSpend   = curTotals.expense  ?: 0.0
        val prevSpend  = prevTotals.expense ?: 0.0
        val delta      = curSpend - prevSpend
        val pct        = if (prevSpend > 0) (delta / prevSpend * 100) else 0.0
        val dir        = if (delta >= 0) "more" else "less"
        val arrow      = if (delta >= 0) "📈" else "📉"

        return "$arrow Month comparison:\n" +
               "• This month: ${kes(curSpend)}\n" +
               "• Last month: ${kes(prevSpend)}\n" +
               "• Difference: ${if (delta >= 0) "+" else ""}${kes(delta)} " +
               "(${String.format(java.util.Locale.US, "%.1f", Math.abs(pct))}% $dir)"
    }

    private suspend fun queryIncome(): String {
        val totals = transactionDao.getMonthTotals(monthKey)
        val income = totals.income ?: 0.0
        return "💵 Income received this month: ${kes(income)}"
    }

    private suspend fun querySavings(): String {
        val totals  = transactionDao.getMonthTotals(monthKey)
        val income  = totals.income  ?: 0.0
        val expense = totals.expense ?: 0.0
        val saved   = income - expense
        return if (saved >= 0) {
            "💾 This month you've saved ${kes(saved)}\n• Income: ${kes(income)}\n• Spent: ${kes(expense)}"
        } else {
            "⚠️ You're spending ${kes(-saved)} more than you've received\n• Income: ${kes(income)}\n• Spent: ${kes(expense)}"
        }
    }

    private suspend fun queryTopSpend(): String {
        val (startIso, endIso) = isoRange(monthKey)
        val merchants = transactionDao.getTopMerchants(startIso, endIso, 5)
        val cats      = transactionDao.getCategoryTotals(startIso, endIso).take(5)

        val mLines = merchants.take(5).mapIndexed { i, m ->
            "  ${i + 1}. ${m.merchant ?: "Unknown"}: ${kes(m.total)}"
        }.joinToString("\n")

        val cLines = cats.mapIndexed { i, c ->
            "  ${i + 1}. ${c.category?.replaceFirstChar { it.uppercase() } ?: "Other"}: ${kes(c.total)}"
        }.joinToString("\n")

        return buildString {
            if (mLines.isNotBlank()) append("🏪 Top merchants:\n$mLines\n\n")
            if (cLines.isNotBlank()) append("🏷️ Top categories:\n$cLines")
        }.ifBlank { "No spending data found for this month." }
    }

    private suspend fun queryBudgets(): String {
        val (startIso, endIso) = isoRange(monthKey)
        val budgets  = budgetDao.getAll()
        if (budgets.isEmpty()) return "No budgets set up. Create one in the Planner → Budgets screen."

        val catSpend = transactionDao.getCategoryTotals(startIso, endIso)
            .associate { it.category to it.total }

        val lines = budgets.take(6).joinToString("\n") { b ->
            val spend = catSpend[b.category] ?: 0.0
            val pct   = if (b.limitAmount > 0) (spend / b.limitAmount * 100).toInt() else 0
            val status = when {
                pct >= 100 -> "🔴 Over"
                pct >= 80  -> "🟡 Close"
                else       -> "🟢 OK"
            }
            "  • ${b.category.replaceFirstChar { it.uppercase() }}: ${kes(spend)}/${kes(b.limitAmount)} ($pct%) $status"
        }
        return "📋 Budget status:\n$lines"
    }

    private suspend fun queryTasks(): String {
        val all     = taskDao.getAll()
        val active  = all.filter { it.status == "active" }
        val overdue = active.filter { it.deadline != null && it.deadline!! < nowIso() }

        val top5 = active.take(5).joinToString("\n") { t ->
            val dueStr = if (t.deadline != null) " — due ${t.deadline!!.take(10)}" else ""
            "  • ${t.title} [${t.priority}]$dueStr"
        }
        return buildString {
            append("✅ Tasks:\n")
            append("• Active: ${active.size}   Overdue: ${overdue.size}\n")
            if (top5.isNotBlank()) append("\nUpcoming:\n$top5")
            if (active.size > 5) append("\n  … and ${active.size - 5} more")
        }
    }

    private suspend fun queryEvents(): String {
        val today  = LocalDate.now(zone).atStartOfDay(zone).format(isoDtFmt)
        val events = eventDao.getFrom(today).take(5)
        if (events.isEmpty()) return "📆 No upcoming events found."

        val lines = events.joinToString("\n") { ev ->
            val dateLabel = try {
                LocalDate.parse(ev.date.take(10)).format(dateFmt)
            } catch (e: Exception) {
                ev.date.take(10)
            }
            "  • $dateLabel — ${ev.title}${if (!ev.location.isNullOrBlank()) " @ ${ev.location}" else ""}"
        }
        return "📆 Upcoming events:\n$lines"
    }

    private suspend fun queryCategory(category: String?): String {
        if (category == null) {
            // No specific category — show top 5
            val (startIso, endIso) = isoRange(monthKey)
            val cats = transactionDao.getCategoryTotals(startIso, endIso)
            val lines = cats.take(5).joinToString("\n") {
                "  • ${it.category?.replaceFirstChar { c -> c.uppercase() } ?: "Other"}: ${kes(it.total)}"
            }
            return "🏷️ Spending by category this month:\n$lines"
        }

        val (startIso, endIso) = isoRange(monthKey)
        val cats = transactionDao.getCategoryTotals(startIso, endIso)
        val match = cats.firstOrNull { it.category.equals(category, ignoreCase = true) }
            ?: return "No spending recorded in '$category' this month."

        val merchants = transactionDao.getTopMerchants(startIso, endIso, 3)
        val mLines = merchants.take(3).joinToString("\n") {
            "  • ${it.merchant ?: "Unknown"}: ${kes(it.total)}"
        }

        return buildString {
            append("🏷️ ${category.replaceFirstChar { it.uppercase() }} this month:\n")
            append("• Total: ${kes(match.total)}\n")
            if (mLines.isNotBlank()) append("\nTop merchants:\n$mLines")
        }
    }

    private suspend fun queryMerchant(merchant: String?): String {
        val (startIso, endIso) = isoRange(monthKey)
        val merchants = transactionDao.getTopMerchants(startIso, endIso, 20)

        if (merchant == null) {
            val lines = merchants.take(5).joinToString("\n") {
                "  • ${it.merchant ?: "Unknown"}: ${kes(it.total)}"
            }
            return "🏪 Top merchants this month:\n$lines"
        }

        val match = merchants.firstOrNull {
            it.merchant?.lowercase()?.contains(merchant.lowercase()) == true
        } ?: return "No transactions found for '$merchant' this month."

        return "🏪 ${match.merchant} this month:\n" +
               "• Total spent: ${kes(match.total)}"
    }

    private suspend fun querySearch(term: String): String {
        val rows = transactionDao.getFiltered(
            search    = term,
            category  = "all",
            type      = null,
            status    = null,
            startDate = null,
            endDate   = null,
            limit     = 10,
            offset    = 0,
        )
        if (rows.isEmpty()) return "🔍 No transactions found matching '$term'."

        val lines = rows.take(5).joinToString("\n") { tx ->
            val date = tx.date?.take(10) ?: ""
            val sign = if (tx.transactionType == "receive") "+" else "-"
            "  $date $sign${kes(tx.amount)} — ${tx.merchant ?: "Unknown"}"
        }
        return "🔍 Found ${rows.size} transaction(s) for '$term':\n$lines" +
               if (rows.size > 5) "\n  … and ${rows.size - 5} more" else ""
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun isoRange(key: String): Pair<String, String> {
        val startMs = monthKeyToStartMillis(key)
        val endMs   = monthKeyToEndMillis(key)
        val start   = Instant.ofEpochMilli(startMs).atZone(zone).format(isoDtFmt)
        val end     = Instant.ofEpochMilli(endMs).atZone(zone).format(isoDtFmt)
        return start to end
    }

    /** Format as "KES 1,234" */
    private fun kes(amount: Double): String =
        "KES ${String.format(java.util.Locale.US, "%,.0f", amount)}"

    /** Check if the string contains any of the given keywords */
    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    /** Try to extract a known category from the query */
    private fun extractCategory(q: String): String? =
        KNOWN_CATEGORIES.firstOrNull { q.contains(it) }

    /** Try to extract a known merchant name from the query */
    private fun extractMerchant(q: String): String? =
        KNOWN_MERCHANTS.firstOrNull { q.contains(it) }

    private fun AssistantMessageEntity.toChatMessage() = ChatMessage(
        id        = id,
        role      = role,
        content   = content,
        createdAt = createdAt ?: nowIso(),
    )
}
