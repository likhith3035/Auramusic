package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.MonotonicClock
import com.music.bitchord.playback.SleepTimerMode
import com.music.bitchord.playback.SleepTimerStatus
import com.music.bitchord.playback.SmartSleepTimer
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class SleepTimerTab {
    DURATION,
    SONG_COUNT,
    INACTIVITY,
}

@Composable
fun SmartSleepTimerSheet(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val status by SmartSleepTimer.status.collectAsStateWithLifecycle()
    val mode by SmartSleepTimer.mode.collectAsStateWithLifecycle()

    var selectedTab by remember(mode) {
        mutableStateOf(
            if (mode is SleepTimerMode.SongCount) SleepTimerTab.SONG_COUNT else SleepTimerTab.DURATION
        )
    }

    PlayerDrawer(
        hazeState = hazeState,
        onDismiss = onDismiss,
        title = "Smart Sleep Timer",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Segmented Tab Selector
            SleepTimerTabsRow(
                selectedTab = selectedTab,
                onTabSelect = {
                    haptics.play(Haptic.Select)
                    selectedTab = it
                },
            )

            Spacer(Modifier.height(18.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SleepTimerTabContent",
            ) { tab ->
                when (tab) {
                    SleepTimerTab.DURATION -> {
                        DurationTimerPane(
                            status = status,
                            mode = mode,
                            onCloseSheet = onDismiss,
                        )
                    }
                    SleepTimerTab.SONG_COUNT -> {
                        SongCountTimerPane(
                            status = status,
                            mode = mode,
                            onCloseSheet = onDismiss,
                        )
                    }
                    SleepTimerTab.INACTIVITY -> {
                        InactivityDetectionPane()
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerTabsRow(
    selectedTab: SleepTimerTab,
    onTabSelect: (SleepTimerTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabItem(
            label = "Duration",
            icon = Icons.Rounded.Timer,
            selected = selectedTab == SleepTimerTab.DURATION,
            onClick = { onTabSelect(SleepTimerTab.DURATION) },
            modifier = Modifier.weight(1f),
        )
        TabItem(
            label = "Songs",
            icon = Icons.Rounded.MusicNote,
            selected = selectedTab == SleepTimerTab.SONG_COUNT,
            onClick = { onTabSelect(SleepTimerTab.SONG_COUNT) },
            modifier = Modifier.weight(1f),
        )
        TabItem(
            label = "Detection",
            icon = Icons.Rounded.Bedtime,
            selected = selectedTab == SleepTimerTab.INACTIVITY,
            onClick = { onTabSelect(SleepTimerTab.INACTIVITY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent
    val contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.6f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = contentColor,
        )
    }
}

// -------------------------------------------------------------------------
// Tab 1: Time-Based Duration Pane
// -------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationTimerPane(
    status: SleepTimerStatus,
    mode: SleepTimerMode,
    onCloseSheet: () -> Unit,
) {
    val haptics = rememberHaptics()
    val isDurationActive = mode is SleepTimerMode.Duration && status != SleepTimerStatus.OFF

    val remainingMs by produceState(SmartSleepTimer.remainingMs(), status) {
        while (isActive) {
            value = SmartSleepTimer.remainingMs()
            delay(500L)
        }
    }

    var customHours by remember { mutableIntStateOf(0) }
    var customMinutes by remember { mutableIntStateOf(30) }
    val finishCurrentSong by SmartSleepTimer.finishCurrentSongOnExpiry.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isDurationActive) {
            // Live Countdown Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val statusText = when (status) {
                        SleepTimerStatus.RUNNING -> "Active Countdown"
                        SleepTimerStatus.PAUSED -> "Timer Paused"
                        SleepTimerStatus.FINISHING_CURRENT_SONG -> "Finishing current song..."
                        SleepTimerStatus.OFF -> "Timer"
                    }
                    val statusDotColor = when (status) {
                        SleepTimerStatus.RUNNING -> Color(0xFF4CAF50)
                        SleepTimerStatus.PAUSED -> Color(0xFFFFB300)
                        SleepTimerStatus.FINISHING_CURRENT_SONG -> Color(0xFFBA68C8)
                        SleepTimerStatus.OFF -> Color.White.copy(alpha = 0.5f)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusDotColor),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val timeText = if (remainingMs != null) {
                        SmartSleepTimer.formatRemaining(remainingMs!!)
                    } else if (status == SleepTimerStatus.FINISHING_CURRENT_SONG) {
                        "00:00"
                    } else {
                        "--:--"
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = Color.White,
                    )

                    Spacer(Modifier.height(14.dp))

                    // Controls Row: Pause/Resume, +5m, +15m, Cancel
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (status == SleepTimerStatus.RUNNING) {
                            ActionButton(
                                icon = Icons.Rounded.Pause,
                                label = "Pause",
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    SmartSleepTimer.pause()
                                },
                            )
                        } else if (status == SleepTimerStatus.PAUSED) {
                            ActionButton(
                                icon = Icons.Rounded.PlayArrow,
                                label = "Resume",
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    SmartSleepTimer.resume()
                                },
                            )
                        }

                        ActionButton(
                            icon = Icons.Rounded.Add,
                            label = "+5m",
                            onClick = {
                                haptics.play(Haptic.Tap)
                                SmartSleepTimer.addMinutes(5)
                            },
                        )

                        ActionButton(
                            icon = Icons.Rounded.Add,
                            label = "+15m",
                            onClick = {
                                haptics.play(Haptic.Tap)
                                SmartSleepTimer.addMinutes(15)
                            },
                        )

                        ActionButton(
                            icon = Icons.Rounded.Close,
                            label = "Cancel",
                            onClick = {
                                haptics.play(Haptic.ToggleOff)
                                SmartSleepTimer.cancel()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // Quick Presets
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
        )

        val activeMinutes = (mode as? SleepTimerMode.Duration)?.totalMinutes
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmartSleepTimer.TIME_PRESETS.forEach { mins ->
                val isSelected = isDurationActive && activeMinutes == mins
                val label = when {
                    mins < 60 -> "${mins}m"
                    mins == 60 -> "1 hour"
                    mins == 90 -> "1h 30m"
                    mins == 120 -> "2 hours"
                    else -> "${mins}m"
                }
                PresetChip(
                    label = label,
                    selected = isSelected,
                    onClick = {
                        haptics.play(Haptic.Select)
                        SmartSleepTimer.startDuration(mins)
                    },
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // Custom Duration Input
        Text(
            text = "Custom Duration",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Hours Stepper
            StepperControl(
                label = "Hours",
                value = customHours,
                onValueChange = { customHours = it.coerceIn(0, 23) },
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Color.White.copy(alpha = 0.1f)),
            )

            // Minutes Stepper
            StepperControl(
                label = "Minutes",
                value = customMinutes,
                onValueChange = { customMinutes = it.coerceIn(0, 59) },
            )
        }

        Spacer(Modifier.height(14.dp))

        val totalCustomMins = customHours * 60 + customMinutes
        Button(
            onClick = {
                if (totalCustomMins > 0) {
                    haptics.play(Haptic.ToggleOn)
                    SmartSleepTimer.startDuration(totalCustomMins)
                }
            },
            enabled = totalCustomMins > 0,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = if (totalCustomMins > 0) "Set Timer for $customHours h $customMinutes m" else "Choose Duration",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }

        Spacer(Modifier.height(18.dp))

        // "Finish Current Song" Feature Setting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable {
                    SmartSleepTimer.finishCurrentSongOnExpiry.value = !finishCurrentSong
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Finish current song completely",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = "Playback will smoothly end after the track rather than cutting off at zero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = finishCurrentSong,
                onCheckedChange = { SmartSleepTimer.finishCurrentSongOnExpiry.value = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(alpha = 0.35f),
                ),
            )
        }
    }
}

// -------------------------------------------------------------------------
// Tab 2: Track-Based Song Count Pane
// -------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SongCountTimerPane(
    status: SleepTimerStatus,
    mode: SleepTimerMode,
    onCloseSheet: () -> Unit,
) {
    val haptics = rememberHaptics()
    val isCountActive = mode is SleepTimerMode.SongCount && status != SleepTimerStatus.OFF

    val completed by SmartSleepTimer.completedSongs.collectAsStateWithLifecycle()
    val remaining by SmartSleepTimer.remainingSongs.collectAsStateWithLifecycle()
    val targetTotal = (mode as? SleepTimerMode.SongCount)?.totalSongs ?: 1

    var customTracks by remember { mutableIntStateOf(5) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isCountActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val statusText = when (status) {
                        SleepTimerStatus.RUNNING -> "Counting Tracks"
                        SleepTimerStatus.PAUSED -> "Counter Paused"
                        SleepTimerStatus.FINISHING_CURRENT_SONG -> "Final song playing..."
                        SleepTimerStatus.OFF -> "Track Timer"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Song ${completed + 1} of $targetTotal",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )

                    Text(
                        text = if (remaining <= 1) "Stopping after current song" else "$remaining songs remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(Modifier.height(12.dp))

                    val progress = if (targetTotal > 0) (completed.toFloat() / targetTotal).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.15f),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (status == SleepTimerStatus.RUNNING) {
                            ActionButton(
                                icon = Icons.Rounded.Pause,
                                label = "Pause",
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    SmartSleepTimer.pause()
                                },
                            )
                        } else if (status == SleepTimerStatus.PAUSED) {
                            ActionButton(
                                icon = Icons.Rounded.PlayArrow,
                                label = "Resume",
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    SmartSleepTimer.resume()
                                },
                            )
                        }

                        ActionButton(
                            icon = Icons.Rounded.Close,
                            label = "Cancel",
                            onClick = {
                                haptics.play(Haptic.ToggleOff)
                                SmartSleepTimer.cancel()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // Presets: 1 song (Stop after this song), 3, 5, 10, 15
        Text(
            text = "Track Presets",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmartSleepTimer.SONG_PRESETS.forEach { count ->
                val isSelected = isCountActive && targetTotal == count
                val label = if (count == 1) "After this song" else "$count songs"
                PresetChip(
                    label = label,
                    selected = isSelected,
                    onClick = {
                        haptics.play(Haptic.Select)
                        SmartSleepTimer.startSongCount(count)
                    },
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // Custom Song Count Stepper
        Text(
            text = "Custom Track Count",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperControl(
                label = "Total Tracks",
                value = customTracks,
                onValueChange = { customTracks = it.coerceIn(1, 99) },
            )
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = {
                if (customTracks > 0) {
                    haptics.play(Haptic.ToggleOn)
                    SmartSleepTimer.startSongCount(customTracks)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = "Stop After $customTracks Songs",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

// -------------------------------------------------------------------------
// Tab 3: Optional Inactivity Detection Pane
// -------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InactivityDetectionPane() {
    val haptics = rememberHaptics()
    val enabled by SmartSleepTimer.inactivityDetectionEnabled.collectAsStateWithLifecycle()
    val thresholdMins by SmartSleepTimer.inactivityThresholdMinutes.collectAsStateWithLifecycle()
    val lastActive by SmartSleepTimer.lastActivityMs.collectAsStateWithLifecycle()

    var customThreshold by remember(thresholdMins) { mutableIntStateOf(thresholdMins) }

    val lastActiveMinutesAgo by produceState(0L, lastActive) {
        while (isActive) {
            val diffMs = (MonotonicClock.nowMs() - lastActive).coerceAtLeast(0L)
            value = diffMs / 60_000L
            delay(15_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Feature Switch Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .clickable {
                    haptics.play(Haptic.ToggleOn)
                    SmartSleepTimer.setInactivityDetectionEnabled(!enabled)
                }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (enabled) Color(0xFFBA68C8).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFFCE93D8) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sleep Inactivity Detection",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    text = if (enabled) "Active • $thresholdMins min threshold" else "Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) Color(0xFFCE93D8) else Color.White.copy(alpha = 0.5f),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    haptics.play(Haptic.ToggleOn)
                    SmartSleepTimer.setInactivityDetectionEnabled(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFAB47BC),
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Explanation Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "AuraMusic monitors your player interactions (play/pause, skips, seeks, volume). If you fall asleep and don't touch the player for the threshold duration, playback will smoothly finish the current song and stop.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Threshold Presets
        Text(
            text = "Inactivity Threshold",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmartSleepTimer.INACTIVITY_PRESETS.forEach { mins ->
                val isSelected = thresholdMins == mins
                PresetChip(
                    label = "$mins min",
                    selected = isSelected,
                    onClick = {
                        haptics.play(Haptic.Select)
                        SmartSleepTimer.setInactivityThresholdMinutes(mins)
                    },
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Custom Inactivity Minutes Stepper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperControl(
                label = "Threshold Minutes",
                value = customThreshold,
                onValueChange = {
                    customThreshold = it.coerceIn(5, 180)
                    SmartSleepTimer.setInactivityThresholdMinutes(customThreshold)
                },
            )
        }

        if (enabled) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Last user activity: ${if (lastActiveMinutesAgo == 0L) "Just now" else "$lastActiveMinutesAgo mins ago"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Reusable UI Elements
// -------------------------------------------------------------------------

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) Color.White else Color.White.copy(alpha = 0.08f)
    val textColor = if (selected) Color.Black else Color.White

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = textColor,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

@Composable
private fun StepperControl(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val haptics = rememberHaptics()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    haptics.play(Haptic.Tap)
                    onValueChange(value - 1)
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = {
                    haptics.play(Haptic.Tap)
                    onValueChange(value + 1)
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
