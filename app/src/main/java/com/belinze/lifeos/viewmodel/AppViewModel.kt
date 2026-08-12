package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.datastore.AppPreferenceState
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.datastore.PreferenceKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class AppViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
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

    fun setAuthenticated(authenticated: Boolean) {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.IS_AUTHENTICATED] = authenticated }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            appPreferences.update { it[PreferenceKeys.HAS_COMPLETED_ONBOARDING] = true }
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
