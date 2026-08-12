package com.belinze.lifeos.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.ProfileViewModel

// ─────────────────────────────────────────────────────────────────────────────
// PersonalInformationScreen — 1:1 port of
// src/screens/profile/PersonalInformationScreen.tsx.
// ─────────────────────────────────────────────────────────────────────────────

private enum class InfoField { Name, Email, Username }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInformationScreen(
    navController: NavHostController,
    viewModel:     ProfileViewModel = hiltViewModel(),
) {
    val prefState by viewModel.prefState.collectAsState()
    var editing by remember { mutableStateOf<InfoField?>(null) }
    var editValue by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf<String?>(null) }

    val displayUsername = prefState.profileUsername.ifBlank {
        prefState.profileName.split(" ").firstOrNull() ?: ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TopBanner(
            visible       = successMsg != null,
            message       = successMsg ?: "",
            tone          = BannerTone.Success,
            onDismiss     = { successMsg = null },
            autoDismissMs = 2000,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Spacer(Modifier.width(40.dp))
            }

            GlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "Name",
                    value = prefState.profileName,
                    placeholder = "Your name",
                    onClick = {
                        editValue = prefState.profileName
                        editing = InfoField.Name
                    },
                )
                InfoRow(
                    icon = Icons.Filled.Email,
                    label = "Email",
                    value = prefState.profileEmail,
                    placeholder = "your@email.com",
                    onClick = {
                        editValue = prefState.profileEmail
                        editing = InfoField.Email
                    },
                )
                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "Username",
                    value = displayUsername,
                    placeholder = "Username",
                    isLast = true,
                    onClick = {
                        editValue = displayUsername
                        editing = InfoField.Username
                    },
                )
            }
        }
    }

    if (editing != null) {
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            val field = editing!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                Text(
                    text  = "Edit ${field.name}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { value ->
                        editValue = if (field == InfoField.Username) {
                            value.lowercase().replace(Regex("[^a-z0-9_]"), "").take(8)
                        } else value
                    },
                    placeholder = { Text(if (field == InfoField.Email) "your@email.com" else field.name) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (field == InfoField.Email) KeyboardType.Email else KeyboardType.Text,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        when (field) {
                            InfoField.Name -> {
                                viewModel.saveNameAndUsername(editValue, prefState.profileUsername) {
                                    successMsg = "Name updated"
                                    editing = null
                                }
                            }
                            InfoField.Email -> {
                                viewModel.saveEmail(editValue.trim().ifEmpty { null }) {
                                    successMsg = "Email updated"
                                    editing = null
                                }
                            }
                            InfoField.Username -> {
                                viewModel.saveNameAndUsername(prefState.profileName, editValue) {
                                    successMsg = "Username updated"
                                    editing = null
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save")
                }
                TextButton(
                    onClick = { editing = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    isLast: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isLast) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp),
                ) else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                onClick = onClick,
            )
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text     = value.ifBlank { placeholder },
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Icon(
            Icons.Filled.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}
