package com.belinze.lifeos.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    val NOTIF_BUDGET_ALERTS      = booleanPreferencesKey("notif_budget_alerts")
    val NOTIF_DAILY_DIGEST       = booleanPreferencesKey("notif_daily_digest")
    val NOTIF_RECURRING_RULES    = booleanPreferencesKey("notif_recurring_rules")
    val NOTIF_TX_ALERTS          = booleanPreferencesKey("notif_tx_alerts")

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

    // Calendar
    val CALENDAR_SWIPE           = booleanPreferencesKey("calendar_swipe")

    // Profile
    val PROFILE_NAME             = stringPreferencesKey("profile_name")
    val PROFILE_EMAIL            = stringPreferencesKey("profile_email")
    val PROFILE_PHONE            = stringPreferencesKey("profile_phone")
    val PROFILE_AVATAR_URI       = stringPreferencesKey("profile_avatar_uri")
    val PROFILE_USERNAME         = stringPreferencesKey("profile_username")
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
    val notifBudgetAlerts: Boolean      = true,
    val notifDailyDigest: Boolean       = false,
    val notifRecurringRules: Boolean    = true,
    val notifTxAlerts: Boolean          = false,
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
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.appDataStore

    val state: Flow<AppPreferenceState> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs.toState() }

    suspend fun update(transform: suspend (MutablePreferences) -> Unit) {
        store.edit { transform(it) }
    }

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
        notifBudgetAlerts      = this[PreferenceKeys.NOTIF_BUDGET_ALERTS]      ?: true,
        notifDailyDigest       = this[PreferenceKeys.NOTIF_DAILY_DIGEST]       ?: false,
        notifRecurringRules    = this[PreferenceKeys.NOTIF_RECURRING_RULES]    ?: true,
        notifTxAlerts          = this[PreferenceKeys.NOTIF_TX_ALERTS]          ?: false,
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
    )
}
