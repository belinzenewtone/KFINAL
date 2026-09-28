package com.belinze.lifeos.di

import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.db.LifeOsDatabase
import com.belinze.lifeos.data.db.LifeOsDatabaseProvider
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
    single<LifeOsDatabase> {
        val db = LifeOsDatabaseProvider.get(androidContext())
        SmsParserDatabase.attach { db.openHelper.writableDatabase }
        db
    }

    // ── DAOs ──────────────────────────────────────────────────────────────────
    single<TransactionDao>    { get<LifeOsDatabase>().transactionDao() }
    single<TaskDao>           { get<LifeOsDatabase>().taskDao() }
    single<EventDao>          { get<LifeOsDatabase>().eventDao() }
    single<BudgetDao>         { get<LifeOsDatabase>().budgetDao() }
    single<IncomeDao>         { get<LifeOsDatabase>().incomeDao() }
    single<PlannerDao>        { get<LifeOsDatabase>().plannerDao() }
    single<AssistantDao>      { get<LifeOsDatabase>().assistantDao() }
    single<SmsDao>            { get<LifeOsDatabase>().smsDao() }
    single<LearningSessionDao>{ get<LifeOsDatabase>().learningSessionDao() }
    single<SmsPipelineDao>    { get<LifeOsDatabase>().smsPipelineDao() }

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
    viewModel { PlannerViewModel(get(), get()) }
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
