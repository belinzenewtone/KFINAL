package com.belinze.lifeos.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.viewmodel.ProfileViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ProfileScreen
//
// 1:1 port of src/screens/profile/ProfileScreen.tsx.
//
// Layout:
//   ‣ Avatar + name + email + phone
//   ‣ Stats card: total transactions + month spend + mom change
//   ‣ Quick links: Edit Profile / Settings / Export / Security / Insights
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel:     ProfileViewModel = hiltViewModel(),
) {
    val prefState by viewModel.prefState.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()
    val stats      = uiState.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Page header ───────────────────────────────────────────────────────
        Text(
            text     = "Profile",
            style    = MaterialTheme.typography.headlineSmall,
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        )

        // ── Avatar + name + email ─────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // Avatar circle
            Box(
                modifier         = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                val initials = prefState.profileName
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "?" }
                Text(initials, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text       = prefState.profileName.ifEmpty { "Your Name" },
                style      = MaterialTheme.typography.titleLarge,
                color      = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            if (prefState.profileEmail.isNotBlank()) {
                Text(
                    text  = prefState.profileEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                )
            }
            if (prefState.profilePhone.isNotBlank()) {
                Text(
                    text  = prefState.profilePhone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // ── Stats card ────────────────────────────────────────────────────────
        FrostCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    label = "Transactions",
                    value = "${stats.totalTxCount}",
                )
                StatItem(
                    label = "This month",
                    value = compactCurrency(stats.thisMonthSpend),
                )
                StatItem(
                    label = "vs Last month",
                    value = "${if (stats.momChangePct >= 0) "+" else ""}${String.format("%.0f", stats.momChangePct)}%",
                    valueColor = when {
                        stats.momChangePct > 0  -> Color(0xFFEF4444)  // spent more → red
                        stats.momChangePct < 0  -> Color(0xFF10B981)  // spent less → green
                        else                    -> MaterialTheme.colorScheme.onBackground
                    },
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Quick links ───────────────────────────────────────────────────────
        SectionHeader(label = "Account")

        ProfileLink(
            icon    = Icons.Filled.Edit,
            label   = "Edit Profile",
            onClick = { navController.navigate(Route.EDIT_PROFILE) },
        )
        ProfileLink(
            icon    = Icons.Filled.Settings,
            label   = "Settings",
            onClick = { navController.navigate(Route.SETTINGS) },
        )
        ProfileLink(
            icon    = Icons.Filled.Lock,
            label   = "Security",
            onClick = { navController.navigate(Route.SECURITY) },
        )

        SectionHeader(label = "Data", modifier = Modifier.padding(top = Spacing.sm))

        ProfileLink(
            icon    = Icons.Filled.Analytics,
            label   = "Insights & Analytics",
            onClick = { navController.navigate(Route.INSIGHTS) },
        )
        ProfileLink(
            icon    = Icons.Filled.Wallet,
            label   = "Budgets & Planner",
            onClick = { navController.navigate(Route.BUDGETS) },
        )
        ProfileLink(
            icon    = Icons.Filled.DataObject,
            label   = "Export Data",
            onClick = { navController.navigate(Route.EXPORT_DATA) },
        )

        Spacer(Modifier.height(Spacing.bottomNavSafeArea))
    }
}

// ─── Stat item ────────────────────────────────────────────────────────────────

@Composable
private fun StatItem(
    label:      String,
    value:      String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = valueColor,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
        )
    }
}

// ─── Profile link ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileLink(
    icon:     ImageVector,
    label:    String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primary           = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(0.12f)),
                onClick           = onClick,
            )
            .padding(horizontal = Spacing.screenHorizontal, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .background(primary.copy(0.10f), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = primary, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(0.35f),
        )
    }
}
