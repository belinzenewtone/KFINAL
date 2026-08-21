package com.lifeos.sms

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Parser-side data access for lifeos.db.
 *
 * SINGLE-WRITER ARCHITECTURE (Phase 3 / PHASE0_DECISIONS.md D3): this class
 * owns NO SQLiteOpenHelper and creates NO schema. It executes on the Room
 * database's connection, supplied via [attachDatabase] by the app's DI module
 * ([com.belinze.lifeos.di.DatabaseModule]). Room is the sole schema owner —
 * all tables, including `import_audit` and `sms_ingest_queue`, are declared
 * as Room entities and created/migrated by Room.
 *
 * The public API surface is unchanged so workers/receivers/SmsService are
 * untouched by the migration.
 */
internal class DbWriter private constructor(private val db: SupportSQLiteDatabase) {
    // ── Connection shims over SupportSQLiteDatabase ───────────────────────────
    // Keep the historical call-shapes (rawQuery/execSQL with String args) so the
    // query bodies stay byte-identical to the pre-Room implementation.

    private fun rawQuery(sql: String, selectionArgs: Array<String>?): android.database.Cursor =
        db.query(SimpleSQLiteQuery(sql, selectionArgs))

    private fun execSQL(sql: String, args: Array<String>? = null) {
        if (args == null) db.execSQL(sql) else db.execSQL(sql, args)
    }

    /** Execute arbitrary SQL — only for integration test schema setup. */
    @androidx.annotation.VisibleForTesting
    internal fun execForTest(sql: String) = execSQL(sql)

    companion object {
        const val TAG = "LifeOS/DbWriter"

        @Volatile private var INSTANCE: DbWriter? = null

        @Volatile private var dbProvider: (() -> SupportSQLiteDatabase)? = null

        /**
         * Called once by the app's DI module to hand the parser the Room
         * database's writable connection. Workers may run before/without DI in
         * tests — [getInstance] throws if no provider was attached.
         */
        fun attachDatabase(provider: () -> SupportSQLiteDatabase) {
            synchronized(this) {
                dbProvider = provider
                INSTANCE = null // rebuild against the supplied connection
            }
        }

        /** Test seam: attach a raw connection directly. */
        @androidx.annotation.VisibleForTesting
        internal fun attachForTest(db: SupportSQLiteDatabase) = attachDatabase { db }

        fun getInstance(context: Context): DbWriter {
            val provider = dbProvider
                ?: error("DbWriter used before Room attachment — DatabaseModule must run first")
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DbWriter(provider()).also { INSTANCE = it }
            }
        }

        /** Reset the singleton between integration tests so each test gets a fresh DB. */
        @androidx.annotation.VisibleForTesting
        internal fun resetForTest() {
            synchronized(this) { INSTANCE = null }
        }

        // Pre-compiled for normalizeMerchant — avoids 2 Regex allocations per call.
        // At 10k bulk imports this eliminates 20k short-lived objects.
        private val MERCHANT_NON_ALNUM = Regex("""[^a-z0-9]+""")
        private val MERCHANT_WS        = Regex("""\s+""")
    }

    /**
     * Force a WAL checkpoint so any writes done through this connection become
     * immediately visible to other readers (e.g. backup/export tools). Called
     * after every batch import.
     */
    fun checkpoint() {
        try {
            rawQuery("PRAGMA wal_checkpoint(RESTART)", null).use { it.moveToFirst() }
        } catch (e: Exception) {
            Log.w(TAG, "wal_checkpoint failed: ${e.message}")
        }
    }

    /**
     * Rate-limited checkpoint for the realtime SMS path. Bulk paybill confirmations
     * arriving in a burst (within [windowMs] of each other) share a single checkpoint
     * instead of each one triggering its own WAL flush. Isolated messages are
     * unaffected — the first call in any 500ms window always checkpoints immediately.
     */
    @Volatile private var lastCheckpointMs = 0L

    fun checkpointDebounced(windowMs: Long = 500L) {
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (now - lastCheckpointMs < windowMs) return
            lastCheckpointMs = now
        }
        checkpoint()
    }

    // ── Bulk-import audit helpers ──────────────────────────────────────────────

    /**
     * Returns a pre-compiled INSERT statement for import_audit. The caller is
     * responsible for closing it (use `stmt.use { ... }`). Compile once per batch
     * transaction and reuse via [insertAuditReusing] — avoids one compilation per
     * row at ~10k rows/import.
     */
    fun compileAuditInsertStatement(): SupportSQLiteStatement =
        db.compileStatement(
            """INSERT INTO import_audit
               (mpesa_code, raw_message, amount, merchant, outcome, failure_reason, confidence, created_at)
               VALUES (?,?,?,?,?,?,?,?)"""
        )

    /**
     * Insert an audit row using a pre-compiled [stmt] from [compileAuditInsertStatement].
     * Bindings are cleared before each use so statement recycling is safe.
     */
    fun insertAuditReusing(
        stmt: SupportSQLiteStatement,
        mpesaCode: String?,
        rawMessage: String,
        amount: Double?,
        merchant: String?,
        outcome: String,
        failureReason: String? = null,
        confidence: String? = null,
    ) {
        try {
            stmt.clearBindings()
            if (mpesaCode != null) stmt.bindString(1, mpesaCode) else stmt.bindNull(1)
            stmt.bindString(2, rawMessage.take(1000))
            if (amount != null) stmt.bindDouble(3, amount) else stmt.bindNull(3)
            if (merchant != null) stmt.bindString(4, merchant) else stmt.bindNull(4)
            stmt.bindString(5, outcome)
            if (failureReason != null) stmt.bindString(6, failureReason) else stmt.bindNull(6)
            if (confidence != null) stmt.bindString(7, confidence) else stmt.bindNull(7)
            stmt.bindString(8, isoNow())
            stmt.executeInsert()
        } catch (e: Exception) {
            Log.e(TAG, "insertAuditReusing failed: ${e.message}", e)
        }
    }

    // ─── Transaction helpers ─────────────────────────────────────────────────

    fun beginTransaction() = db.beginTransaction()

    fun setTransactionSuccessful() = db.setTransactionSuccessful()

    fun endTransaction() = db.endTransaction()

    /**
     * Persist an incoming SMS body. Returns the queue row id, or the existing
     * row id when this exact body was already enqueued (duplicate broadcast),
     * or -1 on failure. Idempotent on body hash.
     */
    fun enqueueIngest(body: String, sender: String = ""): Long {
        val hash = sha256(body.trim())
        try {
            db.compileStatement(
                "INSERT OR IGNORE INTO sms_ingest_queue (body, body_hash, sender_address) VALUES (?, ?, ?)"
            ).use { stmt ->
                stmt.bindString(1, body)
                stmt.bindString(2, hash)
                stmt.bindString(3, sender)
                val rowId = stmt.executeInsert()
                if (rowId >= 0) return rowId
            }
            return rawQuery(
                "SELECT id FROM sms_ingest_queue WHERE body_hash = ? LIMIT 1",
                arrayOf(hash)
            ).use { c -> if (c.moveToFirst()) c.getLong(0) else -1L }
        } catch (e: Exception) {
            Log.e(TAG, "enqueueIngest failed: ${e.message}", e)
            return -1L
        }
    }

    /**
     * Variant for the inbox reconciliation scan: enqueue ONLY if this body has
     * never been seen by the queue. Returns true when a new row was inserted.
     * Single INSERT OR IGNORE — no follow-up SELECT on the hot no-op path.
     */
    private val INGEST_INSERT_SQL =
        "INSERT OR IGNORE INTO sms_ingest_queue (body, body_hash, sender_address) VALUES (?, ?, ?)"

    fun compileIngestInsertStatement(): SupportSQLiteStatement =
        db.compileStatement(INGEST_INSERT_SQL)

    fun enqueueIngestReusing(
        stmt: SupportSQLiteStatement,
        body: String,
        sender: String = "",
    ): Boolean {
        return try {
            stmt.clearBindings()
            stmt.bindString(1, body)
            stmt.bindString(2, sha256(body.trim()))
            stmt.bindString(3, sender)
            stmt.executeInsert() >= 0
        } catch (e: Exception) {
            Log.w(TAG, "enqueueIngestIfNew failed: ${e.message}")
            false
        }
    }

    fun enqueueIngestIfNew(body: String, sender: String = ""): Boolean =
        compileIngestInsertStatement().use { stmt ->
            enqueueIngestReusing(stmt, body, sender)
        }

    /** Terminal success — the message reached a final outcome (imported, duplicate, ignored, quarantined). */
    fun markIngestDone(id: Long) {
        if (id < 0) return
        try {
            execSQL(
                "UPDATE sms_ingest_queue SET status = 'done', last_error = NULL, claimed_at = NULL WHERE id = ?",
                arrayOf(id.toString())
            )
        } catch (e: Exception) {
            Log.w(TAG, "markIngestDone failed: ${e.message}")
        }
    }

    /**
     * Transient failure — bump attempts and schedule an exponential retry
     * (5min · 2^attempts, capped at 6h). After [maxAttempts] the row is marked
     * 'failed' and only a manual Reconcile/Retry will pick it up.
     */
    fun markIngestFailed(id: Long, error: String?, maxAttempts: Int = 8) {
        if (id < 0) return
        try {
            val attempts = rawQuery(
                "SELECT attempts FROM sms_ingest_queue WHERE id = ?", arrayOf(id.toString())
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 } + 1
            val delayMin = minOf(5L shl (attempts - 1).coerceAtMost(10), 360L)
            val status = if (attempts >= maxAttempts) "failed" else "pending"
            execSQL(
                """UPDATE sms_ingest_queue
                   SET attempts = ?, status = ?, last_error = ?,
                       next_retry_at = datetime('now', '+' || ? || ' minutes'),
                       claimed_at = NULL
                   WHERE id = ?""",
                arrayOf(attempts.toString(), status, error?.take(200) ?: "", delayMin.toString(), id.toString())
            )
        } catch (e: Exception) {
            Log.w(TAG, "markIngestFailed failed: ${e.message}")
        }
    }

    /**
     * Atomically claim an ingest row for processing. Returns true if this caller
     * won the race; false if another worker already claimed it.
     */
    fun claimIngestRow(id: Long): Boolean {
        if (id < 0) return false
        return try {
            execSQL(
                """UPDATE sms_ingest_queue
                   SET status = 'processing', claimed_at = datetime('now')
                   WHERE id = ? AND status IN ('pending', 'failed')""",
                arrayOf(id.toString())
            )
            rawQuery("SELECT changes()", null).use { c -> c.moveToFirst() && c.getInt(0) == 1 }
        } catch (e: Exception) {
            Log.w(TAG, "claimIngestRow failed: ${e.message}")
            false
        }
    }

    /** Read the authoritative body and sender for a queue row. */
    fun getIngestBodyAndSender(id: Long): Pair<String, String>? {
        if (id < 0) return null
        return try {
            rawQuery("SELECT body, sender_address FROM sms_ingest_queue WHERE id = ? LIMIT 1", arrayOf(id.toString()))
                .use { c -> if (c.moveToFirst()) (c.getString(0) ?: return null) to (c.getString(1) ?: "") else null }
        } catch (e: Exception) {
            Log.w(TAG, "getIngestBodyAndSender failed: ${e.message}")
            null
        }
    }

    /**
     * Pending rows whose retry time has arrived, plus rows stuck in processing
     * for more than 5 minutes (e.g. a worker process was killed mid-flight).
     * Drained by the sweep worker.
     */
    data class IngestRow(val id: Long, val body: String, val sender: String)

    fun getPendingIngest(limit: Int = 50): List<IngestRow> {
        val rows = mutableListOf<IngestRow>()
        try {
            rawQuery(
                """SELECT id, body, sender_address FROM sms_ingest_queue
                   WHERE (
                     status IN ('pending', 'failed') AND next_retry_at <= datetime('now')
                   ) OR (
                     status = 'processing' AND claimed_at < datetime('now', '-5 minutes')
                   )
                   ORDER BY id ASC LIMIT ?""",
                arrayOf(limit.toString())
            ).use { c ->
                while (c.moveToNext()) rows.add(IngestRow(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: ""))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getPendingIngest failed: ${e.message}")
        }
        return rows
    }

    /** Queue health for Import Health: pending count, failed count, oldest pending age. */
    fun getIngestQueueStats(): Map<String, Any?> {
        var pending = 0L; var failed = 0L; var oldestPendingAt: String? = null
        try {
            rawQuery(
                """SELECT
                   SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END),
                   MIN(CASE WHEN status = 'pending' THEN received_at END)
                   FROM sms_ingest_queue""",
                null
            ).use { c ->
                if (c.moveToFirst()) {
                    pending = if (c.isNull(0)) 0 else c.getLong(0)
                    failed = if (c.isNull(1)) 0 else c.getLong(1)
                    oldestPendingAt = c.getString(2)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getIngestQueueStats failed: ${e.message}")
        }
        return mapOf(
            "pending" to pending,
            "failed" to failed,
            "oldestPendingAt" to oldestPendingAt,
        )
    }

    /** Re-arm 'failed' rows for the sweep (manual Retry from Import Health). */
    fun requeueFailedIngest(): Int {
        return try {
            execSQL(
                "UPDATE sms_ingest_queue SET status = 'pending', attempts = 0, next_retry_at = datetime('now') WHERE status = 'failed'"
            )
            rawQuery("SELECT changes()", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        } catch (e: Exception) {
            Log.w(TAG, "requeueFailedIngest failed: ${e.message}")
            0
        }
    }

    /** Prune terminal rows older than [days] to keep the queue table small. */
    fun pruneIngestQueue(days: Int = 30) {
        try {
            execSQL(
                "DELETE FROM sms_ingest_queue WHERE status = 'done' AND received_at < datetime('now', '-' || ? || ' days')",
                arrayOf(days.toString())
            )
        } catch (e: Exception) {
            Log.w(TAG, "pruneIngestQueue failed: ${e.message}")
        }
    }

    // ─── Deduplication checks ─────────────────────────────────────────────────

    /**
     * Preload existing transaction dedup keys (mpesa_code, source_hash, semantic_hash)
     * from the DB into three sets in ONE query. Turns 30k dedup round-trips
     * (3 queries × 10k rows) into a single sequential scan for bulk imports.
     *
     * We restrict to non-deleted rows and cap to the most recent [limit] rows
     * (default 50k) to keep memory bounded on huge inboxes. Older rows still
     * fall through to the per-row `existsBy*` DB checks.
     */
    fun preloadDedupeHashes(
        seenCodes: MutableSet<String>,
        seenSourceHashes: MutableSet<String>,
        seenSemanticHashes: MutableSet<String>,
        limit: Int = 50_000,
    ) {
        try {
            rawQuery(
                """SELECT mpesa_code, source_hash, semantic_hash
                   FROM transactions
                   WHERE deleted_at IS NULL
                   ORDER BY date DESC
                   LIMIT ?""",
                arrayOf(limit.toString())
            ).use { c ->
                while (c.moveToNext()) {
                    c.getString(0)?.let { seenCodes.add(it) }
                    c.getString(1)?.let { seenSourceHashes.add(it) }
                    c.getString(2)?.let { seenSemanticHashes.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "preloadDedupeHashes failed: ${e.message}")
        }
    }

    fun existsByMpesaCode(code: String): Boolean {
        return rawQuery(
            "SELECT 1 FROM transactions WHERE mpesa_code = ? AND deleted_at IS NULL LIMIT 1",
            arrayOf(code)
        ).use { it.moveToFirst() }
    }

    fun existsByExternalRef(institutionId: String, ref: String): Boolean {
        return rawQuery(
            "SELECT 1 FROM transactions WHERE institution_id = ? AND external_ref = ? AND deleted_at IS NULL LIMIT 1",
            arrayOf(institutionId, ref)
        ).use { it.moveToFirst() }
    }

    fun existsBySourceHash(hash: String): Boolean {
        return rawQuery(
            "SELECT 1 FROM transactions WHERE source_hash = ? AND deleted_at IS NULL LIMIT 1",
            arrayOf(hash)
        ).use { it.moveToFirst() }
    }

    fun existsBySemanticHash(hash: String): Boolean {
        return rawQuery(
            "SELECT 1 FROM transactions WHERE semantic_hash = ? AND deleted_at IS NULL LIMIT 1",
            arrayOf(hash)
        ).use { it.moveToFirst() }
    }

    fun existsPotentialDuplicate(amount: Double, merchant: String, dateMs: Long, windowMs: Long = 300_000L): Boolean {
        // date column stores ISO 8601 with 'T' separator — compare as ISO strings directly.
        val loIso = epochToIso(dateMs - windowMs)
        val hiIso = epochToIso(dateMs + windowMs)
        return rawQuery(
            """SELECT 1 FROM transactions
               WHERE amount = ? AND merchant = ?
               AND date >= ? AND date <= ?
               AND deleted_at IS NULL LIMIT 1""",
            arrayOf(amount.toString(), merchant, loIso, hiIso)
        ).use { it.moveToFirst() }
    }

    // ─── Source hash ──────────────────────────────────────────────────────────

    fun sha256(input: String): String = HashUtils.sha256(input)

    // ─── Merchant category learning ───────────────────────────────────────────

    private fun normalizeMerchant(merchant: String?): String {
        if (merchant.isNullOrBlank()) return ""
        return merchant.lowercase()
            .replace(MERCHANT_NON_ALNUM, " ")
            .replace(MERCHANT_WS, " ")
            .trim()
    }

    private fun lookupMerchantCategory(merchant: String?): String? {
        val normalized = normalizeMerchant(merchant)
        if (normalized.isBlank()) return null
        return rawQuery(
            """SELECT category FROM merchant_categories
               WHERE merchant = ? AND deleted_at IS NULL
               ORDER BY user_corrected DESC, confidence DESC, updated_at DESC
               LIMIT 1""",
            arrayOf(normalized)
        ).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    // ─── Transaction insert ───────────────────────────────────────────────────

    private val TX_INSERT_SQL = """INSERT INTO transactions
        (id, amount, merchant, category, date, source, transaction_type,
         mpesa_code, source_hash, raw_sms, description, balance_after, fee,
         status, created_at, updated_at, sync_state, record_source,
         revision, inferred_category, inference_source, semantic_hash,
         institution_id, external_ref, currency, raw_sender)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"""

    fun compileTransactionInsertStatement(): SupportSQLiteStatement =
        db.compileStatement(TX_INSERT_SQL)

    fun insertTransactionReusing(
        stmt: SupportSQLiteStatement,
        tx: SmsParser.ParsedTransaction,
    ): Long {
        val id = UUID.randomUUID().toString()
        val now = isoNow()
        val appCategory = lookupMerchantCategory(tx.counterparty)
            ?: MerchantCategoryResolver.resolveLabel(tx.counterparty)
            ?: SmsParserConfig.refineAppCategory(tx.category, tx.counterparty, tx.amount)
        Log.d(TAG, "Insert ${tx.mpesaCode} ${tx.category} -> $appCategory amount=${tx.amount} date=${tx.date} cp=${tx.counterparty}")
        val dateIso = epochToIso(tx.date)
        val syncState = when (tx.parseRoute) {
            SmsParser.ParseRoute.DIRECT    -> "pending"
            SmsParser.ParseRoute.REVIEW    -> "pending_review"
            SmsParser.ParseRoute.QUARANTINE -> "quarantine"
        }
        return try {
            stmt.clearBindings()
            stmt.bindString(1, id)
            stmt.bindDouble(2, tx.amount)
            stmt.bindString(3, tx.counterparty ?: (SmsParserConfig.CATEGORY_DISPLAY[tx.category] ?: tx.institutionId))
            stmt.bindString(4, appCategory)
            stmt.bindString(5, dateIso)
            stmt.bindString(6, tx.institutionId)
            stmt.bindString(7, tx.transactionType)
            stmt.bindString(8, tx.mpesaCode)
            stmt.bindString(9, tx.sourceHash)
            stmt.bindString(10, tx.rawSms)
            stmt.bindString(11, tx.description)
            if (tx.balanceAfter != null) stmt.bindDouble(12, tx.balanceAfter) else stmt.bindNull(12)
            if (tx.fee != null) stmt.bindDouble(13, tx.fee) else stmt.bindNull(13)
            stmt.bindString(14, "completed")
            stmt.bindString(15, now)
            stmt.bindString(16, now)
            stmt.bindString(17, syncState)
            stmt.bindString(18, "sms_import")
            stmt.bindLong(19, 0)
            stmt.bindLong(20, 1)
            stmt.bindString(21, "sms_parser")
            stmt.bindString(22, tx.semanticHash)
            stmt.bindString(23, tx.institutionId)
            if (tx.externalRef.isNotBlank()) stmt.bindString(24, tx.externalRef) else stmt.bindNull(24)
            stmt.bindString(25, tx.currency)
            stmt.bindString(26, tx.rawSender)
            stmt.executeInsert()
        } catch (e: Exception) {
            Log.e(TAG, "insertTransactionReusing failed: ${e.message}", e)
            -1L
        }
    }

    /**
     * Inserts a parsed transaction into the transactions table.
     * Returns the new row ID, or -1 if the insert failed.
     */
    fun insertTransaction(tx: SmsParser.ParsedTransaction): Long =
        compileTransactionInsertStatement().use { stmt ->
            insertTransactionReusing(stmt, tx)
        }

    // ─── Fuliza outstanding balance ───────────────────────────────────────────

    /**
     * Records the authoritative outstanding Fuliza balance from a FULIZA_CHARGE SMS.
     * The outstanding amount IS the current balance — we store it as draw_amount_kes
     * so the UI can display the current balance without additional maths.
     * Updates the most recent active loan row; inserts a sentinel row if none exists.
     */
    fun setFulizaOutstanding(outstandingKes: Double) {
        try {
            val now = isoNow()
            val updated = db.compileStatement(
                "UPDATE fuliza_loans SET draw_amount_kes = ?, total_repaid_kes = 0, updated_at = ? WHERE status = 'active'"
            ).use { stmt ->
                stmt.bindDouble(1, outstandingKes)
                stmt.bindString(2, now)
                stmt.executeUpdateDelete()
            }
            if (updated == 0) {
                // No active loan row — insert a sentinel so the UI can display the balance
                db.compileStatement(
                    """INSERT INTO fuliza_loans
                       (id, draw_code, draw_amount_kes, total_repaid_kes, status, draw_date, created_at, updated_at)
                       VALUES (?,?,?,?,?,?,?,?)"""
                ).use { stmt ->
                    stmt.bindString(1, UUID.randomUUID().toString())
                    stmt.bindString(2, "FULIZA_CHARGE")
                    stmt.bindDouble(3, outstandingKes)
                    stmt.bindDouble(4, 0.0)
                    stmt.bindString(5, "active")
                    stmt.bindString(6, now)
                    stmt.bindString(7, now)
                    stmt.bindString(8, now)
                    stmt.executeInsert()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setFulizaOutstanding failed: ${e.message}", e)
        }
    }

    /**
     * Records a Fuliza repayment atomically through the native layer, eliminating the
     * write race where a concurrent JS expo-sqlite write could be overwritten by
     * setFulizaOutstanding() resetting total_repaid_kes to 0.
     *
     * Increments total_repaid_kes by [amountKes] on the active loan row and stamps
     * last_repayment_date. [availableLimitKes] is stored for diagnostic use.
     */
    fun setFulizaRepayment(amountKes: Double, availableLimitKes: Double) {
        try {
            val now = isoNow()
            val rows = db.compileStatement(
                """UPDATE fuliza_loans
                   SET total_repaid_kes = total_repaid_kes + ?,
                       last_repayment_date = ?,
                       updated_at = ?
                   WHERE status = 'active'"""
            ).use { stmt ->
                stmt.bindDouble(1, amountKes)
                stmt.bindString(2, now)
                stmt.bindString(3, now)
                stmt.executeUpdateDelete()
            }
            if (rows == 0) {
                Log.w(TAG, "setFulizaRepayment: no active loan row — repayment of $amountKes unrecorded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "setFulizaRepayment failed: ${e.message}", e)
        }
    }

    // ─── Fuliza outstanding balance ───────────────────────────────────────────

    /**
     * Returns the current active Fuliza outstanding balance, or 0.0 if no active loan row exists.
     */
    fun getFulizaOutstanding(): Double {
        return try {
            rawQuery(
                "SELECT draw_amount_kes - total_repaid_kes FROM fuliza_loans WHERE status = 'active' ORDER BY updated_at DESC LIMIT 1",
                null
            ).use { c ->
                if (c.moveToFirst()) c.getDouble(0) else 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    // ─── Import audit ─────────────────────────────────────────────────────────

    fun insertAudit(
        mpesaCode: String?,
        rawMessage: String,
        amount: Double?,
        merchant: String?,
        outcome: String,
        failureReason: String? = null,
        confidence: String? = null,
    ) {
        try {
            db.compileStatement(
                """INSERT INTO import_audit
                   (mpesa_code, raw_message, amount, merchant, outcome, failure_reason, confidence, created_at)
                   VALUES (?,?,?,?,?,?,?,?)"""
            ).use { stmt ->
                if (mpesaCode != null) stmt.bindString(1, mpesaCode) else stmt.bindNull(1)
                stmt.bindString(2, rawMessage.take(1000))
                if (amount != null) stmt.bindDouble(3, amount) else stmt.bindNull(3)
                if (merchant != null) stmt.bindString(4, merchant) else stmt.bindNull(4)
                stmt.bindString(5, outcome)
                if (failureReason != null) stmt.bindString(6, failureReason) else stmt.bindNull(6)
                if (confidence != null) stmt.bindString(7, confidence) else stmt.bindNull(7)
                stmt.bindString(8, isoNow())
                stmt.executeInsert()
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertAudit failed: ${e.message}", e)
        }
    }

    fun getAuditLog(limit: Int = 100): List<Map<String, Any?>> {
        val results = mutableListOf<Map<String, Any?>>()
        rawQuery(
            "SELECT id, mpesa_code, raw_message, amount, merchant, outcome, failure_reason, confidence, created_at FROM import_audit ORDER BY id DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                results.add(
                    mapOf(
                        "id"            to c.getLong(0),
                        "mpesaCode"     to c.getString(1),
                        "rawMessage"    to (c.getString(2) ?: ""),
                        "amount"        to if (c.isNull(3)) null else c.getDouble(3),
                        "merchant"      to c.getString(4),
                        "outcome"       to (c.getString(5) ?: ""),
                        "failureReason" to c.getString(6),
                        "confidence"    to c.getString(7),
                        "createdAt"     to (c.getString(8) ?: ""),
                    )
                )
            }
        }
        return results
    }

    fun getStats(): Map<String, Any?> {
        // Outcomes are written by workers as either exact strings ('imported_realtime',
        // 'quarantined', 'ignored_not_mpesa', …) or as `${category}:${reason}` (e.g.
        // 'parse_failed:no_code', 'duplicate_detected:mpesa_code'). Match via LIKE so
        // the suffixed variants aren't miscategorised.
        //
        // Only rows whose outcome contains "imported" or "realtime" count toward
        // the imported total. `retried`/`dismissed`/`ignored_*`/`fuliza_*` are
        // administrative and shouldn't inflate the imported count.
        var imported = 0L; var skipped = 0L; var errors = 0L; var quarantined = 0L; var lastAt: String? = null
        rawQuery(
            """SELECT
               SUM(CASE WHEN outcome LIKE 'imported%' OR outcome LIKE 'retry_imported%' THEN 1 ELSE 0 END),
               SUM(CASE WHEN outcome LIKE 'duplicate_detected%' THEN 1 ELSE 0 END),
               SUM(CASE WHEN outcome LIKE 'parse_failed%' OR outcome LIKE 'import_failed%' THEN 1 ELSE 0 END),
               SUM(CASE WHEN outcome LIKE 'quarantined%' THEN 1 ELSE 0 END),
               MAX(created_at)
               FROM import_audit""",
            null
        ).use { c ->
            if (c.moveToFirst()) {
                imported    = if (c.isNull(0)) 0 else c.getLong(0)
                skipped     = if (c.isNull(1)) 0 else c.getLong(1)
                errors      = if (c.isNull(2)) 0 else c.getLong(2)
                quarantined = if (c.isNull(3)) 0 else c.getLong(3)
                lastAt      = c.getString(4)
            }
        }
        return mapOf(
            "totalImported"    to imported,
            "totalDuplicates"  to skipped,
            "totalFailed"      to errors,
            "totalQuarantined" to quarantined,
            "lastImportAt"     to lastAt,
        )
    }

    fun getQuarantinedMessages(): List<Map<String, Any?>> {
        val results = mutableListOf<Map<String, Any?>>()
        rawQuery(
            "SELECT id, raw_message, mpesa_code, amount, merchant, created_at FROM import_audit WHERE outcome = 'quarantined' ORDER BY id DESC LIMIT 200",
            null
        ).use { c ->
            while (c.moveToNext()) {
                results.add(
                    mapOf(
                        "id"         to c.getLong(0),
                        "rawMessage" to (c.getString(1) ?: ""),
                        "mpesaCode"  to c.getString(2),
                        "amount"     to if (c.isNull(3)) null else c.getDouble(3),
                        "merchant"   to c.getString(4),
                        "createdAt"  to c.getString(5),
                    )
                )
            }
        }
        return results
    }

    fun markAuditRetried(ids: List<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        execSQL(
            "UPDATE import_audit SET outcome = 'retried' WHERE id IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray()
        )
    }

    fun markAuditDismissed(ids: List<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        execSQL(
            "UPDATE import_audit SET outcome = 'dismissed' WHERE id IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray()
        )
    }

    /** Clears all rows from the import audit log. The transactions table is untouched. */
    fun clearAuditLog(): Int {
        return try {
            execSQL("DELETE FROM import_audit")
            rawQuery("SELECT changes()", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "clearAuditLog failed: ${e.message}", e)
            0
        }
    }

    fun getQuarantinedById(id: Long): Map<String, Any?>? {
        return rawQuery(
            "SELECT id, raw_message, mpesa_code, amount, merchant, outcome, created_at FROM import_audit WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { c ->
            if (!c.moveToFirst()) return@use null
            mapOf(
                "id"         to c.getLong(0),
                "rawMessage" to (c.getString(1) ?: ""),
                "mpesaCode"  to c.getString(2),
                "amount"     to if (c.isNull(3)) null else c.getDouble(3),
                "merchant"   to c.getString(4),
                "outcome"    to (c.getString(5) ?: ""),
                "createdAt"  to c.getString(6),
            )
        }
    }

    /** Diagnostic counters — used to verify JS and native are looking at the same DB file. */
    fun getTransactionCount(): Long {
        return try {
            rawQuery("SELECT COUNT(*) FROM transactions WHERE deleted_at IS NULL", null).use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "getTransactionCount failed: ${e.message}")
            -1L
        }
    }

    fun getAuditCount(): Long {
        return try {
            rawQuery("SELECT COUNT(*) FROM import_audit", null).use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "getAuditCount failed: ${e.message}")
            -1L
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun isoNow(): String {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return java.time.LocalDateTime.now().format(fmt)
    }

    private fun epochToIso(epochMs: Long): String {
        val ldt = java.time.Instant.ofEpochMilli(epochMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        return java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ldt)
    }
}
