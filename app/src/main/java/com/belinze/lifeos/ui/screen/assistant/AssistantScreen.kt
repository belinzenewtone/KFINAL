package com.belinze.lifeos.ui.screen.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.AssistantViewModel
import com.belinze.lifeos.viewmodel.ChatMessage

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

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val state       by viewModel.uiState.collectAsState()
    val listState   = rememberLazyListState()
    var inputText   by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Auto-scroll to bottom on new message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding(),
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "Assistant",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (state.messages.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearConversation() }) {
                    Icon(
                        imageVector        = Icons.Filled.Clear,
                        contentDescription = "Clear conversation",
                        tint               = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    )
                }
            }
        }

        // ── Message list ─────────────────────────────────────────────────────
        LazyColumn(
            state               = listState,
            modifier            = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    // Empty state
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🤖", fontSize = 48.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(Spacing.md))
                            Text(
                                "Ask me about your finances,\ntasks, or upcoming events.",
                                style     = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color     = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                            )
                        }
                    }
                }
            } else {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message)
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

        // ── Input bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value             = inputText,
                onValueChange     = { inputText = it },
                modifier          = Modifier.weight(1f),
                placeholder       = { Text("Ask something…", color = MaterialTheme.colorScheme.onBackground.copy(0.40f)) },
                singleLine        = false,
                maxLines          = 4,
                keyboardOptions   = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions   = KeyboardActions(onSend = {
                    if (inputText.isNotBlank() && !state.isLoading) {
                        viewModel.updateInput(inputText.trim())
                        viewModel.sendMessage()
                        inputText = ""
                        focusManager.clearFocus()
                    }
                }),
                shape = RoundedCornerShape(16.dp),
            )
            IconButton(
                onClick  = {
                    if (inputText.isNotBlank() && !state.isLoading) {
                        viewModel.updateInput(inputText.trim())
                        viewModel.sendMessage()
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                Icon(
                    imageVector        = Icons.Filled.Send,
                    contentDescription = "Send",
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── Chat bubble ─────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isDark   = isSystemInDarkTheme()
    val isUser   = message.role == "user"

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary
                            else if (isDark) Color(0xFF1E1E2A) else Color(0xFFF0F4F8),
                    shape = RoundedCornerShape(
                        topStart     = 16.dp,
                        topEnd       = 16.dp,
                        bottomStart  = if (isUser) 16.dp else 4.dp,
                        bottomEnd    = if (isUser) 4.dp else 16.dp,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text  = message.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ─── Typing indicator ─────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    Row(horizontalArrangement = Arrangement.Start) {
        Box(
            modifier         = Modifier
                .background(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E1E2A) else Color(0xFFF0F4F8),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier  = Modifier.size(16.dp),
                color     = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
    }
}
