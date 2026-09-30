package com.lifeos.sms

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Public entry point for the app's DI module to hand the parser the app
 * database connection (single-writer architecture). Keeps [DbWriter] itself
 * internal.
 */
object SmsParserDatabase {
    fun attach(provider: () -> SupportSQLiteDatabase) = DbWriter.attachDatabase(provider)
}
