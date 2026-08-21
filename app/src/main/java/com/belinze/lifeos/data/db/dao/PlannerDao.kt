package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.*

/** Aggregated DAO for the Planner hub — covers recurring rules, bills, goals, loans, exports. */
@Dao
interface PlannerDao {
    // ─── Recurring rules ─────────────────────────────────────────────────────

    @Query("SELECT * FROM recurring_rules WHERE deleted_at IS NULL AND enabled = 1 ORDER BY next_run_at ASC")
    suspend fun getActiveRules(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE deleted_at IS NULL ORDER BY created_at DESC")
    suspend fun getAllRules(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id AND deleted_at IS NULL")
    suspend fun getRuleById(id: String): RecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurringRuleEntity)

    @Update
    suspend fun updateRule(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET deleted_at = :ts WHERE id = :id")
    suspend fun softDeleteRule(id: String, ts: String)

    // ─── Bills ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM bills WHERE deleted_at IS NULL AND is_active = 1 ORDER BY next_due_date ASC")
    suspend fun getActiveBills(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE deleted_at IS NULL ORDER BY next_due_date ASC")
    suspend fun getAllBills(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id AND deleted_at IS NULL")
    suspend fun getBillById(id: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity)

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Query("UPDATE bills SET deleted_at = :ts WHERE id = :id")
    suspend fun softDeleteBill(id: String, ts: String)

    // ─── Goals ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM goals WHERE deleted_at IS NULL ORDER BY deadline ASC")
    suspend fun getAllGoals(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id AND deleted_at IS NULL")
    suspend fun getGoalById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET deleted_at = :ts WHERE id = :id")
    suspend fun softDeleteGoal(id: String, ts: String)

    // ─── Fuliza loans ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM fuliza_loans WHERE status = 'active' ORDER BY draw_date DESC")
    suspend fun getActiveLoans(): List<FulizaLoanEntity>

    @Query("SELECT * FROM fuliza_loans ORDER BY draw_date DESC")
    suspend fun getAllLoans(): List<FulizaLoanEntity>

    @Query("SELECT * FROM fuliza_loans WHERE id = :id")
    suspend fun getLoanById(id: String): FulizaLoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: FulizaLoanEntity)

    @Update
    suspend fun updateLoan(loan: FulizaLoanEntity)

    @Query("DELETE FROM fuliza_loans WHERE id = :id")
    suspend fun hardDeleteLoan(id: String)

    // ─── Exports ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM exports ORDER BY created_at DESC")
    suspend fun getAllExports(): List<ExportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportEntity)

    @Query("DELETE FROM exports WHERE id = :id")
    suspend fun deleteExport(id: String)

    @Query("DELETE FROM exports")
    suspend fun deleteAllExports()
}
