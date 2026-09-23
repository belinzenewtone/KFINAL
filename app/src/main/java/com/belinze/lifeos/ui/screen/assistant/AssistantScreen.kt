package com.belinze.lifeos.ui.screen.assistant

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.ui.theme.TabBarDimens
import com.belinze.lifeos.viewmodel.AssistantViewModel
import com.belinze.lifeos.viewmodel.ChatMessage
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// AssistantScreen
//
// 1:1 port of src/screens/assistant/AssistantScreen.tsx.
//
// Layout:
//   ‣ Header: "Assistant" + Clear conversation button
//   ‣ Message list (LazyColumn, newest at bottom)
//   ‣ Empty state when no messages
//   ‣ Input bar: TextField + Send button
//   ‣ IME padding (adjusts when keyboard opens)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val state       by viewModel.uiState.collectAsStateWithLifecycle()
    val quickSuggestionsEnabled by viewModel.quickSuggestionsEnabled.collectAsStateWithLifecycle()
    val listState   = rememberLazyListState()
    var inputText   by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun send(text: String) {
        if (text.isBlank() || state.isLoading) return
        viewModel.updateInput(text.trim())
        viewModel.sendMessage()
        inputText = ""
        focusManager.clearFocus()
    }

    // Auto-scroll to bottom on new message count
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }
    // AS-2: also scroll when last message content changes (e.g. streaming updates)
    LaunchedEffect(state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // When the keyboard is visible, imePadding() on the Column already lifts everything
    // above the IME — we must NOT add the static tab-bar offset on top of that or the
    // input bar ends up TabBarDimens.height above the keyboard (too high).
    // When the keyboard is hidden, add tab-bar height + a 6 dp hairline gap so the input
    // bar sits clearly above the floating tab bar without touching it.
    val imeVisible = WindowInsets.isImeVisible
    val bottomPad = if (imeVisible) 0.dp else TabBarDimens.height + 6.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
            .padding(bottom = bottomPad),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text  = if (state.isLoading) "Thinking…" else "Offline · Rule-based",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.isLoading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (state.messages.isNotEmpty()) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(
                        imageVector        = Icons.Outlined.DeleteOutline,
                        contentDescription = "Clear conversation",
                        tint               = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title            = { Text("Clear chat history?") },
                text             = { Text("This will remove your current conversation and start a fresh one.") },
                confirmButton    = {
                    TextButton(onClick = {
                        showClearConfirm = false
                        viewModel.clearConversation()
                    }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                },
            )
        }

        // ── Message list ─────────────────────────────────────────────────────
        LazyColumn(
            state               = listState,
            modifier            = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding      = PaddingValues(bottom = Spacing.bottomNavSafeArea),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.x3l, bottom = Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0x20 / 255f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(Modifier.height(Spacing.base))
                        Text(
                            "Ask me anything",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            "I can check your spending, income, budgets, tasks, and transactions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message, onActionPress = ::send)
                }
            }

            // AS-3: show quick suggestions until the conversation has more than 1 message.
            // Matches React SuggestedPrompts — only the first 3 of the default list are shown.
            if (quickSuggestionsEnabled && state.messages.size <= 1) {
                item {
                    val prompts = listOf(
                        "How much did I spend this week?",
                        "What is my balance?",
                        "Show my budgets",
                        "What tasks are due today?",
                        "Recent transactions",
                        "Summarize my spending",
                    ).take(3)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Try asking:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.xs),
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(prompts, key = { it }) { prompt ->
                                AssistChip(
                                    onClick = { send(prompt) },
                                    label = { Text(prompt, maxLines = 1) },
                                    modifier = Modifier.wrapContentWidth(),
                                )
                            }
                        }
                    }
                }
            }

            // Typing indicator
            if (state.isLoading) {
                item {
                    TypingIndicator()
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }

        // ── Input bar — single pill control with a trailing send/spinner adornment ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val canSend = inputText.isNotBlank() && !state.isLoading
            OutlinedTextField(
                value             = inputText,
                onValueChange     = { inputText = it },
                modifier          = Modifier.fillMaxWidth(),
                placeholder       = { Text("Message LifeOS…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine        = false,
                maxLines          = 4,
                keyboardOptions   = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions   = KeyboardActions(onSend = { send(inputText) }),
                // AS-5: pill shape to match RN ChatInput component
                shape = RoundedCornerShape(9999.dp),
                trailingIcon = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        IconButton(onClick = { send(inputText) }, enabled = canSend) {
                            Icon(
                                imageVector        = Icons.Outlined.Send,
                                contentDescription = "Send",
                                tint               = if (canSend) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            )
                        }
                    }
                },
            )
        }
    }
}

// ─── Chat bubble ─────────────────────────────────────────────────────────────

private val CHAT_TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun ChatBubble(message: ChatMessage, onActionPress: (String) -> Unit) {
    val isUser = message.role == "user"
    val shape = RoundedCornerShape(
        topStart    = 20.dp,
        topEnd      = 20.dp,
        bottomStart = if (isUser) 20.dp else 4.dp,
        bottomEnd   = if (isUser) 4.dp else 20.dp,
    )
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val textColor   = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val timeColor   = if (isUser) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timeLabel = remember(message.createdAt) {
        runCatching { OffsetDateTime.parse(message.createdAt).format(CHAT_TIME_FMT) }.getOrDefault("")
    }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            modifier             = Modifier.widthIn(max = 280.dp),
            verticalAlignment    = Alignment.Bottom,
        ) {
            // AS-6: bot avatar next to assistant messages, bottom-aligned with the bubble
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                Box(
                    modifier = Modifier
                        .border(1.dp, borderColor, shape)
                        .background(bubbleColor, shape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text(
                            text       = message.content,
                            color      = textColor,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isUser) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (timeLabel.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(timeLabel, style = MaterialTheme.typography.bodySmall, color = timeColor)
                            }
                        }
                    }
                }
                // AS-1: interactive action chips for assistant messages — tapping one
                // sends its text through the same pipeline as manual input.
                if (!isUser && message.actions.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.wrapContentWidth(),
                    ) {
                        message.actions.forEach { action ->
                            AssistChip(
                                onClick = { onActionPress(action) },
                                label   = { Text(action, style = MaterialTheme.typography.bodySmall) },
                                colors  = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor     = MaterialTheme.colorScheme.primary,
                                ),
                                border  = AssistChipDefaults.assistChipBorder(
                                    enabled     = true,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Typing indicator (3-dot bounce) ─────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infinite = rememberInfiniteTransition(label = "typing")
    // Stagger three dots: 0ms, 160ms, 320ms
    val offsets = (0..2).map { i ->
        infinite.animateFloat(
            initialValue = 0f,
            targetValue  = -6f,
            animationSpec = infiniteRepeatable(
                animation  = tween(400, delayMillis = i * 160, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot$i",
        )
    }

    // AS-4: wrap dots + "Thinking…" label in a bordered pill, matching React's TypingIndicator
    Row(horizontalArrangement = Arrangement.Start) {
        Box(
            modifier         = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    offsets.forEach { anim ->
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                                .then(Modifier.offset(y = anim.value.dp)),
                        )
                    }
                }
                Text(
                    text  = "Thinking…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
