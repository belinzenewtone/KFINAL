package com.belinze.lifeos

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.di.appModule
import com.belinze.lifeos.services.BudgetAlertService
import com.belinze.lifeos.services.NotificationSync
import com.belinze.lifeos.services.RuleBundleSync
import com.belinze.lifeos.util.Haptics
import com.lifeos.sms.SmsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.io.File

class LifeOsApplication : Application(), Configuration.Provider {
    private val smsService: SmsService by inject()
    private val prefs: AppPreferences by inject()
    private val notificationSync: NotificationSync by inject()
    private val budgetAlertService: BudgetAlertService by inject()
    private val ruleBundleSync: RuleBundleSync by inject()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@LifeOsApplication)
            modules(appModule)
        }

        installCrashLogger()

        Haptics.init(this)
        appScope.launch {
            try {
                val state = prefs.state.first()
                Haptics.enabled = state.hapticFeedback
                notificationSync.syncAll(state)
                budgetAlertService.checkAllBudgetThresholds(state)
            } catch (e: Exception) {
                Log.e("LifeOS/App", "Startup sync failed", e)
                Haptics.enabled = true
            }
        }

        appScope.launch {
            try {
                ruleBundleSync.initialize()
            } catch (e: Exception) {
                Log.e("LifeOS/App", "Rule bundle sync failed", e)
            }
        }

        try {
            smsService.initialize()
        } catch (e: Exception) {
            Log.e("LifeOS/App", "SMS service init failed", e)
        }
    }

    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(filesDir, "crash.log")
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                file.appendText(
                    "=== ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())} ===\n" +
                    "Thread: ${thread.name}\n$sw\n\n"
                )
                Log.e("LifeOS/Crash", "Uncaught on ${thread.name}", throwable)
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
