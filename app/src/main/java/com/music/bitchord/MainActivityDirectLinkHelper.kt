package com.music.bitchord

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.link.YouTubeLinkParser
import com.music.bitchord.data.link.YouTubeResumeStore
import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.EntityType
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.SearchHistoryEntity
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.EqualizerMode
import com.music.bitchord.playback.EqualizerPreset
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.ui.MainViewModel

internal class DirectLinkUiBridge(
    val voiceClarityEnabled: Boolean,
    val onToggleVoiceClarity: () -> Unit,
    val onPlayTrack: (Song, Long) -> Unit,
    val onPlayNext: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onDownloadTrack: (Song) -> Unit,
    val onOpenBrowse: (BrowseItem) -> Unit,
    val onPlayCollection: (List<Song>, BrowseItem) -> Unit,
    val onOpenExternal: (String) -> Unit,
    val onDownloadAll: (List<Song>) -> Unit,
    val onPasteFromClipboard: () -> Unit,
)

@Composable
internal fun rememberDirectLinkBridge(
    context: Context,
    viewModel: MainViewModel,
    sharedLinkLabel: String,
    currentSong: Song?,
    positionMs: Long,
    durationMs: Long,
    readClipboardText: () -> String,
    playRadioAt: (Song, QueueSource, Long) -> Unit,
    playFrom: (List<Song>, Int, QueueSource) -> Unit,
    playNext: (Song) -> Unit,
    addToQueue: (Song) -> Unit,
    downloadSong: (Song) -> Unit,
    downloadAllSongs: (List<Song>) -> Unit,
): DirectLinkUiBridge {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val eqEnabled by AppSettings.equalizerEnabled.collectAsStateWithLifecycle()
    val eqMode by AppSettings.equalizerMode.collectAsStateWithLifecycle()
    val eqPreset by AppSettings.equalizerPreset.collectAsStateWithLifecycle()
    val eqBands by AppSettings.equalizerBands.collectAsStateWithLifecycle()
    val voiceClarityEnabled = eqEnabled &&
        eqMode == EqualizerMode.MANUAL &&
        eqPreset == EqualizerPreset.SPOKEN_WORD
    var savedEqBeforeVoiceClarity by remember {
        mutableStateOf<Triple<Boolean, EqualizerMode, List<Float>>?>(null)
    }
    val toggleVoiceClarity: () -> Unit = {
        if (voiceClarityEnabled) {
            val prev = savedEqBeforeVoiceClarity
            if (prev != null) {
                AppSettings.setEqualizerMode(prev.second)
                AppSettings.setEqualizerBands(prev.third)
                AppSettings.setEqualizerEnabled(prev.first)
                savedEqBeforeVoiceClarity = null
            } else {
                AppSettings.setEqualizerEnabled(false)
            }
        } else {
            savedEqBeforeVoiceClarity = Triple(eqEnabled, eqMode, eqBands)
            AppSettings.setEqualizerMode(EqualizerMode.MANUAL)
            AppSettings.setEqualizerPreset(EqualizerPreset.SPOKEN_WORD)
            AppSettings.setEqualizerEnabled(true)
        }
    }

    val progressBucket = positionMs / 5_000L
    LaunchedEffect(currentSong?.videoId, progressBucket) {
        val song = currentSong ?: return@LaunchedEffect
        YouTubeResumeStore.recordSongProgress(
            song = song,
            positionMs = positionMs,
            playerDurationMs = durationMs,
        )
    }

    val recordTrackHistory: (Song) -> Unit = { song ->
        viewModel.recordEntity(
            SearchHistoryEntity(
                id = song.videoId,
                title = song.title,
                subtitle = song.artist.ifEmpty { "" },
                artworkUrl = song.thumbnailUrl,
                entityType = EntityType.TRACK,
            ),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.directLinkAutoPlay.collect { req ->
            val track = req.track
            if (track != null) {
                recordTrackHistory(track)
                playRadioAt(
                    track,
                    QueueSource(sharedLinkLabel, PlaybackSourceType.SHARED_LINK, track.videoId),
                    req.startPositionMs,
                )
            } else if (req.collectionSongs.isNotEmpty()) {
                playFrom(
                    req.collectionSongs,
                    0,
                    QueueSource(
                        req.collectionTitle ?: sharedLinkLabel,
                        PlaybackSourceType.BROWSE,
                        req.collectionBrowseId,
                    ),
                )
            }
        }
    }

    return DirectLinkUiBridge(
        voiceClarityEnabled = voiceClarityEnabled,
        onToggleVoiceClarity = toggleVoiceClarity,
        onPlayTrack = { song, startMs ->
            recordTrackHistory(song)
            playRadioAt(
                song,
                QueueSource(sharedLinkLabel, PlaybackSourceType.SHARED_LINK, song.videoId),
                startMs,
            )
        },
        onPlayNext = { song ->
            recordTrackHistory(song)
            playNext(song)
        },
        onAddToQueue = { song ->
            recordTrackHistory(song)
            addToQueue(song)
        },
        onDownloadTrack = { song ->
            recordTrackHistory(song)
            downloadSong(song)
        },
        onOpenBrowse = { item ->
            viewModel.openDetail(
                browseId = item.browseId,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                type = item.type,
            )
        },
        onPlayCollection = { songs, item ->
            playFrom(
                songs,
                0,
                QueueSource(item.title, PlaybackSourceType.BROWSE, item.browseId),
            )
        },
        onOpenExternal = { url ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        },
        onDownloadAll = downloadAllSongs,
        onPasteFromClipboard = {
            val pasted = readClipboardText().trim()
            val parsed = YouTubeLinkParser.parse(pasted)
            if (parsed != null) {
                focusManager.clearFocus()
                keyboardController?.hide()
                viewModel.onQueryChange(parsed.rawUrl)
            } else {
                Toast.makeText(
                    context,
                    if (pasted.isEmpty()) "Clipboard is empty" else "Copy a YouTube link to paste here",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )
}
