package com.belinze.lifeos.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * Thin wrapper around SQLDelight's [LifeOsDatabase] that provides the
 * legacy surface area still needed by a few call sites:
 *  - [SettingsViewModel.clearAllData] calls [clearAllTables]
 *  - [SmsImportHealthViewModel.load] / [repairDb] call [openHelper]
 *
 * The [openHelper] here wraps the underlying [SupportSQLiteDatabase] obtained
 * from [DatabaseDriverFactory] so PRAGMA queries keep working.
 */
class AppDatabase(
    val db: LifeOsDatabase,
    private val factory: DatabaseDriverFactory,
) {
    /** Minimal SupportSQLiteOpenHelper surface used only for PRAGMA queries. */
    val openHelper: OpenHelperProxy = OpenHelperProxy(factory)

    /** Wipes every table — mirrors RoomDatabase.clearAllTables(). */
    fun clearAllTables() {
        db.transactionQueries.transaction {
            db.transactionQueries.deleteAll()
        }
        db.taskQueries.transaction {
            db.taskQueries.deleteAll()
        }
        db.eventQueries.transaction {
            db.eventQueries.deleteAll()
        }
        db.budgetQueries.transaction {
            db.budgetQueries.deleteAll()
        }
        db.incomeQueries.transaction {
            db.incomeQueries.deleteAll()
        }
        db.assistantMessageQueries.transaction {
            db.assistantMessageQueries.deleteAll()
        }
        db.learningSessionQueries.transaction {
            db.learningSessionQueries.deleteAll()
        }
        db.recurringRuleQueries.transaction {
            db.recurringRuleQueries.deleteAll()
        }
        db.billQueries.transaction {
            db.billQueries.deleteAll()
        }
        db.goalQueries.transaction {
            db.goalQueries.deleteAll()
        }
        db.fulizaLoanQueries.transaction {
            db.fulizaLoanQueries.deleteAll()
        }
        db.exportQueries.transaction {
            db.exportQueries.deleteAll()
        }
        db.merchantCategoryQueries.transaction {
            db.merchantCategoryQueries.deleteAll()
        }
        db.paybillRegistryQueries.transaction {
            db.paybillRegistryQueries.deleteAll()
        }
        db.mlTrainingSampleQueries.transaction {
            db.mlTrainingSampleQueries.deleteAll()
        }
        db.importAuditQueries.transaction {
            db.importAuditQueries.deleteAll()
        }
        db.smsIngestQueueQueries.transaction {
            db.smsIngestQueueQueries.deleteAll()
        }
        db.appSettingQueries.transaction {
            db.appSettingQueries.deleteAll()
        }
        db.userProfileQueries.transaction {
            db.userProfileQueries.deleteAll()
        }
        db.counterpartyOverrideQueries.transaction {
            db.counterpartyOverrideQueries.deleteAll()
        }
    }

    /**
     * Minimal proxy that exposes the [SupportSQLiteDatabase] for PRAGMA queries.
     * Only [readableDatabase] and [writableDatabase] are needed.
     */
    class OpenHelperProxy(private val factory: DatabaseDriverFactory) {
        val writableDatabase: SupportSQLiteDatabase get() = factory.writableDatabase()
        val readableDatabase: SupportSQLiteDatabase get() = factory.writableDatabase()
    }
}
