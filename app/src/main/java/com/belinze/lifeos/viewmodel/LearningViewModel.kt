package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.LearningSessionDao
import com.belinze.lifeos.data.db.entity.LearningSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// ─────────────────────────────────────────────────────────────────────────────
// LearningViewModel — LE-1 / LE-10
//
// Provides:
//  - sessions: list of LearningSessionEntity from Room (live)
//  - monthlyHours: SUM(duration_minutes WHERE is_completed=1 AND current month)/60
//  - completed count
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class LearningViewModel
    @Inject
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
        }
    }

    fun logSession(
        title:       String,
        category:    String,
        duration:    Int,
        description: String = "",
    ) {
        viewModelScope.launch {
            val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            dao.insert(
                LearningSessionEntity(
                    id              = UUID.randomUUID().toString(),
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
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            dao.softDelete(id = id, now = now)
        }
    }
}
