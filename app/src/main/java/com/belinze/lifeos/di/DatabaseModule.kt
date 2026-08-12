package com.belinze.lifeos.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.belinze.lifeos.data.db.LifeOsDatabase
import com.belinze.lifeos.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeOsDatabase {
        // IMPORTANT: expo-sqlite (JS layer) stores databases under `filesDir/SQLite/<name>`,
        // NOT the Android-default `databases/` directory. The SMS parser's DbWriter.kt also
        // opens the DB from `filesDir/SQLite/lifeos.db`. We must use the SAME path so Room
        // and DbWriter share one physical file and the WAL journal is consistent.
        //
        // Using Room.databaseBuilder with just a name would open `databases/lifeos.db` — a
        // separate empty file that contains none of the user's existing data. Always pass
        // the absolute path so the legacy expo-sqlite data is visible to Room from day one.
        val dbFile = File(context.filesDir, "SQLite").also { it.mkdirs() }
            .let { File(it, LifeOsDatabase.DATABASE_NAME) }

        return Room.databaseBuilder(
            context = context,
            klass   = LifeOsDatabase::class.java,
            name    = dbFile.absolutePath,
        )
            // WAL mode — required for concurrent writes from the SMS parser workers
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides fun provideTransactionDao(db: LifeOsDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideTaskDao(db: LifeOsDatabase): TaskDao               = db.taskDao()
    @Provides fun provideEventDao(db: LifeOsDatabase): EventDao             = db.eventDao()
    @Provides fun provideBudgetDao(db: LifeOsDatabase): BudgetDao           = db.budgetDao()
    @Provides fun provideIncomeDao(db: LifeOsDatabase): IncomeDao           = db.incomeDao()
    @Provides fun providePlannerDao(db: LifeOsDatabase): PlannerDao         = db.plannerDao()
    @Provides fun provideAssistantDao(db: LifeOsDatabase): AssistantDao     = db.assistantDao()
    @Provides fun provideSmsDao(db: LifeOsDatabase): SmsDao                 = db.smsDao()
}
