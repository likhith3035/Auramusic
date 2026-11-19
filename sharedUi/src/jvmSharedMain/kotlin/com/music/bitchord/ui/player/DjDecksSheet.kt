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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.ai.AuraNeuralDjEngine
import com.music.bitchord.data.ai.HarmonicRelationship
import com.music.bitchord.data.dj.CrossfaderCurve
import com.music.bitchord.data.dj.DjBridge
import com.music.bitchord.data.dj.DjSessionState
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.utils.containSheetGestures
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.NothingCard
import com.music.bitchord.ui.theme.NothingPill
import com.music.bitchord.ui.theme.NothingThemeTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

                CircleGlyph(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close DJ Console",
                    onClick = {
                        DjBridge.current.exitDjMode()
                        onClose()
                    },
                )
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
                            onChangeSong = null,
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
                            onChangeSong = { showTrackPicker = true },
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
                            onChangeSong = null,
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
                            onChangeSong = { showTrackPicker = true },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showTrackPicker) {
        DjTrackPickerModal(
            currentSong = deckASong,
            candidates = candidateQueue,
            onDismiss = { showTrackPicker = false },
            onSelectSong = { picked ->
                DjBridge.current.selectDeckBSong(picked)
                showTrackPicker = false
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

            // Beat Loops & Hot Cue Memory Strip
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
 * Queue candidate picker modal styled with Nothing OS card surfaces.
 */
@Composable
private fun DjTrackPickerModal(
    currentSong: Song?,
    candidates: List<Song>,
    onDismiss: () -> Unit,
    onSelectSong: (Song) -> Unit,
) {
    val haptics = rememberHaptics()

    val currentProfile = remember(currentSong?.videoId) {
        currentSong?.let { AuraNeuralDjEngine.analyzeSong(it) }
    }

    val sortedList = remember(currentSong?.videoId, candidates) {
        val list = if (currentSong != null) {
            AuraNeuralDjEngine.findHarmonicTransitions(currentSong, candidates)
        } else {
            candidates.map { AuraNeuralDjEngine.analyzeSong(it) }
        }
        if (currentProfile != null) {
            list.sortedBy { profile ->
                when (currentProfile.key.getRelationship(profile.key)) {
                    HarmonicRelationship.EXACT -> 0
                    HarmonicRelationship.RELATIVE -> 1
                    HarmonicRelationship.ENERGY_UP -> 2
                    HarmonicRelationship.ENERGY_DOWN -> 3
                    HarmonicRelationship.CLASH -> 4
                }
            }
        } else {
            list
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NothingCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = NothingThemeTokens.Surface,
            borderColor = NothingThemeTokens.Border,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LOAD TO DECK B",
                        color = NothingThemeTokens.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    CircleGlyph(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (sortedList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No other songs in queue",
                            color = NothingThemeTokens.MutedText,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(sortedList) { profile ->
                            val s = profile.song
                            val relationship = if (currentProfile != null) {
                                currentProfile.key.getRelationship(profile.key)
                            } else {
                                HarmonicRelationship.EXACT
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingThemeTokens.SurfaceElevated)
                                    .border(1.dp, NothingThemeTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptics.play(Haptic.Select)
                                        onSelectSong(s)
                                    }
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
                                        .background(NothingThemeTokens.Surface),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.title,
                                        color = NothingThemeTokens.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = s.artist,
                                        color = NothingThemeTokens.MutedText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${profile.bpm} BPM",
                                        color = NothingThemeTokens.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.key.toString(),
                                            color = NothingThemeTokens.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Spacer(Modifier.width(4.dp))
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
