package com.belinze.lifeos.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.GlassCardVariant
import com.belinze.lifeos.ui.components.HeroSurface
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.ShapeLg
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────────────────────
// AuthScreen — 1:1 port of src/screens/auth/AuthScreen.tsx
//
// Hero (eyebrow "Welcome", title "Your PersonalOS", logo badge), a GlassCard
// with Full Name + Username (optional) fields, "Get Started" CTA, and a
// top banner for validation errors.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel:       AppViewModel,
    modifier:        Modifier = Modifier,
) {
    val isDark  = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0A0B) else Color(0xFFE8EDF3)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileName   = uiState.prefs.profileName
    val profileUser   = uiState.prefs.profileUsername

    var fullName by remember { mutableStateOf(profileName) }
    var username by remember { mutableStateOf(profileUser) }
    var banner   by remember { mutableStateOf<String?>(null) }

    val handleSignUp = {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) {
            banner = "Please enter your full name to continue."
        } else {
            viewModel.setProfileName(trimmed)
            viewModel.setProfileUsername(username.trim())
            onAuthenticated()
        }
    }

    // AU-1: loading splash while app state is hydrating
    if (!uiState.hasHydrated) {
        Box(
            modifier = modifier.fillMaxSize().background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, ShapeLg)
                        .border(1.dp, MaterialTheme.colorScheme.primary, ShapeLg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                }
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = Spacing.lg, bottom = Spacing.x2l),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            HeroSurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Welcome",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text  = "Your PersonalOS",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text  = "All your tasks, calendar, and finances — stored locally on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // AU-3: logo badge — 40×40, ShapeLg (20dp radius), 1dp primary border, app icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, ShapeLg)
                            .border(1.dp, MaterialTheme.colorScheme.primary, ShapeLg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(24.dp),
                        )
                    }
                }
            }

            GlassCard(variant = GlassCardVariant.Default, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // AU-2: leading person icon on both fields
                    OutlinedTextField(
                        value         = fullName,
                        onValueChange = { v -> fullName = v; banner = null },
                        label         = { Text("Full Name") },
                        leadingIcon   = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value         = username,
                        onValueChange = { v -> username = v; banner = null },
                        label         = { Text("Username (optional)") },
                        leadingIcon   = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick  = { handleSignUp() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = CircleShape,
                    ) {
                        Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text      = "No account required. Your data stays on this device.",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Top banner for validation errors
    TopBanner(
        tone      = BannerTone.Info,
        message   = banner ?: "",
        visible   = banner != null,
        onDismiss = { banner = null },
    )
}
