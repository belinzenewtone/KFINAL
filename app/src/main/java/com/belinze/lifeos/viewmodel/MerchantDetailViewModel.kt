package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MerchantDetailViewModel — stats + transactions for a single merchant.
 * Mirrors MerchantDetailScreen.tsx.
 */
@HiltViewModel
class MerchantDetailViewModel
    @Inject
    constructor(
    private val dao: TransactionDao,
) : ViewModel() {
    @Immutable
    data class MerchantStats(
        val totalSpend:  Double,
        val txCount:     Int,
        val avgAmount:   Double,
        val activeDays:  Int,
        val avgPerDay:   Double,
        val peakDay:     String?,
        val peakAmount:  Double,
    )

    @Immutable
    data class MerchantDetailUiState(
        val isLoading:    Boolean = true,
        val transactions: ImmutableList<TransactionEntity> = persistentListOf(),
        val stats:        MerchantStats? = null,
    )

    private val _uiState = MutableStateFlow(MerchantDetailUiState())
    val uiState: StateFlow<MerchantDetailUiState> = _uiState.asStateFlow()

    fun load(merchant: String) {
        _uiState.value = MerchantDetailUiState(isLoading = true)
        viewModelScope.launch {
            val txs = dao.getByMerchant(merchant)
            // RFINAL: the headline total and the average cover ALL transactions; only the
            // temporal stats (active days, peak day) are outflow-only. avgPerDay then
            // divides the all-transaction total by the outflow day count — quirky, but
            // that is what the reference screen displays.
            val totalSpend = txs.sumOf { it.amount }
            val outflow = txs.filter { it.transactionType in listOf("expense", "transfer", "fuliza") }
            val dayTotals = outflow
                .filter { !it.date.isNullOrBlank() }
                .groupBy { it.date!!.take(10) }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            val peak = dayTotals.maxByOrNull { it.value }
            val stats = MerchantStats(
                totalSpend = totalSpend,
                txCount    = txs.size,
                avgAmount  = if (txs.isNotEmpty()) totalSpend / txs.size else 0.0,
                activeDays = dayTotals.size,
                avgPerDay  = if (dayTotals.isNotEmpty()) totalSpend / dayTotals.size else 0.0,
                peakDay    = peak?.key,
                peakAmount = peak?.value ?: 0.0,
            )
            _uiState.value = MerchantDetailUiState(isLoading = false, transactions = txs.toImmutableList(), stats = stats)
        }
    }
}
