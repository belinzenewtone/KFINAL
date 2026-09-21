package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Transactions grouped by merchant for the Categorize screen. */
@Immutable
data class MerchantGroup(
    val merchant: String,
    val transactions: ImmutableList<TransactionEntity>,
) {
    val count: Int get() = transactions.size
}

/**
 * CategorizeViewModel — per-transaction and per-merchant categorization.
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
        val isLoading:      Boolean = true,
        val transactions:   ImmutableList<TransactionEntity> = persistentListOf(),
        val merchantGroups: ImmutableList<MerchantGroup>    = persistentListOf(),
        val groupByMerchant: Boolean = true,
        val message:        String? = null,
        val isError:        Boolean = false,
    )

    private val _uiState = MutableStateFlow(CategorizeUiState())
    val uiState: StateFlow<CategorizeUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val txs = dao.getUncategorized()
            val groups = txs
                .groupBy { it.merchant?.trim()?.ifBlank { "Unknown" } ?: "Unknown" }
                .entries
                .sortedByDescending { it.value.size }
                .map { (merchant, list) -> MerchantGroup(merchant, list.toImmutableList()) }
                .toImmutableList()
            _uiState.value = CategorizeUiState(
                isLoading      = false,
                transactions   = txs.toImmutableList(),
                merchantGroups = groups,
                groupByMerchant = _uiState.value.groupByMerchant,
            )
        }
    }

    fun setGroupByMerchant(grouped: Boolean) {
        _uiState.value = _uiState.value.copy(groupByMerchant = grouped)
    }

    /** Assign category to a single transaction. */
    fun assignCategory(id: String, category: String) {
        _uiState.value = _uiState.value.copy(
            transactions   = _uiState.value.transactions.filterNot { it.id == id }.toImmutableList(),
            merchantGroups = _uiState.value.merchantGroups
                .map { g -> g.copy(transactions = g.transactions.filterNot { it.id == id }.toImmutableList()) }
                .filter { it.count > 0 }
                .toImmutableList(),
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

    /** Assign category to ALL transactions for a given merchant at once. */
    fun assignCategoryForMerchant(merchant: String, category: String) {
        val count = _uiState.value.merchantGroups.find { it.merchant == merchant }?.count ?: 0
        val removedIds = _uiState.value.merchantGroups
            .find { it.merchant == merchant }?.transactions?.map { it.id }?.toSet() ?: emptySet()
        _uiState.value = _uiState.value.copy(
            transactions   = _uiState.value.transactions.filterNot { it.id in removedIds }.toImmutableList(),
            merchantGroups = _uiState.value.merchantGroups.filterNot { it.merchant == merchant }.toImmutableList(),
            message        = "Assigned $count transaction${if (count != 1) "s" else ""} as $category",
            isError        = false,
        )
        viewModelScope.launch {
            try {
                dao.updateCategoryForMerchant(merchant, category, nowIso())
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
