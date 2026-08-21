package com.belinze.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.belinze.lifeos.data.db.entity.ImportAuditEntity
import com.belinze.lifeos.data.db.entity.SmsIngestQueueEntity

/**
 * Room-native access to the SMS pipeline tables (`sms_ingest_queue`,
 * `import_audit`). The parser's DbWriter executes its own SQL on the same
 * connection; this DAO is the app-side, compile-time-checked surface for UI
 * and future refactors.
 */
@Dao
interface SmsPipelineDao {
    // ─── Ingest queue ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(row: SmsIngestQueueEntity): Long

    @Query("SELECT * FROM sms_ingest_queue WHERE id = :id")
    suspend fun byId(id: Long): SmsIngestQueueEntity?

    @Query("""
        SELECT * FROM sms_ingest_queue
        WHERE status = 'pending' AND next_retry_at <= :nowIso
        ORDER BY received_at
        LIMIT :limit
    """)
    suspend fun pending(nowIso: String, limit: Int): List<SmsIngestQueueEntity>

    @Query("""
        UPDATE sms_ingest_queue SET status = 'processing', claimed_at = :nowIso
        WHERE id = :id AND status IN ('pending', 'failed')
    """)
    suspend fun claim(id: Long, nowIso: String): Int

    @Query("""
        UPDATE sms_ingest_queue SET status = 'done', last_error = NULL, claimed_at = NULL
        WHERE id = :id
    """)
    suspend fun markDone(id: Long)

    @Query("""
        UPDATE sms_ingest_queue
        SET attempts = attempts + 1, status = :status, last_error = :error,
            next_retry_at = :retryAt, claimed_at = NULL
        WHERE id = :id
    """)
    suspend fun markFailed(id: Long, error: String?, status: String, retryAt: String)

    @Query("UPDATE sms_ingest_queue SET status = 'pending' WHERE status = 'failed'")
    suspend fun requeueFailed(): Int

    @Query("SELECT COUNT(*) FROM sms_ingest_queue WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    // ─── Import audit ─────────────────────────────────────────────────────────

    @Insert
    suspend fun insertAudit(entry: ImportAuditEntity): Long

    @Query("SELECT * FROM import_audit ORDER BY id DESC LIMIT :limit")
    suspend fun recentAudit(limit: Int): List<ImportAuditEntity>

    @Query("SELECT COUNT(*) FROM import_audit WHERE outcome = :outcome")
    suspend fun countByOutcome(outcome: String): Int
}
