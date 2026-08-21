package com.belinze.lifeos.viewmodel

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

data class SearchUiState(
    val query:         String                     = "",
    val activeTab:     SearchTab                  = SearchTab.All,
    val isLoading:     Boolean                    = false,
    val transactions:  List<TransactionEntity>    = emptyList(),
    val tasks:         List<TaskEntity>           = emptyList(),
    val events:        List<EventEntity>          = emptyList(),
    val birthdays:     List<EventEntity>          = emptyList(),
    val anniversaries: List<EventEntity>          = emptyList(),
    val countdowns:    List<EventEntity>          = emptyList(),
    val budgets:       List<BudgetEntity>         = emptyList(),
    val recurring:     List<RecurringRuleEntity>  = emptyList(),
    val bills:         List<BillEntity>           = emptyList(),
    val goals:         List<GoalEntity>           = emptyList(),
    val incomes:       List<IncomeEntity>         = emptyList(),
    val loans:         List<FulizaLoanEntity>     = emptyList(),
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

    init {
        _uiState
            .map { it.query }
            .distinctUntilChanged()
            .debounce(300)
            .onEach { q -> if (q.length >= 2) search(q) else clearResults() }
            .launchIn(viewModelScope)
    }

    fun updateQuery(q: String) {
        _uiState.update { it.copy(query = q, isLoading = q.length >= 2) }
    }

    fun setTab(tab: SearchTab) = _uiState.update { it.copy(activeTab = tab) }

    fun clearQuery() = _uiState.update { SearchUiState() }

    private fun clearResults() {
        _uiState.update { it.copy(
            isLoading = false,
            transactions = emptyList(),
            tasks = emptyList(),
            events = emptyList(),
            birthdays = emptyList(),
            anniversaries = emptyList(),
            countdowns = emptyList(),
            budgets = emptyList(),
            recurring = emptyList(),
            bills = emptyList(),
            goals = emptyList(),
            incomes = emptyList(),
            loans = emptyList(),
        ) }
    }

    private fun search(q: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val lq = q.lowercase()

            val txs = transactionDao.getFiltered(
                search = q, category = "all", type = null, status = null,
                startDate = null, endDate = null, limit = 25, offset = 0,
            )
            val tasks = taskDao.search(q, 25)
            val events = eventDao.search(q, 100)
            val budgets = budgetDao.search(q, 25)
            val incomes = incomeDao.search(q, 25)
            val recurring = plannerDao.getAllRules().filter {
                it.title.lowercase().contains(lq) || (it.category?.lowercase()?.contains(lq) == true)
            }.take(25)
            val bills = plannerDao.getAllBills().filter {
                it.title.lowercase().contains(lq) || (it.notes?.lowercase()?.contains(lq) == true)
            }.take(25)
            val goals = plannerDao.getAllGoals().filter {
                it.title.lowercase().contains(lq) || (it.description?.lowercase()?.contains(lq) == true)
            }.take(25)
            val loans = plannerDao.getAllLoans().filter {
                (it.drawCode?.lowercase()?.contains(lq) == true)
            }.take(25)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    transactions = txs,
                    tasks = tasks,
                    events = events.filter { e -> e.type == "event" },
                    birthdays = events.filter { e -> e.type == "birthday" },
                    anniversaries = events.filter { e -> e.type == "anniversary" },
                    countdowns = events.filter { e -> e.type == "countdown" },
                    budgets = budgets,
                    incomes = incomes,
                    recurring = recurring,
                    bills = bills,
                    goals = goals,
                    loans = loans,
                )
            }
        }
    }
}
