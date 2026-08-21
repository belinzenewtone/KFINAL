package com.belinze.lifeos.ui.screen.auth

import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.belinze.lifeos.data.datastore.AppPreferenceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// AppLockScreen
//
// Clean PIN unlock UI:
//   ‣ Full-screen, theme-surfaced background
//   ‣ Icon + title centered in the top section
//   ‣ PIN dot indicator (shake-on-wrong)
//   ‣ 3×4 digit pad — bottom-left shows bio icon when available
//   ‣ "Forgot PIN?" link below the pad
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppLockScreen(
    prefs:       AppPreferenceState,
    onUnlock:    () -> Unit,
    onForgotPin: ((onConfirm: () -> Unit) -> Unit)? = null,
    modifier:    Modifier = Modifier,
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary

    var enteredPin       by remember { mutableStateOf("") }
    var errorMsg         by remember { mutableStateOf("") }
    var checkingBio      by remember { mutableStateOf(prefs.fingerprintEnabled) }
    var showForgotDialog by remember { mutableStateOf(false) }

    val shakeOffset    = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val canUseBiometric = remember {
        val bm = BiometricManager.from(context)
        val ok = bm.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        // Only BIOMETRIC_SUCCESS means hardware is present AND credentials enrolled.
        // BIOMETRIC_ERROR_NONE_ENROLLED means hardware exists but no fingerprints are
        // registered — the prompt would fail immediately, so we must NOT treat it as valid.
        prefs.fingerprintEnabled && ok == BiometricManager.BIOMETRIC_SUCCESS
    }

    @Suppress("DEPRECATION")
    fun vibrate() {
        (context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator)
            ?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun triggerWrongPin() {
        vibrate()
        coroutineScope.launch {
            shakeOffset.animateTo(14f, tween(50))
            shakeOffset.animateTo(-14f, tween(50))
            shakeOffset.animateTo(10f, tween(50))
            shakeOffset.animateTo(-10f, tween(50))
            shakeOffset.animateTo(0f, tween(60))
        }
    }

    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                checkingBio = false; onUnlock()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                checkingBio = false
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onAuthenticationFailed() {
                checkingBio = false
                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LifeOS")
            .setSubtitle("Use your biometric credential")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()
        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    LaunchedEffect(Unit) { if (canUseBiometric) showBiometricPrompt() }

    val pinLength = prefs.pinCode.length.coerceAtLeast(4)

    fun onDigit(digit: String) {
        if (enteredPin.length >= pinLength) return
        errorMsg = ""
        val newPin = enteredPin + digit
        enteredPin = newPin
        if (newPin.length == pinLength) {
            if (newPin == prefs.pinCode) {
                onUnlock()
            } else {
                errorMsg = "Incorrect PIN, try again"
                triggerWrongPin()
                coroutineScope.launch { delay(300); enteredPin = "" }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) { enteredPin = enteredPin.dropLast(1); errorMsg = "" }
    }

    val isError    = errorMsg.isNotEmpty()
    val dotFill    = if (isError) MaterialTheme.colorScheme.error else primary
    val dotEmpty   = MaterialTheme.colorScheme.outlineVariant

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
                .padding(top = 80.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── App icon + heading ────────────────────────────────────────────
            Surface(
                color  = primary.copy(alpha = 0.12f),
                shape  = RoundedCornerShape(20.dp),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector        = if (checkingBio) {
                            Icons.Outlined.Fingerprint
                        } else {
                            Icons.Outlined.Lock
                        },
                        contentDescription = null,
                        tint               = primary,
                        modifier           = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text       = if (checkingBio) "Verify your identity" else "Enter your PIN",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text  = when {
                    checkingBio -> "Confirm your fingerprint to continue"
                    isError     -> errorMsg
                    else        -> "Unlock to continue"
                },
                fontSize = 14.sp,
                color    = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(Modifier.weight(1f))

            // ── PIN dot indicator ─────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            ) {
                repeat(pinLength) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (filled) dotFill else Color.Transparent)
                            .border(1.5.dp, if (filled) dotFill else dotEmpty, CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── PIN pad ───────────────────────────────────────────────────────
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                // Bottom row: bio / fingerprint | 0 | backspace
                listOf(if (canUseBiometric && !checkingBio) "bio" else "", "0", "del"),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { key ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                PinKey(
                                    key     = key,
                                    primary = primary,
                                    onDigit  = { onDigit(it) },
                                    onDelete = { onBackspace() },
                                    onBio    = { checkingBio = true; showBiometricPrompt() },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Forgot PIN ────────────────────────────────────────────────────
            TextButton(onClick = { showForgotDialog = true }) {
                Text(
                    "Forgot PIN?",
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
    }

    // ── Forgot PIN dialog ─────────────────────────────────────────────────────
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title            = { Text("Reset PIN") },
            text             = { Text("This will disable your PIN lock. You can set a new PIN in Settings.") },
            confirmButton    = {
                TextButton(onClick = {
                    showForgotDialog = false
                    onForgotPin?.invoke(onUnlock) ?: onUnlock()
                }) { Text("Clear PIN", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ─── Pin key ──────────────────────────────────────────────────────────────────

@Composable
private fun PinKey(
    key:     String,
    primary: Color,
    onDigit:  (String) -> Unit,
    onDelete: () -> Unit,
    onBio:    () -> Unit,
) {
    val keySize = 72.dp

    if (key.isEmpty()) {
        Box(Modifier.size(keySize))   // invisible placeholder
        return
    }

    val isSurface = key != "del" && key != "bio"
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(keySize)
            .clip(CircleShape)
            .background(
                color = when (key) {
                    "del", "bio" -> Color.Transparent
                    else         -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
            )
            .then(
                if (!isSurface) Modifier else Modifier    // no border for digit keys
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(alpha = 0.2f)),
                onClick = {
                    when (key) {
                        "del" -> onDelete()
                        "bio" -> onBio()
                        else  -> onDigit(key)
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            "del" -> Icon(
                Icons.Outlined.Backspace,
                contentDescription = "Delete",
                tint               = MaterialTheme.colorScheme.onSurface,
                modifier           = Modifier.size(24.dp),
            )
            "bio" -> Icon(
                Icons.Outlined.Fingerprint,
                contentDescription = "Use fingerprint",
                tint               = primary,
                modifier           = Modifier.size(28.dp),
            )
            else -> Text(
                text       = key,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
