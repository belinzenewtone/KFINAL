package com.belinze.lifeos.data.db.dao.impl

import com.belinze.lifeos.data.db.IncomeQueries
import com.belinze.lifeos.data.db.Incomes
import com.belinze.lifeos.data.db.dao.IncomeDao
import com.belinze.lifeos.data.db.entity.IncomeEntity

class IncomeDaoImpl(private val q: IncomeQueries) : IncomeDao {

    override suspend fun getAll(): List<IncomeEntity> =
        q.getAll().executeAsList().map { it.toEntity() }

    override suspend fun getInRange(startDate: String, endDate: String): List<IncomeEntity> =
        q.getInRange(startDate, endDate).executeAsList().map { it.toEntity() }

    override suspend fun getTotalInRange(startDate: String, endDate: String): Double? =
        q.getTotalInRange(startDate, endDate).executeAsOneOrNull()?.SUM

    override suspend fun getById(id: String): IncomeEntity? =
        q.getById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun search(q2: String, limit: Int): List<IncomeEntity> =
        q.search(q2, limit.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun insert(income: IncomeEntity) = q.insertOrReplace(
        id           = income.id,
        amount       = income.amount,
        source       = income.source,
        date         = income.date,
        note         = income.note,
        isRecurring  = income.isRecurring.toLong(),
        frequency    = income.frequency,
        isActive     = income.isActive.toLong(),
        createdAt    = income.createdAt,
        updatedAt    = income.updatedAt,
        syncState    = income.syncState,
        recordSource = income.recordSource,
        deletedAt    = income.deletedAt,
        revision     = income.revision.toLong(),
        userId       = income.userId,
    )

    override suspend fun update(income: IncomeEntity) = insert(income)

    override suspend fun softDelete(id: String, timestamp: String) =
        q.softDelete(timestamp, id)

    override suspend fun updateActive(id: String, active: Int) =
        q.updateActive(active.toLong(), id)
}

private fun Incomes.toEntity() = IncomeEntity(
    id           = id,
    amount       = amount,
    source       = source,
    date         = date,
    note         = note,
    isRecurring  = is_recurring.toInt(),
    frequency    = frequency,
    isActive     = is_active.toInt(),
    createdAt    = created_at,
    updatedAt    = updated_at,
    syncState    = sync_state,
    recordSource = record_source,
    deletedAt    = deleted_at,
    revision     = revision.toInt(),
    userId       = user_id,
)
