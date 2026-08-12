package com.belinze.lifeos.data.db.dao

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
          AND status = 'completed'
    """)
    suspend fun countUncategorized(): Int

    @Query("""
        SELECT SUM(fee) FROM transactions
        WHERE deleted_at IS NULL
          AND date >= :startDate AND date <= :endDate
          AND fee IS NOT NULL AND fee > 0
    """)
    suspend fun getFeeTotal(startDate: String, endDate: String): Double?

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
}

// ─── Projection data classes ─────────────────────────────────────────────────

data class MonthTotals(val income: Double?, val expense: Double?)
data class CategoryTotal(val category: String?, val total: Double)
data class MerchantTotal(val merchant: String?, val total: Double)
