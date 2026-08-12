package com.belinze.lifeos.data.db.dao

import androidx.room.*
import com.belinze.lifeos.data.db.entity.*

/** DAO for SMS-pipeline-adjacent tables: audit log, ingest queue, merchant lookup. */
@Dao
interface SmsDao {

    // ─── Import audit ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM import_audit ORDER BY created_at DESC LIMIT :limit")
    suspend fun getAuditLog(limit: Int): List<ImportAuditEntity>

    @Query("SELECT * FROM import_audit WHERE outcome = 'rejected' ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentRejections(limit: Int): List<ImportAuditEntity>

    @Query("DELETE FROM import_audit")
    suspend fun clearAuditLog()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(entry: ImportAuditEntity)

    // ─── Ingest queue ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM sms_ingest_queue WHERE status = 'pending'")
    suspend fun countPending(): Int

    @Query("SELECT * FROM sms_ingest_queue WHERE status = 'quarantined' ORDER BY received_at DESC")
    suspend fun getQuarantined(): List<SmsIngestQueueEntity>

    @Query("UPDATE sms_ingest_queue SET status = 'pending', attempts = 0, next_retry_at = :now WHERE status = 'quarantined'")
    suspend fun retryAllQuarantined(now: String)

    @Query("UPDATE sms_ingest_queue SET status = 'pending', attempts = 0, next_retry_at = :now WHERE id = :id")
    suspend fun retryById(id: Long, now: String)

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
