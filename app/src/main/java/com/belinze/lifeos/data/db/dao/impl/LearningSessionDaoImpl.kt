package com.belinze.lifeos.data.db.dao.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.belinze.lifeos.data.db.LearningSessionQueries
import com.belinze.lifeos.data.db.Learning_sessions
import com.belinze.lifeos.data.db.dao.LearningSessionDao
import com.belinze.lifeos.data.db.entity.LearningSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LearningSessionDaoImpl(private val q: LearningSessionQueries) : LearningSessionDao {

    override fun observeAll(): Flow<List<LearningSessionEntity>> =
        q.observeAll().asFlow().mapToList(Dispatchers.IO).map { list -> list.map { it.toEntity() } }

    override fun observeByCategory(category: String?): Flow<List<LearningSessionEntity>> =
        q.observeByCategory(category).asFlow().mapToList(Dispatchers.IO).map { list -> list.map { it.toEntity() } }

    override fun observeMonthlyMinutes(): Flow<Int> =
        q.observeMonthlyMinutes().asFlow().mapToOne(Dispatchers.IO).map { it.toInt() }

    override suspend fun insert(session: LearningSessionEntity) = q.insertOrReplace(
        id              = session.id,
        title           = session.title,
        category        = session.category,
        description     = session.description,
        durationMinutes = session.durationMinutes.toLong(),
        isCompleted     = session.isCompleted.toLong(),
        loggedAt        = session.loggedAt,
        createdAt       = session.createdAt,
        updatedAt       = session.updatedAt,
        deletedAt       = session.deletedAt,
    )

    override suspend fun update(session: LearningSessionEntity) = insert(session)

    override suspend fun setCompleted(id: String, done: Int) =
        q.setCompleted(done.toLong(), id)

    override suspend fun softDelete(id: String, now: String) =
        q.softDelete(now, id)

    override suspend fun getById(id: String): LearningSessionEntity? =
        q.getById(id).executeAsOneOrNull()?.toEntity()
}

private fun Learning_sessions.toEntity() = LearningSessionEntity(
    id              = id,
    title           = title,
    category        = category,
    description     = description,
    durationMinutes = duration_minutes.toInt(),
    isCompleted     = is_completed.toInt(),
    loggedAt        = logged_at,
    createdAt       = created_at,
    updatedAt       = updated_at,
    deletedAt       = deleted_at,
)
