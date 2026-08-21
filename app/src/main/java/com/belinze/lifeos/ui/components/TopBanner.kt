package com.belinze.lifeos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.Motion
import com.belinze.lifeos.ui.theme.ShapeLg
import com.belinze.lifeos.ui.theme.Spacing
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// TopBanner + InlineBanner
//
// 1:1 port of src/components/common/TopBanner.tsx and InlineBanner.tsx.
//
// TopBanner:
//  ‣ AnimatedVisibility with enter 220ms / exit 180ms slide+fade
//  ‣ 4 tones: info, success, warning, error
//  ‣ Shown at top of page, full-width, no horizontal margin
//
// InlineBanner:
//  ‣ Static (no animation), 3 tones: info, success, warning
//  ‣ Horizontal margin applied by caller
// ─────────────────────────────────────────────────────────────────────────────

enum class BannerTone { Info, Success, Warning, Error }

private data class ToneColors(
    val bg:       Color,
    val border:   Color,
    val icon:     Color,
    val text:     Color,
    val icon2:    ImageVector,
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

// ─── TopBanner ────────────────────────────────────────────────────────────────
// React TopBanner is a floating rounded pill anchored below the status bar, not a
// full-width inline bar. Tone color: error→error, success→#7BC47B,
// warning→#F5CB5C, info→primary. Background is that color at 12.5% alpha.

@Composable
fun TopBanner(
    visible:       Boolean,
    message:       String,
    tone:          BannerTone   = BannerTone.Info,
    onDismiss:     (() -> Unit)? = null,
    autoDismissMs: Int?         = null,
    modifier:      Modifier     = Modifier,
) {
    val toneColor = when (tone) {
        BannerTone.Error   -> MaterialTheme.colorScheme.error
        BannerTone.Success -> Color(0xFF7BC47B)
        BannerTone.Warning -> Color(0xFFF5CB5C)
        BannerTone.Info    -> MaterialTheme.colorScheme.primary
    }
    val colors = ToneColors(
        bg     = toneColor.copy(alpha = 0.125f),
        border = toneColor,
        icon   = toneColor,
        text   = toneColor,
        icon2  = when (tone) {
            BannerTone.Error   -> Icons.Outlined.Error
            BannerTone.Success -> Icons.Outlined.CheckCircle
            BannerTone.Warning -> Icons.Outlined.Warning
            BannerTone.Info    -> Icons.Outlined.Info
        },
    )

    if (autoDismissMs != null && onDismiss != null && visible) {
        LaunchedEffect(visible, autoDismissMs, onDismiss) {
            delay(autoDismissMs.toLong())
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(
            animationSpec   = tween(durationMillis = Motion.bannerEnter),
            initialOffsetY  = { -it },
        ) + fadeIn(tween(Motion.bannerEnter)),
        exit    = slideOutVertically(
            animationSpec  = tween(durationMillis = Motion.bannerExit),
            targetOffsetY  = { -it },
        ) + fadeOut(tween(Motion.bannerExit)),
        modifier = modifier,
    ) {
        BannerContent(
            message   = message,
            colors    = colors,
            onDismiss = onDismiss,
            modifier  = Modifier.padding(horizontal = Spacing.screenHorizontal),
        )
    }
}

// ─── InlineBanner ─────────────────────────────────────────────────────────────

@Composable
fun InlineBanner(
    message:   String,
    tone:      BannerTone   = BannerTone.Info,
    modifier:  Modifier     = Modifier,
    onDismiss: (() -> Unit)? = null,   // optional X dismiss button
    action:    String?      = null,     // optional action button label
    onAction:  (() -> Unit)? = null,    // action callback
) {
    val isDark = isSystemInDarkTheme()
    val colors = toneColors(tone, isDark)

    BannerContent(
        message   = message,
        colors    = colors,
        onDismiss = onDismiss,
        action    = action,
        onAction  = onAction,
        modifier  = modifier,
    )
}

// ─── Shared content ───────────────────────────────────────────────────────────

@Composable
private fun BannerContent(
    message:   String,
    colors:    ToneColors,
    onDismiss: (() -> Unit)?,
    modifier:  Modifier     = Modifier,
    action:    String?      = null,
    onAction:  (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, ShapeLg)
            .background(colors.bg, ShapeLg)
            .then(
                if (onDismiss != null) {
                    Modifier.clickable(
                    interactionSource = interactionSource,
                    indication        = null,
                    onClick           = onDismiss,
                )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = Spacing.base, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = colors.icon2,
                contentDescription = null,
                tint               = colors.icon,
                modifier           = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text       = message,
                style      = MaterialTheme.typography.bodyMedium,
                color      = colors.text,
                maxLines   = 2,
                modifier   = Modifier.weight(1f),
            )
            if (action != null && onAction != null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text       = action,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.icon,
                    modifier   = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onAction,
                    ),
                )
            } else if (onDismiss != null) {
                Spacer(Modifier.width(Spacing.sm))
                Icon(
                    imageVector        = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint               = colors.icon,
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}
