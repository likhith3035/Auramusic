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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
private val CardBorderColor = Color.White.copy(alpha = 0.12f)

/**
 * Fullscreen modal displaying the Dual-Deck DJ Console in AuraMusic.
 * Features Deck A, Deck B, equal-power horizontal crossfader,
 * tempo-preserving BPM Sync, auto-drop transition, cue points, and vinyl turntables.
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
    val isBeatSynced by DjSessionState.isBeatSynced.collectAsStateWithLifecycle()
    val isAutoDropping by DjSessionState.isAutoDropping.collectAsStateWithLifecycle()
    val lowKillA by DjSessionState.lowKillA.collectAsStateWithLifecycle()
    val lowKillB by DjSessionState.lowKillB.collectAsStateWithLifecycle()
    val candidateQueue by DjSessionState.candidateQueue.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(Color.Black.copy(alpha = 0.90f))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            // Header
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (deckAPlaying || deckBPlaying) Color(0xFF00F5D4) else Color.Gray),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AURA DUAL-DECK DJ",
                        color = Color.White,
                        fontSize = 15.sp,
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
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close DJ Console",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isWide = maxWidth >= 620.dp

                if (isWide) {
                    // Landscape / Tablet layout: Deck A on left, Mixer center, Deck B on right
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Deck A
                        DeckCard(
                            deckName = "DECK A",
                            themeColor = DeckAThemeColor,
                            song = deckASong,
                            isPlaying = deckAPlaying,
                            positionMs = deckAPos,
                            durationMs = deckADur,
                            bpm = deckABpm,
                            musicalKey = deckAKey,
                            speed = deckASpeed,
                            lowKill = lowKillA,
                            onPlayPause = { DjBridge.current.toggleDeckAPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckA()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillA() },
                            onSpeedChange = { DjBridge.current.setDeckASpeed(it) },
                            onChangeSong = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )

                        // Center Mixer
                        MixerConsole(
                            crossfader = crossfader,
                            isBeatSynced = isBeatSynced,
                            isAutoDropping = isAutoDropping,
                            deckABpm = deckABpm,
                            deckBBpm = deckBBpm,
                            onCrossfaderChange = { fraction ->
                                DjBridge.current.setCrossfader(fraction)
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
                            modifier = Modifier.width(200.dp).fillMaxHeight(),
                        )

                        // Deck B
                        DeckCard(
                            deckName = "DECK B",
                            themeColor = DeckBThemeColor,
                            song = deckBSong,
                            isPlaying = deckBPlaying,
                            positionMs = deckBPos,
                            durationMs = deckBDur,
                            bpm = deckBBpm,
                            musicalKey = deckBKey,
                            speed = deckBSpeed,
                            lowKill = lowKillB,
                            onPlayPause = { DjBridge.current.toggleDeckBPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckB()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillB() },
                            onSpeedChange = { DjBridge.current.setDeckBSpeed(it) },
                            onChangeSong = { showTrackPicker = true },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                } else {
                    // Portrait Phone layout: Deck A (top), Mixer (middle), Deck B (bottom)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Deck A Card
                        DeckCard(
                            deckName = "DECK A",
                            themeColor = DeckAThemeColor,
                            song = deckASong,
                            isPlaying = deckAPlaying,
                            positionMs = deckAPos,
                            durationMs = deckADur,
                            bpm = deckABpm,
                            musicalKey = deckAKey,
                            speed = deckASpeed,
                            lowKill = lowKillA,
                            onPlayPause = { DjBridge.current.toggleDeckAPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckA()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillA() },
                            onSpeedChange = { DjBridge.current.setDeckASpeed(it) },
                            onChangeSong = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )

                        // Center Mixer
                        MixerConsole(
                            crossfader = crossfader,
                            isBeatSynced = isBeatSynced,
                            isAutoDropping = isAutoDropping,
                            deckABpm = deckABpm,
                            deckBBpm = deckBBpm,
                            onCrossfaderChange = { fraction ->
                                DjBridge.current.setCrossfader(fraction)
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

                        // Deck B Card
                        DeckCard(
                            deckName = "DECK B",
                            themeColor = DeckBThemeColor,
                            song = deckBSong,
                            isPlaying = deckBPlaying,
                            positionMs = deckBPos,
                            durationMs = deckBDur,
                            bpm = deckBBpm,
                            musicalKey = deckBKey,
                            speed = deckBSpeed,
                            lowKill = lowKillB,
                            onPlayPause = { DjBridge.current.toggleDeckBPlayPause() },
                            onCue = {
                                haptics.play(Haptic.Select)
                                DjBridge.current.cueDeckB()
                            },
                            onToggleLowKill = { DjBridge.current.toggleLowKillB() },
                            onSpeedChange = { DjBridge.current.setDeckBSpeed(it) },
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
 * Deck console card holding the spinning turntable, BPM/Key pills, track metadata,
 * tactile CUE / PLAY controls, and tempo adjustment.
 */
@Composable
private fun DeckCard(
    deckName: String,
    themeColor: Color,
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    bpm: Int,
    musicalKey: String,
    speed: Float,
    lowKill: Boolean,
    onPlayPause: () -> Unit,
    onCue: () -> Unit,
    onToggleLowKill: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onChangeSong: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MixerSurfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
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
                    // BPM Badge
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
                    // Musical Key Badge
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
                            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Vinyl Turntable
                VinylTurntable(
                    thumbnailUrl = song?.thumbnailUrl,
                    isPlaying = isPlaying,
                    themeColor = themeColor,
                    modifier = Modifier.size(86.dp),
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "No track loaded",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song?.artist ?: "Unknown artist",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    // Time indicator
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
            }

            // Transport Controls: CUE, PLAY/PAUSE, LOW KILL, TEMPO
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
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "CUE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // PLAY/PAUSE Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // LOW KILL (Bass Cut) Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (lowKill) Color(0xFFFF4D4D).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                        .border(1.dp, if (lowKill) Color(0xFFFF4D4D) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleLowKill)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
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

                // Pitch Speed readout
                Text(
                    text = String.format("%.2fx", speed),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/**
 * Center DJ Mixer Console with the Equal-Power Crossfader, Beat SYNC, Auto-Drop, and Commit button.
 */
@Composable
private fun MixerConsole(
    crossfader: Float,
    isBeatSynced: Boolean,
    isAutoDropping: Boolean,
    deckABpm: Int,
    deckBBpm: Int,
    onCrossfaderChange: (Float) -> Unit,
    onSync: () -> Unit,
    onAutoDrop: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    // Equal-power law calculations
    val angle = crossfader * (PI / 2.0).toFloat()
    val gainA = cos(angle)
    val gainB = sin(angle)
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
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Action Buttons: SYNC, AUTO-DROP, COMMIT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // BEAT SYNC BUTTON
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBeatSynced) DeckAThemeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .border(1.dp, if (isBeatSynced) DeckAThemeColor else Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onSync)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isBeatSynced) "SYNCED" else "SYNC",
                        color = if (isBeatSynced) DeckAThemeColor else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // AUTO-DROP BUTTON
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isAutoDropping) {
                                Brush.horizontalGradient(listOf(DeckAThemeColor, DeckBThemeColor))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF2E3856), Color(0xFF1E2436)))
                            },
                        )
                        .border(1.dp, if (isAutoDropping) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isAutoDropping, onClick = onAutoDrop)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isAutoDropping) "DROPPING..." else "AUTO-DROP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // COMMIT / DROP TAKEOVER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeckBThemeColor.copy(alpha = 0.18f))
                        .border(1.dp, DeckBThemeColor.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCommit)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "TAKEOVER",
                        color = DeckBThemeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

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
                    text = "EQUAL POWER",
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

            // Horizontal Equal-Power Crossfader Slider
            var lastHapticPos by remember { mutableFloatStateOf(crossfader) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
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
 * Visual Turntable with realistic concentric vinyl grooves and rotating album art.
 */
@Composable
private fun VinylTurntable(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    themeColor: Color,
    modifier: Modifier = Modifier,
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            rotationAngle = (rotationAngle + 2.4f) % 360f
            delay(16L) // ~60 FPS
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, themeColor.copy(alpha = 0.40f), CircleShape)
            .background(Color(0xFF0F121B)),
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
                .size(42.dp)
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

    val harmonicList = remember(currentSong?.videoId, candidates) {
        if (currentSong != null) {
            AuraNeuralDjEngine.findHarmonicTransitions(currentSong, candidates)
        } else {
            candidates.map { AuraNeuralDjEngine.analyzeSong(it) }
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

                if (harmonicList.isEmpty()) {
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
                        items(harmonicList) { profile ->
                            val s = profile.song
                            val isCompatible = currentProfile?.key?.isCompatibleWith(profile.key) == true

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
                                    Text(
                                        text = profile.key.toString(),
                                        color = if (isCompatible) Color(0xFF00F5D4) else Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
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
