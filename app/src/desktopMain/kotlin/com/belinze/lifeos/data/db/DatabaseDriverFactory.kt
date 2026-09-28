package com.belinze.lifeos.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbPath = System.getProperty("user.home") + "/.lifeos/lifeos.db"
        File(dbPath).parentFile?.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        LifeOsDatabase.Schema.create(driver)
        return driver
    }
}
