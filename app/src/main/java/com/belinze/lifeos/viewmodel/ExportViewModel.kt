package com.belinze.lifeos.viewmodel

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

enum class ExportFormat { JSON, CSV, PDF }

/**
 * ExportViewModel — writes a JSON, CSV, or PDF export of the selected domains
 * filtered by the chosen date window.  After writing, fires a system share
 * sheet so the user can immediately save or send the file.
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
    @Immutable
    data class ExportUiState(
        val isLoading:    Boolean = false,
        val lastExport:   String? = null,
        val error:        String? = null,
        val history:      ImmutableList<ExportEntity> = persistentListOf(),
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
                history      = plannerDao.getAllExports().toImmutableList(),
                domainCounts = counts,
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            plannerDao.deleteAllExports()
            _uiState.value = _uiState.value.copy(history = persistentListOf())
        }
    }

    // ─── Date window helper ───────────────────────────────────────────────────

    /**
     * Returns (startDate, endDate) as ISO date strings (YYYY-MM-DD) or null for
     * "all time".  Used to filter exported records to the selected window.
     */
    private fun computeDateRange(
        dateWindow:  String,
        customStart: String,
        customEnd:   String,
    ): Pair<String?, String?> {
        val today = LocalDate.now()
        return when (dateWindow) {
            "week"   -> today.minusDays(6).toString() to today.toString()
            "month"  -> today.withDayOfMonth(1).toString() to today.toString()
            "last30" -> today.minusDays(29).toString() to today.toString()
            "custom" -> customStart.takeIf { it.isNotBlank() } to customEnd.takeIf { it.isNotBlank() }
            else     -> null to null  // "all" — no filter
        }
    }

    // ─── Share intent ─────────────────────────────────────────────────────────

    /** Fires the system share sheet for [file] using FileProvider so the URI
     *  is valid on all Android versions, including 7+. */
    private fun shareFile(file: File, mimeType: String) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(share, "Share ${file.name}")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    // ─── JSON export ──────────────────────────────────────────────────────────

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun exportJson(
        includeTransactions: Boolean,
        includeTasks:        Boolean,
        includeEvents:       Boolean,
        includeBudgets:      Boolean,
        includeIncomes:      Boolean,
        includeRecurring:    Boolean,
        includeGoals:        Boolean,
        dateWindow:          String  = "all",
        customStart:         String  = "",
        customEnd:           String  = "",
    ) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val (startDate, endDate) = computeDateRange(dateWindow, customStart, customEnd)

                val root     = JSONObject()
                val exported = JSONObject()

                if (includeTransactions) {
                    val txs = transactionDao.getFiltered(
                        search = "", category = "all", type = null, status = null,
                        startDate = startDate, endDate = endDate, limit = 10000, offset = 0,
                    )
                    exported.put("transactions", txs.map { tx -> JSONObject()
                        .put("id", tx.id)
                        .put("amount", tx.amount)
                        .put("merchant", tx.merchant ?: "")
                        .put("category", tx.category ?: "")
                        .put("date", tx.date ?: "")
                        .put("transaction_type", tx.transactionType ?: "")
                        .put("status", tx.status) })
                }
                if (includeTasks) {
                    val tasks = taskDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { t ->
                            val d = t.deadline?.take(10) ?: return@filter true
                            (startDate == null || d >= startDate) && (endDate == null || d <= (endDate ?: d))
                        }
                        } else {
                            all
                        }
                    }
                    exported.put("tasks", tasks.map { t -> JSONObject()
                        .put("id", t.id)
                        .put("title", t.title)
                        .put("status", t.status)
                        .put("deadline", t.deadline ?: "")
                        .put("priority", t.priority) })
                }
                if (includeEvents) {
                    val events = eventDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { e ->
                            val d = e.date.take(10)
                            d >= (startDate ?: d) && d <= (endDate ?: d)
                        }
                        } else {
                            all
                        }
                    }
                    exported.put("events", events.map { e -> JSONObject()
                        .put("id", e.id)
                        .put("title", e.title)
                        .put("date", e.date)
                        .put("type", e.type) })
                }
                if (includeBudgets) {
                    exported.put("budgets", budgetDao.getAll().map { b -> JSONObject()
                        .put("id", b.id)
                        .put("category", b.category)
                        .put("limit_amount", b.limitAmount)
                        .put("period", b.period) })
                }
                if (includeIncomes) {
                    val incomes = incomeDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { i ->
                            val d = i.date?.take(10) ?: return@filter true
                            d >= (startDate ?: d) && d <= (endDate ?: d)
                        }
                        } else {
                            all
                        }
                    }
                    exported.put("incomes", incomes.map { i -> JSONObject()
                        .put("id", i.id)
                        .put("amount", i.amount)
                        .put("source", i.source ?: "")
                        .put("date", i.date ?: "") })
                }
                if (includeRecurring) {
                    exported.put("recurring_rules", plannerDao.getAllRules().map { r -> JSONObject()
                        .put("id", r.id)
                        .put("title", r.title)
                        .put("cadence", r.cadence ?: "")
                        .put("amount", r.amount ?: 0.0)
                        .put("enabled", r.enabled) })
                }
                if (includeGoals) {
                    exported.put("goals", plannerDao.getAllGoals().map { g -> JSONObject()
                        .put("id", g.id)
                        .put("title", g.title)
                        .put("target_value", g.targetValue)
                        .put("current_value", g.currentValue) })
                }

                root.put("app", "LifeOS")
                root.put("exported_at", nowIso())
                root.put("data", exported)

                val dir  = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
                val file = File(dir, "lifeos-export-${System.currentTimeMillis()}.json")
                file.writeText(root.toString(2))

                val recordCount = listOf(
                    if (includeTransactions && exported.has("transactions")) exported.getJSONArray("transactions").length() else 0,
                    if (includeTasks        && exported.has("tasks"))        exported.getJSONArray("tasks").length()        else 0,
                    if (includeEvents       && exported.has("events"))       exported.getJSONArray("events").length()       else 0,
                    if (includeBudgets      && exported.has("budgets"))      exported.getJSONArray("budgets").length()      else 0,
                    if (includeIncomes      && exported.has("incomes"))      exported.getJSONArray("incomes").length()      else 0,
                    if (includeRecurring    && exported.has("recurring_rules")) exported.getJSONArray("recurring_rules").length() else 0,
                    if (includeGoals        && exported.has("goals"))        exported.getJSONArray("goals").length()        else 0,
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

                // BUG #28: share the file immediately via system share sheet
                shareFile(file, "application/json")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // ─── PDF export ───────────────────────────────────────────────────────────

    fun exportPdf(
        includeTransactions: Boolean,
        includeTasks:        Boolean,
        includeEvents:       Boolean,
        includeBudgets:      Boolean,
        dateWindow:          String  = "all",
        customStart:         String  = "",
        customEnd:           String  = "",
    ) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val (startDate, endDate) = computeDateRange(dateWindow, customStart, customEnd)

                val sb    = StringBuilder()
                var count = 0
                val nowStr = nowIso().take(10)
                sb.appendLine("=".repeat(50))
                sb.appendLine("  LifeOS Data Export — $nowStr")
                if (dateWindow != "all") sb.appendLine("  Window: $dateWindow${if (startDate != null) " ($startDate to ${endDate ?: nowStr})" else ""}")
                sb.appendLine("=".repeat(50))
                sb.appendLine()

                if (includeTransactions) {
                    val txs = transactionDao.getFiltered(
                        search = "", category = "all", type = null, status = null,
                        startDate = startDate, endDate = endDate, limit = 10000, offset = 0,
                    )
                    sb.appendLine("Transactions (${txs.size}):")
                    sb.appendLine("-".repeat(40))
                    txs.forEach { tx ->
                        sb.appendLine("  ${tx.date?.take(10) ?: ""}  ${tx.merchant ?: "Unknown"}  KES ${tx.amount}  [${tx.category ?: ""}]")
                        count++
                    }
                    sb.appendLine()
                }
                if (includeTasks) {
                    val tasks = taskDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { t ->
                            val d = t.deadline?.take(10) ?: return@filter true
                            d >= (startDate ?: d) && d <= (endDate ?: d)
                        }
                        } else {
                            all
                        }
                    }
                    sb.appendLine("Tasks (${tasks.size}):")
                    sb.appendLine("-".repeat(40))
                    tasks.forEach { t -> sb.appendLine("  [${t.status}]  ${t.title}  ${t.priority}"); count++ }
                    sb.appendLine()
                }
                if (includeEvents) {
                    val events = eventDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { e ->
                            val d = e.date.take(10)
                            d >= (startDate ?: d) && d <= (endDate ?: d)
                        }
                        } else {
                            all
                        }
                    }
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
                val file = File(dir, "LifeOS_Export_$nowStr.txt")
                file.writeText(sb.toString())

                plannerDao.insertExport(ExportEntity(
                    id          = UUID.randomUUID().toString(),
                    filePath    = file.absolutePath,
                    fileSize    = file.length(),
                    format      = "pdf",
                    createdAt   = nowIso(),
                    recordCount = count,
                ))
                _uiState.value = _uiState.value.copy(
                    isLoading  = false,
                    lastExport = "Exported $count records to ${file.name}",
                )
                loadHistory()

                // BUG #28: share the file immediately
                shareFile(file, "text/plain")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // ─── CSV export ───────────────────────────────────────────────────────────

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun exportCsv(
        includeTransactions: Boolean,
        includeTasks:        Boolean,
        includeEvents:       Boolean,
        includeBudgets:      Boolean,
        includeIncomes:      Boolean,
        includeRecurring:    Boolean,
        includeGoals:        Boolean,
        dateWindow:          String  = "all",
        customStart:         String  = "",
        customEnd:           String  = "",
    ) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val (startDate, endDate) = computeDateRange(dateWindow, customStart, customEnd)

                val sb           = StringBuilder()
                var totalRecords = 0

                if (includeTransactions) {
                    val txs = transactionDao.getFiltered(
                        search = "", category = "all", type = null, status = null,
                        startDate = startDate, endDate = endDate, limit = 10000, offset = 0,
                    )
                    sb.appendLine("# transactions")
                    sb.appendLine("id,amount,merchant,category,date,transaction_type,status")
                    txs.forEach { tx ->
                        sb.appendLine("${tx.id},${tx.amount},\"${tx.merchant ?: ""}\",\"${tx.category ?: ""}\",\"${tx.date ?: ""}\",\"${tx.transactionType ?: ""}\",\"${tx.status}\"")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeTasks) {
                    val tasks = taskDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { t ->
                            val d = t.deadline?.take(10) ?: return@filter true
                            d >= (startDate ?: d) && d <= (endDate ?: d)
                        }
                        } else {
                            all
                        }
                    }
                    sb.appendLine("# tasks")
                    sb.appendLine("id,title,status,deadline,priority")
                    tasks.forEach { t ->
                        sb.appendLine("${t.id},\"${t.title}\",\"${t.status}\",\"${t.deadline ?: ""}\",\"${t.priority}\"")
                        totalRecords++
                    }
                    sb.appendLine()
                }
                if (includeEvents) {
                    val events = eventDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { e ->
                            val d = e.date.take(10)
                            d >= (startDate ?: d) && d <= (endDate ?: d)
                        }
                        } else {
                            all
                        }
                    }
                    sb.appendLine("# events")
                    sb.appendLine("id,title,date,type")
                    events.forEach { e ->
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
                    val incomes = incomeDao.getAll().let { all ->
                        if (startDate != null) {
                            all.filter { i ->
                            val d = i.date?.take(10) ?: return@filter true
                            d >= (startDate ?: d) && d <= (endDate ?: d)
                        }
                        } else {
                            all
                        }
                    }
                    sb.appendLine("# incomes")
                    sb.appendLine("id,amount,source,date")
                    incomes.forEach { i ->
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

                // BUG #28: share the file immediately
                shareFile(file, "text/csv")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
