package com.belinze.lifeos.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.ProfileViewModel

// ─────────────────────────────────────────────────────────────────────────────
// PersonalInformationScreen — matches PersonalInformationScreen.tsx
//
// Allows editing name, email, phone, username (username is read-only displayed).
// On save, updates AppPreferences immediately.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PersonalInformationScreen(
    navController: NavHostController,
    viewModel:     ProfileViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()

    PageScaffold(
        eyebrow = "Profile",
        title   = "Personal Information",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // Success / error inline banners
            if (uiState.error != null) {
                InlineBanner(
                    message  = uiState.error!!,
                    tone     = BannerTone.Error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value         = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label         = { Text("Full name") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            OutlinedTextField(
                value         = formState.email,
                onValueChange = { viewModel.updateEmail(it) },
                label         = { Text("Email address") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            OutlinedTextField(
                value         = formState.phone,
                onValueChange = { viewModel.updatePhone(it) },
                label         = { Text("Phone number") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            OutlinedTextField(
                value         = formState.username,
                onValueChange = { viewModel.updateUsername(it) },
                label         = { Text("Username") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // Error display
            if (formState.error != null) {
                Text(formState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(Spacing.sm))

            Button(
                onClick  = { viewModel.saveProfile { navController.popBackStack() } },
                enabled  = !formState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Changes")
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}
