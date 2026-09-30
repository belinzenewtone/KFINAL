package com.belinze.lifeos.data.db.dao.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.belinze.lifeos.data.db.GetUpcoming
import com.belinze.lifeos.data.db.TaskQueries
import com.belinze.lifeos.data.db.Tasks
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskDaoImpl(private val q: TaskQueries) : TaskDao {
    override suspend fun getAll(): List<TaskEntity> =
        q.getAll().executeAsList().map { it.toEntity() }

    override suspend fun getUpcoming(dueBefore: String, limit: Int): List<TaskEntity> =
        q.getUpcoming(dueBefore, limit.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun countPending(): Int =
        q.countPending().executeAsOne().toInt()

    override suspend fun countDueToday(startOfDay: String, endOfDay: String): Int =
        q.countDueToday(startOfDay, endOfDay).executeAsOne().toInt()

    override suspend fun getById(id: String): TaskEntity? =
        q.getById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun search(q2: String, limit: Int): List<TaskEntity> =
        q.search(q2, limit.toLong()).executeAsList().map { it.toEntity() }

    override fun observeAll(): Flow<List<TaskEntity>> =
        q.observeAll().asFlow().mapToList(Dispatchers.IO).map { list -> list.map { it.toEntity() } }

    override suspend fun insert(task: TaskEntity) = q.insertOrReplace(
        id                = task.id,
        title             = task.title,
        description       = task.description,
        priority          = task.priority,
        deadline          = task.deadline,
        status            = task.status,
        completedAt       = task.completedAt,
        createdAt         = task.createdAt,
        updatedAt         = task.updatedAt,
        reminderOffsets   = task.reminderOffsets,
        alarmEnabled      = task.alarmEnabled.toLong(),
        syncState         = task.syncState,
        recordSource      = task.recordSource,
        deletedAt         = task.deletedAt,
        revision          = task.revision.toLong(),
        userId            = task.userId,
        timeSpentSeconds  = task.timeSpentSeconds.toLong(),
    )

    override suspend fun update(task: TaskEntity) = insert(task)

    override suspend fun softDelete(id: String, timestamp: String) =
        q.softDelete(timestamp, id)

    override suspend fun countCompletedSince(since: String): Int =
        q.countCompletedSince(since).executeAsOne().toInt()

    override suspend fun countAllPending(): Int =
        q.countAllPending().executeAsOne().toInt()
}

private fun GetUpcoming.toEntity() = TaskEntity(
    id               = id,
    title            = title,
    description      = description,
    priority         = priority,
    deadline         = deadline,
    status           = status,
    completedAt      = completed_at,
    createdAt        = created_at,
    updatedAt        = updated_at,
    reminderOffsets  = reminder_offsets,
    alarmEnabled     = alarm_enabled.toInt(),
    syncState        = sync_state,
    recordSource     = record_source,
    deletedAt        = deleted_at,
    revision         = revision.toInt(),
    userId           = user_id,
    timeSpentSeconds = time_spent_seconds.toInt(),
)

private fun Tasks.toEntity() = TaskEntity(
    id               = id,
    title            = title,
    description      = description,
    priority         = priority,
    deadline         = deadline,
    status           = status,
    completedAt      = completed_at,
    createdAt        = created_at,
    updatedAt        = updated_at,
    reminderOffsets  = reminder_offsets,
    alarmEnabled     = alarm_enabled.toInt(),
    syncState        = sync_state,
    recordSource     = record_source,
    deletedAt        = deleted_at,
    revision         = revision.toInt(),
    userId           = user_id,
    timeSpentSeconds = time_spent_seconds.toInt(),
)
