package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val DeckAThemeColor = Color(0xFF00F5D4) // Neon Mint / Cyan
private val DeckBThemeColor = Color(0xFFFFB703) // Neon Amber / Warm Gold
private val MixerSurfaceColor = Color(0xFF0C0F17)

/**
 * Fullscreen modal displaying the Pro Dual-Deck DJ Console in AuraMusic.
 * Features Deck A, Deck B, equal-power / scratch crossfader, touch vinyl scratching,
 * tempo-preserving BPM Sync, 24dB DSP Filter sweeps, beat loops, hot cues, and automated transitions.
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
            .background(Color.Black.copy(alpha = 0.92f))
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
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (deckAPlaying || deckBPlaying) Color(0xFF00F5D4) else Color.Gray),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AURA PRO DUAL-DECK DJ",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                    )
                }

                IconButton(
                    onClick = {
                        DjBridge.current.exitDjMode()
                        onClose()
                    },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close DJ Console",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
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
                            themeColor = DeckAThemeColor,
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
                            themeColor = DeckBThemeColor,
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
                            themeColor = DeckAThemeColor,
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
                            themeColor = DeckBThemeColor,
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
 * Deck console card holding the touch-scratch turntable, BPM/Key pills, track metadata,
 * tactile CUE / PLAY controls, filter sweeps, beat loops, hot cues, and pitch nudge buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckCard(
    deckName: String,
    themeColor: Color,
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

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MixerSurfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)),
        modifier = modifier,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(themeColor.copy(alpha = 0.20f))
                            .border(1.dp, themeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = deckName,
                            color = themeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$bpm BPM",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = musicalKey,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                if (onChangeSong != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeColor.copy(alpha = 0.15f))
                            .clickable(onClick = onChangeSong)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "CHANGE",
                            color = themeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Middle: Turntable + Track Title & Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Vinyl Turntable with interactive touch-scratch gesture
                VinylTurntable(
                    thumbnailUrl = song?.thumbnailUrl,
                    isPlaying = isPlaying,
                    themeColor = themeColor,
                    onScrub = onScrub,
                    modifier = Modifier.size(76.dp),
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "No track loaded",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = song?.artist ?: "Unknown artist",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    val posSec = positionMs / 1000L
                    val durSec = durationMs / 1000L
                    Text(
                        text = String.format("%02d:%02d / %02d:%02d", posSec / 60, posSec % 60, durSec / 60, durSec % 60),
                        color = themeColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Filter & Pitch Nudge Vertical Bar
                Column(horizontalAlignment = Alignment.End) {
                    // Filter readout
                    val filterLabel = when {
                        filterAmount < -0.05f -> "LPF ${kotlin.math.abs((filterAmount * 100).roundToInt())}%"
                        filterAmount > 0.05f -> "HPF ${(filterAmount * 100).roundToInt()}%"
                        else -> "FLTR OFF"
                    }
                    Text(
                        text = filterLabel,
                        color = if (kotlin.math.abs(filterAmount) > 0.05f) themeColor else Color.White.copy(alpha = 0.45f),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    // Nudge buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    haptics.play(Haptic.Tick)
                                    onPitchNudge(false)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    haptics.play(Haptic.Tick)
                                    onPitchNudge(true)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Pro Rows: Loops & Hot Cues
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Beat Loops
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(2, 4, 8).forEach { beats ->
                        val active = loopBeats == beats
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (active) themeColor else Color.White.copy(alpha = 0.07f))
                                .clickable {
                                    haptics.play(Haptic.Select)
                                    onToggleLoop(beats)
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "${beats}B",
                                color = if (active) Color.Black else Color.White.copy(alpha = 0.75f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }

                // Hot Cues (1, 2, 3)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0..2) {
                        val cuePos = hotCues.getOrNull(i)
                        val isSet = cuePos != null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isSet) themeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isSet) themeColor else Color.White.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
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
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = if (isSet && cuePos != null) {
                                    val sec = (cuePos / 1000L)
                                    String.format("%d:%02d", sec / 60, sec % 60)
                                } else {
                                    "H${i + 1}"
                                },
                                color = if (isSet) themeColor else Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
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
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCue)
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "CUE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // BRAKE (Turntable Motor Stop) Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBraking) Color(0xFFFF3366).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
                        .border(1.dp, if (isBraking) Color(0xFFFF3366) else Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .clickable(enabled = isPlaying && !isBraking, onClick = onBrake)
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isBraking) "STOPPING" else "BRAKE",
                        color = if (isBraking) Color(0xFFFF3366) else Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // PLAY/PAUSE Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // LOW KILL (Bass Cut) Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (lowKill) Color(0xFFFF4D4D).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                        .border(1.dp, if (lowKill) Color(0xFFFF4D4D) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleLowKill)
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "LOW KILL",
                        color = if (lowKill) Color(0xFFFF4D4D) else Color.White.copy(alpha = 0.70f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
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
                            thumbColor = if (kotlin.math.abs(filterAmount) > 0.08f) themeColor else Color.Gray,
                            activeTrackColor = themeColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Horizontal Dual Stacked Waveform Phase Meter.
 * Displays moving beat transients and kick-drum downbeats for both Deck A and Deck B,
 * aligned against a center playhead with live phase differential calculation.
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
            .background(Color(0xFF04060A))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // Phase status readout
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PHASE METER",
                color = Color.White.copy(alpha = 0.40f),
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
                    if (deltaMs > 0) "+${deltaMs}ms (SLOW B)" else "${deltaMs}ms (NUDGE B)"
                },
                color = if (isPhaseAligned) Color(0xFF00E676) else if (deckAPlaying && deckBPlaying) Color(0xFFFFD166) else Color.White.copy(alpha = 0.50f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }

        // Dual Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val centerX = w / 2f

                // Deck A (Top Lane, Cyan)
                val beatDistPx = 36f
                val offsetA = (deckAPosMs.toFloat() / beatMsA) * beatDistPx
                val startBeatA = ((offsetA - centerX) / beatDistPx).toInt() - 1
                val endBeatA = ((offsetA + centerX) / beatDistPx).toInt() + 1

                for (b in startBeatA..endBeatA) {
                    val beatX = centerX + (b * beatDistPx - offsetA)
                    if (beatX in 0f..w) {
                        val isDownbeat = (b % 4 == 0)
                        val barH = if (isDownbeat) 10f else 6f
                        drawLine(
                            color = DeckAThemeColor.copy(alpha = if (isDownbeat) 0.90f else 0.45f),
                            start = Offset(beatX, midY - 2f),
                            end = Offset(beatX, midY - 2f - barH),
                            strokeWidth = if (isDownbeat) 2.5f else 1.5f,
                        )
                    }
                }

                // Midline separator
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1f,
                )

                // Deck B (Bottom Lane, Amber)
                val offsetB = (deckBPosMs.toFloat() / beatMsB) * beatDistPx
                val startBeatB = ((offsetB - centerX) / beatDistPx).toInt() - 1
                val endBeatB = ((offsetB + centerX) / beatDistPx).toInt() + 1

                for (b in startBeatB..endBeatB) {
                    val beatX = centerX + (b * beatDistPx - offsetB)
                    if (beatX in 0f..w) {
                        val isDownbeat = (b % 4 == 0)
                        val barH = if (isDownbeat) 10f else 6f
                        drawLine(
                            color = DeckBThemeColor.copy(alpha = if (isDownbeat) 0.90f else 0.45f),
                            start = Offset(beatX, midY + 2f),
                            end = Offset(beatX, midY + 2f + barH),
                            strokeWidth = if (isDownbeat) 2.5f else 1.5f,
                        )
                    }
                }

                // Center playhead cursor line
                drawLine(
                    color = Color(0xFFFF3366),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, h),
                    strokeWidth = 2f,
                )
            }
        }
    }
}

/**
 * Center DJ Mixer Console with the Equal-Power / Cut Crossfader, Curve Selector, Beat SYNC, Auto-Drop, Takeover,
 * Hamster Reversible Crossfader, and Split-Cue Headphone Monitoring.
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

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF07090F),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = modifier,
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isBeatSynced) DeckAThemeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .border(1.dp, if (isBeatSynced) DeckAThemeColor else Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onSync)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isBeatSynced) "SYNCED" else "SYNC",
                        color = if (isBeatSynced) DeckAThemeColor else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // CURVE MODE SWITCH (SMOOTH vs CUT)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onCurveToggle)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (curve == CrossfaderCurve.SMOOTH) "CRV:SMOOTH" else "CRV:CUT",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // HAMSTER REVERSE CROSSFADER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isHamsterMode) Color(0xFFFFB703).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .border(1.dp, if (isHamsterMode) Color(0xFFFFB703) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onHamsterToggle)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isHamsterMode) "HAM:REV" else "HAM:NORM",
                        color = if (isHamsterMode) Color(0xFFFFB703) else Color.White.copy(alpha = 0.70f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // SPLIT-CUE HEADPHONE MONITORING
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSplitCueEnabled) Color(0xFF00E676).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .border(1.dp, if (isSplitCueEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onSplitCueToggle)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSplitCueEnabled) "SPLIT:ON" else "SPLIT:OFF",
                        color = if (isSplitCueEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.70f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isAutoDropping) {
                                Brush.horizontalGradient(listOf(DeckAThemeColor, DeckBThemeColor))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF2E3856), Color(0xFF1E2436)))
                            },
                        )
                        .border(1.dp, if (isAutoDropping) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable(enabled = !isAutoDropping, onClick = onAutoDrop)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isAutoDropping) "DROPPING..." else "AUTO-DROP (3.5S)",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(Modifier.width(8.dp))

                // COMMIT / DROP TAKEOVER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DeckBThemeColor.copy(alpha = 0.18f))
                        .border(1.dp, DeckBThemeColor.copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onCommit)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "TAKEOVER DECK B",
                        color = DeckBThemeColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
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
                    text = "DECK A: $percentA%",
                    color = DeckAThemeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = if (curve == CrossfaderCurve.SMOOTH) "EQUAL POWER" else "SCRATCH CUT",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "DECK B: $percentB%",
                    color = DeckBThemeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // Horizontal Crossfader Slider with tactile detents
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
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DeckBThemeColor,
                        inactiveTrackColor = DeckAThemeColor,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Visual Turntable with realistic concentric vinyl grooves, rotating album art,
 * and touch-scratch drag gesture support.
 */
@Composable
private fun VinylTurntable(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    themeColor: Color,
    onScrub: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, isDragging) {
        while (isActive && isPlaying && !isDragging) {
            rotationAngle = (rotationAngle + 2.4f) % 360f
            delay(16L) // ~60 FPS
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, themeColor.copy(alpha = 0.40f), CircleShape)
            .background(Color(0xFF0F121B))
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
                        rotationAngle = (rotationAngle + delta * 1.8f) % 360f
                        val scrubDeltaMs = (delta * 40L).toLong()
                        onScrub(scrubDeltaMs)
                        haptics.play(Haptic.Tick)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Vinyl grooves canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationAngle },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // Concentric vinyl groove rings
            val grooveColors = Color.White.copy(alpha = 0.04f)
            for (step in 1..6) {
                drawCircle(
                    color = grooveColors,
                    radius = radius * (0.45f + step * 0.08f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }

        // Center Album Art Record Label
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .graphicsLayer { rotationZ = rotationAngle },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Center spindle hole
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(0.5.dp, Color.White, CircleShape),
            )
        }
    }
}

/**
 * Queue candidate picker modal allowing the user to select any song from the active queue
 * to load onto Deck B for mixing.
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
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0E121E),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeckBThemeColor.copy(alpha = 0.40f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .padding(4.dp),
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
                        text = "LOAD ONTO DECK B",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (sortedList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No other songs in queue",
                            color = Color.White.copy(alpha = 0.5f),
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
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
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.DarkGray),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = s.artist,
                                        color = Color.White.copy(alpha = 0.60f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${profile.bpm} BPM",
                                        color = DeckBThemeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.key.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        val badgeColor = when (relationship) {
                                            HarmonicRelationship.EXACT -> Color(0xFF00E676)
                                            HarmonicRelationship.RELATIVE -> Color(0xFF00F5D4)
                                            HarmonicRelationship.ENERGY_UP -> Color(0xFFB388FF)
                                            HarmonicRelationship.ENERGY_DOWN -> Color(0xFF448AFF)
                                            HarmonicRelationship.CLASH -> Color.White.copy(alpha = 0.35f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(badgeColor.copy(alpha = 0.18f))
                                                .border(0.5.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                text = relationship.label,
                                                color = badgeColor,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
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
