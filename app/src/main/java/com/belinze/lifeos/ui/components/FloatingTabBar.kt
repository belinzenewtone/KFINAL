package com.belinze.lifeos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belinze.lifeos.ui.theme.LocalDarkTheme
import com.belinze.lifeos.ui.theme.Motion
import com.belinze.lifeos.ui.theme.ShapePill
import com.belinze.lifeos.ui.theme.ShapeXl
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.ui.theme.TabBarDimens

// ─────────────────────────────────────────────────────────────────────────────
// FloatingTabBar
//
// 1:1 port of src/navigation/FloatingTabBar.tsx.
//
// Spec:
//  ‣ position: absolute, left/right 12dp, height 58dp, borderRadius 24dp, borderWidth 1dp
//  ‣ Bottom offset: max(insets.bottom, 8dp) + 8dp
//  ‣ Shadow elevation 12dp
//  ‣ Background: surfaceVariant, border: outlineVariant
//  ‣ Per-tab spring: friction=7, tension=90 → spring(dampingRatio=0.43f, stiffness=375f)
//  ‣ Pill opacity: tween(100ms) — motion.fast
//  ‣ Active icon: primary. Inactive: onSurfaceVariant.
//  ‣ Icon 24dp, label 10sp weight 500
// ─────────────────────────────────────────────────────────────────────────────

enum class LifeOsTab(
    val label:        String,
    val iconFilled:   ImageVector,
    val iconOutlined: ImageVector,
) {
    Home(
        label        = "Home",
        iconFilled   = Icons.Filled.Home,
        iconOutlined = Icons.Outlined.Home,
    ),
    Finance(
        label        = "Finance",
        iconFilled   = Icons.Filled.AccountBalanceWallet,
        iconOutlined = Icons.Outlined.AccountBalanceWallet,
    ),
    Calendar(
        label        = "Calendar",
        iconFilled   = Icons.Filled.CalendarMonth,
        iconOutlined = Icons.Outlined.CalendarMonth,
    ),
    Assistant(
        label        = "AI",
        iconFilled   = Icons.Filled.AutoAwesome,
        iconOutlined = Icons.Outlined.AutoAwesome,
    ),
    Profile(
        label        = "Profile",
        iconFilled   = Icons.Filled.Person,
        iconOutlined = Icons.Outlined.Person,
    ),
}

@Composable
fun FloatingTabBar(
    selectedTab:  LifeOsTab,
    onTabSelect:  (LifeOsTab) -> Unit,
    modifier:     Modifier = Modifier,
) {
    val isDark  = LocalDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary

    // Bottom offset: max(navigationBarHeight, sm) + sm
    // windowInsetsPadding handles the navigation bar safe area automatically
    Box(
        modifier = modifier
            .padding(horizontal = Spacing.screenHorizontal)   // left/right 12dp
            .windowInsetsPadding(WindowInsets.navigationBars) // safe area
            .padding(bottom = Spacing.sm)                     // +8dp above nav bar
            .shadow(
                elevation        = 12.dp,
                shape            = ShapeXl,                   // borderRadius 24dp
                ambientColor     = Color.Black.copy(alpha = 0.35f),
                spotColor        = Color.Black.copy(alpha = 0.35f),
            )
            .clip(ShapeXl)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeXl)
            .height(TabBarDimens.height),                      // 58dp
    ) {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            LifeOsTab.entries.forEach { tab ->
                TabButton(
                    tab          = tab,
                    isSelected   = tab == selectedTab,
                    primary      = primary,
                    onTabSelect  = onTabSelect,
                    modifier     = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TabButton — single tab with spring scale + pill opacity animations
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabButton(
    tab:        LifeOsTab,
    isSelected: Boolean,
    primary:    Color,
    onTabSelect: (LifeOsTab) -> Unit,
    modifier:   Modifier = Modifier,
) {
    // Scale spring: friction=7, tension=90 → dampingRatio≈0.43, stiffness≈375
    val scale by animateFloatAsState(
        targetValue  = if (isSelected) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.43f, stiffness = 375f),
        label        = "tab_scale_${tab.name}",
    )

    // Pill opacity: tween 100ms (motion.fast)
    val pillAlpha by animateFloatAsState(
        targetValue  = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = Motion.fast),
        label        = "tab_pill_${tab.name}",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(alpha = 0.2f), bounded = false),
                onClick           = { onTabSelect(tab) },
            )
            .padding(vertical = Spacing.xs),   // paddingVertical: 4dp
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(2.dp),  // gap 2dp between icon and label
            modifier              = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        ) {
            // Active pill background (fades in/out behind icon)
            Box(contentAlignment = Alignment.Center) {
                // Pill background
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = pillAlpha),
                            shape = ShapePill,
                        )
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                ) {
                    // Icon
                    Icon(
                        imageVector        = if (isSelected) tab.iconFilled else tab.iconOutlined,
                        contentDescription = tab.label,
                        tint               = if (isSelected) {
                            primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier           = Modifier.size(TabBarDimens.iconSize),   // 24dp
                    )
                }
            }

            // Label
            Text(
                text       = tab.label,
                fontSize   = TabBarDimens.labelSize.sp,                // 10sp
                fontWeight = FontWeight.Medium,                         // weight 500
                color      = if (isSelected) {
                    primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign  = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines   = 1,
            )
        }
    }
}
