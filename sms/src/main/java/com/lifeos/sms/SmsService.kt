package com.lifeos.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmsService — the Compose-side bridge to the SMS parser module.
 *
 * Replaces SmsReceiverModule.kt (the Expo ReactNative bridge). Calls the same
 * underlying Kotlin classes directly — no bridge layer, no JS serialisation.
 *
 * Call [initialize] from Application.onCreate() to arm the background receiver,
 * register the SmsReceiverModule Compose stub, and schedule the periodic sweep.
 */
@Singleton
class SmsService @Inject constructor(
    private val context: Context,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * One-time setup called from Application.onCreate().
     *
     * 1. Ensures [SmsReceiverModule.instance] is live (so SmsProcessWorker events route
     *    to [SmsEventBus] from the very first SMS received after boot).
     * 2. Enables the background BroadcastReceiver via its SharedPreference gate.
     * 3. Schedules the 15-minute ingest sweep worker.
     */
    fun initialize() {
        Log.i(TAG, "Initializing SMS service")

        // Ensure the Compose SmsReceiverModule stub is alive. The default companion
        // value already sets an instance, but re-assign here to make the lifecycle
        // explicit and to support test teardown scenarios.
        if (SmsReceiverModule.instance == null) {
            SmsReceiverModule.instance = SmsReceiverModule()
        }

        // Enable the background BroadcastReceiver. SmsReceiver checks this pref in
        // isBackgroundReceiverEnabled() before passing received SMSs to WorkManager.
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BACKGROUND_RECEIVER, true)
            .apply()

        // Schedule the periodic sweep — idempotent (KEEP policy).
        ensureIngestSweep()

        Log.i(TAG, "SMS service ready — background receiver enabled, sweep scheduled")
    }

    // ─── Permissions ─────────────────────────────────────────────────────────

    fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED

    fun hasReceiveSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED

    fun hasAllSmsPermissions(): Boolean =
        hasReadSmsPermission() && hasReceiveSmsPermission()

    // ─── WorkManager scheduling ───────────────────────────────────────────────

    /**
     * Schedules the 15-minute ingest sweep worker (idempotent — KEEP policy).
     * Called from [initialize]; can also be called directly from a settings screen.
     */
    fun ensureIngestSweep() {
        val request = PeriodicWorkRequestBuilder<IngestSweepWorker>(
            15, TimeUnit.MINUTES,
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "lifeos_ingest_sweep",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Triggers a one-time historical SMS import from the device inbox.
     * Mirrors SmsReceiverModule.importHistoricalSms() from the RN app.
     *
     * @param fromMs start of the scan window (epoch millis)
     * @param toMs   end of the scan window (epoch millis)
     * @param filter "mpesa_only" | "banks_only" | "all" — which institutions to scan
     */
    fun importHistoricalSms(
        fromMs: Long = 0L,
        toMs:   Long = System.currentTimeMillis(),
        filter: String = "all",
    ) {
        val request = OneTimeWorkRequestBuilder<SmsImportWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putLong(SmsImportWorker.KEY_FROM_MS, fromMs)
                    .putLong(SmsImportWorker.KEY_TO_MS, toMs)
                    .putString(SmsImportWorker.KEY_FILTER, filter)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            "lifeos_historical_import",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Retry messages currently awaiting review in the ingest queue.
     * Mirrors SmsReceiverModule.retryQuarantined() from the RN app.
     */
    fun retryQuarantined() {
        DbWriter.getInstance(context).requeueFailedIngest()
        // Sweep worker will drain the re-armed rows on its next tick.
        ensureIngestSweep()
    }

    // ─── Fuliza limit ─────────────────────────────────────────────────────────

    /**
     * Persists the user-configured Fuliza credit limit so [FulizaProjection] can
     * compute the projected balance accurately. Stored in the SharedPreferences
     * silo that SmsProcessWorker reads via [SmsReceiver.PREFS_NAME].
     */
    fun setFulizaLimit(limitKes: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_FULIZA_LIMIT_KES, limitKes.toFloat())
            .apply()
        Log.d(TAG, "Fuliza limit set to $limitKes KES")
    }

    fun getFulizaLimit(): Double =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_FULIZA_LIMIT_KES, 0f).toDouble()

    // ─── Receiver lifecycle ───────────────────────────────────────────────────

    /**
     * Disables the background SMS receiver gate. The manifest-registered
     * BroadcastReceiver still exists but [SmsReceiver.isBackgroundReceiverEnabled]
     * returns false, causing it to silently drop incoming messages.
     */
    fun disableBackgroundReceiver() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BACKGROUND_RECEIVER, false)
            .apply()
    }

    fun enableBackgroundReceiver() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BACKGROUND_RECEIVER, true)
            .apply()
    }

    companion object {
        private const val TAG = "LifeOS/SmsService"
        private const val PREFS_NAME = "lifeos_sms_prefs"           // matches SmsReceiver.PREFS_NAME
        private const val KEY_BACKGROUND_RECEIVER = "background_receiver_enabled"
        private const val KEY_FULIZA_LIMIT_KES = "fuliza_limit_kes" // matches SmsProcessWorker.KEY_FULIZA_LIMIT
    }
}
