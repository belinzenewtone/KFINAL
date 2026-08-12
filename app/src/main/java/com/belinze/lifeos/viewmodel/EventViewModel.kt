package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.entity.EventEntity
import com.belinze.lifeos.util.Haptics
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// EventViewModel
//
// Mirrors useEventStore from the RN app.
// Manages calendar events — full list, day/range queries, form state.
// ─────────────────────────────────────────────────────────────────────────────

/** Calendar view modes matching CalendarScreen tabs */
enum class CalendarView { Month, Week, Agenda }

data class EventUiState(
    val isLoading:     Boolean          = true,
    val events:        List<EventEntity> = emptyList(),
    val selectedDay:   LocalDate        = LocalDate.now(),
    val calendarView:  CalendarView     = CalendarView.Month,
    val nextEvent:     EventEntity?     = null,  // home-screen widget
    val error:         String?          = null,
)

data class EventFormState(
    val id:          String?  = null,
    val title:       String   = "",
    val description: String   = "",
    val date:        String   = nowIso(),
    val endDate:     String?  = null,
    val allDay:      Boolean  = false,
    val type:        String   = "event",  // event | birthday | anniversary | countdown
    val repeatRule:  String?  = null,
    val location:    String   = "",
    val isSaving:    Boolean  = false,
    val error:       String?  = null,
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val dao: EventDao,
) : ViewModel() {

    private val _uiState   = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(EventFormState())
    val formState: StateFlow<EventFormState> = _formState.asStateFlow()

    private val zone = ZoneId.systemDefault()
    private val isoDtFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    init {
        dao.observeAll()
            .onEach { all -> _uiState.update { it.copy(isLoading = false, events = all) } }
            .launchIn(viewModelScope)

        loadNextEvent()
    }

    // ─── View controls ────────────────────────────────────────────────────────

    fun selectDay(day: LocalDate) = _uiState.update { it.copy(selectedDay = day) }

    fun setCalendarView(view: CalendarView) = _uiState.update { it.copy(calendarView = view) }

    /** Events for the currently selected day */
    fun eventsForDay(day: LocalDate): List<EventEntity> {
        val prefix = day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return _uiState.value.events.filter { it.date.startsWith(prefix) }
    }

    private fun loadNextEvent() {
        viewModelScope.launch {
            val today = LocalDate.now(zone)
                .atStartOfDay(zone).format(isoDtFmt)
            _uiState.update { it.copy(nextEvent = dao.getNextUpcoming(today)) }
        }
    }

    // ─── Form ─────────────────────────────────────────────────────────────────

    fun openForm(eventId: String? = null, type: String = "event") {
        viewModelScope.launch {
            val entity = eventId?.let { dao.getById(it) }
            _formState.update {
                if (entity == null) EventFormState(type = type)
                else EventFormState(
                    id          = entity.id,
                    title       = entity.title,
                    description = entity.description ?: "",
                    date        = entity.date,
                    endDate     = entity.endDate,
                    allDay      = entity.allDay != 0,
                    type        = entity.type,
                    repeatRule  = entity.repeatRule,
                    location    = entity.location ?: "",
                )
            }
        }
    }

    fun updateTitle(v: String)      = _formState.update { it.copy(title = v) }
    fun updateDescription(v: String) = _formState.update { it.copy(description = v) }
    fun updateDate(v: String)       = _formState.update { it.copy(date = v) }
    fun updateEndDate(v: String?)   = _formState.update { it.copy(endDate = v) }
    fun updateAllDay(v: Boolean)    = _formState.update { it.copy(allDay = v) }
    fun updateType(v: String)       = _formState.update { it.copy(type = v) }
    fun updateRepeatRule(v: String?) = _formState.update { it.copy(repeatRule = v) }
    fun updateLocation(v: String)   = _formState.update { it.copy(location = v) }

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
                val entity   = (existing ?: EventEntity(
                    id        = UUID.randomUUID().toString(),
                    title     = "",
                    date      = nowIso(),
                    type      = "event",
                    status    = "active",
                    createdAt = nowIso(),
                    updatedAt = nowIso(),
                )).copy(
                    title       = form.title,
                    description = form.description.ifBlank { null },
                    date        = form.date,
                    endDate     = form.endDate,
                    allDay      = if (form.allDay) 1 else 0,
                    type        = form.type,
                    repeatRule  = form.repeatRule ?: "none",
                    location    = form.location.ifBlank { null },
                    updatedAt   = nowIso(),
                )
                dao.insert(entity)
                loadNextEvent()
                _formState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun softDelete(id: String) {
        viewModelScope.launch {
            dao.softDelete(id, nowIso())
            Haptics.warning()
            loadNextEvent()
        }
    }
}
