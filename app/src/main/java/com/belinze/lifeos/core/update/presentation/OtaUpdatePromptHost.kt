package com.belinze.lifeos.core.update.presentation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.belinze.lifeos.core.update.OtaUpdateManager
import com.belinze.lifeos.core.update.OtaUpdateManifest
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// OtaUpdatePromptHost
//
// Exact port of the reference Android implementation.
// Adaptations:
//  • Uses BuildConfig.OTA_MANIFEST_URL instead of SharedBuildConfig
//  • Resolves appName / versionName from LocalContext.current (no injected helper)
//  • Handles install via OtaUpdateManager.launchInstaller() (no platform wrapper)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Drop-in composable that manages the full OTA flow:
 *  1. On first composition, silently checks for a new version.
 *  2. If [OtaCheckResult.UpdateAvailable], shows [OtaUpdateDialog].
 *  3. Download progress, cancellation, install handoff are fully handled here.
 *
 * @param shouldCheckForUpdates Set to false to suppress the check entirely
 *   (e.g. while the app is locked or during onboarding).
 * @param manualTrigger Pass an incrementing Int to force a new check regardless
 *   of [OtaPromptUiState.hasCheckedThisSession] — used by "Check for updates"
 *   in Settings.
 */
@Composable
fun OtaUpdatePromptHost(
    shouldCheckForUpdates: Boolean,
    manualTrigger:         Int = 0,
) {
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
    var activeManifest     by remember { mutableStateOf<OtaUpdateManifest?>(null) }
    var downloadedApkPath  by remember { mutableStateOf<String?>(null) }

    // ── Auto-check on first entry (or on manual trigger) ─────────────────────
    LaunchedEffect(manualTrigger) {
        val isManual = manualTrigger > 0
        if (!isManual && uiState.hasCheckedThisSession) return@LaunchedEffect

        uiState = uiState.copy(isChecking = true)
        val result = runCatching {
            OtaUpdateManager.checkForUpdate(context, BuildConfig.OTA_MANIFEST_URL)
        }.getOrElse { OtaCheckResult.Error(it.message ?: "Update check failed.") }

        uiState = uiState.copy(isChecking = false, hasCheckedThisSession = true)

        if (result is OtaCheckResult.UpdateAvailable &&
            result.manifest.versionCode > uiState.skippedVersionCode
        ) {
            activeManifest = result.manifest
            uiState = uiState.copy(showDialog = true)
        }
    }

    // ── "Checking…" spinner dialog ────────────────────────────────────────────
    if (uiState.isChecking) {
        OtaCheckingDialog(appName = appName)
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
                    },
                    onLater = {
                        uiState        = uiState.dismissForVersion(manifest.versionCode)
                        activeManifest = null
                    },
                    onPrimaryAction = {
                        if (hasDownloadedApk) {
                            // APK already downloaded — launch installer
                            val path = downloadedApkPath ?: return@OtaDialogCallbacks
                            val apkUri = try {
                                android.net.Uri.parse(path)
                            } catch (_: Exception) { return@OtaDialogCallbacks }
                            val activity = context as? Activity ?: return@OtaDialogCallbacks
                            val result = OtaUpdateManager.launchInstaller(activity, apkUri)
                            if (result is OtaInstallResult.RequiresUnknownSourcesPermission) {
                                uiState = uiState.copy(
                                    statusMessage = "Please allow installs from unknown sources, then tap Install again.",
                                )
                            }
                        } else {
                            // Start download
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
                                            isDownloading = false,
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
