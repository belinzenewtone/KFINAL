package com.belinze.lifeos.di

import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.AppDatabase
import com.belinze.lifeos.data.db.DatabaseDriverFactory
import com.belinze.lifeos.data.db.LifeOsDatabase
import com.belinze.lifeos.data.db.dao.AssistantDao
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.dao.IncomeDao
import com.belinze.lifeos.data.db.dao.LearningSessionDao
import com.belinze.lifeos.data.db.dao.PlannerDao
import com.belinze.lifeos.data.db.dao.SmsDao
import com.belinze.lifeos.data.db.dao.SmsPipelineDao
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.dao.impl.AssistantDaoImpl
import com.belinze.lifeos.data.db.dao.impl.BudgetDaoImpl
import com.belinze.lifeos.data.db.dao.impl.EventDaoImpl
import com.belinze.lifeos.data.db.dao.impl.IncomeDaoImpl
import com.belinze.lifeos.data.db.dao.impl.LearningSessionDaoImpl
import com.belinze.lifeos.data.db.dao.impl.PlannerDaoImpl
import com.belinze.lifeos.data.db.dao.impl.SmsDaoImpl
import com.belinze.lifeos.data.db.dao.impl.SmsPipelineDaoImpl
import com.belinze.lifeos.data.db.dao.impl.TaskDaoImpl
import com.belinze.lifeos.data.db.dao.impl.TransactionDaoImpl
import com.belinze.lifeos.ml.TransactionClassifier
import com.belinze.lifeos.services.BudgetAlertService
import com.belinze.lifeos.services.DarajaEnrichmentService
import com.belinze.lifeos.services.NotificationScheduler
import com.belinze.lifeos.services.NotificationSync
import com.belinze.lifeos.services.RuleBundleSync
import com.belinze.lifeos.viewmodel.AppViewModel
import com.belinze.lifeos.viewmodel.AssistantViewModel
import com.belinze.lifeos.viewmodel.BudgetViewModel
import com.belinze.lifeos.viewmodel.CategorizeViewModel
import com.belinze.lifeos.viewmodel.CsvImportViewModel
import com.belinze.lifeos.viewmodel.EventViewModel
import com.belinze.lifeos.viewmodel.ExportViewModel
import com.belinze.lifeos.viewmodel.FeeAnalyticsViewModel
import com.belinze.lifeos.viewmodel.InsightsViewModel
import com.belinze.lifeos.viewmodel.LearningViewModel
import com.belinze.lifeos.viewmodel.MerchantDetailViewModel
import com.belinze.lifeos.viewmodel.MonthlyWrappedViewModel
import com.belinze.lifeos.viewmodel.PlannerViewModel
import com.belinze.lifeos.viewmodel.ProfileViewModel
import com.belinze.lifeos.viewmodel.ReviewQueueViewModel
import com.belinze.lifeos.viewmodel.SearchViewModel
import com.belinze.lifeos.viewmodel.SettingsViewModel
import com.belinze.lifeos.viewmodel.SmsImportHealthViewModel
import com.belinze.lifeos.viewmodel.SmsImportViewModel
import com.belinze.lifeos.viewmodel.TaskViewModel
import com.belinze.lifeos.viewmodel.TransactionViewModel
import com.belinze.lifeos.viewmodel.WeekReviewViewModel
import com.lifeos.sms.SmsParserDatabase
import com.lifeos.sms.SmsService
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Database ──────────────────────────────────────────────────────────────
    single<DatabaseDriverFactory> { DatabaseDriverFactory(androidContext()) }

    single<LifeOsDatabase> {
        val factory = get<DatabaseDriverFactory>()
        val db = LifeOsDatabase(factory.createDriver())
        SmsParserDatabase.attach { factory.writableDatabase() }
        db
    }

    single<AppDatabase> {
        AppDatabase(get<LifeOsDatabase>(), get<DatabaseDriverFactory>())
    }

    // ── DAOs ──────────────────────────────────────────────────────────────────
    single<TransactionDao>     { TransactionDaoImpl(get<LifeOsDatabase>().transactionQueries) }
    single<TaskDao>            { TaskDaoImpl(get<LifeOsDatabase>().taskQueries) }
    single<EventDao>           { EventDaoImpl(get<LifeOsDatabase>().eventQueries) }
    single<BudgetDao>          { BudgetDaoImpl(get<LifeOsDatabase>().budgetQueries) }
    single<IncomeDao>          { IncomeDaoImpl(get<LifeOsDatabase>().incomeQueries) }
    single<AssistantDao>       { AssistantDaoImpl(get<LifeOsDatabase>().assistantMessageQueries) }
    single<LearningSessionDao> { LearningSessionDaoImpl(get<LifeOsDatabase>().learningSessionQueries) }
    single<PlannerDao> {
        val db = get<LifeOsDatabase>()
        PlannerDaoImpl(
            ruleQ   = db.recurringRuleQueries,
            billQ   = db.billQueries,
            goalQ   = db.goalQueries,
            loanQ   = db.fulizaLoanQueries,
            exportQ = db.exportQueries,
        )
    }
    single<SmsDao> {
        val db = get<LifeOsDatabase>()
        SmsDaoImpl(
            merchantQ = db.merchantCategoryQueries,
            paybillQ  = db.paybillRegistryQueries,
            mlQ       = db.mlTrainingSampleQueries,
        )
    }
    single<SmsPipelineDao> {
        val db = get<LifeOsDatabase>()
        SmsPipelineDaoImpl(
            queueQ = db.smsIngestQueueQueries,
            auditQ = db.importAuditQueries,
        )
    }

    // ── DataStore ─────────────────────────────────────────────────────────────
    single<AppPreferences> { AppPreferences(androidContext()) }

    // ── SMS ───────────────────────────────────────────────────────────────────
    single<SmsService> { SmsService(androidContext()) }

    // ── Services ──────────────────────────────────────────────────────────────
    single<NotificationScheduler>   { NotificationScheduler(androidContext()) }
    single<BudgetAlertService>      { BudgetAlertService(get(), get(), get(), get()) }
    single<NotificationSync>        { NotificationSync(androidContext(), get(), get(), get(), get(), get(), get()) }
    single<DarajaEnrichmentService> { DarajaEnrichmentService(get(), androidContext()) }
    single<RuleBundleSync>          { RuleBundleSync(androidContext(), get()) }
    single<TransactionClassifier>   { TransactionClassifier(get(), get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { AppViewModel(get(), get(), get()) }
    viewModel { AssistantViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { BudgetViewModel(get(), get()) }
    viewModel { CategorizeViewModel(get(), get(), get(), get(), get()) }
    viewModel { CsvImportViewModel(get()) }
    viewModel { EventViewModel(get(), get()) }
    viewModel { ExportViewModel(androidContext(), get(), get(), get(), get(), get(), get()) }
    viewModel { FeeAnalyticsViewModel(get()) }
    viewModel { InsightsViewModel(get()) }
    viewModel { LearningViewModel(get()) }
    viewModel { MerchantDetailViewModel(get()) }
    viewModel { MonthlyWrappedViewModel(get()) }
    viewModel { PlannerViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { ReviewQueueViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), androidContext()) }
    viewModel { SmsImportHealthViewModel(get(), get(), get()) }
    viewModel { SmsImportViewModel(androidContext(), get()) }
    viewModel { TaskViewModel(get(), get()) }
    viewModel { TransactionViewModel(get(), get(), get(), get()) }
    viewModel { WeekReviewViewModel(get(), get(), get()) }
}
