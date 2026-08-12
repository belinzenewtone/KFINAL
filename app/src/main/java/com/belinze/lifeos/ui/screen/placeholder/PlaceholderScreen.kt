package com.belinze.lifeos.ui.screen.placeholder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Temporary placeholder composable.
 *
 * Keeps the build compiling while real screen implementations are developed
 * in Phase 5. Removed screen-by-screen as each screen is written.
 *
 * @param label   Displayed in the centre of the screen.
 * @param onBack  If non-null, shows a back arrow at top-left for navigation.
 */
@Composable
fun PlaceholderScreen(
    label:  String,
    onBack: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Back button
            if (onBack != null) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            // Label
            Text(
                text      = label,
                style     = MaterialTheme.typography.titleLarge,
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier  = Modifier.align(Alignment.Center),
            )
        }
    }
}
