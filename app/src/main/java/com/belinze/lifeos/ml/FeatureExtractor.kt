package com.belinze.lifeos.ml

import com.belinze.lifeos.data.db.entity.TransactionEntity
import kotlin.math.ln

internal object FeatureExtractor {
    fun extract(tx: TransactionEntity): Map<String, Double> = buildMap {
        // Amount features
        put("amount",        tx.amount)
        put("amount_log",    if (tx.amount > 0) ln(tx.amount) else 0.0)
        put("amount_bucket", amountBucket(tx.amount))

        // Transaction type one-hot
        put("type_expense",  if (tx.transactionType == "expense")  1.0 else 0.0)
        put("type_receive",  if (tx.transactionType == "receive")  1.0 else 0.0)
        put("type_transfer", if (tx.transactionType == "transfer") 1.0 else 0.0)
        put("type_fuliza",   if (tx.transactionType == "fuliza")   1.0 else 0.0)

        // Merchant prefix — low-bit hash of first 5 chars keeps dimensionality fixed
        val prefix = tx.merchant?.lowercase()?.take(5) ?: ""
        put("merchant_h", (prefix.hashCode() and 0x7FFF).toDouble())

        // Time features from ISO date string (e.g. "2026-09-22T14:30:00")
        val parts        = tx.date?.take(19)?.split("T", "t") ?: emptyList()
        val dateSegments = parts.getOrNull(0)?.split("-") ?: emptyList()
        val timeSegments = parts.getOrNull(1)?.split(":") ?: emptyList()
        put("month", dateSegments.getOrNull(1)?.toDoubleOrNull() ?: 0.0)
        put("hour",  timeSegments.getOrNull(0)?.toDoubleOrNull() ?: 0.0)

        // Auxiliary signals
        put("has_fee",  if ((tx.fee ?: 0.0) > 0) 1.0 else 0.0)
        put("is_mpesa", if (tx.institutionId == "mpesa") 1.0 else 0.0)
    }

    fun toJson(features: Map<String, Double>): String =
        features.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }

    fun fromJson(json: String): Map<String, Double> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return runCatching {
            val inner = json.trim().removePrefix("{").removeSuffix("}")
            inner.split(",").mapNotNull { pair ->
                val colon = pair.indexOf(':')
                if (colon < 0) return@mapNotNull null
                val key   = pair.substring(0, colon).trim().trim('"')
                val value = pair.substring(colon + 1).trim().toDoubleOrNull()
                    ?: return@mapNotNull null
                key to value
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun amountBucket(amount: Double): Double = when {
        amount < 50      -> 0.0
        amount < 200     -> 1.0
        amount < 500     -> 2.0
        amount < 1_000   -> 3.0
        amount < 5_000   -> 4.0
        amount < 20_000  -> 5.0
        else             -> 6.0
    }
}
