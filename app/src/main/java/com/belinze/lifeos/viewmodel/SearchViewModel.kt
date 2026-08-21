package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.dao.IncomeDao
import com.belinze.lifeos.data.db.dao.PlannerDao
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.BillEntity
import com.belinze.lifeos.data.db.entity.BudgetEntity
import com.belinze.lifeos.data.db.entity.EventEntity
import com.belinze.lifeos.data.db.entity.FulizaLoanEntity
import com.belinze.lifeos.data.db.entity.GoalEntity
import com.belinze.lifeos.data.db.entity.IncomeEntity
import com.belinze.lifeos.data.db.entity.RecurringRuleEntity
import com.belinze.lifeos.data.db.entity.TaskEntity
import com.belinze.lifeos.data.db.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab {
    All,
    Transactions,
    Tasks,
    Events,
    Birthdays,
    Anniversaries,
    Countdowns,
    Budgets,
    Recurring,
    Bills,
    Goals,
    Incomes,
    Loans,
}

@Immutable
data class SearchUiState(
    val query:         String                              = "",
    val activeTab:     SearchTab                           = SearchTab.All,
    val isLoading:     Boolean                             = false,
    val transactions:  ImmutableList<TransactionEntity>   = persistentListOf(),
    val tasks:         ImmutableList<TaskEntity>           = persistentListOf(),
    val events:        ImmutableList<EventEntity>          = persistentListOf(),
    val birthdays:     ImmutableList<EventEntity>          = persistentListOf(),
    val anniversaries: ImmutableList<EventEntity>          = persistentListOf(),
    val countdowns:    ImmutableList<EventEntity>          = persistentListOf(),
    val budgets:       ImmutableList<BudgetEntity>         = persistentListOf(),
    val recurring:     ImmutableList<RecurringRuleEntity>  = persistentListOf(),
    val bills:         ImmutableList<BillEntity>           = persistentListOf(),
    val goals:         ImmutableList<GoalEntity>           = persistentListOf(),
    val incomes:       ImmutableList<IncomeEntity>         = persistentListOf(),
    val loans:         ImmutableList<FulizaLoanEntity>     = persistentListOf(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
    private val transactionDao: TransactionDao,
    private val taskDao:        TaskDao,
    private val eventDao:       EventDao,
    private val budgetDao:      BudgetDao,
    private val incomeDao:      IncomeDao,
    private val plannerDao:     PlannerDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    companion object {
        private const val MAX_RECENT_SEARCHES = 5
    }

    // Persisted for the lifetime of the ViewModel (nav back-stack entry) so
    // recent searches survive navigation. Capped at MAX_RECENT_SEARCHES.
    private val _recentSearches = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    val recentSearches: StateFlow<ImmutableList<String>> = _recentSearches.asStateFlow()

    fun addToRecent(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = (_recentSearches.value.filter { it != trimmed } + trimmed)
            .takeLast(MAX_RECENT_SEARCHES)
            .reversed()
            .toImmutableList()
        _recentSearches.value = updated
    }

    fun removeFromRecent(query: String) {
        _recentSearches.value = _recentSearches.value
            .filter { it != query }
            .toImmutableList()
    }

    fun clearRecent() {
        _recentSearches.value = persistentListOf()
    }

    init {
        // BUG-10: re-search when EITHER query OR activeTab changes
        _uiState
            .map { it.query to it.activeTab }
            .distinctUntilChanged()
            .debounce(300)
            .onEach { (q, _) -> if (q.length >= 2) search(q) else clearResults() }
            .launchIn(viewModelScope)
    }

    fun updateQuery(q: String) {
        _uiState.update { it.copy(query = q, isLoading = q.length >= 2) }
    }

    fun setTab(tab: SearchTab) = _uiState.update { it.copy(activeTab = tab) }

    fun clearQuery() = _uiState.update { current ->
        current.copy(
            query        = "",
            isLoading    = false,
            transactions = persistentListOf(),
            tasks        = persistentListOf(),
            events       = persistentListOf(),
            birthdays    = persistentListOf(),
            anniversaries = persistentListOf(),
            countdowns   = persistentListOf(),
            budgets      = persistentListOf(),
            recurring    = persistentListOf(),
            bills        = persistentListOf(),
            goals        = persistentListOf(),
            incomes      = persistentListOf(),
            loans        = persistentListOf(),
            // activeTab preserved — user's tab selection should not reset on clear
        )
    }

    private fun clearResults() {
        _uiState.update { it.copy(
            isLoading = false,
            transactions = persistentListOf(),
            tasks = persistentListOf(),
            events = persistentListOf(),
            birthdays = persistentListOf(),
            anniversaries = persistentListOf(),
            countdowns = persistentListOf(),
            budgets = persistentListOf(),
            recurring = persistentListOf(),
            bills = persistentListOf(),
            goals = persistentListOf(),
            incomes = persistentListOf(),
            loans = persistentListOf(),
        ) }
    }

    private fun search(q: String) {
        val tab = _uiState.value.activeTab
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val lq = q.lowercase()

            // BUG-10: only query DAOs relevant to the active tab — skip the rest
            val needsAll    = tab == SearchTab.All
            val needsTx     = needsAll || tab == SearchTab.Transactions
            val needsTasks  = needsAll || tab == SearchTab.Tasks
            val needsEvents = needsAll || tab == SearchTab.Events || tab == SearchTab.Birthdays ||
                              tab == SearchTab.Anniversaries || tab == SearchTab.Countdowns
            val needsBudget = needsAll || tab == SearchTab.Budgets
            val needsIncome = needsAll || tab == SearchTab.Incomes
            val needsPlan   = needsAll || tab == SearchTab.Recurring || tab == SearchTab.Bills ||
                              tab == SearchTab.Goals || tab == SearchTab.Loans

            val txs = if (needsTx) {
                transactionDao.getFiltered(
                search = q, category = "all", type = null, status = null,
                startDate = null, endDate = null, limit = 25, offset = 0,
            )
            } else {
                emptyList()
            }
            val tasks = if (needsTasks) taskDao.search(q, 25) else emptyList()
            val events = if (needsEvents) eventDao.search(q, 100) else emptyList()
            val budgets = if (needsBudget) budgetDao.search(q, 25) else emptyList()
            val incomes = if (needsIncome) incomeDao.search(q, 25) else emptyList()
            val recurring = if (needsPlan) {
                plannerDao.getAllRules().filter {
                it.title.lowercase().contains(lq) || (it.category?.lowercase()?.contains(lq) == true)
            }.take(25)
            } else {
                emptyList()
            }
            val bills = if (needsPlan) {
                plannerDao.getAllBills().filter {
                it.title.lowercase().contains(lq) || (it.notes?.lowercase()?.contains(lq) == true)
            }.take(25)
            } else {
                emptyList()
            }
            val goals = if (needsPlan) {
                plannerDao.getAllGoals().filter {
                it.title.lowercase().contains(lq) || (it.description?.lowercase()?.contains(lq) == true)
            }.take(25)
            } else {
                emptyList()
            }
            val loans = if (needsPlan) {
                plannerDao.getAllLoans().filter {
                (it.drawCode?.lowercase()?.contains(lq) == true)
            }.take(25)
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    transactions = txs.toImmutableList(),
                    tasks = tasks.toImmutableList(),
                    events = events.filter { e -> e.type == "event" }.toImmutableList(),
                    birthdays = events.filter { e -> e.type == "birthday" }.toImmutableList(),
                    anniversaries = events.filter { e -> e.type == "anniversary" }.toImmutableList(),
                    countdowns = events.filter { e -> e.type == "countdown" }.toImmutableList(),
                    budgets = budgets.toImmutableList(),
                    incomes = incomes.toImmutableList(),
                    recurring = recurring.toImmutableList(),
                    bills = bills.toImmutableList(),
                    goals = goals.toImmutableList(),
                    loans = loans.toImmutableList(),
                )
            }
        }
    }
}
