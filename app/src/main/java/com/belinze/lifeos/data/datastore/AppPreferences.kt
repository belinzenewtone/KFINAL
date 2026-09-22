package com.belinze.lifeos.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// ─── DataStore singleton extension ───────────────────────────────────────────

val Context.appDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "lifeos_prefs")

// ─────────────────────────────────────────────────────────────────────────────
// Key definitions — mirroring AppSettings and UserProfile from useAppStore.ts
// ─────────────────────────────────────────────────────────────────────────────

object PreferenceKeys {
    // Onboarding / auth
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    val IS_AUTHENTICATED         = booleanPreferencesKey("is_authenticated")
    val ONBOARDING_STEP          = intPreferencesKey("onboarding_step")
    val ONBOARDING_GOAL          = stringPreferencesKey("onboarding_goal")

    // Settings
    val THEME                    = stringPreferencesKey("theme")               // "system" | "dark" | "light"
    val CURRENCY                 = stringPreferencesKey("currency")
    val DATE_FORMAT              = stringPreferencesKey("date_format")
    val TIME_FORMAT              = stringPreferencesKey("time_format")         // "12h" | "24h"
    val DECIMAL_PRECISION        = intPreferencesKey("decimal_precision")
    val HAPTIC_FEEDBACK          = booleanPreferencesKey("haptic_feedback")
    val DEFAULT_TX_CATEGORY      = stringPreferencesKey("default_tx_category")
    val FULIZA_LIMIT             = doublePreferencesKey("fuliza_limit")

    // Notifications
    val NOTIFICATIONS_ENABLED    = booleanPreferencesKey("notifications_enabled")
    val NOTIF_REMINDERS          = booleanPreferencesKey("notif_reminders")
    val NOTIF_TASK_REMINDERS     = booleanPreferencesKey("notif_task_reminders")
    val NOTIF_BILL_REMINDERS     = booleanPreferencesKey("notif_bill_reminders")
    val NOTIF_BUDGET_ALERTS      = booleanPreferencesKey("notif_budget_alerts")
    val NOTIF_DAILY_DIGEST       = booleanPreferencesKey("notif_daily_digest")
    val DAILY_DIGEST_MORNING     = booleanPreferencesKey("daily_digest_morning_summary")
    val DAILY_DIGEST_TIME        = stringPreferencesKey("daily_digest_delivery_time")
    val NOTIF_RECURRING_RULES    = booleanPreferencesKey("notif_recurring_rules")
    val NOTIF_TX_ALERTS          = booleanPreferencesKey("notif_tx_alerts")
    val ASSISTANT_QUICK_SUGGESTIONS = booleanPreferencesKey("assistant_quick_suggestions")

    // App lock
    val SCREEN_LOCK_ENABLED      = booleanPreferencesKey("screen_lock_enabled")
    val PIN_CODE                 = stringPreferencesKey("pin_code")
    val FINGERPRINT_ENABLED      = booleanPreferencesKey("fingerprint_enabled")
    val LOCK_TIMEOUT_MINUTES     = intPreferencesKey("lock_timeout_minutes")

    // Budget alerts
    val BUDGET_THRESHOLD_ALERTS  = booleanPreferencesKey("budget_threshold_alerts")
    val ALERT_THRESHOLD_HIGH     = intPreferencesKey("alert_threshold_high")
    val ALERT_THRESHOLD_MEDIUM   = intPreferencesKey("alert_threshold_medium")
    val ALERT_THRESHOLD_LOW      = intPreferencesKey("alert_threshold_low")

    // SMS
    val SMS_BG_RECEIVER          = booleanPreferencesKey("sms_bg_receiver")

    // OTA parser rules
    val PARSER_RULES_JSON        = stringPreferencesKey("parser_rules_json")
    val PARSER_RULES_VERSION     = intPreferencesKey("parser_rules_version")

    // Calendar
    val CALENDAR_SWIPE           = booleanPreferencesKey("calendar_swipe")

    // Profile
    val PROFILE_NAME             = stringPreferencesKey("profile_name")
    val PROFILE_EMAIL            = stringPreferencesKey("profile_email")
    val PROFILE_PHONE            = stringPreferencesKey("profile_phone")
    val PROFILE_AVATAR_URI       = stringPreferencesKey("profile_avatar_uri")
    val PROFILE_USERNAME         = stringPreferencesKey("profile_username")
    val PROFILE_CREATED_AT       = stringPreferencesKey("profile_created_at")

    // Fired budget alerts — JSON map of "category|level|yearMonth" → ISO timestamp
    val FIRED_BUDGET_ALERTS      = stringPreferencesKey("fired_budget_alerts")

    // On-device ML decision tree (JSON serialized)
    val ML_TREE_JSON             = stringPreferencesKey("ml_decision_tree")
}

// ─────────────────────────────────────────────────────────────────────────────
// AppPreferences — injectable wrapper around DataStore<Preferences>
// ─────────────────────────────────────────────────────────────────────────────

/** Strongly-typed snapshot of persisted app state. */
data class AppPreferenceState(
    val hasCompletedOnboarding: Boolean = false,
    val isAuthenticated: Boolean        = false,
    val onboardingStep: Int             = 0,
    val onboardingGoal: String          = "balanced",
    val theme: String                   = "system",
    val currency: String                = "KES",
    val dateFormat: String              = "dd/MM/yyyy",
    val timeFormat: String              = "24h",
    val decimalPrecision: Int           = 2,
    val hapticFeedback: Boolean         = true,
    val defaultTxCategory: String       = "uncategorized",
    val fulizaLimit: Double             = 0.0,
    val notificationsEnabled: Boolean   = false,
    val notifReminders: Boolean         = true,
    val notifTaskReminders: Boolean     = true,
    val notifBillReminders: Boolean     = true,
    val notifBudgetAlerts: Boolean      = true,
    val notifDailyDigest: Boolean       = false,
    val notifRecurringRules: Boolean    = true,
    val notifTxAlerts: Boolean          = false,
    val dailyDigestMorningSummary: Boolean = false,
    val dailyDigestDeliveryTime: String = "06:30",
    val assistantQuickSuggestions: Boolean = true,
    val screenLockEnabled: Boolean      = false,
    val pinCode: String                 = "",
    val fingerprintEnabled: Boolean     = false,
    val lockTimeoutMinutes: Int         = 5,
    val budgetThresholdAlerts: Boolean  = true,
    val alertThresholdHigh: Int         = 90,
    val alertThresholdMedium: Int       = 75,
    val alertThresholdLow: Int          = 50,
    val smsBgReceiver: Boolean          = false,
    val calendarSwipe: Boolean          = true,
    val profileName: String             = "",
    val profileEmail: String            = "",
    val profilePhone: String            = "",
    val profileAvatarUri: String        = "",
    val profileUsername: String         = "",
    val profileCreatedAt: String        = "",   // ISO date set on first onboarding completion
    val firedBudgetAlerts: Map<String, String> = emptyMap(),
)

@Singleton
class AppPreferences
    @Inject
    constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.appDataStore

    val state: Flow<AppPreferenceState> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs.toState() }

    suspend fun update(transform: suspend (MutablePreferences) -> Unit) {
        store.edit { transform(it) }
    }

    /** Wipe all persisted preferences — used by "Clear all local data". */
    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    /** Cached OTA parser-rule bundle: raw JSON paired with its version (0 = none). */
    suspend fun parserRulesCached(): Pair<String?, Int> {
        val p = store.data.first()
        return p[PreferenceKeys.PARSER_RULES_JSON] to (p[PreferenceKeys.PARSER_RULES_VERSION] ?: 0)
    }

    /** Persist a freshly synced OTA parser-rule bundle. */
    suspend fun setParserRules(json: String, version: Int) {
        store.edit {
            it[PreferenceKeys.PARSER_RULES_JSON] = json
            it[PreferenceKeys.PARSER_RULES_VERSION] = version
        }
    }

    /** Persist a fired budget-alert key (category|level|yearMonth) → ISO timestamp. */
    suspend fun markBudgetAlertFired(key: String, timestamp: String) {
        store.edit { prefs ->
            val raw = prefs[PreferenceKeys.FIRED_BUDGET_ALERTS]
            val obj = try {
                if (raw.isNullOrBlank()) org.json.JSONObject() else org.json.JSONObject(raw)
            } catch (_: Exception) {
                org.json.JSONObject()
            }
            obj.put(key, timestamp)
            prefs[PreferenceKeys.FIRED_BUDGET_ALERTS] = obj.toString()
        }
    }

    /** Read the persisted ML decision tree JSON, or null if none. */
    suspend fun getMLTree(): String? =
        store.data.first()[PreferenceKeys.ML_TREE_JSON]

    /** Persist a newly trained ML decision tree. */
    suspend fun setMLTree(json: String) =
        store.edit { it[PreferenceKeys.ML_TREE_JSON] = json }

    private fun Preferences.toState() = AppPreferenceState(
        hasCompletedOnboarding = this[PreferenceKeys.HAS_COMPLETED_ONBOARDING] ?: false,
        isAuthenticated        = this[PreferenceKeys.IS_AUTHENTICATED]         ?: false,
        onboardingStep         = this[PreferenceKeys.ONBOARDING_STEP]          ?: 0,
        onboardingGoal         = this[PreferenceKeys.ONBOARDING_GOAL]          ?: "balanced",
        theme                  = this[PreferenceKeys.THEME]                    ?: "system",
        currency               = this[PreferenceKeys.CURRENCY]                 ?: "KES",
        dateFormat             = this[PreferenceKeys.DATE_FORMAT]              ?: "dd/MM/yyyy",
        timeFormat             = this[PreferenceKeys.TIME_FORMAT]              ?: "24h",
        decimalPrecision       = this[PreferenceKeys.DECIMAL_PRECISION]        ?: 2,
        hapticFeedback         = this[PreferenceKeys.HAPTIC_FEEDBACK]          ?: true,
        defaultTxCategory      = this[PreferenceKeys.DEFAULT_TX_CATEGORY]      ?: "uncategorized",
        fulizaLimit            = this[PreferenceKeys.FULIZA_LIMIT]             ?: 0.0,
        notificationsEnabled   = this[PreferenceKeys.NOTIFICATIONS_ENABLED]    ?: false,
        notifReminders         = this[PreferenceKeys.NOTIF_REMINDERS]          ?: true,
        notifTaskReminders     = this[PreferenceKeys.NOTIF_TASK_REMINDERS]     ?: true,
        notifBillReminders     = this[PreferenceKeys.NOTIF_BILL_REMINDERS]     ?: true,
        notifBudgetAlerts      = this[PreferenceKeys.NOTIF_BUDGET_ALERTS]      ?: true,
        notifDailyDigest       = this[PreferenceKeys.NOTIF_DAILY_DIGEST]       ?: false,
        notifRecurringRules    = this[PreferenceKeys.NOTIF_RECURRING_RULES]    ?: true,
        notifTxAlerts          = this[PreferenceKeys.NOTIF_TX_ALERTS]          ?: false,
        dailyDigestMorningSummary = this[PreferenceKeys.DAILY_DIGEST_MORNING] ?: false,
        dailyDigestDeliveryTime = this[PreferenceKeys.DAILY_DIGEST_TIME]      ?: "06:30",
        assistantQuickSuggestions = this[PreferenceKeys.ASSISTANT_QUICK_SUGGESTIONS] ?: true,
        screenLockEnabled      = this[PreferenceKeys.SCREEN_LOCK_ENABLED]      ?: false,
        pinCode                = this[PreferenceKeys.PIN_CODE]                 ?: "",
        fingerprintEnabled     = this[PreferenceKeys.FINGERPRINT_ENABLED]      ?: false,
        lockTimeoutMinutes     = this[PreferenceKeys.LOCK_TIMEOUT_MINUTES]     ?: 5,
        budgetThresholdAlerts  = this[PreferenceKeys.BUDGET_THRESHOLD_ALERTS]  ?: true,
        alertThresholdHigh     = this[PreferenceKeys.ALERT_THRESHOLD_HIGH]     ?: 90,
        alertThresholdMedium   = this[PreferenceKeys.ALERT_THRESHOLD_MEDIUM]   ?: 75,
        alertThresholdLow      = this[PreferenceKeys.ALERT_THRESHOLD_LOW]      ?: 50,
        smsBgReceiver          = this[PreferenceKeys.SMS_BG_RECEIVER]          ?: false,
        calendarSwipe          = this[PreferenceKeys.CALENDAR_SWIPE]           ?: true,
        profileName            = this[PreferenceKeys.PROFILE_NAME]             ?: "",
        profileEmail           = this[PreferenceKeys.PROFILE_EMAIL]            ?: "",
        profilePhone           = this[PreferenceKeys.PROFILE_PHONE]            ?: "",
        profileAvatarUri       = this[PreferenceKeys.PROFILE_AVATAR_URI]       ?: "",
        profileUsername        = this[PreferenceKeys.PROFILE_USERNAME]         ?: "",
        profileCreatedAt       = this[PreferenceKeys.PROFILE_CREATED_AT]       ?: "",
        firedBudgetAlerts      = parseFiredAlerts(this[PreferenceKeys.FIRED_BUDGET_ALERTS]),
    )

    /** Parse the stored JSON map of fired budget alerts; prune to current+prev month. */
    private fun parseFiredAlerts(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val now = java.time.LocalDate.now()
            val ym = { d: java.time.LocalDate -> "${d.year}-${String.format(java.util.Locale.US, "%02d", d.monthValue)}" }
            val keep = setOf(ym(now), ym(now.minusMonths(1)))
            val result = mutableMapOf<String, String>()
            obj.keys().forEach { key ->
                val parts = key.split("|")
                val month = parts.getOrNull(2)
                if (month != null && keep.contains(month)) {
                    result[key] = obj.optString(key)
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
