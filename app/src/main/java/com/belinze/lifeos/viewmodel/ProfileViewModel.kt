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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@Immutable
data class ProfileFormState(
    val name:      String  = "",
    val email:     String  = "",
    val phone:     String  = "",
    val username:  String  = "",
    val avatarUri: String  = "",
    val isSaving:  Boolean = false,
    val error:     String? = null,
)

@HiltViewModel
class ProfileViewModel
    @Inject
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

    private val _formState = MutableStateFlow(ProfileFormState())
    val formState: StateFlow<ProfileFormState> = _formState.asStateFlow()

    init {
        // Sync form from prefs whenever prefs change (avoid overwrite while saving)
        prefState
            .onEach { p ->
                _formState.update { f ->
                    if (f.isSaving) {
                        f
                    } else {
                        f.copy(
                        name      = p.profileName,
                        email     = p.profileEmail,
                        phone     = p.profilePhone,
                        username  = p.profileUsername,
                        avatarUri = p.profileAvatarUri,
                    )
                    }
                }
            }
            .launchIn(viewModelScope)

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

    fun updateName(v: String) = _formState.update { it.copy(name = v) }

    fun updateEmail(v: String) = _formState.update { it.copy(email = v) }

    fun updatePhone(v: String) = _formState.update { it.copy(phone = v) }

    fun updateUsername(v: String) = _formState.update { it.copy(username = v) }

    fun updateAvatarUri(v: String) = _formState.update { it.copy(avatarUri = v) }

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

    fun saveProfile(onSuccess: () -> Unit) {
        val form = _formState.value
        _formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                appPreferences.update {
                    it[PreferenceKeys.PROFILE_NAME]       = form.name
                    it[PreferenceKeys.PROFILE_EMAIL]      = form.email
                    it[PreferenceKeys.PROFILE_PHONE]      = form.phone
                    it[PreferenceKeys.PROFILE_USERNAME]   = form.username
                    it[PreferenceKeys.PROFILE_AVATAR_URI] = form.avatarUri
                }
                _formState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun refreshStats() = loadStats()
}
