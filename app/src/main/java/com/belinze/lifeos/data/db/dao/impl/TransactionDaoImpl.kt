package com.belinze.lifeos.data.db.dao.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.belinze.lifeos.data.db.TransactionQueries
import com.belinze.lifeos.data.db.Transactions
import com.belinze.lifeos.data.db.dao.BiggestSpend
import com.belinze.lifeos.data.db.dao.CategoryTotal
import com.belinze.lifeos.data.db.dao.DayNet
import com.belinze.lifeos.data.db.dao.DaySpend
import com.belinze.lifeos.data.db.dao.FeeCategoryTotal
import com.belinze.lifeos.data.db.dao.FeeSummary
import com.belinze.lifeos.data.db.dao.IncomeDateRow
import com.belinze.lifeos.data.db.dao.MerchantTotal
import com.belinze.lifeos.data.db.dao.MonthlyCategoryRow
import com.belinze.lifeos.data.db.dao.MonthlyTotalsRow
import com.belinze.lifeos.data.db.dao.MonthTotals
import com.belinze.lifeos.data.db.dao.SizeBreakdownRow
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionDaoImpl(private val q: TransactionQueries) : TransactionDao {

    // ─── Reads ────────────────────────────────────────────────────────────────

    override suspend fun getPage(limit: Int, offset: Int): List<TransactionEntity> =
        q.getPage(limit.toLong(), offset.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun getFiltered(
        search: String, category: String, type: String?, status: String?,
        startDate: String?, endDate: String?, limit: Int, offset: Int,
    ): List<TransactionEntity> =
        q.getFiltered(search, category, type, status, startDate, endDate, limit.toLong(), offset.toLong())
            .executeAsList().map { it.toEntity() }

    override suspend fun getById(id: String): TransactionEntity? =
        q.getById(id).executeAsOneOrNull()?.toEntity()

    override suspend fun getByMerchant(merchant: String): List<TransactionEntity> =
        q.getByMerchant(merchant).executeAsList().map { it.toEntity() }

    override suspend fun getRecent(): List<TransactionEntity> =
        q.getRecent().executeAsList().map { it.toEntity() }

    override suspend fun getMonthTotals(monthPrefix: String): MonthTotals {
        val row = q.getMonthTotals(monthPrefix).executeAsOneOrNull()
        return MonthTotals(income = row?.income, expense = row?.expense)
    }

    override suspend fun getCategoryTotals(startDate: String, endDate: String): List<CategoryTotal> =
        q.getCategoryTotals(startDate, endDate).executeAsList()
            .map { CategoryTotal(category = it.category, total = it.total ?: 0.0) }

    override suspend fun getExpenseCategoryTotals(startDate: String, endDate: String): List<CategoryTotal> =
        q.getExpenseCategoryTotals(startDate, endDate).executeAsList()
            .map { CategoryTotal(category = it.category, total = it.total) }

    override suspend fun getTopMerchants(startDate: String, endDate: String, limit: Int): List<MerchantTotal> =
        q.getTopMerchants(startDate, endDate, limit.toLong()).executeAsList()
            .map { MerchantTotal(merchant = it.merchant, total = it.total ?: 0.0) }

    override suspend fun countUncategorized(): Int =
        q.countUncategorized().executeAsOne().toInt()

    override suspend fun sumUncategorizedAmount(): Double =
        q.sumUncategorizedAmount().executeAsOne()

    override suspend fun getUncategorized(): List<TransactionEntity> =
        q.getUncategorized().executeAsList().map { it.toEntity() }

    override suspend fun updateCategoryForMerchant(merchant: String, category: String, ts: String) =
        q.updateCategoryForMerchant(category, ts, merchant)

    override suspend fun updateCategoryById(id: String, category: String, ts: String) =
        q.updateCategoryById(category, ts, id)

    override suspend fun getFeeTotal(startDate: String, endDate: String): Double? =
        q.getFeeTotal(startDate, endDate).executeAsOneOrNull()

    override suspend fun getFeeByCategory(startDate: String, endDate: String): List<FeeCategoryTotal> =
        q.getFeeByCategory(startDate, endDate).executeAsList()
            .map { FeeCategoryTotal(category = it.category, total = it.total ?: 0.0, count = it.count.toInt()) }

    override suspend fun getChargesByCategory(startDate: String, endDate: String): List<FeeCategoryTotal> =
        q.getChargesByCategory(startDate, endDate).executeAsList()
            .map { FeeCategoryTotal(category = it.category, total = it.total, count = it.count.toInt()) }

    override suspend fun getFeeTransactions(startDate: String, endDate: String): List<TransactionEntity> =
        q.getFeeTransactions(startDate, endDate).executeAsList().map { it.toEntity() }

    // ─── Live queries (Flow) ──────────────────────────────────────────────────

    override fun observeRecent(): Flow<List<TransactionEntity>> =
        q.observeRecent().asFlow().mapToList(Dispatchers.IO).map { list -> list.map { it.toEntity() } }

    // ─── Writes ───────────────────────────────────────────────────────────────

    override suspend fun insert(tx: TransactionEntity) = q.insertOrReplace(
        id               = tx.id,
        amount           = tx.amount,
        merchant         = tx.merchant,
        category         = tx.category,
        date             = tx.date,
        source           = tx.source,
        transactionType  = tx.transactionType,
        mpesaCode        = tx.mpesaCode,
        sourceHash       = tx.sourceHash,
        rawSms           = tx.rawSms,
        description      = tx.description,
        notes            = tx.notes,
        balanceAfter     = tx.balanceAfter,
        fee              = tx.fee,
        status           = tx.status,
        createdAt        = tx.createdAt,
        updatedAt        = tx.updatedAt,
        syncState        = tx.syncState,
        recordSource     = tx.recordSource,
        deletedAt        = tx.deletedAt,
        revision         = tx.revision.toLong(),
        userId           = tx.userId,
        inferredCategory = tx.inferredCategory.toLong(),
        inferenceSource  = tx.inferenceSource,
        semanticHash     = tx.semanticHash,
        institutionId    = tx.institutionId,
        externalRef      = tx.externalRef,
        currency         = tx.currency,
        rawSender        = tx.rawSender,
    )

    override suspend fun insertAll(transactions: List<TransactionEntity>) {
        q.transaction {
            for (tx in transactions) {
                q.insertOrReplace(
                    id               = tx.id,
                    amount           = tx.amount,
                    merchant         = tx.merchant,
                    category         = tx.category,
                    date             = tx.date,
                    source           = tx.source,
                    transactionType  = tx.transactionType,
                    mpesaCode        = tx.mpesaCode,
                    sourceHash       = tx.sourceHash,
                    rawSms           = tx.rawSms,
                    description      = tx.description,
                    notes            = tx.notes,
                    balanceAfter     = tx.balanceAfter,
                    fee              = tx.fee,
                    status           = tx.status,
                    createdAt        = tx.createdAt,
                    updatedAt        = tx.updatedAt,
                    syncState        = tx.syncState,
                    recordSource     = tx.recordSource,
                    deletedAt        = tx.deletedAt,
                    revision         = tx.revision.toLong(),
                    userId           = tx.userId,
                    inferredCategory = tx.inferredCategory.toLong(),
                    inferenceSource  = tx.inferenceSource,
                    semanticHash     = tx.semanticHash,
                    institutionId    = tx.institutionId,
                    externalRef      = tx.externalRef,
                    currency         = tx.currency,
                    rawSender        = tx.rawSender,
                )
            }
        }
    }

    override suspend fun update(tx: TransactionEntity) = insert(tx)

    override suspend fun softDelete(id: String, timestamp: String) =
        q.softDelete(timestamp, id)

    override suspend fun countPendingReview(): Int =
        q.countPendingReview().executeAsOne().toInt()

    // ─── Week review ──────────────────────────────────────────────────────────

    override suspend fun getDaySpends(startDate: String, endDate: String): List<DaySpend> =
        q.getDaySpends(startDate, endDate).executeAsList()
            .map { DaySpend(day = it.day ?: "", total = it.total) }

    override suspend fun getDayNetTotals(startDate: String, endDate: String): List<DayNet> =
        q.getDayNetTotals(startDate, endDate).executeAsList()
            .map { DayNet(day = it.day ?: "", net = it.net) }

    override suspend fun getSpendTotalInRange(startDate: String, endDate: String): Double =
        q.getSpendTotalInRange(startDate, endDate).executeAsOne()

    override suspend fun getTopCategoryInRange(startDate: String, endDate: String): String? =
        q.getTopCategoryInRange(startDate, endDate).executeAsOneOrNull()?.category

    override suspend fun countUncategorizedInRange(startDate: String, endDate: String): Int =
        q.countUncategorizedInRange(startDate, endDate).executeAsOne().toInt()

    override suspend fun countFulizaInRange(startDate: String, endDate: String): Int =
        q.countFulizaInRange(startDate, endDate).executeAsOne().toInt()

    override suspend fun getIncomeTotalInRange(startDate: String, endDate: String): Double =
        q.getIncomeTotalInRange(startDate, endDate).executeAsOne()

    override suspend fun countSpendTransactions(startDate: String, endDate: String): Int =
        q.countSpendTransactions(startDate, endDate).executeAsOne().toInt()

    override suspend fun countActiveDays(startDate: String, endDate: String): Int =
        q.countActiveDays(startDate, endDate).executeAsOne().toInt()

    override suspend fun getFulizaTotalInRange(startDate: String, endDate: String): Double =
        q.getFulizaTotalInRange(startDate, endDate).executeAsOne()

    override suspend fun countFulizaTransactions(startDate: String, endDate: String): Int =
        q.countFulizaTransactions(startDate, endDate).executeAsOne().toInt()

    override suspend fun getBiggestSpend(startDate: String, endDate: String): BiggestSpend? =
        q.getBiggestSpend(startDate, endDate).executeAsOneOrNull()
            ?.let { BiggestSpend(merchant = it.merchant, amount = it.amount, date = it.date ?: "") }

    override suspend fun getMinTransactionDate(): String? =
        q.getMinTransactionDate().executeAsOneOrNull()?.MIN

    override suspend fun getFeeTotalInRange(startDate: String, endDate: String): Double =
        q.getFeeTotalInRange(startDate, endDate).executeAsOne()

    override suspend fun getAverageTransactionInRange(startDate: String, endDate: String): Double =
        q.getAverageTransactionInRange(startDate, endDate).executeAsOne()

    override suspend fun getIncomeInRange(startDate: String, endDate: String): Double =
        q.getIncomeInRange(startDate, endDate).executeAsOne()

    override suspend fun getUncategorizedAmountInRange(startDate: String, endDate: String): Double =
        q.getUncategorizedAmountInRange(startDate, endDate).executeAsOne()

    override suspend fun getFeeSummaryInRange(startDate: String, endDate: String): FeeSummary =
        q.getFeeSummaryInRange(startDate, endDate).executeAsOne()
            .let { FeeSummary(total = it.total, avgFee = it.avgFee, txCount = it.txCount.toInt()) }

    // ─── Review queue ─────────────────────────────────────────────────────────

    override suspend fun getByMpesaCode(code: String): TransactionEntity? =
        q.getByMpesaCode(code).executeAsOneOrNull()?.toEntity()

    override suspend fun updateStatusByMpesaCode(code: String, status: String, ts: String) =
        q.updateStatusByMpesaCode(status, ts, code)

    override suspend fun updateStatusAndSyncStateByMpesaCode(code: String, status: String, syncState: String, ts: String) =
        q.updateStatusAndSyncStateByMpesaCode(status, syncState, ts, code)

    // ─── Insights ─────────────────────────────────────────────────────────────

    override suspend fun getMonthlyTotalsRange(sixMonthsAgo: String): List<MonthlyTotalsRow> =
        q.getMonthlyTotalsRange(sixMonthsAgo).executeAsList()
            .map { MonthlyTotalsRow(monthKey = it.month_key ?: "", expense = it.expense, income = it.income, txCount = it.tx_count.toInt()) }

    override suspend fun getMonthlyCategoryBreakdown(sixMonthsAgo: String): List<MonthlyCategoryRow> =
        q.getMonthlyCategoryBreakdown(sixMonthsAgo).executeAsList()
            .map { MonthlyCategoryRow(monthKey = it.month_key ?: "", category = it.category, total = it.total) }

    override suspend fun getIncomeDates(sixMonthsAgo: String): List<IncomeDateRow> =
        q.getIncomeDates(sixMonthsAgo).executeAsList()
            .map { IncomeDateRow(dt = it) }

    override suspend fun getSizeBreakdown(sixMonthsAgo: String): SizeBreakdownRow? =
        q.getSizeBreakdown(sixMonthsAgo).executeAsOneOrNull()
            ?.let {
                SizeBreakdownRow(
                    microCount  = it.micro_count.toInt(),
                    mediumCount = it.medium_count.toInt(),
                    largeCount  = it.large_count.toInt(),
                    microTotal  = it.micro_total,
                    mediumTotal = it.medium_total,
                    largeTotal  = it.large_total,
                )
            }
}

private fun Transactions.toEntity() = TransactionEntity(
    id               = id,
    amount           = amount,
    merchant         = merchant,
    category         = category,
    date             = date,
    source           = source,
    transactionType  = transaction_type,
    mpesaCode        = mpesa_code,
    sourceHash       = source_hash,
    rawSms           = raw_sms,
    description      = description,
    notes            = notes,
    balanceAfter     = balance_after,
    fee              = fee,
    status           = status,
    createdAt        = created_at,
    updatedAt        = updated_at,
    syncState        = sync_state,
    recordSource     = record_source,
    deletedAt        = deleted_at,
    revision         = revision.toInt(),
    userId           = user_id,
    inferredCategory = inferred_category.toInt(),
    inferenceSource  = inference_source,
    semanticHash     = semantic_hash,
    institutionId    = institution_id,
    externalRef      = external_ref,
    currency         = currency,
    rawSender        = raw_sender,
)
