package com.belinze.lifeos.data.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.belinze.lifeos.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // ─── Reads ────────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
        ORDER BY date DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPage(limit: Int, offset: Int): List<TransactionEntity>

    /**
     * Paging 3 source — Room auto-generates the PagingSource implementation and
     * registers with InvalidationTracker so any write to `transactions` instantly
     * invalidates this source, triggering a smooth list refresh without reload().
     */
    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
          AND (:search = '' OR merchant LIKE '%' || :search || '%' OR mpesa_code LIKE '%' || :search || '%')
          AND (:category = 'all' OR category = :category)
          AND (:type IS NULL OR transaction_type = :type)
          AND (:status IS NULL OR status = :status)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC
    """)
    fun getFilteredPaged(
        search: String,
        category: String,
        type: String?,
        status: String?,
        startDate: String?,
        endDate: String?,
    ): PagingSource<Int, TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
          AND (:search = '' OR merchant LIKE '%' || :search || '%' OR mpesa_code LIKE '%' || :search || '%')
          AND (:category = 'all' OR category = :category)
          AND (:type IS NULL OR transaction_type = :type)
          AND (:status IS NULL OR status = :status)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC
        LIMIT :limit OFFSET :offset
    """)
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

    @Query("SELECT * FROM transactions WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions
        WHERE merchant = :merchant AND deleted_at IS NULL
        ORDER BY date DESC
    """)
    suspend fun getByMerchant(merchant: String): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
          AND status = 'completed'
          AND transaction_type NOT IN ('topup')
        ORDER BY date DESC
        LIMIT 5
    """)
    suspend fun getRecent(): List<TransactionEntity>

    /** Month totals: income sum and expense sum for a given ISO month prefix (e.g. "2025-01") */
    @Query("""
        SELECT
          SUM(CASE WHEN transaction_type = 'receive'  THEN amount ELSE 0 END) AS income,
          SUM(CASE WHEN transaction_type IN ('expense','transfer','fuliza') THEN amount ELSE 0 END) AS expense
        FROM transactions
        WHERE deleted_at IS NULL
          AND date LIKE :monthPrefix || '%'
          AND status = 'completed'
    """)
    suspend fun getMonthTotals(monthPrefix: String): MonthTotals

    @Query("""
        SELECT category, SUM(amount) AS total
        FROM transactions
        WHERE deleted_at IS NULL
          AND transaction_type IN ('expense','transfer','fuliza')
          AND date >= :startDate AND date <= :endDate
          AND status = 'completed'
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotals(startDate: String, endDate: String): List<CategoryTotal>

    @Query("""
        SELECT merchant, SUM(amount) AS total
        FROM transactions
        WHERE deleted_at IS NULL
          AND transaction_type IN ('expense','transfer','fuliza')
          AND date >= :startDate AND date <= :endDate
          AND status = 'completed'
          AND merchant IS NOT NULL AND merchant != ''
        GROUP BY merchant
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getTopMerchants(startDate: String, endDate: String, limit: Int): List<MerchantTotal>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE deleted_at IS NULL
          AND (category IS NULL OR category = 'uncategorized')
    """)
    suspend fun countUncategorized(): Int

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
          AND (category IS NULL OR category = 'uncategorized')
        ORDER BY date DESC
    """)
    suspend fun getUncategorized(): List<TransactionEntity>

    @Query("""
        UPDATE transactions
        SET category = :category, updated_at = :ts
        WHERE deleted_at IS NULL
          AND merchant = :merchant
    """)
    suspend fun updateCategoryForMerchant(merchant: String, category: String, ts: String)

    @Query("""
        UPDATE transactions
        SET category = :category, updated_at = :ts
        WHERE id = :id
    """)
    suspend fun updateCategoryById(id: String, category: String, ts: String)

    @Query("""
        SELECT SUM(fee) FROM transactions
        WHERE deleted_at IS NULL
          AND date >= :startDate AND date <= :endDate
          AND fee IS NOT NULL AND fee > 0
    """)
    suspend fun getFeeTotal(startDate: String, endDate: String): Double?

    @Query("""
        SELECT category, SUM(fee) AS total, COUNT(*) AS count
        FROM transactions
        WHERE deleted_at IS NULL
          AND fee IS NOT NULL AND fee > 0
          AND date >= :startDate AND date <= :endDate
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getFeeByCategory(startDate: String, endDate: String): List<FeeCategoryTotal>

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
          AND fee IS NOT NULL AND fee > 0
          AND date >= :startDate AND date <= :endDate
        ORDER BY date DESC
        LIMIT 50
    """)
    suspend fun getFeeTransactions(startDate: String, endDate: String): List<TransactionEntity>

    // ─── Live queries (Flow) ──────────────────────────────────────────────────

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
        ORDER BY date DESC
        LIMIT 50
    """)
    fun observeRecent(): Flow<List<TransactionEntity>>

    // ─── Writes ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(tx: TransactionEntity)

    /** Soft-delete */
    @Query("UPDATE transactions SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: String)

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE deleted_at IS NULL
          AND status = 'review'
    """)
    suspend fun countPendingReview(): Int

    // ─── Week review queries ──────────────────────────────────────────────────

    /** Per-day spend totals for a date range. `day` is "YYYY-MM-DD" (local). */
    @Query("""
        SELECT substr(date, 1, 10) as day, COALESCE(SUM(amount), 0) as total
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
        GROUP BY day ORDER BY day
    """)
    suspend fun getDaySpends(startDate: String, endDate: String): List<DaySpend>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun getSpendTotalInRange(startDate: String, endDate: String): Double

    @Query("""
        SELECT category FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
          AND category IS NOT NULL AND category != '' AND category != 'uncategorized'
        GROUP BY category ORDER BY SUM(amount) DESC LIMIT 1
    """)
    suspend fun getTopCategoryInRange(startDate: String, endDate: String): String?

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND (category IS NULL OR category = '' OR category = 'uncategorized')
          AND deleted_at IS NULL
    """)
    suspend fun countUncategorizedInRange(startDate: String, endDate: String): Int

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type = 'fuliza'
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun countFulizaInRange(startDate: String, endDate: String): Int

    // ─── Monthly wrapped queries ──────────────────────────────────────────────

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type = 'income'
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun getIncomeTotalInRange(startDate: String, endDate: String): Double

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun countSpendTransactions(startDate: String, endDate: String): Int

    @Query("""
        SELECT COUNT(DISTINCT substr(date, 1, 10)) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun countActiveDays(startDate: String, endDate: String): Int

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type = 'fuliza'
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun getFulizaTotalInRange(startDate: String, endDate: String): Double

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type = 'fuliza'
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun countFulizaTransactions(startDate: String, endDate: String): Int

    @Query("""
        SELECT merchant, amount, date FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
        ORDER BY amount DESC LIMIT 1
    """)
    suspend fun getBiggestSpend(startDate: String, endDate: String): BiggestSpend?

    @Query("SELECT MIN(date) FROM transactions WHERE deleted_at IS NULL")
    suspend fun getMinTransactionDate(): String?

    @Query("""
        SELECT COALESCE(SUM(fee), 0.0) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND fee > 0 AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun getFeeTotalInRange(startDate: String, endDate: String): Double

    // ─── Analytics tab helpers ────────────────────────────────────────────────

    @Query("""
        SELECT COALESCE(AVG(amount), 0.0) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun getAverageTransactionInRange(startDate: String, endDate: String): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND transaction_type = 'receive'
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun getIncomeInRange(startDate: String, endDate: String): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND (category IS NULL OR category = '' OR category = 'uncategorized')
          AND deleted_at IS NULL
    """)
    suspend fun getUncategorizedAmountInRange(startDate: String, endDate: String): Double

    /** Fee summary for a date range: total fees, top category, avg fee, tx count. */
    @Query("""
        SELECT
          COALESCE(SUM(amount), 0.0) AS total,
          COALESCE(AVG(amount), 0.0) AS avgFee,
          COUNT(*) AS txCount
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
          AND UPPER(category) IN ('AIRTIME','FULIZA','WITHDRAWAL','SUBSCRIPTION','FEE')
          AND deleted_at IS NULL
          AND status = 'completed'
    """)
    suspend fun getFeeSummaryInRange(startDate: String, endDate: String): FeeSummary

    // ─── Review queue helpers ─────────────────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE mpesa_code = :code AND deleted_at IS NULL LIMIT 1")
    suspend fun getByMpesaCode(code: String): TransactionEntity?

    @Query("UPDATE transactions SET status = :status, updated_at = :ts WHERE mpesa_code = :code AND deleted_at IS NULL")
    suspend fun updateStatusByMpesaCode(code: String, status: String, ts: String)

    /** Approve a review-queue transaction: mark completed and flag for sync. */
    @Query("UPDATE transactions SET status = :status, sync_state = :syncState, updated_at = :ts WHERE mpesa_code = :code AND deleted_at IS NULL")
    suspend fun updateStatusAndSyncStateByMpesaCode(code: String, status: String, syncState: String, ts: String)

    // ─── Insights tab deep queries ────────────────────────────────────────────────

    @Query("""
        SELECT
          strftime('%Y-%m', date) as month_key,
          COALESCE(SUM(CASE WHEN transaction_type IN ('expense','transfer','fuliza') THEN amount ELSE 0 END), 0.0) as expense,
          COALESCE(SUM(CASE WHEN transaction_type IN ('income','receive') THEN amount ELSE 0 END), 0.0) as income,
          COUNT(CASE WHEN transaction_type IN ('expense','transfer','fuliza') THEN 1 END) as tx_count
        FROM transactions
        WHERE date >= :sixMonthsAgo AND status = 'completed' AND deleted_at IS NULL
        GROUP BY month_key
        ORDER BY month_key ASC
    """)
    suspend fun getMonthlyTotalsRange(sixMonthsAgo: String): List<MonthlyTotalsRow>

    @Query("""
        SELECT
          strftime('%Y-%m', date) as month_key,
          category,
          COALESCE(SUM(amount), 0.0) as total
        FROM transactions
        WHERE date >= :sixMonthsAgo AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
        GROUP BY month_key, category
        ORDER BY month_key ASC, total DESC
    """)
    suspend fun getMonthlyCategoryBreakdown(sixMonthsAgo: String): List<MonthlyCategoryRow>

    @Query("""
        SELECT date(date) as dt
        FROM transactions
        WHERE transaction_type IN ('income','receive') AND status = 'completed' AND deleted_at IS NULL
          AND date >= :sixMonthsAgo
        ORDER BY date DESC LIMIT 12
    """)
    suspend fun getIncomeDates(sixMonthsAgo: String): List<IncomeDateRow>

    @Query("""
        SELECT
          COUNT(CASE WHEN amount < 500 THEN 1 END) as micro_count,
          COUNT(CASE WHEN amount >= 500 AND amount < 2000 THEN 1 END) as medium_count,
          COUNT(CASE WHEN amount >= 2000 THEN 1 END) as large_count,
          COALESCE(SUM(CASE WHEN amount < 500 THEN amount ELSE 0 END), 0.0) as micro_total,
          COALESCE(SUM(CASE WHEN amount >= 500 AND amount < 2000 THEN amount ELSE 0 END), 0.0) as medium_total,
          COALESCE(SUM(CASE WHEN amount >= 2000 THEN amount ELSE 0 END), 0.0) as large_total
        FROM transactions
        WHERE date >= :sixMonthsAgo AND transaction_type IN ('expense','transfer','fuliza')
          AND status = 'completed' AND deleted_at IS NULL
    """)
    suspend fun getSizeBreakdown(sixMonthsAgo: String): SizeBreakdownRow?
}

// ─── Projection data classes ─────────────────────────────────────────────────

data class MonthTotals(val income: Double?, val expense: Double?)

data class CategoryTotal(val category: String?, val total: Double)

data class MerchantTotal(val merchant: String?, val total: Double)

data class FeeCategoryTotal(val category: String?, val total: Double, val count: Int)

data class DaySpend(val day: String, val total: Double)

data class BiggestSpend(val merchant: String?, val amount: Double, val date: String)

data class FeeSummary(val total: Double, val avgFee: Double, val txCount: Int)

data class MonthlyTotalsRow(
    @ColumnInfo(name = "month_key") val monthKey: String,
    val expense: Double,
    val income: Double,
    @ColumnInfo(name = "tx_count") val txCount: Int,
)

data class MonthlyCategoryRow(
    @ColumnInfo(name = "month_key") val monthKey: String,
    val category: String?,
    val total: Double,
)

data class IncomeDateRow(val dt: String)

data class SizeBreakdownRow(
    @ColumnInfo(name = "micro_count") val microCount:  Int,
    @ColumnInfo(name = "medium_count") val mediumCount: Int,
    @ColumnInfo(name = "large_count") val largeCount:  Int,
    @ColumnInfo(name = "micro_total") val microTotal:  Double,
    @ColumnInfo(name = "medium_total") val mediumTotal: Double,
    @ColumnInfo(name = "large_total") val largeTotal:  Double,
)
