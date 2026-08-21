package com.lifeos.sms

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Public entry point for the app's DI module to hand the parser its Room
 * database connection (single-writer architecture — see PHASE0_DECISIONS.md
 * D3). Keeps [DbWriter] itself internal.
 */
object SmsParserDatabase {
    fun attach(provider: () -> SupportSQLiteDatabase) = DbWriter.attachDatabase(provider)
}
