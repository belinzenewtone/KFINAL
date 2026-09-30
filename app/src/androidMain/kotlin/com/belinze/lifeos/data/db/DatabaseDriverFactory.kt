package com.belinze.lifeos.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File

/**
 * Android-specific database driver factory for SQLDelight.
 *
 * Keeps a reference to the underlying SupportSQLiteOpenHelper so the
 * SMS parser (DbWriter / SmsParserDatabase) can attach to the same
 * database connection without a second file handle — single-writer
 * architecture is preserved.
 *
 * Historical database location from the expo-sqlite era (filesDir/SQLite/lifeos.db)
 * is retained so existing installs keep their data.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    // Lazy-create the helper so it is only opened when createDriver() is first called.
    private val helper: SupportSQLiteOpenHelper by lazy {
        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val dbFile = File(context.filesDir, "SQLite").also { it.mkdirs() }
            .let { File(it, "lifeos.db") }
        factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbFile.absolutePath)
                .callback(AndroidSqliteDriver.Callback(LifeOsDatabase.Schema))
                .build()
        )
    }

    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(helper.writableDatabase)

    /**
     * Exposes the writable database for the SMS parser bridge.
     * Must be called after [createDriver] to ensure the schema is created.
     */
    fun writableDatabase(): SupportSQLiteDatabase = helper.writableDatabase
}
