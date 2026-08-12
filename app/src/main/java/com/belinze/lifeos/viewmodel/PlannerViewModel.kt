package com.belinze.lifeos.viewmodel

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
import com.belinze.lifeos.util.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// PlannerViewModel
//
// Covers the Planner hub — recurring rules, bills, goals, Fuliza loans, and
// income. Mirrors usePlannerStore / individual stores in RN.
// ─────────────────────────────────────────────────────────────────────────────

enum class PlannerTab { Recurring, Bills, Goals, Loans, Income }

data class PlannerUiState(
    val isLoading:      Boolean                  = true,
    val activeTab:      PlannerTab               = PlannerTab.Recurring,

    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val bills:          List<BillEntity>          = emptyList(),
    val goals:          List<GoalEntity>          = emptyList(),
    val loans:          List<FulizaLoanEntity>    = emptyList(),
    val income:         List<IncomeEntity>        = emptyList(),
    val exports:        List<ExportEntity>        = emptyList(),

    val totalMonthlyBills: Double                 = 0.0,
    val totalActiveLoans:  Double                 = 0.0,

    val error: String? = null,
)

// ─── Individual form states ───────────────────────────────────────────────────

data class RecurringFormState(
    val id:          String? = null,
    val name:        String  = "",
    val amount:      String  = "",
    val category:    String  = "uncategorized",
    val frequency:   String  = "monthly",  // daily | weekly | monthly | yearly
    val nextRunAt:   String  = nowIso(),
    val notes:       String  = "",
    val enabled:     Boolean = true,
    val isSaving:    Boolean = false,
    val error:       String? = null,
)

data class BillFormState(
    val id:          String?  = null,
    val name:        String   = "",
    val amount:      String   = "",
    val category:    String   = "bills",
    val frequency:   String   = "monthly",
    val nextDueDate: String   = nowIso(),
    val notes:       String   = "",
    val isActive:    Boolean  = true,
    val isSaving:    Boolean  = false,
    val error:       String?  = null,
)

data class GoalFormState(
    val id:          String?  = null,
    val name:        String   = "",
    val targetAmount: String  = "",
    val savedAmount: String   = "0",
    val deadline:    String?  = null,
    val category:    String   = "savings",
    val notes:       String   = "",
    val isSaving:    Boolean  = false,
    val error:       String?  = null,
)

data class LoanFormState(
    val id:            String?  = null,
    val drawCode:      String   = "",
    val drawAmountKes: String   = "",
    val drawDate:      String   = nowIso(),
    val notes:         String   = "",
    val isSaving:      Boolean  = false,
    val error:         String?  = null,
)

data class IncomeFormState(
    val id:       String?  = null,
    val source:   String   = "",
    val amount:   String   = "",
    val date:     String   = nowIso(),
    val category: String   = "salary",
    val notes:    String   = "",
    val isSaving: Boolean  = false,
    val error:    String?  = null,
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
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
                        recurringRules    = rules,
                        bills             = bills,
                        goals             = goals,
                        loans             = loans,
                        income            = income,
                        exports           = exports,
                        totalMonthlyBills = bills.filter { b -> b.isActive }.sumOf { b -> b.amount },
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
                if (e == null) RecurringFormState()
                else RecurringFormState(
                    id = e.id, name = e.name, amount = e.amount.toString(),
                    category = e.category, frequency = e.frequency,
                    nextRunAt = e.nextRunAt, notes = e.notes ?: "", enabled = e.enabled,
                )
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
                    id = UUID.randomUUID().toString(), name = "", amount = 0.0,
                    category = "uncategorized", frequency = "monthly",
                    nextRunAt = nowIso(), enabled = true, createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    name = form.name, amount = amt, category = form.category,
                    frequency = form.frequency, nextRunAt = form.nextRunAt,
                    notes = form.notes.ifBlank { null }, enabled = form.enabled, updatedAt = nowIso(),
                )
                plannerDao.insertRule(e)
                loadAll()
                _recurringForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _recurringForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    fun deleteRule(id: String) = viewModelScope.launch { plannerDao.softDeleteRule(id, nowIso()); loadAll() }

    // ─── Bills ────────────────────────────────────────────────────────────────

    fun openBillForm(billId: String? = null) {
        viewModelScope.launch {
            val e = billId?.let { plannerDao.getBillById(it) }
            _billForm.update {
                if (e == null) BillFormState()
                else BillFormState(
                    id = e.id, name = e.name, amount = e.amount.toString(),
                    category = e.category, frequency = e.frequency,
                    nextDueDate = e.nextDueDate, notes = e.notes ?: "", isActive = e.isActive,
                )
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
                    id = UUID.randomUUID().toString(), name = "", amount = 0.0,
                    category = "bills", frequency = "monthly",
                    nextDueDate = nowIso(), isActive = true, createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    name = form.name, amount = amt, category = form.category,
                    frequency = form.frequency, nextDueDate = form.nextDueDate,
                    notes = form.notes.ifBlank { null }, isActive = form.isActive, updatedAt = nowIso(),
                )
                plannerDao.insertBill(e)
                loadAll()
                _billForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _billForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    fun deleteBill(id: String) = viewModelScope.launch { plannerDao.softDeleteBill(id, nowIso()); loadAll() }

    // ─── Goals ────────────────────────────────────────────────────────────────

    fun openGoalForm(goalId: String? = null) {
        viewModelScope.launch {
            val e = goalId?.let { plannerDao.getGoalById(it) }
            _goalForm.update {
                if (e == null) GoalFormState()
                else GoalFormState(
                    id = e.id, name = e.name, targetAmount = e.targetAmount.toString(),
                    savedAmount = e.savedAmount.toString(), deadline = e.deadline,
                    category = e.category, notes = e.notes ?: "",
                )
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
                    id = UUID.randomUUID().toString(), name = "", targetAmount = 0.0,
                    savedAmount = 0.0, category = "savings", createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    name = form.name, targetAmount = target, savedAmount = saved,
                    deadline = form.deadline, category = form.category,
                    notes = form.notes.ifBlank { null }, updatedAt = nowIso(),
                )
                plannerDao.insertGoal(e)
                loadAll()
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
            plannerDao.updateGoal(e.copy(savedAmount = e.savedAmount + amount, updatedAt = nowIso()))
            loadAll()
        }
    }

    // ─── Loans ────────────────────────────────────────────────────────────────

    fun openLoanForm(loanId: String? = null) {
        viewModelScope.launch {
            val e = loanId?.let { plannerDao.getLoanById(it) }
            _loanForm.update {
                if (e == null) LoanFormState()
                else LoanFormState(
                    id = e.id, drawCode = e.drawCode,
                    drawAmountKes = e.drawAmountKes.toString(), drawDate = e.drawDate,
                    notes = e.notes ?: "",
                )
            }
        }
    }

    fun deleteLoan(id: String) = viewModelScope.launch { plannerDao.hardDeleteLoan(id); loadAll() }

    // ─── Income ───────────────────────────────────────────────────────────────

    fun openIncomeForm(incomeId: String? = null) {
        viewModelScope.launch {
            val e = incomeId?.let { incomeDao.getById(it) }
            _incomeForm.update {
                if (e == null) IncomeFormState()
                else IncomeFormState(
                    id = e.id, source = e.source, amount = e.amount.toString(),
                    date = e.date, category = e.category, notes = e.notes ?: "",
                )
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
                    date = nowIso(), category = "salary", createdAt = nowIso(), updatedAt = nowIso(),
                )).copy(
                    source = form.source, amount = amt, date = form.date,
                    category = form.category, notes = form.notes.ifBlank { null }, updatedAt = nowIso(),
                )
                incomeDao.insert(e)
                loadAll()
                _incomeForm.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (ex: Exception) {
                _incomeForm.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }

    fun deleteIncome(id: String) = viewModelScope.launch { incomeDao.softDelete(id, nowIso()); loadAll() }
}
