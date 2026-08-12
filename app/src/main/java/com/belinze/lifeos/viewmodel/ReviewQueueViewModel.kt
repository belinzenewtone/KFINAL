package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.util.nowIso
import com.lifeos.sms.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// ReviewQueueViewModel — full parity with ReviewQueueScreen.tsx
//
// Data source: SmsService audit log filtered to pending outcomes.
// (NOT TransactionViewModel — the old implementation was wrong.)
//
// Pending outcomes: quarantined | imported_review | batch_pending | pending
// ─────────────────────────────────────────────────────────────────────────────

private val PENDING_OUTCOMES = setOf("quarantined", "imported_review", "batch_pending", "pending")

data class ReviewQueueUiState(
    val isLoading:   Boolean                    = true,
    val entries:     List<SmsService.AuditEntry> = emptyList(),
    val dismissed:   Set<Long>                  = emptySet(),  // ids hidden without recovery
    val banner:      BannerState?               = null,
    val error:       String?                    = null,
)

data class BannerState(
    val message:  String,
    val isSuccess: Boolean,
)

@HiltViewModel
class ReviewQueueViewModel @Inject constructor(
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
                it.outcome in PENDING_OUTCOMES && it.id !in state.dismissed
            }
        }

    // ─── Load ─────────────────────────────────────────────────────────────────

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val all = smsService.getAuditLog(500)
                val pending = all.filter { it.outcome in PENDING_OUTCOMES }
                _uiState.update { it.copy(isLoading = false, entries = pending) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Per-entry actions ────────────────────────────────────────────────────

    /**
     * Recover / Approve a single entry.
     * - quarantined → retry via SmsService (re-parse)
     * - imported_review → set transaction status to 'completed' in Room
     * - others → reload (batch_pending flows through the next sweep)
     */
    fun recoverEntry(entry: SmsService.AuditEntry) {
        viewModelScope.launch {
            try {
                when (entry.outcome) {
                    "quarantined" -> {
                        smsService.retryQuarantinedMessages()
                        showBanner("Recovery triggered", true)
                    }
                    "imported_review" -> {
                        entry.mpesaCode?.let { code ->
                            transactionDao.updateStatusByMpesaCode(code, "completed", nowIso())
                        }
                        showBanner("Transaction approved", true)
                    }
                    else -> {
                        smsService.importHistoricalSms()
                        showBanner("Re-import triggered", true)
                    }
                }
                load()
            } catch (e: Exception) {
                showBanner("Action failed: ${e.message}", false)
            }
        }
    }

    /** Dismiss (soft-hide) a single entry without recovering it. */
    fun dismissEntry(entry: SmsService.AuditEntry) {
        _uiState.update { it.copy(dismissed = it.dismissed + entry.id) }
    }

    // ─── Bulk actions ─────────────────────────────────────────────────────────

    /** Recover all quarantined entries at once. */
    fun recoverAll() {
        viewModelScope.launch {
            try {
                // Approve all imported_review entries in Room
                val reviewEntries = _uiState.value.entries.filter {
                    it.outcome == "imported_review" && it.id !in _uiState.value.dismissed
                }
                for (e in reviewEntries) {
                    e.mpesaCode?.let { code ->
                        transactionDao.updateStatusByMpesaCode(code, "completed", nowIso())
                    }
                }
                // Retry all quarantined via SmsService
                val hasQuarantined = _uiState.value.entries.any {
                    it.outcome == "quarantined" && it.id !in _uiState.value.dismissed
                }
                if (hasQuarantined) smsService.retryQuarantinedMessages()

                showBanner("All recoveries triggered", true)
                load()
            } catch (e: Exception) {
                showBanner("Recovery failed: ${e.message}", false)
            }
        }
    }

    /** Dismiss all visible entries (soft-hide, no DB action). */
    fun dismissAll() {
        val visible = visibleEntries.map { it.id }.toSet()
        _uiState.update { it.copy(dismissed = it.dismissed + visible) }
    }

    // ─── Banner ───────────────────────────────────────────────────────────────

    private fun showBanner(message: String, isSuccess: Boolean) {
        _uiState.update { it.copy(banner = BannerState(message, isSuccess)) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(banner = null) }
    }
}
