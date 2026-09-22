package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.*

/**
 * DAO for Room-owned tables adjacent to the SMS pipeline.
 *
 * `import_audit` and `sms_ingest_queue` are intentionally absent: they are
 * owned by the untouched parser (DbWriter) and accessed via SmsService.
 */
@Dao
interface SmsDao {
    // ─── Merchant categories ──────────────────────────────────────────────────

    @Query("SELECT * FROM merchant_categories WHERE merchant = :merchant AND deleted_at IS NULL")
    suspend fun getMerchantCategory(merchant: String): MerchantCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMerchantCategory(entity: MerchantCategoryEntity)

    // ─── Paybill registry ─────────────────────────────────────────────────────

    @Query("SELECT display_name FROM paybill_registry WHERE paybill_number = :paybillNumber")
    suspend fun getPaybillName(paybillNumber: String): String?

    @Query("SELECT * FROM paybill_registry ORDER BY usage_count DESC LIMIT :limit")
    suspend fun getTopPaybills(limit: Int): List<PaybillRegistryEntity>

    @Query("""
        INSERT INTO paybill_registry (paybill_number, display_name, last_seen_at, usage_count, last_amount_kes)
        VALUES (:paybillNumber, :displayName, :lastSeenAt, 1, :lastAmountKes)
        ON CONFLICT(paybill_number) DO UPDATE SET
            display_name = excluded.display_name,
            last_seen_at = excluded.last_seen_at,
            usage_count = paybill_registry.usage_count + 1,
            last_amount_kes = excluded.last_amount_kes
    """)
    suspend fun upsertPaybillUsage(
        paybillNumber: String,
        displayName: String,
        lastSeenAt: String,
        lastAmountKes: Double?,
    )

    // ─── ML training samples ──────────────────────────────────────────────────

    @Insert
    suspend fun insertSample(sample: MlTrainingSampleEntity)

    @Query("SELECT COUNT(*) FROM ml_training_samples")
    suspend fun countSamples(): Int

    @Query("SELECT * FROM ml_training_samples ORDER BY recorded_at DESC LIMIT :limit")
    suspend fun getSamples(limit: Int): List<MlTrainingSampleEntity>
}
