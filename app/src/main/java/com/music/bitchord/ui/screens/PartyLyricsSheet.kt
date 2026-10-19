package com.music.bitchord.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.components.FloatingReactionOverlay
import com.music.bitchord.ui.components.PartyReactionQuickBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Synced Live Karaoke Lyrics Sheet with interactive verse-jump and real-time party reactions.
 */
@Composable
fun PartyLyricsSheet(
    onDismiss: () -> Unit,
    viewModel: MainViewModel? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by ListenTogether.state.collectAsStateWithLifecycle()
    val track = state.playback.track
    val isHost = state.you?.isHost == true
    val isLocked = state.controlsLocked && !isHost

    // Ensure lyrics are loaded for current party track
    LaunchedEffect(track?.videoId) {
        if (track != null && viewModel != null) {
            viewModel.loadLyrics(
                videoId = track.videoId,
                title = track.title,
                artist = track.artist,
                durationMs = track.durationMs ?: 0L,
            )
        }
    }

    val lyricsList: List<LyricLine>? = if (viewModel != null) {
        val l by viewModel.lyrics.collectAsStateWithLifecycle()
        l
    } else null

    // Track real-time playback position aligned with ListenTogether master clock
    var currentPosMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.playback.isPlaying) {
        while (isActive) {
            currentPosMs = ListenTogether.partyPositionMs() ?: 0L
            delay(50L)
        }
    }

    // Identify current active lyric line
    val activeLineIndex = remember(lyricsList, currentPosMs) {
        val list = lyricsList ?: return@remember -1
        if (list.isEmpty()) -1
        else {
            val idx = list.indexOfLast { it.timeMs <= currentPosMs }
            if (idx >= 0) idx else 0
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            listState.animateScrollToItem(
                index = (activeLineIndex - 2).coerceAtLeast(0),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFE91E63),
                                        Color(0xFF9C27B0),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Live Synced Karaoke",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE91E63).copy(alpha = 0.2f),
                            ) {
                                Text(
                                    text = "SYNCED",
                                    color = Color(0xFFE91E63),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                            }
                        }

                        if (track != null) {
                            Text(
                                text = "${track.title} • ${track.artist}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = "No track playing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Hint Banner for Verse Jump
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = if (isLocked) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "Host has locked room controls (verse-jump disabled)"
                        else "Tap any verse to jump playback for all room listeners",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Lyrics Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    track == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Waiting for track to start...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    lyricsList == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Syncing lyrics for party...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    lyricsList.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "No synced lyrics available for this song.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            itemsIndexed(lyricsList) { idx, line ->
                                val isActive = idx == activeLineIndex
                                val isPast = idx < activeLineIndex

                                val textColor by animateColorAsState(
                                    targetValue = when {
                                        isActive -> MaterialTheme.colorScheme.primary
                                        isPast -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                    },
                                    animationSpec = tween(200),
                                    label = "lyricTextColor",
                                )

                                val lineBg by animateColorAsState(
                                    targetValue = if (isActive) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    },
                                    animationSpec = tween(200),
                                    label = "lyricBgColor",
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(lineBg)
                                        .clickable {
                                            if (isLocked) {
                                                Toast.makeText(
                                                    context,
                                                    "Room controls are locked by the host",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            } else {
                                                ListenTogether.seek(line.timeMs)
                                                val mm = (line.timeMs / 1000) / 60
                                                val ss = (line.timeMs / 1000) % 60
                                                Toast.makeText(
                                                    context,
                                                    "Verse Jump: %d:%02d".format(mm, ss),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = line.text.ifBlank { "♪ ♪ ♪" },
                                            style = if (isActive) {
                                                MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 19.sp,
                                                    lineHeight = 26.sp,
                                                )
                                            } else {
                                                MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 16.sp,
                                                    lineHeight = 22.sp,
                                                )
                                            },
                                            color = textColor,
                                            modifier = Modifier.weight(1f),
                                        )

                                        if (isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.GraphicEq,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Reaction Bar at Bottom of Lyrics
            PartyReactionQuickBar(
                onSendReaction = { emoji ->
                    ListenTogether.sendReaction(emoji)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }

        // Floating Emoji Sparks drifting across the karaoke lyrics screen
        FloatingReactionOverlay(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
