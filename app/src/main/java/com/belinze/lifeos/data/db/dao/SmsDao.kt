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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaybill(entry: PaybillRegistryEntity)

    // ─── ML training samples ──────────────────────────────────────────────────

    @Insert
    suspend fun insertSample(sample: MlTrainingSampleEntity)

    @Query("SELECT COUNT(*) FROM ml_training_samples")
    suspend fun countSamples(): Int

    @Query("SELECT * FROM ml_training_samples ORDER BY recorded_at DESC LIMIT :limit")
    suspend fun getSamples(limit: Int): List<MlTrainingSampleEntity>
}
