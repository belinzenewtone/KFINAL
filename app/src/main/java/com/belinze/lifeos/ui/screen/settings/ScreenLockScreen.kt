package com.belinze.lifeos.ui.screen.settings

import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SettingsViewModel

private const val PIN_LENGTH = 6

private val RELOCK_OPTIONS = listOf(
    0  to "Immediately",
    1  to "1 min",
    5  to "5 min",
    15 to "15 min",
    30 to "30 min",
    60 to "1 hour",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenLockScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    var activeTab   by remember { mutableStateOf("biometric") }
    var newPin      by remember { mutableStateOf("") }
    var confirmPin  by remember { mutableStateOf("") }
    var message     by remember { mutableStateOf<String?>(null) }
    var error       by remember { mutableStateOf<String?>(null) }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                message = "Biometric verified successfully"
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(context, "Not recognised — try again", Toast.LENGTH_SHORT).show()
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify biometric")
            .setSubtitle("Confirm your biometric credential")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()
        BiometricPrompt(activity, executor, callback).authenticate(info)
    }

    PageScaffold(
        title      = "Screen Lock",
        onBack     = { navController.popBackStack() },
        scrollable = false,
        topBanner  = {
            TopBanner(
                visible       = message != null,
                message       = message ?: "",
                tone          = BannerTone.Success,
                onDismiss     = { message = null },
                autoDismissMs = 2500,
            )
        },
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.bottomNavSafeArea),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            // ── Tab switcher ──────────────────────────────────────────────────
            val primary          = MaterialTheme.colorScheme.primary
            val surfaceVar       = MaterialTheme.colorScheme.surfaceVariant
            val onSurfaceVar     = MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(surfaceVar),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("biometric" to "Biometric", "pin" to "PIN").forEach { (tab, label) ->
                        val selected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) primary else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { activeTab = tab }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (selected) Color.White else onSurfaceVar,
                                fontSize   = 15.sp,
                            )
                        }
                    }
                }
            }

            // ── Biometric tab ─────────────────────────────────────────────────
            if (activeTab == "biometric") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    // Biometric Lock row
                    LockRow(
                        icon     = Icons.Outlined.Fingerprint,
                        title    = "Biometric Lock",
                        subtitle = "Use fingerprint or face to unlock",
                    ) {
                        Switch(
                            checked       = settings.fingerprintEnabled,
                            onCheckedChange = { v ->
                                viewModel.setFingerprintEnabled(v)
                                message = if (v) "Biometric unlock enabled" else null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = Color.White,
                                checkedTrackColor   = primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    }

                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = Spacing.sm),
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    // Relock Delay row
                    var relockExpanded by remember { mutableStateOf(false) }
                    val relockLabel = RELOCK_OPTIONS.find { it.first == settings.lockTimeoutMinutes }
                        ?.second ?: "${settings.lockTimeoutMinutes}m"

                    LockRow(
                        icon     = Icons.Outlined.Timer,
                        title    = "Relock Delay",
                        subtitle = "Grace period before requiring biometric again",
                    ) {
                        Box {
                            Surface(
                                onClick = { relockExpanded = true },
                                shape   = RoundedCornerShape(8.dp),
                                color   = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Row(
                                    modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        relockLabel,
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color      = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Icon(Icons.Outlined.ArrowDropDown, null,
                                        Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded        = relockExpanded,
                                onDismissRequest = { relockExpanded = false },
                            ) {
                                RELOCK_OPTIONS.forEach { (mins, label) ->
                                    DropdownMenuItem(
                                        text    = { Text(label) },
                                        onClick = {
                                            viewModel.setLockTimeout(mins)
                                            relockExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Authenticate Now button
                    Spacer(Modifier.height(Spacing.sm))
                    Button(
                        onClick  = { triggerBiometric() },
                        enabled  = settings.fingerprintEnabled,
                        shape    = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor   = Color.White,
                        ),
                    ) {
                        Icon(Icons.Outlined.Fingerprint, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Authenticate Now", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }

            // ── PIN tab ───────────────────────────────────────────────────────
            if (activeTab == "pin") {
                // PIN Lock toggle card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    LockRow(
                        icon     = Icons.Outlined.Pin,
                        title    = "PIN Lock",
                        subtitle = "Use a ${PIN_LENGTH}-digit PIN to unlock",
                    ) {
                        Switch(
                            checked       = settings.screenLockEnabled,
                            onCheckedChange = { v ->
                                if (v) {
                                    viewModel.setScreenLockEnabled(true)
                                } else {
                                    viewModel.disableScreenLock()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = Color.White,
                                checkedTrackColor   = primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    }
                }

                // PIN setup card — shown when lock is enabled
                if (settings.screenLockEnabled) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (settings.pinCode.isNotEmpty()) {
                                "Reset your secure access code"
                            } else {
                                "Set up your secure access code"
                            },
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "Use exactly $PIN_LENGTH digits. Your new PIN is stored only on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.lg))

                        PinInput("New PIN", newPin) { newPin = it }
                        PinInput("Confirm new PIN", confirmPin) { confirmPin = it }

                        if (error != null) {
                            Text(
                                error!!,
                                color    = MaterialTheme.colorScheme.error,
                                style    = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = Spacing.xs),
                            )
                        }
                    }

                    // Update button — outside the card, full-width pill
                    Button(
                        onClick = {
                            error = null
                            when {
                                newPin.length != PIN_LENGTH || confirmPin.length != PIN_LENGTH ->
                                    error = "PIN must be exactly $PIN_LENGTH digits"
                                newPin != confirmPin ->
                                    error = "PINs don't match"
                                else -> {
                                    viewModel.setPinCode(newPin)
                                    viewModel.setScreenLockEnabled(true)
                                    newPin = ""; confirmPin = ""
                                    message = if (settings.pinCode.isEmpty()) {
                                        "PIN set successfully"
                                    } else {
                                        "PIN updated successfully"
                                    }
                                }
                            }
                        },
                        enabled  = newPin.length == PIN_LENGTH && confirmPin.length == PIN_LENGTH,
                        shape    = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = primary,
                            contentColor           = Color.White,
                            disabledContainerColor = primary.copy(alpha = 0.45f),
                            disabledContentColor   = Color.White.copy(alpha = 0.7f),
                        ),
                    ) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            if (settings.pinCode.isNotEmpty()) "Update PIN" else "Set PIN",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─── Lock settings row ────────────────────────────────────────────────────────

@Composable
private fun LockRow(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    control:  @Composable () -> Unit,
) {
    Row(
        modifier             = Modifier.fillMaxWidth(),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Right-side control (toggle / chip / etc.)
        control()
    }
}

// ─── PIN input field ──────────────────────────────────────────────────────────

@Composable
private fun PinInput(label: String, value: String, onChange: (String) -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${value.length}/$PIN_LENGTH",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value               = value,
            onValueChange       = { onChange(it.filter { c -> c.isDigit() }.take(PIN_LENGTH)) },
            keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            placeholder         = { Text("000000", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
            singleLine          = true,
            modifier            = Modifier.fillMaxWidth(),
            colors              = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
        )
    }
}
