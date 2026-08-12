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
import java.time.LocalDateTime
import java.time.ZoneId
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

    // ─── Parser-owned data access (import_audit / sms_ingest_queue) ────────────

    data class AuditEntry(
        val id: Long,
        val mpesaCode: String?,
        val rawMessage: String,
        val amount: Double?,
        val merchant: String?,
        val outcome: String,
        val failureReason: String?,
        val confidence: String?,
        val createdAt: String,
    )

    data class IngestQueueStatus(
        val pending: Long,
        val failed: Long,
        val oldestPendingAt: String?,
    )

    data class RejectionEntry(
        val reason: String,
        val rawSms: String,
        val timestampMs: Long,
    )

    fun getAuditLog(limit: Int = 100): List<AuditEntry> =
        DbWriter.getInstance(context).getAuditLog(limit.coerceIn(1, 500)).mapNotNull { raw ->
            val id = raw["id"] as? Long ?: return@mapNotNull null
            AuditEntry(
                id            = id,
                mpesaCode     = raw["mpesaCode"] as? String,
                rawMessage    = raw["rawMessage"] as? String ?: "",
                amount        = raw["amount"] as? Double,
                merchant      = raw["merchant"] as? String,
                outcome       = raw["outcome"] as? String ?: "",
                failureReason = raw["failureReason"] as? String,
                confidence    = raw["confidence"] as? String,
                createdAt     = raw["createdAt"] as? String ?: "",
            )
        }

    fun getRecentRejections(limit: Int = 20): List<RejectionEntry> =
        SmsParser.RejectionLog.recent(limit.coerceIn(1, 50)).map {
            RejectionEntry(
                reason      = it.reason,
                rawSms      = it.rawSms.take(200),
                timestampMs = it.timestampMs,
            )
        }

    fun getIngestQueueStatus(): IngestQueueStatus {
        val raw = DbWriter.getInstance(context).getIngestQueueStats()
        return IngestQueueStatus(
            pending         = raw["pending"] as? Long ?: 0L,
            failed          = raw["failed"] as? Long ?: 0L,
            oldestPendingAt = raw["oldestPendingAt"] as? String,
        )
    }

    /**
     * Re-parse every quarantined audit entry. Mirrors SmsReceiverModule.retryQuarantined
     * so the parser remains the single owner of its own tables and write logic.
     */
    fun retryQuarantinedMessages(): Pair<Int, Int> {
        val db = DbWriter.getInstance(context)
        val quarantined = db.getQuarantinedMessages()
        var importedCount = 0
        val ids = mutableListOf<Long>()

        for (entry in quarantined) {
            val rawMsg = entry["rawMessage"] as? String ?: continue
            val id = entry["id"] as? Long ?: continue
            ids.add(id)

            val receivedAtMs = isoToEpoch(entry["createdAt"] as? String)
            val result = SmsParser.parse(rawMsg, receivedAtMs)
            if (result !is SmsParser.SmsParseResult.Success) continue
            val tx = result.transaction
            if (tx.parseRoute == SmsParser.ParseRoute.QUARANTINE) continue

            val dupReason = SmsDedupeEngine.check(SmsDedupeEngine.Context(), tx, db)
            if (dupReason != SmsDedupeEngine.Result.NEW) continue

            val rowId = db.insertTransaction(tx)
            if (rowId >= 0) {
                db.insertAudit(tx.mpesaCode, rawMsg, tx.amount, tx.counterparty, "retry_imported")
                importedCount++
            }
        }
        if (ids.isNotEmpty()) db.markAuditRetried(ids)
        db.checkpoint()
        return quarantined.size to importedCount
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

    fun isBackgroundReceiverEnabled(): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BACKGROUND_RECEIVER, false)

    /**
     * Returns true if the app is currently exempt from battery optimizations.
     * Mirrors the React-native check used to show the battery-optimization warning.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun isoToEpoch(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            LocalDateTime.parse(iso)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    companion object {
        private const val TAG = "LifeOS/SmsService"
        private const val PREFS_NAME = "lifeos_sms_prefs"           // matches SmsReceiver.PREFS_NAME
        private const val KEY_BACKGROUND_RECEIVER = "background_receiver_enabled"
        private const val KEY_FULIZA_LIMIT_KES = "fuliza_limit_kes" // matches SmsProcessWorker.KEY_FULIZA_LIMIT
    }
}
