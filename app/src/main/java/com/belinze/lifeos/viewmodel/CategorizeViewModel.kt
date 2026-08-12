package com.belinze.lifeos.viewmodel

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

/**
 * CategorizeViewModel — drives the Categorize screen (grouped by merchant).
 *
 * Mirrors CategorizeScreen.tsx: loads uncategorized transactions, groups them
 * by merchant, and assigns a category to every transaction in a merchant group.
 */
@HiltViewModel
class CategorizeViewModel @Inject constructor(
    private val dao: TransactionDao,
) : ViewModel() {

    data class MerchantGroup(
        val merchant:        String,
        val transactionCount: Int,
        val totalAmount:     Double,
        val latestDate:      String,
    )

    data class CategorizeUiState(
        val isLoading: Boolean      = true,
        val groups:    List<MerchantGroup> = emptyList(),
        val message:   String?      = null,
        val isError:   Boolean      = false,
    )

    private val _uiState = MutableStateFlow(CategorizeUiState())
    val uiState: StateFlow<CategorizeUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val txs = dao.getUncategorized()
            val byMerchant = txs.groupBy { it.merchant ?: "Unknown" }
            val groups = byMerchant.entries.map { (merchant, list) ->
                MerchantGroup(
                    merchant         = merchant,
                    transactionCount = list.size,
                    totalAmount      = list.sumOf { it.amount },
                    latestDate       = list.maxOfOrNull { it.date ?: "" } ?: "",
                )
            }.sortedByDescending { it.latestDate }
            _uiState.value = CategorizeUiState(isLoading = false, groups = groups)
        }
    }

    fun assignCategory(merchant: String, category: String) {
        _uiState.value = _uiState.value.copy(
            groups = _uiState.value.groups.filterNot { it.merchant == merchant },
        )
        viewModelScope.launch {
            try {
                dao.updateCategoryForMerchant(merchant, category, nowIso())
                _uiState.value = _uiState.value.copy(
                    message = "Saved for $merchant",
                    isError = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to save category",
                    isError = true,
                )
                refresh()
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, isError = false)
    }
}
