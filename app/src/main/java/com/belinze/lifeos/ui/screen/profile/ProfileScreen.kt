package com.belinze.lifeos.ui.screen.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.ProfileViewModel
import com.belinze.lifeos.viewmodel.SettingsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// ProfileScreen — 1:1 port of src/screens/profile/ProfileScreen.tsx.
// ─────────────────────────────────────────────────────────────────────────────

private const val USERNAME_MAX = 8
private val WARNING = Color(0xFFF5CB5C)

private data class ToolItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
)

private val TOOL_HUB = listOf(
    ToolItem("Analytics", Icons.Outlined.Analytics,      Color(0xFF2DD4BF), Route.INSIGHTS),
    ToolItem("Review",    Icons.Outlined.Explore,        Color(0xFFA78BFA), Route.WEEK_REVIEW),
    ToolItem("Search",    Icons.Outlined.Search,         Color(0xFF60A5FA), Route.SEARCH),
    ToolItem("Recurring", Icons.Outlined.Repeat,         Color(0xFF34D399), Route.RECURRING),
    ToolItem("Export",    Icons.Outlined.Download,       Color(0xFFFBBF24), Route.EXPORT_DATA),
    ToolItem("Hub",       Icons.Outlined.LibraryBooks,   Color(0xFF22D3EE), Route.PLANNER),
    ToolItem("Wrapped",   Icons.Outlined.CardGiftcard,   Color(0xFFF472B6), Route.MONTHLY_WRAPPED),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController:      NavHostController,
    viewModel:          ProfileViewModel  = hiltViewModel(),
    settingsViewModel:  SettingsViewModel = hiltViewModel(),
) {
    val prefState by viewModel.prefState.collectAsStateWithLifecycle()
    val context    = LocalContext.current

    var successMessage by remember { mutableStateOf<String?>(null) }
    var editVisible by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(prefState.profileName) }
    var editUsername by remember { mutableStateOf(prefState.profileUsername) }
    var photoSheetVisible by remember { mutableStateOf(false) }
    var photoViewerVisible by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { settingsViewModel.setProfileAvatarUri(it.toString()) }
    }

    val avatarBitmap: Bitmap? by produceState<Bitmap?>(
        initialValue = null,
        key1 = prefState.profileAvatarUri,
    ) {
        value = if (prefState.profileAvatarUri.isNotEmpty()) {
            runCatching {
                context.contentResolver
                    .openInputStream(Uri.parse(prefState.profileAvatarUri))
                    ?.use { stream -> BitmapFactory.decodeStream(stream) }
            }.getOrNull()
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Text(
                text  = "Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // ── Hero glass card ────────────────────────────────────────────
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                ) {
                    // Avatar ring (84 total, 2 border, 3 padding, 74 inner)
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(3.dp)
                            .clickable { photoSheetVisible = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                val initials = prefState.profileName
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .take(2)
                                    .joinToString("")
                                    .ifEmpty { "?" }
                                Text(
                                    text     = initials,
                                    style    = MaterialTheme.typography.headlineSmall,
                                    color    = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text     = prefState.profileName.trim().ifEmpty { "Set up your profile" },
                            style    = MaterialTheme.typography.titleLarge,
                            color    = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text     = if (prefState.profileUsername.isNotBlank()) {
                                "@${prefState.profileUsername}"
                            } else {
                                "No username set"
                            },
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
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
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(9999.dp),
                                    )
                                    .padding(horizontal = Spacing.sm, vertical = 4.dp),
                            ) {
                                Text(
                                    text  = "Member since $memberSince",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.base))

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(
                        onClick = {
                            editName = prefState.profileName
                            editUsername = prefState.profileUsername
                            editVisible = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp)),
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Edit Profile", color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedButton(
                        onClick = { navController.navigate(Route.SETTINGS) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp)),
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Settings", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ── Tool hub glass card ─────────────────────────────────────────
            GlassCard {
                Text(
                    text = "TOOL HUB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.base),
                )
                val rows = TOOL_HUB.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    rows.forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            rowItems.forEach { item ->
                                ToolHubCard(
                                    item = item,
                                    onClick = { navController.navigate(item.route) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Bottom safe area so FloatingTabBar doesn't cover the last card
            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }

    // ── Edit profile modal ────────────────────────────────────────────────────
    if (editVisible) {
        ModalBottomSheet(
            onDismissRequest = { editVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                Text(
                    text  = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Column {
                    Text(
                        text = "Full name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = { Text("Full name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Username",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${editUsername.length}/$USERNAME_MAX",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (editUsername.length >= USERNAME_MAX) {
                                WARNING
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = {
                            editUsername = it.lowercase()
                                .replace(Regex("[^a-z0-9_]"), "")
                                .take(USERNAME_MAX)
                        },
                        placeholder = { Text("e.g. john") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Shown in the app greeting · letters, numbers, _ only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.saveNameAndUsername(editName, editUsername) {
                            editVisible = false
                            successMessage = "Profile updated"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save")
                }
                TextButton(
                    onClick = { editVisible = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // ── Photo sheet modal ──────────────────────────────────────────────────────
    if (photoSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { photoSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                Text(
                    text  = "Profile Photo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (prefState.profileAvatarUri.isNotEmpty()) {
                    PhotoSheetOption(
                        icon = Icons.Outlined.Visibility,
                        label = "View",
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            photoSheetVisible = false
                            photoViewerVisible = true
                        },
                    )
                }
                PhotoSheetOption(
                    icon = Icons.Outlined.Image,
                    label = "Choose from gallery",
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        photoSheetVisible = false
                        imagePickerLauncher.launch("image/*")
                    },
                )
                if (prefState.profileAvatarUri.isNotEmpty()) {
                    PhotoSheetOption(
                        icon = Icons.Outlined.Delete,
                        label = "Remove",
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            photoSheetVisible = false
                            viewModel.removeProfilePhoto {
                                successMessage = "Profile photo removed"
                            }
                        },
                    )
                }
                TextButton(
                    onClick = { photoSheetVisible = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // ── Full-screen photo viewer ───────────────────────────────────────────────
    if (photoViewerVisible && prefState.profileAvatarUri.isNotEmpty()) {
        Dialog(
            onDismissRequest = { photoViewerVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { photoViewerVisible = false },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                avatarBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.7f),
                    )
                }
            }
        }
    }

    // Banner overlaid so it doesn't shift scrollable content — shown regardless of photo viewer state
    TopBanner(
        visible       = successMessage != null,
        message       = successMessage ?: "",
        tone          = BannerTone.Success,
        onDismiss     = { successMessage = null },
        autoDismissMs = 3000,
    )
    } // end Box
}

@Composable
private fun ToolHubCard(
    item: ToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(item.color.copy(alpha = 0x14 / 255f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = item.color.copy(alpha = 0x44 / 255f)),
                onClick = onClick,
            )
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(item.color.copy(alpha = 0x28 / 255f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = item.color,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun PhotoSheetOption(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = color.copy(alpha = 0.2f)),
                onClick = onClick,
            )
            .padding(vertical = Spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
