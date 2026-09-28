package com.belinze.lifeos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.belinze.lifeos.ui.theme.LocalDarkTheme
import com.belinze.lifeos.ui.theme.ShapeLg

// ─────────────────────────────────────────────────────────────────────────────
// AppAlertDialog
//
// Compact, glass-styled replacement for M3 AlertDialog. Uses the app's own
// layered background so it matches GlassCard instead of using the grey
// MaterialTheme.colorScheme.surfaceContainer default.
//
// Padding: 16dp (vs Material's 24dp) for a tighter, less-bloated look.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    val isDark = LocalDarkTheme.current

    val gradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF101014), Color(0xFF0E1B2E), Color(0xFF101014)),
            start  = Offset.Zero,
            end    = Offset.Infinite,
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF), Color(0xFFFFFFFF)),
            start  = Offset.Zero,
            end    = Offset.Infinite,
        )
    }

    val tintColor  = if (isDark) Color(0xFF1E232D).copy(alpha = 0.55f)
                     else         Color(0xFFF1F5F9).copy(alpha = 0.55f)
    val frostColor = if (isDark) Color(0xFF14161C).copy(alpha = 0.45f)
                     else         Color(0xFFF8FAFC).copy(alpha = 0.50f)

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeLg)
                    .drawBehind {
                        drawRect(brush = gradient)
                        drawRect(color = tintColor)
                        drawRect(color = frostColor)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (title != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) {
                        title()
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (text != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        text()
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (dismissButton != null) dismissButton()
                    confirmButton()
                }
            }
        }
    }
}
