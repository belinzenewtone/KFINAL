package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// PlannerHubScreen — matches PlannerHubScreen.tsx "Finance Hub"
//
// 8 navigation cards (Budgets, Income, Recurring, Loans, Bills, Goals,
// Search Finance, Export)
// ─────────────────────────────────────────────────────────────────────────────

private data class HubEntry(
    val icon:  ImageVector,
    val label: String,
    val desc:  String,
    val route: String,
)

@Composable
fun PlannerHubScreen(navController: NavHostController) {
    val entries = remember {
        listOf(
            HubEntry(Icons.Filled.BarChart,       "Budgets",        "Spending guardrails per category", Route.BUDGETS),
            HubEntry(Icons.Filled.AttachMoney,     "Income",         "Salary, freelance, and other income", Route.INCOME),
            HubEntry(Icons.Filled.Loop,            "Recurring",      "Subscriptions and standing expenses", Route.RECURRING),
            HubEntry(Icons.Filled.AccountBalance,  "Loans & Fuliza", "Loans drawn down and repayments", Route.LOANS),
            HubEntry(Icons.Filled.Receipt,         "Bills",          "One-off bills and due dates", Route.BILLS),
            HubEntry(Icons.Filled.Savings,         "Goals",          "Savings targets and progress", Route.GOALS),
            HubEntry(Icons.Filled.Analytics,       "Insights",       "Trends, categories, and merchants", Route.INSIGHTS),
            HubEntry(Icons.Filled.FileUpload,      "Export Data",    "Download your data as CSV", Route.EXPORT_DATA),
        )
    }

    PageScaffold(
        eyebrow = "Finance",
        title   = "Finance Hub",
        onBack  = { navController.popBackStack() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            entries.forEach { entry ->
                HubCard(entry = entry, onClick = { navController.navigate(entry.route) })
            }
            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun HubCard(entry: HubEntry, onClick: () -> Unit) {
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(interactionSource = interactionSource,
                indication = ripple(color = primary.copy(0.12f)),
                onClick    = onClick)
            .padding(Spacing.lg),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .background(primary.copy(0.12f), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(entry.icon, contentDescription = entry.label, tint = primary,
                    modifier = Modifier.size(20.dp))
            }
            Column {
                Text(entry.label, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(entry.desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(0.35f))
    }
}
