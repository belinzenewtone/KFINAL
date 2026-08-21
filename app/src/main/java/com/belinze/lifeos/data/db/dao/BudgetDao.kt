package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.BudgetEntity

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE deleted_at IS NULL AND is_active = 1 ORDER BY category ASC")
    suspend fun getActive(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE deleted_at IS NULL ORDER BY category ASC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE deleted_at IS NULL AND category LIKE '%' || :q || '%' ORDER BY category ASC LIMIT :limit")
    suspend fun search(q: String, limit: Int): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("UPDATE budgets SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: String)
}
