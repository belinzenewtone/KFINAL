package com.belinze.lifeos.data.db.dao.impl

import com.belinze.lifeos.data.db.BudgetQueries
import com.belinze.lifeos.data.db.Budgets
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.entity.BudgetEntity

class BudgetDaoImpl(private val q: BudgetQueries) : BudgetDao {

    override suspend fun getActive(): List<BudgetEntity> =
        q.getActive().executeAsList().map { it.toEntity() }

    override suspend fun getAll(): List<BudgetEntity> =
        q.getAll().executeAsList().map { it.toEntity() }

    override suspend fun getById(id: String): BudgetEntity? =
        q.getById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun search(q2: String, limit: Int): List<BudgetEntity> =
        q.search(q2, limit.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun insert(budget: BudgetEntity) = q.insertOrReplace(
        id            = budget.id,
        category      = budget.category,
        limitAmount   = budget.limitAmount,
        period        = budget.period,
        alertThreshold = budget.alertThreshold,
        isActive      = budget.isActive.toLong(),
        createdAt     = budget.createdAt,
        updatedAt     = budget.updatedAt,
        syncState     = budget.syncState,
        recordSource  = budget.recordSource,
        deletedAt     = budget.deletedAt,
        revision      = budget.revision.toLong(),
        userId        = budget.userId,
    )

    override suspend fun update(budget: BudgetEntity) = insert(budget)

    override suspend fun softDelete(id: String, timestamp: String) =
        q.softDelete(timestamp, id)
}

private fun Budgets.toEntity() = BudgetEntity(
    id            = id,
    category      = category,
    limitAmount   = limit_amount,
    period        = period,
    alertThreshold = alert_threshold,
    isActive      = is_active.toInt(),
    createdAt     = created_at,
    updatedAt     = updated_at,
    syncState     = sync_state,
    recordSource  = record_source,
    deletedAt     = deleted_at,
    revision      = revision.toInt(),
    userId        = user_id,
)
