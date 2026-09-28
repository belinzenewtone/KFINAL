package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.*

/** Aggregated DAO for the Planner hub — covers recurring rules, bills, goals, loans, exports. */
interface PlannerDao {
    // ─── Recurring rules ─────────────────────────────────────────────────────

        suspend fun getActiveRules(): List<RecurringRuleEntity>

        suspend fun getAllRules(): List<RecurringRuleEntity>

        suspend fun getRuleById(id: String): RecurringRuleEntity?

        suspend fun insertRule(rule: RecurringRuleEntity)

        suspend fun updateRule(rule: RecurringRuleEntity)

        suspend fun softDeleteRule(id: String, ts: String)

    // ─── Bills ───────────────────────────────────────────────────────────────

        suspend fun getActiveBills(): List<BillEntity>

        suspend fun getAllBills(): List<BillEntity>

        suspend fun getBillById(id: String): BillEntity?

        suspend fun insertBill(bill: BillEntity)

        suspend fun updateBill(bill: BillEntity)

        suspend fun softDeleteBill(id: String, ts: String)

    // ─── Goals ───────────────────────────────────────────────────────────────

        suspend fun getAllGoals(): List<GoalEntity>

        suspend fun getGoalById(id: String): GoalEntity?

        suspend fun insertGoal(goal: GoalEntity)

        suspend fun updateGoal(goal: GoalEntity)

        suspend fun softDeleteGoal(id: String, ts: String)

    // ─── Fuliza loans ─────────────────────────────────────────────────────────

        suspend fun getActiveLoans(): List<FulizaLoanEntity>

        suspend fun getAllLoans(): List<FulizaLoanEntity>

        suspend fun getLoanById(id: String): FulizaLoanEntity?

        suspend fun insertLoan(loan: FulizaLoanEntity)

        suspend fun updateLoan(loan: FulizaLoanEntity)

        suspend fun hardDeleteLoan(id: String)

    // ─── Exports ─────────────────────────────────────────────────────────────

        suspend fun getAllExports(): List<ExportEntity>

        suspend fun insertExport(export: ExportEntity)

        suspend fun deleteExport(id: String)

        suspend fun deleteAllExports()
}
