package com.belinze.lifeos.core.update

// ─────────────────────────────────────────────────────────────────────────────
// OTA data models — mirrors the reference Kotlin implementation exactly.
// ─────────────────────────────────────────────────────────────────────────────

data class OtaUpdateManifest(
    val versionCode: Long,
    val versionName: String?,
    val apkUrl:      String,
    val apkSha256:   String?,
    val changelog:   String?,
    val mandatory:   Boolean = false,
    val title:       String?,
    val message:     String?,
    val websiteUrl:  String?,
)

sealed interface OtaCheckResult {
    data class UpdateAvailable(val manifest: OtaUpdateManifest) : OtaCheckResult

    data object UpToDate        : OtaCheckResult

    data object NotConfigured   : OtaCheckResult

    data class Error(val message: String) : OtaCheckResult
}

sealed interface OtaDownloadResult {
    data class Success(val apkUri: String) : OtaDownloadResult

    data object Cancelled                  : OtaDownloadResult

    data class Error(val message: String)  : OtaDownloadResult
}

sealed interface OtaInstallResult {
    data object Started                        : OtaInstallResult

    data object RequiresUnknownSourcesPermission : OtaInstallResult

    data class Error(val message: String)      : OtaInstallResult
}

data class OtaDownloadProgress(
    val progressPercent:       Int?,
    val downloadedBytes:       Long,
    val totalBytes:            Long?,
    val bytesPerSecond:        Long?,
)
