package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.datastore.AppPreferenceState
import com.belinze.lifeos.data.datastore.AppPreferences
import com.belinze.lifeos.data.datastore.PreferenceKeys
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.previousMonthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ProfileViewModel
//
// Mirrors the Profile screen store.
// Exposes profile fields (name, email, phone, avatar) + derived stats
// (total transactions, month-over-month change) for the Profile tab.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class ProfileStats(
    val totalTxCount:   Int    = 0,
    val thisMonthSpend: Double = 0.0,
    val lastMonthSpend: Double = 0.0,
    val momChangePct:   Double = 0.0,   // positive = spent more this month
)

@Immutable
data class ProfileUiState(
    val isLoading:     Boolean      = true,
    val stats:         ProfileStats = ProfileStats(),
    val error:         String?      = null,
)

class ProfileViewModel
constructor(
    private val appPreferences:  AppPreferences,
    private val transactionDao:  TransactionDao,
) : ViewModel() {
    /** Live prefs snapshot for the profile page (name, avatar, etc.) */
    val prefState: StateFlow<AppPreferenceState> = appPreferences.state.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPreferenceState(),
    )

    private val _uiState   = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val curKey  = currentMonthKey()
                val prevKey = previousMonthKey()

                val curTotals  = transactionDao.getMonthTotals(curKey)
                val prevTotals = transactionDao.getMonthTotals(prevKey)

                val curSpend  = curTotals.expense  ?: 0.0
                val prevSpend = prevTotals.expense ?: 0.0
                val momPct    = if (prevSpend > 0) {
                    ((curSpend - prevSpend) / prevSpend) * 100.0
                } else {
                    0.0
                }

                // Approx total transaction count
                val recentCount = transactionDao.getPage(1000, 0).size

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats     = ProfileStats(
                            totalTxCount   = recentCount,
                            thisMonthSpend = curSpend,
                            lastMonthSpend = prevSpend,
                            momChangePct   = momPct,
                        ),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Form ─────────────────────────────────────────────────────────────────

    /** Save only name + username (the Profile hero edit modal), preserving the rest. */
    fun saveNameAndUsername(
        name: String,
        username: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                appPreferences.update {
                    it[PreferenceKeys.PROFILE_NAME]     = name.trim().ifEmpty { "User" }
                    it[PreferenceKeys.PROFILE_USERNAME] = username
                }
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    /** Save only the email field. */
    fun saveEmail(email: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                appPreferences.update {
                    if (email.isNullOrBlank()) {
                        it.remove(PreferenceKeys.PROFILE_EMAIL)
                    } else {
                        it[PreferenceKeys.PROFILE_EMAIL] = email
                    }
                }
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    /** Save only the phone field. */
    fun savePhone(phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                appPreferences.update {
                    if (phone.isBlank()) {
                        it.remove(PreferenceKeys.PROFILE_PHONE)
                    } else {
                        it[PreferenceKeys.PROFILE_PHONE] = phone
                    }
                }
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    /** Remove the profile photo (Profile hero photo sheet). */
    fun removeProfilePhoto(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                appPreferences.update {
                    it.remove(PreferenceKeys.PROFILE_AVATAR_URI)
                }
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun refreshStats() = loadStats()
}
