package com.music.bitchord.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.collection.LruCache
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.listentogether.PartyChatMessage
import com.music.bitchord.data.listentogether.PartySongRecommendation
import com.music.bitchord.data.listentogether.PartySoundbite
import com.music.bitchord.data.listentogether.PartyTrack
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.FloatingReactionOverlay
import com.music.bitchord.ui.components.PartyReactionQuickBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PartyChatSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by ListenTogether.chatMessages.collectAsStateWithLifecycle()
    val state by ListenTogether.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showDevModal by ListenTogether.showDevModal.collectAsStateWithLifecycle()
    val showDevConsole by ListenTogether.showDevConsole.collectAsStateWithLifecycle()
    val activeGodAnnouncement by ListenTogether.godAnnouncement.collectAsStateWithLifecycle()
    val typingUsers by ListenTogether.typingUsers.collectAsStateWithLifecycle()
    var filterJukeboxOnly by remember { mutableStateOf(false) }
    var selectedLightboxImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<PartyChatMessage?>(null) }
    val context = LocalContext.current
    var replyingTo by remember { mutableStateOf<PartyChatMessage?>(null) }
    var inputText by remember { mutableStateOf("") }
    var isCompressingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val caption = inputText.trim()
                isCompressingPhoto = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val base64 = ListenTogether.compressImageUriToBase64(context, uri)
                        if (base64 != null) {
                            withContext(Dispatchers.Main) {
                                inputText = ""
                            }
                            ListenTogether.sendChatMessage(
                                text = caption,
                                imageUrl = base64,
                                replyTo = replyingTo,
                            )
                            replyingTo = null
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Could not load photo. Please try another.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (t: Throwable) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Photo error: ${t.localizedMessage ?: "Failed"}", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        isCompressingPhoto = false
                    }
                }
            }
        }
    )

    var isSearchDrawerOpen by remember { mutableStateOf(false) }
    var isSoundboardOpen by remember { mutableStateOf(false) }
    var activeSoundbiteToast by remember { mutableStateOf<PartySoundbite?>(null) }

    // Clear unread badge when chat sheet is open
    LaunchedEffect(Unit) {
        ListenTogether.clearUnreadChat()
    }

    // Real-time soundbites visual banner toast (audio playback is handled globally by PartySync)
    LaunchedEffect(Unit) {
        ListenTogether.soundbites.collect { bite ->
            activeSoundbiteToast = bite
            delay(2800)
            if (activeSoundbiteToast == bite) {
                activeSoundbiteToast = null
            }
        }
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.90f)
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 8.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Party Chat & Jukebox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val listeningCount = state.members.count { it.connected }.takeIf { it > 0 } ?: state.members.size
                        Text(
                            text = "$listeningCount listening",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text = "Recommend songs & vote in real-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Filter Tabs: All vs Jukebox Only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (!filterJukeboxOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable { filterJukeboxOnly = false }
            ) {
                Text(
                    text = "All (${messages.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (!filterJukeboxOnly) FontWeight.Bold else FontWeight.Normal,
                    color = if (!filterJukeboxOnly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            val jukeboxCount = messages.count { it.song != null }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (filterJukeboxOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable { filterJukeboxOnly = true }
            ) {
                Text(
                    text = "🎵 Jukebox Only ($jukeboxCount)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (filterJukeboxOnly) FontWeight.Bold else FontWeight.Normal,
                    color = if (filterJukeboxOnly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (state.you?.isFounder == true || state.you?.displayName.equals("likki", ignoreCase = true)) {
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.7f)),
                    modifier = Modifier.clickable { ListenTogether.setShowDevConsole(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "DEV CONSOLE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB300)
                        )
                    }
                }
            }
        }

        // Real-time God Announcement Alert Banner
        if (activeGodAnnouncement != null) {
            val announcement = activeGodAnnouncement!!
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1705),
                border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFB300).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ GOD ANNOUNCEMENT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFB300),
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "from ${announcement.senderName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                        Text(
                            text = announcement.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = { ListenTogether.dismissGodAnnouncement() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Pinned Announcement Banner
        val pinnedMessage = messages.lastOrNull { it.isPinned }
        if (pinnedMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        coroutineScope.launch {
                            val idx = messages.indexOfFirst { it.id == pinnedMessage.id }
                            if (idx >= 0) listState.animateScrollToItem(idx)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📌", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pinned Announcement · ${pinnedMessage.senderName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = pinnedMessage.text.ifBlank { pinnedMessage.song?.title.orEmpty() },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (state.you?.isHost == true) {
                        IconButton(
                            onClick = { ListenTogether.pinChatMessage(pinnedMessage.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Unpin",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Inline Search Drawer or Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val displayMessages = if (filterJukeboxOnly) messages.filter { it.song != null } else messages
            if (displayMessages.isEmpty() && !isSearchDrawerOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (filterJukeboxOnly) "No music recommendations yet" else "No messages yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (filterJukeboxOnly) "Tap the 🎵 note below to suggest a track to everyone!"
                        else "Send a text, share a photo, drop a soundbite, or tap the 🎵 note below to suggest a track to everyone!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == state.you?.memberId
                        val senderMember = state.members.firstOrNull { it.memberId == msg.senderId }
                        val senderIsHost = senderMember?.isHost == true
                        val senderIsAux = msg.senderId == state.auxMemberId
                        val senderIsFounder = msg.isFounder || senderMember?.isFounder == true || msg.senderName.equals("likki", ignoreCase = true)
                        ChatMessageBubble(
                            message = msg,
                            isMe = isMe,
                            myMemberId = state.you?.memberId,
                            isHost = state.you?.isHost == true,
                            senderIsHost = senderIsHost,
                            senderIsAux = senderIsAux,
                            senderIsFounder = senderIsFounder,
                            onImageClick = { selectedLightboxImageUrl = it },
                            onPlayNext = { rec ->
                                val track = PartyTrack(
                                    videoId = rec.videoId,
                                    title = rec.title,
                                    artist = rec.artist,
                                    thumbnailUrl = rec.coverUrl,
                                    durationMs = rec.durationSec?.let { it * 1000L },
                                )
                                ListenTogether.queueAdd(listOf(track), playNext = true)
                            },
                            onAddToQueue = { rec ->
                                val track = PartyTrack(
                                    videoId = rec.videoId,
                                    title = rec.title,
                                    artist = rec.artist,
                                    thumbnailUrl = rec.coverUrl,
                                    durationMs = rec.durationSec?.let { it * 1000L },
                                )
                                ListenTogether.queueAdd(listOf(track), playNext = false)
                            },
                            onUpvote = {
                                ListenTogether.upvoteChatMessage(msg.id)
                            },
                            onReply = { target ->
                                replyingTo = target
                            },
                            onReact = { messageId, emoji ->
                                ListenTogether.reactToChatMessage(messageId, emoji)
                            },
                            onPin = if (state.you?.isHost == true) { id ->
                                ListenTogether.pinChatMessage(id)
                            } else null,
                            onScrollToReply = { replyId ->
                                coroutineScope.launch {
                                    val idx = displayMessages.indexOfFirst { it.id == replyId }
                                    if (idx >= 0) listState.animateScrollToItem(idx)
                                }
                            },
                            onLongClick = { selectedMessageForMenu = it },
                        )
                    }
                }
            }

            // Inline YouTube Music Search Overlay
            if (isSearchDrawerOpen) {
                PartySongSearchDrawer(
                    onClose = { isSearchDrawerOpen = false },
                    onSelectSong = { song ->
                        val rec = PartySongRecommendation(
                            videoId = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            coverUrl = song.thumbnailUrl,
                            durationSec = null,
                        )
                        ListenTogether.sendChatMessage(
                            text = "🎶 Suggested: ${song.title}",
                            song = rec,
                            replyTo = replyingTo,
                        )
                        replyingTo = null
                        isSearchDrawerOpen = false
                    },
                )
            }

            // Real-time floating reaction animation layer
            FloatingReactionOverlay()

            // Real-time Soundbite Toast Overlay
            SoundbiteToastOverlay(
                activeSoundbiteToast = activeSoundbiteToast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        // Live Party Soundboard Bar
        AnimatedVisibility(visible = isSoundboardOpen) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PartySoundbiteAudio.SOUNDBITES.forEach { sb ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    PartySoundbiteAudio.markLocalPlay(sb.key)
                                    PartySoundbiteAudio.play(sb.key)
                                    ListenTogether.sendSoundbite(sb.key)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(sb.emoji, fontSize = 20.sp)
                            Text(
                                text = sb.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Reaction Emoji Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            PartyReactionQuickBar()
        }

        // Quoted Reply Preview Bar
        AnimatedVisibility(visible = replyingTo != null) {
            replyingTo?.let { target ->
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${target.senderName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = target.text.ifBlank { target.song?.title ?: "Recommendation" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { replyingTo = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // Share Now Playing Quick Action Chip
        val currentTrack = state.playback.track
        if (currentTrack != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .clickable {
                        ListenTogether.sendChatMessage(
                            text = "Now playing 🎧",
                            song = PartySongRecommendation(
                                videoId = currentTrack.videoId,
                                title = currentTrack.title,
                                artist = currentTrack.artist,
                                coverUrl = currentTrack.thumbnailUrl,
                                durationSec = currentTrack.durationMs?.let { (it / 1000).toInt() }
                            )
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎧", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Share Now Playing:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${currentTrack.title} · ${currentTrack.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Live Typing Indicator Bar
        if (typingUsers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✏️", fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                val typingText = if (typingUsers.size == 1) {
                    "${typingUsers.first()} is typing..."
                } else {
                    "${typingUsers.take(2).joinToString(" & ")} are typing..."
                }
                Text(
                    text = typingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Photo Compressing / Sending Indicator
        if (isCompressingPhoto) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sending photo...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Input Row
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1-Tap Share Now Playing Button
                val currentPartyTrack = state.playback.track
                if (currentPartyTrack != null) {
                    IconButton(
                        onClick = {
                            ListenTogether.sendChatMessage(
                                text = "Now playing 🎧",
                                song = PartySongRecommendation(
                                    videoId = currentPartyTrack.videoId,
                                    title = currentPartyTrack.title,
                                    artist = currentPartyTrack.artist,
                                    coverUrl = currentPartyTrack.thumbnailUrl,
                                    durationSec = currentPartyTrack.durationMs?.let { (it / 1000).toInt() },
                                )
                            )
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = "Share Now Playing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Song Suggestion Button
                IconButton(
                    onClick = { isSearchDrawerOpen = !isSearchDrawerOpen },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSearchDrawerOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Suggest Song",
                        tint = if (isSearchDrawerOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Soundboard Button
                IconButton(
                    onClick = { isSoundboardOpen = !isSoundboardOpen },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSoundboardOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Soundboard",
                        tint = if (isSoundboardOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Photo Attachment Button
                IconButton(
                    onClick = {
                        if (!isCompressingPhoto) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    if (isCompressingPhoto) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = "Attach Photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Text Field
                BasicTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        val wasEmpty = inputText.isEmpty()
                        inputText = newValue
                        if (wasEmpty != newValue.isEmpty()) {
                            ListenTogether.sendTyping(newValue.isNotEmpty())
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                ListenTogether.sendChatMessage(
                                    text = inputText.trim(),
                                    replyTo = replyingTo,
                                )
                                inputText = ""
                                replyingTo = null
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (replyingTo != null) "Type reply..." else "Message room or tap 🎵...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            ListenTogether.sendChatMessage(
                                text = inputText.trim(),
                                replyTo = replyingTo,
                            )
                            inputText = ""
                            replyingTo = null
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }

    // Photo Lightbox Fullscreen Dialog with Pinch-to-Zoom & Pan
    if (selectedLightboxImageUrl != null) {
        val currentImageUrl = selectedLightboxImageUrl
        Dialog(
            onDismissRequest = { selectedLightboxImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val bitmapState = produceState<Bitmap?>(
                initialValue = currentImageUrl?.let { PartyChatBitmapCache.get(it) },
                key1 = currentImageUrl
            ) {
                if (value == null && currentImageUrl != null) {
                    value = withContext(Dispatchers.IO) {
                        PartyChatBitmapCache.load(currentImageUrl)
                    }
                }
            }
            val bitmap = bitmapState.value
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    selectedLightboxImageUrl = null
                                }
                            },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full photo preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .fillMaxHeight(0.82f)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    if (scale > 1f) {
                                        offset += pan
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            }
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = Color.White
                    )
                }

                // Top Controls Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (scale > 1f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Text("1x", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentImageUrl != null) {
                                coroutineScope.launch {
                                    val saved = saveChatImageToGallery(context, currentImageUrl)
                                    Toast.makeText(
                                        context,
                                        if (saved) "Photo saved to Pictures/AuraMusic" else "Failed to save photo",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Save photo",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            if (currentImageUrl != null) {
                                coroutineScope.launch {
                                    shareChatImage(context, currentImageUrl)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share photo",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { selectedLightboxImageUrl = null },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close preview",
                            tint = Color.White
                        )
                    }
                }

                // Bottom Action Pill Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                if (currentImageUrl != null) {
                                    coroutineScope.launch {
                                        val saved = saveChatImageToGallery(context, currentImageUrl)
                                        Toast.makeText(
                                            context,
                                            if (saved) "Photo saved to Pictures/AuraMusic" else "Failed to save photo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    Row(
                        modifier = Modifier
                            .clickable {
                                if (currentImageUrl != null) {
                                    coroutineScope.launch {
                                        shareChatImage(context, currentImageUrl)
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Message Actions Sheet for long-pressed chat message
    selectedMessageForMenu?.let { msg ->
        val isMe = msg.senderId == state.you?.memberId
        MessageActionsSheet(
            message = msg,
            isMe = isMe,
            isHost = state.you?.isHost == true,
            onDismiss = { selectedMessageForMenu = null },
            onReply = {
                replyingTo = msg
            },
            onCopyText = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Chat Message", msg.text))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onPinToggle = {
                ListenTogether.pinChatMessage(msg.id)
            },
            onDelete = {
                ListenTogether.deleteChatMessage(msg.id)
                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
            },
            onSaveImage = {
                val img = msg.imageUrl
                if (!img.isNullOrBlank()) {
                    coroutineScope.launch {
                        val saved = saveChatImageToGallery(context, img)
                        Toast.makeText(
                            context,
                            if (saved) "Photo saved to Pictures/AuraMusic" else "Failed to save photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onReact = { emoji ->
                ListenTogether.reactToChatMessage(msg.id, emoji)
            }
        )
    }

    // Developer Terminal Modal for likhith3035. secret access
    if (showDevModal) {
        var devDob by remember { mutableStateOf("") }
        var devNickname by remember { mutableStateOf("likki") }
        var devConfirm by remember { mutableStateOf("") }
        var devError by remember { mutableStateOf<String?>(null) }

        Dialog(
            onDismissRequest = {
                ListenTogether.setShowDevModal(false)
            }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF13151F),
                border = BorderStroke(1.5.dp, Color(0xFFFFB300).copy(alpha = 0.6f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Developer Terminal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Enter developer authorization to elevate host privileges",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = devDob,
                        onValueChange = { devDob = it; devError = null },
                        label = { Text("Date of Birth (DD MM YYYY)") },
                        placeholder = { Text("29 07 2005") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFFB300),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = devNickname,
                        onValueChange = { devNickname = it; devError = null },
                        label = { Text("Developer Nickname") },
                        placeholder = { Text("likki") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFFB300),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = devConfirm,
                        onValueChange = { devConfirm = it; devError = null },
                        label = { Text("Type 'iam' to host") },
                        placeholder = { Text("iam") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFFB300),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (devError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = devError.orEmpty(),
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { ListenTogether.setShowDevModal(false) }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val ok = ListenTogether.devElevate(devDob, devNickname, devConfirm)
                                if (!ok) {
                                    devError = "Verification failed. Check DOB & confirm."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB300),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("👑 Elevate to Host", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDevConsole) {
        Dialog(
            onDismissRequest = {
                ListenTogether.dismissDevConsole()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
            ) {
                PartyDevConsoleSheet(onDismiss = { ListenTogether.dismissDevConsole() })
            }
        }
    }
}

@Composable
private fun ChatImageCard(
    imageUrl: String,
    onImageClick: (String) -> Unit,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bitmapState = produceState<Bitmap?>(initialValue = PartyChatBitmapCache.get(imageUrl), key1 = imageUrl) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                PartyChatBitmapCache.load(imageUrl)
            }
        }
    }
    val bitmap = bitmapState.value

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(imageUrl) {
                detectTapGestures(
                    onTap = { onImageClick(imageUrl) },
                    onDoubleTap = { onDoubleTap() }
                )
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Shared photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 240.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: PartyChatMessage,
    isMe: Boolean,
    myMemberId: String?,
    isHost: Boolean,
    senderIsHost: Boolean = false,
    senderIsAux: Boolean = false,
    senderIsFounder: Boolean = false,
    onImageClick: (String) -> Unit = {},
    onPlayNext: (PartySongRecommendation) -> Unit,
    onAddToQueue: (PartySongRecommendation) -> Unit,
    onUpvote: () -> Unit,
    onReply: (PartyChatMessage) -> Unit,
    onReact: (String, String) -> Unit,
    onPin: ((String) -> Unit)?,
    onScrollToReply: (String) -> Unit,
    onLongClick: (PartyChatMessage) -> Unit = {},
) {
    val context = LocalContext.current
    val hasUpvoted = myMemberId != null && message.upvoterIds.contains(myMemberId)
    var showEmojiDock by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMe) {
            if (message.senderAvatar != null) {
                AsyncImage(
                    model = message.senderAvatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = if (message.song != null) Modifier.fillMaxWidth(0.86f) else Modifier.widthIn(max = 290.dp),
        ) {
            // Sender name & role badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
            ) {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (senderIsHost) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = "👑 Host",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (senderIsAux) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = "⚡ Aux DJ",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (senderIsFounder) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFF4500).copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = "🔥 Founder",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Message Bubble Card
            @OptIn(ExperimentalFoundationApi::class)
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp,
                ),
                color = if (isMe) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp,
                        )
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onLongClick(message) },
                    ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Pinned Indicator Badge
                    if (message.isPinned) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text("📌", fontSize = 11.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Pinned",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Quoted Reply Preview Header
                    if (message.replyToSender != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable {
                                    message.replyToId?.let { onScrollToReply(it) }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "↩ ${message.replyToSender}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = message.replyToText.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }

                    // Attached Photo
                    val photoUrl = message.imageUrl
                    if (!photoUrl.isNullOrBlank()) {
                        ChatImageCard(
                            imageUrl = photoUrl,
                            onImageClick = onImageClick,
                            onDoubleTap = { onReact(message.id, "❤️") },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Song Recommendation Card
                    message.song?.let { song ->
                        if (message.text.isNotBlank()) Spacer(Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (song.coverUrl != null) {
                                        AsyncImage(
                                            model = song.coverUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (message.autoQueued) {
                                            Spacer(Modifier.height(3.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            ) {
                                                Text(
                                                    text = "🗳️ Auto-Queued by Democracy",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Action Buttons Row: [▶ Next] [+ Queue] [👍 Vote]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Play Next button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onPlayNext(song) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = "Next",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }

                                    // Add to Queue button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onAddToQueue(song) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.QueueMusic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = "+Queue",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    // Upvote Button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (hasUpvoted) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .clickable { onUpvote() },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ThumbUp,
                                                contentDescription = null,
                                                tint = if (hasUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (message.upvotes > 0) "${message.upvotes}" else "Vote",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (hasUpvoted) FontWeight.Bold else FontWeight.Medium,
                                                color = if (hasUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Human-readable Relative Timestamp
                    Spacer(Modifier.height(2.dp))
                    val timeFormatted = remember(message.timestampMs) {
                        val rel = formatRelativeTime(message.timestampMs)
                        val clockTime = DateFormat.format("h:mm a", message.timestampMs).toString()
                        if (rel == "Just now" || rel.endsWith("m ago")) rel else "$rel · $clockTime"
                    }
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            // Reaction Chips & Action Buttons Row
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, start = if (isMe) 0.dp else 4.dp, end = if (isMe) 4.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            ) {
                // Reaction clusters
                message.reactions.forEach { (emoji, members) ->
                    if (members.isNotEmpty()) {
                        val userReacted = myMemberId != null && members.contains(myMemberId)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (userReacted) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (userReacted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { onReact(message.id, emoji) }
                        ) {
                            Text(
                                text = "$emoji ${members.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = if (userReacted) FontWeight.Bold else FontWeight.Medium,
                                color = if (userReacted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Reaction trigger (+)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { showEmojiDock = !showEmojiDock },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "➕", fontSize = 10.sp)
                }

                Spacer(Modifier.width(6.dp))

                // Reply trigger (↩)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { onReply(message) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "↩️", fontSize = 10.sp)
                }

                // Pin trigger (📌) for host
                if (onPin != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (message.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable { onPin(message.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (message.isPinned) "📌" else "📍", fontSize = 10.sp)
                    }
                }
            }

            // Popover Emoji reaction dock
            AnimatedVisibility(visible = showEmojiDock) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("❤️", "🔥", "😂", "👍", "😮", "👏", "🎉").forEach { em ->
                            Text(
                                text = em,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onReact(message.id, em)
                                        showEmojiDock = false
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartySongSearchDrawer(
    onClose: () -> Unit,
    onSelectSong: (Song) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Search Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Search Song to Suggest",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Search Input Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        searchJob?.cancel()
                        if (query.trim().length >= 2) {
                            searchJob = scope.launch {
                                delay(300) // debounce
                                isSearching = true
                                val res = YtMusicRepository.search(query.trim(), SearchFilter.SONGS)
                                searchResults = res.getOrNull()?.mapNotNull { r ->
                                    when (r) {
                                        is SearchResult.Track -> r.song
                                        is SearchResult.TopTrack -> r.song
                                        else -> null
                                    }
                                } ?: emptyList()
                                isSearching = false
                            }
                        } else {
                            searchResults = emptyList()
                            isSearching = false
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Type song or artist name...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                )
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Results
        if (searchResults.isEmpty() && searchQuery.trim().length >= 2 && !isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No songs found for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(searchResults, key = { it.videoId }) { song ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSong(song) },
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "Share",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundbiteToastOverlay(
    activeSoundbiteToast: PartySoundbite?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = activeSoundbiteToast != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier,
    ) {
        activeSoundbiteToast?.let { bite ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val sbInfo = PartySoundbiteAudio.SOUNDBITES.firstOrNull { it.key == bite.sound }
                    Text(sbInfo?.emoji ?: "🔊", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${bite.senderName} blasted ${sbInfo?.label ?: bite.sound}!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

internal fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = (now - timestampMs).coerceAtLeast(0)
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 50 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> DateFormat.format("MMM d", timestampMs).toString()
    }
}

@Composable
private fun MessageActionsSheet(
    message: PartyChatMessage,
    isMe: Boolean,
    isHost: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onCopyText: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
    onSaveImage: () -> Unit,
    onReact: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(14.dp))

                    // Quick Reactions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("❤️", "🔥", "😂", "👍", "😮", "👏", "🎉").forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .clickable {
                                        onReact(em)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = em, fontSize = 20.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Message Summary Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = message.text.ifBlank {
                                        message.song?.title ?: if (!message.imageUrl.isNullOrBlank()) "📷 Attached Photo" else "Message"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatRelativeTime(message.timestampMs),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Actions List
                    ContextActionRow(
                        icon = Icons.Rounded.Reply,
                        title = "Reply",
                        subtitle = "Quote message in room chat",
                        onClick = {
                            onReply()
                            onDismiss()
                        }
                    )

                    if (message.text.isNotBlank()) {
                        ContextActionRow(
                            icon = Icons.Rounded.ContentCopy,
                            title = "Copy Text",
                            subtitle = "Copy message text to clipboard",
                            onClick = {
                                onCopyText()
                                onDismiss()
                            }
                        )
                    }

                    if (!message.imageUrl.isNullOrBlank()) {
                        ContextActionRow(
                            icon = Icons.Rounded.Download,
                            title = "Save Photo",
                            subtitle = "Save image to device gallery",
                            onClick = {
                                onSaveImage()
                                onDismiss()
                            }
                        )
                    }

                    if (isHost) {
                        ContextActionRow(
                            icon = Icons.Rounded.PushPin,
                            title = if (message.isPinned) "Unpin Message" else "Pin Announcement",
                            subtitle = if (message.isPinned) "Remove from top banner" else "Pin to top for all party listeners",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                onPinToggle()
                                onDismiss()
                            }
                        )
                    }

                    if (isMe || isHost) {
                        ContextActionRow(
                            icon = Icons.Rounded.Delete,
                            title = "Delete Message",
                            subtitle = if (isMe) "Delete for everyone in party" else "Delete message as host",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = {
                                onDelete()
                                onDismiss()
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ContextActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private suspend fun saveChatImageToGallery(context: Context, rawData: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = decodeBitmapFromChatImage(rawData) ?: return@withContext false
        val filename = "auramusic_party_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AuraMusic")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: return@withContext false
        true
    }.getOrDefault(false)
}

private suspend fun shareChatImage(context: Context, rawData: String) = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = decodeBitmapFromChatImage(rawData) ?: return@withContext
        val folder = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(folder, "party_photo_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Photo").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

internal object PartyChatBitmapCache {
    private val lru = object : LruCache<String, Bitmap>(40) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun get(url: String): Bitmap? = lru.get(url)

    fun load(url: String): Bitmap? {
        if (url.isBlank()) return null
        lru.get(url)?.let { return it }
        val bmp = runCatching {
            if (url.startsWith("data:image/") || url.contains("base64,")) {
                val base64Data = if (url.contains("base64,")) url.substringAfter("base64,") else url
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                java.net.URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }.getOrNull()
        if (bmp != null) {
            lru.put(url, bmp)
        }
        return bmp
    }
}

private fun decodeBitmapFromChatImage(rawData: String): Bitmap? {
    return PartyChatBitmapCache.load(rawData)
}

