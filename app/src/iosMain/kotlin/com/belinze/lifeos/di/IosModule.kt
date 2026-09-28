package com.belinze.lifeos.di

import com.belinze.lifeos.data.db.DatabaseDriverFactory
import com.belinze.lifeos.data.db.LifeOsDatabase
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
    single { LifeOsDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
