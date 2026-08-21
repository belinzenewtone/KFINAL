package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.util.nowIso
import com.lifeos.sms.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// ─────────────────────────────────────────────────────────────────────────────
// ReviewQueueViewModel — full parity with ReviewQueueScreen.tsx
//
// Data source: SmsService audit log filtered to pending outcomes.
// (NOT TransactionViewModel — the old implementation was wrong.)
//
// Pending outcomes (substring match, mirroring React's outcome.includes()):
//   contains "quarantin" | contains "pending" | contains "review"
//   but NOT "imported_review_approved" / "dismissed" / "retried"
// ─────────────────────────────────────────────────────────────────────────────

/** Mirror React's outcome.includes() logic for queue membership. */
private fun isPendingOutcome(outcome: String): Boolean =
    (outcome.contains("quarantin") ||
     outcome.contains("pending") ||
     outcome.contains("review")) &&
    !outcome.contains("approved") &&
    !outcome.contains("dismissed") &&
    !outcome.contains("retried")

@Immutable
data class ReviewQueueUiState(
    val isLoading:   Boolean                             = true,
    val entries:     ImmutableList<SmsService.AuditEntry> = persistentListOf(),
    val dismissed:   Set<Long>                  = emptySet(),  // ids hidden without recovery
    val banner:      BannerState?               = null,
    val error:       String?                    = null,
)

@Immutable
data class BannerState(
    val message:  String,
    val isSuccess: Boolean,
)

@HiltViewModel
class ReviewQueueViewModel
    @Inject
    constructor(
    private val smsService:     SmsService,
    private val transactionDao: TransactionDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewQueueUiState())
    val uiState: StateFlow<ReviewQueueUiState> = _uiState.asStateFlow()

    init { load() }

    // ─── Derived helpers ──────────────────────────────────────────────────────

    /** Visible entries: pending outcomes and not locally dismissed. */
    val visibleEntries: List<SmsService.AuditEntry>
        get() {
            val state = _uiState.value
            return state.entries.filter {
                isPendingOutcome(it.outcome) && it.id !in state.dismissed
            }
        }

    // ─── Load ─────────────────────────────────────────────────────────────────

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val all = smsService.getAuditLog(500)
                val pending = all.filter { isPendingOutcome(it.outcome) }
                _uiState.update { it.copy(isLoading = false, entries = pending.toImmutableList()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Per-entry actions ────────────────────────────────────────────────────

    /**
     * Recover / Approve a single entry.
     * - quarantined → retrySingleQuarantined(id) — re-parse just this entry
     * - imported_review → set status='completed', sync_state='pending' + mark audit approved
     * - others → re-import sweep (batch_pending flows through the next sweep)
     */
    fun recoverEntry(entry: SmsService.AuditEntry) {
        viewModelScope.launch {
            try {
                when {
                    entry.outcome.contains("quarantin") -> {
                        val recovered = withContext(Dispatchers.IO) {
                            smsService.retrySingleQuarantined(entry.id)
                        }
                        showBanner(
                            if (recovered) "Transaction recovered" else "Could not parse — message stays quarantined",
                            recovered,
                        )
                    }
                    entry.outcome.contains("review") -> {
                        withContext(Dispatchers.IO) {
                            entry.mpesaCode?.let { code ->
                                // Mirror React: mark completed + set sync_state pending for sync
                                transactionDao.updateStatusAndSyncStateByMpesaCode(
                                    code, "completed", "pending", nowIso(),
                                )
                            }
                            // Mark audit row so it won't re-appear in the queue
                            smsService.markAuditApproved(entry.id)
                        }
                        showBanner("Transaction approved", true)
                    }
                    else -> {
                        withContext(Dispatchers.IO) { smsService.importHistoricalSms() }
                        showBanner("Re-import triggered", true)
                    }
                }
                load()
            } catch (e: Exception) {
                showBanner("Action failed: ${e.message}", false)
            }
        }
    }

    /**
     * Dismiss (soft-delete + audit mark) a single entry without recovering it.
     * Mirrors React which soft-deletes the transaction and marks the audit row dismissed.
     */
    fun dismissEntry(entry: SmsService.AuditEntry) {
        // Optimistically hide in UI immediately
        _uiState.update { it.copy(dismissed = it.dismissed + entry.id) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Soft-delete the transaction row if this code exists in DB
                    entry.mpesaCode?.let { code ->
                        transactionDao.getByMpesaCode(code)?.let { tx ->
                            transactionDao.softDelete(tx.id, nowIso())
                        }
                    }
                    // Mark the audit row as dismissed so it won't reload into the queue
                    smsService.dismissAuditEntry(entry.id)
                }
            } catch (_: Exception) {
                // DB write failed — the in-memory dismiss still holds for this session
            }
        }
    }

    // ─── Bulk actions ─────────────────────────────────────────────────────────

    /** Recover all visible entries at once. */
    fun recoverAll() {
        viewModelScope.launch {
            try {
                val visible = visibleEntries
                withContext(Dispatchers.IO) {
                    val ts = nowIso()
                    // Approve all imported_review entries: completed + sync_state pending + mark audit
                    val reviewEntries = visible.filter { it.outcome.contains("review") }
                    for (e in reviewEntries) {
                        e.mpesaCode?.let { code ->
                            transactionDao.updateStatusAndSyncStateByMpesaCode(
                                code, "completed", "pending", ts,
                            )
                        }
                        smsService.markAuditApproved(e.id)
                    }
                    // Retry all quarantined entries individually
                    val quarantinedEntries = visible.filter { it.outcome.contains("quarantin") }
                    for (e in quarantinedEntries) {
                        smsService.retrySingleQuarantined(e.id)
                    }
                    // BUG-F6: batch_pending entries are not quarantined/review — trigger one
                    // re-import sweep to pick them up (mirrors recoverEntry's else branch).
                    val batchPendingEntries = visible.filter {
                        !it.outcome.contains("quarantin") && !it.outcome.contains("review")
                    }
                    if (batchPendingEntries.isNotEmpty()) {
                        smsService.importHistoricalSms()
                    }
                }
                showBanner("All recoveries triggered", true)
                load()
            } catch (e: Exception) {
                showBanner("Recovery failed: ${e.message}", false)
            }
        }
    }

    /**
     * Dismiss all visible entries: soft-delete their transactions and mark audits dismissed.
     * Mirrors React's dismissAll which applies the same dismiss logic to every visible card.
     */
    fun dismissAll() {
        val visible = visibleEntries
        _uiState.update { it.copy(dismissed = it.dismissed + visible.map { e -> e.id }.toSet()) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val ts = nowIso()
                    for (e in visible) {
                        e.mpesaCode?.let { code ->
                            transactionDao.getByMpesaCode(code)?.let { tx ->
                                transactionDao.softDelete(tx.id, ts)
                            }
                        }
                    }
                    smsService.dismissAllAuditEntries(visible.map { it.id })
                }
            } catch (_: Exception) {
                // In-memory dismiss already applied — DB failure is non-fatal
            }
        }
    }

    // ─── Banner ───────────────────────────────────────────────────────────────

    private fun showBanner(message: String, isSuccess: Boolean) {
        _uiState.update { it.copy(banner = BannerState(message, isSuccess)) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(banner = null) }
    }
}
