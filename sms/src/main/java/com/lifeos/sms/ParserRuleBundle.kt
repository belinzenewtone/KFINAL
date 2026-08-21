package com.lifeos.sms

/**
 * JSON-serialisable representation of a parser rule set that can be fetched from
 * a CDN and hot-swapped into [SmsParserConfig] without an app release.
 *
 * Wire format (example):
 * {
 *   "version": 2,
 *   "publishedAt": "2026-07-26",
 *   "rules": [
 *     {
 *       "id": "sent_p2p",
 *       "category": "SENT",
 *       "description": "Peer-to-peer transfer",
 *       "patterns": ["(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to\\s+[A-Z].+?(?:\\s+on\\s|\\.)"],
 *       "fallbackPatterns": ["sent to\\s+[A-Z]"],
 *       "counterpartyPatterns": ["(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to\\s+(.+?)(?:\\s+on\\s|\\.)"]
 *     }
 *   ]
 * }
 *
 * The category string must match a [SmsParserConfig.SmsCategory] enum name.
 * Rules that fail to compile (bad regex or unknown category) are silently dropped —
 * the caller should validate BundleCompiler.compile returns the expected count.
 */
data class ParserRuleBundle(
    val version: Int,
    val publishedAt: String,
    val rules: List<RuleBundleEntry>,
)

data class RuleBundleEntry(
    val id: String,
    val category: String,
    val description: String,
    val patterns: List<String>,
    val fallbackPatterns: List<String>,
    val counterpartyPatterns: List<String>,
)
