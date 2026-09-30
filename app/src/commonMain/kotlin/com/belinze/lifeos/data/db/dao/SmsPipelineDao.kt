package com.belinze.lifeos.data.db.dao

import com.belinze.lifeos.data.db.entity.ImportAuditEntity
import com.belinze.lifeos.data.db.entity.SmsIngestQueueEntity

/**
 * Access to the SMS pipeline tables (`sms_ingest_queue`,
 * `import_audit`). The parser's DbWriter executes its own SQL on the same
 * connection; this DAO is the app-side, compile-time-checked surface for UI
 * and future refactors.
 */
interface SmsPipelineDao {
    // ─── Ingest queue ─────────────────────────────────────────────────────────

        suspend fun enqueue(row: SmsIngestQueueEntity): Long

        suspend fun byId(id: Long): SmsIngestQueueEntity?

        suspend fun pending(nowIso: String, limit: Int): List<SmsIngestQueueEntity>

        suspend fun claim(id: Long, nowIso: String): Int

        suspend fun markDone(id: Long)

        suspend fun markFailed(id: Long, error: String?, status: String, retryAt: String)

        suspend fun requeueFailed(): Int

        suspend fun countByStatus(status: String): Int

    // ─── Import audit ─────────────────────────────────────────────────────────

        suspend fun insertAudit(entry: ImportAuditEntity): Long

        suspend fun recentAudit(limit: Int): List<ImportAuditEntity>

        suspend fun countByOutcome(outcome: String): Int
}
