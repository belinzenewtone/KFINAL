package com.belinze.lifeos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.belinze.lifeos.data.db.dao.*
import com.belinze.lifeos.data.db.entity.*

/**
 * Room database for LifeOS — the SINGLE owner and writer of `lifeos.db`.
 *
 * The database file lives at `filesDir/SQLite/lifeos.db` (historical location
 * from the expo-sqlite era; kept so existing installs see their data).
 * The SMS parser's DbWriter executes on THIS database's connection via
 * [DbWriter.attachDatabase] — there is no second SQLiteOpenHelper anywhere.
 *
 * Schema history:
 *  v1 → v2: add learning_sessions (LE-1)
 *  v2 → v3: adopt parser-owned `import_audit` / `sms_ingest_queue` into the
 *           Room schema (rebuild: drops datetime() expression defaults that
 *           Room cannot declare; timestamps are now supplied by the writer).
 *  v3 → v4: drop orphaned legacy `idx_tx_*` indices that Room v3 validation
 *           rejects because they are not declared in @Entity annotations.
 */
@Database(
    entities = [
        TransactionEntity::class,
        TaskEntity::class,
        EventEntity::class,
        BudgetEntity::class,
        IncomeEntity::class,
        RecurringRuleEntity::class,
        BillEntity::class,
        GoalEntity::class,
        FulizaLoanEntity::class,
        MerchantCategoryEntity::class,
        PaybillRegistryEntity::class,
        AppSettingEntity::class,
        UserProfileEntity::class,
        ExportEntity::class,
        AssistantMessageEntity::class,
        CounterpartyOverrideEntity::class,
        MlTrainingSampleEntity::class,
        LearningSessionEntity::class,
        ImportAuditEntity::class,
        SmsIngestQueueEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun taskDao(): TaskDao

    abstract fun eventDao(): EventDao

    abstract fun budgetDao(): BudgetDao

    abstract fun incomeDao(): IncomeDao

    abstract fun plannerDao(): PlannerDao

    abstract fun assistantDao(): AssistantDao

    abstract fun smsDao(): SmsDao

    abstract fun learningSessionDao(): LearningSessionDao

    abstract fun smsPipelineDao(): SmsPipelineDao

    companion object {
        const val DATABASE_NAME = "lifeos.db"

        // LE-1: migration 1→2 — add learning_sessions table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop legacy idx_tx_* indices that were replaced by auto-named
                // index_transactions_* indices in the current TransactionEntity.
                // Room validates the full index set after migration; any extra
                // indices that are not declared in the @Entity will fail validation.
                db.execSQL("DROP INDEX IF EXISTS idx_tx_inst_cat")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_inst_date")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_inst_extref")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_semantic_hash")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_source_hash")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS learning_sessions (
                        id               TEXT    NOT NULL PRIMARY KEY,
                        title            TEXT    NOT NULL DEFAULT '',
                        category         TEXT    NOT NULL DEFAULT 'General',
                        description      TEXT,
                        duration_minutes INTEGER NOT NULL DEFAULT 0,
                        is_completed     INTEGER NOT NULL DEFAULT 0,
                        logged_at        TEXT,
                        created_at       TEXT,
                        updated_at       TEXT,
                        deleted_at       TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_sessions_category ON learning_sessions (category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_sessions_logged_at ON learning_sessions (logged_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_sessions_is_completed ON learning_sessions (is_completed)")
            }
        }

        // v2 → v3: adopt the parser-owned tables into the Room schema.
        // The legacy tables carry `DEFAULT (datetime('now'))` expression defaults
        // and extra NOT NULLs that Room cannot declare, so both are rebuilt and
        // the data copied. Timestamps are now supplied by the writer.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Defensive: some devices arrive at v2 still carrying legacy
                // idx_tx_* indices from the expo-sqlite era (MIGRATION_1_2 may
                // have skipped them if the old index names differed on that build).
                // Drop them here so MIGRATION_3_4 is not also needed for those.
                db.execSQL("DROP INDEX IF EXISTS idx_tx_inst_cat")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_inst_date")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_inst_extref")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_semantic_hash")
                db.execSQL("DROP INDEX IF EXISTS idx_tx_source_hash")

                db.execSQL("""
                    CREATE TABLE `_tmp_import_audit` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mpesa_code` TEXT,
                        `raw_message` TEXT,
                        `amount` REAL,
                        `merchant` TEXT,
                        `outcome` TEXT,
                        `failure_reason` TEXT,
                        `confidence` TEXT,
                        `created_at` TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `_tmp_import_audit`
                        (`id`,`mpesa_code`,`raw_message`,`amount`,`merchant`,`outcome`,`failure_reason`,`confidence`,`created_at`)
                    SELECT `id`,`mpesa_code`,`raw_message`,`amount`,`merchant`,`outcome`,`failure_reason`,`confidence`,`created_at`
                    FROM `import_audit`
                """.trimIndent())
                db.execSQL("DROP TABLE `import_audit`")
                db.execSQL("ALTER TABLE `_tmp_import_audit` RENAME TO `import_audit`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_import_audit_outcome` ON `import_audit` (`outcome`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_import_audit_created_at` ON `import_audit` (`created_at`)")

                db.execSQL("""
                    CREATE TABLE `_tmp_sms_ingest_queue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `body` TEXT NOT NULL,
                        `body_hash` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'pending',
                        `attempts` INTEGER NOT NULL DEFAULT 0,
                        `last_error` TEXT,
                        `received_at` TEXT,
                        `next_retry_at` TEXT,
                        `claimed_at` TEXT,
                        `sender_address` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `_tmp_sms_ingest_queue`
                        (`id`,`body`,`body_hash`,`status`,`attempts`,`last_error`,`received_at`,`next_retry_at`,`claimed_at`,`sender_address`)
                    SELECT `id`,`body`,`body_hash`,`status`,`attempts`,`last_error`,`received_at`,`next_retry_at`,`claimed_at`,COALESCE(`sender_address`,'')
                    FROM `sms_ingest_queue`
                """.trimIndent())
                db.execSQL("DROP TABLE `sms_ingest_queue`")
                db.execSQL("ALTER TABLE `_tmp_sms_ingest_queue` RENAME TO `sms_ingest_queue`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_ingest_queue_status_next_retry_at` ON `sms_ingest_queue` (`status`, `next_retry_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_ingest_queue_status_claimed_at` ON `sms_ingest_queue` (`status`, `claimed_at`)")
                // body_hash must be UNIQUE — matches @Index(unique = true) on SmsIngestQueueEntity
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_ingest_queue_body_hash` ON `sms_ingest_queue` (`body_hash`)")
            }
        }

        // v3 → v4: two schema drift fixes that survived MIGRATION_2_3:
        //  1. Drop orphaned legacy idx_tx_* indices from the expo-sqlite era —
        //     Room validates the exact index set declared in @Entity, so any
        //     undeclared index is a fatal mismatch.
        //  2. Recreate index_sms_ingest_queue_body_hash as UNIQUE — MIGRATION_2_3
        //     created it without the UNIQUE keyword but @Entity declares it unique.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1 — legacy transaction indices (IF EXISTS = no-op if already gone)
                db.execSQL("DROP INDEX IF EXISTS `idx_tx_inst_cat`")
                db.execSQL("DROP INDEX IF EXISTS `idx_tx_inst_date`")
                db.execSQL("DROP INDEX IF EXISTS `idx_tx_inst_extref`")
                db.execSQL("DROP INDEX IF EXISTS `idx_tx_semantic_hash`")
                db.execSQL("DROP INDEX IF EXISTS `idx_tx_source_hash`")

                // 2 — fix body_hash index uniqueness on sms_ingest_queue
                db.execSQL("DROP INDEX IF EXISTS `index_sms_ingest_queue_body_hash`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_ingest_queue_body_hash` ON `sms_ingest_queue` (`body_hash`)")
            }
        }
    }
}
