package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.LifeOsDatabase
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.lifeos.sms.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * SmsImportHealthViewModel — full parity with SmsImportHealthScreen.tsx.
 *
 * Derives all displayed data from:
 *  - SmsService (parser-owned tables: import_audit, sms_ingest_queue)
 *  - LifeOsDatabase (app Room DB: tx count, integrity, js DB path)
 *  - SmsService.isIgnoringBatteryOptimizations() for battery warning
 *  - SmsService.getReceiverStatus() for realtime receiver state
 */
@HiltViewModel
class SmsImportHealthViewModel
    @Inject
    constructor(
    private val smsService:     SmsService,
    private val transactionDao: TransactionDao,
    private val database:       LifeOsDatabase,
) : ViewModel() {
    // ─── Receiver status ──────────────────────────────────────────────────────

    /** Mirrors React ReceiverStatus: active | idle | disabled | unknown */
    enum class ReceiverStatus { Active, Idle, Disabled, Unknown }

    // ─── UI state ─────────────────────────────────────────────────────────────

    @Immutable
    data class UiState(
        val isLoading:          Boolean                        = true,
        // Receiver Status section
        val receiverStatus:     ReceiverStatus                 = ReceiverStatus.Unknown,
        val lastFireMs:         Long?                          = null,
        val isReceiverEnabled:  Boolean                        = false,
        val batteryExempt:      Boolean                        = false,
        val hasDbIntegrityIssue: Boolean                       = false,
        val dbIntegrityMessage: String?                        = null,
        // Lifetime Counters section (from parser getStats)
        val totalImported:      Int                            = 0,
        val totalDuplicates:    Int                            = 0,
        val totalQuarantined:   Int                            = 0,
        val totalFailed:        Int                            = 0,
        val lastImportAt:       String?                        = null,
        // Activity section
        val txCountInDb:        Int                            = 0,
        val oldestPendingAt:    String?                        = null,
        val pendingQueueCount:  Long                           = 0L,
        val failedQueueCount:   Long                           = 0L,
        // Recent Rejections (conditional)
        val rejections:         ImmutableList<SmsService.RejectionEntry> = persistentListOf(),
        // Diagnostics
        val jsDbPath:           String?                        = null,
        val nativeDbPath:       String?                        = null,
        val nativeTxCount:      Long?                          = null,
        val nativeAuditCount:   Long?                          = null,
        // Import Log
        val audit:              ImmutableList<SmsService.AuditEntry> = persistentListOf(),
        // Last clear marker (entry id below which are cleared from view)
        val lastClearedAuditId: Long                           = 0L,
        // Actions in-flight
        val isReconciling:      Boolean                        = false,
        val isRetrying:         Boolean                        = false,
        // One-shot alert message from a completed action
        val resultMessage:      String?                        = null,
        val error:              String?                        = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
        // SH-18: auto-reload whenever the transaction count in DB changes
        viewModelScope.launch {
            transactionDao.observeRecent()
                .map { it.size }
                .distinctUntilChanged()
                .drop(1) // skip initial emission — load() handles the first fetch
                .collect { load() }
        }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val stats      = smsService.getStats()
                val audit      = smsService.getAuditLog(200)
                val queue      = smsService.getIngestQueueStatus()
                val rejections = smsService.getRecentRejections(20)
                val receiver   = smsService.getReceiverStatus()
                val batExempt  = smsService.isIgnoringBatteryOptimizations()
                val txCount    = transactionDao.getPage(10_000, 0).size

                // Receiver status: active | idle | disabled | unknown (mirrors React)
                val status = when {
                    !receiver.enabled -> ReceiverStatus.Disabled
                    receiver.lastFireMs == 0L -> ReceiverStatus.Unknown
                    (System.currentTimeMillis() - receiver.lastFireMs) < 24 * 60 * 60 * 1000L ->
                        ReceiverStatus.Active
                    else -> ReceiverStatus.Idle
                }

                // Enrich audit entries with smsDate from the transactions table
                // (mirrors enrichAuditWithSmsDates in React).
                val dateByCode = withContext(Dispatchers.IO) {
                    val codes = audit.mapNotNull { it.mpesaCode }.toSet()
                    val map = HashMap<String, String>()
                    for (code in codes) {
                        transactionDao.getByMpesaCode(code)?.let { tx ->
                            if (tx.date != null) map[code] = tx.date
                        }
                    }
                    map
                }
                val enrichedAudit = audit.map { entry ->
                    if (entry.mpesaCode != null && dateByCode.containsKey(entry.mpesaCode)) {
                        entry.copy(smsDate = dateByCode[entry.mpesaCode])
                    } else {
                        entry
                    }
                }

                // Diagnostics
                val integrity = runCatching {
                    database.openHelper.readableDatabase.query(
                        "PRAGMA integrity_check",
                    ).use { c ->
                        if (c.moveToFirst()) c.getString(0) ?: "ok" else "ok"
                    }
                }.getOrDefault("ok")

                val jsPath = runCatching {
                    database.openHelper.readableDatabase.query(
                        "PRAGMA database_list",
                    ).use { c ->
                        var path: String? = null
                        while (c.moveToNext()) {
                            if (c.getColumnIndexOrThrow("name").let { c.getString(it) } == "main") {
                                path = c.getString(c.getColumnIndexOrThrow("file"))
                            }
                        }
                        path
                    }
                }.getOrNull()

                val nativeDiag = runCatching { smsService.getNativeDiagnosticInfo() }.getOrNull()

                _uiState.update {
                    it.copy(
                        isLoading            = false,
                        receiverStatus       = status,
                        lastFireMs           = if (receiver.lastFireMs == 0L) null else receiver.lastFireMs,
                        isReceiverEnabled    = receiver.enabled,
                        batteryExempt        = batExempt,
                        hasDbIntegrityIssue  = integrity != "ok",
                        dbIntegrityMessage   = if (integrity == "ok") null else integrity,
                        totalImported        = stats.totalImported.toInt(),
                        totalDuplicates      = stats.totalDuplicates.toInt(),
                        totalQuarantined     = stats.totalQuarantined.toInt(),
                        totalFailed          = stats.totalFailed.toInt(),
                        lastImportAt         = stats.lastImportAt,
                        txCountInDb          = txCount,
                        oldestPendingAt      = queue.oldestPendingAt,
                        pendingQueueCount    = queue.pending,
                        failedQueueCount     = queue.failed,
                        rejections           = rejections.toImmutableList(),
                        jsDbPath             = jsPath,
                        nativeDbPath         = nativeDiag?.nativeDbPath,
                        nativeTxCount        = nativeDiag?.nativeTxCount,
                        nativeAuditCount     = nativeDiag?.nativeAuditCount,
                        audit                = enrichedAudit.toImmutableList(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun retryQuarantined() {
        if (_uiState.value.isRetrying || _uiState.value.isReconciling) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true, error = null) }
            try {
                val (quarantined, imported) = withContext(Dispatchers.IO) {
                    smsService.retryQuarantinedMessages()
                }
                load()
                _uiState.update { it.copy(
                    isRetrying = false,
                    resultMessage = "Retry complete — reprocessed $quarantined entries · $imported recovered",
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRetrying = false, error = e.message) }
            }
        }
    }

    fun reconcile() {
        if (_uiState.value.isRetrying || _uiState.value.isReconciling) return
        viewModelScope.launch {
            _uiState.update { it.copy(isReconciling = true, error = null) }
            try {
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                val result = withContext(Dispatchers.IO) {
                    smsService.importHistoricalSms(sevenDaysAgo, System.currentTimeMillis())
                }
                load()
                _uiState.update { it.copy(
                    isReconciling = false,
                    resultMessage = "Reconcile complete — scanned ${result.total} messages · " +
                        "${result.imported} new · ${result.duplicates} duplicates · ${result.failed} failed",
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isReconciling = false, error = e.message) }
            }
        }
    }

    fun retryIngestQueue() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { smsService.retryIngestQueue() }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearImportLog() {
        val topId = _uiState.value.audit.maxOfOrNull { it.id } ?: return
        _uiState.update { it.copy(lastClearedAuditId = topId) }
    }

    fun enableReceiver() {
        smsService.enableBackgroundReceiver()
        load()
    }

    fun disableReceiver() {
        smsService.disableBackgroundReceiver()
        load()
    }

    fun requestBatteryExemption() {
        // Mirrors the React requestIgnoreBatteryOptimizations behaviour.
        smsService.requestIgnoreBatteryOptimizations()
        load()
    }

    fun repairDb() {
        viewModelScope.launch {
            val integrity = withContext(Dispatchers.IO) {
                try {
                    // Mirror React's handleRepairDb: checkpoint the WAL first, then re-check.
                    database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
                } catch (_: Exception) {
                }
                database.openHelper.readableDatabase.query("PRAGMA integrity_check").use { c ->
                    if (c.moveToFirst()) c.getString(0) ?: "ok" else "ok"
                }
            }
            val ok = integrity == "ok"
            _uiState.update {
                it.copy(
                    hasDbIntegrityIssue  = !ok,
                    dbIntegrityMessage   = if (ok) null else integrity,
                    resultMessage        = if (ok) {
                        "Database repaired — the integrity check is now passing."
                    } else {
                        "Still corrupted — integrity check: $integrity\n\n" +
                            "Export your data from Finance → Export, then reinstall the app."
                    },
                )
            }
        }
    }
}
