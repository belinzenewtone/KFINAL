package com.belinze.lifeos.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 3 gate: proves MIGRATION_2_3 correctly adopts the legacy parser-owned
 * tables into the Room schema.
 *
 * Strategy: recreate the EXACT legacy (pre-Room) DDL for `import_audit` and
 * `sms_ingest_queue` — including the `datetime('now')` expression defaults and
 * NOT NULLs that Room cannot declare — seed rows, run the migration, then
 * assert the rebuilt schema matches the Room entities and the data survived.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Migration2To3Test {
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @BeforeTest
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ctx)
                .name(null) // in-memory
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(sdb: SupportSQLiteDatabase) {
                        // ── Legacy DbWriter DDL (verbatim from RFINAL) ──
                        sdb.execSQL("""
                            CREATE TABLE IF NOT EXISTS import_audit (
                                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                                mpesa_code    TEXT,
                                raw_message   TEXT NOT NULL,
                                amount        REAL,
                                merchant      TEXT,
                                outcome       TEXT NOT NULL,
                                failure_reason TEXT,
                                confidence    TEXT,
                                created_at    TEXT NOT NULL DEFAULT (datetime('now'))
                            )
                        """.trimIndent())
                        sdb.execSQL("CREATE INDEX IF NOT EXISTS idx_import_audit_outcome ON import_audit(outcome)")
                        sdb.execSQL("CREATE INDEX IF NOT EXISTS idx_import_audit_created_at ON import_audit(created_at DESC)")
                        sdb.execSQL("""
                            CREATE TABLE IF NOT EXISTS sms_ingest_queue (
                                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                                body          TEXT NOT NULL,
                                body_hash     TEXT NOT NULL UNIQUE,
                                status        TEXT NOT NULL DEFAULT 'pending',
                                attempts      INTEGER NOT NULL DEFAULT 0,
                                last_error    TEXT,
                                received_at   TEXT NOT NULL DEFAULT (datetime('now')),
                                next_retry_at TEXT NOT NULL DEFAULT (datetime('now')),
                                claimed_at    TEXT,
                                sender_address TEXT DEFAULT ''
                            )
                        """.trimIndent())
                        sdb.execSQL("CREATE INDEX IF NOT EXISTS idx_ingest_pending ON sms_ingest_queue (status, next_retry_at)")
                        sdb.execSQL("CREATE INDEX IF NOT EXISTS idx_ingest_processing ON sms_ingest_queue (status, claimed_at)")

                        // Seed rows that must survive the migration.
                        sdb.execSQL("""
                            INSERT INTO import_audit (mpesa_code, raw_message, amount, merchant, outcome, failure_reason, confidence, created_at)
                            VALUES ('SB34MNO567', 'raw body', 1500.0, 'JANE DOE', 'imported_realtime', NULL, 'high', '2026-01-01T10:00:00')
                        """.trimIndent())
                        sdb.execSQL("""
                            INSERT INTO sms_ingest_queue (body, body_hash, status, attempts, sender_address)
                            VALUES ('some body', 'abc123', 'pending', 1, 'MPESA')
                        """.trimIndent())
                    }

                    override fun onUpgrade(sdb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) { /* legacy DBs never upgrade in tests */ }
                })
                .build()
        )
        db = helper.writableDatabase
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun `migration 2 to 3 rebuilds both tables and preserves data`() {
        LifeOsDatabase.MIGRATION_2_3.migrate(db)

        // ── Schema assertions: columns match the Room entities exactly ──
        val auditCols = tableInfo(db, "import_audit")
        assertEquals(
            listOf(
                "id", "mpesa_code", "raw_message", "amount", "merchant",
                "outcome", "failure_reason", "confidence", "created_at",
            ),
            auditCols.map { it.name },
        )
        // Expression defaults and NOT NULLs are gone (Room-declared shape).
        assertTrue(auditCols.all { it.defaultValue == null }, "no column may keep a legacy default")
        assertTrue(auditCols.first { it.name == "raw_message" }.notNull == false)

        val queueCols = tableInfo(db, "sms_ingest_queue")
        assertEquals(
            listOf("id", "body", "body_hash", "status", "attempts", "last_error", "received_at", "next_retry_at", "claimed_at", "sender_address"),
            queueCols.map { it.name },
        )
        assertEquals("'pending'", queueCols.first { it.name == "status" }.defaultValue)
        assertEquals("''", queueCols.first { it.name == "sender_address" }.defaultValue)

        // ── Data survival ──
        db.query("SELECT mpesa_code, outcome FROM import_audit").use { c ->
            assertTrue(c.moveToFirst(), "audit row must survive")
            assertEquals("SB34MNO567", c.getString(0))
            assertEquals("imported_realtime", c.getString(1))
        }
        db.query("SELECT body_hash, status, sender_address FROM sms_ingest_queue").use { c ->
            assertTrue(c.moveToFirst(), "queue row must survive")
            assertEquals("abc123", c.getString(0))
            assertEquals("pending", c.getString(1))
            assertEquals("MPESA", c.getString(2))
        }

        // ── New-shape writes work: writer supplies timestamps ──
        db.execSQL("""
            INSERT INTO import_audit (mpesa_code, raw_message, amount, merchant, outcome, created_at)
            VALUES ('XX1', 'b', 1.0, 'm', 'quarantined', '2026-08-21T00:00:00')
        """.trimIndent())
        db.query("SELECT COUNT(*) FROM import_audit").use { c ->
            c.moveToFirst(); assertEquals(2L, c.getLong(0))
        }
    }

    private fun tableInfo(db: SupportSQLiteDatabase, table: String): List<ColumnInfo> {
        val cols = mutableListOf<ColumnInfo>()
        db.query("PRAGMA table_info(`$table`)").use { c ->
            while (c.moveToNext()) {
                cols += ColumnInfo(
                    name         = c.getString(c.getColumnIndexOrThrow("name")),
                    type         = c.getString(c.getColumnIndexOrThrow("type")),
                    notNull      = c.getInt(c.getColumnIndexOrThrow("notnull")) == 1,
                    defaultValue = c.getString(c.getColumnIndexOrThrow("dflt_value")),
                )
            }
        }
        return cols
    }

    private data class ColumnInfo(
        val name: String,
        val type: String?,
        val notNull: Boolean,
        val defaultValue: String?,
    )
}
