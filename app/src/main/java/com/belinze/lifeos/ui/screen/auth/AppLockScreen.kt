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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
// 1:1 port of AppLockScreen.tsx.
//
// Behaviour:
//   ‣ If fingerprintEnabled → show BiometricPrompt immediately on compose
//   ‣ PIN pad: 4-dot indicator + 3×4 grid (1–9, ∅/bio, 0, backspace)
//   ‣ Correct PIN → onUnlock()
//   ‣ Wrong PIN → error-coloured dots + shake animation + vibration
//   ‣ Forgot PIN? → disables screen lock (mirrors RN Alert flow)
//   ‣ "Try fingerprint again" button appears after initial bio check
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppLockScreen(
    prefs:    AppPreferenceState,
    onUnlock: () -> Unit,
    onForgotPin: ((onConfirm: () -> Unit) -> Unit)? = null, // host shows dialog then calls onConfirm
    modifier: Modifier = Modifier,
) {
    val context    = LocalContext.current
    val isDark     = isSystemInDarkTheme()
    val bgColor    = if (isDark) Color(0xFF0A0A0B) else Color(0xFFE8EDF3)
    val primary    = MaterialTheme.colorScheme.primary

    var enteredPin       by remember { mutableStateOf("") }
    var errorMsg         by remember { mutableStateOf("") }
    var checkingBio      by remember { mutableStateOf(prefs.fingerprintEnabled) }

    val shakeOffset   = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val canUseBiometric = remember {
        val bm = BiometricManager.from(context)
        val result = bm.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        prefs.fingerprintEnabled &&
            (result == BiometricManager.BIOMETRIC_SUCCESS ||
             result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
    }

    @Suppress("DEPRECATION")
    fun vibrate() {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun triggerWrongPin() {
        vibrate()
        coroutineScope.launch {
            // RN uses Vibration.vibrate(300); Compose adds a shake for extra tactile feedback
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
                checkingBio = false
                onUnlock()
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

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LifeOS")
            .setSubtitle("Use your biometric credential")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        if (canUseBiometric) showBiometricPrompt()
    }

    fun onDigit(digit: String) {
        if (enteredPin.length >= 6) return
        errorMsg = ""
        val newPin = enteredPin + digit
        enteredPin = newPin
        if (newPin.length == 6) {
            if (newPin == prefs.pinCode) {
                onUnlock()
            } else {
                errorMsg   = "Incorrect PIN, try again"
                triggerWrongPin()
                coroutineScope.launch {
                    delay(300)
                    enteredPin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMsg = ""
        }
    }

    val isError = errorMsg.isNotEmpty()
    val subtitle = when {
        checkingBio -> "Confirm your fingerprint to continue"
        isError     -> errorMsg
        else        -> "Unlock to continue"
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier            = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon circle (lock ↔ fingerprint) — matches RN iconCircle
        Box(
            modifier         = Modifier
                .size(72.dp)
                .background(color = primary.copy(alpha = 0.12f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = if (checkingBio) Icons.Filled.Fingerprint else Icons.Outlined.Lock,
                contentDescription = null,
                tint               = primary,
                modifier           = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text       = if (checkingBio) "Fingerprint required" else "Enter your PIN",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text  = subtitle,
            fontSize = 14.sp,
            color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )

        // "Try fingerprint again" — matches RN Button above the dots
        if (canUseBiometric && !checkingBio) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = {
                    checkingBio = true
                    showBiometricPrompt()
                },
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = primary),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Try fingerprint again", fontSize = 14.sp)
            }
        } else {
            Spacer(Modifier.height(20.dp))
        }

        Spacer(Modifier.height(20.dp))

        // 6-dot PIN indicator (with shake + error colouring)
        val dotColor  = if (isError) MaterialTheme.colorScheme.error else primary
        val emptyDot  = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        ) {
            repeat(6) { idx ->
                val filled = idx < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (filled) dotColor else Color.Transparent,
                            shape = CircleShape,
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (filled) dotColor else emptyDot,
                            shape = CircleShape,
                        ),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // PIN pad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("bio", "0", "del"),  // bio = fingerprint shortcut; null in RN (empty placeholder)
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { key ->
                        PinKey(
                            key      = key,
                            primary  = primary,
                            hasBio   = canUseBiometric && !checkingBio,
                            onDigit  = { onDigit(it) },
                            onDelete = { onBackspace() },
                            onBio    = {
                                checkingBio = true
                                showBiometricPrompt()
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Forgot PIN? — matches RN Button at bottom
        TextButton(
            onClick = {
                onForgotPin?.invoke(onUnlock) ?: run {
                    // Fallback when no host-supplied dialog: Android Toast
                    Toast.makeText(context, "Contact support to reset your PIN", Toast.LENGTH_LONG).show()
                }
            },
        ) {
            Text("Forgot PIN?", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun PinKey(
    key:     String,
    primary: Color,
    hasBio:  Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onBio:   () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val size = 72.dp

    Box(
        modifier         = Modifier
            .size(size)
            .background(
                color = when (key) {
                    "bio", "del" -> Color.Transparent
                    else         -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
                },
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(alpha = 0.20f)),
                enabled           = key != "bio" || hasBio,
                onClick = {
                    when (key) {
                        "del" -> onDelete()
                        "bio" -> if (hasBio) onBio()
                        else  -> onDigit(key)
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            "del" -> Icon(
                imageVector        = Icons.Filled.Backspace,
                contentDescription = "Delete",
                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier           = Modifier.size(24.dp),
            )
            "bio" -> if (hasBio) Icon(
                imageVector        = Icons.Filled.Fingerprint,
                contentDescription = "Biometric",
                tint               = primary,
                modifier           = Modifier.size(28.dp),
            )
            else  -> Text(
                text       = key,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
