package com.belinze.lifeos.services

import android.content.Context
import com.belinze.lifeos.data.db.dao.SmsDao
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// DarajaEnrichmentService — 1:1 port of src/services/darajaEnrichment.ts.
//
// Resolves human-readable business names for M-Pesa paybill and Buy-Goods
// (till) numbers. Lookup chain:
//   1. In-memory cache (process lifetime).
//   2. paybill_registry table (persisted across sessions).
//   3. Safaricom Daraja B2B Name Lookup API (OAuth2 — skipped when no creds).
//   4. merchant_categories keyed by the numeric code.
//
// Daraja credentials are never hard-coded; pass them per call (absent → the
// network layer is skipped silently).
// ─────────────────────────────────────────────────────────────────────────────

data class EnrichmentResult(
    val code: String,
    val displayName: String,
    val source: String, // cache | db_registry | daraja | merchant_categories | unknown
)

@Singleton
class DarajaEnrichmentService
    @Inject
    constructor(
    private val smsDao: SmsDao,
    @ApplicationContext private val context: Context,
) {
    private val cache = ConcurrentHashMap<String, String>()

    private companion object {
        const val DARAJA_BASE = "https://api.safaricom.co.ke"
        const val TIMEOUT_MS = 6_000
    }

    /** Resolve a human-readable name for a paybill/till number. */
    suspend fun resolvePaybillName(
        code: String,
        darajaConsumerKey: String? = null,
        darajaConsumerSecret: String? = null,
        lastAmountKes: Double? = null,
    ): EnrichmentResult {
        val normalised = code.trim()
        if (normalised.isEmpty()) return EnrichmentResult(code, code, "unknown")

        // 1 — in-memory cache
        cache[normalised]?.let { return EnrichmentResult(normalised, it, "cache") }

        // 2 — paybill_registry
        smsDao.getPaybillName(normalised)?.let {
            cache[normalised] = it
            return EnrichmentResult(normalised, it, "db_registry")
        }

        // 3 — Daraja API (only when credentials are provided)
        if (!darajaConsumerKey.isNullOrBlank() && !darajaConsumerSecret.isNullOrBlank()) {
            val token = fetchDarajaToken(darajaConsumerKey, darajaConsumerSecret)
            if (token != null) {
                val name = fetchDarajaName(normalised, token)
                if (name != null) {
                    cache[normalised] = name
                    smsDao.upsertPaybillUsage(normalised, name, nowIso(), lastAmountKes)
                    return EnrichmentResult(normalised, name, "daraja")
                }
            }
        }

        // 4 — merchant_categories keyed by the numeric code
        smsDao.getMerchantCategory(normalised)?.let {
            cache[normalised] = it.merchant
            return EnrichmentResult(normalised, it.merchant, "merchant_categories")
        }

        return EnrichmentResult(normalised, normalised, "unknown")
    }

    /** Pre-warm the in-memory cache from the local registry (call at app start). */
    suspend fun warmCache() {
        smsDao.getTopPaybills(500).forEach { row ->
            row.displayName?.let { cache[row.paybillNumber] = it }
        }
    }

    /** Number of entries in the process-lifetime cache (debug/health). */
    fun enrichmentCacheSize(): Int = cache.size

    // ── Daraja HTTP (HttpURLConnection, no extra dependency) ─────────────────

    private fun fetchDarajaToken(consumerKey: String, consumerSecret: String): String? {
        val creds = android.util.Base64.encodeToString(
            "$consumerKey:$consumerSecret".toByteArray(),
            android.util.Base64.NO_WRAP,
        )
        val body = httpGet(
            "$DARAJA_BASE/oauth/v1/generate?grant_type=client_credentials",
            mapOf("Authorization" to "Basic $creds"),
        ) ?: return null
        return runCatching {
            org.json.JSONObject(body).optString("access_token").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun fetchDarajaName(code: String, token: String): String? {
        val url = "$DARAJA_BASE/v1/business/search?BusinessShortCode=" + URLEncoder.encode(code, "UTF-8")
        val body = httpGet(
            url,
            mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
        ) ?: return null
        return runCatching {
            val params = org.json.JSONObject(body)
                .optJSONObject("Result")
                ?.optJSONObject("ResultParameters")
                ?.optJSONArray("ResultParameter")
                ?: return@runCatching null
            (0 until params.length())
                .map { params.getJSONObject(it) }
                .firstOrNull {
                    it.optString("Key") == "BusinessName" || it.optString("Key") == "ReceiverPartyPublicName"
                }
                ?.optString("Value")?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun httpGet(url: String, headers: Map<String, String>): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
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
