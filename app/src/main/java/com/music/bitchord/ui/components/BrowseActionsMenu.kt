package com.music.bitchord.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.download.DownloadState
import com.music.bitchord.download.Downloads

/**
 * The album or playlist a long-press or 3-dots action is acting on.
 */
data class BrowseTarget(
    val browseId: String?,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val type: BrowseType = BrowseType.OTHER,
    val songs: List<Song> = emptyList(),
    val playlist: UserPlaylist? = null,
    val fromCard: Boolean = true,
    val downloadId: String? = null,
)

/**
 * Native Aura Action Sheet for albums, playlists, and artists with rich collection header,
 * touch-friendly rows, and zero emojis.
 */
@Composable
fun BrowseActionsMenu(
    target: BrowseTarget,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onDownloadAll: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    onReorder: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null,
    onRenameInSheet: (() -> Unit)? = null,
    startRenaming: Boolean = false,
    onDismiss: (() -> Unit)? = null,
) {
    var renaming by remember { mutableStateOf(startRenaming) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var confirmingDeleteDownload by remember { mutableStateOf(false) }

    val playlist = target.playlist
    if (renaming && playlist != null && onRename != null) {
        RenamePlaylistForm(
            playlist = playlist,
            onBack = { renaming = false },
            onRename = onRename,
            modifier = modifier,
        )
        return
    }

    CompositionLocalProvider(LocalActionRowStyle provides ActionRowStyle.Menu) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // Collection Header
            AuraBrowseHeader(
                target = target,
                onDismiss = { onDismiss?.invoke() },
            )

            // Primary Playback Actions
            if (onPlay != null || onShuffle != null) {
                AuraActionDivider()
                AuraSectionHeading("Playback")

                onPlay?.let {
                    ActionRow(
                        icon = Icons.Rounded.PlayArrow,
                        label = stringResource(R.string.play),
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = it,
                    )
                }
                onShuffle?.let {
                    ActionRow(
                        icon = Icons.Rounded.Shuffle,
                        label = stringResource(R.string.shuffle),
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = it,
                    )
                }
            }

            // Queue Operations
            AuraActionDivider()
            AuraSectionHeading("Queue")

            ActionRow(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                label = stringResource(R.string.play_next),
                accent = MaterialTheme.colorScheme.primary,
                onClick = onPlayNext,
            )
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = stringResource(R.string.add_to_queue),
                accent = MaterialTheme.colorScheme.primary,
                onClick = onAddToQueue,
            )

            // Download & Collection Options
            AuraActionDivider()
            AuraSectionHeading("Options")

            onDownloadAll?.let { download ->
                val active by Downloads.active.collectAsStateWithLifecycle()
                val requested by Downloads.requested.collectAsStateWithLifecycle()
                val saved by Downloads.saved.collectAsStateWithLifecycle()
                val waiting = target.browseId?.let { requested[it] }.orEmpty().any { id ->
                    when (active[id]) {
                        is DownloadState.Queued, is DownloadState.Running -> true
                        else -> false
                    }
                }
                val ids = remember(target.songs) { target.songs.mapTo(HashSet()) { it.videoId } }
                val downloaded = !waiting && ids.isNotEmpty() && ids.all { it in saved }
                ActionRow(
                    icon = when {
                        waiting -> Icons.Rounded.AccessTime
                        downloaded -> Icons.Rounded.DownloadDone
                        else -> Icons.Rounded.Download
                    },
                    label = stringResource(R.string.download_all),
                    accent = MaterialTheme.colorScheme.primary,
                    value = when {
                        waiting -> stringResource(R.string.downloading)
                        downloaded -> stringResource(R.string.downloaded)
                        else -> null
                    },
                    tint = if (downloaded) Color(0xFF00E676) else null,
                    onClick = download,
                )
            }

            onOpen?.let {
                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    label = target.type.localizedOpenLabel(),
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = it,
                )
            }

            onShare?.let {
                ActionRow(
                    icon = Icons.Rounded.Share,
                    label = stringResource(R.string.share),
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = it,
                )
            }

            onTogglePin?.let {
                ActionRow(
                    icon = Icons.Rounded.PushPin,
                    label = stringResource(if (isPinned) R.string.unpin else R.string.pin),
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = it,
                )
            }

            if (onRename != null) {
                ActionRow(
                    icon = Icons.Rounded.Edit,
                    label = stringResource(R.string.rename),
                    accent = MaterialTheme.colorScheme.primary,
                ) {
                    if (onRenameInSheet != null) {
                        onRenameInSheet()
                    } else {
                        renaming = true
                    }
                }
            }

            onReorder?.let {
                ActionRow(
                    icon = Icons.Rounded.Reorder,
                    label = stringResource(R.string.reorder_songs),
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = it,
                )
            }

            if (onDelete != null) {
                if (confirmingDelete) {
                    ActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.delete_playlist_confirmation, target.title),
                        accent = MaterialTheme.colorScheme.error,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDelete,
                    )
                } else {
                    ActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.delete_playlist),
                        accent = MaterialTheme.colorScheme.error,
                    ) {
                        confirmingDelete = true
                    }
                }
            }

            if (onDeleteDownload != null) {
                if (confirmingDeleteDownload) {
                    ActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.delete_download_confirmation, target.title),
                        accent = MaterialTheme.colorScheme.error,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDeleteDownload,
                    )
                } else {
                    ActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.delete_download),
                        accent = MaterialTheme.colorScheme.error,
                    ) {
                        confirmingDeleteDownload = true
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseType.localizedOpenLabel(): String = when (this) {
    BrowseType.ALBUM -> stringResource(R.string.open_album)
    BrowseType.ARTIST -> stringResource(R.string.open_artist)
    BrowseType.PLAYLIST -> stringResource(R.string.open_playlist)
    BrowseType.OTHER -> stringResource(R.string.open)
}
