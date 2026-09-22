package com.belinze.lifeos.services

import android.content.Context
import com.belinze.lifeos.data.datastore.AppPreferences
import com.lifeos.sms.BundleCompiler
import com.lifeos.sms.SmsParserConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// RuleBundleSync — 1:1 port of src/services/ruleSync.ts.
//
// At startup, loads the most recent parser-rule bundle (cached OTA > bundled)
// into SmsParserConfig, then fetches a versioned bundle from the CDN and
// hot-swaps it when strictly newer. Gives instant coverage for new Safaricom
// SMS formats without a Play Store release.
//
// The remote bundle is only applied when newer than what is already loaded,
// and is persisted to DataStore so subsequent cold starts reuse it.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class RuleBundleSync
    @Inject
    constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences,
) {
    private companion object {
        const val CDN_URL = "https://cdn.lifeos.app/parser-rules/v1/rules.json"
        const val TIMEOUT_MS = 5_000
        const val BUNDLED_VERSION = 1
    }

    @Volatile
    private var loadedVersion: Int = BUNDLED_VERSION

    /** Load cached (or bundled) rules, then attempt a CDN sync. Called at startup. */
    suspend fun initialize() {
        loadCached()
        syncFromCdn()
    }

    private suspend fun loadCached() {
        val (json, version) = prefs.parserRulesCached()
        if (json.isNullOrBlank() || version <= 0) return
        val bundle = BundleCompiler.fromJson(json) ?: return
        if (bundle.version < BUNDLED_VERSION) return
        val rules = BundleCompiler.compile(bundle)
        if (rules.isEmpty()) return
        SmsParserConfig.loadBundle(rules)
        loadedVersion = bundle.version
    }

    /** Fetch the latest bundle from the CDN and hot-swap when strictly newer. */
    suspend fun syncFromCdn() {
        val json = httpGet(CDN_URL) ?: return
        val bundle = BundleCompiler.fromJson(json) ?: return
        if (bundle.version <= loadedVersion) return
        val rules = BundleCompiler.compile(bundle)
        if (rules.isEmpty()) return
        SmsParserConfig.loadBundle(rules)
        loadedVersion = bundle.version
        prefs.setParserRules(json, bundle.version)
    }

    private fun httpGet(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                if (conn.responseCode !in 200..299) return null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}
