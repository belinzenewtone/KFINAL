package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionDao {
    // ─── Reads ────────────────────────────────────────────────────────────────

        suspend fun getPage(limit: Int, offset: Int): List<TransactionEntity>

        suspend fun getFiltered(
        search: String,
        category: String,
        type: String?,
        status: String?,
        startDate: String?,
        endDate: String?,
        limit: Int,
        offset: Int,
    ): List<TransactionEntity>

        suspend fun getById(id: String): TransactionEntity?

        suspend fun getByMerchant(merchant: String): List<TransactionEntity>

        suspend fun getRecent(): List<TransactionEntity>

    /** Month totals: income sum and expense sum for a given ISO month prefix (e.g. "2025-01") */
        suspend fun getMonthTotals(monthPrefix: String): MonthTotals

        suspend fun getCategoryTotals(startDate: String, endDate: String): List<CategoryTotal>

    /** Budget-spend population — RFINAL's BudgetRepository counts EXPENSES ONLY
     *  (`transaction_type = 'expense' AND status = 'completed'`), so transfers and
     *  Fuliza must NOT inflate a budget's used percentage. Deliberately separate from
     *  [getCategoryTotals], whose transfer/fuliza inclusion is correct elsewhere
     *  (MonthlyWrapped top categories, Insights sparklines). */
        suspend fun getExpenseCategoryTotals(startDate: String, endDate: String): List<CategoryTotal>

        suspend fun getTopMerchants(startDate: String, endDate: String, limit: Int): List<MerchantTotal>

        suspend fun countUncategorized(): Int

        suspend fun sumUncategorizedAmount(): Double

        suspend fun getUncategorized(): List<TransactionEntity>

        suspend fun updateCategoryForMerchant(merchant: String, category: String, ts: String)

        suspend fun updateCategoryById(id: String, category: String, ts: String)

    /** RFINAL's "service charges" population: sums the AMOUNT of rows whose CATEGORY is
     *  one of the fee-ish categories — NOT the per-transaction `fee` column. Shared by
     *  the Finance "Charges" card and FeeAnalyticsScreen. */
        suspend fun getFeeTotal(startDate: String, endDate: String): Double?

    /** Per-category breakdown of the `fee` column — the Analytics tab's fee summary.
     *  Its RFINAL counterpart (TransactionRepository.getFeesSummaryInRange's topCategory
     *  query) is also fee-column based and carries the same `status = 'completed'` filter,
     *  so this is verified rather than assumed. */
        suspend fun getFeeByCategory(startDate: String, endDate: String): List<FeeCategoryTotal>

    /** FeeAnalyticsScreen's per-category breakdown — amount by fee CATEGORY, matching
     *  RFINAL's query (SUM(amount), no status filter). */
        suspend fun getChargesByCategory(startDate: String, endDate: String): List<FeeCategoryTotal>

        suspend fun getFeeTransactions(startDate: String, endDate: String): List<TransactionEntity>

    // ─── Live queries (Flow) ──────────────────────────────────────────────────

        fun observeRecent(): Flow<List<TransactionEntity>>

    // ─── Writes ───────────────────────────────────────────────────────────────

        suspend fun insert(tx: TransactionEntity)

        suspend fun insertAll(transactions: List<TransactionEntity>)

        suspend fun update(tx: TransactionEntity)

    /** Soft-delete */
        suspend fun softDelete(id: String, timestamp: String)

        suspend fun countPendingReview(): Int

    // ─── Week review queries ──────────────────────────────────────────────────

    /** Per-day spend totals for a date range. `day` is "YYYY-MM-DD" (local). */
        suspend fun getDaySpends(startDate: String, endDate: String): List<DaySpend>

    /** Per-day NET totals for completed rows — RFINAL's FinanceScreen dayTotal rule:
     *  outflows subtract, inflows add, and rows whose status isn't 'completed' are
     *  skipped. Computed in SQL so the Finance list headers don't have to sum a
     *  partial Paging-3 window (which would show wrong numbers). */
        suspend fun getDayNetTotals(startDate: String, endDate: String): List<DayNet>

        suspend fun getSpendTotalInRange(startDate: String, endDate: String): Double

        suspend fun getTopCategoryInRange(startDate: String, endDate: String): String?

        suspend fun countUncategorizedInRange(startDate: String, endDate: String): Int

        suspend fun countFulizaInRange(startDate: String, endDate: String): Int

    // ─── Monthly wrapped queries ──────────────────────────────────────────────

        suspend fun getIncomeTotalInRange(startDate: String, endDate: String): Double

        suspend fun countSpendTransactions(startDate: String, endDate: String): Int

        suspend fun countActiveDays(startDate: String, endDate: String): Int

        suspend fun getFulizaTotalInRange(startDate: String, endDate: String): Double

        suspend fun countFulizaTransactions(startDate: String, endDate: String): Int

        suspend fun getBiggestSpend(startDate: String, endDate: String): BiggestSpend?

        suspend fun getMinTransactionDate(): String?

        suspend fun getFeeTotalInRange(startDate: String, endDate: String): Double

    // ─── Analytics tab helpers ────────────────────────────────────────────────

        suspend fun getAverageTransactionInRange(startDate: String, endDate: String): Double

        suspend fun getIncomeInRange(startDate: String, endDate: String): Double

        suspend fun getUncategorizedAmountInRange(startDate: String, endDate: String): Double

    /** Fee summary for a date range: sums the per-transaction M-Pesa fee column.
     *  Mirrors RFINAL's TransactionRepository.getFeesSummaryInRange, including its
     *  `status = 'completed'` filter. */
        suspend fun getFeeSummaryInRange(startDate: String, endDate: String): FeeSummary

    // ─── Review queue helpers ─────────────────────────────────────────────────

        suspend fun getByMpesaCode(code: String): TransactionEntity?

        suspend fun updateStatusByMpesaCode(code: String, status: String, ts: String)

    /** Approve a review-queue transaction: mark completed and flag for sync. */
        suspend fun updateStatusAndSyncStateByMpesaCode(code: String, status: String, syncState: String, ts: String)

    // ─── Insights tab deep queries ────────────────────────────────────────────────

        suspend fun getMonthlyTotalsRange(sixMonthsAgo: String): List<MonthlyTotalsRow>

        suspend fun getMonthlyCategoryBreakdown(sixMonthsAgo: String): List<MonthlyCategoryRow>

        suspend fun getIncomeDates(sixMonthsAgo: String): List<IncomeDateRow>

        suspend fun getSizeBreakdown(sixMonthsAgo: String): SizeBreakdownRow?
}

// ─── Projection data classes ─────────────────────────────────────────────────

data class MonthTotals(val income: Double?, val expense: Double?)

data class CategoryTotal(val category: String?, val total: Double)

data class MerchantTotal(val merchant: String?, val total: Double)

data class FeeCategoryTotal(val category: String?, val total: Double, val count: Int)

data class DaySpend(val day: String, val total: Double)

data class DayNet(val day: String, val net: Double)

data class BiggestSpend(val merchant: String?, val amount: Double, val date: String)

data class FeeSummary(val total: Double, val avgFee: Double, val txCount: Int)

data class MonthlyTotalsRow(
    val monthKey: String,
    val expense: Double,
    val income: Double,
    val txCount: Int,
)

data class MonthlyCategoryRow(
    val monthKey: String,
    val category: String?,
    val total: Double,
)

data class IncomeDateRow(val dt: String)

data class SizeBreakdownRow(
    val microCount:  Int,
    val mediumCount: Int,
    val largeCount:  Int,
    val microTotal:  Double,
    val mediumTotal: Double,
    val largeTotal:  Double,
)
