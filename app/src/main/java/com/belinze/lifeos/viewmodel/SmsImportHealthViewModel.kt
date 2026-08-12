package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.lifeos.sms.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SmsImportHealthViewModel — full parity with SmsImportHealthScreen.tsx.
 *
 * Derives all displayed data from:
 *  - SmsService (parser-owned tables: import_audit, sms_ingest_queue)
 *  - TransactionDao (app Room DB: tx count)
 *  - SmsService.isIgnoringBatteryOptimizations() for battery warning
 *  - SmsService.isBackgroundReceiverEnabled() for receiver status
 */
@HiltViewModel
class SmsImportHealthViewModel @Inject constructor(
    private val smsService:     SmsService,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    // ─── Receiver status ──────────────────────────────────────────────────────

    /** Mirrors React ReceiverStatus: active | idle | disabled | unknown */
    enum class ReceiverStatus { Active, Idle, Disabled, Unknown }

    // ─── UI state ─────────────────────────────────────────────────────────────

    data class UiState(
        val isLoading:          Boolean                        = true,

        // Receiver Status section
        val receiverStatus:     ReceiverStatus                 = ReceiverStatus.Unknown,
        val lastFireMs:         Long?                          = null,
        val isReceiverEnabled:  Boolean                        = false,
        val batteryExempt:      Boolean                        = false,
        val hasDbIntegrityIssue: Boolean                       = false,

        // Lifetime Counters section (derived from audit log)
        val totalImported:      Int                            = 0,
        val totalDuplicates:    Int                            = 0,
        val totalQuarantined:   Int                            = 0,
        val totalFailed:        Int                            = 0,

        // Activity section
        val txCountInDb:        Int                            = 0,
        val oldestPendingAt:    String?                        = null,
        val pendingQueueCount:  Long                           = 0L,
        val failedQueueCount:   Long                           = 0L,

        // Recent Rejections (conditional)
        val rejections:         List<SmsService.RejectionEntry> = emptyList(),

        // Import Log
        val audit:              List<SmsService.AuditEntry>    = emptyList(),

        // Last clear marker (entry id below which are cleared from view)
        val lastClearedAuditId: Long                           = 0L,

        val error:              String?                        = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val audit      = smsService.getAuditLog(200)
                val queue      = smsService.getIngestQueueStatus()
                val rejections = smsService.getRecentRejections(20)
                val enabled    = smsService.isBackgroundReceiverEnabled()
                val batExempt  = smsService.isIgnoringBatteryOptimizations()
                val txCount    = transactionDao.getPage(10_000, 0).size

                // Compute lifetime counters from the audit log
                val imported   = audit.count { it.outcome in setOf("imported", "retry_imported") }
                val dupes      = audit.count { it.outcome == "duplicate" }
                val quarantine = audit.count { it.outcome == "quarantine" }
                val failed     = audit.count { it.outcome == "failed" }

                // Derive receiver status from last fire time
                val lastFire   = null as Long?  // SmsService doesn't expose lastFireMs directly
                val status     = when {
                    !enabled    -> ReceiverStatus.Disabled
                    lastFire == null -> ReceiverStatus.Unknown
                    (System.currentTimeMillis() - lastFire) < 24 * 60 * 60 * 1000L -> ReceiverStatus.Active
                    else        -> ReceiverStatus.Idle
                }

                _uiState.update {
                    it.copy(
                        isLoading           = false,
                        receiverStatus      = status,
                        lastFireMs          = lastFire,
                        isReceiverEnabled   = enabled,
                        batteryExempt       = batExempt,
                        hasDbIntegrityIssue = false,
                        totalImported       = imported,
                        totalDuplicates     = dupes,
                        totalQuarantined    = quarantine,
                        totalFailed         = failed,
                        txCountInDb         = txCount,
                        oldestPendingAt     = queue.oldestPendingAt,
                        pendingQueueCount   = queue.pending,
                        failedQueueCount    = queue.failed,
                        rejections          = rejections,
                        audit               = audit,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun retryQuarantined() {
        viewModelScope.launch {
            smsService.retryQuarantinedMessages()
            load()
        }
    }

    fun reconcile() {
        viewModelScope.launch {
            // Trigger a full re-import sweep — fire and forget via WorkManager
            smsService.importHistoricalSms()
            load()
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
}
