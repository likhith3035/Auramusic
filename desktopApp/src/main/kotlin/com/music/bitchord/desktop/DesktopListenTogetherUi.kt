package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.listentogether.JamInvite
import com.music.bitchord.data.listentogether.PartyActivity
import com.music.bitchord.data.listentogether.PartyChatMessage
import com.music.bitchord.data.listentogether.PartyMember
import com.music.bitchord.data.listentogether.PartyPreview
import com.music.bitchord.data.listentogether.PartyReaction
import com.music.bitchord.data.listentogether.PartySessionTrack
import com.music.bitchord.data.listentogether.PartySongRecommendation
import com.music.bitchord.data.listentogether.PartySoundbite
import com.music.bitchord.data.listentogether.PartyTrack
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Desktop counterpart of Android's complete Listen Together page. */
@Composable
internal fun DesktopListenTogetherDialog(autoplayEnabled: Boolean, onDismiss: () -> Unit) {
    val state by DesktopListenTogether.state.collectAsState()
    val activity by DesktopListenTogether.activity.collectAsState()
    val server by DesktopListenTogether.customServerUrl.collectAsState()
    val serverStatus by DesktopListenTogether.serverStatus.collectAsState()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(DesktopListenTogether.nickname()) }
    var maxMembers by remember { mutableStateOf(5) }
    var serverDraft by remember { mutableStateOf(server) }
    var preview by remember { mutableStateOf<PartyPreview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showRecapDialog by remember { mutableStateOf(false) }

    LaunchedEffect(server) {
        if (serverDraft == server || serverDraft.isBlank()) serverDraft = server
    }

    if (showRecapDialog) {
        DesktopPartyRecapDialog(onDismiss = { showRecapDialog = false })
    }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 920) {
        Column(desktopPanelBody(cardMax = 760.dp, cardMin = 560.dp, fill = true)) {
            Row(
                Modifier.fillMaxWidth().padding(start = panelInset(28.dp), end = panelInset(18.dp), top = 22.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        DesktopStrings["listen_together", "Listen together"],
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (state.inParty) "Everyone hears the same song at the same moment"
                        else "Start a room or join friends with a six-character code",
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // A page is left by the window's back, as every other page is.
                if (!LocalDesktopPanelIsPage.current) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
                }
            }
            if (!LocalDesktopPanelIsPage.current) HorizontalDivider(color = DesktopCardEdge)

            when {
                state.inParty -> PartyRoom(
                    state = state,
                    activity = activity,
                    busy = busy,
                    onOpenRecap = { showRecapDialog = true },
                    onLeave = {
                        busy = true
                        scope.launch {
                            showRecapDialog = true
                            DesktopListenTogether.leaveParty()
                            busy = false
                        }
                    },
                )
                preview != null -> JoinConfirmation(
                    preview = preview!!,
                    nickname = nickname,
                    onNicknameChange = {
                        nickname = it.take(80)
                        DesktopListenTogether.setNickname(nickname)
                    },
                    busy = busy,
                    error = localError ?: state.error,
                    onBack = { preview = null; localError = null },
                    onJoin = {
                        busy = true
                        localError = null
                        scope.launch {
                            DesktopListenTogether.joinParty(preview!!.code, nickname)
                                .onFailure { localError = it.message }
                            busy = false
                        }
                    },
                )
                else -> PartyLanding(
                    signedIn = DesktopListenTogether.canJoin(),
                    enabled = DesktopListenTogether.canJoin() && DesktopListenTogether.hasServer,
                    avatarUrl = DesktopListenTogether.myAvatarUrl(),
                    nickname = nickname,
                    onNicknameChange = {
                        nickname = it.take(80)
                        DesktopListenTogether.setNickname(nickname)
                    },
                    maxMembers = maxMembers,
                    onMaxMembersChange = { maxMembers = it.coerceIn(2, 10) },
                    code = code,
                    onCodeChange = {
                        code = JamInvite.parse(it)
                            ?: it.filter(Char::isLetterOrDigit).uppercase().take(DesktopListenTogether.CODE_LENGTH)
                        localError = null
                    },
                    server = serverDraft,
                    serverStatus = serverStatus,
                    onServerChange = { serverDraft = it },
                    onSaveServer = {
                        localError = null
                        DesktopListenTogether.normalizeServerUrl(serverDraft)
                            .onSuccess { normalized ->
                                DesktopListenTogether.setCustomServerUrl(normalized)
                                    .onSuccess { serverDraft = normalized }
                                    .onFailure { localError = it.message }
                            }
                            .onFailure { localError = it.message }
                    },
                    onRefreshServer = DesktopListenTogether::refreshServerHealth,
                    busy = busy,
                    error = localError ?: state.error,
                    onCreate = {
                        busy = true
                        localError = null
                        scope.launch {
                            DesktopListenTogether.createParty(nickname, maxMembers, autoplayEnabled)
                                .onFailure { localError = it.message }
                            busy = false
                        }
                    },
                    onPreview = {
                        busy = true
                        localError = null
                        scope.launch {
                            DesktopListenTogether.previewParty(code)
                                .onSuccess { preview = it }
                                .onFailure { localError = it.message }
                            busy = false
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PartyLanding(
    signedIn: Boolean,
    enabled: Boolean,
    avatarUrl: String?,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    maxMembers: Int,
    onMaxMembersChange: (Int) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    server: String,
    serverStatus: DesktopListenTogether.ServerStatus,
    onServerChange: (String) -> Unit,
    onSaveServer: () -> Unit,
    onRefreshServer: () -> Unit,
    busy: Boolean,
    error: String?,
    onCreate: () -> Unit,
    onPreview: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                PartyChoiceCard(
                    title = "Start a party",
                    subtitle = "Choose how friends see you and how many can join.",
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PartyAvatar(avatarUrl, 44)
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = onNicknameChange,
                            label = { Text("Nickname") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Party size", fontWeight = FontWeight.Medium)
                            Text("2–10 people", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Stepper("−", enabled && maxMembers > 2) { onMaxMembersChange(maxMembers - 1) }
                        Text("$maxMembers", modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                        Stepper("+", enabled && maxMembers < 10) { onMaxMembersChange(maxMembers + 1) }
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Start party", busy, enabled, onCreate)
                }

                PartyChoiceCard(
                    title = "Join a party",
                    subtitle = "Enter the code first. You’ll see who is inside before joining.",
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        label = { Text("Party code") },
                        placeholder = { Text("ABC123") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 5.sp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        label = "Look up party",
                        busy = busy,
                        enabled = enabled && code.length == DesktopListenTogether.CODE_LENGTH,
                        onClick = onPreview,
                    )
                }
            }
        }
        if (!signedIn) item { Notice("Sign in to YouTube Music before creating or joining a party.", true) }
        error?.takeIf(String::isNotBlank)?.let { item { Notice(it, true) } }
        item {
            Column(Modifier.desktopCardInset(RoundedCornerShape(14.dp)).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Settings, null, tint = DesktopSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Party server", fontWeight = FontWeight.Medium)
                        Text("Leave blank to use the server included with this build.", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                DesktopSearchField(
                    query = server,
                    onQueryChange = onServerChange,
                    onSearch = {},
                    placeholder = "Custom server address",
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        serverStatusLine(serverStatus),
                        color = when (serverStatus.health) {
                            DesktopListenTogether.Health.ONLINE -> DesktopSecondary
                            DesktopListenTogether.Health.CHECKING -> DesktopSecondary
                            DesktopListenTogether.Health.OFFLINE -> DesktopDestructive
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefreshServer) { Text("Test") }
                    Button(
                        onClick = onSaveServer,
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopCardInsetFill),
                    ) { Text(if (server.isBlank()) "Use built-in" else "Save") }
                }
            }
        }
    }
}

@Composable
private fun PartyChoiceCard(title: String, subtitle: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.desktopCardInset(RoundedCornerShape(16.dp)).padding(20.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = DesktopSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun JoinConfirmation(
    preview: PartyPreview,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onJoin: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = panelInset(36.dp), vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Groups, null, tint = DesktopAccent, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(14.dp))
        Text(
            if (preview.hostName.isBlank()) "Join this party?" else "Join ${preview.hostName}’s party?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text("${preview.memberCount} of ${preview.maxMembers} people · ${preview.code}", color = DesktopSecondary)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
            preview.members.take(5).forEach { PartyAvatar(it.avatarUrl, 50, it.displayName) }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("Nickname") },
            singleLine = true,
            modifier = Modifier.width(360.dp),
        )
        error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = DesktopDestructive) }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onBack, enabled = !busy) { Text("Back") }
            PrimaryButton("Join party", busy, !preview.isFull, onJoin, Modifier.width(190.dp))
        }
    }
}

@Composable
private fun PartyRoom(
    state: DesktopListenTogether.State,
    activity: List<PartyActivity>,
    busy: Boolean,
    onOpenRecap: () -> Unit,
    onLeave: () -> Unit,
) {
    var activeTab by remember { mutableStateOf(0) }
    val unreadCount by DesktopListenTogether.unreadChatCount.collectAsState()

    Row(Modifier.fillMaxSize().padding(horizontal = panelInset(24.dp), vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { RoomHero(state) }
            item { NowPlayingCard(state) }
            item { QueueCard(state) }
        }

        Column(Modifier.weight(1.05f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PartyTabButton(
                    title = "Chat",
                    badge = if (unreadCount > 0 && activeTab != 0) "$unreadCount" else null,
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    selected = activeTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeTab = 0
                        DesktopListenTogether.clearUnreadChat()
                    }
                )
                PartyTabButton(
                    title = "Soundstage",
                    icon = Icons.Rounded.GraphicEq,
                    selected = activeTab == 1,
                    modifier = Modifier.weight(1.1f),
                    onClick = { activeTab = 1 }
                )
                PartyTabButton(
                    title = "Members",
                    icon = Icons.Rounded.Groups,
                    selected = activeTab == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = 2 }
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    0 -> PartyChatPanel(state)
                    1 -> PartySoundstagePanel(state)
                    2 -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        item { MembersCard(state) }
                        if (state.you?.isHost == true) item { HostControls(state) }
                        if (activity.isNotEmpty()) item { ActivityCard(activity) }
                        state.error?.let { item { Notice(it, true) } }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onOpenRecap, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = DesktopAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Recap", color = DesktopAccent)
                }
                TextButton(onClick = onLeave, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text("Leave party", color = DesktopDestructive)
                }
            }
        }
    }
}

@Composable
private fun PartyTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DesktopCardEdge.copy(alpha = 0.8f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) DesktopAccent else DesktopSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) DesktopAccent else DesktopSecondary,
                maxLines = 1
            )
            if (badge != null) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DesktopDestructive)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomHero(state: DesktopListenTogether.State) {
    val invite = DesktopListenTogether.inviteUrl(state.code.orEmpty())
    Row(Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(16.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("PARTY CODE", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
            Text(state.code.orEmpty(), fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            Text(connectionLine(state), color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text("Scan from a phone or copy the invite link", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
        }
        DesktopQrCode(invite)
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { DesktopExternalLinks.copy(invite) }) {
                Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Copy invite")
            }
        }
    }
}

@Composable
private fun NowPlayingCard(state: DesktopListenTogether.State) {
    val track = state.playback.track
    val skipVote by DesktopListenTogether.skipVoteState.collectAsState()

    Column(Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(16.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NOW PLAYING", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            if (track != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (skipVote.hasVoted) DesktopAccent.copy(alpha = 0.2f) else DesktopCardEdge.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { DesktopListenTogether.voteSkip() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Vote Skip",
                            tint = if (skipVote.hasVoted) DesktopAccent else DesktopSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (skipVote.hasVoted) "Voted Skip (${skipVote.activeVotes}/${skipVote.totalNeeded})"
                            else "Vote Skip (${skipVote.activeVotes}/${skipVote.totalNeeded})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (skipVote.hasVoted) DesktopAccent else DesktopSecondary,
                            fontWeight = if (skipVote.hasVoted) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (track == null) {
            Text("Nothing playing yet", color = DesktopSecondary)
            if (state.you?.isHost == true) Text("Play a song and it will appear for everyone.", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DesktopArtwork(track.thumbnailUrl, Modifier.size(62.dp).clip(RoundedCornerShape(8.dp)), px = 160)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = DesktopSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    state.playback.startedByName?.let { Text("Started by $it", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun QueueCard(state: DesktopListenTogether.State) {
    Column(Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(16.dp)).padding(18.dp)) {
        Text("UP NEXT", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        val upcoming = state.queue.items.drop((state.queue.index + 1).coerceAtLeast(0)).take(5)
        if (upcoming.isEmpty()) Text("The shared queue is empty", color = DesktopSecondary)
        upcoming.forEachIndexed { index, track ->
            if (index > 0 && !LocalDesktopPanelIsPage.current) HorizontalDivider(color = DesktopCardEdge)
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", color = DesktopSecondary, modifier = Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MembersCard(state: DesktopListenTogether.State) {
    Column(Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(16.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LISTENING", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("${state.members.size}/${state.maxMembers}", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))
        state.members.forEachIndexed { index, member ->
            if (index > 0 && !LocalDesktopPanelIsPage.current) HorizontalDivider(color = DesktopCardEdge)
            MemberRow(member, member.memberId == state.you?.memberId, state.you?.isHost == true && !member.isHost) {
                DesktopListenTogether.kick(member.memberId)
            }
        }
    }
}

@Composable
private fun HostControls(state: DesktopListenTogether.State) {
    Column(Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(16.dp)).padding(18.dp)) {
        Text("HOST CONTROLS", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (state.hostOnlyControl) Icons.Rounded.Lock else Icons.Rounded.LockOpen, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Only I can control playback")
                Text("Guests can still pause on their own device.", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Switch(state.hostOnlyControl, onCheckedChange = DesktopListenTogether::setHostOnlyControl)
        }
        if (LocalDesktopPanelIsPage.current) {
            Spacer(Modifier.height(20.dp))
        } else {
            HorizontalDivider(color = DesktopCardEdge, modifier = Modifier.padding(vertical = 10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Party size")
                Text("Never removes people already inside.", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Stepper("−", state.maxMembers > maxOf(2, state.members.size)) { DesktopListenTogether.setMaxMembers(state.maxMembers - 1) }
            Text("${state.maxMembers}", modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
            Stepper("+", state.maxMembers < 10) { DesktopListenTogether.setMaxMembers(state.maxMembers + 1) }
        }
    }
}

@Composable
private fun ActivityCard(entries: List<PartyActivity>) {
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(16.dp)).padding(18.dp)) {
        Text("ACTIVITY", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        entries.take(8).forEach { entry ->
            Text(
                "${clock.format(Date(entry.atMs))}  ${entry.by}: ${entry.detail.ifBlank { entry.action }}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun MemberRow(member: PartyMember, isYou: Boolean, canKick: Boolean, onKick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        PartyAvatar(member.avatarUrl, 34, member.displayName)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(member.displayName.ifBlank { "Someone" } + if (isYou) " · You" else "", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when { member.isHost -> "Host"; !member.connected -> "Away"; else -> "Connected" },
                color = DesktopSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (canKick) IconButton(onClick = onKick, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Rounded.RemoveCircleOutline, "Remove", tint = DesktopDestructive, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PartyAvatar(url: String?, size: Int, fallback: String = "") {
    if (url != null) {
        DesktopArtwork(url, Modifier.size(size.dp).clip(CircleShape), px = size * 3)
    } else {
        Box(
            Modifier.size(size.dp).clip(CircleShape).background(DesktopCardInsetFill).border(0.5.dp, DesktopCardEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (fallback.isNotBlank()) Text(fallback.take(1).uppercase(), fontWeight = FontWeight.Bold)
            else Icon(Icons.Rounded.Person, null, tint = DesktopSecondary, modifier = Modifier.size((size * .55f).dp))
        }
    }
}

@Composable
private fun Stepper(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(DesktopCardInsetFill).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (enabled) Color.White else DesktopSecondary) }
}

@Composable
private fun PrimaryButton(
    label: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
        modifier = modifier.height(46.dp),
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
        else Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Notice(text: String, destructive: Boolean) {
    Text(
        text,
        color = if (destructive) DesktopDestructive else DesktopSecondary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().desktopCardInset(RoundedCornerShape(12.dp)).padding(14.dp),
    )
}

@Composable
private fun connectionLine(state: DesktopListenTogether.State): String = when {
    state.connection == DesktopListenTogether.Connection.CONNECTING -> "Reconnecting…"
    state.connection == DesktopListenTogether.Connection.OFFLINE -> "Offline"
    !state.clockSynced -> "Syncing clocks…"
    else -> "In sync · ${state.roundTripMs} ms"
}

private fun serverStatusLine(status: DesktopListenTogether.ServerStatus): String = when {
    status.health == DesktopListenTogether.Health.CHECKING -> "Checking the party server…"
    status.health == DesktopListenTogether.Health.OFFLINE -> "Party server is offline"
    status.isFallback -> "Custom server is offline · built-in server online · ${status.latencyMs} ms"
    else -> "Party server online · ${status.latencyMs} ms"
}

@Composable
private fun PartyChatPanel(state: DesktopListenTogether.State) {
    val messages by DesktopListenTogether.chatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showSearchDrawer by remember { mutableStateOf(false) }
    var showSoundboard by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<PartyChatMessage?>(null) }
    var activeSoundbiteToast by remember { mutableStateOf<PartySoundbite?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult.Track>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var activeReaction by remember { mutableStateOf<PartyReaction?>(null) }
    LaunchedEffect(Unit) {
        DesktopListenTogether.reactions.collect { r ->
            activeReaction = r
            delay(2400)
            if (activeReaction?.id == r.id) activeReaction = null
        }
    }

    // Audio soundbites real-time receiver
    LaunchedEffect(Unit) {
        DesktopListenTogether.soundbites.collect { bite ->
            DesktopSoundbiteAudio.play(bite.sound)
            activeSoundbiteToast = bite
            delay(2800)
            if (activeSoundbiteToast == bite) activeSoundbiteToast = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        DesktopListenTogether.clearUnreadChat()
    }

    Column(
        Modifier
            .fillMaxSize()
            .desktopCardInset(RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "ROOM CHAT & JUKEBOX",
                color = DesktopSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { showSoundboard = !showSoundboard },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        null,
                        modifier = Modifier.size(15.dp),
                        tint = if (showSoundboard) DesktopAccent else DesktopSecondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Soundboard",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showSoundboard) DesktopAccent else DesktopSecondary
                    )
                }

                TextButton(
                    onClick = { showSearchDrawer = !showSearchDrawer },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        if (showSearchDrawer) Icons.Rounded.Close else Icons.Rounded.Search,
                        null,
                        modifier = Modifier.size(15.dp),
                        tint = DesktopAccent
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (showSearchDrawer) "Close Search" else "Suggest Song",
                        style = MaterialTheme.typography.labelSmall,
                        color = DesktopAccent
                    )
                }
            }
        }

        // Pinned Announcement Banner
        val pinnedMessage = messages.lastOrNull { it.isPinned }
        if (pinnedMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DesktopAccent.copy(alpha = 0.15f))
                    .border(1.dp, DesktopAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .clickable {
                        val idx = messages.indexOfFirst { it.id == pinnedMessage.id }
                        if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📌", fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pinned Announcement · ${pinnedMessage.senderName}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DesktopAccent)
                    Text(pinnedMessage.text.ifBlank { pinnedMessage.song?.title.orEmpty() }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                }
                if (state.you?.isHost == true) {
                    IconButton(onClick = { DesktopListenTogether.pinChatMessage(pinnedMessage.id) }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Rounded.Close, "Unpin", tint = DesktopSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Soundbite Toast Notification
        activeSoundbiteToast?.let { bite ->
            val info = DesktopSoundbiteAudio.SOUNDBITES.firstOrNull { it.key == bite.sound }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DesktopAccent)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(info?.emoji ?: "🔊", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text("${bite.senderName} blasted ${info?.label ?: bite.sound}!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Live Soundboard Strip
        if (showSoundboard) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DesktopCardInsetFill)
                    .border(0.5.dp, DesktopCardEdge, RoundedCornerShape(8.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DesktopSoundbiteAudio.SOUNDBITES.forEach { sb ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                DesktopListenTogether.sendSoundbite(sb.key)
                                DesktopSoundbiteAudio.play(sb.key)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(sb.emoji, fontSize = 18.sp)
                        Text(sb.label, fontSize = 9.sp, color = DesktopSecondary)
                    }
                }
            }
        }

        if (showSearchDrawer) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .desktopCardInset(RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            if (q.trim().length >= 2) {
                                scope.launch {
                                    isSearching = true
                                    DesktopSearchClient.search(q.trim()).onSuccess { res ->
                                        searchResults = res.filterIsInstance<SearchResult.Track>().take(4)
                                    }
                                    isSearching = false
                                }
                            } else {
                                searchResults = emptyList()
                            }
                        },
                        placeholder = { Text("Search YouTube Music...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSearching) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = DesktopAccent)
                    }
                }

                if (searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        searchResults.forEach { item ->
                            val s = item.song
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DesktopCardInsetFill)
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DesktopArtwork(s.thumbnailUrl, Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)), px = 100)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(s.artist, style = MaterialTheme.typography.labelSmall, color = DesktopSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(6.dp))
                                TextButton(
                                    onClick = {
                                        DesktopListenTogether.sendChatMessage(
                                            text = "Suggested: ${s.title}",
                                            song = PartySongRecommendation(
                                                videoId = s.videoId,
                                                title = s.title,
                                                artist = s.artist,
                                                coverUrl = s.thumbnailUrl,
                                                durationSec = 180,
                                            ),
                                            replyTo = replyingTo
                                        )
                                        replyingTo = null
                                        showSearchDrawer = false
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(14.dp), tint = DesktopAccent)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Send", style = MaterialTheme.typography.labelSmall, color = DesktopAccent)
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = DesktopCardEdge, modifier = Modifier.padding(bottom = 8.dp))

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No messages yet.\nSay hello, drop a soundbite, or suggest a song for the room!",
                    color = DesktopSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    PartyChatMessageRow(
                        msg = msg,
                        isYou = msg.senderId == state.you?.memberId,
                        myMemberId = state.you?.memberId,
                        isHost = state.you?.isHost == true,
                        onReply = { replyingTo = it },
                        onReact = { id, em -> DesktopListenTogether.reactToChatMessage(id, em) },
                        onPin = if (state.you?.isHost == true) { id -> DesktopListenTogether.pinChatMessage(id) } else null,
                        onScrollToReply = { replyId ->
                            val idx = messages.indexOfFirst { it.id == replyId }
                            if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Quoted Reply Preview Bar
        replyingTo?.let { target ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DesktopCardInsetFill)
                    .border(0.5.dp, DesktopAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(2.dp).height(20.dp).background(DesktopAccent))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Replying to ${target.senderName}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DesktopAccent)
                    Text(target.text.ifBlank { target.song?.title ?: "Song recommendation" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = DesktopSecondary)
                }
                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Rounded.Close, "Cancel", tint = DesktopSecondary, modifier = Modifier.size(13.dp))
                }
            }
        }

        // Live Reactions Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Room React:", style = MaterialTheme.typography.labelSmall, color = DesktopSecondary)
            listOf("🔥", "❤️", "👏", "🎉", "🚀", "💎").forEach { emoji ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DesktopCardInsetFill,
                    modifier = Modifier.clickable { DesktopListenTogether.sendReaction(emoji) }
                ) {
                    Text(
                        emoji,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            activeReaction?.let { r ->
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DesktopAccent.copy(alpha = 0.25f),
                ) {
                    Text(
                        "${r.emoji} ${r.senderName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DesktopAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(if (replyingTo != null) "Type reply..." else "Say something in party...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        DesktopListenTogether.sendChatMessage(
                            text = inputText.trim(),
                            replyTo = replyingTo,
                        )
                        inputText = ""
                        replyingTo = null
                    }
                },
                enabled = inputText.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = if (inputText.isNotBlank()) DesktopAccent else DesktopSecondary)
            }
        }
    }
}

@Composable
private fun PartyChatMessageRow(
    msg: PartyChatMessage,
    isYou: Boolean,
    myMemberId: String?,
    isHost: Boolean,
    onReply: (PartyChatMessage) -> Unit,
    onReact: (String, String) -> Unit,
    onPin: ((String) -> Unit)?,
    onScrollToReply: (String) -> Unit,
) {
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var showEmojiDock by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isYou) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isYou) {
                PartyAvatar(msg.senderAvatar, 22, msg.senderName)
                Spacer(Modifier.width(6.dp))
                Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = DesktopSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
            }
            Text(clock.format(Date(msg.timestampMs)), style = MaterialTheme.typography.labelSmall, color = DesktopSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
        }

        Spacer(Modifier.height(3.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isYou) DesktopAccent.copy(alpha = 0.15f) else DesktopCardInsetFill)
                .border(0.5.dp, if (isYou) DesktopAccent.copy(alpha = 0.3f) else DesktopCardEdge, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                // Pinned Badge
                if (msg.isPinned) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Text("📌", fontSize = 11.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("Pinned", style = MaterialTheme.typography.labelSmall, color = DesktopAccent, fontWeight = FontWeight.Bold)
                    }
                }

                // Quoted reply header
                if (msg.replyToSender != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DesktopCardFill)
                            .clickable { msg.replyToId?.let { onScrollToReply(it) } }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(2.dp).height(18.dp).background(DesktopAccent))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("↩ ${msg.replyToSender}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DesktopAccent, fontSize = 11.sp)
                            Text(msg.replyToText.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = DesktopSecondary, fontSize = 11.sp)
                        }
                    }
                }

                if (msg.text.isNotBlank()) {
                    Text(msg.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }

                val song = msg.song
                if (song != null) {
                    if (msg.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesktopCardFill)
                            .padding(8.dp)
                    ) {
                        if (msg.autoQueued || (msg.upvotes >= 2 && song != null)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DesktopAccent.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    "🗳️ Auto-Queued by Democracy",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DesktopAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DesktopArtwork(song.coverUrl, Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)), px = 120)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = DesktopSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { DesktopListenTogether.upvoteChatMessage(msg.id) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.ThumbUp, null, modifier = Modifier.size(13.dp), tint = if (msg.upvotes > 0) DesktopAccent else DesktopSecondary)
                                Spacer(Modifier.width(4.dp))
                                Text("${msg.upvotes}", style = MaterialTheme.typography.labelSmall, color = if (msg.upvotes > 0) DesktopAccent else DesktopSecondary)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        DesktopListenTogether.queueAdd(
                                            listOf(
                                                PartyTrack(
                                                    videoId = song.videoId,
                                                    title = song.title,
                                                    artist = song.artist,
                                                    thumbnailUrl = song.coverUrl,
                                                    durationMs = song.durationSec?.let { it * 1000L }
                                                )
                                            ),
                                            playNext = true
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(13.dp), tint = DesktopAccent)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Next", style = MaterialTheme.typography.labelSmall, color = DesktopAccent)
                                }

                                TextButton(
                                    onClick = {
                                        DesktopListenTogether.queueAdd(
                                            listOf(
                                                PartyTrack(
                                                    videoId = song.videoId,
                                                    title = song.title,
                                                    artist = song.artist,
                                                    thumbnailUrl = song.coverUrl,
                                                    durationMs = song.durationSec?.let { it * 1000L }
                                                )
                                            ),
                                            playNext = false
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(13.dp), tint = DesktopAccent)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Queue", style = MaterialTheme.typography.labelSmall, color = DesktopAccent)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reaction chips and actions below bubble
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isYou) Arrangement.End else Arrangement.Start
        ) {
            // Reaction cluster chips
            msg.reactions.forEach { (emoji, members) ->
                if (members.isNotEmpty()) {
                    val reacted = myMemberId != null && members.contains(myMemberId)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (reacted) DesktopAccent.copy(alpha = 0.25f) else DesktopCardInsetFill,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (reacted) DesktopAccent.copy(alpha = 0.5f) else DesktopCardEdge),
                        modifier = Modifier.padding(end = 4.dp).clickable { onReact(msg.id, emoji) }
                    ) {
                        Text(
                            "$emoji ${members.size}",
                            fontSize = 11.sp,
                            fontWeight = if (reacted) FontWeight.Bold else FontWeight.Medium,
                            color = if (reacted) DesktopAccent else DesktopSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // + Reaction trigger
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(DesktopCardInsetFill)
                    .clickable { showEmojiDock = !showEmojiDock },
                contentAlignment = Alignment.Center
            ) {
                Text("➕", fontSize = 9.sp)
            }

            Spacer(Modifier.width(4.dp))

            // Reply trigger
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(DesktopCardInsetFill)
                    .clickable { onReply(msg) },
                contentAlignment = Alignment.Center
            ) {
                Text("↩️", fontSize = 9.sp)
            }

            // Pin trigger (if host)
            if (onPin != null) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (msg.isPinned) DesktopAccent.copy(alpha = 0.25f) else DesktopCardInsetFill)
                        .clickable { onPin(msg.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (msg.isPinned) "📌" else "📍", fontSize = 9.sp)
                }
            }
        }

        // Inline emoji picker dock
        if (showEmojiDock) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DesktopCardFill,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DesktopCardEdge),
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
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onReact(msg.id, em)
                                    showEmojiDock = false
                                }
                                .padding(3.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class DesktopSoundstagePreset(val name: String, val description: String, val color: Color)
private data class DesktopVisualizerPalette(val id: String, val name: String, val color: Color)

private val DESKTOP_SOUNDSTAGE_PRESETS = listOf(
    DesktopSoundstagePreset("Aura Spatial 3D", "Immersive virtual surround soundstage", Color(0xFF7C4DFF)),
    DesktopSoundstagePreset("Bass Booster Mega", "Deep low-end rumble for dance & hip-hop", Color(0xFFFF5252)),
    DesktopSoundstagePreset("Vocal Clarity HD", "Crisp midrange tuned for vocals and lyrics", Color(0xFF40C4FF)),
    DesktopSoundstagePreset("Club Stadium", "Wide acoustic hall reverb & energy", Color(0xFFFFAB40)),
    DesktopSoundstagePreset("Lofi Chill Room", "Warm tape-saturation and gentle highs", Color(0xFF69F0AE)),
)

private val DESKTOP_VISUALIZER_PALETTES = listOf(
    DesktopVisualizerPalette("neon_sunset", "Neon Sunset", Color(0xFFFF4081)),
    DesktopVisualizerPalette("cyber_emerald", "Cyber Emerald", Color(0xFF00E676)),
    DesktopVisualizerPalette("electric_indigo", "Electric Indigo", Color(0xFF7C4DFF)),
    DesktopVisualizerPalette("midnight_amber", "Midnight Amber", Color(0xFFFF9100)),
    DesktopVisualizerPalette("solar_flare", "Solar Flare", Color(0xFFFFD600)),
)

@Composable
private fun PartySoundstagePanel(state: DesktopListenTogether.State) {
    val syncedTheme by DesktopListenTogether.syncedTheme.collectAsState()
    val isHost = state.you?.isHost == true
    val activePreset = syncedTheme?.presetName?.ifBlank { "Aura Spatial 3D" } ?: "Aura Spatial 3D"
    val activePalette = syncedTheme?.palette?.ifBlank { "neon_sunset" } ?: "neon_sunset"

    LazyColumn(
        modifier = Modifier.fillMaxSize().desktopCardInset(RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("PARTY SOUNDSTAGE & VISUALIZERS", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isHost) "You are Room Host · Changes sync to all listeners in real-time"
                    else "Synced with Room Host: $activePreset",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isHost) DesktopAccent else DesktopSecondary
                )
            }
        }

        item {
            Text("ACOUSTIC SOUNDSTAGE PRESETS", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DESKTOP_SOUNDSTAGE_PRESETS.forEach { preset ->
                    val selected = activePreset == preset.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) preset.color.copy(alpha = 0.15f) else DesktopCardInsetFill)
                            .border(1.dp, if (selected) preset.color else DesktopCardEdge, RoundedCornerShape(10.dp))
                            .clickable(enabled = isHost) {
                                DesktopListenTogether.syncTheme(preset.name, activePalette)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(preset.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.GraphicEq, null, tint = preset.color, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(preset.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text(preset.description, color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        if (selected) {
                            Icon(Icons.Rounded.Check, "Active", tint = preset.color, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            Text("VISUALIZER COLOR PALETTES", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DESKTOP_VISUALIZER_PALETTES.forEach { pal ->
                    val selected = activePalette == pal.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) pal.color.copy(alpha = 0.15f) else DesktopCardInsetFill)
                            .border(1.dp, if (selected) pal.color else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable(enabled = isHost) {
                                DesktopListenTogether.syncTheme(activePreset, pal.id)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(pal.color)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(pal.name, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, maxLines = 1, color = if (selected) DesktopAccent else DesktopSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopPartyRecapDialog(onDismiss: () -> Unit) {
    val currentHistory by DesktopListenTogether.sessionHistory.collectAsState()
    val sessionStartedAt by DesktopListenTogether.sessionStartedAtMs.collectAsState()
    val state by DesktopListenTogether.state.collectAsState()

    val history = if (currentHistory.isNotEmpty()) currentHistory else DesktopListenTogether.lastCompletedSessionHistory
    val durationMs = if (sessionStartedAt > 0L) {
        (System.currentTimeMillis() - sessionStartedAt).coerceAtLeast(0L)
    } else {
        DesktopListenTogether.lastCompletedSessionDurationMs
    }

    val durationMinutes = (durationMs / 60000L).coerceAtLeast(1)
    val roomCode = state.code ?: "Aura"

    val topDj = remember(history) {
        history.mapNotNull { it.queuedByName }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }?.key
            ?: state.members.firstOrNull { it.isHost }?.displayName
            ?: "Room Host"
    }

    var savedMessage by remember { mutableStateOf<String?>(null) }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 560, popup = true) {
        Column(desktopPanelBody(cardMax = 560.dp, cardMin = 420.dp, fill = true)) {
            Row(
                Modifier.fillMaxWidth().padding(start = panelInset(24.dp), end = panelInset(16.dp), top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Party Recap ✨", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Session breakdown for Room $roomCode", color = DesktopSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
            }

            HorizontalDivider(color = DesktopCardEdge)

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = panelInset(24.dp), vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RecapStatBox("DURATION", "${durationMinutes}m", Icons.Rounded.Schedule, Modifier.weight(1f))
                        RecapStatBox("TRACKS", "${history.size}", Icons.Rounded.MusicNote, Modifier.weight(1f))
                        RecapStatBox("TOP DJ", topDj, Icons.Rounded.AutoAwesome, Modifier.weight(1.2f))
                    }
                }

                if (savedMessage != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DesktopAccent.copy(alpha = 0.15f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(savedMessage!!, color = DesktopAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Text("PLAYED IN THIS SESSION", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
                }

                if (history.isEmpty()) {
                    item {
                        Text("No songs played during this session.", color = DesktopSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(history) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DesktopCardInsetFill)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DesktopArtwork(track.coverUrl, Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)), px = 100)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = DesktopSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (track.queuedByName != null) {
                                Text("via ${track.queuedByName}", color = DesktopSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = DesktopCardEdge)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = panelInset(24.dp), vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (history.isNotEmpty()) {
                            val persistence = DesktopPersistence()
                            val existing = persistence.playlists()
                            val newPlaylist = DesktopPlaylist(
                                title = "Party $roomCode Recap",
                                songs = history.map { t ->
                                    Song(
                                        videoId = t.videoId,
                                        title = t.title,
                                        artist = t.artist,
                                        thumbnailUrl = t.coverUrl,
                                        durationText = t.durationMs?.let { ms ->
                                            val sec = ms / 1000
                                            "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
                                        }
                                    )
                                }
                            )
                            persistence.savePlaylists(listOf(newPlaylist) + existing)
                            savedMessage = "Saved ${history.size} songs to 'Party $roomCode Recap' in Library!"
                        }
                    },
                    enabled = history.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1.2f).height(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save as Playlist", fontWeight = FontWeight.SemiBold)
                }

                TextButton(
                    onClick = {
                        val recapText = buildString {
                            appendLine("✨ AuraMusic Listen Together Recap")
                            appendLine("Room Code: $roomCode")
                            appendLine("Duration: ${durationMinutes}m | Songs: ${history.size}")
                            appendLine("Top DJ: $topDj")
                            appendLine()
                            appendLine("Songs Played:")
                            history.forEachIndexed { i, t ->
                                appendLine("${i + 1}. ${t.title} - ${t.artist}")
                            }
                        }
                        DesktopExternalLinks.copy(recapText)
                        savedMessage = "Recap summary copied to clipboard!"
                    },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp), tint = DesktopAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Summary", color = DesktopAccent)
                }
            }
        }
    }
}

@Composable
private fun RecapStatBox(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DesktopCardInsetFill)
            .border(0.5.dp, DesktopCardEdge, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = DesktopSecondary)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = DesktopSecondary, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
