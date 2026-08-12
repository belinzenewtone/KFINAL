package com.belinze.lifeos.ui.screen.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.GlassCardVariant
import com.belinze.lifeos.ui.components.HeroSurface
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingScreen — full 7-step flow, 1:1 with OnboardingScreen.tsx
//
// Step 1: Welcome          — logo, tagline, feature rows
// Step 2: Pillars          — productivity / calendar / finance cards
// Step 3: Profile setup    — full name + primary focus (goal) selection
// Step 4: Notifications    — allow / skip
// Step 5: SMS access       — allow / skip
// Step 6: Background       — enable background SMS capture / skip
// Step 7: Final            — all set
//
// Persists step, goal, profile name, notification + SMS prefs via AppViewModel.
// ─────────────────────────────────────────────────────────────────────────────

private const val TOTAL_STEPS = 7

private val STEP_SUBTITLES = mapOf(
    1 to "A calm setup to personalize your planning and finance workspace.",
    2 to "Understand the core pillars that shape your daily flow.",
    3 to "Tell us your name and what you want to focus on.",
    4 to "Allow notifications so timers and reminders always reach you.",
    5 to "Allow SMS access so M-Pesa imports and Fuliza tracking work automatically.",
    6 to "Allow background capture so M-Pesa messages are imported even when the app is closed.",
    7 to "Final checks before launching into your dashboard.",
)

private data class GoalOption(
    val key:        String,
    val title:      String,
    val description: String,
    val icon:       ImageVector,
)

private val GOALS = listOf(
    GoalOption("productivity", "Optimize Productivity", "Sharper focus, smarter routines, better execution.", Icons.Filled.Speed),
    GoalOption("finance",      "Strengthen Finance",    "Track spending and budgets with clear control.",    Icons.Filled.PieChart),
    GoalOption("balanced",     "Balance Everything",    "Plan work, money, and time in one calm system.",   Icons.Filled.AutoAwesome),
)

@Composable
fun OnboardingScreen(
    onComplete:   () -> Unit,
    viewModel:    AppViewModel,
    modifier:     Modifier = Modifier,
) {
    val isDark   = isSystemInDarkTheme()
    val bgColor  = if (isDark) Color(0xFF0A0A0B) else Color(0xFFE8EDF3)

    // Persisted state (hydrated by AppViewModel prefs)
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.prefs

    var step      by remember { mutableStateOf(prefs.onboardingStep.coerceIn(1, TOTAL_STEPS)) }
    var goal      by remember { mutableStateOf(prefs.onboardingGoal) }
    var fullName  by remember { mutableStateOf(prefs.profileName) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    var notificationsAllowed by remember { mutableStateOf(prefs.notificationsEnabled) }
    var smsAllowed           by remember { mutableStateOf(false) }
    var smsChecked           by remember { mutableStateOf(false) }
    var bgReceiverEnabled    by remember { mutableStateOf(prefs.smsBgReceiver) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted
        viewModel.setNotificationsEnabled(granted)
    }

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_SMS] == true ||
            result[Manifest.permission.RECEIVE_SMS] == true
        smsAllowed = granted
        smsChecked = true
    }

    val saveStep: (Int) -> Unit = { newStep ->
        step = newStep
        viewModel.setOnboardingStep(newStep)
        errorMsg = null
    }

    val complete = {
        if (fullName.isBlank()) {
            errorMsg = "Please provide your full name before finishing."
        } else {
            viewModel.setProfileName(fullName.trim())
            viewModel.setProfileUsername(fullName.trim().split(" ").firstOrNull() ?: "")
            saveStep(TOTAL_STEPS)
            onComplete()
        }
    }

    val handleContinue = {
        when {
            step == 3 && fullName.isBlank() -> errorMsg = "Please provide your full name to continue."
            step >= TOTAL_STEPS              -> complete()
            else                             -> saveStep(step + 1)
        }
    }

    val ctaLabel = when (step) {
        1          -> "Let's Begin"
        TOTAL_STEPS -> "Start My Journey"
        else       -> "Continue"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        ) {
            // ── Hero header ────────────────────────────────────────────────
            HeroSurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (step > 1) {
                        IconButton(onClick = { if (step > 1) saveStep(step - 1) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint               = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                    Column {
                        Text(
                            text       = "Step $step of $TOTAL_STEPS",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text     = "PersonalOS setup",
                            style    = MaterialTheme.typography.titleLarge,
                            color    = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text  = STEP_SUBTITLES[step] ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(Spacing.md))
                InlineBanner(
                    tone    = BannerTone.Warning,
                    message = errorMsg ?: "",
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            // ── Step body ─────────────────────────────────────────────────
            GlassCard(variant = GlassCardVariant.Default, modifier = Modifier.fillMaxWidth()) {
                when (step) {
                    1 -> WelcomeStep()
                    2 -> PillarsStep()
                    3 -> ProfileSetupStep(
                        fullName    = fullName,
                        onNameChange = { v -> fullName = v; errorMsg = null },
                        selectedGoal = goal,
                        onGoalSelect = { g -> goal = g; viewModel.setOnboardingGoal(g) },
                    )
                    4 -> PermissionStep(
                        allowed = notificationsAllowed,
                        onAllow = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        onSkip  = { notificationsAllowed = false; viewModel.setNotificationsEnabled(false) },
                        title   = "Stay up to date",
                        body    = "Allow notifications so task timers and reminders can reach you even when the app is in the background.",
                    )
                    5 -> PermissionStep(
                        allowed = smsAllowed,
                        onAllow = { smsPermLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)) },
                        onSkip  = { smsAllowed = false; smsChecked = true },
                        title   = "Smart finance imports",
                        body    = "Allow SMS access so M-Pesa transactions and Fuliza activity can be imported automatically.",
                    )
                    6 -> BackgroundReceiverStep(
                        enabled = bgReceiverEnabled,
                        onEnable = {
                            bgReceiverEnabled = true
                            viewModel.setSmsBgReceiver(true)
                        },
                        onSkip = {
                            bgReceiverEnabled = false
                            viewModel.setSmsBgReceiver(false)
                        },
                    )
                    else -> FinalStep()
                }
            }

            Spacer(Modifier.height(Spacing.lg))
        }

        // ── CTA + progress dots ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.lg),
        ) {
            Button(
                onClick  = { handleContinue() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = CircleShape,
            ) {
                Text(ctaLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(TOTAL_STEPS) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = 28.dp, height = 4.dp)
                            .background(
                                if (index < step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

// ─── Step 1: Welcome ─────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep() {
    StepColumn {
        Text("Welcome to your PersonalOS", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Text("Your sanctuary for productivity, finance, and mindful planning.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FeatureRow(Icons.Filled.Speed, "Productivity — tasks, routines, and focused planning")
        FeatureRow(Icons.Filled.PieChart, "Finance — budgets, spending, and trends at a glance")
        FeatureRow(Icons.Filled.CalendarMonth, "Calendar — events, birthdays, and smart reminders")
    }
}

// ─── Step 2: Pillars ─────────────────────────────────────────────────────────

@Composable
private fun PillarsStep() {
    StepColumn {
        Text("One place for everything.", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Text("PersonalOS keeps your planning and money flows aligned in one calm surface.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PillarCard(Icons.Filled.Speed, "Productivity", "Prioritize what matters and keep focused execution daily.")
        PillarCard(Icons.Filled.CalendarMonth, "Planning & Calendar", "Events, reminders, birthdays, and countdowns — all in one view.")
        PillarCard(Icons.Filled.PieChart, "Finance", "Track spending, watch budgets, and review trends with confidence.")
    }
}

// ─── Step 3: Profile setup ───────────────────────────────────────────────────

@Composable
private fun ProfileSetupStep(
    fullName:     String,
    onNameChange: (String) -> Unit,
    selectedGoal: String,
    onGoalSelect: (String) -> Unit,
) {
    StepColumn {
        Text("Tell us about yourself.", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Text("This helps personalize your workspace.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value          = fullName,
            onValueChange  = onNameChange,
            label          = { Text("Your name") },
            singleLine     = true,
            modifier       = Modifier.fillMaxWidth(),
        )

        Text("Your primary focus", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        GOALS.forEach { goal ->
            val selected = selectedGoal == goal.key
            Card(
                onClick  = { onGoalSelect(goal.key) },
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                border   = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(goal.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.title, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(goal.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) {
                        Icon(Icons.Filled.Person, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// ─── Steps 4 & 5: Permission steps ───────────────────────────────────────────

@Composable
private fun PermissionStep(
    allowed: Boolean,
    onAllow: () -> Unit,
    onSkip:  () -> Unit,
    title:   String,
    body:    String,
) {
    StepColumn {
        Text(title, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PillarCard(Icons.Filled.Shield, "Private & secure", "Your data stays on-device — nothing is uploaded.")
        if (allowed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Spacing.sm))
                Text("Allowed", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text("Allow") }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now")
            }
        }
    }
}

// ─── Step 6: Background receiver ─────────────────────────────────────────────

@Composable
private fun BackgroundReceiverStep(
    enabled: Boolean,
    onEnable: () -> Unit,
    onSkip:   () -> Unit,
) {
    StepColumn {
        Text("Capture M-Pesa in the background", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Text("Even when the app is closed, new M-Pesa messages can be imported automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PillarCard(Icons.Filled.RocketLaunch, "Automatic imports", "Receive money or buy airtime — the transaction appears without opening the app.")
        PillarCard(Icons.Filled.Shield, "Keep it running", "You may need to allow unrestricted battery use so Android does not block the receiver.")
        if (enabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Spacing.sm))
                Text("Background capture enabled", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text("Enable Background Capture") }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now")
            }
        }
    }
}

// ─── Step 7: Final ───────────────────────────────────────────────────────────

@Composable
private fun FinalStep() {
    StepColumn {
        Text("You're all set.", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Text("Welcome to your new digital sanctuary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PillarCard(Icons.Filled.AutoAwesome, "Personalized Insights", "Actionable summaries tuned to your real usage.")
        PillarCard(Icons.Filled.Speed, "Unified Workflow", "Tasks, calendar, and finance in a single rhythm.")
        PillarCard(Icons.Filled.Shield, "Private & Secure", "Your data stays controlled, with transparent protection.")
    }
}

// ─── Shared step helpers ─────────────────────────────────────────────────────

@Composable
private fun StepColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        content = content,
    )
}

@Composable
private fun FeatureRow(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PillarCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape    = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Spacing.xs))
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
