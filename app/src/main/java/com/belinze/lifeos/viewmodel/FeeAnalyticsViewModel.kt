package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.FeeCategoryTotal
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * FeeAnalyticsViewModel — service-charge analytics for the month.
 * Mirrors FeeAnalyticsScreen.tsx.
 */
class FeeAnalyticsViewModel
constructor(
    private val dao: TransactionDao,
) : ViewModel() {
    @Immutable
    data class FeeAnalyticsUiState(
        val isLoading:    Boolean = true,
        val totalFees:    Double  = 0.0,
        val categories:   ImmutableList<FeeCategoryTotal> = persistentListOf(),
        val transactions: ImmutableList<TransactionEntity> = persistentListOf(),
    )

    private val _uiState = MutableStateFlow(FeeAnalyticsUiState())
    val uiState: StateFlow<FeeAnalyticsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = FeeAnalyticsUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val key = currentMonthKey()
                val zone = TimeZone.currentSystemDefault()
                val startIso = Instant.fromEpochMilliseconds(monthKeyToStartMillis(key))
                    .toLocalDateTime(zone).toString()
                val endIso = Instant.fromEpochMilliseconds(monthKeyToEndMillis(key))
                    .toLocalDateTime(zone).toString()

                val total  = dao.getFeeTotal(startIso, endIso) ?: 0.0
                val cats   = dao.getChargesByCategory(startIso, endIso)
                val txs    = dao.getFeeTransactions(startIso, endIso)
                _uiState.value = FeeAnalyticsUiState(
                    isLoading    = false,
                    totalFees    = total,
                    categories   = cats.toImmutableList(),
                    transactions = txs.toImmutableList(),
                )
            } catch (e: Exception) {
                _uiState.value = FeeAnalyticsUiState(isLoading = false)
            }
        }
    }
}
