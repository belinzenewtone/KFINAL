# publish_ota_release.ps1
# ─────────────────────────────────────────────────────────────────────────────
# Builds a signed release APK, publishes it as a GitHub Release, and updates
# ota/manifest.json so the in-app OTA prompt picks up the new version.
#
# Usage:
#   .\scripts\publish_ota_release.ps1 -VersionName "1.1.0" -VersionCode 2
#   .\scripts\publish_ota_release.ps1 -VersionName "1.2.0" -VersionCode 3 -Mandatory $true
#
# Prerequisites:
#   - gh CLI authenticated: gh auth login
#   - keystore.properties present (contains storeFile, storePassword, keyAlias, keyPassword)
#   - git remote origin = https://github.com/belinzenewtone/KFINAL.git
# ─────────────────────────────────────────────────────────────────────────────
param(
    [Parameter(Mandatory)][string] $VersionName,
    [Parameter(Mandatory)][long]   $VersionCode,
    [string] $Changelog  = "",
    [string] $Title      = "",
    [string] $Message    = "",
    [string] $WebsiteUrl = "https://github.com/belinzenewtone/KFINAL/releases",
    [bool]   $Mandatory  = $false
)

$ErrorActionPreference = "Stop"
$RepoRoot  = Split-Path -Parent $PSScriptRoot
$TagName   = "v$VersionName"
$ApkName   = "lifeos-release.apk"

# ── 1. Bump versionCode + versionName in build.gradle.kts ────────────────────
Write-Host "Bumping version to $VersionName ($VersionCode)…" -ForegroundColor Cyan
$buildGradle = Join-Path $RepoRoot "app\build.gradle.kts"
$content = Get-Content $buildGradle -Raw
$content = $content -replace 'versionCode\s*=\s*\d+',  "versionCode = $VersionCode"
$content = $content -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$VersionName`""
Set-Content $buildGradle $content -Encoding UTF8

# ── 2. Build signed release APK ───────────────────────────────────────────────
Write-Host "Building release APK…" -ForegroundColor Cyan
Push-Location $RepoRoot
& ".\gradlew.bat" assembleRelease --quiet
Pop-Location

$apkSrc = Join-Path $RepoRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkSrc)) {
    Write-Error "Build failed — APK not found at $apkSrc"
    exit 1
}

# ── 3. Compute SHA-256 ───────────────────────────────────────────────────────
Write-Host "Computing SHA-256…" -ForegroundColor Cyan
$sha256 = (Get-FileHash $apkSrc -Algorithm SHA256).Hash.ToLower()
Write-Host "  $sha256"

# ── 4. Create GitHub Release and upload APK ──────────────────────────────────
Write-Host "Creating GitHub Release $TagName…" -ForegroundColor Cyan
$releaseNotes = if ($Changelog) { $Changelog } else { "LifeOS $VersionName" }

gh release create $TagName $apkSrc `
    --title "LifeOS $VersionName" `
    --notes $releaseNotes `
    --repo "belinzenewtone/KFINAL"

$apkUrl = "https://github.com/belinzenewtone/KFINAL/releases/download/$TagName/$ApkName"
Write-Host "  APK URL: $apkUrl"

# ── 5. Update ota/manifest.json ───────────────────────────────────────────────
Write-Host "Updating ota/manifest.json…" -ForegroundColor Cyan
$otaPath     = Join-Path $RepoRoot "ota\manifest.json"
$titleFinal  = if ($Title)   { $Title }   else { "LifeOS $VersionName" }
$messageFinal = if ($Message) { $Message } else { "A new version of LifeOS is available. Update now for the latest improvements." }
$logFinal    = if ($Changelog) { $Changelog } else { "• Bug fixes and improvements" }

$manifest = [ordered]@{
    version_code  = $VersionCode
    version_name  = $VersionName
    apk_url       = $apkUrl
    apk_sha256    = $sha256
    mandatory     = $Mandatory
    title         = $titleFinal
    message       = $messageFinal
    changelog     = $logFinal
    website_url   = $WebsiteUrl
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content $otaPath -Encoding UTF8
Write-Host "  manifest.json written."

# ── 6. Commit + push manifest + gradle bump ───────────────────────────────────
Write-Host "Committing manifest update…" -ForegroundColor Cyan
Push-Location $RepoRoot
git add "ota/manifest.json" "app/build.gradle.kts"
git commit -m "chore: OTA release $VersionName (versionCode $VersionCode)"
git push origin master
Pop-Location

Write-Host ""
Write-Host "Done! OTA release $TagName published." -ForegroundColor Green
Write-Host "  Manifest: https://raw.githubusercontent.com/belinzenewtone/KFINAL/master/ota/manifest.json"
Write-Host "  APK:      $apkUrl"
