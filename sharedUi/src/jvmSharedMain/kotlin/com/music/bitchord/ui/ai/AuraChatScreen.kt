package com.music.bitchord.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.ai.AuraActionRef
import com.music.bitchord.data.ai.AuraAiPersona
import com.music.bitchord.data.ai.AuraAiService
import com.music.bitchord.data.ai.AuraChatMessage
import com.music.bitchord.data.ai.AuraUserBehaviorProfile
import com.music.bitchord.data.ai.ChatSender
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Flagship Aura AI Companion Chat Screen.
 * Fully aligned with BitChord's native design system across Dark, Light, and Nothing OS modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraChatScreen(
    userProfile: AuraUserBehaviorProfile,
    currentSong: Song?,
    isPlaying: Boolean,
    initialPersona: AuraAiPersona = AuraAiPersona.COMPANION,
    customGrokKey: String? = null,
    customGeminiKey: String? = null,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onPlaySong: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    onPlayMix: (List<Song>) -> Unit,
    onQueueMix: (List<Song>) -> Unit,
    onOpenEq: () -> Unit,
    onOpenParty: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenReplay: () -> Unit,
    onOpenRadar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activePersona by remember { mutableStateOf(initialPersona) }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var showPersonaMenu by remember { mutableStateOf(false) }
    var isMiniPlayerDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(currentSong?.videoId) {
        isMiniPlayerDismissed = false
    }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > 0

    val initialWelcomeMessage = remember {
        AuraChatMessage(
            id = "welcome",
            sender = ChatSender.AURA_AI,
            text = "✨ **Welcome to Aura AI** — your acoustic companion and music intelligence.\n\n" +
                    "I can identify songs from brief lyrics, curate personalized mixes, analyze your acoustic habits, and answer questions about AuraMusic.\n\n" +
                    "Tap a suggestion below or type anything to get started.",
            timestamp = System.currentTimeMillis(),
            actionChips = listOf(
                AuraActionRef("ANALYZE_HABITS", "🧠 Analyze My Music"),
                AuraActionRef("ROAST_TASTE", "🔥 Roast My Taste"),
                AuraActionRef("CREATOR_INFO", "👨‍💻 About AuraMusic"),
            ),
        )
    }

    val messages = remember {
        mutableStateListOf(initialWelcomeMessage)
    }

    // Auto-scroll on new message or thinking state change, ensuring thinking indicator is visible
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            val targetIndex = if (isThinking) messages.size else (messages.size - 1)
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Auto-scroll when keyboard opens so recent messages stay visible above input bar
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && messages.isNotEmpty()) {
            val targetIndex = if (isThinking) messages.size else (messages.size - 1)
            listState.animateScrollToItem(targetIndex)
        }
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank() || isThinking) return
        val userMsg = AuraChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.USER,
            text = prompt.trim(),
            timestamp = System.currentTimeMillis(),
        )
        messages.add(userMsg)
        inputText = ""
        keyboardController?.hide()
        isThinking = true

        scope.launch {
            try {
                // Prior turns excluding the current user message just added
                val history = messages
                    .dropLast(1)
                    .takeLast(6)
                    .map { (if (it.sender == ChatSender.USER) "user" else "assistant") to it.text }

                val aiMsg = AuraAiService.sendMessage(
                    userPrompt = prompt,
                    profile = userProfile,
                    persona = activePersona,
                    currentSong = currentSong,
                    history = history,
                    customGrokKey = customGrokKey,
                    customGeminiKey = customGeminiKey,
                )
                messages.add(aiMsg)
            } catch (e: Exception) {
                messages.add(
                    AuraChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.AURA_AI,
                        text = "I encountered an acoustic anomaly. Re-routing to local offline intelligence.\n\n" +
                                "Ask me about your music habits, lyrics, or app features anytime!",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            } finally {
                isThinking = false
            }
        }
    }

    fun handleAction(action: AuraActionRef) {
        when {
            action.type == "ANALYZE_HABITS" -> sendPrompt("Analyze my listening habits today and explain my chronotype.")
            action.type == "ROAST_TASTE" -> sendPrompt("Roast my music taste based on my listening history.")
            action.type == "CREATOR_INFO" -> sendPrompt("Who created this app and how can I connect with the developer?")
            action.type == "OPEN_EQ" -> onOpenEq()
            action.type == "OPEN_PARTY" -> onOpenParty()
            action.type == "OPEN_SLEEP_TIMER" -> onOpenSleepTimer()
            action.type == "OPEN_REPLAY" -> onOpenReplay()
            action.type.startsWith("OPEN_URL:") || action.type == "OPEN_URL" -> {
                val rawUrl = action.payload ?: action.type.removePrefix("OPEN_URL:").trim()
                if (rawUrl.isNotBlank()) {
                    runCatching {
                        val url = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                            "https://$rawUrl"
                        } else rawUrl
                        uriHandler.openUri(url)
                    }
                }
            }
            action.type.startsWith("SEARCH_SONG:") || action.type == "SEARCH_SONG" -> {
                val q = action.payload ?: action.type.removePrefix("SEARCH_SONG:").trim()
                sendPrompt("Recommend songs similar to $q")
            }
            else -> sendPrompt(action.label)
        }
    }

    // Root Surface uses BitChord's native background
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraOrbIcon(
                        size = 34.dp,
                        isThinking = isThinking,
                        isPlaying = isPlaying,
                    )

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Aura AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.width(6.dp))
                            // Version badge
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                ),
                            ) {
                                Text(
                                    text = "2.0 FLASH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                )
                            }
                        }

                        // Persona Selector Pill
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showPersonaMenu = true }
                                    .padding(vertical = 1.dp),
                            ) {
                                Text(
                                    text = "Persona: ${activePersona.displayName} ▾",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                )
                            }

                            DropdownMenu(
                                expanded = showPersonaMenu,
                                onDismissRequest = { showPersonaMenu = false },
                            ) {
                                AuraAiPersona.entries.forEach { persona ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(persona.displayName, fontWeight = FontWeight.Bold)
                                                Text(persona.subtitle, style = MaterialTheme.typography.bodySmall)
                                            }
                                        },
                                        onClick = {
                                            activePersona = persona
                                            showPersonaMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Header Action Buttons: Clear Chat, Radar, Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.add(
                                AuraChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    sender = ChatSender.AURA_AI,
                                    text = "✨ **Chat reset.** Ask me about songs, lyrics, habit analysis, or app features anytime!",
                                    timestamp = System.currentTimeMillis(),
                                    actionChips = listOf(
                                        AuraActionRef("ANALYZE_HABITS", "🧠 Analyze My Music"),
                                        AuraActionRef("ROAST_TASTE", "🔥 Roast My Taste"),
                                        AuraActionRef("CREATOR_INFO", "👨‍💻 About AuraMusic"),
                                    ),
                                )
                            )
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    IconButton(
                        onClick = onOpenRadar,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Aura Radar (Song Recognition)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Aura Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Divider matching app style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )

            // --- CHAT FEED ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    AuraChatBubble(
                        message = msg,
                        currentPlayingSong = currentSong,
                        isPlaying = isPlaying,
                        onPlaySong = onPlaySong,
                        onQueueSong = onQueueSong,
                        onPlayMix = onPlayMix,
                        onQueueMix = onQueueMix,
                        onActionClick = ::handleAction,
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(text = "●", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = "Aura is thinking...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val bottomDockModifier = if (isKeyboardOpen) {
                Modifier.imePadding()
            } else {
                Modifier.navigationBarsPadding()
            }

            // --- BOTTOM DOCKING CONTAINER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(bottomDockModifier),
            ) {
                // Quick prompt chips — cleanly hidden when typing or keyboard is open to maximize chat space
                AnimatedVisibility(
                    visible = inputText.isEmpty() && !isKeyboardOpen,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        QuickPromptChip(label = "🧠 Analyze My Habits") {
                            sendPrompt("Analyze my listening habits today and explain my chronotype.")
                        }
                        QuickPromptChip(label = "🔥 Roast My Taste") {
                            sendPrompt("Roast my music taste based on my listening history.")
                        }
                        QuickPromptChip(label = "👨‍💻 About AuraMusic") {
                            sendPrompt("Who created this app and how can I connect with the developer?")
                        }
                        QuickPromptChip(label = "🎵 Telugu Lyrics Search") {
                            sendPrompt("Find Telugu song with lyric: 'kallara chusedhi'")
                        }
                        QuickPromptChip(label = "⚡ Late Night Flow") {
                            sendPrompt("Generate a late night ambient chill mix of 5 songs.")
                        }
                        QuickPromptChip(label = "🎛️ Studio EQ Tips") {
                            sendPrompt("How do I use the 10-Band Studio EQ for bass heavy music?")
                        }
                    }
                }

                // Currently Playing mini-player strip
                if (currentSong != null && !isMiniPlayerDismissed) {
                    AuraChatMiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        onTogglePlayPause = onTogglePlayPause,
                        onClick = {
                            sendPrompt("Tell me interesting facts, musical director info, and vibe breakdown of currently playing track '${currentSong.title}' by ${currentSong.artist}.")
                        },
                        onDismiss = { isMiniPlayerDismissed = true },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }

                // --- INPUT BAR ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Text field container
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 0.5.dp,
                                color = if (inputText.isNotBlank())
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(24.dp),
                            )
                            .clickable {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Ask Aura, search lyrics, or analyze taste...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 13.5.sp,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    sendPrompt(inputText)
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            )
                        }

                        if (inputText.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { inputText = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Radar / mic button
                    IconButton(
                        onClick = onOpenRadar,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Song Recognition (Aura Radar)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Send button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .border(
                                width = 0.5.dp,
                                color = if (inputText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable(enabled = inputText.isNotBlank() && !isThinking) {
                                sendPrompt(inputText)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank())
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPromptChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}
