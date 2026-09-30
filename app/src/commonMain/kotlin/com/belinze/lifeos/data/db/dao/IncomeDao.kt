package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.IncomeEntity

interface IncomeDao {
        suspend fun getAll(): List<IncomeEntity>

        suspend fun getInRange(startDate: String, endDate: String): List<IncomeEntity>

        suspend fun getTotalInRange(startDate: String, endDate: String): Double?

        suspend fun getById(id: String): IncomeEntity?

        suspend fun search(q: String, limit: Int): List<IncomeEntity>

        suspend fun insert(income: IncomeEntity)

        suspend fun update(income: IncomeEntity)

        suspend fun softDelete(id: String, timestamp: String)

        suspend fun updateActive(id: String, active: Int)
}
