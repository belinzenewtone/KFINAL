package com.lifeos.sms

import org.json.JSONArray
import org.json.JSONObject

/**
 * Compiles a [ParserRuleBundle] (received as raw JSON from a CDN) into compiled
 * [SmsParserConfig.DetectionRule] objects that [SmsParserConfig.activeRules]
 * can serve directly.
 *
 * Compilation validates each rule entry:
 *  - Unknown [SmsParserConfig.SmsCategory] values → rule silently dropped
 *  - Invalid regex patterns → pattern silently dropped (rule kept if ≥1 pattern survives)
 *  - Rule with zero valid patterns AND zero valid fallbacks → rule dropped
 *
 * All failures are non-fatal so a partially valid bundle still improves accuracy
 * for the rules it does contain.
 */
object BundleCompiler {
    /** Compiles every valid entry in [bundle] into DetectionRule objects. */
    fun compile(bundle: ParserRuleBundle): List<SmsParserConfig.DetectionRule> =
        bundle.rules.mapNotNull { compileEntry(it) }

    private fun compileEntry(entry: RuleBundleEntry): SmsParserConfig.DetectionRule? {
        val category = runCatching {
            SmsParserConfig.SmsCategory.valueOf(entry.category)
        }.getOrNull() ?: return null

        val patterns = entry.patterns.mapNotNull { compileRegex(it) }
        val fallbacks = entry.fallbackPatterns.mapNotNull { compileRegex(it) }
        if (patterns.isEmpty() && fallbacks.isEmpty()) return null

        return SmsParserConfig.DetectionRule(
            id                   = entry.id,
            category             = category,
            description          = entry.description,
            patterns             = patterns,
            fallbackPatterns     = fallbacks,
            counterpartyPatterns = entry.counterpartyPatterns.mapNotNull { compileRegex(it) },
        )
    }

    private fun compileRegex(pattern: String): Regex? =
        runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull()

    // ── JSON deserialisation ─────────────────────────────────────────────────

    /**
     * Parses raw JSON into a [ParserRuleBundle].
     * Returns null on any parse failure so the caller can fall back to the hardcoded rules.
     */
    fun fromJson(json: String): ParserRuleBundle? = runCatching {
        val root       = JSONObject(json)
        val version    = root.getInt("version")
        val publishedAt = root.getString("publishedAt")
        val rulesArr   = root.getJSONArray("rules")
        val rules      = (0 until rulesArr.length()).map { i ->
            val r = rulesArr.getJSONObject(i)
            RuleBundleEntry(
                id                   = r.getString("id"),
                category             = r.getString("category"),
                description          = r.optString("description", ""),
                patterns             = jsonStringArray(r.getJSONArray("patterns")),
                fallbackPatterns     = jsonStringArray(r.optJSONArray("fallbackPatterns")),
                counterpartyPatterns = jsonStringArray(r.optJSONArray("counterpartyPatterns")),
            )
        }
        ParserRuleBundle(version, publishedAt, rules)
    }.getOrNull()

    private fun jsonStringArray(arr: JSONArray?): List<String> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }
}
