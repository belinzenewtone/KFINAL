package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.IncomeEntity

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes WHERE deleted_at IS NULL ORDER BY date DESC")
    suspend fun getAll(): List<IncomeEntity>

    @Query("""
        SELECT * FROM incomes
        WHERE deleted_at IS NULL
          AND date >= :startDate AND date <= :endDate
        ORDER BY date DESC
    """)
    suspend fun getInRange(startDate: String, endDate: String): List<IncomeEntity>

    @Query("SELECT SUM(amount) FROM incomes WHERE deleted_at IS NULL AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalInRange(startDate: String, endDate: String): Double?

    @Query("SELECT * FROM incomes WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): IncomeEntity?

    @Query("SELECT * FROM incomes WHERE deleted_at IS NULL AND source LIKE '%' || :q || '%' ORDER BY date DESC LIMIT :limit")
    suspend fun search(q: String, limit: Int): List<IncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(income: IncomeEntity)

    @Update
    suspend fun update(income: IncomeEntity)

    @Query("UPDATE incomes SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: String)
}
