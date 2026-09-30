package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.LearningSessionDao
import com.belinze.lifeos.data.db.entity.LearningSessionEntity
import com.belinze.lifeos.util.Haptics
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.belinze.lifeos.util.newId
import kotlinx.datetime.Clock

// ─────────────────────────────────────────────────────────────────────────────
// LearningViewModel — LE-1 / LE-10
//
// Provides:
//  - sessions: list of LearningSessionEntity from Room (live)
//  - monthlyHours: SUM(duration_minutes WHERE is_completed=1 AND current month)/60
//  - completed count
// ─────────────────────────────────────────────────────────────────────────────

class LearningViewModel
constructor(
    private val dao: LearningSessionDao,
) : ViewModel() {
    @Immutable
    data class UiState(
        val sessions:     ImmutableList<LearningSessionEntity> = persistentListOf(),
        val monthlyHours: Float                       = 0f,
        val isLoading:    Boolean                     = true,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.observeAll(),
                dao.observeMonthlyMinutes(),
            ) { sessions, minutes ->
                UiState(
                    sessions     = sessions.toImmutableList(),
                    monthlyHours = minutes / 60f,
                    isLoading    = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleCompleted(id: String, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            dao.setCompleted(id = id, done = if (currentlyCompleted) 0 else 1)
            Haptics.success()
        }
    }

    fun logSession(
        title:       String,
        category:    String,
        duration:    Int,
        description: String = "",
    ) {
        viewModelScope.launch {
            val now = Clock.System.now().toString()
            dao.insert(
                LearningSessionEntity(
                    id              = newId(),
                    title           = title,
                    category        = category,
                    description     = description.ifBlank { null },
                    durationMinutes = duration,
                    isCompleted     = 0,
                    loggedAt        = now,
                    createdAt       = now,
                    updatedAt       = now,
                )
            )
            Haptics.success()
        }
    }

    fun updateSession(
        id:          String,
        title:       String,
        category:    String,
        duration:    Int,
        description: String = "",
    ) {
        viewModelScope.launch {
            val existing = dao.getById(id) ?: return@launch
            dao.update(
                existing.copy(
                    title           = title,
                    category        = category,
                    durationMinutes = duration,
                    description     = description.ifBlank { null },
                    updatedAt       = Clock.System.now().toString(),
                )
            )
            Haptics.success()
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            val now = Clock.System.now().toString()
            dao.softDelete(id = id, now = now)
            Haptics.warning()
        }
    }
}
