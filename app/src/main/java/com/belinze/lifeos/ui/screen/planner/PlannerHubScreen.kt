package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing

private data class HubEntry(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val route: String,
)

@Composable
fun PlannerHubScreen(navController: NavHostController) {
    val entries = listOf(
        HubEntry(Icons.Outlined.Wallet,       "Budgets",        Color(0xFF34D399), Route.BUDGETS),
        HubEntry(Icons.Outlined.Payments,     "Income",         Color(0xFF4DB8FF), Route.INCOME),
        HubEntry(Icons.Outlined.Loop,         "Recurring",      Color(0xFF8B5CF6), Route.RECURRING),
        HubEntry(Icons.Outlined.TrendingDown, "Loans & Fuliza", Color(0xFFFF6B6B), Route.LOANS),
        HubEntry(Icons.Outlined.Receipt,      "Bills",          Color(0xFFF59E0B), Route.BILLS),
        HubEntry(Icons.Outlined.Flag,         "Goals",          Color(0xFFEC4899), Route.GOALS),
        HubEntry(Icons.Outlined.Search,       "Search Finance", Color(0xFFA78BFA), Route.SEARCH),
        HubEntry(Icons.Outlined.Download,     "Export",         Color(0xFF14B8A6), Route.EXPORT),
    )

    PageScaffold(
        title = "Finance Hub",
        onBack = { navController.popBackStack() },
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
    GlassCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(entry.color.copy(alpha = 0x20 / 255f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(entry.icon, contentDescription = entry.label, tint = entry.color, modifier = Modifier.size(22.dp))
            }
            Text(
                entry.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
