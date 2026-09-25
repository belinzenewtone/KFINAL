package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.dao.AssistantDao
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.dao.IncomeDao
import com.belinze.lifeos.data.db.dao.PlannerDao
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.AssistantMessageEntity
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.formatCurrency
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// AssistantViewModel — full parity port of AssistantEngine.ts
//
// 100 % local / offline — no Anthropic API calls at runtime.
// Mirrors AssistantEngine.ts intent dispatch order exactly:
//   follow-up resolution | greeting | help | fuliza | bills | recurring |
//   goals | events/today | category | spend | income | balance | budget |
//   tasks | recent transactions | top merchants | compare | snapshot | fallback
//
// Adds: a relative-date period parser (today/yesterday/this-last week/month/
// year/rolling windows), follow-up ("what about last month?") resolution using
// recent conversation history, richer category synonym matching, and
// suggested-action chips returned alongside every reply (persisted to DB).
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

/** How many recent messages are handed to the engine for follow-up resolution. */
private const val HISTORY_WINDOW = 10

/** Category → synonym keywords, mirrors AssistantEngine.ts CATEGORY_KEYWORDS. */
private val CATEGORY_KEYWORDS: Map<String, List<String>> = mapOf(
    "food" to listOf(
        "food", "restaurant", "eating", "eat out", "lunch", "dinner", "breakfast",
        "kfc", "java", "burger", "pizza", "meal", "snack", "cafe", "coffee",
    ),
    "groceries" to listOf(
        "groceries", "grocery", "naivas", "carrefour", "quickmart", "supermarket", "shop", "market",
    ),
    "transport" to listOf(
        "transport", "uber", "bolt", "fare", "taxi", "matatu", "bus", "petrol",
        "fuel", "boda", "commute", "trip", "ride",
    ),
    "airtime" to listOf(
        "airtime", "bundles", "data bundle", "safaricom", "airtel", "telkom", "credit",
    ),
    "utilities" to listOf(
        "utilities", "electricity", "kplc", "water", "internet", "wifi", "power",
    ),
    "entertainment" to listOf(
        "entertainment", "dstv", "zuku", "cinema", "movies", "film", "fun", "game", "club", "bar",
    ),
    "health" to listOf(
        "health", "hospital", "pharmacy", "doctor", "nhif", "medicine", "clinic", "prescription", "medical",
    ),
    "education" to listOf(
        "education", "school", "college", "university", "helb", "fees", "tuition", "books",
    ),
    "housing" to listOf(
        "housing", "rent", "landlord", "property", "house", "flat", "apartment",
    ),
    "subscriptions" to listOf(
        "subscription", "subscriptions", "netflix", "spotify", "showmax", "monthly plan",
    ),
    "shopping" to listOf(
        "shopping", "clothes", "shoes", "fashion", "mall", "online", "jumia", "kilimall",
    ),
)

/** Intent handler result: reply text + suggested follow-up action chips. */
private data class EngineResponse(val content: String, val actions: List<String> = emptyList())

private fun engineResponse(content: String, vararg actions: String) =
    EngineResponse(content, actions.toList())

/** A resolved relative-date window, mirrors AssistantEngine.ts `Period`. */
private data class Period(
    val label:   String,
    val startMs: Long,
    val endMs:   Long,
    val year:    Int,
    val month:   Int,   // 1-12
)

private data class PeriodTotals(val income: Double, val expense: Double)

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
    private val plannerDao:     PlannerDao,
    private val incomeDao:      IncomeDao,
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
    private val dateFmt   = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
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

            // Recent conversation (before this turn) — used for follow-up resolution.
            val history = _uiState.value.messages.takeLast(HISTORY_WINDOW).map { it.role to it.content }

            // Run local engine
            val response = runEngine(input, history)

            // Persist assistant reply (with suggested action chips, if any)
            assistantDao.insert(AssistantMessageEntity(
                id             = UUID.randomUUID().toString(),
                conversationId = DEFAULT_CONVERSATION_ID,
                role           = "assistant",
                content        = response.content,
                actions        = if (response.actions.isEmpty()) null else JSONArray(response.actions).toString(),
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

    // ─── Local engine — full intent router (mirrors AssistantEngine.process) ──

    private suspend fun runEngine(
        query:   String,
        history: List<Pair<String, String>>,
    ): EngineResponse {
        val q = query.lowercase().trim()

        // ── Follow-up resolution ────────────────────────────────────────────
        // "what about last month?" after a spending question is resolved by
        // prepending the previous user question, then re-running the router
        // once more (without history, so it can't recurse a second time).
        if (history.isNotEmpty() && isFollowUpMessage(q)) {
            val lastUserMsg = history.asReversed().firstOrNull { it.first == "user" }?.second?.lowercase()
            if (!lastUserMsg.isNullOrBlank()) {
                val stripped = q
                    .replaceFirst(Regex("^(what|how)\\s+about\\s+"), "")
                    .replaceFirst(Regex("^(same|and)\\s+for\\s+"), "")
                    .replaceFirst(Regex("^and\\s+(last|this)\\s+"), "$1 ")
                    .replaceFirst(Regex("^what\\s+of\\s+"), "")
                return runEngine("$lastUserMsg $stripped", emptyList())
            }
        }

        val period = extractPeriod(q)

        // ── Greeting ──────────────────────────────────────────────────────────
        if (q.containsAny(
                "hello", "hi ", "hey ", "hi!", "hey!", "hola", "good morning",
                "good afternoon", "good evening", "habari", "sasa", "mambo",
            )
        ) {
            return getGreeting()
        }

        // ── Help ──────────────────────────────────────────────────────────────
        if (q.containsAny(
                "help", "what can you", "what do you", "capabilities", "commands",
                "what can i ask", "how do i", "guide me",
            )
        ) {
            return getHelp()
        }

        // ── Fuliza / debt ────────────────────────────────────────────────────
        if (q.containsAny(
                "fuliza", "loan", "owe", "debt", "borrow", "borrowed",
                "overdraft", "outstanding loan", "i owe",
            )
        ) {
            return getFulizaSummary()
        }

        // ── Bills ─────────────────────────────────────────────────────────────
        if (q.containsAny(
                "bill", "bills", "due soon", "next payment", "overdue",
                "upcoming payment", "what do i owe", "when is my",
            )
        ) {
            return getBillsSummary()
        }

        // ── Recurring rules ───────────────────────────────────────────────────
        if (q.containsAny(
                "recurring", "scheduled", "automatic", "auto payment", "repeating",
                "regular payment", "standing order", "every month", "monthly rule", "weekly rule",
            )
        ) {
            return getRecurringRules()
        }

        // ── Goals ─────────────────────────────────────────────────────────────
        if (q.containsAny(
                "goal", "goals", "target", "saving goal", "savings goal", "progress",
                "how far", "achievement", "am i on track", "how close",
            )
        ) {
            return getGoalsSummary()
        }

        // ── Events / calendar ─────────────────────────────────────────────────
        if (q.containsAny(
                "event", "events", "calendar", "appointment", "schedule for",
                "what is happening", "what's on", "meeting", "what do i have today",
            )
        ) {
            return getEventsSummary(q)
        }

        // "today" alone or with event-flavoured context
        if (q == "today" ||
            (q.contains("today") && !q.containsAny("spend", "spent", "expense", "bought", "paid", "task", "todo"))
        ) {
            return getEventsSummary(q)
        }

        // ── Category-specific spending ────────────────────────────────────────
        val matchedCategory = extractCategory(q)
        if (matchedCategory != null) {
            return getCategorySpending(matchedCategory, period)
        }

        // ── Spending / expenses ───────────────────────────────────────────────
        if (q.containsAny(
                "spend", "spent", "expense", "expenses", "cost", "costs", "paid out",
                "bought", "how much did i", "how much have i", "what did i spend",
                "total expenses", "outgoings",
            )
        ) {
            return getSpendingSummary(period)
        }

        // ── Income ────────────────────────────────────────────────────────────
        if (q.containsAny(
                "income", "earned", "how much came in", "salary", "earn", "earnings",
                "inflow", "money in", "how much received", "what i received",
            )
        ) {
            return getIncomeSummary(period)
        }

        // ── Balance / net ─────────────────────────────────────────────────────
        if (q.containsAny(
                "balance", "net", "how much left", "remaining", "save", "saved",
                "saving", "can i afford", "financial health", "how am i doing",
                "am i saving", "money left", "surplus", "deficit",
            )
        ) {
            return getBalanceSummary(period)
        }

        // ── Budgets ───────────────────────────────────────────────────────────
        if (q.containsAny(
                "budget", "budgets", "over budget", "limit", "allowance",
                "on budget", "within budget", "exceeded",
            )
        ) {
            return getBudgetSummary()
        }

        // ── Tasks ─────────────────────────────────────────────────────────────
        if (q.containsAny(
                "task", "tasks", "todo", "to-do", "to do", "pending task",
                "due task", "overdue task", "complete", "unfinished", "what should i do",
            )
        ) {
            return getTasksSummary()
        }

        // ── Recent transactions ───────────────────────────────────────────────
        if (q.containsAny(
                "transaction", "transactions", "recent", "latest", "last transaction",
                "show me", "list my", "what did i buy",
            )
        ) {
            return getRecentTransactions()
        }

        // ── Top merchants ─────────────────────────────────────────────────────
        if (q.containsAny(
                "merchant", "merchants", "top merchant", "where did i spend",
                "where i spend", "most spent at", "biggest merchant", "frequen",
            )
        ) {
            return getTopMerchantsResponse(period)
        }

        // ── Comparison / trends ───────────────────────────────────────────────
        if (q.containsAny(
                "compare", "vs ", "versus", "more than last", "less than last",
                "trend", "increase", "decrease", "better or worse", "compared to",
            )
        ) {
            return getSpendingComparison()
        }

        // ── General overview / snapshot ───────────────────────────────────────
        if (q.containsAny(
                "summary", "overview", "snapshot", "dashboard", "report",
                "how are things", "update me", "what is my financial", "financial situation",
            )
        ) {
            return getFinancialSnapshot()
        }

        return getFallback(q)
    }

    // ─── Follow-up detection ──────────────────────────────────────────────────

    private fun isFollowUpMessage(text: String): Boolean = listOf(
        "what about ", "how about ", "same for ", "and for ",
        "and last ", "and this ", "what of ", "and what about ",
    ).any { text.startsWith(it) }

    // ─── Period extraction ────────────────────────────────────────────────────

    private fun extractPeriod(text: String): Period {
        val now   = ZonedDateTime.now(zone)
        val year  = now.year
        val month = now.monthValue

        fun ms(zdt: ZonedDateTime) = zdt.toInstant().toEpochMilli()

        fun endOfDay(date: LocalDate) = date.atTime(23, 59, 59, 999_000_000).atZone(zone)

        if (text.contains("yesterday")) {
            val d = now.minusDays(1).toLocalDate()
            return Period("yesterday", ms(d.atStartOfDay(zone)), ms(endOfDay(d)), year, month)
        }
        if (text.contains("today")) {
            return Period("today", ms(now.toLocalDate().atStartOfDay(zone)), ms(now), year, month)
        }
        if (text.containsAny("this week", "past week", "last 7 days", "last seven days", "seven days", "past 7")) {
            val start = now.minusDays(7).toLocalDate().atStartOfDay(zone)
            return Period("the past 7 days", ms(start), ms(now), year, month)
        }
        if (text.containsAny("last week", "previous week", "week before")) {
            val start = now.minusDays(14).toLocalDate().atStartOfDay(zone)
            val end   = endOfDay(now.minusDays(7).toLocalDate())
            return Period("last week", ms(start), ms(end), year, month)
        }
        if (text.containsAny("last month", "previous month", "month before")) {
            val lm = if (month == 1) 12 else month - 1
            val ly = if (month == 1) year - 1 else year
            val firstOfMonth = LocalDate.of(ly, lm, 1)
            val start = firstOfMonth.atStartOfDay(zone)
            val end   = endOfDay(firstOfMonth.with(TemporalAdjusters.lastDayOfMonth()))
            return Period("last month", ms(start), ms(end), ly, lm)
        }
        if (text.containsAny("last 30 days", "past 30 days", "thirty days", "30 days")) {
            val start = now.minusDays(30).toLocalDate().atStartOfDay(zone)
            return Period("the last 30 days", ms(start), ms(now), year, month)
        }
        if (text.containsAny("this year", "year to date", "ytd", "so far this year")) {
            val start = LocalDate.of(year, 1, 1).atStartOfDay(zone)
            return Period("this year", ms(start), ms(now), year, month)
        }
        if (text.containsAny("last year", "previous year")) {
            val start = LocalDate.of(year - 1, 1, 1).atStartOfDay(zone)
            val end   = endOfDay(LocalDate.of(year - 1, 12, 31))
            return Period("last year", ms(start), ms(end), year - 1, 12)
        }
        // Default: this month
        val start = LocalDate.of(year, month, 1).atStartOfDay(zone)
        return Period("this month", ms(start), ms(now), year, month)
    }

    private fun extractCategory(text: String): String? =
        CATEGORY_KEYWORDS.entries.firstOrNull { (_, keywords) -> keywords.any { text.contains(it) } }?.key

    /** Income/expense totals for a period; uses the fast monthKey aggregate for
     *  the two fixed-calendar-month periods and a row scan for rolling windows. */
    private suspend fun getPeriodTotals(period: Period): PeriodTotals {
        if (period.label == "this month" || period.label == "last month") {
            val key   = "%04d-%02d".format(period.year, period.month)
            val t     = transactionDao.getMonthTotals(key)
            return PeriodTotals(t.income ?: 0.0, t.expense ?: 0.0)
        }
        val startIso = isoFromMillis(period.startMs)
        val endIso   = isoFromMillis(period.endMs)
        val rows = transactionDao.getFiltered(
            search = "", category = "all", type = null, status = "completed",
            startDate = startIso, endDate = endIso, limit = 5000, offset = 0,
        )
        val income  = rows.filter { it.transactionType == "receive" }.sumOf { it.amount }
        val expense = rows.filter { it.transactionType in listOf("expense", "transfer", "fuliza") }.sumOf { it.amount }
        return PeriodTotals(income, expense)
    }

    // ─── Intent handlers ──────────────────────────────────────────────────────

    private suspend fun getGreeting(): EngineResponse {
        val hour  = ZonedDateTime.now(zone).hour
        val greet = if (hour < 12) "Good morning" else if (hour < 17) "Good afternoon" else "Good evening"
        // RFINAL greets with the profile name exactly as stored. We were truncating to
        // the first word, so "Jane Wanjiku" was greeted as "Jane".
        val name  = runCatching { appPreferences.state.first().profileName }
            .getOrNull()?.trim()?.takeIf { it.isNotBlank() }
        val nameStr = if (name != null) " $name" else ""
        return engineResponse(
            "$greet$nameStr! I can help with your M-Pesa spending, budgets, bills, goals, tasks, and calendar. " +
                "What would you like to know?",
            "How much did I spend this month?", "What bills are due?", "Show my goals", "Show tasks",
        )
    }

    private fun getHelp(): EngineResponse {
        val content = listOf(
            "Here's what I can help with:\n",
            "💰 Spending — \"How much did I spend this week?\" · \"Food this month\"",
            "📥 Income — \"What is my income this month?\"",
            "⚖️ Balance — \"What is my net balance?\" · \"Am I saving?\"",
            "📊 Budgets — \"Am I over budget?\"",
            "🧾 Bills — \"What bills are due?\" · \"Overdue bills\"",
            "🔁 Recurring — \"Show recurring payments\" · \"Scheduled rules\"",
            "🎯 Goals — \"How are my savings goals?\"",
            "✅ Tasks — \"What tasks are pending?\"",
            "📅 Calendar — \"What is on today?\" · \"Upcoming events\"",
            "📉 Fuliza — \"How much Fuliza do I owe?\"",
            "🏪 Merchants — \"Where did I spend most?\" · \"Top merchants\"\n",
            "Add time periods: \"today\", \"this week\", \"last month\", \"this year\".",
            "Follow-up: \"What about last month?\" after any question.",
        ).joinToString("\n")
        return engineResponse(content, "How much did I spend this month?", "What bills are due?", "Show my goals")
    }

    private suspend fun getSpendingSummary(period: Period): EngineResponse {
        val startIso = isoFromMillis(period.startMs)
        val endIso   = isoFromMillis(period.endMs)
        val totals   = getPeriodTotals(period)

        if (totals.expense <= 0.0) {
            return engineResponse("No expenses recorded ${period.label}.", "View transactions")
        }

        var comparisonText = ""
        if (period.label == "this month") {
            val lastExpense = transactionDao.getMonthTotals(prevKey).expense ?: 0.0
            if (lastExpense > 0) {
                val change = (totals.expense - lastExpense) / lastExpense * 100
                val dir    = if (change >= 0) "up" else "down"
                comparisonText = " That is ${"%.1f".format(abs(change))}% $dir from last month."
            }
        }

        val cats = transactionDao.getCategoryTotals(startIso, endIso).take(3)
        val topCatText = if (cats.isNotEmpty()) {
            "\n\nTop categories: " + cats.joinToString(" · ") { "${it.category ?: "Other"} ${kes(it.total)}" } + "."
        } else {
            ""
        }

        val merchants = transactionDao.getTopMerchants(startIso, endIso, 3)
        val topMerchantText = if (merchants.isNotEmpty()) {
            "\n\nTop merchants: " + merchants.joinToString(" · ") { "${it.merchant ?: "Unknown"} ${kes(it.total)}" } + "."
        } else {
            ""
        }

        return engineResponse(
            "You spent ${kes(totals.expense)} ${period.label}.$comparisonText$topCatText$topMerchantText",
            "View transactions", "View budgets", "Break down by category",
        )
    }

    private suspend fun getCategorySpending(category: String, period: Period): EngineResponse {
        val startIso = isoFromMillis(period.startMs)
        val endIso   = isoFromMillis(period.endMs)
        val cats     = transactionDao.getCategoryTotals(startIso, endIso)
        val match    = cats.firstOrNull { it.category?.contains(category, ignoreCase = true) == true }

        if (match == null || match.total <= 0.0) {
            return engineResponse("No $category spending recorded ${period.label}.", "View all spending", "View transactions")
        }

        val allExpense = cats.sumOf { it.total }
        val pct        = if (allExpense > 0) (match.total / allExpense * 100) else 0.0
        return engineResponse(
            "You spent ${kes(match.total)} on $category ${period.label} — ${"%.1f".format(pct)}% of total expenses.",
            "View transactions", "View budgets",
        )
    }

    private suspend fun getIncomeSummary(period: Period): EngineResponse {
        val totals = getPeriodTotals(period)
        val startDate = isoFromMillis(period.startMs).take(10)
        val endDate   = isoFromMillis(period.endMs).take(10)
        val sources = runCatching { incomeDao.getInRange(startDate, endDate) }
            .getOrDefault(emptyList())
            .mapNotNull { it.source?.takeIf { s -> s.isNotBlank() } }
            .distinct()
            .take(3)
        val sourceText = if (sources.isNotEmpty()) "\n\nSources: ${sources.joinToString(", ")}." else ""

        return engineResponse(
            "Income ${period.label}: ${kes(totals.income)}.$sourceText",
            "View transactions", "View dashboard",
        )
    }

    private suspend fun getBalanceSummary(period: Period): EngineResponse {
        val totals = getPeriodTotals(period)
        val net    = totals.income - totals.expense
        val icon   = if (net > 0) "✓" else "⚠"
        val status = if (net > 0) {
            "You are saving ${kes(net)} ${period.label}."
        } else {
            "You are spending ${kes(-net)} more than you earned ${period.label}."
        }
        return engineResponse(
            "$icon $status\n\nIncome: ${kes(totals.income)}\nExpenses: ${kes(totals.expense)}\nNet: ${kes(net)}",
            "View dashboard", "View budgets", "View transactions",
        )
    }

    private suspend fun getBudgetSummary(): EngineResponse {
        val budgets = budgetDao.getAll()
        if (budgets.isEmpty()) {
            return engineResponse("You have not set any budgets yet. Go to Planner to create one.", "View budgets")
        }

        val (startIso, endIso) = isoRange(monthKey)
        val spentMap = transactionDao.getCategoryTotals(startIso, endIso)
            .associate { (it.category ?: "") to it.total }

        data class WithStatus(val category: String, val limit: Double, val spent: Double)
        val withStatus = budgets.map { b -> WithStatus(b.category, b.limitAmount, spentMap[b.category] ?: 0.0) }
        val over = withStatus.filter { it.limit > 0 && it.spent > it.limit }
        val near = withStatus.filter { it.limit > 0 && it.spent <= it.limit && it.spent / it.limit >= 0.8 }
        val safe = withStatus.size - over.size - near.size

        if (over.isNotEmpty()) {
            val overList = over.joinToString(", ") { "${it.category} (${kes(it.spent)} / ${kes(it.limit)})" }
            val nearText = if (near.isNotEmpty()) {
                "\n\nNearing limit: " + near.joinToString(", ") { "${it.category} ${(it.spent / it.limit * 100).toInt()}%" } + "."
            } else {
                ""
            }
            val safeText = if (safe > 0) "\n$safe other categor${if (safe > 1) "ies" else "y"} on track." else ""
            return engineResponse("Over budget in: $overList.$nearText$safeText", "View budgets")
        }

        if (near.isNotEmpty()) {
            val nearList = near.joinToString(", ") { "${it.category} ${(it.spent / it.limit * 100).toInt()}%" }
            return engineResponse(
                "You are within budget, but getting close in: $nearList.\n" +
                    "$safe other categor${if (safe > 1) "ies are" else "y is"} comfortably on track.",
                "View budgets",
            )
        }

        val totalBudgeted = withStatus.sumOf { it.limit }
        val totalSpent    = withStatus.sumOf { it.spent }
        val pct           = if (totalBudgeted > 0) (totalSpent / totalBudgeted * 100).toInt() else 0
        return engineResponse(
            "On track across all ${budgets.size} budget categories this month " +
                "($pct% used: ${kes(totalSpent)} / ${kes(totalBudgeted)}).",
            "View budgets",
        )
    }

    private suspend fun getGoalsSummary(): EngineResponse {
        val goals     = plannerDao.getAllGoals()
        val active    = goals.filter { it.status == "active" }
        val completed = goals.filter { it.status == "completed" }

        if (active.isEmpty() && completed.isEmpty()) {
            return engineResponse("You have no goals set yet. Go to Goals to create a savings target.", "View goals")
        }
        if (active.isEmpty()) {
            return engineResponse("All ${completed.size} goals completed!", "View goals")
        }

        val lines = active.take(5).map { g ->
            val pct    = if (g.targetValue > 0) (g.currentValue / g.targetValue * 100).coerceAtMost(100.0).roundToInt() else 0
            val filled = (pct / 10.0).roundToInt()
            val bar    = "█".repeat(filled) + "░".repeat(10 - filled)
            val deadline = g.deadline?.take(10)?.let { d ->
                " · due " + runCatching { LocalDate.parse(d).format(dateFmt) }.getOrDefault(d)
            } ?: ""
            "${g.title}\n  $bar $pct% — ${kes(g.currentValue)} / ${kes(g.targetValue)}$deadline"
        }
        val completedNote = if (completed.isNotEmpty()) {
            "\n\n${completed.size} goal${if (completed.size > 1) "s" else ""} already completed!"
        } else {
            ""
        }

        return engineResponse(
            "Active goals (${active.size}):\n\n${lines.joinToString("\n\n")}$completedNote",
            "View goals",
        )
    }

    private suspend fun getBillsSummary(): EngineResponse {
        val bills  = plannerDao.getAllBills()
        val active = bills.filter { it.isActive != 0 }
        if (active.isEmpty()) {
            return engineResponse("No active bills set up. Go to Planner to add recurring payments.", "View bills")
        }

        val now    = ZonedDateTime.now(zone)
        val nowStr = now.format(isoDtFmt)
        val in7Str = now.plusDays(7).format(isoDtFmt)
        val overdue = active.filter { it.nextDueDate != null && it.nextDueDate!! < nowStr && it.paidStatus == 0 }
        val dueSoon = active.filter { it.nextDueDate != null && it.nextDueDate!! in nowStr..in7Str }
            .sortedBy { it.nextDueDate }

        val sb = StringBuilder()
        if (overdue.isNotEmpty()) {
            sb.append("Overdue (${overdue.size}):\n")
            sb.append(overdue.joinToString("\n") { "• ${it.title} — ${kes(it.amount ?: 0.0)}" })
            sb.append("\n\n")
        }
        if (dueSoon.isNotEmpty()) {
            val list = dueSoon.joinToString("\n") { b ->
                val days   = daysBetween(nowStr, b.nextDueDate!!)
                val whenTx = when (days) { 0 -> "today"; 1 -> "tomorrow"; else -> "in $days days" }
                "• ${b.title} — ${kes(b.amount ?: 0.0)} ($whenTx)"
            }
            sb.append("Due soon:\n$list\n\n")
        }
        if (overdue.isEmpty() && dueSoon.isEmpty()) {
            sb.append("No bills due in the next 7 days. ")
        }
        val totalMonthly = active.sumOf { it.amount ?: 0.0 }
        sb.append("${active.size} active bills · ${kes(totalMonthly)} monthly.")

        return engineResponse(sb.toString().trim(), "View bills")
    }

    private suspend fun getRecurringRules(): EngineResponse {
        val rules  = plannerDao.getAllRules()
        val active = rules.filter { it.enabled != 0 }
        if (active.isEmpty()) {
            return engineResponse(
                "No recurring rules set up yet. Go to Planner to create scheduled payments or income.",
                "View planner",
            )
        }

        val now    = ZonedDateTime.now(zone)
        val in14   = now.plusDays(14).format(isoDtFmt)
        val dueNext = active.filter { it.nextRunAt != null && it.nextRunAt!! <= in14 }.take(5)

        val upcoming = if (dueNext.isNotEmpty()) {
            dueNext.joinToString("\n") { r ->
                val dateLabel = r.nextRunAt!!.take(10).let { d ->
                    runCatching { LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)) }
                        .getOrDefault(d)
                }
                val amt      = r.amount?.let { " — ${kes(it)}" } ?: ""
                val cadence  = r.cadence?.let { " ($it)" } ?: ""
                "• ${r.title}$amt$cadence · next: $dateLabel"
            }
        } else {
            null
        }

        val expenseRules = active.filter { it.type == "expense" }
        val incomeRules  = active.filter { it.type == "income" }
        val totalMonthlyExpense = expenseRules
            .filter { it.cadence == "monthly" && it.amount != null }
            .sumOf { it.amount ?: 0.0 }

        var content = "${active.size} active recurring rule${if (active.size != 1) "s" else ""} " +
            "(${expenseRules.size} expense, ${incomeRules.size} income)."
        if (totalMonthlyExpense > 0) content += "\nMonthly scheduled expenses: ${kes(totalMonthlyExpense)}."
        if (upcoming != null) content += "\n\nDue in 14 days:\n$upcoming"

        return engineResponse(content, "View planner")
    }

    private suspend fun getFulizaSummary(): EngineResponse {
        val loans  = plannerDao.getAllLoans()
        val active = loans.filter { it.status == "active" }

        if (active.isEmpty()) {
            val repaid = loans.filter { it.status == "repaid" }
            val note   = if (repaid.isNotEmpty()) {
                " You have ${repaid.size} repaid Fuliza loan${if (repaid.size > 1) "s" else ""} in history."
            } else {
                ""
            }
            return engineResponse("No outstanding Fuliza loans — you are clear!$note", "View transactions")
        }

        val outstanding = active.sumOf { it.drawAmountKes }
        val repaidAmt   = active.sumOf { it.totalRepaidKes }
        val lines = active.take(3).joinToString("\n") { l ->
            val date = l.drawDate?.take(10)?.let { d ->
                runCatching { LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) }.getOrDefault(d)
            } ?: "unknown date"
            val code = l.drawCode?.let { " ($it)" } ?: ""
            "• ${kes(l.drawAmountKes)} drawn on $date$code"
        }

        return engineResponse(
            "${active.size} active Fuliza loan${if (active.size > 1) "s" else ""}:\n\n" +
                "Outstanding: ${kes(outstanding)}\nRepaid so far: ${kes(repaidAmt)}\n\n$lines",
            "View transactions",
        )
    }

    private suspend fun getTasksSummary(): EngineResponse {
        val now    = ZonedDateTime.now(zone)
        val nowStr = now.format(isoDtFmt)
        val in7Str = now.plusDays(7).format(isoDtFmt)
        val tasks  = taskDao.getUpcoming(in7Str, 100)

        if (tasks.isEmpty()) {
            return engineResponse("No tasks due in the next 7 days — you are all caught up!", "View tasks", "Add task")
        }

        val high     = tasks.filter { it.priority == "high" }
        val overdue  = tasks.filter { it.deadline != null && it.deadline!! < nowStr }
        val upcoming = tasks.take(4)

        var text = "${tasks.size} task${if (tasks.size != 1) "s" else ""} due in the next 7 days"
        if (high.isNotEmpty()) text += ", ${high.size} high priority"
        if (overdue.isNotEmpty()) text += "\n${overdue.size} already overdue"
        text += "."

        val list = upcoming.joinToString("\n") { t ->
            val due = t.deadline?.take(10)?.let { d ->
                runCatching { LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)) }.getOrDefault(d)
            } ?: "no date"
            "• ${t.title} ($due)"
        }
        text += "\n\n$list"
        if (tasks.size > 4) text += "\n…and ${tasks.size - 4} more."

        return engineResponse(text, "View tasks", "Add task")
    }

    private suspend fun getEventsSummary(text: String): EngineResponse {
        val now = ZonedDateTime.now(zone)

        if (text.contains("today")) {
            val todayStart = now.toLocalDate().atStartOfDay(zone).format(isoDtFmt)
            val todayEnd   = now.toLocalDate().atTime(23, 59, 59).atZone(zone).format(isoDtFmt)
            val events     = eventDao.getInRange(todayStart, todayEnd)
            if (events.isEmpty()) return engineResponse("Nothing on your calendar today.", "View calendar")

            val list = events.take(6).joinToString("\n") { e ->
                val time = if (e.allDay != 0) {
                    "all day"
                } else {
                    runCatching { java.time.OffsetDateTime.parse(e.date).format(DateTimeFormatter.ofPattern("HH:mm")) }
                        .getOrDefault(if (e.date.length >= 16) e.date.substring(11, 16) else "")
                }
                "• ${e.title} ($time)"
            }
            return engineResponse(
                "Today (${events.size} event${if (events.size != 1) "s" else ""}):\n\n$list",
                "View calendar",
            )
        }

        val nowStr = now.format(isoDtFmt)
        val endStr = now.plusDays(7).format(isoDtFmt)
        val events = eventDao.getInRange(nowStr, endStr)
        if (events.isEmpty()) return engineResponse("No upcoming events in the next 7 days.", "View calendar")

        val list = events.take(6).joinToString("\n") { e ->
            val date = runCatching {
                LocalDate.parse(e.date.take(10)).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
            }.getOrDefault(e.date.take(10))
            "• ${e.title} ($date)"
        }
        return engineResponse("Upcoming events (${events.size}):\n\n$list", "View calendar")
    }

    private suspend fun getRecentTransactions(): EngineResponse {
        val txs = transactionDao.getRecent()
        if (txs.isEmpty()) {
            return engineResponse("No transactions yet. Import your M-Pesa SMS to get started.", "Import SMS")
        }

        val list = txs.joinToString("\n") { t ->
            val date = t.date?.take(10)?.let { d ->
                runCatching { LocalDate.parse(d).format(dateFmt) }.getOrDefault(d)
            } ?: ""
            val icon = when (t.transactionType) {
                "receive"  -> "↑"
                "transfer" -> "↔"
                else       -> "↓"
            }
            "$icon ${t.merchant ?: "Unknown"} — ${kes(t.amount)} ($date)"
        }
        return engineResponse("Recent transactions:\n\n$list", "View all transactions")
    }

    private suspend fun getTopMerchantsResponse(period: Period): EngineResponse {
        val startIso  = isoFromMillis(period.startMs)
        val endIso    = isoFromMillis(period.endMs)
        val merchants = transactionDao.getTopMerchants(startIso, endIso, 8)

        if (merchants.isEmpty()) {
            return engineResponse("No merchant spending data ${period.label}.", "View transactions")
        }

        val list = merchants.mapIndexed { i, m -> "${i + 1}. ${m.merchant ?: "Unknown"} — ${kes(m.total)}" }
            .joinToString("\n")
        return engineResponse("Top merchants ${period.label}:\n\n$list", "View transactions", "View budgets")
    }

    private suspend fun getSpendingComparison(): EngineResponse {
        val current = transactionDao.getMonthTotals(monthKey)
        val last    = transactionDao.getMonthTotals(prevKey)
        val curExpense  = current.expense ?: 0.0
        val lastExpense = last.expense ?: 0.0

        if (lastExpense <= 0.0) {
            return engineResponse("This month you have spent ${kes(curExpense)}. No data for last month to compare.")
        }

        val expChange = (curExpense - lastExpense) / lastExpense * 100
        val dir       = if (expChange >= 0) "more" else "less"
        val icon      = if (expChange >= 0) "📈" else "📉"

        var text = "$icon Spending ${"%.1f".format(abs(expChange))}% $dir this month vs last:\n\n" +
            "This month: ${kes(curExpense)}\nLast month: ${kes(lastExpense)}\n" +
            "Difference: ${kes(abs(curExpense - lastExpense))}"

        val curIncome  = current.income ?: 0.0
        val lastIncome = last.income ?: 0.0
        if (lastIncome > 0) {
            val incChange = (curIncome - lastIncome) / lastIncome * 100
            val incDir    = if (incChange >= 0) "up" else "down"
            text += "\n\nIncome ${"%.1f".format(abs(incChange))}% $incDir (${kes(curIncome)} vs ${kes(lastIncome)})."
        }

        return engineResponse(text, "View budgets", "View transactions")
    }

    private suspend fun getFinancialSnapshot(): EngineResponse {
        val totals  = transactionDao.getMonthTotals(monthKey)
        val income  = totals.income ?: 0.0
        val expense = totals.expense ?: 0.0
        val net     = income - expense

        val budgets = budgetDao.getAll()
        val (startIso, endIso) = isoRange(monthKey)
        val spentMap = transactionDao.getCategoryTotals(startIso, endIso).associate { (it.category ?: "") to it.total }
        val overBudget = budgets.count { it.limitAmount > 0 && (spentMap[it.category] ?: 0.0) > it.limitAmount }

        val goals       = plannerDao.getAllGoals()
        val activeGoals = goals.count { it.status == "active" }

        val now    = ZonedDateTime.now(zone)
        val in7Str = now.plusDays(7).format(isoDtFmt)
        val bills       = plannerDao.getAllBills()
        val activeBills = bills.filter { it.isActive != 0 }
        val dueSoonCount = activeBills.count { it.nextDueDate != null && it.nextDueDate!! <= in7Str }

        val fulizaLoans   = plannerDao.getAllLoans()
        val activeFuliza  = fulizaLoans.filter { it.status == "active" }
        val recurringRules = plannerDao.getAllRules()
        val activeRecurring = recurringRules.count { it.enabled != 0 }

        val lines = buildList {
            add("${if (net >= 0) "✓" else "⚠"} This month: income ${kes(income)}, expenses ${kes(expense)}, net ${kes(net)}")
            if (budgets.isNotEmpty()) {
                add(
                    if (overBudget > 0) {
                        "  ⚠ Over budget in $overBudget categor${if (overBudget > 1) "ies" else "y"}"
                    } else {
                        "  ✓ All ${budgets.size} budgets on track"
                    },
                )
            }
            if (dueSoonCount > 0) add("  $dueSoonCount bill${if (dueSoonCount > 1) "s" else ""} due in 7 days")
            if (activeGoals > 0) add("  $activeGoals active savings goal${if (activeGoals > 1) "s" else ""}")
            if (activeFuliza.isNotEmpty()) {
                add("  ⚠ Fuliza outstanding: ${kes(activeFuliza.sumOf { it.drawAmountKes })}")
            }
            if (activeRecurring > 0) add("  $activeRecurring active recurring rule${if (activeRecurring > 1) "s" else ""}")
        }

        return engineResponse(
            lines.joinToString("\n"),
            "View budgets", "What bills are due?", "Show my goals", "How much did I spend?",
        )
    }

    // ─── Fallback ─────────────────────────────────────────────────────────────

    private fun getFallback(text: String): EngineResponse {
        if (text.containsAny("ksh", "money", "cash", "pay", "paid", "mpesa", "m-pesa", "send", "sent", "receive")) {
            return engineResponse(
                "Looks like you are asking about money. Try one of these:",
                "How much did I spend this month?", "What is my income?", "Show recent transactions", "What is my balance?",
            )
        }
        if (text.containsAny("when", "due", "next", "upcoming", "remind")) {
            return engineResponse(
                "Looking for something coming up? Try:",
                "What bills are due?", "What tasks are pending?", "What's on today?", "Show recurring payments",
            )
        }
        if (text.containsAny("how", "much", "total", "amount", "sum")) {
            return engineResponse(
                "Need a total or amount? Try:",
                "How much did I spend this month?", "What is my net balance?", "Am I over budget?",
            )
        }
        return engineResponse(
            "I didn't quite catch that. You can ask about your spending, income, balance, budgets, goals, bills, " +
                "tasks, or calendar.\n\nTip: follow up any answer with \"what about last month?\" or \"and this week?\"",
            "How much did I spend this month?", "What bills are due?", "Show my goals", "Show budgets",
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun isoRange(key: String): Pair<String, String> {
        val startMs = monthKeyToStartMillis(key)
        val endMs   = monthKeyToEndMillis(key)
        return isoFromMillis(startMs) to isoFromMillis(endMs)
    }

    private fun isoFromMillis(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(zone).format(isoDtFmt)

    /** Whole calendar days between two ISO-ish date(-time) strings (end - start). */
    private fun daysBetween(startIso: String, endIso: String): Int = runCatching {
        val start = LocalDate.parse(startIso.take(10))
        val end   = LocalDate.parse(endIso.take(10))
        (end.toEpochDay() - start.toEpochDay()).toInt()
    }.getOrDefault(0)

    /**
     * Amounts inside assistant replies. RFINAL formats these with
     * Intl.NumberFormat('en-KE', { currency: 'KES', 2 decimals }) → "KSh 1,234.00".
     * This was hand-rolled as "KES 1,234" (literal KES, no decimals, US grouping), so
     * every reply body disagreed with the rest of the app. Delegate to the shared
     * formatter instead — it renders the same symbol, grouping and 2 decimals.
     */
    private fun kes(amount: Double): String = formatCurrency(amount)

    /** Check if the string contains any of the given keywords */
    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    private fun AssistantMessageEntity.toChatMessage() = ChatMessage(
        id        = id,
        role      = role,
        content   = content,
        createdAt = createdAt ?: nowIso(),
        actions   = parseActionsJson(actions).toImmutableList(),
    )

    private fun parseActionsJson(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
