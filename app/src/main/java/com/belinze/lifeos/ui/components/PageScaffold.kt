package com.belinze.lifeos.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belinze.lifeos.ui.theme.AppBarDimens
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// PageScaffold
//
// 1:1 port of src/components/layout/PageScaffold.tsx.
//
// Props:
//  ‣ eyebrow   — small uppercase label above title (optional)
//  ‣ title     — main page title (optional)
//  ‣ subtitle  — secondary text below title (optional)
//  ‣ onBack    — if non-null, shows back chevron in AppBar
//  ‣ actions   — trailing slot in AppBar (optional)
//  ‣ scrollable — wraps content in verticalScroll when true (default true)
//  ‣ gradient  — page background uses gradient when true (default true)
//
// Layout:
//  ┌─ StatusBar inset ──────────────────────┐
//  │ AppBar  (52dp)                          │
//  │   [back] eyebrow/title/subtitle actions │
//  ├─────────────────────────────────────────┤
//  │ Content + bottomNavSafeArea padding     │
//  └─────────────────────────────────────────┘
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PageScaffold(
    modifier:   Modifier               = Modifier,
    eyebrow:    String?                = null,
    title:      String?                = null,
    subtitle:   String?                = null,
    onBack:     (() -> Unit)?          = null,
    actions:    @Composable (() -> Unit)? = null,
    scrollable: Boolean                = true,
    gradient:   Boolean                = true,
    content:    @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()

    // Page background gradient matching RN LinearGradient in index.ts
    val bgGradient: Brush = if (gradient) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0A0B), Color(0xFF0D1117), Color(0xFF0A0A0B)),
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(Color(0xFFE8EDF3), Color(0xFFDDE4EE), Color(0xFFE8EDF3)),
            )
        }
    } else {
        Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = bgGradient) }
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // ── AppBar ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppBarDimens.height),
        ) {
            // Back button (leading)
            if (onBack != null) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = AppBarDimens.horizontalPad)
                        .size(AppBarDimens.backBtnSize),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = MaterialTheme.colorScheme.onBackground,
                        modifier           = Modifier.size(AppBarDimens.iconSize),
                    )
                }
            }

            // Title block (center or leading if no back)
            val titleStartPad = if (onBack != null) AppBarDimens.backBtnSize + AppBarDimens.horizontalPad else Spacing.screenHorizontal
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = titleStartPad, end = Spacing.xl),
                horizontalAlignment = Alignment.Start,
            ) {
                if (eyebrow != null) {
                    Text(
                        text       = eyebrow.uppercase(),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                        letterSpacing = 0.8.sp,
                        maxLines   = 1,
                    )
                }
                if (title != null) {
                    Text(
                        text       = title,
                        style      = MaterialTheme.typography.titleLarge,   // 18sp/600
                        color      = MaterialTheme.colorScheme.onBackground,
                        maxLines   = 1,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text     = subtitle,
                        style    = MaterialTheme.typography.bodySmall,      // 12sp
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        maxLines = 1,
                    )
                }
            }

            // Actions (trailing)
            if (actions != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = AppBarDimens.horizontalPad),
                ) {
                    actions()
                }
            }
        }

        // ── Content ──────────────────────────────────────────────────────────
        if (scrollable) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical   = Spacing.sm,
                    )
                    .padding(bottom = Spacing.bottomNavSafeArea),
                content = content,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                content = content,
            )
        }
    }
}
