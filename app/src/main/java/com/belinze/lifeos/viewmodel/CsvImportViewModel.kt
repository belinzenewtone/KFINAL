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
import java.util.UUID
import javax.inject.Inject

/**
 * CsvImportViewModel — parses pasted CSV, auto-detects columns, previews rows,
 * and inserts transactions. Mirrors csvImportService.ts + CsvImportScreen.tsx.
 */
@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val dao: TransactionDao,
) : ViewModel() {

    data class CsvRow(
        val amount:   Double,
        val merchant: String,
        val date:     String,
        val category: String,
        val type:     String,
    )

    data class CsvImportUiState(
        val isLoading:  Boolean = false,
        val headers:    List<String> = emptyList(),
        val preview:    List<CsvRow> = emptyList(),
        val totalRows:  Int = 0,
        val imported:   Int = 0,
        val error:      String? = null,
        val done:       Boolean = false,
    )

    private val _uiState = MutableStateFlow(CsvImportUiState())
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    /** Parse raw CSV text into rows using a simple RFC-4180 splitter. */
    fun parse(raw: String) {
        _uiState.value = CsvImportUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val rows = parseCsv(raw)
                if (rows.isEmpty()) {
                    _uiState.value = CsvImportUiState(error = "No rows found in CSV")
                    return@launch
                }
                val headers = rows.first().map { it.trim().lowercase() }
                val data = rows.drop(1)
                val amtIdx  = headers.indexOfFirst { it.contains("amount") || it.contains("ksh") }
                val merchIdx = headers.indexOfFirst { it.contains("merchant") || it.contains("name") || it.contains("payee") || it.contains("counterpart") }
                val dateIdx = headers.indexOfFirst { it.contains("date") || it.contains("time") }
                val catIdx = headers.indexOfFirst { it.contains("categor") }
                val typeIdx = headers.indexOfFirst { it.contains("type") || it.contains("direction") }

                val preview = data.take(10).mapNotNull { row ->
                    val amount = row.getOrNull(amtIdx)?.replace(",", "")?.trim()?.toDoubleOrNull()
                        ?: row.getOrNull(amtIdx)?.replace(",", "")?.trim()?.toDoubleOrNull()
                        ?: return@mapNotNull null
                    val merchant = row.getOrNull(merchIdx)?.trim() ?: ""
                    if (merchant.isEmpty()) return@mapNotNull null
                    val date = normalizeDate(row.getOrNull(dateIdx)?.trim())
                    CsvRow(
                        amount   = amount,
                        merchant = merchant,
                        date     = date,
                        category = row.getOrNull(catIdx)?.trim()?.lowercase() ?: "uncategorized",
                        type     = when {
                            row.getOrNull(typeIdx)?.trim()?.lowercase()?.contains("in") == true ||
                                row.getOrNull(typeIdx)?.trim()?.lowercase()?.contains("receive") == true -> "receive"
                            else -> "expense"
                        },
                    )
                }

                _uiState.value = CsvImportUiState(
                    isLoading = false,
                    headers   = headers,
                    preview   = preview,
                    totalRows = preview.size,
                )
            } catch (e: Exception) {
                _uiState.value = CsvImportUiState(error = "Failed to parse CSV: ${e.message}")
            }
        }
    }

    fun importAll() {
        if (_uiState.value.preview.isEmpty()) return
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                var count = 0
                _uiState.value.preview.forEach { row ->
                    dao.insert(TransactionEntity(
                        id              = UUID.randomUUID().toString(),
                        amount          = row.amount,
                        merchant        = row.merchant,
                        category        = row.category,
                        date            = row.date,
                        source          = "csv",
                        transactionType = row.type,
                        status          = "completed",
                        createdAt       = nowIso(),
                        updatedAt       = nowIso(),
                    ))
                    count++
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    imported  = count,
                    done      = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun normalizeDate(raw: String?): String {
        if (raw.isNullOrBlank()) return nowIso()
        // Accept YYYY-MM-DD, DD/MM/YYYY, DD-MM-YYYY
        return try {
            val parts = raw.trim().split("/", "-", ".")
            when {
                raw.contains("-") && parts.size == 3 && parts[0].length == 4 ->
                    "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}T00:00:00"
                parts.size == 3 && parts[2].length == 4 ->
                    "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}T00:00:00"
                else -> nowIso()
            }
        } catch (_: Exception) {
            nowIso()
        }
    }

    /** Minimal RFC-4180 CSV splitter (handles quoted fields with commas). */
    private fun parseCsv(raw: String): List<List<String>> {
        val result = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        var field = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = raw.trim()
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < s.length && s[i + 1] == '"') { field.append('"'); i++ }
                        else inQuotes = false
                    } else field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field = StringBuilder() }
                c == '\n' -> {
                    row.add(field.toString()); field = StringBuilder()
                    result.add(row); row = mutableListOf()
                }
                c == '\r' -> Unit
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); result.add(row) }
        return result
    }
}
