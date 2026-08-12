package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.entity.TaskEntity
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// TaskViewModel
//
// Mirrors useTaskStore from the RN app.
// Manages task list (all + filter by status/priority), upcoming tasks for
// the Home screen widget, and the add/edit form.
// ─────────────────────────────────────────────────────────────────────────────

enum class TaskFilter { All, Active, Completed, Overdue }
enum class TaskSort { Deadline, CreatedAt, Priority }

data class TaskUiState(
    val isLoading:  Boolean           = true,
    val tasks:      List<TaskEntity>  = emptyList(),
    val upcoming:   List<TaskEntity>  = emptyList(),  // Home widget feed
    val filter:     TaskFilter        = TaskFilter.Active,
    val sort:       TaskSort          = TaskSort.Deadline,
    val pendingCount: Int             = 0,
    val error:      String?           = null,
)

data class TaskFormState(
    val id:          String?  = null,
    val title:       String   = "",
    val notes:       String   = "",
    val priority:    String   = "medium",   // low | medium | high
    val deadline:    String?  = null,
    val alarmEnabled: Boolean = false,
    val isSaving:    Boolean  = false,
    val error:       String?  = null,
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val dao: TaskDao,
) : ViewModel() {

    private val _uiState   = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    init {
        // Observe live changes from DB (e.g. from notification-triggered status change)
        dao.observeAll()
            .onEach { all -> applyFilter(all) }
            .launchIn(viewModelScope)

        loadUpcoming()
        loadPendingCount()
    }

    // ─── List & filter ────────────────────────────────────────────────────────

    private fun applyFilter(all: List<TaskEntity>) {
        val nowIso = nowIso()
        val filtered = when (_uiState.value.filter) {
            TaskFilter.All       -> all
            TaskFilter.Active    -> all.filter { it.status == "active" }
            TaskFilter.Completed -> all.filter { it.status == "completed" }
            TaskFilter.Overdue   -> all.filter {
                it.status == "active" && it.deadline != null && it.deadline < nowIso
            }
        }
        val sorted = when (_uiState.value.sort) {
            TaskSort.Deadline   -> filtered.sortedWith(compareBy(nullsLast()) { it.deadline })
            TaskSort.CreatedAt  -> filtered.sortedByDescending { it.createdAt }
            TaskSort.Priority   -> {
                val order = mapOf("high" to 0, "medium" to 1, "low" to 2)
                filtered.sortedBy { order[it.priority] ?: 99 }
            }
        }
        _uiState.update { it.copy(isLoading = false, tasks = sorted) }
    }

    fun setFilter(f: TaskFilter) {
        _uiState.update { it.copy(filter = f) }
        viewModelScope.launch {
            applyFilter(dao.getAll())
        }
    }

    fun setSort(s: TaskSort) {
        _uiState.update { it.copy(sort = s) }
        viewModelScope.launch {
            applyFilter(dao.getAll())
        }
    }

    private fun loadUpcoming() {
        viewModelScope.launch {
            // Next 7 days
            val until = java.time.LocalDate.now()
                .plusDays(7).atStartOfDay(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val tasks = dao.getUpcoming(until, 5)
            _uiState.update { it.copy(upcoming = tasks) }
        }
    }

    private fun loadPendingCount() {
        viewModelScope.launch {
            val count = dao.countPending()
            _uiState.update { it.copy(pendingCount = count) }
        }
    }

    // ─── Form ─────────────────────────────────────────────────────────────────

    fun openForm(taskId: String? = null) {
        viewModelScope.launch {
            val entity = taskId?.let { dao.getById(it) }
            _formState.update {
                if (entity == null) TaskFormState()
                else TaskFormState(
                    id          = entity.id,
                    title       = entity.title,
                    notes       = entity.notes ?: "",
                    priority    = entity.priority,
                    deadline    = entity.deadline,
                    alarmEnabled = entity.alarmEnabled,
                )
            }
        }
    }

    fun updateTitle(v: String)       = _formState.update { it.copy(title = v) }
    fun updateNotes(v: String)       = _formState.update { it.copy(notes = v) }
    fun updatePriority(v: String)    = _formState.update { it.copy(priority = v) }
    fun updateDeadline(v: String?)   = _formState.update { it.copy(deadline = v) }
    fun toggleAlarm(v: Boolean)      = _formState.update { it.copy(alarmEnabled = v) }

    fun saveForm(onSuccess: () -> Unit) {
        val form = _formState.value
        if (form.title.isBlank()) {
            _formState.update { it.copy(error = "Title cannot be empty") }
            return
        }
        _formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val existing = form.id?.let { dao.getById(it) }
                val entity   = (existing ?: TaskEntity(
                    id        = UUID.randomUUID().toString(),
                    title     = "",
                    status    = "active",
                    priority  = "medium",
                    createdAt = nowIso(),
                    updatedAt = nowIso(),
                )).copy(
                    title        = form.title,
                    notes        = form.notes.ifBlank { null },
                    priority     = form.priority,
                    deadline     = form.deadline,
                    alarmEnabled = form.alarmEnabled,
                    updatedAt    = nowIso(),
                )
                dao.insert(entity)
                loadPendingCount()
                _formState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun complete(id: String) {
        viewModelScope.launch {
            val entity = dao.getById(id) ?: return@launch
            dao.update(entity.copy(status = "completed", updatedAt = nowIso()))
            loadPendingCount()
        }
    }

    fun softDelete(id: String) {
        viewModelScope.launch {
            dao.softDelete(id, nowIso())
            loadPendingCount()
        }
    }
}
