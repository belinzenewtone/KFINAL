package com.belinze.lifeos.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Process-wide holder/builder for [LifeOsDatabase].
 *
 * Single construction path for both entry points:
 *  - Hilt ([com.belinze.lifeos.di.DatabaseModule]) at app startup, and
 *  - the SMS parser's DbWriter on worker threads before/without DI.
 * Both end up with the SAME Room instance — one connection, one schema owner.
 */
object LifeOsDatabaseProvider {
    @Volatile
    private var db: LifeOsDatabase? = null

    fun get(context: Context): LifeOsDatabase =
        db ?: synchronized(this) {
            db ?: build(context.applicationContext).also { db = it }
        }

    /** Used by tests to force a specific (e.g. in-memory) instance. */
    fun setForTest(database: LifeOsDatabase?) {
        synchronized(this) { db = database }
    }

    fun build(context: Context): LifeOsDatabase {
        // Historical location from the expo-sqlite era. Kept so existing
        // installs see their data; Room is now the sole owner of this file.
        val dbFile = File(context.filesDir, "SQLite").also { it.mkdirs() }
            .let { File(it, LifeOsDatabase.DATABASE_NAME) }

        return Room.databaseBuilder(
            context = context,
            klass   = LifeOsDatabase::class.java,
            name    = dbFile.absolutePath,
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(
                LifeOsDatabase.MIGRATION_1_2,
                LifeOsDatabase.MIGRATION_2_3,
                LifeOsDatabase.MIGRATION_3_4,
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}
