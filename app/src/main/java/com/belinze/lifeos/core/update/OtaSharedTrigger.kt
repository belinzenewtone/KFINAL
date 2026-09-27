package com.belinze.lifeos.core.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-singleton that lets [SettingsViewModel] hand an [OtaUpdateManifest]
 * to [OtaUpdatePromptHost] for display, without prop-drilling through the nav
 * graph.
 *
 * Flow:
 *  1. "Check for Updates" button → [SettingsViewModel.checkForOtaUpdate]
 *  2. SettingsViewModel calls [emitManifest] when an update is found.
 *  3. [OtaUpdatePromptHost] observes [pendingManifest] and shows the dialog
 *     (respects skippedVersionCode so it won't re-show a dismissed version).
 *  4. When the user dismisses/skips, the host calls [clearManifest].
 *
 *  "Download" button flow (bypasses skip logic):
 *  1. "Download" button → [SettingsViewModel.triggerDownload]
 *  2. SettingsViewModel calls [emitForceManifest].
 *  3. [OtaUpdatePromptHost] observes [forceManifest] and shows the dialog
 *     unconditionally — user explicitly asked to download.
 *  4. Host calls [clearForceManifest] after consuming.
 *
 * [isChecking] is written by [SettingsViewModel] so the Check button can
 * show a spinner and block duplicate taps while the network request is in flight.
 */
object OtaSharedTrigger {

    private val _pendingManifest = MutableStateFlow<OtaUpdateManifest?>(null)
    /** Non-null while an update manifest is waiting to be shown in the dialog. */
    val pendingManifest: StateFlow<OtaUpdateManifest?> = _pendingManifest

    private val _forceManifest = MutableStateFlow<OtaUpdateManifest?>(null)
    /**
     * Like [pendingManifest] but bypasses [OtaPromptUiState.skippedVersionCode].
     * Set by [SettingsViewModel.triggerDownload] when the user taps "Download"
     * after a check already confirmed an available update.
     */
    val forceManifest: StateFlow<OtaUpdateManifest?> = _forceManifest

    private val _isChecking = MutableStateFlow(false)
    /** True while [SettingsViewModel] is performing a manual check. */
    val isChecking: StateFlow<Boolean> = _isChecking

    /** Called by [SettingsViewModel] when a manual check finds an update. */
    fun emitManifest(manifest: OtaUpdateManifest) {
        _pendingManifest.value = manifest
    }

    /** Called by [OtaUpdatePromptHost] after the dialog is dismissed or skipped. */
    fun clearManifest() {
        _pendingManifest.value = null
    }

    /**
     * Called by [SettingsViewModel.triggerDownload] — forces the dialog open
     * even if the user previously dismissed this version.
     */
    fun emitForceManifest(manifest: OtaUpdateManifest) {
        _forceManifest.value = manifest
    }

    /** Called by [OtaUpdatePromptHost] after consuming [forceManifest]. */
    fun clearForceManifest() {
        _forceManifest.value = null
    }

    /** Called by [SettingsViewModel] to broadcast check-in-progress state. */
    fun setChecking(checking: Boolean) {
        _isChecking.value = checking
    }
}
