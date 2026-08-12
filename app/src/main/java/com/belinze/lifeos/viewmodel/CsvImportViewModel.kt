package com.belinze.lifeos.viewmodel

import android.net.Uri
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

data class CsvColumnMapping(
    val amount: String = "",
    val merchant: String = "",
    val date: String = "",
    val category: String = "",
    val type: String = "",
    val status: String = "",
    val description: String = "",
)

data class CsvImportCandidate(
    val amount: Double,
    val merchant: String,
    val date: String,
    val category: String,
    val type: String,
    val description: String?,
    val errors: List<String> = emptyList(),
)

/**
 * CsvImportViewModel — file-based CSV import with column mapping and validation.
 */
@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val dao: TransactionDao,
) : ViewModel() {

    data class CsvImportUiState(
        val isLoading:  Boolean = false,
        val headers:    List<String> = emptyList(),
        val mapping:    CsvColumnMapping = CsvColumnMapping(),
        val valid:      List<CsvImportCandidate> = emptyList(),
        val invalid:    List<CsvImportCandidate> = emptyList(),
        val imported:   Int = 0,
        val error:      String? = null,
        val done:       Boolean = false,
    )

    private val _uiState = MutableStateFlow(CsvImportUiState())
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    private var rawDataRows: List<List<String>> = emptyList()

    fun loadFromUri(uri: Uri, readText: (Uri) -> String?) {
        _uiState.value = CsvImportUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val raw = readText(uri) ?: run {
                    _uiState.value = CsvImportUiState(error = "Failed to read CSV file")
                    return@launch
                }
                val rows = parseCsv(raw)
                if (rows.isEmpty()) {
                    _uiState.value = CsvImportUiState(error = "No rows found in CSV")
                    return@launch
                }
                val headers = rows.first()
                rawDataRows = rows.drop(1)
                val detected = detectMapping(headers)
                val mapping = CsvColumnMapping(
                    amount = detected.amount,
                    merchant = detected.merchant,
                    date = detected.date,
                    category = detected.category,
                    type = detected.type,
                    status = detected.status,
                    description = detected.description,
                )
                recompute(headers, rawDataRows, mapping)
            } catch (e: Exception) {
                _uiState.value = CsvImportUiState(error = "Failed to parse CSV: ${e.message}")
            }
        }
    }

    fun updateMapping(field: String, header: String) {
        val current = _uiState.value
        val mapping = when (field) {
            "amount" -> current.mapping.copy(amount = header)
            "merchant" -> current.mapping.copy(merchant = header)
            "date" -> current.mapping.copy(date = header)
            "category" -> current.mapping.copy(category = header)
            "type" -> current.mapping.copy(type = header)
            "status" -> current.mapping.copy(status = header)
            "description" -> current.mapping.copy(description = header)
            else -> current.mapping
        }
        recompute(current.headers, rawDataRows, mapping)
    }

    fun importValid() {
        val valid = _uiState.value.valid
        if (valid.isEmpty()) return
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                var count = 0
                valid.forEach { row ->
                    dao.insert(TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        amount = row.amount,
                        merchant = row.merchant,
                        category = row.category,
                        date = row.date,
                        source = "csv",
                        transactionType = row.type,
                        status = "completed",
                        description = row.description,
                        createdAt = nowIso(),
                        updatedAt = nowIso(),
                    ))
                    count++
                }
                _uiState.value = _uiState.value.copy(isLoading = false, imported = count, done = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun recompute(headers: List<String>, dataRows: List<List<String>>, mapping: CsvColumnMapping) {
        val headerIdx = { h: String -> if (h.isBlank()) -1 else headers.indexOfFirst { it.equals(h, ignoreCase = true) } }
        val amtIdx = headerIdx(mapping.amount)
        val merchIdx = headerIdx(mapping.merchant)
        val dateIdx = headerIdx(mapping.date)
        val catIdx = headerIdx(mapping.category)
        val typeIdx = headerIdx(mapping.type)
        val statusIdx = headerIdx(mapping.status)
        val descIdx = headerIdx(mapping.description)

        val candidates = dataRows.mapNotNull { row ->
            val errors = mutableListOf<String>()
            val amount = row.getOrNull(amtIdx)?.replace(",", "")?.trim()?.toDoubleOrNull()
            if (amount == null) errors += "Missing or invalid amount"
            val merchant = row.getOrNull(merchIdx)?.trim().orEmpty()
            if (merchant.isEmpty()) errors += "Missing merchant"
            val date = normalizeDate(row.getOrNull(dateIdx))
            if (date.isBlank()) errors += "Invalid date"

            if (amount == null) null
            else CsvImportCandidate(
                amount = amount,
                merchant = merchant,
                date = date,
                category = row.getOrNull(catIdx)?.trim()?.lowercase().orEmpty().ifBlank { "uncategorized" },
                type = when {
                    row.getOrNull(typeIdx)?.trim()?.lowercase()?.contains("in") == true -> "income"
                    else -> "expense"
                },
                description = row.getOrNull(descIdx)?.trim()?.ifBlank { null },
                errors = errors,
            )
        }

        _uiState.value = CsvImportUiState(
            isLoading = false,
            headers = headers,
            mapping = mapping,
            valid = candidates.filter { it.errors.isEmpty() },
            invalid = candidates.filter { it.errors.isNotEmpty() },
        )
    }

    private fun detectMapping(headers: List<String>): CsvColumnMapping {
        fun find(vararg keys: String): String =
            headers.firstOrNull { h -> keys.any { h.lowercase().contains(it) } }.orEmpty()

        return CsvColumnMapping(
            amount = find("amount", "ksh"),
            merchant = find("merchant", "name", "payee", "counterpart"),
            date = find("date", "time"),
            category = find("categor"),
            type = find("type", "direction"),
            status = find("status"),
            description = find("description", "note"),
        )
    }

    private fun normalizeDate(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            val parts = raw.trim().split("/", "-", ".")
            when {
                raw.contains("-") && parts.size == 3 && parts[0].length == 4 ->
                    "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}T00:00:00"
                parts.size == 3 && parts[2].length == 4 ->
                    "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}T00:00:00"
                else -> ""
            }
        } catch (_: Exception) { "" }
    }

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
