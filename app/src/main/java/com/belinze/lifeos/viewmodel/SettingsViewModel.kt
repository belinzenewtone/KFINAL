package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.datastore.PreferenceKeys
import com.belinze.lifeos.data.db.LifeOsDatabase
import com.belinze.lifeos.util.Haptics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// SettingsViewModel
//
// Mirrors the Settings screen store. Wraps AppPreferences to expose all
// toggleable settings as a StateFlow and provides typed update actions.
//
// Shared with AppViewModel — the settings shape comes from AppPreferenceState.
// This VM is scoped to the Settings screen graph so it doesn't stay alive
// when the user is on other tabs.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val db: LifeOsDatabase,
) : ViewModel() {

    /** The full prefs snapshot, shared with AppViewModel but scoped here. */
    val settings: StateFlow<com.belinze.lifeos.data.datastore.AppPreferenceState> =
        prefs.state.stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = com.belinze.lifeos.data.datastore.AppPreferenceState(),
        )

    // ── Theme ─────────────────────────────────────────────────────────────────

    fun setTheme(theme: String) = update { it[PreferenceKeys.THEME] = theme }

    // ── Notifications ─────────────────────────────────────────────────────────

    fun setNotificationsEnabled(enabled: Boolean) =
        update { it[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled }

    fun setNotifReminders(v: Boolean)     = update { it[PreferenceKeys.NOTIF_REMINDERS] = v }
    fun setNotifBudgetAlerts(v: Boolean)  = update { it[PreferenceKeys.NOTIF_BUDGET_ALERTS] = v }
    fun setNotifDailyDigest(v: Boolean)   = update { it[PreferenceKeys.NOTIF_DAILY_DIGEST] = v }
    fun setNotifRecurring(v: Boolean)     = update { it[PreferenceKeys.NOTIF_RECURRING_RULES] = v }
    fun setNotifTxAlerts(v: Boolean)      = update { it[PreferenceKeys.NOTIF_TX_ALERTS] = v }

    // ── Screen lock ───────────────────────────────────────────────────────────

    fun setScreenLockEnabled(v: Boolean)  = update { it[PreferenceKeys.SCREEN_LOCK_ENABLED] = v }
    fun setPinCode(pin: String)           = update { it[PreferenceKeys.PIN_CODE] = pin }
    fun setFingerprintEnabled(v: Boolean) = update { it[PreferenceKeys.FINGERPRINT_ENABLED] = v }
    fun setLockTimeout(minutes: Int)      = update { it[PreferenceKeys.LOCK_TIMEOUT_MINUTES] = minutes }

    // Convenience: disable screen lock and clear PIN
    fun disableScreenLock() = viewModelScope.launch {
        prefs.update {
            it[PreferenceKeys.SCREEN_LOCK_ENABLED]  = false
            it[PreferenceKeys.FINGERPRINT_ENABLED]  = false
            it[PreferenceKeys.PIN_CODE]             = ""
        }
    }

    // ── Budget alerts ─────────────────────────────────────────────────────────

    fun setBudgetAlerts(v: Boolean)       = update { it[PreferenceKeys.BUDGET_THRESHOLD_ALERTS] = v }
    fun setAlertThresholdHigh(v: Int)     = update { it[PreferenceKeys.ALERT_THRESHOLD_HIGH] = v }
    fun setAlertThresholdMedium(v: Int)   = update { it[PreferenceKeys.ALERT_THRESHOLD_MEDIUM] = v }
    fun setAlertThresholdLow(v: Int)      = update { it[PreferenceKeys.ALERT_THRESHOLD_LOW] = v }

    // ── SMS ───────────────────────────────────────────────────────────────────

    fun setSmsBgReceiver(v: Boolean)      = update { it[PreferenceKeys.SMS_BG_RECEIVER] = v }

    // ── Fuliza ────────────────────────────────────────────────────────────────

    fun setFulizaLimit(limit: Double)     = update { it[PreferenceKeys.FULIZA_LIMIT] = limit }

    // ── Currency ──────────────────────────────────────────────────────────────

    fun setCurrency(v: String)            = update { it[PreferenceKeys.CURRENCY] = v }

    // ── Profile ───────────────────────────────────────────────────────────────

    fun setProfileAvatarUri(uri: String)  = update { it[PreferenceKeys.PROFILE_AVATAR_URI] = uri }

    // ── Display ───────────────────────────────────────────────────────────────

    fun setDateFormat(v: String)          = update { it[PreferenceKeys.DATE_FORMAT] = v }
    fun setTimeFormat(v: String)          = update { it[PreferenceKeys.TIME_FORMAT] = v }
    fun setDecimalPrecision(v: Int)       = update { it[PreferenceKeys.DECIMAL_PRECISION] = v }
    fun setHapticFeedback(v: Boolean) {
        Haptics.enabled = v
        update { it[PreferenceKeys.HAPTIC_FEEDBACK] = v }
        // Mirrors RN: a success pulse when the user enables haptics.
        if (v) Haptics.success()
    }
    fun setCalendarSwipe(v: Boolean)      = update { it[PreferenceKeys.CALENDAR_SWIPE] = v }
    fun setDefaultCategory(v: String)     = update { it[PreferenceKeys.DEFAULT_TX_CATEGORY] = v }

    // ─── Danger zone ───────────────────────────────────────────────────────────

    /**
     * Wipe every local table and all preferences, returning the app to a
     * fresh-install state (the auth guard will show Onboarding again).
     */
    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                db.clearAllTables()
                prefs.clearAll()
                onDone()
            } catch (e: Exception) {
                // Best-effort; still clear prefs so the app can restart cleanly.
                prefs.clearAll()
                onDone()
            }
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun update(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { prefs.update(block) }
    }
}
