package com.belinze.lifeos.core.update.presentation

import androidx.compose.runtime.saveable.listSaver

// ─────────────────────────────────────────────────────────────────────────────
// OtaPromptUiState
//
// Exact port of the reference implementation.
// Persisted across process restores via rememberSaveable + listSaver so that:
//  • skippedVersionCode survives — user won't see the dialog for a skipped
//    version even after the app is killed and restarted.
//  • hasCheckedThisSession is reset to false on restore — forces a fresh
//    check in case a new manifest was uploaded while the app was killed.
// ─────────────────────────────────────────────────────────────────────────────

internal data class OtaPromptUiState(
    val hasCheckedThisSession:    Boolean = false,
    val skippedVersionCode:       Long    = -1L,
    val isChecking:               Boolean = false,
    val isDownloading:            Boolean = false,
    val downloadPercent:          Int?    = null,
    val downloadedBytes:          Long    = 0L,
    val totalBytes:               Long?   = null,
    val downloadSpeedBytesPerSec: Long?   = null,
    val statusMessage:            String? = null,
    val activeDownloadId:         Long    = -1L,
    val showDialog:               Boolean = false,
    /** True after a download attempt ends in [OtaDownloadResult.Error]; cleared on retry. */
    val downloadFailed:           Boolean = false,
) {
    fun dismissForVersion(versionCode: Long): OtaPromptUiState =
        copy(skippedVersionCode = versionCode, showDialog = false)

    companion object {
        val Saver = listSaver<OtaPromptUiState, Any?>(
            save = {
                listOf(
                    it.hasCheckedThisSession,
                    it.skippedVersionCode,
                    it.isChecking,
                    it.isDownloading,
                    it.downloadPercent,
                    it.downloadedBytes,
                    it.totalBytes,
                    it.downloadSpeedBytesPerSec,
                    it.statusMessage,
                    it.activeDownloadId,
                    it.showDialog,
                    it.downloadFailed,
                )
            },
            restore = {
                OtaPromptUiState(
                    // Always re-check on fresh launch / process restore.
                    hasCheckedThisSession    = false,
                    skippedVersionCode       = it[1] as Long,
                    // Reset in-progress flags — process may have died mid-check.
                    isChecking               = false,
                    isDownloading            = it[3] as Boolean,
                    downloadPercent          = it[4] as Int?,
                    downloadedBytes          = it[5] as Long,
                    totalBytes               = it[6] as Long?,
                    downloadSpeedBytesPerSec = it[7] as Long?,
                    statusMessage            = it[8] as String?,
                    activeDownloadId         = it[9] as Long,
                    showDialog               = it[10] as Boolean,
                    downloadFailed           = it[11] as Boolean,
                )
            },
        )
    }
}
