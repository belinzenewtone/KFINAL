package com.belinze.lifeos.ui.navigation

// ─────────────────────────────────────────────────────────────────────────────
// Route — all app routes as string constants.
//
// Matches RootStackParamList in src/navigation/types.ts 1:1.
// Parameterised routes use {param} placeholder — extracted via
// NavBackStackEntry.arguments?.getString("param").
//
// Optional params use ?param={param} with a default of "" (empty string).
// ─────────────────────────────────────────────────────────────────────────────

object Route {
    // ── Auth / onboarding flow ────────────────────────────────────────────────

    /** Shown while DataStore is hydrating. No navigation stack. */
    const val LOADING    = "loading"
    const val ONBOARDING = "onboarding"
    const val AUTH       = "auth"
    const val APP_LOCK   = "app_lock"

    // ── Main tab scaffold (root of authenticated graph) ───────────────────────

    const val MAIN = "main"

    // ── Finance ───────────────────────────────────────────────────────────────

    /** Detail view for a single transaction. Required param: transactionId */
    const val TRANSACTION_DETAIL      = "transaction_detail/{transactionId}"

    /** Create or edit a transaction. Optional param: transactionId */
    const val TRANSACTION_FORM        = "transaction_form?transactionId={transactionId}"
    const val CATEGORIZE              = "categorize"
    const val FEE_ANALYTICS           = "fee_analytics"

    /** Detail for a merchant. Required param: merchant (name, URL-encoded) */
    const val MERCHANT_DETAIL         = "merchant_detail/{merchant}"

    // ── Planner ───────────────────────────────────────────────────────────────

    const val PLANNER                 = "planner"

    const val BUDGETS                 = "budgets"
    const val BUDGET_DETAIL           = "budget_detail/{budgetId}"
    const val BUDGET_FORM             = "budget_form?budgetId={budgetId}"

    const val INCOME                  = "income"
    const val INCOME_FORM             = "income_form?incomeId={incomeId}"

    const val RECURRING               = "recurring"
    const val RECURRING_FORM          = "recurring_form?ruleId={ruleId}"

    const val BILLS                   = "bills"
    const val BILL_FORM               = "bill_form?billId={billId}"

    const val LOANS                   = "loans"
    const val LOAN_FORM               = "loan_form?loanId={loanId}"

    const val GOALS                   = "goals"
    const val GOAL_FORM               = "goal_form?goalId={goalId}"

    const val EXPORT                  = "export"

    /** Alias for EXPORT used by Finance/Profile screens */
    const val EXPORT_DATA             = "export"
    const val CSV_IMPORT              = "csv_import?fileUri={fileUri}&fileName={fileName}"

    /** Alias for CSV_IMPORT used by FinanceScreen chip */
    const val IMPORT_CSV              = "csv_import?fileUri=&fileName="

    /** SMS import sheet */
    const val IMPORT_SMS              = "sms_import"

    /** Uncategorized / pending review queue */
    const val UNCATEGORIZED           = "review_queue"

    // ── Tasks ─────────────────────────────────────────────────────────────────

    const val TASKS                   = "tasks"
    const val TASK_DETAIL             = "task_detail/{taskId}"
    const val TASK_FORM               = "task_form?taskId={taskId}"

    // ── Calendar / Events ─────────────────────────────────────────────────────

    const val EVENTS                  = "events"
    const val EVENT_DETAIL            = "event_detail/{eventId}"

    /**
     * Optional params: eventId, type (event|birthday|anniversary|countdown)
     */
    const val EVENT_FORM              = "event_form?eventId={eventId}&type={type}"

    // ── Analytics / Insights ─────────────────────────────────────────────────

    const val INSIGHTS                = "insights"

    // ── Settings ─────────────────────────────────────────────────────────────

    const val SETTINGS                = "settings"
    const val PERSONAL_INFORMATION    = "personal_information"
    const val REVIEW_QUEUE            = "review_queue"
    const val SMS_IMPORT_HEALTH       = "sms_import_health"
    const val CHANGELOG               = "changelog"
    const val SCREEN_LOCK             = "screen_lock"
    const val NOTIFICATIONS           = "notifications"

    // ── Search ───────────────────────────────────────────────────────────────

    const val SEARCH                  = "search"

    // ── Profile extras ───────────────────────────────────────────────────────

    const val MONTHLY_WRAPPED         = "monthly_wrapped?initialMonthOffset={initialMonthOffset}"
    const val WEEK_REVIEW             = "week_review"
    const val LEARNING                = "learning"

    // ── Convenience aliases (used by screen composables) ─────────────────────

    /** Profile edit → maps to PERSONAL_INFORMATION */
    const val EDIT_PROFILE            = "personal_information"

    /** Security / screen lock → maps to SCREEN_LOCK */
    const val SECURITY                = "screen_lock"
}

// ─────────────────────────────────────────────────────────────────────────────
// Route builder helpers — build concrete route strings for navigation calls
// ─────────────────────────────────────────────────────────────────────────────

object NavTo {
    fun transactionDetail(transactionId: String) =
        "transaction_detail/$transactionId"

    fun transactionForm(transactionId: String? = null) =
        if (transactionId != null) {
            "transaction_form?transactionId=$transactionId"
        } else {
            "transaction_form?transactionId="
        }

    fun merchantDetail(merchant: String) =
        "merchant_detail/${merchant.encodeForRoute()}"

    fun budgetDetail(budgetId: String) = "budget_detail/$budgetId"

    fun budgetForm(budgetId: String? = null) =
        "budget_form?budgetId=${budgetId ?: ""}"

    fun incomeForm(incomeId: String? = null) =
        "income_form?incomeId=${incomeId ?: ""}"

    fun recurringForm(ruleId: String? = null) =
        "recurring_form?ruleId=${ruleId ?: ""}"

    fun billForm(billId: String? = null) =
        "bill_form?billId=${billId ?: ""}"

    fun loanForm(loanId: String? = null) =
        "loan_form?loanId=${loanId ?: ""}"

    fun goalForm(goalId: String? = null) =
        "goal_form?goalId=${goalId ?: ""}"

    fun taskDetail(taskId: String) = "task_detail/$taskId"

    fun taskForm(taskId: String? = null) =
        "task_form?taskId=${taskId ?: ""}"

    fun eventDetail(eventId: String) = "event_detail/$eventId"

    fun eventForm(
        eventId: String? = null,
        type: String? = null,
    ) = "event_form?eventId=${eventId ?: ""}&type=${type ?: ""}"

    fun csvImport(fileUri: String? = null, fileName: String? = null) =
        "csv_import?fileUri=${fileUri?.encodeForRoute() ?: ""}&fileName=${fileName?.encodeForRoute() ?: ""}"

    fun monthlyWrapped(initialMonthOffset: Int? = null) =
        "monthly_wrapped?initialMonthOffset=${initialMonthOffset ?: 0}"

    private fun String.encodeForRoute(): String =
        java.net.URLEncoder.encode(this, "UTF-8")
}
