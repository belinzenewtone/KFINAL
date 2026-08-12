package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.FeeCategoryTotal
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.util.currentMonthKey
import com.belinze.lifeos.util.monthKeyToEndMillis
import com.belinze.lifeos.util.monthKeyToStartMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * FeeAnalyticsViewModel — service-charge analytics for the month.
 * Mirrors FeeAnalyticsScreen.tsx.
 */
@HiltViewModel
class FeeAnalyticsViewModel @Inject constructor(
    private val dao: TransactionDao,
) : ViewModel() {

    data class FeeAnalyticsUiState(
        val isLoading:    Boolean = true,
        val totalFees:    Double  = 0.0,
        val categories:   List<FeeCategoryTotal> = emptyList(),
        val transactions: List<TransactionEntity> = emptyList(),
    )

    private val _uiState = MutableStateFlow(FeeAnalyticsUiState())
    val uiState: StateFlow<FeeAnalyticsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = FeeAnalyticsUiState(isLoading = true)
        viewModelScope.launch {
            val key = currentMonthKey()
            val startIso = Instant.ofEpochMilli(monthKeyToStartMillis(key))
                .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endIso = Instant.ofEpochMilli(monthKeyToEndMillis(key))
                .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            val total  = dao.getFeeTotal(startIso, endIso) ?: 0.0
            val cats   = dao.getFeeByCategory(startIso, endIso)
            val txs    = dao.getFeeTransactions(startIso, endIso)
            _uiState.value = FeeAnalyticsUiState(
                isLoading    = false,
                totalFees    = total,
                categories   = cats,
                transactions = txs,
            )
        }
    }
}
