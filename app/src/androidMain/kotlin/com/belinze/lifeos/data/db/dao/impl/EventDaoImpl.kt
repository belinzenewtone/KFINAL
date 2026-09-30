package com.belinze.lifeos.data.db.dao.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.belinze.lifeos.data.db.EventQueries
import com.belinze.lifeos.data.db.Events
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.entity.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventDaoImpl(private val q: EventQueries) : EventDao {
    override suspend fun getFrom(fromDate: String): List<EventEntity> =
        q.getFrom(fromDate).executeAsList().map { it.toEntity() }

    override suspend fun getAll(): List<EventEntity> =
        q.getAll().executeAsList().map { it.toEntity() }

    override suspend fun getById(id: String): EventEntity? =
        q.getById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun search(q2: String, limit: Int): List<EventEntity> =
        q.search(q2, limit.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun getInRange(startDate: String, endDate: String): List<EventEntity> =
        q.getInRange(startDate, endDate).executeAsList().map { it.toEntity() }

    override suspend fun getNextUpcoming(today: String): EventEntity? =
        q.getNextUpcoming(today).executeAsOneOrNull()?.toEntity()

    override fun observeAll(): Flow<List<EventEntity>> =
        q.observeAll().asFlow().mapToList(Dispatchers.IO).map { list -> list.map { it.toEntity() } }

    override suspend fun insert(event: EventEntity) = q.insertOrReplace(
        id                        = event.id,
        title                     = event.title,
        description               = event.description,
        date                      = event.date,
        endDate                   = event.endDate,
        type                      = event.type,
        kind                      = event.kind,
        importance                = event.importance,
        status                    = event.status,
        hasReminder               = event.hasReminder.toLong(),
        reminderMinutesBefore     = event.reminderMinutesBefore?.toLong(),
        reminderOffsets           = event.reminderOffsets,
        reminderTimeOfDayMinutes  = event.reminderTimeOfDayMinutes?.toLong(),
        allDay                    = event.allDay.toLong(),
        repeatRule                = event.repeatRule,
        repeatEndDate             = event.repeatEndDate,
        location                  = event.location,
        guests                    = event.guests,
        timeZoneId                = event.timeZoneId,
        alarmEnabled              = event.alarmEnabled.toLong(),
        createdAt                 = event.createdAt,
        updatedAt                 = event.updatedAt,
        syncState                 = event.syncState,
        recordSource              = event.recordSource,
        deletedAt                 = event.deletedAt,
        revision                  = event.revision.toLong(),
        userId                    = event.userId,
    )

    override suspend fun update(event: EventEntity) = insert(event)

    override suspend fun softDelete(id: String, timestamp: String) =
        q.softDelete(timestamp, id)
}

private fun Events.toEntity() = EventEntity(
    id                       = id,
    title                    = title,
    description              = description,
    date                     = date,
    endDate                  = end_date,
    type                     = type,
    kind                     = kind,
    importance               = importance,
    status                   = status,
    hasReminder              = has_reminder.toInt(),
    reminderMinutesBefore    = reminder_minutes_before?.toInt(),
    reminderOffsets          = reminder_offsets,
    reminderTimeOfDayMinutes = reminder_time_of_day_minutes?.toInt(),
    allDay                   = all_day.toInt(),
    repeatRule               = repeat_rule,
    repeatEndDate            = repeat_end_date,
    location                 = location,
    guests                   = guests,
    timeZoneId               = time_zone_id,
    alarmEnabled             = alarm_enabled.toInt(),
    createdAt                = created_at,
    updatedAt                = updated_at,
    syncState                = sync_state,
    recordSource             = record_source,
    deletedAt                = deleted_at,
    revision                 = revision.toInt(),
    userId                   = user_id,
)
