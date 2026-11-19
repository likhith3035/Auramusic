package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.ai.AuraNeuralDjEngine
import com.music.bitchord.data.ai.HarmonicRelationship
import com.music.bitchord.data.dj.CrossfaderCurve
import com.music.bitchord.data.dj.DjBridge
import com.music.bitchord.data.dj.DjSessionState
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.utils.containSheetGestures
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.NothingCard
import com.music.bitchord.ui.theme.NothingPill
import com.music.bitchord.ui.theme.NothingThemeTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class DjDeckTarget {
    DECK_A,
    DECK_B,
}

/**
 * Fullscreen modal displaying the Pro Dual-Deck DJ Console in AuraMusic.
 * Built with the authentic Nothing OS Design System: pitch black surfaces, stark white typography,
 * subtle slate borders, and Nothing Red accents.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun DjDecksSheet(
    visible: Boolean,
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val haptics = rememberHaptics()
    var showTrackPicker by remember { mutableStateOf(false) }
    var pickerTargetDeck by remember { mutableStateOf(DjDeckTarget.DECK_B) }

    LaunchedEffect(visible) {
        if (visible) {
            DjBridge.current.startDjMode()
            haptics.play(Haptic.Select)
        }
    }

    val isDjActive by DjSessionState.isDjActive.collectAsStateWithLifecycle()
    val crossfader by DjSessionState.crossfader.collectAsStateWithLifecycle()
    val crossfaderCurve by DjSessionState.crossfaderCurve.collectAsStateWithLifecycle()
    val deckASong by DjSessionState.deckASong.collectAsStateWithLifecycle()
    val deckBSong by DjSessionState.deckBSong.collectAsStateWithLifecycle()
    val deckAPlaying by DjSessionState.deckAPlaying.collectAsStateWithLifecycle()
    val deckBPlaying by DjSessionState.deckBPlaying.collectAsStateWithLifecycle()
    val deckAPos by DjSessionState.deckAPositionMs.collectAsStateWithLifecycle()
    val deckADur by DjSessionState.deckADurationMs.collectAsStateWithLifecycle()
    val deckBPos by DjSessionState.deckBPositionMs.collectAsStateWithLifecycle()
    val deckBDur by DjSessionState.deckBDurationMs.collectAsStateWithLifecycle()
    val deckABpm by DjSessionState.deckABpm.collectAsStateWithLifecycle()
    val deckBBpm by DjSessionState.deckBBpm.collectAsStateWithLifecycle()
    val deckAKey by DjSessionState.deckAKey.collectAsStateWithLifecycle()
    val deckBKey by DjSessionState.deckBKey.collectAsStateWithLifecycle()
    val deckASpeed by DjSessionState.deckASpeed.collectAsStateWithLifecycle()
    val deckBSpeed by DjSessionState.deckBSpeed.collectAsStateWithLifecycle()
    val deckAFilter by DjSessionState.deckAFilter.collectAsStateWithLifecycle()
    val deckBFilter by DjSessionState.deckBFilter.collectAsStateWithLifecycle()
    val deckALoopBeats by DjSessionState.deckALoopBeats.collectAsStateWithLifecycle()
    val deckBLoopBeats by DjSessionState.deckBLoopBeats.collectAsStateWithLifecycle()
    val deckAHotCues by DjSessionState.deckAHotCues.collectAsStateWithLifecycle()
    val deckBHotCues by DjSessionState.deckBHotCues.collectAsStateWithLifecycle()
    val isBeatSynced by DjSessionState.isBeatSynced.collectAsStateWithLifecycle()
    val isHamsterMode by DjSessionState.isHamsterMode.collectAsStateWithLifecycle()
    val isSplitCueEnabled by DjSessionState.isSplitCueEnabled.collectAsStateWithLifecycle()
    val isBrakingA by DjSessionState.isBrakingA.collectAsStateWithLifecycle()
    val isBrakingB by DjSessionState.isBrakingB.collectAsStateWithLifecycle()
    val isAutoDropping by DjSessionState.isAutoDropping.collectAsStateWithLifecycle()
    val lowKillA by DjSessionState.lowKillA.collectAsStateWithLifecycle()
    val lowKillB by DjSessionState.lowKillB.collectAsStateWithLifecycle()
    val candidateQueue by DjSessionState.candidateQueue.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(NothingThemeTokens.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (deckAPlaying || deckBPlaying) NothingThemeTokens.Red else NothingThemeTokens.DimDot),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DUAL-DECK ENGINE",
                        color = NothingThemeTokens.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NothingPill(
                        text = "QUEUE (${candidateQueue.size})",
                        onClick = {
                            pickerTargetDeck = DjDeckTarget.DECK_B
                            showTrackPicker = true
                        },
                        isActive = showTrackPicker,
                    )
                    CircleGlyph(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close DJ Console",
                        onClick = {
                            DjBridge.current.exitDjMode()
                            onClose()
                        },
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isWide = maxWidth >= 620.dp

                if (isWide) {
                    // Landscape / Tablet layout: Deck A on left, Mixer center, Deck B on right
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DeckCard(
                            deckName = "DECK A",
                            song = deckASong,
                            isPlaying = deckAPlaying,
                            isBraking = isBrakingA,
                            positionMs = deckAPos,
                            durationMs = deckADur,
                            bpm = deckABpm,
                            musicalKey = deckAKey,
                            speed = deckASpeed,
                            filterAmount = deckAFilter,
                            loopBeats = deckALoopBeats,
                            hotCues = deckAHotCues,
                            lowKill = lowKillA,
                            onPlayPause = { DjBridge.current.toggleDeckAPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckA()
                            },
                            onBrake = {
                                haptics.play(Haptic.Tick)
                                DjBridge.current.triggerBrakeA()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillA() },
                            onSpeedChange = { DjBridge.current.setDeckASpeed(it) },
                            onPitchNudge = { pos -> DjBridge.current.nudgePitchA(pos) },
                            onFilterChange = { DjBridge.current.setDeckAFilter(it) },
                            onToggleLoop = { DjBridge.current.toggleDeckALoop(it) },
                            onTriggerHotCue = { DjBridge.current.triggerHotCueA(it) },
                            onClearHotCue = { DjBridge.current.clearHotCueA(it) },
                            onScrub = { DjBridge.current.scrubDeckA(it) },
                            onSeek = { DjBridge.current.seekDeckA(it) },
                            onBeatJump = { DjBridge.current.beatJumpA(it) },
                            onChangeSong = {
                                pickerTargetDeck = DjDeckTarget.DECK_A
                                showTrackPicker = true
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )

                        MixerConsole(
                            crossfader = crossfader,
                            curve = crossfaderCurve,
                            isBeatSynced = isBeatSynced,
                            isAutoDropping = isAutoDropping,
                            isHamsterMode = isHamsterMode,
                            isSplitCueEnabled = isSplitCueEnabled,
                            deckAPosMs = deckAPos,
                            deckADurationMs = deckADur,
                            deckAPlaying = deckAPlaying,
                            deckABpm = deckABpm,
                            deckBPosMs = deckBPos,
                            deckBDurationMs = deckBDur,
                            deckBPlaying = deckBPlaying,
                            deckBBpm = deckBBpm,
                            onCrossfaderChange = { fraction ->
                                DjBridge.current.setCrossfader(fraction)
                            },
                            onCurveToggle = {
                                val next = if (crossfaderCurve == CrossfaderCurve.SMOOTH) CrossfaderCurve.CUT else CrossfaderCurve.SMOOTH
                                DjBridge.current.setCrossfaderCurve(next)
                            },
                            onHamsterToggle = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.toggleHamsterMode()
                            },
                            onSplitCueToggle = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.toggleSplitCue()
                            },
                            onSync = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.syncDeckB()
                            },
                            onAutoDrop = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.autoDrop()
                            },
                            onCommit = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.commitTransition()
                            },
                            modifier = Modifier.width(230.dp).fillMaxHeight(),
                        )

                        DeckCard(
                            deckName = "DECK B",
                            song = deckBSong,
                            isPlaying = deckBPlaying,
                            isBraking = isBrakingB,
                            positionMs = deckBPos,
                            durationMs = deckBDur,
                            bpm = deckBBpm,
                            musicalKey = deckBKey,
                            speed = deckBSpeed,
                            filterAmount = deckBFilter,
                            loopBeats = deckBLoopBeats,
                            hotCues = deckBHotCues,
                            lowKill = lowKillB,
                            onPlayPause = { DjBridge.current.toggleDeckBPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckB()
                            },
                            onBrake = {
                                haptics.play(Haptic.Tick)
                                DjBridge.current.triggerBrakeB()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillB() },
                            onSpeedChange = { DjBridge.current.setDeckBSpeed(it) },
                            onPitchNudge = { pos -> DjBridge.current.nudgePitchB(pos) },
                            onFilterChange = { DjBridge.current.setDeckBFilter(it) },
                            onToggleLoop = { DjBridge.current.toggleDeckBLoop(it) },
                            onTriggerHotCue = { DjBridge.current.triggerHotCueB(it) },
                            onClearHotCue = { DjBridge.current.clearHotCueB(it) },
                            onScrub = { DjBridge.current.scrubDeckB(it) },
                            onSeek = { DjBridge.current.seekDeckB(it) },
                            onBeatJump = { DjBridge.current.beatJumpB(it) },
                            onChangeSong = {
                                pickerTargetDeck = DjDeckTarget.DECK_B
                                showTrackPicker = true
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                } else {
                    // Portrait Phone layout: Deck A (top), Mixer (middle), Deck B (bottom)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DeckCard(
                            deckName = "DECK A",
                            song = deckASong,
                            isPlaying = deckAPlaying,
                            isBraking = isBrakingA,
                            positionMs = deckAPos,
                            durationMs = deckADur,
                            bpm = deckABpm,
                            musicalKey = deckAKey,
                            speed = deckASpeed,
                            filterAmount = deckAFilter,
                            loopBeats = deckALoopBeats,
                            hotCues = deckAHotCues,
                            lowKill = lowKillA,
                            onPlayPause = { DjBridge.current.toggleDeckAPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckA()
                            },
                            onBrake = {
                                haptics.play(Haptic.Tick)
                                DjBridge.current.triggerBrakeA()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillA() },
                            onSpeedChange = { DjBridge.current.setDeckASpeed(it) },
                            onPitchNudge = { pos -> DjBridge.current.nudgePitchA(pos) },
                            onFilterChange = { DjBridge.current.setDeckAFilter(it) },
                            onToggleLoop = { DjBridge.current.toggleDeckALoop(it) },
                            onTriggerHotCue = { DjBridge.current.triggerHotCueA(it) },
                            onClearHotCue = { DjBridge.current.clearHotCueA(it) },
                            onScrub = { DjBridge.current.scrubDeckA(it) },
                            onSeek = { DjBridge.current.seekDeckA(it) },
                            onBeatJump = { DjBridge.current.beatJumpA(it) },
                            onChangeSong = {
                                pickerTargetDeck = DjDeckTarget.DECK_A
                                showTrackPicker = true
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )

                        MixerConsole(
                            crossfader = crossfader,
                            curve = crossfaderCurve,
                            isBeatSynced = isBeatSynced,
                            isAutoDropping = isAutoDropping,
                            isHamsterMode = isHamsterMode,
                            isSplitCueEnabled = isSplitCueEnabled,
                            deckAPosMs = deckAPos,
                            deckADurationMs = deckADur,
                            deckAPlaying = deckAPlaying,
                            deckABpm = deckABpm,
                            deckBPosMs = deckBPos,
                            deckBDurationMs = deckBDur,
                            deckBPlaying = deckBPlaying,
                            deckBBpm = deckBBpm,
                            onCrossfaderChange = { fraction ->
                                DjBridge.current.setCrossfader(fraction)
                            },
                            onCurveToggle = {
                                val next = if (crossfaderCurve == CrossfaderCurve.SMOOTH) CrossfaderCurve.CUT else CrossfaderCurve.SMOOTH
                                DjBridge.current.setCrossfaderCurve(next)
                            },
                            onHamsterToggle = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.toggleHamsterMode()
                            },
                            onSplitCueToggle = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.toggleSplitCue()
                            },
                            onSync = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.syncDeckB()
                            },
                            onAutoDrop = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.autoDrop()
                            },
                            onCommit = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.commitTransition()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        DeckCard(
                            deckName = "DECK B",
                            song = deckBSong,
                            isPlaying = deckBPlaying,
                            isBraking = isBrakingB,
                            positionMs = deckBPos,
                            durationMs = deckBDur,
                            bpm = deckBBpm,
                            musicalKey = deckBKey,
                            speed = deckBSpeed,
                            filterAmount = deckBFilter,
                            loopBeats = deckBLoopBeats,
                            hotCues = deckBHotCues,
                            lowKill = lowKillB,
                            onPlayPause = { DjBridge.current.toggleDeckBPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckB()
                            },
                            onBrake = {
                                haptics.play(Haptic.Tick)
                                DjBridge.current.triggerBrakeB()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillB() },
                            onSpeedChange = { DjBridge.current.setDeckBSpeed(it) },
                            onPitchNudge = { pos -> DjBridge.current.nudgePitchB(pos) },
                            onFilterChange = { DjBridge.current.setDeckBFilter(it) },
                            onToggleLoop = { DjBridge.current.toggleDeckBLoop(it) },
                            onTriggerHotCue = { DjBridge.current.triggerHotCueB(it) },
                            onClearHotCue = { DjBridge.current.clearHotCueB(it) },
                            onScrub = { DjBridge.current.scrubDeckB(it) },
                            onSeek = { DjBridge.current.seekDeckB(it) },
                            onBeatJump = { DjBridge.current.beatJumpB(it) },
                            onChangeSong = {
                                pickerTargetDeck = DjDeckTarget.DECK_B
                                showTrackPicker = true
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showTrackPicker) {
        DjQueueAndSearchModal(
            currentSongA = deckASong,
            currentSongB = deckBSong,
            targetDeck = pickerTargetDeck,
            onTargetDeckChange = { pickerTargetDeck = it },
            queue = candidateQueue,
            onDismiss = { showTrackPicker = false },
            onSelectSongA = { picked ->
                DjBridge.current.selectDeckASong(picked)
                showTrackPicker = false
            },
            onSelectSongB = { picked ->
                DjBridge.current.selectDeckBSong(picked)
                showTrackPicker = false
            },
            onReorder = { from, to ->
                DjBridge.current.reorderQueue(from, to)
            },
            onRemoveItem = { index ->
                DjBridge.current.removeQueueItem(index)
            },
        )
    }
}

/**
 * Deck console card styled in Nothing OS hardware language:
 * Matte obsidian squircle surface, clean typography, tactile platter,
 * and minimal status pills.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckCard(
    deckName: String,
    song: Song?,
    isPlaying: Boolean,
    isBraking: Boolean,
    positionMs: Long,
    durationMs: Long,
    bpm: Int,
    musicalKey: String,
    speed: Float,
    filterAmount: Float,
    loopBeats: Int?,
    hotCues: List<Long?>,
    lowKill: Boolean,
    onPlayPause: () -> Unit,
    onCue: () -> Unit,
    onBrake: () -> Unit,
    onToggleLowKill: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchNudge: (Boolean) -> Unit,
    onFilterChange: (Float) -> Unit,
    onToggleLoop: (Int) -> Unit,
    onTriggerHotCue: (Int) -> Unit,
    onClearHotCue: (Int) -> Unit,
    onScrub: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onBeatJump: (Int) -> Unit,
    onChangeSong: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    NothingCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = NothingThemeTokens.Surface,
        borderColor = NothingThemeTokens.BorderSubtle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Deck Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Deck Name Pill with Red LED indicator
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(NothingThemeTokens.Red),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = deckName,
                            color = NothingThemeTokens.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    // BPM Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "$bpm BPM",
                            color = NothingThemeTokens.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    // Key Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = musicalKey,
                            color = NothingThemeTokens.MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                if (onChangeSong != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(50))
                            .clickable {
                                haptics.play(Haptic.Select)
                                onChangeSong()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "SELECT",
                            color = NothingThemeTokens.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            // Interactive Turntable & Track Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Turntable Platter
                NothingTurntable(
                    thumbnailUrl = song?.thumbnailUrl,
                    isPlaying = isPlaying,
                    onScrub = onScrub,
                    modifier = Modifier.size(68.dp),
                )

                Spacer(Modifier.width(10.dp))

                // Track Details & Elapsed Time
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = song?.title ?: "No track loaded",
                        color = NothingThemeTokens.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song?.artist ?: "Deck standby",
                        color = NothingThemeTokens.MutedText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))

                    // Time Readout
                    val posSec = positionMs / 1000L
                    val durSec = durationMs / 1000L
                    Text(
                        text = String.format("%d:%02d / %d:%02d", posSec / 60, posSec % 60, durSec / 60, durSec % 60),
                        color = NothingThemeTokens.MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // Pitch Nudge and Speed Slider
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Momentary Nudge +
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(1.dp, NothingThemeTokens.Border, CircleShape)
                            .clickable {
                                haptics.play(Haptic.Tick)
                                onPitchNudge(true)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", color = NothingThemeTokens.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = String.format("%.2fx", speed),
                        color = if (speed != 1.0f) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )

                    // Momentary Nudge -
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(1.dp, NothingThemeTokens.Border, CircleShape)
                            .clickable {
                                haptics.play(Haptic.Tick)
                                onPitchNudge(false)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("-", color = NothingThemeTokens.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive Timeline Scrubber with Hot Cue Pins and Loop Region
            DjTimelineScrubber(
                positionMs = positionMs,
                durationMs = durationMs,
                hotCues = hotCues,
                loopBeats = loopBeats,
                bpm = bpm,
                onSeek = onSeek,
                modifier = Modifier.padding(vertical = 2.dp),
            )

            // Beat Loops, Beat Jump & Hot Cue Memory Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Beat Loops (2B, 4B, 8B)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(2, 4, 8).forEach { beats ->
                        val isLooping = loopBeats == beats
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLooping) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface)
                                .border(1.dp, if (isLooping) NothingThemeTokens.Red else NothingThemeTokens.BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.play(Haptic.Select)
                                    onToggleLoop(beats)
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLooping) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(NothingThemeTokens.Red),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = "${beats}B",
                                    color = if (isLooping) NothingThemeTokens.White else NothingThemeTokens.MutedText,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }

                // Beat Jump (-4B, +4B)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(-4, 4).forEach { jump ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingThemeTokens.SurfaceElevated)
                                .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.play(Haptic.Tick)
                                    onBeatJump(jump)
                                }
                                .padding(horizontal = 5.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (jump > 0) "+${jump}B" else "${jump}B",
                                color = NothingThemeTokens.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }

                // 3 Hot Cue Memory Pads (H1, H2, H3)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0..2) {
                        val cuePos = hotCues.getOrNull(i)
                        val isSet = cuePos != null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSet) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface)
                                .border(1.dp, if (isSet) NothingThemeTokens.White.copy(alpha = 0.5f) else NothingThemeTokens.BorderSubtle, RoundedCornerShape(6.dp))
                                .combinedClickable(
                                    onClick = {
                                        haptics.play(Haptic.Select)
                                        onTriggerHotCue(i)
                                    },
                                    onLongClick = {
                                        haptics.play(Haptic.Tick)
                                        onClearHotCue(i)
                                    },
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = if (cuePos != null) {
                                    val sec = (cuePos / 1000L)
                                    String.format("%d:%02d", sec / 60, sec % 60)
                                } else {
                                    "H${i + 1}"
                                },
                                color = if (isSet) NothingThemeTokens.White else NothingThemeTokens.MutedText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            // Transport Row: CUE, BRAKE, PLAY/PAUSE, LOW KILL, Filter Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // CUE Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingThemeTokens.SurfaceElevated)
                        .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onCue)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "CUE",
                        color = NothingThemeTokens.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }

                // BRAKE (Turntable Motor Stop) Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBraking) NothingThemeTokens.Red.copy(alpha = 0.20f) else NothingThemeTokens.SurfaceElevated)
                        .border(1.dp, if (isBraking) NothingThemeTokens.Red else NothingThemeTokens.Border, RoundedCornerShape(8.dp))
                        .clickable(enabled = isPlaying && !isBraking, onClick = onBrake)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isBraking) "STOPPING" else "BRAKE",
                        color = if (isBraking) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }

                // PLAY/PAUSE Button (CircleGlyph)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NothingThemeTokens.White)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NothingThemeTokens.Black,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // LOW KILL (Bass Cut) Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (lowKill) NothingThemeTokens.Red.copy(alpha = 0.20f) else NothingThemeTokens.SurfaceElevated)
                        .border(1.dp, if (lowKill) NothingThemeTokens.Red else NothingThemeTokens.BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleLowKill)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "LOW KILL",
                        color = if (lowKill) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }

                // Mini Filter Sweep Slider (-1.0f .. +1.0f)
                Box(modifier = Modifier.width(82.dp)) {
                    Slider(
                        value = filterAmount,
                        onValueChange = { newVal ->
                            if (kotlin.math.abs(newVal) < 0.08f) {
                                onFilterChange(0f)
                            } else {
                                onFilterChange(newVal)
                            }
                        },
                        valueRange = -1.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (kotlin.math.abs(filterAmount) > 0.08f) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                            activeTrackColor = NothingThemeTokens.Red,
                            inactiveTrackColor = NothingThemeTokens.Border,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Minimalist Nothing hardware phase meter.
 * Dual monochrome beat markers for Deck A and Deck B with center Nothing Red cursor.
 */
@Composable
private fun StackedPhaseWaveform(
    deckAPosMs: Long,
    deckADurationMs: Long,
    deckAPlaying: Boolean,
    deckABpm: Int,
    deckBPosMs: Long,
    deckBDurationMs: Long,
    deckBPlaying: Boolean,
    deckBBpm: Int,
    modifier: Modifier = Modifier,
) {
    val bpmA = deckABpm.coerceAtLeast(60)
    val bpmB = deckBBpm.coerceAtLeast(60)
    val beatMsA = 60_000f / bpmA
    val beatMsB = 60_000f / bpmB
    val phaseA = (deckAPosMs % beatMsA.toLong()) / beatMsA
    val phaseB = (deckBPosMs % beatMsB.toLong()) / beatMsB
    val phaseDiff = ((phaseB - phaseA + 1.5f) % 1.0f) - 0.5f
    val isPhaseAligned = kotlin.math.abs(phaseDiff) < 0.08f && deckAPlaying && deckBPlaying

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NothingThemeTokens.Black)
            .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // Phase status readout
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PHASE",
                color = NothingThemeTokens.MutedText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (!deckAPlaying || !deckBPlaying) {
                    "STANDBY"
                } else if (isPhaseAligned) {
                    "IN PHASE"
                } else {
                    val deltaMs = (phaseDiff * beatMsB).roundToInt()
                    if (deltaMs > 0) "+${deltaMs}ms" else "${deltaMs}ms"
                },
                color = if (isPhaseAligned) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }

        // Dual Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val centerX = w / 2f

                // Deck A (Top Lane)
                val beatDistPx = 36f
                val offsetA = (deckAPosMs.toFloat() / beatMsA) * beatDistPx
                val startBeatA = ((offsetA - centerX) / beatDistPx).toInt() - 1
                val endBeatA = ((offsetA + centerX) / beatDistPx).toInt() + 1

                for (b in startBeatA..endBeatA) {
                    val beatX = centerX + (b * beatDistPx - offsetA)
                    if (beatX in 0f..w) {
                        val isDownbeat = (b % 4 == 0)
                        val barH = if (isDownbeat) 8f else 5f
                        drawLine(
                            color = if (isDownbeat) NothingThemeTokens.White else NothingThemeTokens.MutedText.copy(alpha = 0.5f),
                            start = Offset(beatX, midY - 2f),
                            end = Offset(beatX, midY - 2f - barH),
                            strokeWidth = if (isDownbeat) 2f else 1.2f,
                        )
                    }
                }

                // Midline separator
                drawLine(
                    color = NothingThemeTokens.BorderSubtle,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1f,
                )

                // Deck B (Bottom Lane)
                val offsetB = (deckBPosMs.toFloat() / beatMsB) * beatDistPx
                val startBeatB = ((offsetB - centerX) / beatDistPx).toInt() - 1
                val endBeatB = ((offsetB + centerX) / beatDistPx).toInt() + 1

                for (b in startBeatB..endBeatB) {
                    val beatX = centerX + (b * beatDistPx - offsetB)
                    if (beatX in 0f..w) {
                        val isDownbeat = (b % 4 == 0)
                        val barH = if (isDownbeat) 8f else 5f
                        drawLine(
                            color = if (isDownbeat) NothingThemeTokens.White else NothingThemeTokens.MutedText.copy(alpha = 0.5f),
                            start = Offset(beatX, midY + 2f),
                            end = Offset(beatX, midY + 2f + barH),
                            strokeWidth = if (isDownbeat) 2f else 1.2f,
                        )
                    }
                }

                // Center red playhead cursor
                drawLine(
                    color = NothingThemeTokens.Red,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, h),
                    strokeWidth = 2f,
                )
            }
        }
    }
}

/**
 * Center DJ Mixer Console with Nothing OS hardware styling:
 * Minimalist capsule buttons, stark contrast fader, and clean readouts.
 */
@Composable
private fun MixerConsole(
    crossfader: Float,
    curve: CrossfaderCurve,
    isBeatSynced: Boolean,
    isAutoDropping: Boolean,
    isHamsterMode: Boolean,
    isSplitCueEnabled: Boolean,
    deckAPosMs: Long,
    deckADurationMs: Long,
    deckAPlaying: Boolean,
    deckABpm: Int,
    deckBPosMs: Long,
    deckBDurationMs: Long,
    deckBPlaying: Boolean,
    deckBBpm: Int,
    onCrossfaderChange: (Float) -> Unit,
    onCurveToggle: () -> Unit,
    onHamsterToggle: () -> Unit,
    onSplitCueToggle: () -> Unit,
    onSync: () -> Unit,
    onAutoDrop: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    val effectiveFader = if (isHamsterMode) 1.0f - crossfader else crossfader
    val gainA: Float
    val gainB: Float
    when (curve) {
        CrossfaderCurve.SMOOTH -> {
            val angle = effectiveFader * (PI / 2.0).toFloat()
            gainA = cos(angle)
            gainB = sin(angle)
        }
        CrossfaderCurve.CUT -> {
            gainA = if (effectiveFader >= 0.95f) 0.0f else 1.0f
            gainB = if (effectiveFader <= 0.05f) 0.0f else 1.0f
        }
    }
    val percentA = (gainA * 100f).roundToInt()
    val percentB = (gainB * 100f).roundToInt()

    NothingCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = NothingThemeTokens.Surface,
        borderColor = NothingThemeTokens.BorderSubtle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            // Row 1: SYNC, CURVE, HAMSTER, SPLIT CUE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // BEAT SYNC BUTTON
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isBeatSynced) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface)
                        .border(1.dp, if (isBeatSynced) NothingThemeTokens.Red else NothingThemeTokens.BorderSubtle, RoundedCornerShape(50))
                        .clickable(onClick = onSync)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isBeatSynced) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(NothingThemeTokens.Red),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = "SYNC",
                            color = if (isBeatSynced) NothingThemeTokens.White else NothingThemeTokens.MutedText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                // CURVE MODE SWITCH (SMOOTH vs CUT)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(NothingThemeTokens.SurfaceElevated)
                        .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(50))
                        .clickable(onClick = onCurveToggle)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (curve == CrossfaderCurve.SMOOTH) "SMOOTH" else "CUT",
                        color = NothingThemeTokens.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                    )
                }

                // HAMSTER REVERSE CROSSFADER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isHamsterMode) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface)
                        .border(1.dp, if (isHamsterMode) NothingThemeTokens.Red else NothingThemeTokens.BorderSubtle, RoundedCornerShape(50))
                        .clickable(onClick = onHamsterToggle)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isHamsterMode) "HAMSTER: REV" else "HAMSTER",
                        color = if (isHamsterMode) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                    )
                }

                // SPLIT-CUE HEADPHONE MONITORING
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSplitCueEnabled) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface)
                        .border(1.dp, if (isSplitCueEnabled) NothingThemeTokens.Red else NothingThemeTokens.BorderSubtle, RoundedCornerShape(50))
                        .clickable(onClick = onSplitCueToggle)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSplitCueEnabled) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(NothingThemeTokens.Red),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = "SPLIT CUE",
                            color = if (isSplitCueEnabled) NothingThemeTokens.White else NothingThemeTokens.MutedText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            // Row 2: AUTO-DROP and TAKEOVER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // AUTO-DROP BUTTON
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingThemeTokens.SurfaceElevated)
                        .border(1.dp, if (isAutoDropping) NothingThemeTokens.Red else NothingThemeTokens.Border, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isAutoDropping, onClick = onAutoDrop)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isAutoDropping) "TRANSITIONING..." else "AUTO-DROP (3.5S)",
                        color = if (isAutoDropping) NothingThemeTokens.Red else NothingThemeTokens.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }

                Spacer(Modifier.width(8.dp))

                // COMMIT / DROP TAKEOVER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingThemeTokens.SurfaceElevated)
                        .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onCommit)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "TAKEOVER DECK B",
                        color = NothingThemeTokens.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            // Dual Stacked Waveform Phase Meter
            StackedPhaseWaveform(
                deckAPosMs = deckAPosMs,
                deckADurationMs = deckADurationMs,
                deckAPlaying = deckAPlaying,
                deckABpm = deckABpm,
                deckBPosMs = deckBPosMs,
                deckBDurationMs = deckBDurationMs,
                deckBPlaying = deckBPlaying,
                deckBBpm = deckBBpm,
            )

            // Crossfader Volume Readouts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "A: $percentA%",
                    color = NothingThemeTokens.MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = if (curve == CrossfaderCurve.SMOOTH) "EQUAL POWER" else "SCRATCH CUT",
                    color = NothingThemeTokens.MutedText.copy(alpha = 0.60f),
                    fontSize = 8.5.sp,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "B: $percentB%",
                    color = NothingThemeTokens.MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // Horizontal Crossfader Slider with Nothing Red Center Detent
            var lastHapticPos by remember { mutableFloatStateOf(crossfader) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                Slider(
                    value = crossfader,
                    onValueChange = { newVal ->
                        if (kotlin.math.abs(newVal - lastHapticPos) >= 0.05f) {
                            haptics.play(Haptic.Tick)
                            lastHapticPos = newVal
                        }
                        if (kotlin.math.abs(newVal - 0.5f) < 0.02f && kotlin.math.abs(lastHapticPos - 0.5f) >= 0.02f) {
                            haptics.play(Haptic.Select)
                        }
                        onCrossfaderChange(newVal)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = NothingThemeTokens.White,
                        activeTrackColor = NothingThemeTokens.White,
                        inactiveTrackColor = NothingThemeTokens.Border,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Visual Turntable styled in Nothing OS hardware language:
 * Matte vinyl record, subtle micro-grooves, rotating album sleeve label,
 * and Nothing Red center core spindle.
 */
@Composable
private fun NothingTurntable(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    onScrub: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, isDragging) {
        while (isActive && isPlaying && !isDragging) {
            rotationAngle = (rotationAngle + 2.0f) % 360f
            delay(16L) // ~60 FPS
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, NothingThemeTokens.Border, CircleShape)
            .background(NothingThemeTokens.SurfaceElevated)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        haptics.play(Haptic.Select)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x
                        rotationAngle = (rotationAngle + delta * 1.5f) % 360f
                        val scrubDeltaMs = (delta * 40L).toLong()
                        onScrub(scrubDeltaMs)
                        haptics.play(Haptic.Tick)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Vinyl micro-grooves canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationAngle },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            val grooveColor = Color.White.copy(alpha = 0.04f)
            for (step in 1..4) {
                drawCircle(
                    color = grooveColor,
                    radius = radius * (0.50f + step * 0.10f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }

        // Center Album Art Record Label
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.dp, NothingThemeTokens.Border, CircleShape)
                .graphicsLayer { rotationZ = rotationAngle },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Center red spindle dot
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(NothingThemeTokens.Red)
                    .border(0.5.dp, NothingThemeTokens.Black, CircleShape),
            )
        }
    }
}

/**
 * Sleek Nothing OS linear timeline progress scrubber with drag-to-seek,
 * hot cue pins (H1, H2, H3), active loop span indicator, and live elapsed/remaining countdown readouts.
 */
@Composable
private fun DjTimelineScrubber(
    positionMs: Long,
    durationMs: Long,
    hotCues: List<Long?>,
    loopBeats: Int?,
    bpm: Int,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val safeDur = durationMs.coerceAtLeast(1L)
    val currentFraction = if (isDragging) dragFraction else (positionMs.toFloat() / safeDur).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(safeDur) {
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val target = (f * safeDur).toLong()
                        haptics.play(Haptic.Select)
                        onSeek(target)
                    }
                }
                .pointerInput(safeDur) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            haptics.play(Haptic.Select)
                        },
                        onDragEnd = {
                            val target = (dragFraction * safeDur).toLong()
                            onSeek(target)
                            isDragging = false
                            haptics.play(Haptic.Tick)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                val railY = size.height / 2f
                val strokeW = 3.dp.toPx()

                // Background Rail
                drawLine(
                    color = NothingThemeTokens.BorderSubtle,
                    start = Offset(0f, railY),
                    end = Offset(size.width, railY),
                    strokeWidth = strokeW,
                )

                // Loop Span Highlight (if active)
                if (loopBeats != null && bpm > 0) {
                    val beatMs = 60_000L / bpm
                    val loopDur = loopBeats * beatMs
                    val loopStartX = (positionMs.toFloat() / safeDur).coerceIn(0f, 1f) * size.width
                    val loopEndX = ((positionMs + loopDur).toFloat() / safeDur).coerceIn(0f, 1f) * size.width
                    drawLine(
                        color = NothingThemeTokens.Red.copy(alpha = 0.6f),
                        start = Offset(loopStartX, railY),
                        end = Offset(loopEndX, railY),
                        strokeWidth = strokeW + 2f,
                    )
                }

                // Progress Active Fill
                val progressX = currentFraction * size.width
                if (progressX > 0f) {
                    drawLine(
                        color = NothingThemeTokens.White,
                        start = Offset(0f, railY),
                        end = Offset(progressX, railY),
                        strokeWidth = strokeW,
                    )
                }

                // Hot Cue Markers (H1, H2, H3)
                hotCues.forEach { cueMs ->
                    if (cueMs != null && cueMs in 0..safeDur) {
                        val cueX = (cueMs.toFloat() / safeDur).coerceIn(0f, 1f) * size.width
                        drawLine(
                            color = NothingThemeTokens.Red,
                            start = Offset(cueX, railY - 5.dp.toPx()),
                            end = Offset(cueX, railY + 5.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                }

                // Playhead Thumb
                drawCircle(
                    color = NothingThemeTokens.White,
                    radius = if (isDragging) 6.dp.toPx() else 4.5.dp.toPx(),
                    center = Offset(progressX, railY),
                )
                drawCircle(
                    color = NothingThemeTokens.Red,
                    radius = if (isDragging) 3.5.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(progressX, railY),
                )
            }
        }

        // Time Readouts (Elapsed on Left, Remaining Countdown on Right)
        val displayPos = if (isDragging) (dragFraction * safeDur).toLong() else positionMs
        val posSec = displayPos / 1000L
        val remSec = ((safeDur - displayPos).coerceAtLeast(0L)) / 1000L
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = String.format("%d:%02d", posSec / 60, posSec % 60),
                color = if (isDragging) NothingThemeTokens.Red else NothingThemeTokens.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = String.format("-%d:%02d", remSec / 60, remSec % 60),
                color = NothingThemeTokens.MutedText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/**
 * DJ Setlist and Universal Search modal styled with Nothing OS card surfaces.
 * Features live debounced YouTube Music search, Camelot harmonic key compatibility,
 * and authentic AuraMusic queue reordering and track removal.
 */
@Composable
private fun DjQueueAndSearchModal(
    currentSongA: Song?,
    currentSongB: Song?,
    targetDeck: DjDeckTarget,
    onTargetDeckChange: (DjDeckTarget) -> Unit,
    queue: List<Song>,
    onDismiss: () -> Unit,
    onSelectSongA: (Song) -> Unit,
    onSelectSongB: (Song) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Harmonic profile for Deck A
    val profileA = remember(currentSongA?.videoId) {
        currentSongA?.let { AuraNeuralDjEngine.analyzeSong(it) }
    }

    // Trigger debounced online search
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(350L)
            isSearching = true
            val res = YtMusicRepository.search(q, SearchFilter.SONGS)
            if (res.isSuccess) {
                val songs = res.getOrNull().orEmpty().mapNotNull { item ->
                    when (item) {
                        is SearchResult.Track -> item.song
                        is SearchResult.TopTrack -> item.song
                        else -> null
                    }
                }
                searchResults = songs
            }
            isSearching = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NothingCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(2.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = NothingThemeTokens.Surface,
            borderColor = NothingThemeTokens.Border,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                // Header with Target Deck Toggle and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DJ QUEUE & SEARCH",
                        color = NothingThemeTokens.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )

                    CircleGlyph(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Target Deck Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NothingPill(
                        text = "TARGET: DECK A",
                        onClick = {
                            haptics.play(Haptic.Select)
                            onTargetDeckChange(DjDeckTarget.DECK_A)
                        },
                        isActive = targetDeck == DjDeckTarget.DECK_A,
                        modifier = Modifier.weight(1f),
                    )
                    NothingPill(
                        text = "TARGET: DECK B",
                        onClick = {
                            haptics.play(Haptic.Select)
                            onTargetDeckChange(DjDeckTarget.DECK_B)
                        },
                        isActive = targetDeck == DjDeckTarget.DECK_B,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Search Bar in Nothing OS Surface
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingThemeTokens.SurfaceElevated)
                        .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = NothingThemeTokens.MutedText,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = NothingThemeTokens.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(NothingThemeTokens.Red),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search YouTube Music or Queue...",
                                    color = NothingThemeTokens.MutedText,
                                    fontSize = 13.sp,
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (searchQuery.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(NothingThemeTokens.Border)
                                .clickable {
                                    searchQuery = ""
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = NothingThemeTokens.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Content: Either Search Results OR Live Queue with Drag Reordering
                if (searchQuery.isNotBlank()) {
                    // Search Mode
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Searching...",
                                color = NothingThemeTokens.MutedText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No tracks found for \"$searchQuery\"",
                                color = NothingThemeTokens.MutedText,
                                fontSize = 12.sp,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(searchResults) { s ->
                                val songProfile = remember(s.videoId) { AuraNeuralDjEngine.analyzeSong(s) }
                                val relationship = if (profileA != null) {
                                    profileA.key.getRelationship(songProfile.key)
                                } else {
                                    HarmonicRelationship.EXACT
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NothingThemeTokens.SurfaceElevated)
                                        .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = s.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .thumbnailBorder(RoundedCornerShape(8.dp))
                                            .background(NothingThemeTokens.Surface),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            color = NothingThemeTokens.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = s.artist,
                                            color = NothingThemeTokens.MutedText,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))

                                    // Key & Harmonic Badges
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${songProfile.bpm} BPM | ${songProfile.key}",
                                            color = NothingThemeTokens.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        val isGood = relationship.isCompatible
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isGood) NothingThemeTokens.Surface else NothingThemeTokens.SurfaceElevated)
                                                .border(0.5.dp, if (isGood) NothingThemeTokens.Red else NothingThemeTokens.Border, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                text = relationship.label,
                                                color = if (isGood) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Load Buttons
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (targetDeck == DjDeckTarget.DECK_A) NothingThemeTokens.Red else NothingThemeTokens.Surface)
                                                .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    haptics.play(Haptic.Select)
                                                    onSelectSongA(s)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                text = "A",
                                                color = NothingThemeTokens.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (targetDeck == DjDeckTarget.DECK_B) NothingThemeTokens.Red else NothingThemeTokens.Surface)
                                                .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    haptics.play(Haptic.Select)
                                                    onSelectSongB(s)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                text = "B",
                                                color = NothingThemeTokens.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Queue Mode (Native Aura App Queue Style)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Section: Deck A Master
                        item(key = "section_deck_a") {
                            Text(
                                text = "DECK A (MASTER)",
                                color = NothingThemeTokens.MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }

                        if (currentSongA != null) {
                            item(key = "deck_a_${currentSongA.videoId}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NothingThemeTokens.SurfaceElevated)
                                        .border(1.dp, NothingThemeTokens.Red.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = "Playing",
                                        tint = NothingThemeTokens.Red,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    AsyncImage(
                                        model = currentSongA.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .thumbnailBorder(RoundedCornerShape(6.dp)),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentSongA.title,
                                            color = NothingThemeTokens.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = currentSongA.artist,
                                            color = NothingThemeTokens.MutedText,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        // Section: Deck B Standby
                        item(key = "section_deck_b") {
                            Text(
                                text = "DECK B (STANDBY)",
                                color = NothingThemeTokens.MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }

                        if (currentSongB != null) {
                            item(key = "deck_b_${currentSongB.videoId}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NothingThemeTokens.SurfaceElevated)
                                        .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NothingThemeTokens.Surface)
                                            .border(0.5.dp, NothingThemeTokens.Border, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "CUE",
                                            color = NothingThemeTokens.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    AsyncImage(
                                        model = currentSongB.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .thumbnailBorder(RoundedCornerShape(6.dp)),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentSongB.title,
                                            color = NothingThemeTokens.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = currentSongB.artist,
                                            color = NothingThemeTokens.MutedText,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        // Section: Upcoming DJ Setlist
                        item(key = "section_queue") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "UP NEXT IN QUEUE (${queue.size})",
                                    color = NothingThemeTokens.MutedText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                )
                                Text(
                                    text = "DRAG TO REORDER",
                                    color = NothingThemeTokens.MutedText.copy(alpha = 0.6f),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }

                        if (queue.isEmpty()) {
                            item(key = "empty_queue") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No other songs in queue. Search above to add songs.",
                                        color = NothingThemeTokens.MutedText,
                                        fontSize = 11.5.sp,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(queue, key = { index, song -> "q_${song.videoId}_$index" }) { index, s ->
                                val songProfile = remember(s.videoId) { AuraNeuralDjEngine.analyzeSong(s) }
                                val relationship = if (profileA != null) {
                                    profileA.key.getRelationship(songProfile.key)
                                } else {
                                    HarmonicRelationship.EXACT
                                }

                                var dragOffsetY by remember { mutableFloatStateOf(0f) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NothingThemeTokens.SurfaceElevated)
                                        .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Drag Reorder Handle
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .pointerInput(index, queue.size) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        haptics.play(Haptic.Select)
                                                    },
                                                    onDragEnd = {
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragCancel = {
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        val rowThreshold = 46.dp.toPx()
                                                        if (dragOffsetY > rowThreshold && index + 1 < queue.size) {
                                                            haptics.play(Haptic.Tick)
                                                            onReorder(index, index + 1)
                                                            dragOffsetY -= rowThreshold
                                                        } else if (dragOffsetY < -rowThreshold && index > 0) {
                                                            haptics.play(Haptic.Tick)
                                                            onReorder(index, index - 1)
                                                            dragOffsetY += rowThreshold
                                                        }
                                                    },
                                                )
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DragHandle,
                                            contentDescription = "Drag to reorder",
                                            tint = NothingThemeTokens.MutedText,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    AsyncImage(
                                        model = s.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .thumbnailBorder(RoundedCornerShape(6.dp))
                                            .background(NothingThemeTokens.Surface),
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            color = NothingThemeTokens.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = s.artist,
                                            color = NothingThemeTokens.MutedText,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Key & Match Badge
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${songProfile.bpm} BPM | ${songProfile.key}",
                                            color = NothingThemeTokens.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        val isGood = relationship.isCompatible
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isGood) NothingThemeTokens.Surface else NothingThemeTokens.SurfaceElevated)
                                                .border(0.5.dp, if (isGood) NothingThemeTokens.Red else NothingThemeTokens.Border, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                text = relationship.label,
                                                color = if (isGood) NothingThemeTokens.Red else NothingThemeTokens.MutedText,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Quick Load Pills
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NothingThemeTokens.Surface)
                                                .border(0.8.dp, NothingThemeTokens.Border, RoundedCornerShape(4.dp))
                                                .clickable {
                                                    haptics.play(Haptic.Select)
                                                    onSelectSongA(s)
                                                }
                                                .padding(horizontal = 5.dp, vertical = 3.dp),
                                        ) {
                                            Text("A", color = NothingThemeTokens.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NothingThemeTokens.Surface)
                                                .border(0.8.dp, NothingThemeTokens.Border, RoundedCornerShape(4.dp))
                                                .clickable {
                                                    haptics.play(Haptic.Select)
                                                    onSelectSongB(s)
                                                }
                                                .padding(horizontal = 5.dp, vertical = 3.dp),
                                        ) {
                                            Text("B", color = NothingThemeTokens.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    // Delete / Remove Item from Queue
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                haptics.play(Haptic.Tick)
                                                onRemoveItem(index)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove from Queue",
                                            tint = NothingThemeTokens.MutedText,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
