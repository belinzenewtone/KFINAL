package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lifeos.sms.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * SmsImportViewModel — drives the Import SMS screen.
 *
 * Mirrors the SMS import state machine in FinanceScreen.tsx (detect/confirm/
 * import phases collapsed to a run-in-background action here). The in-app
 * progress banner is driven by observing the WorkManager worker state for the
 * unique work name "lifeos_historical_import", so the Finance screen shows
 * "Importing…" for the full duration of the worker, not just the enqueue call.
 */
@HiltViewModel
class SmsImportViewModel @Inject constructor(
    @ApplicationContext context: android.content.Context,
    private val smsService: SmsService,
) : ViewModel() {

    data class SmsImportUiState(
        val permissionGranted: Boolean = false,
        val isImporting:       Boolean = false,
        val banner:            String?  = null,
    )

    private val _uiState = MutableStateFlow(SmsImportUiState())
    val uiState: StateFlow<SmsImportUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        // Observe the historical-import worker so the banner reflects real progress.
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("lifeos_historical_import")
                .distinctUntilChanged()
                .collect { infos ->
                    val running = infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    _uiState.value = _uiState.value.copy(isImporting = running)
                }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            permissionGranted = granted,
            banner = if (granted) "SMS access granted · ready to import"
                     else "SMS permission denied · enable it in device Settings",
        )
    }

    fun setBanner(text: String) {
        _uiState.value = _uiState.value.copy(banner = text)
    }

    fun runImport(periodDays: Long?, filter: String) {
        if (_uiState.value.isImporting) return
        _uiState.value = _uiState.value.copy(banner = null)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val fromMs = periodDays?.let { now - TimeUnit.DAYS.toMillis(it) } ?: 0L
            smsService.importHistoricalSms(fromMs = fromMs, toMs = now, filter = filter)
            // The banner stays live via the WorkInfo observer above; enqueue
            // completes quickly, so only show the "started" text if the worker
            // hasn't flipped to RUNNING yet.
            val running = workManager.getWorkInfosForUniqueWorkFlow("lifeos_historical_import")
                .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
                .first()
            if (!running) {
                _uiState.value = _uiState.value.copy(banner = "Import started — this runs in the background.")
            }
        }
    }
}
