package com.belinze.lifeos.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.belinze.lifeos.ui.scaffold.MainScaffold
import com.belinze.lifeos.ui.screen.calendar.EventDetailScreen
import com.belinze.lifeos.ui.screen.calendar.EventFormScreen
import com.belinze.lifeos.ui.screen.finance.ReviewQueueScreen
import com.belinze.lifeos.ui.screen.finance.TransactionDetailScreen
import com.belinze.lifeos.ui.screen.finance.TransactionFormScreen
import com.belinze.lifeos.ui.screen.insights.InsightsScreen
import com.belinze.lifeos.ui.screen.insights.MonthlyWrappedScreen
import com.belinze.lifeos.ui.screen.placeholder.PlaceholderScreen
import com.belinze.lifeos.ui.screen.planner.BudgetFormScreen
import com.belinze.lifeos.ui.screen.planner.BudgetsScreen
import com.belinze.lifeos.ui.screen.planner.PlannerHubScreen
import com.belinze.lifeos.ui.screen.profile.PersonalInformationScreen
import com.belinze.lifeos.ui.screen.review.WeekReviewScreen
import com.belinze.lifeos.ui.screen.search.SearchScreen
import com.belinze.lifeos.ui.screen.settings.NotificationsScreen
import com.belinze.lifeos.ui.screen.settings.ScreenLockScreen
import com.belinze.lifeos.ui.screen.settings.SettingsScreen
import com.belinze.lifeos.ui.screen.tasks.TaskDetailScreen
import com.belinze.lifeos.ui.screen.tasks.TaskFormScreen
import com.belinze.lifeos.ui.screen.tasks.TasksScreen
import com.belinze.lifeos.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────────────────────
// MainNavHost — authenticated navigation graph.
//
// Mirrors the authenticated Stack.Navigator in AppNavigator.tsx:
//   ‣ Root: Main (tab scaffold with FloatingTabBar)
//   ‣ All detail/form screens slide in from the right in 100ms
//   ‣ Pop = slide back to left in 100ms
// ─────────────────────────────────────────────────────────────────────────────

// Slide transition matching RN: animation='slide_from_right', animationDuration=100
private val slideEnter = slideInHorizontally(tween(100)) { it } + fadeIn(tween(100))
private val slideExit  = slideOutHorizontally(tween(100)) { -it } + fadeOut(tween(100))
private val popEnter   = slideInHorizontally(tween(100)) { -it } + fadeIn(tween(100))
private val popExit    = slideOutHorizontally(tween(100)) { it } + fadeOut(tween(100))

@Composable
fun MainNavHost(
    appViewModel: AppViewModel,
    modifier:     Modifier            = Modifier,
    navController: NavHostController  = rememberNavController(),
) {
    NavHost(
        navController        = navController,
        startDestination     = Route.MAIN,
        modifier             = modifier,
        enterTransition      = { slideEnter },
        exitTransition       = { slideExit },
        popEnterTransition   = { popEnter },
        popExitTransition    = { popExit },
    ) {

        // ── Main tab scaffold ─────────────────────────────────────────────────
        composable(Route.MAIN) {
            MainScaffold(
                navController = navController,
                appViewModel  = appViewModel,
            )
        }

        // ── Finance ───────────────────────────────────────────────────────────
        composable(
            route     = Route.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) { back ->
            val id = back.arguments?.getString("transactionId") ?: ""
            TransactionDetailScreen(transactionId = id, navController = navController)
        }

        composable(
            route     = Route.TRANSACTION_FORM,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("transactionId")
            TransactionFormScreen(transactionId = id?.ifEmpty { null }, navController = navController)
        }

        composable(Route.CATEGORIZE) {
            com.belinze.lifeos.ui.screen.finance.CategorizeScreen(navController = navController)
        }

        composable(Route.FEE_ANALYTICS) {
            com.belinze.lifeos.ui.screen.finance.FeeAnalyticsScreen(navController = navController)
        }

        composable(
            route     = Route.MERCHANT_DETAIL,
            arguments = listOf(navArgument("merchant") { type = NavType.StringType }),
        ) { back ->
            val merchant = back.arguments?.getString("merchant")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            } ?: ""
            com.belinze.lifeos.ui.screen.finance.MerchantDetailScreen(merchant = merchant, navController = navController)
        }

        // ── Planner ───────────────────────────────────────────────────────────
        composable(Route.PLANNER) {
            PlannerHubScreen(navController = navController)
        }

        composable(Route.BUDGETS) {
            BudgetsScreen(navController = navController)
        }

        composable(
            route     = Route.BUDGET_DETAIL,
            arguments = listOf(navArgument("budgetId") { type = NavType.StringType }),
        ) { back ->
            val id = back.arguments?.getString("budgetId") ?: ""
            com.belinze.lifeos.ui.screen.planner.BudgetDetailScreen(budgetId = id, navController = navController)
        }

        composable(
            route     = Route.BUDGET_FORM,
            arguments = listOf(navArgument("budgetId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("budgetId")
            BudgetFormScreen(budgetId = id?.ifEmpty { null }, navController = navController)
        }

        composable(Route.INCOME) {
            com.belinze.lifeos.ui.screen.planner.IncomeScreen(navController = navController)
        }

        composable(
            route     = Route.INCOME_FORM,
            arguments = listOf(navArgument("incomeId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("incomeId")
            com.belinze.lifeos.ui.screen.planner.IncomeFormScreen(incomeId = id?.ifEmpty { null }, navController = navController)
        }

        composable(Route.RECURRING) {
            com.belinze.lifeos.ui.screen.planner.RecurringScreen(navController = navController)
        }

        composable(
            route     = Route.RECURRING_FORM,
            arguments = listOf(navArgument("ruleId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("ruleId")
            com.belinze.lifeos.ui.screen.planner.RecurringFormScreen(ruleId = id?.ifEmpty { null }, navController = navController)
        }

        composable(Route.BILLS) {
            com.belinze.lifeos.ui.screen.planner.BillsScreen(navController = navController)
        }

        composable(
            route     = Route.BILL_FORM,
            arguments = listOf(navArgument("billId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("billId")
            com.belinze.lifeos.ui.screen.planner.BillFormScreen(billId = id?.ifEmpty { null }, navController = navController)
        }

        composable(Route.LOANS) {
            com.belinze.lifeos.ui.screen.planner.LoansScreen(navController = navController)
        }

        composable(
            route     = Route.LOAN_FORM,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("loanId")
            com.belinze.lifeos.ui.screen.planner.LoanFormScreen(loanId = id?.ifEmpty { null }, navController = navController)
        }

        composable(Route.GOALS) {
            com.belinze.lifeos.ui.screen.planner.GoalsScreen(navController = navController)
        }

        composable(
            route     = Route.GOAL_FORM,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("goalId")
            com.belinze.lifeos.ui.screen.planner.GoalFormScreen(goalId = id?.ifEmpty { null }, navController = navController)
        }

        composable(Route.EXPORT) {
            com.belinze.lifeos.ui.screen.planner.ExportScreen(navController = navController)
        }

        // sms_import
        composable(Route.IMPORT_SMS) {
            com.belinze.lifeos.ui.screen.finance.ImportSmsScreen(navController = navController)
        }

        composable(
            route     = Route.CSV_IMPORT,
            arguments = listOf(
                navArgument("fileUri")  { type = NavType.StringType; defaultValue = "" },
                navArgument("fileName") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            com.belinze.lifeos.ui.screen.planner.CsvImportScreen(navController = navController)
        }

        // Review queue (uncategorized)
        composable(Route.REVIEW_QUEUE) {
            ReviewQueueScreen(navController = navController)
        }

        // ── Tasks ─────────────────────────────────────────────────────────────
        composable(Route.TASKS) {
            TasksScreen(navController = navController)
        }

        composable(
            route     = Route.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { back ->
            val id = back.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(taskId = id, navController = navController)
        }

        composable(
            route     = Route.TASK_FORM,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType; defaultValue = "" }),
        ) { back ->
            val id = back.arguments?.getString("taskId")
            TaskFormScreen(taskId = id?.ifEmpty { null }, navController = navController)
        }

        // ── Events ────────────────────────────────────────────────────────────
        composable(Route.EVENTS) {
            com.belinze.lifeos.ui.screen.calendar.EventsScreen(navController = navController)
        }

        composable(
            route     = Route.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) { back ->
            val id = back.arguments?.getString("eventId") ?: ""
            EventDetailScreen(eventId = id, navController = navController)
        }

        composable(
            route     = Route.EVENT_FORM,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType; defaultValue = "" },
                navArgument("type")    { type = NavType.StringType; defaultValue = "event" },
            ),
        ) { back ->
            val id   = back.arguments?.getString("eventId")
            val type = back.arguments?.getString("type") ?: "event"
            EventFormScreen(
                eventId       = id?.ifEmpty { null },
                type          = type.ifEmpty { "event" },
                navController = navController,
            )
        }

        // ── Analytics ─────────────────────────────────────────────────────────
        composable(Route.INSIGHTS) {
            InsightsScreen(navController = navController)
        }

        // ── Settings ─────────────────────────────────────────────────────────
        composable(Route.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        composable(Route.PERSONAL_INFORMATION) {
            PersonalInformationScreen(navController = navController)
        }

        composable(Route.SMS_IMPORT_HEALTH) {
            com.belinze.lifeos.ui.screen.settings.SmsImportHealthScreen(navController = navController)
        }

        composable(Route.CHANGELOG) {
            com.belinze.lifeos.ui.screen.settings.ChangelogScreen(navController = navController)
        }

        composable(Route.SCREEN_LOCK) {
            ScreenLockScreen(navController = navController)
        }

        composable(Route.NOTIFICATIONS) {
            NotificationsScreen(navController = navController)
        }

        // ── Search ────────────────────────────────────────────────────────────
        composable(Route.SEARCH) {
            SearchScreen(navController = navController)
        }

        // ── Profile extras ────────────────────────────────────────────────────
        composable(
            route     = Route.MONTHLY_WRAPPED,
            arguments = listOf(navArgument("initialMonthOffset") { type = NavType.IntType; defaultValue = 0 }),
        ) { back ->
            val offset = back.arguments?.getInt("initialMonthOffset") ?: 0
            MonthlyWrappedScreen(initialMonthOffset = offset, navController = navController)
        }

        composable(Route.WEEK_REVIEW) {
            WeekReviewScreen(navController = navController)
        }

        composable(Route.LEARNING) {
            com.belinze.lifeos.ui.screen.learning.LearningScreen(navController = navController)
        }
    }
}
