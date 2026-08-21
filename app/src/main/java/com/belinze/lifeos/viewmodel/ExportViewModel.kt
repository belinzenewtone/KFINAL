package com.belinze.lifeos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.BudgetDao
import com.belinze.lifeos.data.db.dao.EventDao
import com.belinze.lifeos.data.db.dao.IncomeDao
import com.belinze.lifeos.data.db.dao.PlannerDao
import com.belinze.lifeos.data.db.dao.TaskDao
import com.belinze.lifeos.data.db.dao.TransactionDao
import com.belinze.lifeos.data.db.entity.ExportEntity
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject

enum class ExportFormat { JSON, CSV, PDF }

/**
 * ExportViewModel — writes a JSON or CSV export of the selected domains to
 * the app's external files directory and records it in the exports table.
 * Mirrors ExportScreen.tsx.
 */
@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
    @ApplicationContext private val context: Context,
    private val transactionDao: TransactionDao,
    private val taskDao:        TaskDao,
    private val eventDao:       EventDao,
    private val budgetDao:      BudgetDao,
    private val incomeDao:      IncomeDao,
    private val plannerDao:     PlannerDao,
) : ViewModel() {
    data class ExportUiState(
        val isLoading:    Boolean = false,
        val lastExport:   String? = null,
        val error:        String? = null,
        val history:      List<ExportEntity> = emptyList(),
        val domainCounts: Map<String, Int> = emptyMap(),
    )

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init { loadHistory() }

    fun loadHistory() {
        viewModelScope.launch {
            val counts = mapOf(
                "transactions" to transactionDao.getPage(10000, 0).size,
                "tasks"        to taskDao.getAll().size,
                "events"       to eventDao.getAll().size,
                "budgets"      to budgetDao.getAll().size,
                "incomes"      to incomeDao.getAll().size,
                "recurring"    to plannerDao.getAllRules().size,
                "goals"        to plannerDao.getAllGoals().size,
            )
            _uiState.value = _uiState.value.copy(
                history      = plannerDao.getAllExports(),
                domainCounts = counts,
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            plannerDao.deleteAllExports()
            _uiState.value = _uiState.value.copy(history = emptyList())
        }
    }

    fun exportJson(
        includeTransactions: Boolean,
        includeTasks:        Boolean,
        includeEvents:       Boolean,
        includeBudgets:      Boolean,
        includeIncomes:      Boolean,
        includeRecurring:    Boolean,
        includeGoals:        Boolean,
    ) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val root = JSONObject()
                val exported = JSONObject()

                if (includeTransactions) {
                    exported.put("transactions", transactionDao.getPage(10000, 0)
                        .map { tx -> JSONObject()
                            .put("id", tx.id)
                            .put("amount", tx.amount)
                            .put("merchant", tx.merchant ?: "")
                            .put("category", tx.category ?: "")
                            .put("date", tx.date ?: "")
                            .put("transaction_type", tx.transactionType ?: "")
                            .put("status", tx.status) })
                }
                if (includeTasks) {
                    exported.put("tasks", taskDao.getAll()
                        .map { t -> JSONObject()
                            .put("id", t.id)
                            .put("title", t.title)
                            .put("status", t.status)
                            .put("deadline", t.deadline ?: "")
                            .put("priority", t.priority) })
                }
                if (includeEvents) {
                    exported.put("events", eventDao.getAll()
                        .map { e -> JSONObject()
                            .put("id", e.id)
                            .put("title", e.title)
                            .put("date", e.date)
                            .put("type", e.type) })
                }
                if (includeBudgets) {
                    exported.put("budgets", budgetDao.getAll()
                        .map { b -> JSONObject()
                            .put("id", b.id)
                            .put("category", b.category)
                            .put("limit_amount", b.limitAmount)
                            .put("period", b.period) })
                }
                if (includeIncomes) {
                    exported.put("incomes", incomeDao.getAll()
                        .map { i -> JSONObject()
                            .put("id", i.id)
                            .put("amount", i.amount)
                            .put("source", i.source ?: "")
                            .put("date", i.date ?: "") })
                }
                if (includeRecurring) {
                    exported.put("recurring_rules", plannerDao.getAllRules()
                        .map { r -> JSONObject()
                            .put("id", r.id)
                            .put("title", r.title)
                            .put("cadence", r.cadence ?: "")
                            .put("amount", r.amount ?: 0.0)
                            .put("enabled", r.enabled) })
                }
                if (includeGoals) {
                    exported.put("goals", plannerDao.getAllGoals()
                        .map { g -> JSONObject()
                            .put("id", g.id)
                            .put("title", g.title)
                            .put("target_value", g.targetValue)
                            .put("current_value", g.currentValue) })
                }

                root.put("app", "LifeOS")
                root.put("exported_at", nowIso())
                root.put("data", exported)

                // Write to external files dir (public for sharing)
                val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
                val file = File(dir, "lifeos-export-${System.currentTimeMillis()}.json")
                file.writeText(root.toString(2))

                val recordCount = listOf(
                    if (includeTransactions) exported.getJSONArray("transactions").length() else 0,
                    if (includeTasks) exported.getJSONArray("tasks").length() else 0,
                    if (includeEvents) exported.getJSONArray("events").length() else 0,
                    if (includeBudgets) exported.getJSONArray("budgets").length() else 0,
                    if (includeIncomes) exported.getJSONArray("incomes").length() else 0,
                    if (includeRecurring) exported.getJSONArray("recurring_rules").length() else 0,
                    if (includeGoals) exported.getJSONArray("goals").length() else 0,
                ).sum()

                plannerDao.insertExport(ExportEntity(
                    id          = UUID.randomUUID().toString(),
                    filePath    = file.absolutePath,
                    fileSize    = file.length(),
                    format      = "json",
                    createdAt   = nowIso(),
                    recordCount = recordCount,
                ))

                _uiState.value = _uiState.value.copy(
                    isLoading  = false,
                    lastExport = "Exported $recordCount records to ${file.name}",
                )
                loadHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun exportPdf(
        includeTransactions: Boolean,
        includeTasks:        Boolean,
        includeEvents:       Boolean,
        includeBudgets:      Boolean,
    ) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val sb = StringBuilder()
                var count = 0
                val nowStr = nowIso().take(10)
                sb.appendLine("=".repeat(50))
                sb.appendLine("  LifeOS Data Export — $nowStr")
                sb.appendLine("=".repeat(50))
                sb.appendLine()

                if (includeTransactions) {
                    val txs = transactionDao.getPage(10000, 0)
                    sb.appendLine("Transactions (${txs.size}):")
                    sb.appendLine("-".repeat(40))
                    txs.forEach { tx ->
                        sb.appendLine("  ${tx.date?.take(10) ?: ""}  ${tx.merchant ?: "Unknown"}  KES ${tx.amount}  [${tx.category ?: ""}]")
                        count++
                    }
                    sb.appendLine()
                }
                if (includeTasks) {
                    val tasks = taskDao.getAll()
                    sb.appendLine("Tasks (${tasks.size}):")
                    sb.appendLine("-".repeat(40))
                    tasks.forEach { t -> sb.appendLine("  [${t.status}]  ${t.title}  ${t.priority}"); count++ }
                    sb.appendLine()
                }
                if (includeEvents) {
                    val events = eventDao.getAll()
                    sb.appendLine("Events (${events.size}):")
                    sb.appendLine("-".repeat(40))
                    events.forEach { e -> sb.appendLine("  ${e.date.take(10)}  ${e.title}  (${e.type})"); count++ }
                    sb.appendLine()
                }
                if (includeBudgets) {
                    val budgets = budgetDao.getAll()
                    sb.appendLine("Budgets (${budgets.size}):")
                    sb.appendLine("-".repeat(40))
                    budgets.forEach { b -> sb.appendLine("  ${b.category}  Limit: KES ${b.limitAmount}"); count++ }
                    sb.appendLine()
                }
                sb.appendLine("=".repeat(50))
                sb.appendLine("  Generated by LifeOS")
                sb.appendLine("=".repeat(50))

                val dir  = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
                val file = File(dir, "LifeOS_Export_${nowIso().take(10)}.txt")
                file.writeText(sb.toString())
                plannerDao.insertExport(ExportEntity(
                    id = UUID.randomUUID().toString(), filePath = file.absolutePath,
                    fileSize = file.length(), format = "pdf", createdAt = nowIso(), recordCount = count,
                ))
                _uiState.value = _uiState.value.copy(isLoading = false, lastExport = "Exported $count records to ${file.name}")
                loadHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun exportCsv(
        includeTransactions: Boolean,
        includeTasks:        Boolean,
        includeEvents:       Boolean,
        includeBudgets:      Boolean,
        includeIncomes:      Boolean,
        includeRecurring:    Boolean,
        includeGoals:        Boolean,
    ) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val sb = StringBuilder()
                var totalRecords = 0

                if (includeTransactions) {
                    sb.appendLine("# transactions")
                    sb.appendLine("id,amount,merchant,category,date,transaction_type,status")
                    transactionDao.getPage(10000, 0).forEach { tx ->
                        sb.appendLine("${tx.id},${tx.amount},\"${tx.merchant ?: ""}\",\"${tx.category ?: ""}\",\"${tx.date ?: ""}\",\"${tx.transactionType ?: ""}\",\"${tx.status}\"")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeTasks) {
                    sb.appendLine("# tasks")
                    sb.appendLine("id,title,status,deadline,priority")
                    taskDao.getAll().forEach { t ->
                        sb.appendLine("${t.id},\"${t.title}\",\"${t.status}\",\"${t.deadline ?: ""}\",\"${t.priority}\"")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeEvents) {
                    sb.appendLine("# events")
                    sb.appendLine("id,title,date,type")
                    eventDao.getAll().forEach { e ->
                        sb.appendLine("${e.id},\"${e.title}\",\"${e.date}\",\"${e.type}\"")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeBudgets) {
                    sb.appendLine("# budgets")
                    sb.appendLine("id,category,limit_amount,period")
                    budgetDao.getAll().forEach { b ->
                        sb.appendLine("${b.id},\"${b.category}\",${b.limitAmount},\"${b.period}\"")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeIncomes) {
                    sb.appendLine("# incomes")
                    sb.appendLine("id,amount,source,date")
                    incomeDao.getAll().forEach { i ->
                        sb.appendLine("${i.id},${i.amount},\"${i.source ?: ""}\",\"${i.date ?: ""}\"")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeRecurring) {
                    sb.appendLine("# recurring_rules")
                    sb.appendLine("id,title,cadence,amount,enabled")
                    plannerDao.getAllRules().forEach { r ->
                        sb.appendLine("${r.id},\"${r.title}\",\"${r.cadence ?: ""}\",${r.amount ?: 0.0},${r.enabled}")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeGoals) {
                    sb.appendLine("# goals")
                    sb.appendLine("id,title,target_value,current_value")
                    plannerDao.getAllGoals().forEach { g ->
                        sb.appendLine("${g.id},\"${g.title}\",${g.targetValue},${g.currentValue}")
                        totalRecords++
                    }
                }

                val dir  = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
                val file = File(dir, "lifeos-export-${System.currentTimeMillis()}.csv")
                file.writeText(sb.toString())

                plannerDao.insertExport(ExportEntity(
                    id          = UUID.randomUUID().toString(),
                    filePath    = file.absolutePath,
                    fileSize    = file.length(),
                    format      = "csv",
                    createdAt   = nowIso(),
                    recordCount = totalRecords,
                ))

                _uiState.value = _uiState.value.copy(
                    isLoading  = false,
                    lastExport = "Exported $totalRecords records to ${file.name}",
                )
                loadHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
