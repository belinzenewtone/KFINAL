package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.EventEntity
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

// ─────────────────────────────────────────────────────────────────────────────
// SearchViewModel
//
// Mirrors the Search screen store. Cross-entity search over transactions,
// tasks, and events — debounced 350ms, runs concurrently in 3 coroutines.
// ─────────────────────────────────────────────────────────────────────────────

enum class SearchTab { All, Transactions, Tasks, Events }

data class SearchUiState(
    val query:        String                   = "",
    val activeTab:    SearchTab                = SearchTab.All,
    val isLoading:    Boolean                  = false,

    val transactions: List<TransactionEntity>  = emptyList(),
    val tasks:        List<TaskEntity>         = emptyList(),
    val events:       List<EventEntity>        = emptyList(),

    val totalResults: Int                      = 0,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val taskDao:        TaskDao,
    private val eventDao:       EventDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Debounced query watcher
    init {
        _uiState
            .map { it.query }
            .distinctUntilChanged()
            .debounce(350)
            .onEach { q -> if (q.length >= 2) search(q) else clearResults() }
            .launchIn(viewModelScope)
    }

    fun updateQuery(q: String) {
        _uiState.update { it.copy(query = q, isLoading = q.length >= 2) }
    }

    fun setTab(tab: SearchTab) = _uiState.update { it.copy(activeTab = tab) }

    fun clearQuery() {
        _uiState.update { SearchUiState() }
    }

    private fun clearResults() {
        _uiState.update {
            it.copy(
                isLoading    = false,
                transactions = emptyList(),
                tasks        = emptyList(),
                events       = emptyList(),
                totalResults = 0,
            )
        }
    }

    private fun search(q: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Run the 3 entity searches as parallel coroutine children
            val txsDeferred  = launch {
                val txs = transactionDao.getFiltered(
                    search    = q,
                    category  = "all",
                    type      = null,
                    status    = null,
                    startDate = null,
                    endDate   = null,
                    limit     = 25,
                    offset    = 0,
                )
                _uiState.update { it.copy(transactions = txs) }
            }

            val tasksDeferred = launch {
                val all  = taskDao.getAll()
                val lq   = q.lowercase()
                val hits = all.filter { t ->
                    t.title.lowercase().contains(lq) ||
                    t.notes?.lowercase()?.contains(lq) == true
                }.take(25)
                _uiState.update { it.copy(tasks = hits) }
            }

            val eventsDeferred = launch {
                val all  = eventDao.getAll()
                val lq   = q.lowercase()
                val hits = all.filter { e ->
                    e.title.lowercase().contains(lq) ||
                    e.description?.lowercase()?.contains(lq) == true ||
                    e.location?.lowercase()?.contains(lq) == true
                }.take(25)
                _uiState.update { it.copy(events = hits) }
            }

            // Wait for all three to finish
            txsDeferred.join()
            tasksDeferred.join()
            eventsDeferred.join()

            _uiState.update { s ->
                s.copy(
                    isLoading    = false,
                    totalResults = s.transactions.size + s.tasks.size + s.events.size,
                )
            }
        }
    }
}
