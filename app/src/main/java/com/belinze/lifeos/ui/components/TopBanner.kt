package com.belinze.lifeos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belinze.lifeos.ui.theme.ShapeLg
import com.belinze.lifeos.ui.theme.ShapePill
import com.belinze.lifeos.ui.theme.Spacing
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// TopBanner (toast) + InlineBanner
//
// TopBanner:  1:1 port of src/context/ToastContext.tsx.
//   ‣ Near-black glass pill, centred, icon + tone-coloured text
//   ‣ Spring scale+translate enter, timing fade-out exit
//   ‣ 4 tones: info, success, warning, error
//
// InlineBanner:
//   ‣ Static (no animation), per-tone light/dark bg+border+text
//   ‣ Horizontal margin applied by caller
// ─────────────────────────────────────────────────────────────────────────────

enum class BannerTone { Info, Success, Warning, Error }

private val TONE_COLORS = mapOf(
    BannerTone.Success to Color(0xFF4ADE80),
    BannerTone.Error   to Color(0xFFF87171),
    BannerTone.Warning to Color(0xFFFBBF24),
    BannerTone.Info    to Color(0xFF60A5FA),
)

private val TONE_ICONS = mapOf(
    BannerTone.Success to Icons.Outlined.CheckCircle,
    BannerTone.Error   to Icons.Outlined.Error,
    BannerTone.Warning to Icons.Outlined.Warning,
    BannerTone.Info    to Icons.Outlined.Info,
)

private val PILL_BG = Color(0xF00E0E12)   // rgba(14, 14, 18, 0.94)
private val PILL_SHAPE = ShapePill

// ─── TopBanner (toast pill) ──────────────────────────────────────────────────

@Composable
fun TopBanner(
    visible:       Boolean,
    message:       String,
    tone:          BannerTone   = BannerTone.Info,
    onDismiss:     (() -> Unit)? = null,
    autoDismissMs: Int?         = null,
    modifier:      Modifier     = Modifier,
) {
    val toneColor = TONE_COLORS[tone] ?: MaterialTheme.colorScheme.primary
    val toneIcon  = TONE_ICONS[tone] ?: Icons.Outlined.Info

    if (autoDismissMs != null && onDismiss != null && visible) {
        LaunchedEffect(message) {
            delay(autoDismissMs.toLong())
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec   = spring(dampingRatio = 0.65f, stiffness = 500f),
            initialScale    = 0.82f,
            transformOrigin = TransformOrigin.Center,
        ) + slideInVertically(
            animationSpec  = spring(dampingRatio = 0.65f, stiffness = 500f),
            initialOffsetY = { -it / 3 },
        ) + fadeIn(tween(160)),
        exit = scaleOut(
            animationSpec   = tween(200),
            targetScale     = 0.88f,
            transformOrigin = TransformOrigin.Center,
        ) + slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { -it / 4 },
        ) + fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 16.dp,
                        shape = PILL_SHAPE,
                        ambientColor = Color.Black.copy(alpha = 0.45f),
                        spotColor = Color.Black.copy(alpha = 0.45f),
                    )
                    .background(PILL_BG, PILL_SHAPE)
                    .then(
                        if (onDismiss != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = Spacing.base, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = toneIcon,
                    contentDescription = null,
                    tint = toneColor,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp,
                    color = toneColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

// ─── InlineBanner ────────────────────────────────────────────────────────────

private data class ToneColors(
    val bg:     Color,
    val border: Color,
    val icon:   Color,
    val text:   Color,
    val icon2:  ImageVector,
)

@Composable
private fun toneColors(tone: BannerTone, isDark: Boolean): ToneColors = when (tone) {
    BannerTone.Info    -> ToneColors(
        bg     = if (isDark) Color(0xFF0C2340) else Color(0xFFEFF6FF),
        border = if (isDark) Color(0xFF1E4A7A) else Color(0xFFBFDBFE),
        icon   = if (isDark) Color(0xFF57B9FF) else Color(0xFF0369A1),
        text   = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF),
        icon2  = Icons.Outlined.Info,
    )
    BannerTone.Success -> ToneColors(
        bg     = if (isDark) Color(0xFF052E16) else Color(0xFFF0FDF4),
        border = if (isDark) Color(0xFF14532D) else Color(0xFFBBF7D0),
        icon   = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
        text   = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534),
        icon2  = Icons.Outlined.CheckCircle,
    )
    BannerTone.Warning -> ToneColors(
        bg     = if (isDark) Color(0xFF1C1A00) else Color(0xFFFFFBEB),
        border = if (isDark) Color(0xFF3D3500) else Color(0xFFFDE68A),
        icon   = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
        text   = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E),
        icon2  = Icons.Outlined.Warning,
    )
    BannerTone.Error   -> ToneColors(
        bg     = if (isDark) Color(0xFF2D0A0A) else Color(0xFFFFF1F2),
        border = if (isDark) Color(0xFF5A1010) else Color(0xFFFECACA),
        icon   = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
        text   = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
        icon2  = Icons.Outlined.Error,
    )
}

@Composable
fun InlineBanner(
    message:   String,
    tone:      BannerTone   = BannerTone.Info,
    modifier:  Modifier     = Modifier,
    onDismiss: (() -> Unit)? = null,
    action:    String?      = null,
    onAction:  (() -> Unit)? = null,
) {
    val isDark = isSystemInDarkTheme()
    val colors = toneColors(tone, isDark)

    InlineBannerContent(
        message   = message,
        colors    = colors,
        onDismiss = onDismiss,
        action    = action,
        onAction  = onAction,
        modifier  = modifier,
    )
}

@Composable
private fun InlineBannerContent(
    message:   String,
    colors:    ToneColors,
    onDismiss: (() -> Unit)?,
    modifier:  Modifier     = Modifier,
    action:    String?      = null,
    onAction:  (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = ShapeLg,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .border(1.dp, colors.border, ShapeLg)
            .background(colors.bg, ShapeLg)
            .then(
                if (onDismiss != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = Spacing.base, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = colors.icon2,
                contentDescription = null,
                tint = colors.icon,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            if (action != null && onAction != null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = action,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.icon,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    ),
                )
            } else if (onDismiss != null) {
                Spacer(Modifier.width(Spacing.sm))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = colors.icon,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
