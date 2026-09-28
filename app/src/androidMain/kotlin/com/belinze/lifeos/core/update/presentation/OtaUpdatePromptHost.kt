package com.belinze.lifeos.core.update.presentation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.belinze.lifeos.BuildConfig
import com.belinze.lifeos.core.update.OtaCheckResult
import com.belinze.lifeos.core.update.OtaDownloadResult
import com.belinze.lifeos.core.update.OtaInstallResult
import com.belinze.lifeos.core.update.OtaSharedTrigger
import com.belinze.lifeos.core.update.OtaUpdateManager
import com.belinze.lifeos.core.update.OtaUpdateManifest
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// OtaUpdatePromptHost
//
// Owns the full OTA dialog flow for the entire app:
//
//  Auto-check (LaunchedEffect(Unit)):
//    • Runs once per composition lifetime — fires on first mount, never again
//      for the same composition instance.
//    • hasCheckedThisSession (rememberSaveable) prevents a re-check when the
//      Compose tree is recreated by a config change, predictive-back gesture,
//      or OPlus/ColorOS activity restart.
//    • Silent — shows nothing unless an update is found.
//
//  Manual check (Settings → SettingsViewModel.checkForOtaUpdate):
//    • SettingsViewModel runs the network call independently; if an update is
//      found it calls OtaSharedTrigger.emitManifest().
//    • This composable observes pendingManifest and shows the dialog when it
//      becomes non-null — so the dialog always renders in the same host,
//      regardless of whether the check was automatic or manual.
//
// The "Checking…" spinner / "You're up to date" alert were removed — the
// spinner is now shown inline on the Settings row and the "up to date" result
// is shown in the Settings banner, not as a global dialog.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * @param shouldCheckForUpdates Set to false to suppress the auto-check
 *   (e.g. while the app is locked or during onboarding).
 */
@Composable
fun OtaUpdatePromptHost(shouldCheckForUpdates: Boolean) {
    if (!shouldCheckForUpdates) return

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val appName = remember(context) {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }
    val currentVersionName = remember(context) {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                ?: BuildConfig.VERSION_NAME
        }.getOrDefault(BuildConfig.VERSION_NAME)
    }

    var uiState by rememberSaveable(stateSaver = OtaPromptUiState.Saver) {
        mutableStateOf(OtaPromptUiState())
    }
    var activeManifest    by remember { mutableStateOf<OtaUpdateManifest?>(null) }
    var downloadedApkPath by remember { mutableStateOf<String?>(null) }

    // ── Auto-check: once per session ──────────────────────────────────────────
    // LaunchedEffect(Unit) fires exactly once per composition lifetime — it will
    // NOT re-fire on recompositions, config changes, or back-gesture previews.
    // hasCheckedThisSession (rememberSaveable) adds a second guard so that even
    // if the activity is fully recreated (OPlus kill-and-restart) we skip the
    // check if it already ran earlier this process instance.
    LaunchedEffect(Unit) {
        if (uiState.hasCheckedThisSession) return@LaunchedEffect
        val result = runCatching {
            OtaUpdateManager.checkForUpdate(context, BuildConfig.OTA_MANIFEST_URL)
        }.getOrElse { OtaCheckResult.Error(it.message ?: "Update check failed.") }
        uiState = uiState.copy(hasCheckedThisSession = true)
        if (result is OtaCheckResult.UpdateAvailable &&
            result.manifest.versionCode > uiState.skippedVersionCode
        ) {
            activeManifest = result.manifest
            uiState = uiState.copy(showDialog = true)
        }
    }

    // ── Manual check result from Settings (respects skip) ────────────────────
    // SettingsViewModel calls OtaSharedTrigger.emitManifest() when a manual
    // check finds an update. We consume it here and clear the trigger so it
    // cannot re-fire on recomposition.
    val pendingManifest by OtaSharedTrigger.pendingManifest.collectAsState()
    LaunchedEffect(pendingManifest) {
        val manifest = pendingManifest ?: return@LaunchedEffect
        if (manifest.versionCode > uiState.skippedVersionCode) {
            activeManifest = manifest
            uiState = uiState.copy(showDialog = true)
        }
        // Always clear so a recomposition doesn't re-show the dialog.
        OtaSharedTrigger.clearManifest()
    }

    // ── "Download" button from Settings (bypasses skip) ───────────────────────
    // SettingsViewModel calls OtaSharedTrigger.emitForceManifest() when the
    // user taps "Download" after a check confirmed an available update. We show
    // the dialog unconditionally — the user explicitly asked to download.
    val forceManifest by OtaSharedTrigger.forceManifest.collectAsState()
    LaunchedEffect(forceManifest) {
        val manifest = forceManifest ?: return@LaunchedEffect
        activeManifest = manifest
        uiState = uiState.copy(showDialog = true)
        OtaSharedTrigger.clearForceManifest()
    }

    // ── Main update dialog ────────────────────────────────────────────────────
    activeManifest?.let { manifest ->
        if (uiState.showDialog) {
            val hasDownloadedApk = downloadedApkPath != null && !uiState.isDownloading

            OtaUpdateDialog(
                appName            = appName,
                currentVersionName = currentVersionName,
                manifest           = manifest,
                state              = uiState,
                hasDownloadedApk   = hasDownloadedApk,
                callbacks = OtaDialogCallbacks(
                    onDismiss = {
                        uiState        = uiState.dismissForVersion(manifest.versionCode)
                        activeManifest = null
                        OtaSharedTrigger.clearManifest()
                    },
                    onLater = {
                        uiState        = uiState.dismissForVersion(manifest.versionCode)
                        activeManifest = null
                        OtaSharedTrigger.clearManifest()
                    },
                    onPrimaryAction = {
                        if (hasDownloadedApk) {
                            val path = downloadedApkPath ?: return@OtaDialogCallbacks
                            val apkUri = try {
                                android.net.Uri.parse(path)
                            } catch (_: Exception) {
                                return@OtaDialogCallbacks
                            }
                            val activity = context as? Activity ?: return@OtaDialogCallbacks
                            val result = OtaUpdateManager.launchInstaller(activity, apkUri)
                            if (result is OtaInstallResult.RequiresUnknownSourcesPermission) {
                                uiState = uiState.copy(
                                    statusMessage = "Please allow installs from unknown sources, then tap Install again.",
                                )
                            }
                        } else {
                            scope.launch {
                                uiState = uiState.copy(isDownloading = true, downloadFailed = false)
                                val dlResult = OtaUpdateManager.downloadUpdate(
                                    context  = context,
                                    manifest = manifest,
                                    onProgress = { percent ->
                                        uiState = uiState.copy(downloadPercent = percent)
                                    },
                                    onProgressDetails = { details ->
                                        uiState = uiState.copy(
                                            downloadedBytes          = details.downloadedBytes,
                                            totalBytes               = details.totalBytes,
                                            downloadSpeedBytesPerSec = details.bytesPerSecond,
                                        )
                                    },
                                    onEnqueued = { id ->
                                        uiState = uiState.copy(activeDownloadId = id)
                                    },
                                )
                                when (dlResult) {
                                    is OtaDownloadResult.Success -> {
                                        downloadedApkPath = dlResult.apkUri
                                        uiState = uiState.copy(
                                            isDownloading   = false,
                                            downloadPercent = 100,
                                        )
                                    }
                                    is OtaDownloadResult.Error -> {
                                        uiState = uiState.copy(
                                            isDownloading  = false,
                                            downloadFailed = true,
                                        )
                                    }
                                    OtaDownloadResult.Cancelled -> {
                                        uiState = uiState.copy(
                                            isDownloading    = false,
                                            activeDownloadId = -1L,
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onCancelDownload = {
                        scope.launch {
                            if (uiState.activeDownloadId >= 0) {
                                OtaUpdateManager.cancelDownload(context, uiState.activeDownloadId)
                            }
                            uiState = uiState.copy(isDownloading = false, activeDownloadId = -1L)
                        }
                    },
                    onWebsite = {
                        val url = manifest.websiteUrl?.takeIf { it.isNotBlank() } ?: manifest.apkUrl
                        OtaUpdateManager.openWebsite(context, url)
                    },
                ),
            )
        }
    }
}
