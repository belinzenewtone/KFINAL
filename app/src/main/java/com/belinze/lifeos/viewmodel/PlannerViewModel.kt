package com.belinze.lifeos.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belinze.lifeos.data.db.dao.IncomeDao
import com.belinze.lifeos.data.db.dao.PlannerDao
import com.belinze.lifeos.data.db.entity.BillEntity
import com.belinze.lifeos.data.db.entity.ExportEntity
import com.belinze.lifeos.data.db.entity.FulizaLoanEntity
import com.belinze.lifeos.data.db.entity.GoalEntity
import com.belinze.lifeos.data.db.entity.IncomeEntity
import com.belinze.lifeos.data.db.entity.RecurringRuleEntity
import com.belinze.lifeos.util.Haptics
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// ─────────────────────────────────────────────────────────────────────────────
// PlannerViewModel
//
// Covers the Planner hub — recurring rules, bills, goals, Fuliza loans, and
// income. Mirrors usePlannerStore / individual stores in RN.
// ─────────────────────────────────────────────────────────────────────────────

enum class PlannerTab { Recurring, Bills, Goals, Loans, Income }

@Immutable
data class PlannerUiState(
    val isLoading:      Boolean                           = true,
    val activeTab:      PlannerTab                        = PlannerTab.Recurring,
    val recurringRules: ImmutableList<RecurringRuleEntity> = persistentListOf(),
    val bills:          ImmutableList<BillEntity>          = persistentListOf(),
    val goals:          ImmutableList<GoalEntity>          = persistentListOf(),
    val loans:          ImmutableList<FulizaLoanEntity>    = persistentListOf(),
    val income:         ImmutableList<IncomeEntity>        = persistentListOf(),
    val exports:        ImmutableList<ExportEntity>        = persistentListOf(),
    val totalMonthlyBills: Double                 = 0.0,
    val totalActiveLoans:  Double                 = 0.0,
    val error: String? = null,
)

// ─── Individual form states ───────────────────────────────────────────────────

@Immutable
data class RecurringFormState(
    val id:          String? = null,
    val name:        String  = "",
    val type:        String  = "expense",   // expense | income | task
    val amount:      String  = "",
    val category:    String  = "uncategorized",
    val frequency:   String  = "monthly",  // hourly | daily | weekly | biweekly | mon_fri | monthly | yearly
    val nextRunAt:   String  = nowIso(),
    val notes:       String  = "",
    val enabled:     Boolean = true,
    val isSaving:    Boolean = false,
    val error:       String? = null,
)

@Immutable
data class BillFormState(
    val id:          String?  = null,
    val name:        String   = "",
    val amount:      String   = "",
    val category:    String   = "bills",
    val frequency:   String   = "monthly",
    val nextDueDate: String   = nowIso(),
    val notes:       String   = "",
    val isPaid:      Boolean  = false,
    val isActive:    Boolean  = true,
    val isSaving:    Boolean  = false,
    val error:       String?  = null,
)

@Immutable
data class GoalFormState(
    val id:           String?  = null,
    val name:         String   = "",
    val description:  String   = "",
    val targetAmount: String   = "",
    val savedAmount:  String   = "0",
    val unit:         String   = "",
    val deadline:     String?  = null,
    val status:       String   = "active",
    val category:     String   = "savings",
    val notes:        String   = "",
    val isSaving:     Boolean  = false,
    val error:        String?  = null,
)

@Immutable
data class LoanFormState(
    val id:                String?  = null,
    val drawCode:          String   = "",
    val drawAmountKes:     String   = "",
    val drawDate:          String   = nowIso(),
    val status:            String   = "active",
    val notes:             String   = "",
    val isSaving:          Boolean  = false,
    val error:             String?  = null,
    // Read-only display fields populated from the stored entity
    val totalRepaidKes:     Double   = 0.0,
    val lastRepaymentDate:  String?  = null,
)

@Immutable
data class IncomeFormState(
    val id:          String?  = null,
    val source:      String   = "",
    val amount:      String   = "",
    val date:        String   = nowIso(),
    val category:    String   = "salary",
    val notes:       String   = "",
    val isRecurring: Boolean  = false,
    val frequency:   String   = "once",
    val isSaving:    Boolean  = false,
    val error:       String?  = null,
)

@HiltViewModel
class PlannerViewModel
    @Inject
    constructor(
    private val plannerDao: PlannerDao,
    private val incomeDao:  IncomeDao,
) : ViewModel() {
    private val _uiState        = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    private val _recurringForm  = MutableStateFlow(RecurringFormState())
    val recurringForm: StateFlow<RecurringFormState> = _recurringForm.asStateFlow()

    private val _billForm       = MutableStateFlow(BillFormState())
    val billForm: StateFlow<BillFormState> = _billForm.asStateFlow()

    private val _goalForm       = MutableStateFlow(GoalFormState())
    val goalForm: StateFlow<GoalFormState> = _goalForm.asStateFlow()

    private val _loanForm       = MutableStateFlow(LoanFormState())
    val loanForm: StateFlow<LoanFormState> = _loanForm.asStateFlow()

    private val _incomeForm     = MutableStateFlow(IncomeFormState())
    val incomeForm: StateFlow<IncomeFormState> = _incomeForm.asStateFlow()

    init { loadAll() }

    fun setTab(tab: PlannerTab) = _uiState.update { it.copy(activeTab = tab) }

    // ─── Form field updates ───────────────────────────────────────────────────

    fun updateRecurringName(v: String) = _recurringForm.update { it.copy(name = v) }

    fun updateRecurringType(v: String) = _recurringForm.update { it.copy(type = v) }

    fun updateRecurringAmount(v: String) = _recurringForm.update { it.copy(amount = v) }

    fun updateRecurringCategory(v: String) = _recurringForm.update { it.copy(category = v) }

    fun updateRecurringFrequency(v: String) = _recurringForm.update { it.copy(frequency = v) }

    fun updateRecurringNextRun(v: String) = _recurringForm.update { it.copy(nextRunAt = v) }

    fun updateRecurringEnabled(v: Boolean) = _recurringForm.update { it.copy(enabled = v) }

    fun updateRecurringError(v: String?) = _recurringForm.update { it.copy(error = v) }

    fun updateBillName(v: String) = _billForm.update { it.copy(name = v) }

    fun updateBillAmount(v: String) = _billForm.update { it.copy(amount = v) }

    fun updateBillFrequency(v: String) = _billForm.update { it.copy(frequency = v) }

    fun updateBillNextDue(v: String) = _billForm.update { it.copy(nextDueDate = v) }

    fun updateBillNotes(v: String) = _billForm.update { it.copy(notes = v) }

    fun updateBillPaid(v: Boolean) = _billForm.update { it.copy(isPaid = v) }

    fun updateBillActive(v: Boolean) = _billForm.update { it.copy(isActive = v) }

    fun updateBillError(v: String?) = _billForm.update { it.copy(error = v) }

    fun updateGoalName(v: String) = _goalForm.update { it.copy(name = v) }

    fun updateGoalDescription(v: String) = _goalForm.update { it.copy(description = v) }

    fun updateGoalTarget(v: String) = _goalForm.update { it.copy(targetAmount = v) }

    fun updateGoalSaved(v: String) = _goalForm.update { it.copy(savedAmount = v) }

    fun updateGoalUnit(v: String) = _goalForm.update { it.copy(unit = v) }

    fun updateGoalDeadline(v: String?) = _goalForm.update { it.copy(deadline = v) }

    fun updateGoalStatus(v: String) = _goalForm.update { it.copy(status = v) }

    fun updateGoalError(v: String?) = _goalForm.update { it.copy(error = v) }

    fun updateLoanDrawCode(v: String) = _loanForm.update { it.copy(drawCode = v) }

    fun updateLoanDrawAmount(v: String) = _loanForm.update { it.copy(drawAmountKes = v) }

    fun updateLoanDrawDate(v: String) = _loanForm.update { it.copy(drawDate = v) }

    fun updateLoanStatus(v: String) = _loanForm.update { it.copy(status = v) }

    fun updateLoanError(v: String?) = _loanForm.update { it.copy(error = v) }

    fun updateIncomeSource(v: String) = _incomeForm.update { it.copy(source = v) }

    fun updateIncomeAmount(v: String) = _incomeForm.update { it.copy(amount = v) }

    fun updateIncomeDate(v: String) = _incomeForm.update { it.copy(date = v) }

    fun updateIncomeNote(v: String) = _incomeForm.update { it.copy(notes = v) }

    fun updateIncomeRecurring(v: Boolean) = _incomeForm.update { it.copy(isRecurring = v) }

    fun updateIncomeFrequency(v: String) = _incomeForm.update { it.copy(frequency = v) }

    fun updateIncomeError(v: String?) = _incomeForm.update { it.copy(error = v) }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val rules   = plannerDao.getAllRules()
                val bills   = plannerDao.getAllBills()
                val goals   = plannerDao.getAllGoals()
                val loans   = plannerDao.getAllLoans()
                val income  = incomeDao.getAll()
                val exports = plannerDao.getAllExports()

                _uiState.update {
                    it.copy(
                        isLoading         = false,
                        recurringRules    = rules.toImmutableList(),
                        bills             = bills.toImmutableList(),
                        goals             = goals.toImmutableList(),
                        loans             = loans.toImmutableList(),
                        income            = income.toImmutableList(),
                        exports           = exports.toImmutableList(),
                        totalMonthlyBills = bills.filter { b -> b.isActive != 0 }.sumOf { b -> b.amount ?: 0.0 },
                        totalActiveLoans  = loans.filter { l -> l.status == "active" }.sumOf { l -> l.drawAmountKes - l.totalRepaidKes },
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Recurring ────────────────────────────────────────────────────────────

    fun openRecurringForm(ruleId: String? = null) {
        viewModelScope.launch {
            val e = ruleId?.let { plannerDao.getRuleById(it) }
            _recurringForm.update {
                if (e == null) {
                    RecurringFormState()
                } else {
                    RecurringFormState(
                    id = e.id, name = e.title, type = e.type ?: "expense",
                    amount = e.amount?.toString() ?: "",
                    category = e.category ?: "uncategorized", frequency = e.cadence ?: "monthly",
                    nextRunAt = e.nextRunAt ?: nowIso(), notes = "", enabled = e.enabled != 0,
                )
                }
            }
        }
    }

    fun saveRecurring(onSuccess: () -> Unit) {
        val form = _recurringForm.value
        val amt  = form.amount.toDoubleOrNull()
        if (form.name.isBlank() || amt == null || amt <= 0) {
            _recurringForm.update { it.copy(error = "Name and valid amount required") }
            return
        }
        _recurringForm.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val e = (form.id?.let { plannerDao.getRuleById(it) } ?: RecurringRuleEntity(
                    id = UUID.randomUUID().toString(), title = "", type = "expense", cadence = "monthly",
                    nextRunAt = nowIso(), amount = 0.0, category = "uncategorized", enabled = 1,
                    createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    title = form.name,
                    type = form.type,
                    amount = amt,
                    category = if (form.type == "expense") form.category else null,
                    cadence = form.frequency,
                    nextRunAt = form.nextRunAt,
                    enabled = if (form.enabled) 1 else 0,
                    updatedAt = nowIso(),
                )
                plannerDao.insertRule(e)
                loadAll()
                Haptics.success()
                _recurringForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _recurringForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    fun deleteRule(id: String) = viewModelScope.launch { plannerDao.softDeleteRule(id, nowIso()); loadAll() }

    fun toggleRecurringEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val e = plannerDao.getRuleById(id) ?: return@launch
            plannerDao.updateRule(e.copy(enabled = if (enabled) 1 else 0, updatedAt = nowIso()))
            loadAll()
        }
    }

    // ─── Bills ────────────────────────────────────────────────────────────────

    fun openBillForm(billId: String? = null) {
        viewModelScope.launch {
            val e = billId?.let { plannerDao.getBillById(it) }
            _billForm.update {
                if (e == null) {
                    BillFormState()
                } else {
                    BillFormState(
                    id = e.id, name = e.title, amount = e.amount?.toString() ?: "",
                    category = e.cycle ?: "bills", frequency = e.cycle ?: "monthly",
                    nextDueDate = e.nextDueDate ?: nowIso(), notes = e.notes ?: "",
                    isPaid = e.paidStatus != 0, isActive = e.isActive != 0,
                )
                }
            }
        }
    }

    fun saveBill(onSuccess: () -> Unit) {
        val form = _billForm.value
        val amt  = form.amount.toDoubleOrNull()
        if (form.name.isBlank() || amt == null || amt <= 0) {
            _billForm.update { it.copy(error = "Name and valid amount required") }
            return
        }
        _billForm.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val e = (form.id?.let { plannerDao.getBillById(it) } ?: BillEntity(
                    id = UUID.randomUUID().toString(), title = "", amount = 0.0,
                    cycle = "monthly", nextDueDate = nowIso(), isActive = 1,
                    createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    title = form.name, amount = amt, cycle = form.frequency,
                    nextDueDate = form.nextDueDate,
                    notes = form.notes.ifBlank { null },
                    paidStatus = if (form.isPaid) 1 else 0,
                    lastPaidAt = if (form.isPaid) nowIso() else null,
                    isActive = if (form.isActive) 1 else 0, updatedAt = nowIso(),
                )
                plannerDao.insertBill(e)
                loadAll()
                Haptics.success()
                _billForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _billForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    fun deleteBill(id: String) = viewModelScope.launch { plannerDao.softDeleteBill(id, nowIso()); loadAll() }

    /** Toggle a bill's paid status; advancing the due date for recurring cycles (mirrors BillsScreen.tsx). */
    fun toggleBillPaid(id: String) {
        viewModelScope.launch {
            val bill = plannerDao.getBillById(id) ?: return@launch
            val wasPaid = bill.paidStatus != 0
            val updated = if (!wasPaid) {
                val nextDue = advanceDueDate(bill.nextDueDate, bill.cycle)
                bill.copy(
                    paidStatus = 1,
                    lastPaidAt = nowIso(),
                    nextDueDate = nextDue ?: bill.nextDueDate,
                    updatedAt = nowIso(),
                )
            } else {
                bill.copy(paidStatus = 0, lastPaidAt = null, updatedAt = nowIso())
            }
            plannerDao.updateBill(updated)
            Haptics.success()
            loadAll()
        }
    }

    /** Advance a due date by one cycle — mirrors advanceDueDate() in billCycle.ts. */
    private fun advanceDueDate(due: String?, cycle: String?): String? {
        if (due == null) return null
        return try {
            val date = java.time.LocalDate.parse(due.take(10))
            val next = when (cycle?.lowercase()) {
                "daily"   -> date.plusDays(1)
                "weekly"  -> date.plusWeeks(1)
                "monthly" -> date.plusMonths(1)
                "yearly"  -> date.plusYears(1)
                else      -> return null
            }
            next.toString()
        } catch (_: Exception) {
            null
        }
    }

    // ─── Goals ────────────────────────────────────────────────────────────────

    fun openGoalForm(goalId: String? = null) {
        viewModelScope.launch {
            val e = goalId?.let { plannerDao.getGoalById(it) }
            _goalForm.update {
                if (e == null) {
                    GoalFormState()
                } else {
                    GoalFormState(
                    id = e.id, name = e.title, description = e.description ?: "",
                    targetAmount = e.targetValue.toString(),
                    savedAmount = e.currentValue.toString(), unit = e.unit ?: "",
                    deadline = e.deadline, status = e.status,
                    category = e.category ?: "savings", notes = "",
                )
                }
            }
        }
    }

    fun saveGoal(onSuccess: () -> Unit) {
        val form   = _goalForm.value
        val target = form.targetAmount.toDoubleOrNull()
        val saved  = form.savedAmount.toDoubleOrNull() ?: 0.0
        if (form.name.isBlank() || target == null || target <= 0) {
            _goalForm.update { it.copy(error = "Name and target amount required") }
            return
        }
        _goalForm.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val e = (form.id?.let { plannerDao.getGoalById(it) } ?: GoalEntity(
                    id = UUID.randomUUID().toString(), title = "", targetValue = 0.0,
                    currentValue = 0.0, category = "savings", createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    title = form.name,
                    description = form.description.ifBlank { null },
                    targetValue = target,
                    currentValue = saved,
                    unit = form.unit.ifBlank { null },
                    deadline = form.deadline,
                    status = form.status,
                    category = form.category,
                    updatedAt = nowIso(),
                )
                plannerDao.insertGoal(e)
                loadAll()
                Haptics.success()
                _goalForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _goalForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    fun deleteGoal(id: String) = viewModelScope.launch { plannerDao.softDeleteGoal(id, nowIso()); loadAll() }

    fun addToGoal(goalId: String, amount: Double) {
        viewModelScope.launch {
            val e = plannerDao.getGoalById(goalId) ?: return@launch
            plannerDao.updateGoal(e.copy(currentValue = e.currentValue + amount, updatedAt = nowIso()))
            loadAll()
        }
    }

    fun logGoalProgress(goalId: String, amount: Double) {
        viewModelScope.launch {
            val e = plannerDao.getGoalById(goalId) ?: return@launch
            if (amount <= 0) return@launch
            val next = (e.currentValue + amount).coerceAtMost(e.targetValue)
            val reached = next >= e.targetValue
            plannerDao.updateGoal(
                e.copy(
                    currentValue = next,
                    status = if (reached) "completed" else e.status,
                    updatedAt = nowIso(),
                )
            )
            loadAll()
        }
    }

    fun markGoalComplete(goalId: String) {
        viewModelScope.launch {
            val e = plannerDao.getGoalById(goalId) ?: return@launch
            plannerDao.updateGoal(e.copy(status = "completed", updatedAt = nowIso()))
            loadAll()
        }
    }

    // ─── Loans ────────────────────────────────────────────────────────────────

    fun openLoanForm(loanId: String? = null) {
        viewModelScope.launch {
            val e = loanId?.let { plannerDao.getLoanById(it) }
            _loanForm.update {
                if (e == null) {
                    LoanFormState()
                } else {
                    LoanFormState(
                    id = e.id, drawCode = e.drawCode ?: "",
                    drawAmountKes = e.drawAmountKes.toString(), drawDate = e.drawDate ?: nowIso(),
                    status = e.status, notes = "",
                    totalRepaidKes = e.totalRepaidKes,
                    lastRepaymentDate = e.lastRepaymentDate,
                )
                }
            }
        }
    }

    fun deleteLoan(id: String) = viewModelScope.launch { plannerDao.hardDeleteLoan(id); loadAll() }

    fun logRepayment(loanId: String, amount: Double, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val loan = plannerDao.getLoanById(loanId)
            if (loan == null || amount <= 0) {
                onDone(false)
                return@launch
            }
            val outstanding = (loan.drawAmountKes - loan.totalRepaidKes).coerceAtLeast(0.0)
            val applied = amount.coerceAtMost(outstanding)
            val nextRepaid = loan.totalRepaidKes + applied
            val fullyPaid = nextRepaid >= loan.drawAmountKes - 0.005
            plannerDao.updateLoan(
                loan.copy(
                    totalRepaidKes = nextRepaid,
                    lastRepaymentDate = nowIso(),
                    status = if (fullyPaid) "repaid" else loan.status,
                    updatedAt = nowIso(),
                )
            )
            loadAll()
            onDone(true)
        }
    }

    fun markRepaid(loanId: String) {
        viewModelScope.launch {
            val loan = plannerDao.getLoanById(loanId) ?: return@launch
            plannerDao.updateLoan(
                loan.copy(
                    totalRepaidKes = loan.drawAmountKes,
                    lastRepaymentDate = nowIso(),
                    status = "repaid",
                    updatedAt = nowIso(),
                )
            )
            loadAll()
        }
    }

    fun saveLoan(onSuccess: () -> Unit) {
        val form = _loanForm.value
        val amt  = form.drawAmountKes.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            _loanForm.update { it.copy(error = "Enter a valid draw amount") }
            return
        }
        _loanForm.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val e = (form.id?.let { plannerDao.getLoanById(it) } ?: FulizaLoanEntity(
                    id = UUID.randomUUID().toString(),
                    drawCode = form.drawCode.ifBlank { null },
                    drawAmountKes = amt,
                    status = "active",
                    drawDate = form.drawDate,
                    createdAt = nowIso(),
                    updatedAt = nowIso(),
                )).copy(
                    drawCode = form.drawCode.ifBlank { null },
                    drawAmountKes = amt,
                    status = form.status,
                    drawDate = form.drawDate,
                    updatedAt = nowIso(),
                )
                plannerDao.insertLoan(e)
                loadAll()
                Haptics.success()
                _loanForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _loanForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    // ─── Income ───────────────────────────────────────────────────────────────

    fun openIncomeForm(incomeId: String? = null) {
        viewModelScope.launch {
            val e = incomeId?.let { incomeDao.getById(it) }
            _incomeForm.update {
                if (e == null) {
                    IncomeFormState()
                } else {
                    IncomeFormState(
                    id = e.id, source = e.source ?: "", amount = e.amount.toString(),
                    date = e.date ?: nowIso(), category = "salary", notes = e.note ?: "",
                    isRecurring = e.isRecurring != 0, frequency = e.frequency ?: "once",
                )
                }
            }
        }
    }

    fun saveIncome(onSuccess: () -> Unit) {
        val form = _incomeForm.value
        val amt  = form.amount.toDoubleOrNull()
        if (form.source.isBlank() || amt == null || amt <= 0) {
            _incomeForm.update { it.copy(error = "Source and valid amount required") }
            return
        }
        _incomeForm.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val e = (form.id?.let { incomeDao.getById(it) } ?: IncomeEntity(
                    id = UUID.randomUUID().toString(), source = "", amount = 0.0,
                    date = nowIso(), note = null, isRecurring = 0,
                    createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    source = form.source, amount = amt, date = form.date,
                    note = form.notes.ifBlank { null },
                    isRecurring = if (form.isRecurring) 1 else 0,
                    frequency = if (form.isRecurring) form.frequency else null,
                    updatedAt = nowIso(),
                )
                incomeDao.insert(e)
                loadAll()
                Haptics.success()
                _incomeForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _incomeForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    fun deleteIncome(id: String) = viewModelScope.launch { incomeDao.softDelete(id, nowIso()); loadAll() }
}
