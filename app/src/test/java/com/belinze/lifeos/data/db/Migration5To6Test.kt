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

/**
 * Proves MIGRATION_5_6 renames the assistant conversation id 'main' → 'default'
 * so a user's existing chat history stays visible after the constant change
 * (RFINAL's CONVERSATION_ID is 'default').
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Migration5To6Test {
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @BeforeTest
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ctx)
                .name(null) // in-memory
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(sdb: SupportSQLiteDatabase) {
                        sdb.execSQL(
                            """
                            CREATE TABLE assistant_messages (
                                id              TEXT NOT NULL PRIMARY KEY,
                                conversation_id TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        sdb.execSQL("INSERT INTO assistant_messages (id, conversation_id) VALUES ('a', 'main'), ('b', 'default')")
                    }

                    override fun onUpgrade(sdb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) { /* not used */ }
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
    fun `migration 5 to 6 renames main conversation to default and leaves default untouched`() {
        LifeOsDatabase.MIGRATION_5_6.migrate(db)

        db.query("SELECT conversation_id FROM assistant_messages WHERE id = 'a'").use { c ->
            c.moveToFirst()
            assertEquals("default", c.getString(0), "'main' row must be renamed to 'default'")
        }
        db.query("SELECT conversation_id FROM assistant_messages WHERE id = 'b'").use { c ->
            c.moveToFirst()
            assertEquals("default", c.getString(0), "existing 'default' row must stay 'default'")
        }
    }
}
