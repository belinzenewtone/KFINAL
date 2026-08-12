package com.belinze.lifeos.ui.screen.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.viewmodel.ProfileViewModel
import com.belinze.lifeos.viewmodel.SettingsViewModel

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
    navController:   NavHostController,
    viewModel:       ProfileViewModel  = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefState by viewModel.prefState.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()
    val stats      = uiState.stats
    val context    = LocalContext.current

    // Image picker — updates profileAvatarUri in DataStore on selection
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { settingsViewModel.setProfileAvatarUri(it.toString()) }
    }

    // Decode avatar bitmap off the main thread whenever the URI changes
    val avatarBitmap: Bitmap? by produceState<Bitmap?>(
        initialValue = null,
        key1         = prefState.profileAvatarUri,
    ) {
        value = if (prefState.profileAvatarUri.isNotEmpty()) {
            runCatching {
                context.contentResolver
                    .openInputStream(Uri.parse(prefState.profileAvatarUri))
                    ?.use { stream -> BitmapFactory.decodeStream(stream) }
            }.getOrNull()
        } else null
    }

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
            // Avatar circle — tap to pick a photo from the gallery
            Box(
                modifier         = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.15f))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap             = avatarBitmap!!.asImageBitmap(),
                        contentDescription = "Profile photo",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                } else {
                    val initials = prefState.profileName
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "?" }
                    Text(initials, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
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

            // ── Member since badge ────────────────────────────────────────────
            val memberSince = remember(prefState.profileCreatedAt) {
                runCatching {
                    val d = LocalDate.parse(prefState.profileCreatedAt)
                    DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()).format(d)
                }.getOrNull()
            }
            if (memberSince != null) {
                Box(
                    modifier = Modifier
                        .background(
                            color  = MaterialTheme.colorScheme.surfaceVariant,
                            shape  = MaterialTheme.shapes.extraLarge,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = "Member since $memberSince",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    value = "${if (stats.momChangePct >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.0f", stats.momChangePct)}%",
                    valueColor = when {
                        stats.momChangePct > 0  -> Color(0xFFEF4444)  // spent more → red
                        stats.momChangePct < 0  -> Color(0xFF10B981)  // spent less → green
                        else                    -> MaterialTheme.colorScheme.onBackground
                    },
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Tool Hub — 3-column icon-tile grid matching RN ────────────────────
        SectionHeader(label = "Tool Hub")
        Spacer(Modifier.height(Spacing.sm))

        FrostCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            // Tool tiles: 3 per row
            val toolTiles = listOf(
                Triple("Analytics", Icons.Filled.Analytics,        Color(0xFF6366F1)) to Route.INSIGHTS,
                Triple("Review",    Icons.Filled.RateReview,       Color(0xFFF59E0B)) to Route.UNCATEGORIZED,
                Triple("Search",    Icons.Filled.Search,           Color(0xFF10B981)) to Route.CATEGORIZE,
                Triple("Recurring", Icons.Filled.AutoAwesomeMotion,Color(0xFF8B5CF6)) to Route.RECURRING,
                Triple("Export",    Icons.Filled.FileUpload,       Color(0xFF3B82F6)) to Route.EXPORT_DATA,
                Triple("Budgets",   Icons.Filled.Wallet,           Color(0xFFEF4444)) to Route.BUDGETS,
                Triple("Wrapped",   Icons.Filled.Stars,            Color(0xFFEC4899)) to Route.INSIGHTS,
            )
            val rows = toolTiles.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        rowItems.forEach { (tile, route) ->
                            ToolHubTile(
                                label   = tile.first,
                                icon    = tile.second,
                                color   = tile.third,
                                onClick = { navController.navigate(route) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Fill remaining cells so rows of < 3 stay aligned
                        repeat(3 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Account links ─────────────────────────────────────────────────────
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
        ProfileLink(
            icon    = Icons.Filled.DataObject,
            label   = "Export Data",
            onClick = { navController.navigate(Route.EXPORT_DATA) },
        )

        Spacer(Modifier.height(Spacing.bottomNavSafeArea))
    }
}

// ─── Tool Hub tile ────────────────────────────────────────────────────────────

@Composable
private fun ToolHubTile(
    label:    String,
    icon:     ImageVector,
    color:    Color,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = color.copy(0.20f)),
                onClick           = onClick,
            )
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.15f), MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = color,
                modifier           = Modifier.size(22.dp),
            )
        }
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onBackground.copy(0.75f),
            maxLines  = 1,
        )
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
