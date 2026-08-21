package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
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
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// ─────────────────────────────────────────────────────────────────────────────
// EventViewModel
//
// Mirrors useEventStore + TaskEventForm from the React app.
// Full parity with TaskEventForm.tsx — all type-specific fields.
// ─────────────────────────────────────────────────────────────────────────────

/** Calendar view modes matching CalendarScreen tabs */
enum class CalendarView { Month, Week, Agenda }

@Immutable
data class EventUiState(
    val isLoading:    Boolean                    = true,
    val events:       ImmutableList<EventEntity> = persistentListOf(),
    val selectedDay:  LocalDate         = LocalDate.now(),
    val calendarView: CalendarView      = CalendarView.Month,
    val nextEvent:    EventEntity?      = null,
    val error:        String?           = null,
)

/**
 * Full form state matching TaskEventForm.tsx fields.
 *
 * - [startDateStr] "YYYY-MM-DD"  — date part of event start
 * - [startTimeStr] "HH:mm"       — time part of event start (empty = all-day)
 * - [endDateStr]   "YYYY-MM-DD"  — date part of event end (null = no end)
 * - [endTimeStr]   "HH:mm"       — time part of event end
 * - [kind]         event sub-category: meeting|task|reminder|goal|other
 * - [importance]   low|medium|high  (priority chips)
 * - [reminderOffsets] list of minutes-before values from presets
 * - [addYear]      birthday: whether birth year is captured
 * - [countdownReminderTime] "HH:mm" — for countdown "Remind at"
 * - [remindBefore] countdown: "Remind 3 days before" toggle
 */
@Immutable
data class EventFormState(
    val id:                   String?      = null,
    val type:                 String       = "event",   // task|event|birthday|anniversary|countdown
    val title:                String       = "",
    val description:          String       = "",
    // ─ Date / time ─
    val startDateStr:         String       = LocalDate.now().toString(),  // "YYYY-MM-DD"
    val startTimeStr:         String       = "08:00",                      // "HH:mm"
    val endDateStr:           String?      = null,
    val endTimeStr:           String       = "09:00",
    val allDay:               Boolean      = false,
    // ─ Repeat ─
    val repeatRule:           String       = "none",
    val repeatEndDate:        String?      = null,
    // ─ Event-specific ─
    val location:             String       = "",
    val guests:               ImmutableList<String> = persistentListOf(),
    val timeZoneId:           String       = ZoneId.systemDefault().id,
    val kind:                 String       = "other",       // event category dropdown
    val importance:           String       = "medium",      // priority
    val reminderOffsets:      ImmutableList<Int> = persistentListOf(),   // minutes-before list
    val alarmEnabled:         Boolean      = false,
    // ─ Birthday ─
    val addYear:              Boolean      = false,
    // ─ Countdown ─
    val countdownReminderTime: String      = "08:00",       // "HH:mm"
    val remindBefore:         Boolean      = false,
    // ─ UI ─
    val isSaving:             Boolean      = false,
    val error:                String?      = null,
)

@HiltViewModel
class EventViewModel
    @Inject
    constructor(
    private val dao: EventDao,
) : ViewModel() {
    private val _uiState   = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(EventFormState())
    val formState: StateFlow<EventFormState> = _formState.asStateFlow()

    private val zone      = ZoneId.systemDefault()
    private val dateFmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt   = DateTimeFormatter.ofPattern("HH:mm")
    private val isoOffFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    init {
        dao.observeAll()
            .onEach { all -> _uiState.update { it.copy(isLoading = false, events = all.toImmutableList()) } }
            .launchIn(viewModelScope)

        loadNextEvent()
    }

    // ─── View controls ────────────────────────────────────────────────────────

    fun selectDay(day: LocalDate) = _uiState.update { it.copy(selectedDay = day) }

    fun setCalendarView(view: CalendarView) = _uiState.update { it.copy(calendarView = view) }

    fun eventsForDay(day: LocalDate): List<EventEntity> {
        val prefix = day.format(dateFmt)
        return _uiState.value.events.filter { it.date.startsWith(prefix) }
    }

    private fun loadNextEvent() {
        viewModelScope.launch {
            val today = LocalDate.now(zone).atStartOfDay(zone).format(isoOffFmt)
            _uiState.update { it.copy(nextEvent = dao.getNextUpcoming(today)) }
        }
    }

    // ─── Form lifecycle ───────────────────────────────────────────────────────

    fun openForm(eventId: String? = null, type: String = "event") {
        viewModelScope.launch {
            val entity = eventId?.let { dao.getById(it) }
            if (entity == null) {
                _formState.value = EventFormState(type = type)
            } else {
                _formState.value = entityToFormState(entity)
            }
        }
    }

    // ─── Update functions ─────────────────────────────────────────────────────

    fun updateType(v: String) = _formState.update { it.copy(type = v, error = null) }

    fun updateTitle(v: String) = _formState.update { it.copy(title = v) }

    fun updateDescription(v: String) = _formState.update { it.copy(description = v) }

    fun updateStartDate(v: String) = _formState.update { it.copy(startDateStr = v) }

    fun updateStartTime(v: String) = _formState.update { it.copy(startTimeStr = v) }

    fun updateEndDate(v: String?) = _formState.update { it.copy(endDateStr = v) }

    fun updateEndTime(v: String) = _formState.update { it.copy(endTimeStr = v) }

    fun updateAllDay(v: Boolean) = _formState.update { it.copy(allDay = v) }

    fun updateRepeatRule(v: String) = _formState.update { it.copy(repeatRule = v) }

    fun updateRepeatEndDate(v: String?) = _formState.update { it.copy(repeatEndDate = v) }

    fun updateLocation(v: String) = _formState.update { it.copy(location = v) }

    fun updateTimeZoneId(v: String) = _formState.update { it.copy(timeZoneId = v) }

    fun updateKind(v: String) = _formState.update { it.copy(kind = v) }

    fun updateImportance(v: String) = _formState.update { it.copy(importance = v) }

    fun updateAlarmEnabled(v: Boolean) = _formState.update { it.copy(alarmEnabled = v) }

    fun updateAddYear(v: Boolean) = _formState.update { it.copy(addYear = v) }

    fun updateCountdownReminderTime(v: String) = _formState.update { it.copy(countdownReminderTime = v) }

    fun updateRemindBefore(v: Boolean) = _formState.update { it.copy(remindBefore = v) }

    fun addGuest(email: String) {
        _formState.update { s ->
            if (email.isBlank() || s.guests.contains(email)) {
                s
            } else {
                s.copy(guests = (s.guests + email).toImmutableList())
            }
        }
    }

    fun removeGuest(email: String) =
        _formState.update { it.copy(guests = it.guests.filter { g -> g != email }.toImmutableList()) }

    fun toggleReminderOffset(minutesBefore: Int) {
        _formState.update { s ->
            val current = s.reminderOffsets
            val updated = if (current.contains(minutesBefore)) {
                current.filter { it != minutesBefore }.toImmutableList()
            } else {
                (current + minutesBefore).sorted().toImmutableList()
            }
            s.copy(reminderOffsets = updated)
        }
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

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
                val entity   = formStateToEntity(form, existing)
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

    // ─── Entity ↔ FormState helpers ───────────────────────────────────────────

    private fun entityToFormState(e: EventEntity): EventFormState {
        val (startDate, startTime) = parseIsoDateTimeParts(e.date)
        val (endDate, endTime)     = if (e.endDate != null) parseIsoDateTimeParts(e.endDate) else null to "09:00"
        val offsets = parseJsonIntArray(e.reminderOffsets)
        val remTimeMinutes = e.reminderTimeOfDayMinutes
        return EventFormState(
            id                   = e.id,
            type                 = e.type,
            title                = e.title,
            description          = e.description ?: "",
            startDateStr         = startDate,
            startTimeStr         = startTime,
            endDateStr           = endDate,
            endTimeStr           = endTime,
            allDay               = e.allDay != 0,
            repeatRule           = e.repeatRule,
            repeatEndDate        = e.repeatEndDate,
            location             = e.location ?: "",
            guests               = parseJsonStringArray(e.guests).toImmutableList(),
            timeZoneId           = e.timeZoneId,
            kind                 = e.kind,
            importance           = e.importance,
            reminderOffsets      = offsets.toImmutableList(),
            alarmEnabled         = e.alarmEnabled != 0,
            addYear              = e.reminderMinutesBefore != null && e.type == "birthday",
            countdownReminderTime = if (remTimeMinutes != null) {
                minutesToHHmm(remTimeMinutes)
            } else {
                "08:00"
            },
            remindBefore         = e.type == "countdown" && offsets.contains(3 * 24 * 60),
        )
    }

    private fun formStateToEntity(form: EventFormState, existing: EventEntity?): EventEntity {
        val startIso = buildIso(form.startDateStr, if (form.allDay) "00:00" else form.startTimeStr, form.timeZoneId)
        val endIso   = form.endDateStr?.let { buildIso(it, if (form.allDay) "23:59" else form.endTimeStr, form.timeZoneId) }

        val guestsJson   = if (form.guests.isEmpty()) {
            null
        } else {
            JSONArray(form.guests).toString()
        }
        val offsetsJson  = if (form.reminderOffsets.isEmpty()) {
            null
        } else {
            JSONArray(form.reminderOffsets).toString()
        }
        val remTimeMin   = if (form.type == "countdown") {
            hhmmToMinutes(form.countdownReminderTime)
        } else {
            null
        }
        // For countdown "remindBefore 3 days": stored as 4320 min in offsets
        val finalOffsets = if (form.type == "countdown" && form.remindBefore) {
            val base = form.reminderOffsets.filter { it != 3 * 24 * 60 }
            JSONArray(base + 3 * 24 * 60).toString()
        } else {
            offsetsJson
        }

        val base = existing ?: EventEntity(
            id        = UUID.randomUUID().toString(),
            title     = "",
            date      = nowIso(),
            type      = "event",
            status    = "active",
            createdAt = nowIso(),
            updatedAt = nowIso(),
        )
        return base.copy(
            title                    = form.title,
            description              = form.description.ifBlank { null },
            date                     = startIso,
            endDate                  = endIso,
            allDay                   = if (form.allDay) 1 else 0,
            type                     = form.type,
            kind                     = form.kind,
            importance               = form.importance,
            repeatRule               = form.repeatRule,
            repeatEndDate            = form.repeatEndDate,
            location                 = form.location.ifBlank { null },
            guests                   = guestsJson,
            timeZoneId               = form.timeZoneId,
            reminderOffsets          = finalOffsets,
            reminderTimeOfDayMinutes = remTimeMin,
            reminderMinutesBefore    = if (form.type == "birthday" && form.addYear) 1 else null,
            alarmEnabled             = if (form.alarmEnabled) 1 else 0,
            updatedAt                = nowIso(),
        )
    }

    // ─── ISO parsing helpers ──────────────────────────────────────────────────

    /** Returns ("YYYY-MM-DD", "HH:mm") from any ISO date-time string. */
    private fun parseIsoDateTimeParts(iso: String): Pair<String, String> {
        return try {
            val zdt = ZonedDateTime.parse(iso, isoOffFmt)
            zdt.format(dateFmt) to zdt.format(timeFmt)
        } catch (_: Exception) {
            // Fallback: extract manually from prefix
            val datePart = iso.take(10)
            val timePart = if (iso.length >= 16) iso.substring(11, 16) else "00:00"
            datePart to timePart
        }
    }

    /** Build ISO offset date-time from "YYYY-MM-DD" + "HH:mm" + timeZoneId. */
    private fun buildIso(dateStr: String, timeStr: String, tzId: String): String {
        return try {
            val tz   = ZoneId.of(tzId)
            val date = LocalDate.parse(dateStr)
            val parts = timeStr.split(":")
            val hour  = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val min   = parts.getOrNull(1)?.toIntOrNull() ?: 0
            date.atTime(hour, min).atZone(tz).format(isoOffFmt)
        } catch (_: Exception) {
            "${dateStr}T$timeStr:00"
        }
    }

    private fun minutesToHHmm(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }

    private fun hhmmToMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun parseJsonIntArray(json: String?): List<Int> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getInt(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseJsonStringArray(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
