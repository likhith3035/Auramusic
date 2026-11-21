package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.connect.AuraConnectManager
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.connect.AuraConnectDevice
import com.music.bitchord.data.connect.AuraPlaybackState
import com.music.bitchord.data.connect.ConnectTrack
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.NothingThemeTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class RemoteStudioTab {
    CONTROLS,
    QUEUE_AND_SEARCH,
}

/**
 * Dedicated full-screen remote studio for Aura Connect.
 * Provides complete remote playback control, live waveform visualizer, master volume and EQ,
 * integrated search-to-add songs directly to remote queue, and pro drag-to-adjust song reordering.
 */
@Composable
fun AuraRemoteStudioScreen(
    device: AuraConnectDevice,
    playbackState: AuraPlaybackState?,
    hazeState: HazeState? = null,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(RemoteStudioTab.CONTROLS) }

    // Live position interpolation
    var currentPositionMs by remember { mutableStateOf(playbackState?.positionMs ?: 0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderRatio by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playbackState?.positionMs, playbackState?.updatedAtMs) {
        if (!isDraggingSlider && playbackState != null) {
            currentPositionMs = playbackState.positionMs
        }
    }

    LaunchedEffect(playbackState?.isPlaying) {
        if (playbackState?.isPlaying == true) {
            while (isActive) {
                delay(100)
                if (!isDraggingSlider) {
                    val duration = playbackState.durationMs
                    val next = currentPositionMs + 100
                    currentPositionMs = if (duration > 0) next.coerceAtMost(duration) else next
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            RemoteStudioTopBar(
                device = device,
                playbackState = playbackState,
                onDismiss = onDismiss,
                onDisconnect = onDisconnect,
            )

            // Segmented Tab Selector
            RemoteStudioTabSelector(
                selectedTab = selectedTab,
                queueCount = playbackState?.queue?.size ?: 0,
                onTabSelect = { tab ->
                    haptics.play(Haptic.Tap)
                    selectedTab = tab
                },
            )

            Spacer(Modifier.height(8.dp))

            // Body Content
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                modifier = Modifier.weight(1f),
            ) { tab ->
                when (tab) {
                    RemoteStudioTab.CONTROLS -> {
                        RemoteControlsTabContent(
                            playbackState = playbackState,
                            currentPositionMs = currentPositionMs,
                            isDraggingSlider = isDraggingSlider,
                            sliderRatio = sliderRatio,
                            onSliderRatioChange = {
                                isDraggingSlider = true
                                sliderRatio = it
                            },
                            onSliderRelease = {
                                isDraggingSlider = false
                                val duration = playbackState?.durationMs ?: 0L
                                val targetMs = (sliderRatio * duration).toLong().coerceAtLeast(0L)
                                currentPositionMs = targetMs
                                haptics.play(Haptic.Select)
                                AuraConnectManager.seekTo(targetMs)
                            },
                            onTogglePlayPause = {
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.sendCommand("PLAY_PAUSE")
                            },
                            onPrevious = {
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.sendCommand("PREVIOUS")
                            },
                            onNext = {
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.sendCommand("NEXT")
                            },
                            onToggleShuffle = {
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.toggleShuffle()
                            },
                            onToggleRepeat = {
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.toggleRepeat()
                            },
                            onVolumeChange = { v ->
                                AuraConnectManager.setVolume(v)
                            },
                            onToggleMute = {
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.toggleMute()
                            },
                            onSetEqPreset = { preset ->
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.setEqPreset(preset)
                            },
                        )
                    }
                    RemoteStudioTab.QUEUE_AND_SEARCH -> {
                        RemoteQueueAndSearchTabContent(
                            playbackState = playbackState,
                            onPlayIndex = { index ->
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.playIndex(index)
                            },
                            onRemoveIndex = { index ->
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.removeIndex(index)
                            },
                            onMoveIndex = { from, to ->
                                haptics.play(Haptic.Select)
                                AuraConnectManager.moveIndex(from, to)
                            },
                            onClearQueue = {
                                haptics.play(Haptic.Tap)
                                AuraConnectManager.clearQueue()
                            },
                            onAddSong = { song, playNext ->
                                haptics.play(Haptic.Select)
                                AuraConnectManager.addTrack(song, playNext = playNext)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Top navigation and device status bar. */
@Composable
private fun RemoteStudioTopBar(
    device: AuraConnectDevice,
    playbackState: AuraPlaybackState?,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Minimize",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NothingThemeTokens.Red),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "AURA REMOTE STUDIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Text(
                text = "${device.name} · ${playbackState?.audioQuality ?: "320 kbps AAC"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onDisconnect)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CastConnected,
                    contentDescription = null,
                    tint = NothingThemeTokens.Red,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Disconnect",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

/** Segmented tab selector between Remote Controls and Queue/Search. */
@Composable
private fun RemoteStudioTabSelector(
    selectedTab: RemoteStudioTab,
    queueCount: Int,
    onTabSelect: (RemoteStudioTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(4.dp),
    ) {
        val controlsSelected = selectedTab == RemoteStudioTab.CONTROLS
        val queueSelected = selectedTab == RemoteStudioTab.QUEUE_AND_SEARCH

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (controlsSelected) NothingThemeTokens.Red else Color.Transparent)
                .clickable { onTabSelect(RemoteStudioTab.CONTROLS) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "CONTROLS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = if (controlsSelected) Color.White else Color.White.copy(alpha = 0.6f),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (queueSelected) NothingThemeTokens.Red else Color.Transparent)
                .clickable { onTabSelect(RemoteStudioTab.QUEUE_AND_SEARCH) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "QUEUE & SEARCH ($queueCount)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = if (queueSelected) Color.White else Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

/** Tab 1: Full Remote Playback Controls, Artwork, Waveform, Timeline, Volume, EQ. */
@Composable
private fun RemoteControlsTabContent(
    playbackState: AuraPlaybackState?,
    currentPositionMs: Long,
    isDraggingSlider: Boolean,
    sliderRatio: Float,
    onSliderRatioChange: (Float) -> Unit,
    onSliderRelease: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSetEqPreset: (String) -> Unit,
) {
    val track = playbackState?.track
    val isPlaying = playbackState?.isPlaying ?: false
    val durationMs = playbackState?.durationMs ?: 0L

    val effectivePos = if (isDraggingSlider) (sliderRatio * durationMs).toLong() else currentPositionMs
    val elapsedText = formatTime(effectivePos)
    val remainingText = if (durationMs > effectivePos) "-${formatTime(durationMs - effectivePos)}" else "-00:00"
    val sliderProgress = if (durationMs > 0) (effectivePos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero Artwork with Rounded Corners
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            if (track?.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp),
                )
            }

            // Live Waveform Visualizer Badge
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    RemoteWaveformVisualizer(isPlaying = true)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Title and Artist
        Text(
            text = track?.title?.takeIf { it.isNotBlank() } ?: "No Track Playing on Laptop",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = track?.artist?.takeIf { it.isNotBlank() } ?: "Use search or queue to play music",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        // Interactive Timeline Scrubber
        Slider(
            value = if (isDraggingSlider) sliderRatio else sliderProgress,
            onValueChange = onSliderRatioChange,
            onValueChangeFinished = onSliderRelease,
            colors = SliderDefaults.colors(
                thumbColor = NothingThemeTokens.Red,
                activeTrackColor = NothingThemeTokens.Red,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Primary Transport Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val shuffleOn = playbackState?.shuffleMode?.equals("ON", ignoreCase = true) == true
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleOn) NothingThemeTokens.Red else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Big Play/Pause Action Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NothingThemeTokens.Red)
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            val repeatMode = playbackState?.repeatMode?.uppercase() ?: "OFF"
            val repeatOn = repeatMode != "OFF"
            IconButton(onClick = onToggleRepeat) {
                Icon(
                    imageVector = if (repeatMode == "ONE") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatOn) NothingThemeTokens.Red else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Volume & Mute Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isMuted = playbackState?.isMuted == true || (playbackState?.volume ?: 1f) < 0.01f
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = "Mute",
                    tint = if (isMuted) NothingThemeTokens.Red else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Slider(
                value = if (isMuted) 0f else (playbackState?.volume ?: 1f),
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Equalizer Presets
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "REMOTE EQUALIZER PRESET",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
            )

            Spacer(Modifier.height(8.dp))

            val currentPreset = playbackState?.eqPreset?.uppercase() ?: "FLAT"
            val presets = listOf("FLAT", "BASS BOOST", "VOCAL", "CLUB", "ROCK")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    val selected = currentPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .border(
                                width = 0.5.dp,
                                color = if (selected) NothingThemeTokens.Red else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(percent = 50),
                            )
                            .background(
                                if (selected) NothingThemeTokens.Red.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                            )
                            .clickable { onSetEqPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) NothingThemeTokens.Red else Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

/** Tab 2: Integrated Search to Add Songs to Laptop + Drag-to-Adjust Remote Queue. */
@Composable
private fun RemoteQueueAndSearchTabContent(
    playbackState: AuraPlaybackState?,
    onPlayIndex: (Int) -> Unit,
    onRemoveIndex: (Int) -> Unit,
    onMoveIndex: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onAddSong: (Song, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val haptics = rememberHaptics()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    val addedIds = remember { mutableStateListOf<String>() }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun executeSearch(q: String) {
        val query = q.trim()
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return
        }
        searchJob = scope.launch {
            isSearching = true
            val res = YtMusicRepository.search(query, SearchFilter.SONGS)
            if (res.isSuccess) {
                val tracks = res.getOrNull().orEmpty().mapNotNull { item ->
                    when (item) {
                        is SearchResult.Track -> item.song
                        is SearchResult.TopTrack -> item.song
                        else -> null
                    }
                }
                searchResults = tracks
            }
            isSearching = false
        }
    }

    val suggestionChips = remember {
        listOf("Top Hits", "Trending", "Party", "Chill", "Lo-Fi", "Rock", "Acoustic")
    }

    val queue = playbackState?.queue.orEmpty()
    val currentIndex = playbackState?.queueIndex ?: -1
    val upcomingQueue = remember(queue, currentIndex) {
        if (currentIndex in queue.indices) queue.drop(currentIndex + 1) else queue
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Search Input Bar
        item(key = "search_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        executeSearch(it)
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(NothingThemeTokens.Red),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        executeSearch(searchQuery)
                    }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "Search song to add to laptop...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.45f),
                            )
                        }
                        innerTextField()
                    },
                )
                if (searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable {
                                searchQuery = ""
                                searchResults = emptyList()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Quick Suggestion Chips
        item(key = "suggestion_chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                suggestionChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
                            .clickable {
                                searchQuery = chip
                                executeSearch(chip)
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // Search Results Drawer (if active)
        if (isSearching) {
            item(key = "search_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = NothingThemeTokens.Red,
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else if (searchResults.isNotEmpty()) {
            item(key = "search_results_header") {
                Text(
                    text = "SEARCH RESULTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = NothingThemeTokens.Red,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            itemsIndexed(searchResults, key = { _, s -> "search_${s.videoId}" }) { _, song ->
                val added = addedIds.contains(song.videoId)
                RemoteSearchResultRow(
                    song = song,
                    isAdded = added,
                    onPlayNext = {
                        addedIds.add(song.videoId)
                        onAddSong(song, true)
                    },
                    onAddToQueue = {
                        addedIds.add(song.videoId)
                        onAddSong(song, false)
                    },
                )
            }
            item(key = "search_divider") {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.12f),
                )
            }
        }

        // Live Laptop Queue Section Header
        item(key = "queue_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "UP NEXT ON LAPTOP (${upcomingQueue.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                if (upcomingQueue.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NothingThemeTokens.Red,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onClearQueue)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // Drag to Adjust Reorderable List
        if (upcomingQueue.isEmpty()) {
            item(key = "empty_queue") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Laptop queue is empty.\nUse search above to add songs to play next.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            itemsIndexed(upcomingQueue, key = { i, t -> "queue_${t.videoId}_$i" }) { indexInUpcoming, track ->
                val absoluteIndex = if (currentIndex in queue.indices) currentIndex + 1 + indexInUpcoming else indexInUpcoming
                RemoteQueueTrackRow(
                    track = track,
                    indexInUpcoming = indexInUpcoming,
                    totalUpcoming = upcomingQueue.size,
                    onClick = { onPlayIndex(absoluteIndex) },
                    onRemove = { onRemoveIndex(absoluteIndex) },
                    onMoveUp = {
                        if (indexInUpcoming > 0) {
                            onMoveIndex(absoluteIndex, absoluteIndex - 1)
                        }
                    },
                    onMoveDown = {
                        if (indexInUpcoming < upcomingQueue.size - 1) {
                            onMoveIndex(absoluteIndex, absoluteIndex + 1)
                        }
                    },
                )
            }
        }
    }
}

/** Row for Search Results with 1-tap '+ Next' and '+ Queue' actions. */
@Composable
private fun RemoteSearchResultRow(
    song: Song,
    isAdded: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        if (isAdded) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(NothingThemeTokens.Red.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = NothingThemeTokens.Red,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "Added",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingThemeTokens.Red,
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onPlayNext)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "+ Next",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(NothingThemeTokens.Red)
                        .clickable(onClick = onAddToQueue)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "+ Queue",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/** Row for tracks in the Remote Queue with Drag Handles (=), Delete (x), and Jump (tap). */
@Composable
private fun RemoteQueueTrackRow(
    track: ConnectTrack,
    indexInUpcoming: Int,
    totalUpcoming: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val haptics = rememberHaptics()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (offsetY != 0f) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Prominent Drag Handle with gesture detection
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .pointerInput(indexInUpcoming, totalUpcoming) {
                    detectDragGestures(
                        onDragStart = {
                            haptics.play(Haptic.Select)
                        },
                        onDragEnd = {
                            if (offsetY < -30f && indexInUpcoming > 0) {
                                onMoveUp()
                            } else if (offsetY > 30f && indexInUpcoming < totalUpcoming - 1) {
                                onMoveDown()
                            }
                            offsetY = 0f
                        },
                        onDragCancel = {
                            offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY = (offsetY + dragAmount.y).coerceIn(-60f, 60f)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = if (offsetY != 0f) NothingThemeTokens.Red else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(6.dp))

        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Delete button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove",
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 5-bar live equalizer visualizer for remote playback. */
@Composable
private fun RemoteWaveformVisualizer(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "remoteEq")
    val h1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(350), repeatMode = RepeatMode.Reverse), label = "h1",
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(420), repeatMode = RepeatMode.Reverse), label = "h2",
    )
    val h3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(280), repeatMode = RepeatMode.Reverse), label = "h3",
    )
    val h4 by transition.animateFloat(
        initialValue = 0.9f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(460), repeatMode = RepeatMode.Reverse), label = "h4",
    )
    val h5 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(320), repeatMode = RepeatMode.Reverse), label = "h5",
    )

    Row(
        modifier = Modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val bars = listOf(h1, h2, h3, h4, h5)
        bars.forEach { factor ->
            val barHeight = if (isPlaying) (14.dp * factor).coerceAtLeast(3.dp) else 3.dp
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(NothingThemeTokens.Red),
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
