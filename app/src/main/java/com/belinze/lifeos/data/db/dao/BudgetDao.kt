package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.BudgetEntity
interface BudgetDao {
        suspend fun getActive(): List<BudgetEntity>

        suspend fun getAll(): List<BudgetEntity>

        suspend fun getById(id: String): BudgetEntity?

        suspend fun search(q: String, limit: Int): List<BudgetEntity>

        suspend fun insert(budget: BudgetEntity)

        suspend fun update(budget: BudgetEntity)

        suspend fun softDelete(id: String, timestamp: String)
}
