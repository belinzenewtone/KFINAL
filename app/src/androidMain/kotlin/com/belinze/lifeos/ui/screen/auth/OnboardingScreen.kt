package com.belinze.lifeos.ui.screen.auth

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.GlassCardVariant
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.theme.LocalDarkTheme
import com.belinze.lifeos.ui.theme.ShapeLg
import com.belinze.lifeos.ui.theme.ShapeMd
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingScreen — 7-step flow, one screen per step, no scrolling.
//
// Each step fills the available space between the header and the CTA bar.
// Uses the app's GlassCard and glass-surface patterns instead of plain Cards.
// ─────────────────────────────────────────────────────────────────────────────

private const val TOTAL_STEPS = 7

private val STEP_SUBTITLES = mapOf(
    1 to "Your new home for planning, finance, and focus.",
    2 to "Three pillars that shape your daily flow.",
    3 to "Tell us your name and primary focus.",
    4 to "Get reminded about tasks, bills, and events on time.",
    5 to "Auto-import M-Pesa transactions without lifting a finger.",
    6 to "Keep imports running even when the app is closed.",
    7 to "You're all set — let's get started.",
)

private data class GoalOption(
    val key:         String,
    val title:       String,
    val description: String,
    val icon:        ImageVector,
)

private val GOALS = listOf(
    GoalOption("productivity", "Optimize Productivity", "Sharper focus and better execution.", Icons.Outlined.Speed),
    GoalOption("finance",      "Strengthen Finance",    "Track spending with clear control.",  Icons.Outlined.PieChart),
    GoalOption("balanced",     "Balance Everything",    "Work, money, and time in one flow.",  Icons.Outlined.Tune),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel:  AppViewModel,
    modifier:   Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark  = LocalDarkTheme.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.prefs

    var step     by remember { mutableStateOf(prefs.onboardingStep.coerceIn(1, TOTAL_STEPS)) }
    var goal     by remember { mutableStateOf(prefs.onboardingGoal) }
    var fullName by remember { mutableStateOf(prefs.profileName) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var notificationsAllowed by remember { mutableStateOf(prefs.notificationsEnabled) }
    var smsAllowed           by remember { mutableStateOf(false) }
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
        smsAllowed = result[Manifest.permission.READ_SMS] == true ||
            result[Manifest.permission.RECEIVE_SMS] == true
    }

    val saveStep: (Int) -> Unit = { newStep ->
        step = newStep
        viewModel.setOnboardingStep(newStep)
        errorMsg = null
    }

    val handleContinue = {
        when {
            step == 3 && fullName.isBlank() -> errorMsg = "Please enter your name to continue."
            step >= TOTAL_STEPS -> {
                if (fullName.isBlank()) {
                    errorMsg = "Please enter your name before finishing."
                } else {
                    viewModel.setProfileName(fullName.trim())
                    viewModel.setProfileUsername(fullName.trim().split(" ").firstOrNull() ?: "")
                    saveStep(TOTAL_STEPS)
                    onComplete()
                }
            }
            else -> saveStep(step + 1)
        }
    }

    val ctaLabel = when (step) {
        1           -> "Let's Begin"
        TOTAL_STEPS -> "Start My Journey"
        else        -> "Continue"
    }

    // App background — matches the main app background token
    val bgColor = if (isDark) Color(0xFF08090E) else Color(0xFFE8EDF3)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
    ) {
        // ── Step header ────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 1) {
                IconButton(
                    onClick  = { saveStep(step - 1) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            } else {
                Spacer(Modifier.width(36.dp))
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = "Step $step of $TOTAL_STEPS",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text  = STEP_SUBTITLES[step] ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Error banner ───────────────────────────────────────────────────
        if (errorMsg != null) {
            InlineBanner(
                tone      = BannerTone.Warning,
                message   = errorMsg ?: "",
                modifier  = Modifier.padding(horizontal = Spacing.screenHorizontal),
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        // ── Step body ──────────────────────────────────────────────────────
        AnimatedContent(
            targetState    = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it else it } + fadeOut())
            },
            label     = "onboardingStep",
            modifier  = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.screenHorizontal),
        ) { targetStep ->
            when (targetStep) {
                1    -> WelcomeStep()
                2    -> PillarsStep()
                3    -> ProfileSetupStep(
                    fullName     = fullName,
                    onNameChange = { v -> fullName = v; errorMsg = null },
                    selectedGoal = goal,
                    onGoalSelect = { g -> goal = g; viewModel.setOnboardingGoal(g) },
                )
                4    -> PermissionStep(
                    allowed     = notificationsAllowed,
                    onAllow     = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onSkip      = { notificationsAllowed = false; viewModel.setNotificationsEnabled(false); saveStep(step + 1) },
                    icon        = Icons.Outlined.Speed,
                    title       = "Stay up to date",
                    body        = "Task timers and reminders will reach you even when the app is in the background.",
                    pillarTitle = "Timely nudges",
                    pillarBody  = "Tasks, bills, and events — notified right when they're due.",
                )
                5    -> PermissionStep(
                    allowed     = smsAllowed,
                    onAllow     = { smsPermLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)) },
                    onSkip      = { smsAllowed = false; saveStep(step + 1) },
                    icon        = Icons.Outlined.RocketLaunch,
                    title       = "Smart finance imports",
                    body        = "M-Pesa transactions and Fuliza activity imported automatically — no manual entry.",
                    pillarTitle = "Zero manual entry",
                    pillarBody  = "Debits, credits, and Fuliza draws appear without opening the app.",
                )
                6    -> BackgroundReceiverStep(
                    enabled  = bgReceiverEnabled,
                    onEnable = {
                        bgReceiverEnabled = true
                        viewModel.setSmsBgReceiver(true)
                        runCatching {
                            context.startActivity(
                                Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    },
                    onSkip = { bgReceiverEnabled = false; viewModel.setSmsBgReceiver(false); saveStep(step + 1) },
                )
                else -> FinalStep()
            }
        }

        // ── CTA bar ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.lg, top = Spacing.sm),
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
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(TOTAL_STEPS) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (index == step - 1) 24.dp else 6.dp, height = 4.dp)
                            .background(
                                if (index < step) MaterialTheme.colorScheme.primary
                                else              MaterialTheme.colorScheme.outlineVariant,
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
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ShapeLg,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text       = "Welcome to LifeOS",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text  = "Your sanctuary for productivity, finance, and mindful planning.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            FeatureRow(Icons.Outlined.Speed,        "Tasks, routines, and focused planning")
            FeatureRow(Icons.Outlined.PieChart,     "Budgets, spending, and financial trends")
            FeatureRow(Icons.Outlined.CalendarMonth,"Events, birthdays, and smart reminders")
        }
    }
}

// ─── Step 2: Pillars ─────────────────────────────────────────────────────────

@Composable
private fun PillarsStep() {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text       = "One place for everything.",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text  = "Planning and money flows, aligned in one calm surface.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OnboardingPillarCard(Icons.Outlined.Speed,        "Productivity",       "Prioritize what matters and keep focused execution daily.")
            OnboardingPillarCard(Icons.Outlined.CalendarMonth,"Planning & Calendar","Events, reminders, birthdays — all in one view.")
            OnboardingPillarCard(Icons.Outlined.PieChart,     "Finance",            "Track spending, watch budgets, and review trends.")
        }
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
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text       = "Tell us about yourself.",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.lg))
        OutlinedTextField(
            value         = fullName,
            onValueChange = onNameChange,
            label         = { Text("Your name") },
            leadingIcon   = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text  = "Primary focus",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            GOALS.forEach { goal ->
                val selected = selectedGoal == goal.key
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeMd)
                        .border(
                            width = 1.5.dp,
                            color = if (selected) primary else MaterialTheme.colorScheme.outline,
                            shape = ShapeMd,
                        )
                        .clickable(interactionSource = interactionSource, indication = ripple()) {
                            onGoalSelect(goal.key)
                        }
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        goal.icon,
                        contentDescription = null,
                        tint     = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            goal.title,
                            style     = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color     = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selected) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint     = primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── Steps 4 & 5: Permission steps ───────────────────────────────────────────

@Composable
private fun PermissionStep(
    allowed:     Boolean,
    onAllow:     () -> Unit,
    onSkip:      () -> Unit,
    icon:        ImageVector,
    title:       String,
    body:        String,
    pillarTitle: String,
    pillarBody:  String,
) {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text  = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OnboardingPillarCard(Icons.Outlined.Shield, "Private & secure",  "Your data stays on-device — nothing is uploaded.")
            OnboardingPillarCard(icon,                  pillarTitle,         pillarBody)
        }
        Spacer(Modifier.height(Spacing.lg))
        if (allowed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Allowed", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text("Allow") }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
            }
        }
    }
}

// ─── Step 6: Background receiver ─────────────────────────────────────────────

@Composable
private fun BackgroundReceiverStep(
    enabled:  Boolean,
    onEnable: () -> Unit,
    onSkip:   () -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text       = "Capture M-Pesa in the background",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text  = "New M-Pesa messages can be imported automatically, even when the app is closed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OnboardingPillarCard(Icons.Outlined.RocketLaunch, "Automatic imports", "Receive money or top up — the transaction appears without opening the app.")
            OnboardingPillarCard(Icons.Outlined.Shield,       "Keep it running",   "Allow unrestricted battery so Android doesn't block the receiver.")
        }
        Spacer(Modifier.height(Spacing.lg))
        if (enabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Background capture enabled", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text("Enable Background Capture") }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
            }
        }
    }
}

// ─── Step 7: Final ───────────────────────────────────────────────────────────

@Composable
private fun FinalStep() {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), ShapeLg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text       = "You're all set.",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text  = "Welcome to your new digital sanctuary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            FeatureRow(Icons.Outlined.AutoAwesome, "Personalized insights tuned to your real usage")
            FeatureRow(Icons.Outlined.Speed,       "Tasks, calendar, and finance in a single rhythm")
            FeatureRow(Icons.Outlined.Shield,      "Your data stays controlled and private")
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), ShapeMd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OnboardingPillarCard(icon: ImageVector, title: String, description: String) {
    GlassCard(variant = GlassCardVariant.Elevated) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), ShapeMd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
