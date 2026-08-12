package com.belinze.lifeos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.belinze.lifeos.data.db.dao.*
import com.belinze.lifeos.data.db.entity.*

/**
 * Room database for LifeOS.
 *
 * Opens the same `lifeos.db` file that the existing SMS parser writes to.
 * WAL mode is enabled via [RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING] so
 * the parser's WorkManager workers can write concurrently.
 *
 * Migration strategy:
 *   The existing database was created by expo-sqlite with PRAGMA user_version = 0
 *   (or a small positive integer set by the TS migration runner). We define a
 *   no-op Migration(0 → 1) so Room takes ownership without touching the schema.
 *   All subsequent schema changes must be expressed as proper Room migrations.
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
        ImportAuditEntity::class,
        SmsIngestQueueEntity::class,
        CounterpartyOverrideEntity::class,
        MlTrainingSampleEntity::class,
    ],
    version = 1,
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

    companion object {
        const val DATABASE_NAME = "lifeos.db"

        /**
         * No-op migration: the schema already exists from expo-sqlite.
         * Room just records that it now owns versioning from here.
         *
         * IMPORTANT: Validate against a live database dump before releasing to
         * production. Run `PRAGMA table_info(table_name)` on `lifeos.db` and
         * compare every column against the entity definitions above.
         */
        val MIGRATION_0_1 = object : Migration(0, 1) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema already created by expo-sqlite; no DDL changes needed.
                // Room takes ownership of version tracking from this point.
            }
        }
    }
}
