package com.belinze.lifeos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.SmsDao
import com.belinze.lifeos.data.db.entity.ImportAuditEntity
import com.belinze.lifeos.data.db.entity.SmsIngestQueueEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SmsImportHealthViewModel — recent import audit + quarantine queue.
 * Mirrors SmsImportHealthScreen.tsx.
 */
@HiltViewModel
class SmsImportHealthViewModel @Inject constructor(
    private val smsDao: SmsDao,
) : ViewModel() {

    data class SmsImportHealthUiState(
        val isLoading:    Boolean = true,
        val audit:        List<ImportAuditEntity> = emptyList(),
        val auditCount:   Int = 0,
        val pendingQueue: Int = 0,
        val quarantined:  List<SmsIngestQueueEntity> = emptyList(),
        val rejections:   List<ImportAuditEntity> = emptyList(),
    )

    private val _uiState = MutableStateFlow(SmsImportHealthUiState())
    val uiState: StateFlow<SmsImportHealthUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = SmsImportHealthUiState(isLoading = true)
        viewModelScope.launch {
            val audit      = smsDao.getAuditLog(50)
            val pending    = smsDao.countPending()
            val quarantined = smsDao.getQuarantined()
            val rejections = smsDao.getRecentRejections(50)
            _uiState.value = SmsImportHealthUiState(
                isLoading    = false,
                audit        = audit,
                auditCount   = audit.size,
                pendingQueue = pending,
                quarantined  = quarantined,
                rejections   = rejections,
            )
        }
    }

    fun retryQuarantined() {
        viewModelScope.launch {
            smsDao.retryAllQuarantined(java.time.LocalDateTime.now().toString())
            load()
        }
    }
}
