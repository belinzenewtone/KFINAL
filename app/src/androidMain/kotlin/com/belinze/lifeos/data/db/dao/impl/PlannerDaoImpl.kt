package com.belinze.lifeos.data.db.dao.impl

import com.belinze.lifeos.data.db.BillQueries
import com.belinze.lifeos.data.db.Bills
import com.belinze.lifeos.data.db.ExportQueries
import com.belinze.lifeos.data.db.Exports
import com.belinze.lifeos.data.db.FulizaLoanQueries
import com.belinze.lifeos.data.db.Fuliza_loans
import com.belinze.lifeos.data.db.GoalQueries
import com.belinze.lifeos.data.db.Goals
import com.belinze.lifeos.data.db.RecurringRuleQueries
import com.belinze.lifeos.data.db.Recurring_rules
import com.belinze.lifeos.data.db.dao.PlannerDao
import com.belinze.lifeos.data.db.entity.BillEntity
import com.belinze.lifeos.data.db.entity.ExportEntity
import com.belinze.lifeos.data.db.entity.FulizaLoanEntity
import com.belinze.lifeos.data.db.entity.GoalEntity
import com.belinze.lifeos.data.db.entity.RecurringRuleEntity

class PlannerDaoImpl(
    private val ruleQ: RecurringRuleQueries,
    private val billQ: BillQueries,
    private val goalQ: GoalQueries,
    private val loanQ: FulizaLoanQueries,
    private val exportQ: ExportQueries,
) : PlannerDao {
    // ─── Recurring rules ──────────────────────────────────────────────────────

    override suspend fun getActiveRules(): List<RecurringRuleEntity> =
        ruleQ.getActiveRules().executeAsList().map { it.toEntity() }

    override suspend fun getAllRules(): List<RecurringRuleEntity> =
        ruleQ.getAllRules().executeAsList().map { it.toEntity() }

    override suspend fun getRuleById(id: String): RecurringRuleEntity? =
        ruleQ.getRuleById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun insertRule(rule: RecurringRuleEntity) = ruleQ.insertOrReplace(
        id           = rule.id,
        title        = rule.title,
        type         = rule.type,
        cadence      = rule.cadence,
        nextRunAt    = rule.nextRunAt,
        amount       = rule.amount,
        category     = rule.category,
        enabled      = rule.enabled.toLong(),
        createdAt    = rule.createdAt,
        updatedAt    = rule.updatedAt,
        syncState    = rule.syncState,
        recordSource = rule.recordSource,
        deletedAt    = rule.deletedAt,
        revision     = rule.revision.toLong(),
        userId       = rule.userId,
    )

    override suspend fun updateRule(rule: RecurringRuleEntity) = insertRule(rule)

    override suspend fun softDeleteRule(id: String, ts: String) =
        ruleQ.softDelete(ts, id)

    // ─── Bills ────────────────────────────────────────────────────────────────

    override suspend fun getActiveBills(): List<BillEntity> =
        billQ.getActiveBills().executeAsList().map { it.toEntity() }

    override suspend fun getAllBills(): List<BillEntity> =
        billQ.getAllBills().executeAsList().map { it.toEntity() }

    override suspend fun getBillById(id: String): BillEntity? =
        billQ.getBillById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun insertBill(bill: BillEntity) = billQ.insertOrReplace(
        id          = bill.id,
        userId      = bill.userId,
        title       = bill.title,
        amount      = bill.amount,
        cycle       = bill.cycle,
        nextDueDate = bill.nextDueDate,
        lastPaidAt  = bill.lastPaidAt,
        notes       = bill.notes,
        isActive    = bill.isActive.toLong(),
        paidStatus  = bill.paidStatus.toLong(),
        createdAt   = bill.createdAt,
        updatedAt   = bill.updatedAt,
        syncState   = bill.syncState,
        deletedAt   = bill.deletedAt,
        revision    = bill.revision.toLong(),
    )

    override suspend fun updateBill(bill: BillEntity) = insertBill(bill)

    override suspend fun softDeleteBill(id: String, ts: String) =
        billQ.softDelete(ts, id)

    // ─── Goals ────────────────────────────────────────────────────────────────

    override suspend fun getAllGoals(): List<GoalEntity> =
        goalQ.getAllGoals().executeAsList().map { it.toEntity() }

    override suspend fun getGoalById(id: String): GoalEntity? =
        goalQ.getGoalById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun insertGoal(goal: GoalEntity) = goalQ.insertOrReplace(
        id           = goal.id,
        userId       = goal.userId,
        title        = goal.title,
        description  = goal.description,
        targetValue  = goal.targetValue,
        currentValue = goal.currentValue,
        unit         = goal.unit,
        category     = goal.category,
        deadline     = goal.deadline,
        status       = goal.status,
        createdAt    = goal.createdAt,
        updatedAt    = goal.updatedAt,
        syncState    = goal.syncState,
        deletedAt    = goal.deletedAt,
        revision     = goal.revision.toLong(),
    )

    override suspend fun updateGoal(goal: GoalEntity) = insertGoal(goal)

    override suspend fun softDeleteGoal(id: String, ts: String) =
        goalQ.softDelete(ts, id)

    // ─── Fuliza loans ─────────────────────────────────────────────────────────

    override suspend fun getActiveLoans(): List<FulizaLoanEntity> =
        loanQ.getActiveLoans().executeAsList().map { it.toEntity() }

    override suspend fun getAllLoans(): List<FulizaLoanEntity> =
        loanQ.getAllLoans().executeAsList().map { it.toEntity() }

    override suspend fun getLoanById(id: String): FulizaLoanEntity? =
        loanQ.getLoanById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun insertLoan(loan: FulizaLoanEntity) = loanQ.insertOrReplace(
        id                 = loan.id,
        drawCode           = loan.drawCode,
        drawAmountKes      = loan.drawAmountKes,
        totalRepaidKes     = loan.totalRepaidKes,
        status             = loan.status,
        drawDate           = loan.drawDate,
        lastRepaymentDate  = loan.lastRepaymentDate,
        createdAt          = loan.createdAt,
        updatedAt          = loan.updatedAt,
        userId             = loan.userId,
    )

    override suspend fun updateLoan(loan: FulizaLoanEntity) = insertLoan(loan)

    override suspend fun hardDeleteLoan(id: String) =
        loanQ.hardDelete(id)

    // ─── Exports ──────────────────────────────────────────────────────────────

    override suspend fun getAllExports(): List<ExportEntity> =
        exportQ.getAllExports().executeAsList().map { it.toEntity() }

    override suspend fun insertExport(export: ExportEntity) = exportQ.insertOrReplace(
        id          = export.id,
        filePath    = export.filePath,
        fileSize    = export.fileSize,
        format      = export.format,
        createdAt   = export.createdAt,
        recordCount = export.recordCount?.toLong(),
    )

    override suspend fun deleteExport(id: String) =
        exportQ.deleteExport(id)

    override suspend fun deleteAllExports() =
        exportQ.deleteAllExports()
}

private fun Recurring_rules.toEntity() = RecurringRuleEntity(
    id           = id,
    title        = title,
    type         = type,
    cadence      = cadence,
    nextRunAt    = next_run_at,
    amount       = amount,
    category     = category,
    enabled      = enabled.toInt(),
    createdAt    = created_at,
    updatedAt    = updated_at,
    syncState    = sync_state,
    recordSource = record_source,
    deletedAt    = deleted_at,
    revision     = revision.toInt(),
    userId       = user_id,
)

private fun Bills.toEntity() = BillEntity(
    id          = id,
    userId      = user_id,
    title       = title,
    amount      = amount,
    cycle       = cycle,
    nextDueDate = next_due_date,
    lastPaidAt  = last_paid_at,
    notes       = notes,
    isActive    = is_active.toInt(),
    paidStatus  = paid_status.toInt(),
    createdAt   = created_at,
    updatedAt   = updated_at,
    syncState   = sync_state,
    deletedAt   = deleted_at,
    revision    = revision.toInt(),
)

private fun Goals.toEntity() = GoalEntity(
    id           = id,
    userId       = user_id,
    title        = title,
    description  = description,
    targetValue  = target_value,
    currentValue = current_value,
    unit         = unit,
    category     = category,
    deadline     = deadline,
    status       = status,
    createdAt    = created_at,
    updatedAt    = updated_at,
    syncState    = sync_state,
    deletedAt    = deleted_at,
    revision     = revision.toInt(),
)

private fun Fuliza_loans.toEntity() = FulizaLoanEntity(
    id                = id,
    drawCode          = draw_code,
    drawAmountKes     = draw_amount_kes,
    totalRepaidKes    = total_repaid_kes,
    status            = status,
    drawDate          = draw_date,
    lastRepaymentDate = last_repayment_date,
    createdAt         = created_at,
    updatedAt         = updated_at,
    userId            = user_id,
)

private fun Exports.toEntity() = ExportEntity(
    id          = id,
    filePath    = file_path,
    fileSize    = file_size,
    format      = format,
    createdAt   = created_at,
    recordCount = record_count?.toInt(),
)
