package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import com.music.bitchord.data.network.AuraNetworkMonitor
import com.music.bitchord.data.network.NetworkTier
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.video_quality
import com.music.bitchord.sharedui.resources.video_quality_auto
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.ui.theme.NothingThemeTokens
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.theme.HideSystemBars
import kotlinx.coroutines.delay
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.music.bitchord.playback.PlaybackPosition
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** How long the controls stay over a playing video untouched. */
private const val VIDEO_CONTROLS_IDLE_MS = 3_500L

/**
 * A music video on a phone held sideways: the picture is the whole screen,
 * letterboxed in black, with the system bars away. A tap brings the controls
 * up over it — the title, the scrubber and the transport — and they stand
 * down again a few seconds after the last touch while it plays, as in every
 * video player. [onExitFullscreen] goes back to the ordinary landscape player.
 */
@Composable
internal fun FullscreenVideoPlayer(
    song: Song,
    isPlaying: Boolean,
    artRequest: ImageRequest,
    onRenderedChanged: (Boolean) -> Unit,
    rendered: Boolean,
    onExitFullscreen: () -> Unit,
    onOpenQuality: () -> Unit,
    scrubber: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    position: PlaybackPosition? = null,
    durationMs: Long = 0L,
    onSeek: ((Long) -> Unit)? = null,
) {
    HideSystemBars(true)
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val userPrefersFill by PlayerSettings.fullscreenVideoFill.collectAsStateWithLifecycle()
    val hiResAudio by PlayerSettings.hiResAudioOverVideo.collectAsStateWithLifecycle()
    var isFillScreen by remember(userPrefersFill) { mutableStateOf(userPrefersFill) }
    var isLocked by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isFastForwarding by remember { mutableStateOf(false) }
    var seekNoticeSide by remember { mutableIntStateOf(0) }
    var seekNoticeSeconds by remember { mutableIntStateOf(0) }
    var controlsShown by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }

    val screenBrightness by PlayerPlatform.host.screenBrightness.collectAsStateWithLifecycle()
    val systemVolume by PlayerPlatform.host.volume.level.collectAsStateWithLifecycle()
    var activeHudSide by remember { mutableIntStateOf(0) }
    var currentBrightnessValue by remember(screenBrightness) { mutableFloatStateOf(screenBrightness) }
    var currentVolumeValue by remember(systemVolume) { mutableFloatStateOf(systemVolume) }
    var hudHideJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            PlayerPlatform.host.restorePlaybackSpeed()
        }
    }

    PlayerBackHandler(enabled = true) {
        if (isLocked) {
            isLocked = false
            haptics.play(Haptic.ToggleOff)
        } else {
            onExitFullscreen()
        }
    }

    LaunchedEffect(controlsShown, isPlaying, touches) {
        if (controlsShown && isPlaying) {
            delay(VIDEO_CONTROLS_IDLE_MS)
            controlsShown = false
        }
    }

    val maxPanX = if (scale > 1f) (scale - 1f) * 600f else 0f
    val maxPanY = if (scale > 1f) (scale - 1f) * 350f else 0f
    val clampedPanOffset = Offset(
        panOffset.x.coerceIn(-maxPanX, maxPanX),
        panOffset.y.coerceIn(-maxPanY, maxPanY),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(isFillScreen, durationMs, isLocked) {
                coroutineScope {
                    var holdJob: Job? = null
                    var singleTapJob: Job? = null
                    var lastTapTime = 0L
                    var lastTapSide = 0
                    var accumulatedSeekSeconds = 0

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (isLocked) continue
                            val changes = event.changes

                            if (changes.size >= 2) {
                                singleTapJob?.cancel()
                                holdJob?.cancel()
                                if (isFastForwarding) {
                                    isFastForwarding = false
                                    PlayerPlatform.host.restorePlaybackSpeed()
                                }

                                val p0 = changes[0].position
                                val p1 = changes[1].position
                                val prev0 = changes[0].previousPosition
                                val prev1 = changes[1].previousPosition

                                val dx = p0.x - p1.x
                                val dy = p0.y - p1.y
                                val currentDist = kotlin.math.hypot(dx, dy)

                                val prevDx = prev0.x - prev1.x
                                val prevDy = prev0.y - prev1.y
                                val prevDist = kotlin.math.hypot(prevDx, prevDy)

                                if (prevDist > 10f && currentDist > 10f) {
                                    val zoom = currentDist / prevDist
                                    scale = (scale * zoom).coerceIn(1.0f, 3.5f)
                                    if (scale > 1.0f) {
                                        val currentCentroid = (p0 + p1) / 2f
                                        val prevCentroid = (prev0 + prev1) / 2f
                                        panOffset += (currentCentroid - prevCentroid)
                                    } else {
                                        panOffset = Offset.Zero
                                    }
                                }
                                changes.forEach { it.consume() }
                            } else if (changes.size == 1) {
                                val change = changes[0]
                                if (change.pressed && !change.previousPressed) {
                                    val downPos = change.position
                                    var held = false
                                    var isVerticalSwipe = false
                                    var lastHapticTick = currentVolumeValue

                                    holdJob?.cancel()
                                    holdJob = launch {
                                        delay(300L)
                                        if (scale <= 1.05f && !isVerticalSwipe) {
                                            held = true
                                            isFastForwarding = true
                                            haptics.play(Haptic.ToggleOn)
                                            PlayerPlatform.host.setTemporaryPlaybackSpeed(2.0f)
                                        }
                                    }

                                    while (true) {
                                        val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                                        val nextChange = nextEvent.changes.firstOrNull { it.id == change.id } ?: break

                                        val moveDx = nextChange.position.x - downPos.x
                                        val moveDy = nextChange.position.y - downPos.y
                                        val movedDist = kotlin.math.hypot(moveDx, moveDy)

                                        val screenWidth = size.width
                                        val screenHeight = size.height

                                        if (scale <= 1.05f && !held && kotlin.math.abs(moveDy) > 18f && kotlin.math.abs(moveDy) > kotlin.math.abs(moveDx) * 1.35f) {
                                            isVerticalSwipe = true
                                            holdJob?.cancel()
                                            val dyDelta = nextChange.position.y - nextChange.previousPosition.y
                                            val step = -dyDelta / (screenHeight * 0.65f)

                                            if (downPos.x < screenWidth * 0.44f) {
                                                activeHudSide = -1
                                                hudHideJob?.cancel()
                                                val newBri = (currentBrightnessValue + step).coerceIn(0.01f, 1.0f)
                                                currentBrightnessValue = newBri
                                                PlayerPlatform.host.setScreenBrightness(newBri)
                                                nextChange.consume()
                                                hudHideJob = launch {
                                                    delay(1200L)
                                                    activeHudSide = 0
                                                }
                                            } else if (downPos.x > screenWidth * 0.56f) {
                                                activeHudSide = 1
                                                hudHideJob?.cancel()
                                                val curVol = PlayerPlatform.host.volume.level.value
                                                val newVol = (curVol + step).coerceIn(0f, 1f)
                                                currentVolumeValue = newVol
                                                PlayerPlatform.host.volume.set(newVol)
                                                if (kotlin.math.abs(newVol - lastHapticTick) >= 0.05f) {
                                                    haptics.play(Haptic.Select)
                                                    lastHapticTick = newVol
                                                }
                                                nextChange.consume()
                                                hudHideJob = launch {
                                                    delay(1200L)
                                                    activeHudSide = 0
                                                }
                                            }
                                        } else if (!held && movedDist > 25f) {
                                            holdJob?.cancel()
                                            if (scale > 1.0f) {
                                                panOffset += (nextChange.position - nextChange.previousPosition)
                                                nextChange.consume()
                                            }
                                        } else if (scale > 1.0f && movedDist > 10f) {
                                            panOffset += (nextChange.position - nextChange.previousPosition)
                                            nextChange.consume()
                                        }

                                        if (!nextChange.pressed && nextChange.previousPressed) {
                                            holdJob?.cancel()
                                            if (held || isFastForwarding) {
                                                isFastForwarding = false
                                                haptics.play(Haptic.ToggleOff)
                                                PlayerPlatform.host.restorePlaybackSpeed()
                                                nextChange.consume()
                                            } else if (isVerticalSwipe) {
                                                nextChange.consume()
                                            } else if (movedDist < 25f) {
                                                val upTime = System.currentTimeMillis()
                                                val screenWidth = size.width
                                                val side = when {
                                                    downPos.x < screenWidth * 0.38f -> -1
                                                    downPos.x > screenWidth * 0.62f -> 1
                                                    else -> 0
                                                }

                                                val isDoubleTap = (upTime - lastTapTime < 320L) && (side != 0) && (side == lastTapSide)

                                                if (isDoubleTap) {
                                                    singleTapJob?.cancel()
                                                    lastTapTime = upTime
                                                    accumulatedSeekSeconds += 5
                                                    haptics.play(Haptic.Select)

                                                    val curPos = position?.positionMs ?: 0L
                                                    if (side == -1) {
                                                        val target = (curPos - 5000L).coerceAtLeast(0L)
                                                        onSeek?.invoke(target)
                                                        seekNoticeSide = -1
                                                        seekNoticeSeconds = accumulatedSeekSeconds
                                                    } else {
                                                        val target = (curPos + 5000L).coerceAtMost(durationMs)
                                                        onSeek?.invoke(target)
                                                        seekNoticeSide = 1
                                                        seekNoticeSeconds = accumulatedSeekSeconds
                                                    }
                                                    launch {
                                                        delay(700L)
                                                        seekNoticeSide = 0
                                                        accumulatedSeekSeconds = 0
                                                    }
                                                } else {
                                                    lastTapTime = upTime
                                                    lastTapSide = side
                                                    accumulatedSeekSeconds = 0

                                                    if (side == 0) {
                                                        controlsShown = !controlsShown
                                                        touches++
                                                    } else {
                                                        singleTapJob?.cancel()
                                                        singleTapJob = launch {
                                                            delay(260L)
                                                            controlsShown = !controlsShown
                                                            touches++
                                                        }
                                                    }
                                                }
                                                nextChange.consume()
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
    ) {
        if (!rendered) VideoLoadingBackdrop(Modifier.fillMaxSize())
        MusicVideoSurface(
            videoId = song.videoId,
            fit = !isFillScreen,
            onRenderedChanged = onRenderedChanged,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = clampedPanOffset.x
                    translationY = clampedPanOffset.y
                },
        )

        AnimatedVisibility(
            visible = isFastForwarding,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1))
                    )
                    Text(
                        text = ">> 2X SPEED",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = scale > 1.05f,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isFastForwarding) 62.dp else 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable {
                        scale = 1f
                        panOffset = Offset.Zero
                        haptics.play(Haptic.Select)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val formattedScale = ((scale * 10).toInt() / 10f).toString()
                    Text(
                        text = "ZOOM ${formattedScale}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = "[RESET]",
                        color = Color(0xFFA78BFA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = seekNoticeSide != 0,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.85f),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(if (seekNoticeSide == -1) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 48.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (seekNoticeSide == -1) {
                        Text(
                            text = "<<",
                            color = Color(0xFF6366F1),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "-${seekNoticeSeconds}s",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    } else {
                        Text(
                            text = "+${seekNoticeSeconds}s",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = ">>",
                            color = Color(0xFF6366F1),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }

        // Vertical Swipe Brightness & Volume HUDs
        AnimatedVisibility(
            visible = activeHudSide == -1,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.85f),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp),
        ) {
            VerticalGestureCapsuleHud(
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Brightness4,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                },
                percent = (currentBrightnessValue * 100).roundToInt(),
                progress = currentBrightnessValue,
                isLeft = true,
            )
        }

        AnimatedVisibility(
            visible = activeHudSide == 1,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.85f),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp),
        ) {
            val vol = currentVolumeValue
            VerticalGestureCapsuleHud(
                icon = {
                    Icon(
                        imageVector = when {
                            vol <= 0f -> Icons.AutoMirrored.Rounded.VolumeMute
                            vol < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                            else -> Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                },
                percent = (vol * 100).roundToInt(),
                progress = vol,
                isLeft = false,
            )
        }

        // Floating Lock Notice when Locked
        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .clickable {
                        isLocked = false
                        controlsShown = true
                        haptics.play(Haptic.ToggleOff)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "LOCKED - TAP TO UNLOCK",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsShown && !isLocked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.62f),
                            0.30f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    )
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                touches++
                            }
                        }
                    }
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    VideoCornerControls(
                        fullscreen = true,
                        onToggleFullscreen = onExitFullscreen,
                        onOpenQuality = onOpenQuality,
                        isFillScreen = isFillScreen,
                        onToggleFillScreen = {
                            val next = !isFillScreen
                            isFillScreen = next
                            PlayerSettings.setFullscreenVideoFill(next)
                            haptics.play(Haptic.Select)
                        },
                        isLocked = isLocked,
                        onToggleLock = {
                            isLocked = true
                            controlsShown = false
                        },
                        hiResAudio = hiResAudio,
                        onToggleHiResAudio = {
                            PlayerSettings.setHiResAudioOverVideo(!hiResAudio)
                        },
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 420.dp),
                ) {
                    transport()
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    scrubber()
                }
            }
        }
    }
}

/**
 * What stands in for a music video until its first frame lands: black, with a
 * spinner, rather than the still artwork. Shared by the portrait card and the
 * fullscreen landscape player.
 */
@Composable
internal fun VideoLoadingBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
    }
}

/**
 * The glass boxes in a music video's top-right corner: the FILL/FIT toggle (in fullscreen),
 * the quality box, and the fullscreen toggle.
 */
@Composable
internal fun VideoCornerControls(
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onOpenQuality: () -> Unit,
    isFillScreen: Boolean = true,
    onToggleFillScreen: (() -> Unit)? = null,
    isLocked: Boolean = false,
    onToggleLock: (() -> Unit)? = null,
    hiResAudio: Boolean = false,
    onToggleHiResAudio: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (fullscreen && onToggleHiResAudio != null) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (hiResAudio) Color(0xFF6366F1).copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.38f))
                    .border(
                        1.dp,
                        if (hiResAudio) Color(0xFF8B5CF6).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp),
                    )
                    .clickable {
                        haptics.play(Haptic.Select)
                        onToggleHiResAudio()
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hiResAudio) "HI-RES AUDIO" else "STD AUDIO",
                    color = if (hiResAudio) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                )
            }
        }
        if (fullscreen && onToggleFillScreen != null) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFillScreen) Color(0xFF6366F1).copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.38f))
                    .border(
                        1.dp,
                        if (isFillScreen) Color(0xFF8B5CF6).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp),
                    )
                    .clickable {
                        haptics.play(Haptic.Select)
                        onToggleFillScreen()
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isFillScreen) "FILL" else "FIT",
                    color = if (isFillScreen) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                )
            }
        }
        if (fullscreen && onToggleLock != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable {
                        haptics.play(Haptic.ToggleOn)
                        onToggleLock()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        VideoQualityBox(onClick = onOpenQuality)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onToggleFullscreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun MusicVideoQualityUi.selected(): Int? =
    if (held == null || held !in heights) activeHeight else held

/**
 * The playing video's quality: 360p, 480p, HD, FullHD, 2K, 4K — what is on
 * screen, which is not always what was picked (4K held, then a video shot in
 * 1080p). Nothing until the renditions are known and there is a choice to make.
 */
@Composable
private fun VideoQualityBox(onClick: () -> Unit) {
    val quality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    if (quality.heights.size < 2) return
    val label = quality.selected()?.let(::videoQualityLabel) ?: "Auto"
    Box(
        modifier = Modifier
            .height(30.dp)
            .widthIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "videoQualityLabel",
        ) { text ->
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * The quality list, in the same drawer as the audio output: Auto, then every
 * height this video offers, the one playing marked. Picking one holds it from
 * now on — for the next video too, where that video has it.
 */
@Composable
internal fun VideoQualitySheet(hazeState: HazeState, onDismiss: () -> Unit) {
    val quality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    val selected = quality.selected()
    PlayerDrawer(
        hazeState = hazeState,
        title = stringResource(Res.string.video_quality),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QualityRow(
                name = stringResource(Res.string.video_quality_auto),
                detail = if (quality.held == null) selected?.let(::videoQualityLabel) else null,
                active = quality.held == null,
            ) {
                PlayerPlatform.host.setMusicVideoQuality(null)
                onDismiss()
            }
            quality.heights.forEach { height ->
                QualityRow(
                    name = videoQualityLabel(height),
                    detail = "${height}p",
                    active = quality.held != null && selected == height,
                ) {
                    PlayerPlatform.host.setMusicVideoQuality(height)
                    onDismiss()
                }
            }
        }
    }
}

/** One row of [VideoQualitySheet], drawn as the audio output drawer draws an output. */
@Composable
private fun QualityRow(name: String, detail: String?, active: Boolean, onSelect: () -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ROW_SHAPE)
            .background(Color.White.copy(alpha = if (active) 0.10f else 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !active,
            ) {
                haptics.play(Haptic.Select)
                onSelect()
            }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = Color.White.copy(alpha = if (active) 1f else 0.85f),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (active) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * YouTube Music style discrete frosted glass pill switch to toggle between
 * Song (audio-only with album cover/canvas) and Video (synchronized picture).
 */
@Composable
internal fun SongVideoModePill(
    isVideo: Boolean,
    onSelectMode: (Boolean) -> Unit,
    onRequestBandwidthCheck: () -> Unit = {},
    modifier: Modifier = Modifier,
    nothingOsMode: Boolean = false,
) {
    val networkQuality by AuraNetworkMonitor.quality.collectAsStateWithLifecycle()
    val signalDotColor = when (networkQuality.tier) {
        NetworkTier.FAST -> Color(0xFF00E676)
        NetworkTier.MODERATE -> Color(0xFFFFB74D)
        NetworkTier.SLOW -> Color(0xFFFF5252)
        NetworkTier.OFFLINE -> Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = Color.Black.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.18f),
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // "Song" Option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (!isVideo) {
                            Color(0xFF6366F1).copy(alpha = 0.40f)
                        } else Color.Transparent,
                    )
                    .clickable { onSelectMode(false) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = if (!isVideo) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "Song",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (!isVideo) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp,
                        ),
                        color = if (!isVideo) Color.White else Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            // "Video" Option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isVideo) {
                            Color(0xFF8B5CF6).copy(alpha = 0.40f)
                        } else Color.Transparent,
                    )
                    .clickable {
                        if (!isVideo && (networkQuality.tier == NetworkTier.SLOW || (networkQuality.isMetered && networkQuality.bandwidthKbps < 5000))) {
                            onRequestBandwidthCheck()
                        } else {
                            onSelectMode(true)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isVideo) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp,
                        ),
                        color = if (isVideo) Color.White else Color.White.copy(alpha = 0.5f),
                    )
                    // Ambient Network Speed Glyph
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(signalDotColor),
                    )
                    Text(
                        text = networkQuality.tier.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = if (isVideo) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}


@Composable
private fun VerticalGestureCapsuleHud(
    icon: @Composable () -> Unit,
    percent: Int,
    progress: Float,
    isLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(46.dp)
            .height(176.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(23.dp)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(bottomStart = 23.dp, bottomEnd = 23.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.90f),
                            Color(0xFF6366F1).copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${percent}%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            icon()
        }
    }
}
