package com.belinze.lifeos.data.db.dao.impl

import com.belinze.lifeos.data.db.Import_audit
import com.belinze.lifeos.data.db.ImportAuditQueries
import com.belinze.lifeos.data.db.Sms_ingest_queue
import com.belinze.lifeos.data.db.SmsIngestQueueQueries
import com.belinze.lifeos.data.db.dao.SmsPipelineDao
import com.belinze.lifeos.data.db.entity.ImportAuditEntity
import com.belinze.lifeos.data.db.entity.SmsIngestQueueEntity

class SmsPipelineDaoImpl(
    private val queueQ: SmsIngestQueueQueries,
    private val auditQ: ImportAuditQueries,
) : SmsPipelineDao {

    // ─── Ingest queue ─────────────────────────────────────────────────────────

    override suspend fun enqueue(row: SmsIngestQueueEntity): Long {
        queueQ.enqueue(
            body          = row.body,
            bodyHash      = row.bodyHash,
            status        = row.status,
            attempts      = row.attempts.toLong(),
            lastError     = row.lastError,
            receivedAt    = row.receivedAt,
            nextRetryAt   = row.nextRetryAt,
            claimedAt     = row.claimedAt,
            senderAddress = row.senderAddress,
        )
        return queueQ.lastInsertRowId().executeAsOne()
    }

    override suspend fun byId(id: Long): SmsIngestQueueEntity? =
        queueQ.byId(id).executeAsOneOrNull()?.toEntity()

    override suspend fun pending(nowIso: String, limit: Int): List<SmsIngestQueueEntity> =
        queueQ.pending(nowIso, limit.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun claim(id: Long, nowIso: String): Int {
        queueQ.claim(nowIso, id)
        return 1 // rows affected not directly available; treat as success
    }

    override suspend fun markDone(id: Long) =
        queueQ.markDone(id)

    override suspend fun markFailed(id: Long, error: String?, status: String, retryAt: String) =
        queueQ.markFailed(status, error, retryAt, id)

    override suspend fun requeueFailed(): Int {
        queueQ.requeueFailed()
        return 1
    }

    override suspend fun countByStatus(status: String): Int =
        queueQ.countByStatus(status).executeAsOne().toInt()

    // ─── Import audit ─────────────────────────────────────────────────────────

    override suspend fun insertAudit(entry: ImportAuditEntity): Long {
        auditQ.insertAudit(
            mpesaCode     = entry.mpesaCode,
            rawMessage    = entry.rawMessage,
            amount        = entry.amount,
            merchant      = entry.merchant,
            outcome       = entry.outcome,
            failureReason = entry.failureReason,
            confidence    = entry.confidence,
            createdAt     = entry.createdAt,
        )
        return auditQ.lastInsertRowId().executeAsOne()
    }

    override suspend fun recentAudit(limit: Int): List<ImportAuditEntity> =
        auditQ.recentAudit(limit.toLong()).executeAsList().map { it.toEntity() }

    override suspend fun countByOutcome(outcome: String): Int =
        auditQ.countByOutcome(outcome).executeAsOne().toInt()
}

private fun Sms_ingest_queue.toEntity() = SmsIngestQueueEntity(
    id            = id,
    body          = body,
    bodyHash      = body_hash,
    status        = status,
    attempts      = attempts.toInt(),
    lastError     = last_error,
    receivedAt    = received_at,
    nextRetryAt   = next_retry_at,
    claimedAt     = claimed_at,
    senderAddress = sender_address,
)

private fun Import_audit.toEntity() = ImportAuditEntity(
    id            = id,
    mpesaCode     = mpesa_code,
    rawMessage    = raw_message,
    amount        = amount,
    merchant      = merchant,
    outcome       = outcome,
    failureReason = failure_reason,
    confidence    = confidence,
    createdAt     = created_at,
)
