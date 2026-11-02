package com.music.bitchord.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.webdav.WebDavUploads
import com.music.bitchord.download.DownloadState
import com.music.bitchord.download.Downloads
import com.music.bitchord.playback.SleepTimer
import com.music.bitchord.ui.player.ThinSlider
import com.music.bitchord.ui.theme.ArtworkPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Aura Glass Action Sheet for songs, redesigned for Android mobile ergonomics
 * with rich artwork header, 1-tap quick action pills, categorized sections,
 * and highlighted Aura superpowers with zero emojis.
 */
@Composable
fun SongActionsMenu(
    song: Song,
    signedIn: Boolean,
    likeStatus: LikeStatus,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onStartRadio: () -> Unit,
    onDownload: () -> Unit,
    onUploadToWebDav: (() -> Unit)? = null,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromCache: (() -> Unit)? = null,
    onRemoveFromHistory: (() -> Unit)? = null,
    showSleepTimer: Boolean = false,
    onRollbackToOriginal: (() -> Unit)? = null,
    onUpgradeQuality: (() -> Unit)? = null,
    upgradeQualityInProgress: Boolean = false,
    onToggleAudioVersion: (() -> Unit)? = null,
    isAudioVersion: Boolean = false,
    audioVersionMissing: Boolean = false,
    smartVersionPending: Boolean = false,
    onShowSongInfo: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onCopyLog: (() -> Unit)? = null,
    onLyricsOffset: (() -> Unit)? = null,
    palette: ArtworkPalette? = null,
    resolvingLinks: Boolean = false,
    onAskAuraAboutSong: (() -> Unit)? = null,
    onOpenKaraoke: (() -> Unit)? = null,
    onOpenNeuralDj: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val palette = palette ?: neutralMenuPalette()
    val liked = likeStatus == LikeStatus.LIKE
    val disliked = likeStatus == LikeStatus.DISLIKE
    val isOffline = song.localUri != null

    val activeDownloads by Downloads.active.collectAsStateWithLifecycle()
    val downloaded = isOffline || (song.videoId in Downloads.saved.collectAsStateWithLifecycle().value)

    var pickingSleepTimer by remember { mutableStateOf(false) }
    var showingSongInfo by remember(song.videoId) { mutableStateOf(false) }
    if (showingSongInfo) {
        SongInfoDialog(song) { showingSongInfo = false }
    }

    val rows: @Composable ColumnScope.() -> Unit = {
        // Track Header (Album Art, Title, Artist)
        AuraTrackHeader(
            song = song,
            onDismiss = { onDismiss?.invoke() },
        )

        // Quick Actions Pill Bar (Like, Dislike, Download, Radio, Share)
        val canRate = isOffline || signedIn
        AuraQuickActionsBar(
            liked = liked,
            disliked = disliked,
            downloaded = downloaded,
            onToggleLike = onToggleLike,
            onToggleDislike = if (signedIn && !isOffline) onToggleDislike else null,
            onDownload = if (!isOffline) onDownload else null,
            onStartRadio = if (!isOffline) onStartRadio else null,
            onShare = if (!isOffline) onShare else null,
            palette = palette,
        )

        // Aura Superpowers Showcase (When unlocked)
        val auraUnlocked by AppSettings.auraAiUnlocked.collectAsStateWithLifecycle()
        if (auraUnlocked && (onAskAuraAboutSong != null || onOpenNeuralDj != null || onOpenKaraoke != null)) {
            AuraActionDivider()
            AuraSectionHeading("Aura Superpowers")

            onAskAuraAboutSong?.let {
                AuraSuperpowerCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Aura AI Companion",
                    subtitle = "Acoustic psychology, roasts and recommendations",
                    badgeText = "AI COMPANION",
                    accentColor = Color(0xFF00F5D4),
                    onClick = it,
                )
            }

            onOpenNeuralDj?.let {
                AuraSuperpowerCard(
                    icon = Icons.Rounded.DiscFull,
                    title = "Aura Neural DJ",
                    subtitle = "Harmonic Camelot Wheel transitions and BPM matching",
                    badgeText = "HARMONIC MIX",
                    accentColor = Color(0xFF7B2CBF),
                    onClick = it,
                )
            }

            onOpenKaraoke?.let {
                AuraSuperpowerCard(
                    icon = Icons.Rounded.Mic,
                    title = "Vocal Stem Karaoke",
                    subtitle = "Real-time AI vocal separation and instrumentals",
                    badgeText = "STEM SEPARATOR",
                    accentColor = Color(0xFFFF007A),
                    onClick = it,
                )
            }
        }

        // Playback & Versions
        if (onRollbackToOriginal != null || onUpgradeQuality != null || onToggleAudioVersion != null) {
            AuraActionDivider()
            AuraSectionHeading("Playback & Quality")

            (onRollbackToOriginal ?: onUpgradeQuality)?.let { action ->
                ActionRow(
                    icon = if (onRollbackToOriginal != null) Icons.Rounded.Undo else Icons.Rounded.HighQuality,
                    label = stringResource(
                        if (onRollbackToOriginal != null) R.string.revert_to_original else R.string.upgrade_quality
                    ),
                    accent = palette.accent,
                    enabled = onRollbackToOriginal != null || !upgradeQualityInProgress,
                    onClick = action,
                )
            }

            onToggleAudioVersion?.let { toggle ->
                ActionRow(
                    icon = if (isAudioVersion) Icons.Rounded.Videocam else Icons.Rounded.GraphicEq,
                    label = stringResource(
                        when {
                            audioVersionMissing && isAudioVersion -> R.string.no_video_version
                            audioVersionMissing -> R.string.no_audio_version
                            isAudioVersion -> R.string.convert_to_video
                            else -> R.string.convert_to_audio
                        }
                    ),
                    description = when {
                        smartVersionPending && !audioVersionMissing -> stringResource(R.string.smart_version_alignment)
                        audioVersionMissing -> null
                        isAudioVersion -> "Switch to official music video"
                        else -> "Switch to official audio stream"
                    },
                    accent = palette.accent,
                    enabled = !audioVersionMissing,
                    onClick = toggle,
                )
            }
        }

        // Queue Operations
        AuraActionDivider()
        AuraSectionHeading("Queue")

        ActionRow(
            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
            label = stringResource(R.string.play_next),
            accent = palette.accent,
            onClick = onPlayNext,
        )
        ActionRow(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            label = stringResource(R.string.add_to_queue),
            accent = palette.accent,
            onClick = onAddToQueue,
        )

        // Library & Navigation
        AuraActionDivider()
        AuraSectionHeading("Library & Collection")

        ActionRow(
            icon = Icons.Rounded.BookmarkAdd,
            label = stringResource(R.string.add_to_playlist),
            accent = palette.accent,
            onClick = onAddToPlaylist,
        )

        onRemoveFromPlaylist?.let {
            ActionRow(
                icon = Icons.Rounded.PlaylistRemove,
                label = stringResource(R.string.remove_from_playlist),
                accent = MaterialTheme.colorScheme.error,
                onClick = it,
            )
        }

        when (val id = song.albumId) {
            null -> if (resolvingLinks) {
                LoadingActionRow(Icons.Rounded.Album, stringResource(R.string.open_album), palette)
            }
            else -> ActionRow(
                icon = Icons.Rounded.Album,
                label = stringResource(R.string.open_album),
                accent = palette.accent,
                description = song.albumName,
                onClick = { onOpenAlbum(id) },
            )
        }

        when (val id = song.artistId) {
            null -> if (resolvingLinks) {
                LoadingActionRow(Icons.Rounded.Person, stringResource(R.string.open_artist), palette)
            }
            else -> ActionRow(
                icon = Icons.Rounded.Person,
                label = stringResource(R.string.open_artist),
                accent = palette.accent,
                description = song.artist.takeIf { it.isNotBlank() },
                onClick = { onOpenArtist(id) },
            )
        }

        // Utilities & Details
        AuraActionDivider()
        AuraSectionHeading("Utilities & Info")

        if (showSleepTimer) {
            SleepTimerActionRow(
                palette = palette,
                onDirectClick = null,
                onOpenPicker = { pickingSleepTimer = true },
            )
        }

        ActionRow(
            icon = Icons.Rounded.Info,
            label = stringResource(R.string.song_info),
            accent = palette.accent,
            onClick = { showingSongInfo = true },
        )

        onLyricsOffset?.let {
            ActionRow(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.lyrics_offset),
                accent = palette.accent,
                onClick = it,
            )
        }

        onCopyLog?.let {
            ActionRow(
                icon = Icons.Rounded.ContentCopy,
                label = stringResource(R.string.copy_log),
                accent = palette.accent,
                onClick = it,
            )
        }

        onRemoveFromCache?.let {
            ActionRow(
                icon = Icons.Rounded.DeleteOutline,
                label = stringResource(R.string.remove_from_cache),
                accent = palette.accent,
                onClick = it,
            )
        }

        WebDavUploadRow(song, palette, isOffline, onUploadToWebDav)

        onRemoveFromHistory?.let {
            ActionRow(
                icon = Icons.Rounded.History,
                label = stringResource(R.string.remove_from_history),
                accent = palette.accent,
                onClick = it,
            )
        }

        ActionRow(
            icon = Icons.Rounded.BugReport,
            label = stringResource(R.string.copy_log),
            accent = palette.accent,
            onClick = {
                val clip = android.content.ClipData.newPlainText("Song Debug Log", song.toString())
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                cm?.setPrimaryClip(clip)
            },
        )
    }

    CompositionLocalProvider(LocalActionRowStyle provides ActionRowStyle.Menu) {
        Column(modifier = modifier.fillMaxWidth()) {
            if (pickingSleepTimer) {
                SleepTimerPicker(palette = palette, onBack = { pickingSleepTimer = false })
            } else {
                rows()
            }
        }
    }
}

/**
 * Row density and layout style.
 */
internal enum class ActionRowStyle { Sheet, Menu }

internal val LocalActionRowStyle = staticCompositionLocalOf { ActionRowStyle.Sheet }

@Composable
private fun neutralMenuPalette(): ArtworkPalette {
    val accent = MaterialTheme.colorScheme.primary
    val colors = iosMenuColors()
    return remember(accent, colors) {
        ArtworkPalette(
            background = colors.fill,
            wash = colors.fill,
            elevated = colors.fill,
            accent = accent,
            onBackground = colors.label,
            onBackgroundVariant = colors.secondaryLabel,
            divider = colors.separator,
        )
    }
}

@Composable
private fun DownloadRow(
    song: Song,
    palette: ArtworkPalette,
    isOffline: Boolean,
    onDownload: () -> Unit,
) {
    val context = LocalContext.current
    val active by Downloads.active.collectAsStateWithLifecycle()
    val saved by Downloads.saved.collectAsStateWithLifecycle()
    val recorded = saved[song.videoId]
    val file by produceState(recorded?.let(Uri::parse), song.videoId, recorded) {
        value = Downloads.savedUri(context, song.videoId)
    }
    val scope = rememberCoroutineScope()

    DisposableEffect(song.videoId) {
        onDispose { Downloads.dismissFailure(song.videoId) }
    }

    when (val state = active[song.videoId]) {
        is DownloadState.Queued -> ActionRow(
            icon = Icons.Rounded.Download,
            label = stringResource(R.string.queued),
            value = stringResource(R.string.cancel),
            accent = palette.accent,
            onClick = { Downloads.cancel(song.videoId) },
        )
        is DownloadState.Running -> ActionRow(
            icon = Icons.Rounded.Download,
            label = stringResource(R.string.download_notification_title),
            value = if (state.fraction > 0f) "${(state.fraction * 100).toInt()}%" else null,
            tint = palette.accent,
            accent = palette.accent,
            onClick = { Downloads.cancel(song.videoId) },
        )
        is DownloadState.Failed -> ActionRow(
            icon = Icons.Rounded.ErrorOutline,
            label = state.reason,
            value = stringResource(R.string.retry),
            tint = MaterialTheme.colorScheme.error,
            accent = MaterialTheme.colorScheme.error,
            onClick = onDownload,
        )
        null -> if (file != null) {
            ActionRow(
                icon = Icons.Rounded.DownloadDone,
                label = stringResource(R.string.saved_to_downloads),
                value = stringResource(R.string.delete),
                tint = Color(0xFF00E676),
                accent = palette.accent,
                onClick = { scope.launch { Downloads.delete(context, song.videoId) } },
            )
        } else if (!isOffline) {
            ActionRow(
                icon = Icons.Rounded.Download,
                label = stringResource(R.string.download),
                accent = palette.accent,
                onClick = onDownload,
            )
        }
    }
}

@Composable
private fun WebDavUploadRow(
    song: Song,
    palette: ArtworkPalette,
    isOffline: Boolean,
    onUploadToWebDav: (() -> Unit)?,
) {
    if (onUploadToWebDav == null) return
    val active by WebDavUploads.active.collectAsStateWithLifecycle()
    when (val state = active[song.videoId]) {
        is WebDavUploads.TrackState.Running -> ActionRow(
            icon = Icons.Rounded.CloudUpload,
            label = stringResource(R.string.upload_to_webdav),
            value = if (state.fraction > 0f) "${(state.fraction * 100).toInt()}%" else null,
            tint = palette.accent,
            accent = palette.accent,
            enabled = false,
            onClick = {},
        )
        is WebDavUploads.TrackState.Failed -> ActionRow(
            icon = Icons.Rounded.ErrorOutline,
            label = state.reason ?: stringResource(R.string.upload_to_webdav),
            value = stringResource(R.string.try_again),
            tint = MaterialTheme.colorScheme.error,
            accent = MaterialTheme.colorScheme.error,
            onClick = onUploadToWebDav,
        )
        else -> ActionRow(
            icon = Icons.Rounded.CloudUpload,
            label = stringResource(R.string.upload_to_webdav),
            accent = palette.accent,
            onClick = onUploadToWebDav,
        )
    }
}

@Composable
private fun SleepTimerActionRow(
    palette: ArtworkPalette,
    onDirectClick: (() -> Unit)?,
    onOpenPicker: () -> Unit,
) {
    val countdown = sleepTimerCountdown()
    val afterTrack by SleepTimer.afterTrack.collectAsStateWithLifecycle()
    val active = countdown != null || afterTrack

    if (active) {
        ActionRow(
            icon = Icons.Rounded.Close,
            label = stringResource(R.string.turn_off_timer),
            accent = palette.accent,
            description = when {
                countdown != null -> stringResource(R.string.pause_countdown, countdown)
                else -> stringResource(R.string.pause_after_song)
            },
            onClick = { SleepTimer.cancel() },
        )
    } else {
        ActionRow(
            icon = Icons.Rounded.Bedtime,
            label = stringResource(R.string.start_sleep_timer),
            accent = palette.accent,
            onClick = onDirectClick ?: onOpenPicker,
        )
    }
}

@Composable
private fun SleepTimerPicker(palette: ArtworkPalette, onBack: () -> Unit) {
    val chosen by SleepTimer.minutes.collectAsStateWithLifecycle()
    val afterTrack by SleepTimer.afterTrack.collectAsStateWithLifecycle()
    val countdown = sleepTimerCountdown()
    val custom = chosen?.takeIf { it !in SleepTimer.PRESETS }
    var pickingCustom by remember { mutableStateOf(false) }

    if (pickingCustom) {
        CustomSleepTimerPage(
            palette = palette,
            initialMinutes = custom ?: 90,
            onBack = { pickingCustom = false },
            onStart = { minutes ->
                SleepTimer.start(minutes)
                onBack()
            },
        )
        return
    }

    Column(Modifier.fillMaxWidth()) {
        SleepTimerHeader(
            title = stringResource(R.string.sleep_timer),
            subtitle = when {
                countdown != null -> stringResource(R.string.pause_countdown, countdown)
                afterTrack -> stringResource(R.string.pause_after_song)
                else -> stringResource(R.string.pause_after_a_while)
            },
            onBack = onBack,
        )

        SleepOption(
            title = stringResource(R.string.turn_off_timer),
            palette = palette,
            selected = chosen == null && !afterTrack,
            onClick = { SleepTimer.cancel(); onBack() },
        )

        SleepOption(
            title = stringResource(R.string.pause_after_song),
            palette = palette,
            selected = afterTrack,
            onClick = { SleepTimer.startAfterTrack(); onBack() },
        )

        SleepTimer.PRESETS.forEach { minutes ->
            SleepOption(
                title = if (minutes == 60) {
                    stringResource(R.string.one_hour)
                } else {
                    pluralStringResource(R.plurals.minute_count, minutes, minutes)
                },
                palette = palette,
                selected = chosen == minutes && !afterTrack,
                onClick = { SleepTimer.start(minutes); onBack() },
            )
        }

        SleepOption(
            title = custom?.let {
                stringResource(R.string.sleep_timer_custom) + " · " + sleepDuration(it)
            } ?: stringResource(R.string.sleep_timer_custom),
            palette = palette,
            selected = custom != null,
            onClick = { pickingCustom = true },
        )
    }
}

@Composable
private fun SleepTimerHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun CustomSleepTimerPage(
    palette: ArtworkPalette,
    initialMinutes: Int,
    onBack: () -> Unit,
    onStart: (Int) -> Unit,
) {
    var minutes by remember { mutableStateOf(initialMinutes) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        SleepTimerHeader(
            title = stringResource(R.string.sleep_timer_custom),
            subtitle = sleepDuration(minutes),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StepButton(label = "-15m", onClick = { minutes = (minutes - 15).coerceAtLeast(SleepTimer.STEP_MINUTES) }, modifier = Modifier.weight(1f))
                StepButton(label = "-5m", onClick = { minutes = (minutes - 5).coerceAtLeast(SleepTimer.STEP_MINUTES) }, modifier = Modifier.weight(1f))
                StepButton(label = "+5m", onClick = { minutes = (minutes + 5).coerceAtMost(SleepTimer.MAX_MINUTES) }, modifier = Modifier.weight(1f))
                StepButton(label = "+15m", onClick = { minutes = (minutes + 15).coerceAtMost(SleepTimer.MAX_MINUTES) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            ThinSlider(
                value = SleepTimer.fractionOf(minutes),
                onValueChange = { minutes = SleepTimer.minutesAt(it) },
                idleHeight = 6.dp,
                activeHeight = 10.dp,
                activeColor = palette.accent,
                inactiveColor = Color.White.copy(alpha = 0.26f),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp).continuous(),
                color = palette.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp).continuous())
                    .clickable { onStart(minutes) }
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    text = "Start Timer",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun StepButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp).continuous())
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

private fun sleepDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

@Composable
private fun SleepOption(
    title: String,
    palette: ArtworkPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ActionRow(
        icon = if (selected) Icons.Rounded.Check else null,
        label = title,
        accent = palette.accent,
        tint = if (selected) palette.accent else Color.White.copy(alpha = 0.85f),
        onClick = onClick,
    )
}

@Composable
private fun sleepTimerCountdown(): String? {
    val remainingMs by produceState<Long?>(initialValue = SleepTimer.remainingMs()) {
        while (true) {
            value = SleepTimer.remainingMs()
            delay(1000L)
        }
    }
    return remainingMs?.let { ms ->
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

@Composable
internal fun ActionRow(
    icon: ImageVector?,
    label: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    description: String? = null,
    value: String? = null,
    tint: Color? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    AuraActionRow(
        icon = icon,
        label = label,
        description = description,
        iconTint = tint ?: accent,
        enabled = enabled,
        trailing = trailing ?: value?.let { v ->
            {
                Surface(
                    shape = RoundedCornerShape(8.dp).continuous(),
                    color = Color.White.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = v,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun LoadingActionRow(icon: ImageVector, label: String, palette: ArtworkPalette) {
    AuraActionRow(
        icon = icon,
        label = label,
        enabled = false,
        trailing = {
            CircularProgressIndicator(
                color = palette.accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
        },
        onClick = null,
    )
}

@Composable
internal fun SheetTrackHeader(
    song: Song,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    AuraTrackHeader(song = song, onDismiss = {}, modifier = modifier)
}

@Composable
internal fun SheetHeading(text: String) {
    AuraSectionHeading(title = text)
}
