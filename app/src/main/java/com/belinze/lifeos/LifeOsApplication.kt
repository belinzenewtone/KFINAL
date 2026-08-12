package com.belinze.lifeos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.services.BudgetAlertService
import com.belinze.lifeos.services.NotificationSync
import com.belinze.lifeos.util.Haptics
import com.lifeos.sms.SmsService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point.
 *
 * @HiltAndroidApp triggers Hilt's code generation and sets up the component hierarchy.
 * Implements [Configuration.Provider] so Hilt can inject its WorkerFactory into WorkManager
 * (required for WorkManager workers that use @Inject constructor).
 */
@HiltAndroidApp
class LifeOsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Provided by SmsModule; initialised before any Activity or Worker runs. */
    @Inject
    lateinit var smsService: SmsService

    @Inject
    lateinit var prefs: AppPreferences

    @Inject
    lateinit var notificationSync: NotificationSync

    @Inject
    lateinit var budgetAlertService: BudgetAlertService

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Seed the haptics gate from the persisted preference (mirrors the RN
        // haptic() helper reading useAppStore.settings.hapticFeedback).
        Haptics.init(this)
        appScope.launch {
            try {
                val state = prefs.state.first()
                Haptics.enabled = state.hapticFeedback
                // Reconcile scheduled notifications after hydration (mirrors
                // syncAllNotifications in AppNavigator.tsx bootstrap).
                notificationSync.syncAll(state)
                // Re-evaluate budget thresholds on cold start in case spending
                // crossed a threshold while the app was closed (mirrors RN).
                budgetAlertService.checkAllBudgetThresholds(state)
            } catch (_: Exception) {
                Haptics.enabled = true
            }
        }

        // Arm the background SMS BroadcastReceiver, ensure the SmsReceiverModule
        // Compose stub is live, and schedule the periodic ingest sweep worker.
        smsService.initialize()
    }
}
