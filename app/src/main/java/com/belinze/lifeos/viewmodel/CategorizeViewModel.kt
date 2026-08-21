package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * CategorizeViewModel — per-transaction categorization.
 * Mirrors CategorizeScreen.tsx.
 */
@HiltViewModel
class CategorizeViewModel
    @Inject
    constructor(
    private val dao: TransactionDao,
) : ViewModel() {
    @Immutable
    data class CategorizeUiState(
        val isLoading:    Boolean = true,
        val transactions: ImmutableList<TransactionEntity> = persistentListOf(),
        val message:      String? = null,
        val isError:      Boolean = false,
    )

    private val _uiState = MutableStateFlow(CategorizeUiState())
    val uiState: StateFlow<CategorizeUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val txs = dao.getUncategorized()
            _uiState.value = CategorizeUiState(isLoading = false, transactions = txs.toImmutableList())
        }
    }

    fun assignCategory(id: String, category: String) {
        _uiState.value = _uiState.value.copy(
            transactions = _uiState.value.transactions.filterNot { it.id == id }.toImmutableList(),
        )
        viewModelScope.launch {
            try {
                dao.updateCategoryById(id, category, nowIso())
                _uiState.value = _uiState.value.copy(message = "Saved", isError = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "Failed to save category", isError = true)
                refresh()
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, isError = false)
    }
}
