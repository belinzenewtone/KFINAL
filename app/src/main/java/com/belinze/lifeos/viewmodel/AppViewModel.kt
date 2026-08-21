package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.datastore.AppPreferenceState
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.datastore.PreferenceKeys
import com.belinze.lifeos.services.BudgetAlertService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI state — mirrors useAppStore shape from the RN app
// ─────────────────────────────────────────────────────────────────────────────

data class AppUiState(
    val hasHydrated: Boolean          = false,
    val isAppLocked: Boolean          = false,
    val prefs:       AppPreferenceState = AppPreferenceState(),
)

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
    private val appPreferences: AppPreferences,
    private val budgetAlertService: BudgetAlertService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.state.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        hasHydrated = true,
                        prefs       = prefs,
                    )
                }
            }
        }
    }

    fun setAppLocked(locked: Boolean) {
        _uiState.update { it.copy(isAppLocked = locked) }
    }

    /**
     * Foreground re-check: mirrors the RN AppState 'active' handler that runs
     * syncAllNotifications + checkAllBudgetThresholds (throttled) when the app
     * returns to the foreground.
     */
    fun refreshOnForeground() {
        viewModelScope.launch {
            runCatching {
                val state = appPreferences.state.first()
                budgetAlertService.checkAllBudgetThresholds(state)
            }
        }
    }

    fun setAuthenticated(authenticated: Boolean) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.IS_AUTHENTICATED] = authenticated }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()   // ISO yyyy-MM-dd
            appPreferences.update {
                it[PreferenceKeys.HAS_COMPLETED_ONBOARDING] = true
                // Stamp member-since date once; never overwrite once set.
                if (it[PreferenceKeys.PROFILE_CREATED_AT].isNullOrBlank()) {
                    it[PreferenceKeys.PROFILE_CREATED_AT] = today
                }
            }
        }
    }

    // ─── Onboarding flow state ─────────────────────────────────────────────────

    fun setOnboardingStep(step: Int) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.ONBOARDING_STEP] = step }
        }
    }

    fun setOnboardingGoal(goal: String) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.ONBOARDING_GOAL] = goal }
        }
    }

    fun setProfileName(name: String) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.PROFILE_NAME] = name }
        }
    }

    fun setProfileUsername(username: String) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.PROFILE_USERNAME] = username }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled }
        }
    }

    fun setSmsBgReceiver(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.SMS_BG_RECEIVER] = enabled }
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.THEME] = theme }
        }
    }

    fun updateFulizaLimit(limit: Double) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.FULIZA_LIMIT] = limit }
        }
    }

    /**
     * Mirrors the RN "Forgot PIN?" flow: disables screen lock, clears PIN and fingerprint.
     * After this call the auth guard will stop showing AppLockScreen.
     */
    fun forgotPin() {
        viewModelScope.launch {
            appPreferences.update { prefs ->
                prefs[PreferenceKeys.SCREEN_LOCK_ENABLED]  = false
                prefs[PreferenceKeys.PIN_CODE]             = ""
                prefs[PreferenceKeys.FINGERPRINT_ENABLED]  = false
            }
            _uiState.update { it.copy(isAppLocked = false) }
        }
    }
}
