package com.belinze.lifeos.core.update

import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// ─────────────────────────────────────────────────────────────────────────────
// OtaUpdateManager
//
// Ported from the reference Kotlin project (shared/androidMain).
// Adaptations:
//  • Uses HttpURLConnection instead of Ktor (no new dependency)
//  • Uses org.json instead of kotlinx-serialization (no new dependency)
//  • Telemetry calls removed (no analytics dependency)
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("TooManyFunctions")
object OtaUpdateManager {

    // ── Manifest check ────────────────────────────────────────────────────────

    suspend fun checkForUpdate(
        context:     Context,
        manifestUrl: String,
    ): OtaCheckResult = withContext(Dispatchers.IO) {
        if (manifestUrl.isBlank()) return@withContext OtaCheckResult.NotConfigured

        runCatching {
            val body = fetchManifestBody(manifestUrl)
            if (body.isBlank()) return@runCatching OtaCheckResult.Error("Empty OTA manifest response.")

            val manifest = parseManifest(body)
                ?: return@runCatching OtaCheckResult.Error("Invalid OTA manifest JSON.")

            evaluateManifest(manifest, getCurrentVersionCode(context))
        }.getOrElse { OtaCheckResult.Error(it.message ?: "Failed to check for updates.") }
    }

    private fun fetchManifestBody(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        conn.requestMethod  = "GET"
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    suspend fun downloadUpdate(
        context:           Context,
        manifest:          OtaUpdateManifest,
        onProgress:        (Int?) -> Unit         = {},
        onProgressDetails: (OtaDownloadProgress) -> Unit = {},
        onEnqueued:        (Long) -> Unit         = {},
    ): OtaDownloadResult = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return@withContext OtaDownloadResult.Error("Download manager unavailable.")

        val request = DownloadManager.Request(manifest.apkUrl.toUri())
            .setMimeType("application/vnd.android.package-archive")
            .setTitle("Updating to v${manifest.versionName ?: manifest.versionCode}")
            .setDescription("Tap to return to the app")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "lifeos_update_v${manifest.versionCode}.apk",
            )

        val downloadId = runCatching { manager.enqueue(request) }
            .getOrElse { return@withContext OtaDownloadResult.Error(it.message ?: "Failed to start download.") }
        onEnqueued(downloadId)

        return@withContext try {
            waitForDownloadCompletion(
                context        = context,
                manager        = manager,
                downloadId     = downloadId,
                expectedSha256 = manifest.apkSha256,
                onProgress     = onProgress,
                onProgressDetails = onProgressDetails,
            )
        } catch (_: CancellationException) {
            manager.remove(downloadId)
            OtaDownloadResult.Cancelled
        }
    }

    // ── Install ───────────────────────────────────────────────────────────────

    fun launchInstaller(activity: Activity, apkUri: Uri): OtaInstallResult {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${activity.packageName}".toUri()
            }
            activity.startActivity(intent)
            return OtaInstallResult.RequiresUnknownSourcesPermission
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            activity.startActivity(installIntent)
            OtaInstallResult.Started
        } catch (e: ActivityNotFoundException) {
            OtaInstallResult.Error(e.message ?: "No installer available on device.")
        } catch (e: SecurityException) {
            OtaInstallResult.Error(e.message ?: "Installer launch denied.")
        }
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    fun cancelDownload(context: Context, downloadId: Long): Boolean {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return false
        return runCatching { manager.remove(downloadId) > 0L }.getOrDefault(false)
    }

    // ── Open website ──────────────────────────────────────────────────────────

    fun openWebsite(context: Context, url: String): Boolean {
        val target = url.trim()
        if (target.isBlank()) return false
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, target.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        } catch (_: ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getCurrentVersionCode(context: Context): Long =
        PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0),
        )

    private suspend fun waitForDownloadCompletion(
        context:        Context,
        manager:        DownloadManager,
        downloadId:     Long,
        expectedSha256: String?,
        onProgress:     (Int?) -> Unit,
        onProgressDetails: (OtaDownloadProgress) -> Unit,
    ): OtaDownloadResult {
        val query = DownloadManager.Query().setFilterById(downloadId)
        var lastSampleMs = System.currentTimeMillis()
        var lastBytes    = 0L

        while (true) {
            val snapshot = readSnapshot(manager, query)
                ?: return OtaDownloadResult.Error("Download entry not found.")

            onProgress(snapshot.progress)
            onProgressDetails(snapshot.toProgressSample(lastSampleMs, lastBytes))
            lastSampleMs = System.currentTimeMillis()
            lastBytes    = snapshot.downloadedBytes.coerceAtLeast(0L)

            when (snapshot.status) {
                DownloadManager.STATUS_SUCCESSFUL ->
                    return verifyAndBuildSuccess(context, manager, downloadId, expectedSha256)
                DownloadManager.STATUS_FAILED ->
                    return OtaDownloadResult.Error("Download failed (reason ${snapshot.reason ?: -1}).")
                // PAUSED / PENDING / RUNNING → keep polling
            }
            delay(750L)
        }
    }

    private fun readSnapshot(manager: DownloadManager, query: DownloadManager.Query): DownloadSnapshot? =
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val status   = cursor.intCol(DownloadManager.COLUMN_STATUS)
            val downloaded = cursor.longCol(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val total    = cursor.longCol(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val progress = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else null
            DownloadSnapshot(
                status        = status,
                progress      = progress,
                downloadedBytes = downloaded,
                totalBytes    = total.takeIf { it > 0L },
                reason        = cursor.intColOrNull(DownloadManager.COLUMN_REASON),
            )
        }

    private fun verifyAndBuildSuccess(
        context:        Context,
        manager:        DownloadManager,
        downloadId:     Long,
        expectedSha256: String?,
    ): OtaDownloadResult {
        val uri = manager.getUriForDownloadedFile(downloadId)
            ?: return OtaDownloadResult.Error("Downloaded APK URI unavailable.")
        if (expectedSha256.isNullOrBlank()) return OtaDownloadResult.Success(uri.toString())
        val actual = computeSha256(context, uri)
            ?: return OtaDownloadResult.Error("Failed to verify update file.")
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            manager.remove(downloadId)
            return OtaDownloadResult.Error("APK integrity check failed.")
        }
        return OtaDownloadResult.Success(uri.toString())
    }

    private fun computeSha256(context: Context, uri: Uri): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        } ?: return null
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
}

// ── JSON parsing ──────────────────────────────────────────────────────────────

/** Alternate key names accepted in the manifest (matches reference implementation). */
private val alternateKeys = mapOf(
    "download_url"    to "apk_url",
    "checksum_sha256" to "apk_sha256",
    "required"        to "mandatory",
    "release_notes"   to "changelog",
)

internal fun parseManifest(body: String): OtaUpdateManifest? = runCatching {
    val raw = JSONObject(body)
    // Canonicalize alternate key names
    val obj = JSONObject(raw.toString()).also { canon ->
        alternateKeys.forEach { (alt, canonical) ->
            if (!raw.has(canonical) && raw.has(alt)) {
                canon.put(canonical, raw.get(alt))
            }
        }
    }
    OtaUpdateManifest(
        versionCode = obj.getLong("version_code"),
        versionName = obj.optString("version_name").takeIf { it.isNotBlank() },
        apkUrl      = obj.getString("apk_url"),
        apkSha256   = obj.optString("apk_sha256").takeIf { it.isNotBlank() },
        changelog   = obj.optString("changelog").takeIf { it.isNotBlank() },
        mandatory   = obj.optBoolean("mandatory", false),
        title       = obj.optString("title").takeIf { it.isNotBlank() },
        message     = obj.optString("message").takeIf { it.isNotBlank() },
        websiteUrl  = obj.optString("website_url").takeIf { it.isNotBlank() },
    )
}.getOrNull()

internal fun evaluateManifest(manifest: OtaUpdateManifest, currentVersionCode: Long): OtaCheckResult {
    if (manifest.apkUrl.isBlank() || manifest.versionCode <= 0L) {
        return OtaCheckResult.Error("Invalid OTA manifest fields.")
    }
    return if (manifest.versionCode > currentVersionCode) {
        OtaCheckResult.UpdateAvailable(manifest)
    } else {
        OtaCheckResult.UpToDate
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

private data class DownloadSnapshot(
    val status:         Int,
    val progress:       Int?,
    val downloadedBytes: Long,
    val totalBytes:     Long?,
    val reason:         Int?,
)

private fun DownloadSnapshot.toProgressSample(
    prevSampleMs: Long,
    prevBytes:    Long,
): OtaDownloadProgress {
    val elapsed       = System.currentTimeMillis() - prevSampleMs
    val safeBytes     = downloadedBytes.coerceAtLeast(0L)
    val speedBps      = if (elapsed > 0L) {
        ((safeBytes - prevBytes).coerceAtLeast(0L) * 1000L / elapsed).coerceAtLeast(0L)
    } else null
    return OtaDownloadProgress(
        progressPercent = progress,
        downloadedBytes = safeBytes,
        totalBytes      = totalBytes,
        bytesPerSecond  = speedBps,
    )
}

private fun Cursor.intCol(name: String): Int = getInt(getColumnIndexOrThrow(name))
private fun Cursor.intColOrNull(name: String): Int? {
    val idx = getColumnIndex(name)
    return if (idx >= 0) getInt(idx) else null
}
private fun Cursor.longCol(name: String): Long = getLong(getColumnIndexOrThrow(name))
