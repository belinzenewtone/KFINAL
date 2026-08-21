package com.belinze.lifeos.di

import android.content.Context
import com.belinze.lifeos.data.db.LifeOsDatabase
import com.belinze.lifeos.data.db.LifeOsDatabaseProvider
import com.belinze.lifeos.data.db.dao.*
import com.lifeos.sms.SmsParserDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LifeOsDatabase {
        val db = LifeOsDatabaseProvider.get(context)

        // Single-writer guarantee: the SMS parser executes on THIS Room
        // instance's connection. No second SQLiteOpenHelper exists anywhere.
        SmsParserDatabase.attach { db.openHelper.writableDatabase }

        return db
    }

    @Provides fun provideTransactionDao(db: LifeOsDatabase): TransactionDao = db.transactionDao()

    @Provides fun provideTaskDao(db: LifeOsDatabase): TaskDao = db.taskDao()

    @Provides fun provideEventDao(db: LifeOsDatabase): EventDao = db.eventDao()

    @Provides fun provideBudgetDao(db: LifeOsDatabase): BudgetDao = db.budgetDao()

    @Provides fun provideIncomeDao(db: LifeOsDatabase): IncomeDao = db.incomeDao()

    @Provides fun providePlannerDao(db: LifeOsDatabase): PlannerDao = db.plannerDao()

    @Provides fun provideAssistantDao(db: LifeOsDatabase): AssistantDao = db.assistantDao()

    @Provides fun provideSmsDao(db: LifeOsDatabase): SmsDao = db.smsDao()

    @Provides fun provideLearningSessionDao(db: LifeOsDatabase): LearningSessionDao = db.learningSessionDao()

    @Provides fun provideSmsPipelineDao(db: LifeOsDatabase): SmsPipelineDao = db.smsPipelineDao()
}
