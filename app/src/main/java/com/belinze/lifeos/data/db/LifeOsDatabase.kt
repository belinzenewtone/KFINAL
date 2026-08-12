package com.belinze.lifeos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.belinze.lifeos.data.db.dao.*
import com.belinze.lifeos.data.db.entity.*

/**
 * Room database for LifeOS.
 *
 * Opens the same `lifeos.db` file that the untouched SMS parser writes to.
 * WAL mode is enabled via [RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING] so
 * the parser's WorkManager workers can write concurrently.
 *
 * The SMS parser owns `import_audit` and `sms_ingest_queue` (created by
 * DbWriter.kt / schema.ts). Those tables are intentionally NOT declared here
 * because Room cannot express their exact DDL. They are read and written only
 * through SmsService → DbWriter.
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
    }
}
