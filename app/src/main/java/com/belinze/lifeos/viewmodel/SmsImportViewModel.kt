package com.belinze.lifeos.viewmodel

import android.database.Cursor
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lifeos.sms.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
class SmsImportViewModel
    @Inject
    constructor(
    @ApplicationContext private val context: android.content.Context,
    private val smsService: SmsService,
) : ViewModel() {
    @Immutable
    data class ImportProgress(
        val imported: Int,
        val total:    Int,
        val quarantined: Int = 0,
    )

    @Immutable
    data class ImportResult(
        val imported:    Int,
        val total:       Int,
        val duplicates:  Int,
        val quarantined: Int,
        val failed:      Int,
    )

    @Immutable
    data class DetectedInstitution(val institutionId: String, val count: Int)

    /** Mirrors RN's INSTITUTION_DISPLAY map (ImportSmsSheet.tsx). */
    companion object {
        val INSTITUTION_DISPLAY: Map<String, String> = mapOf(
            "mpesa" to "M-Pesa",
            "kcb" to "KCB Bank",
            "equity" to "Equity Bank",
            "coopbank" to "Co-op Bank",
            "ncba" to "NCBA / Loop",
            "absa" to "Absa Bank",
            "stanchart" to "Standard Chartered",
            "dtb" to "DTB Bank",
            "family" to "Family Bank",
            "im" to "I&M Bank",
            "stanbic" to "Stanbic Bank",
            "pesalink" to "PesaLink",
            "airtel" to "Airtel Money",
            "tkash" to "T-Kash",
            "sbm" to "SBM Bank",
            "hfgroup" to "HF Group",
            "gulf" to "Gulf African Bank",
            "boa" to "Bank of Africa",
            "primebank" to "Prime Bank",
        )

        fun institutionLabel(id: String): String = INSTITUTION_DISPLAY[id] ?: id.uppercase()

        /** Best-effort sender-address → institution-id matching (uppercase substrings). */
        private val INSTITUTION_KEYWORDS: List<Pair<String, List<String>>> = listOf(
            "mpesa" to listOf("MPESA", "M-PESA"),
            "kcb" to listOf("KCB"),
            "equity" to listOf("EQUITY"),
            "coopbank" to listOf("COOPBANK", "CO-OP", "COOPERATIVE"),
            "ncba" to listOf("NCBA", "LOOP"),
            "absa" to listOf("ABSA"),
            "stanchart" to listOf("STANCHART", "STANDARD CHARTERED"),
            "dtb" to listOf("DTB"),
            "family" to listOf("FAMILY"),
            "im" to listOf("I&M", "IMBANK"),
            "stanbic" to listOf("STANBIC"),
            "pesalink" to listOf("PESALINK"),
            "airtel" to listOf("AIRTEL"),
            "tkash" to listOf("TKASH", "T-KASH"),
            "sbm" to listOf("SBM"),
            "hfgroup" to listOf("HFGROUP", "HF GROUP", "HFC"),
            "gulf" to listOf("GULF"),
            "boa" to listOf("BANK OF AFRICA", "BOA"),
            "primebank" to listOf("PRIMEBANK", "PRIME BANK"),
        )

        private fun institutionFor(address: String): String? {
            val upper = address.uppercase()
            return INSTITUTION_KEYWORDS.firstOrNull { (_, keywords) -> keywords.any { it in upper } }?.first
        }
    }

    @Immutable
    data class SmsImportUiState(
        val permissionGranted: Boolean = false,
        val isImporting:       Boolean = false,
        val banner:            String?  = null,
        val previewCount:      Int?     = null,  // SMS found in window (null = not yet scanned)
        val isPreviewing:      Boolean  = false,
        /** Live chunk-by-chunk progress while the worker is running. */
        val importProgress:    ImportProgress? = null,
        /** Final counts once the worker succeeds — shown until next import starts. */
        val importResult:      ImportResult?   = null,
        /** Bank-detection phase (banks_only/all modes only — mpesa_only skips straight to import). */
        val isDetecting:          Boolean = false,
        val detectedInstitutions: List<DetectedInstitution>? = null,
        val showDetectionResult:  Boolean = false,
    )

    private data class PendingImport(val periodDays: Long?, val filter: String)
    private var pendingImport: PendingImport? = null

    private val _uiState = MutableStateFlow(SmsImportUiState())
    val uiState: StateFlow<SmsImportUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        // Observe the historical-import worker: live progress + final result counts.
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("lifeos_historical_import")
                .distinctUntilChanged()
                .collect { infos ->
                    val running = infos.any {
                        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                    }
                    val runningInfo = infos.firstOrNull {
                        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                    }
                    val doneInfo = infos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED }

                    // Live progress from setProgress() calls in the worker
                    val progress = if (running && runningInfo != null) {
                        val tot = runningInfo.progress.getInt("total", 0)
                        if (tot > 0) {
                            ImportProgress(
                            imported    = runningInfo.progress.getInt("imported", 0),
                            total       = tot,
                            quarantined = runningInfo.progress.getInt("quarantined", 0),
                        )
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    // Final counts from Result.success(outputData) in the worker
                    val result = if (!running && doneInfo != null) {
                        val data = doneInfo.outputData
                        ImportResult(
                            imported    = data.getInt("imported", 0),
                            total       = data.getInt("total", 0),
                            duplicates  = data.getInt("duplicates", 0),
                            quarantined = data.getInt("quarantined", 0),
                            failed      = data.getInt("failed", 0),
                        )
                    } else {
                        null
                    }

                    _uiState.update { cur ->
                        cur.copy(
                            isImporting    = running,
                            importProgress = progress,
                            // Keep the last result visible until a new import clears it
                            importResult   = when {
                                running -> null            // hide result while re-importing
                                result != null -> result   // fresh result just finished
                                else -> cur.importResult   // keep existing result
                            },
                            // RN's exact result-banner copy (FinanceScreen.tsx).
                            banner = if (result != null) {
                                when {
                                    result.total == 0 -> "No messages found in this window"
                                    result.imported == 0 && result.duplicates > 0 && result.failed == 0 ->
                                        "Everything up to date · ${result.duplicates} already imported"
                                    else ->
                                        "Import complete · ${result.imported} new · ${result.duplicates} dupes · ${result.failed} failed"
                                }
                            } else {
                                cur.banner
                            },
                        )
                    }
                }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        val wasGranted = _uiState.value.permissionGranted
        _uiState.value = _uiState.value.copy(
            permissionGranted = granted,
            // BUG-F7: only show the "granted" banner on the denied→granted transition.
            // If the screen opens while permission is already held, wasGranted==true so
            // we preserve whatever banner is already showing (or null).
            banner = when {
                granted && !wasGranted -> "SMS access granted · ready to import"
                !granted -> "SMS permission denied · enable it in device Settings"
                else -> _uiState.value.banner  // already granted — no state change
            },
        )
    }

    fun setBanner(text: String) {
        _uiState.value = _uiState.value.copy(banner = text)
    }

    fun clearBanner() {
        _uiState.value = _uiState.value.copy(banner = null)
    }

    /**
     * Count SMS in the selected window without importing — gives the user a
     * "Found X messages" preview before they tap Start Import.
     */
    fun previewImport(periodDays: Long?, filter: String) {
        if (!_uiState.value.permissionGranted) return
        _uiState.value = _uiState.value.copy(isPreviewing = true, previewCount = null)
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    val now    = System.currentTimeMillis()
                    val fromMs = periodDays?.let { now - TimeUnit.DAYS.toMillis(it) } ?: 0L
                    val uri    = android.net.Uri.parse("content://sms")
                    val selection = if (fromMs > 0L) "date >= ?" else null
                    val selArgs   = if (fromMs > 0L) arrayOf(fromMs.toString()) else null
                    val c: Cursor? = context.contentResolver.query(
                        uri, arrayOf("_id", "address"), selection, selArgs, null
                    )
                    if (c == null) return@runCatching 0
                    val result = if (filter == "all") {
                        c.count
                    } else {
                        val addrIdx = c.getColumnIndexOrThrow("address")
                        var kept = 0
                        while (c.moveToNext()) {
                            val addr = (c.getString(addrIdx) ?: "").uppercase()
                            val ok = when (filter) {
                                "mpesa_only" -> "MPESA" in addr || "M-PESA" in addr
                                "banks_only" -> listOf("KCB", "EQUITY", "COOPERATIVE", "ABSA",
                                    "STANDARD", "DTB", "NCBA", "BANK").any { it in addr }
                                else         -> true
                            }
                            if (ok) kept++
                        }
                        kept
                    }
                    c.close()
                    result
                }.getOrElse { null }
            }
            _uiState.value = _uiState.value.copy(isPreviewing = false, previewCount = count)
        }
    }

    /** Entry point from the sheet: mpesa_only imports immediately; banks_only/all detect first. */
    fun startImportFlow(periodDays: Long?, filter: String) {
        if (filter == "mpesa_only") {
            runImport(periodDays, filter)
        } else {
            detectInstitutions(periodDays, filter)
        }
    }

    fun detectInstitutions(periodDays: Long?, filter: String) {
        if (!_uiState.value.permissionGranted) return
        pendingImport = PendingImport(periodDays, filter)
        _uiState.value = _uiState.value.copy(isDetecting = true, showDetectionResult = false, detectedInstitutions = null)
        viewModelScope.launch {
            val detected = withContext(Dispatchers.IO) {
                runCatching {
                    val now    = System.currentTimeMillis()
                    val fromMs = periodDays?.let { now - TimeUnit.DAYS.toMillis(it) } ?: 0L
                    val uri    = android.net.Uri.parse("content://sms")
                    val selection = if (fromMs > 0L) "date >= ?" else null
                    val selArgs   = if (fromMs > 0L) arrayOf(fromMs.toString()) else null
                    val c: Cursor? = context.contentResolver.query(
                        uri, arrayOf("_id", "address"), selection, selArgs, null
                    )
                    val counts = mutableMapOf<String, Int>()
                    if (c != null) {
                        val addrIdx = c.getColumnIndexOrThrow("address")
                        while (c.moveToNext()) {
                            val addr = c.getString(addrIdx) ?: continue
                            val inst = institutionFor(addr) ?: continue
                            if (filter == "banks_only" && inst == "mpesa") continue
                            counts[inst] = (counts[inst] ?: 0) + 1
                        }
                        c.close()
                    }
                    counts.map { (id, n) -> DetectedInstitution(id, n) }
                        .sortedWith(compareBy({ it.institutionId != "mpesa" }, { -it.count }))
                }.getOrElse { emptyList() }
            }
            _uiState.value = _uiState.value.copy(
                isDetecting          = false,
                detectedInstitutions = detected,
                showDetectionResult  = true,
            )
        }
    }

    fun confirmBankImport() {
        val pending = pendingImport ?: return
        _uiState.value = _uiState.value.copy(showDetectionResult = false, detectedInstitutions = null)
        pendingImport = null
        runImport(pending.periodDays, pending.filter)
    }

    fun cancelDetection() {
        pendingImport = null
        _uiState.value = _uiState.value.copy(showDetectionResult = false, detectedInstitutions = null, isDetecting = false)
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
