package com.music.bitchord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.link.ParsedYouTubeLink
import com.music.bitchord.data.link.ResolvedYouTubeLink
import com.music.bitchord.data.link.VideoChapter
import com.music.bitchord.data.link.YouTubeLinkKind
import com.music.bitchord.data.link.YouTubeLinkParser
import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.model.isSameTrackAs
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.theme.NothingCard
import com.music.bitchord.ui.theme.NothingPill
import com.music.bitchord.ui.theme.NothingThemeTokens

/** UI state for a YouTube / YouTube Music URL pasted into Search or picked from clipboard. */
sealed interface DirectLinkUiState {
    data class Loading(val link: ParsedYouTubeLink) : DirectLinkUiState

    data class Ready(
        val resolved: ResolvedYouTubeLink,
        /** True when a studio match exists and is currently selected instead of the raw video upload. */
        val useStudioMatch: Boolean = true,
        /** Follow-up related tracks for music links; empty for standalone long non-music videos. */
        val relatedTracks: List<Song> = emptyList(),
    ) : DirectLinkUiState {
        fun activeSong(): Song? = when (resolved) {
            is ResolvedYouTubeLink.Track -> {
                if (useStudioMatch && resolved.studioMatch != null) resolved.studioMatch
                else resolved.videoSong
            }
            is ResolvedYouTubeLink.Collection -> null
        }
    }

    data class Error(
        val link: ParsedYouTubeLink,
        val message: String,
    ) : DirectLinkUiState
}

private val LINK_CARD_SHAPE = RoundedCornerShape(16.dp)
private val LINK_BADGE_SHAPE = RoundedCornerShape(12.dp)
private val LINK_ACTION_SHAPE = RoundedCornerShape(10.dp)
private val LINK_ART_SHAPE = RoundedCornerShape(10.dp)

/**
 * Slim dismissible banner shown when a copied YouTube URL is detected on the clipboard.
 * Uses the exact proportions of [SignInBanner] in Standard mode and [NothingCard] in Nothing OS mode.
 */
@Composable
fun CopiedYouTubeLinkBanner(
    link: ParsedYouTubeLink,
    onPlayLink: (ParsedYouTubeLink) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nothingOsMode by AppUi.host.nothingOsMode.collectAsStateWithLifecycle()
    val kindLabel = when (link.kind) {
        YouTubeLinkKind.SHORTS -> "YouTube Shorts copied"
        YouTubeLinkKind.LIVE -> "YouTube Live copied"
        YouTubeLinkKind.PAGE -> if (link.browseId?.startsWith("UC") == true) {
            "YouTube channel copied"
        } else {
            "YouTube playlist copied"
        }
        YouTubeLinkKind.SEARCH -> "YouTube search link copied"
        YouTubeLinkKind.TRACK -> "YouTube link copied"
    }
    val subtitle = buildString {
        append(compactUrlDisplay(link))
        YouTubeLinkParser.formatTimestamp(link.startSeconds)?.let { ts ->
            append(" · at ")
            append(ts)
        }
    }

    val containerModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)

    if (nothingOsMode) {
        NothingCard(
            modifier = containerModifier.clickable { onPlayLink(link) },
            shape = RoundedCornerShape(18.dp),
        ) {
            BannerRowContent(
                title = kindLabel,
                subtitle = subtitle,
                nothingOsMode = true,
                onPlay = { onPlayLink(link) },
                onDismiss = onDismiss,
            )
        }
    } else {
        Surface(
            modifier = containerModifier.clickable { onPlayLink(link) },
            shape = LINK_CARD_SHAPE,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
            BannerRowContent(
                title = kindLabel,
                subtitle = subtitle,
                nothingOsMode = false,
                onPlay = { onPlayLink(link) },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun BannerRowContent(
    title: String,
    subtitle: String,
    nothingOsMode: Boolean,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (nothingOsMode) NothingThemeTokens.SurfaceElevated
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = if (nothingOsMode) NothingThemeTokens.Red else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (nothingOsMode) NothingThemeTokens.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (nothingOsMode) {
            NothingPill(
                text = "OPEN",
                isActive = true,
                onClick = onPlay,
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(LINK_BADGE_SHAPE)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.background,
                )
            }
        }
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Direct-link preview card rendered inside Search when a user pastes or shares a YouTube URL.
 * Mirrors [TopResultCard]'s typography, 72.dp artwork, and action layout while adapting cleanly
 * to Aura Standard Dark/Light and Nothing OS modes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectLinkCard(
    state: DirectLinkUiState,
    currentSong: Song?,
    isPlaying: Boolean,
    onSelectStudioMatch: (Boolean) -> Unit,
    onPlayTrack: (Song, Long) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
    onOpenCollection: (BrowseItem) -> Unit,
    onPlayCollection: (List<Song>, BrowseItem) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    voiceClarityEnabled: Boolean = false,
    onToggleVoiceClarity: () -> Unit = {},
    onDownloadAll: (List<Song>) -> Unit = {},
) {
    val nothingOsMode by AppUi.host.nothingOsMode.collectAsStateWithLifecycle()

    val outerModifier = modifier
        .fillMaxWidth()
        .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 10.dp, bottom = 8.dp)

    val container: @Composable (@Composable () -> Unit) -> Unit = { inner ->
        if (nothingOsMode) {
            NothingCard(
                modifier = outerModifier,
                shape = RoundedCornerShape(20.dp),
            ) {
                Box(Modifier.padding(14.dp)) { inner() }
            }
        } else {
            Surface(
                modifier = outerModifier,
                shape = LINK_CARD_SHAPE,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            ) {
                Box(Modifier.padding(14.dp)) { inner() }
            }
        }
    }

    container {
        when (state) {
            is DirectLinkUiState.Loading -> DirectLinkLoadingBody(state.link, nothingOsMode)
            is DirectLinkUiState.Error -> DirectLinkErrorBody(
                link = state.link,
                message = state.message,
                nothingOsMode = nothingOsMode,
                onRetry = onRetry,
                onOpenExternal = { onOpenExternalUrl(state.link.canonicalUrl) },
            )
            is DirectLinkUiState.Ready -> when (val resolved = state.resolved) {
                is ResolvedYouTubeLink.Track -> {
                    val activeSong = state.activeSong() ?: resolved.videoSong
                    val isCurrent = activeSong.isSameTrackAs(currentSong)
                    val currentlyPlaying = isPlaying && isCurrent
                    val hasStudioOption = resolved.studioMatch != null
                    val studioSelected = state.useStudioMatch && hasStudioOption
                    val effectiveStartMs = when {
                        studioSelected -> 0L
                        resolved.startPositionMs > 0L -> resolved.startPositionMs
                        resolved.resumePositionMs > 0L -> resolved.resumePositionMs
                        else -> 0L
                    }
                    DirectLinkTrackBody(
                        resolved = resolved,
                        displayedSong = activeSong,
                        useStudioMatch = state.useStudioMatch,
                        voiceClarityEnabled = voiceClarityEnabled,
                        effectiveStartMs = effectiveStartMs,
                        isCurrent = isCurrent,
                        isPlaying = currentlyPlaying,
                        nothingOsMode = nothingOsMode,
                        onSelectStudioMatch = onSelectStudioMatch,
                        onToggleVoiceClarity = onToggleVoiceClarity,
                        onPlayAt = { startMs -> onPlayTrack(activeSong, startMs) },
                        onPlayNext = { onPlayNext(activeSong) },
                        onAddToQueue = { onAddToQueue(activeSong) },
                        onDownload = { onDownload(activeSong) },
                        onMore = { onSongMore(activeSong) },
                        onOpenPlaylist = resolved.playlistBrowseId?.let { browseId ->
                            {
                                onOpenCollection(
                                    BrowseItem(
                                        browseId = browseId,
                                        title = resolved.videoSong.title,
                                        subtitle = "YouTube Playlist",
                                        thumbnailUrl = resolved.videoSong.thumbnailUrl,
                                        type = BrowseType.PLAYLIST,
                                    ),
                                )
                            }
                        },
                    )
                }
                is ResolvedYouTubeLink.Collection -> {
                    val item = BrowseItem(
                        browseId = resolved.browseId,
                        title = resolved.title,
                        subtitle = resolved.subtitle,
                        thumbnailUrl = resolved.thumbnailUrl,
                        type = resolved.type,
                    )
                    DirectLinkCollectionBody(
                        collection = resolved,
                        nothingOsMode = nothingOsMode,
                        onOpen = { onOpenCollection(item) },
                        onPlayAll = { onPlayCollection(resolved.songs, item) },
                        onDownloadAll = { onDownloadAll(resolved.songs) },
                        onPlayTrack = { song -> onPlayTrack(song, 0L) },
                        onSongMore = onSongMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectLinkLoadingBody(link: ParsedYouTubeLink, nothingOsMode: Boolean) {
    val label = when (link.kind) {
        YouTubeLinkKind.SHORTS -> "Resolving YouTube Shorts"
        YouTubeLinkKind.LIVE -> "Resolving live stream"
        YouTubeLinkKind.PAGE -> "Loading YouTube playlist"
        YouTubeLinkKind.SEARCH -> "Opening search link"
        YouTubeLinkKind.TRACK -> "Resolving YouTube link"
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinkMetaChip(label, emphasized = false, nothingOsMode = nothingOsMode)
            YouTubeLinkParser.formatTimestamp(link.startSeconds)?.let { ts ->
                LinkMetaChip("Starts $ts", emphasized = true, nothingOsMode = nothingOsMode)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(60.dp), shape = LINK_ART_SHAPE)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.78f).height(16.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.48f).height(13.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(modifier = Modifier.width(74.dp).height(32.dp), shape = LINK_ACTION_SHAPE)
            ShimmerBox(modifier = Modifier.width(82.dp).height(32.dp), shape = LINK_ACTION_SHAPE)
        }
    }
}

@Composable
private fun DirectLinkTrackBody(
    resolved: ResolvedYouTubeLink.Track,
    displayedSong: Song,
    useStudioMatch: Boolean,
    voiceClarityEnabled: Boolean,
    effectiveStartMs: Long,
    isCurrent: Boolean,
    isPlaying: Boolean,
    nothingOsMode: Boolean,
    onSelectStudioMatch: (Boolean) -> Unit,
    onToggleVoiceClarity: () -> Unit,
    onPlayAt: (Long) -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
    onOpenPlaylist: (() -> Unit)?,
) {
    val studioSelected = resolved.studioMatch != null && useStudioMatch
    val kindChip = when {
        resolved.isLive -> "LIVE"
        resolved.isShort -> "SHORTS"
        studioSelected -> "STUDIO AUDIO"
        resolved.videoSong.isVideo -> "VIDEO AUDIO"
        else -> "TRACK"
    }
    val explicitStartText = if (!studioSelected && resolved.startPositionMs > 0L) {
        YouTubeLinkParser.formatTimestamp(resolved.startPositionMs / 1000L)
    } else null
    val resumeText = if (!studioSelected && resolved.startPositionMs == 0L && resolved.resumePositionMs > 0L) {
        YouTubeLinkParser.formatTimestamp(resolved.resumePositionMs / 1000L)
    } else null
    val showVoiceClarityChip = !studioSelected && !resolved.isShort &&
        (resolved.isLongNonMusicVideo || resolved.resolvedFromUniversalFallback)

    val titleColor = when {
        isCurrent -> PlayingAccent
        nothingOsMode -> NothingThemeTokens.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinkMetaChip(kindChip, emphasized = true, nothingOsMode = nothingOsMode)
            displayedSong.durationText?.takeIf { it.isNotBlank() && !resolved.isLive }?.let { duration ->
                LinkMetaChip(duration, emphasized = false, nothingOsMode = nothingOsMode)
            }
            explicitStartText?.let { ts ->
                LinkMetaChip("Starts $ts", emphasized = false, nothingOsMode = nothingOsMode)
            }
            resumeText?.let { ts ->
                LinkMetaChip("Resume $ts", emphasized = false, nothingOsMode = nothingOsMode)
            }
            if (showVoiceClarityChip) {
                LinkMetaChip(
                    text = "Voice clarity",
                    emphasized = voiceClarityEnabled,
                    nothingOsMode = nothingOsMode,
                    leadingIcon = Icons.Rounded.GraphicEq,
                    onClick = onToggleVoiceClarity,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .longPressMenuClickable(onClick = { onPlayAt(effectiveStartMs) }, onLongClick = onMore)
                .contextClick(onMore),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val resumeTotalMs = resolved.videoSong.durationMillis()
            val showResumeBar = !studioSelected && resolved.resumePositionMs > 0L && resumeTotalMs > 0L
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(LINK_ART_SHAPE)
                    .thumbnailBorder(LINK_ART_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = displayedSong.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                )
                if (isCurrent && isPlaying) {
                    SearchPlayingBars(Modifier.align(Alignment.Center))
                }
                if (showResumeBar) {
                    val fraction = (resolved.resumePositionMs.toFloat() / resumeTotalMs.toFloat())
                        .coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(3.dp)
                                .background(if (nothingOsMode) NothingThemeTokens.Red else PlayingAccent),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = displayedSong.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                val subtitle = listOfNotNull(
                    displayedSong.artist.takeIf { it.isNotBlank() },
                    displayedSong.albumName?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { "YouTube" }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    DownloadedBadge(
                        videoId = displayedSong.videoId,
                        tint = subtitleColor,
                    )
                }
            }
            RowMoreButton(
                onClick = onMore,
                tint = if (nothingOsMode) NothingThemeTokens.White else MaterialTheme.colorScheme.onSurface,
                iconSize = 22.dp,
            )
        }

        if (resolved.studioMatch != null) {
            Spacer(Modifier.height(10.dp))
            if (nothingOsMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NothingPill(
                        text = "STUDIO MATCH",
                        isActive = useStudioMatch,
                        onClick = { onSelectStudioMatch(true) },
                    )
                    NothingPill(
                        text = "VIDEO AUDIO",
                        isActive = !useStudioMatch,
                        onClick = { onSelectStudioMatch(false) },
                    )
                }
            } else {
                PillSegmentedControl(
                    options = listOf("Studio match", "Video audio"),
                    selectedIndex = if (useStudioMatch) 0 else 1,
                    onSelect = { idx -> onSelectStudioMatch(idx == 0) },
                )
            }
        }

        if (!studioSelected && resolved.chapters.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Chapters (${resolved.chapters.size})",
                style = MaterialTheme.typography.labelMedium,
                color = subtitleColor,
            )
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                resolved.chapters.forEach { chapter ->
                    LinkChapterChip(
                        chapter = chapter,
                        nothingOsMode = nothingOsMode,
                        onClick = { onPlayAt(chapter.startSeconds * 1000L) },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinkActionButton(
                label = when {
                    isCurrent && isPlaying -> "Playing"
                    resumeText != null -> "Resume $resumeText"
                    else -> "Play"
                },
                icon = Icons.Rounded.PlayArrow,
                primary = true,
                nothingOsMode = nothingOsMode,
                onClick = { onPlayAt(effectiveStartMs) },
            )
            if (resumeText != null) {
                LinkActionButton(
                    label = "Restart",
                    icon = Icons.Rounded.Replay,
                    primary = false,
                    nothingOsMode = nothingOsMode,
                    onClick = { onPlayAt(0L) },
                )
            }
            LinkActionButton(
                label = "Next",
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                primary = false,
                nothingOsMode = nothingOsMode,
                onClick = onPlayNext,
            )
            LinkActionButton(
                label = "Queue",
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                primary = false,
                nothingOsMode = nothingOsMode,
                onClick = onAddToQueue,
            )
            if (!resolved.isLive) {
                LinkActionButton(
                    label = "Save",
                    icon = Icons.Rounded.Download,
                    primary = false,
                    nothingOsMode = nothingOsMode,
                    onClick = onDownload,
                )
            }
            if (onOpenPlaylist != null) {
                LinkActionButton(
                    label = "Playlist",
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    primary = false,
                    nothingOsMode = nothingOsMode,
                    onClick = onOpenPlaylist,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectLinkCollectionBody(
    collection: ResolvedYouTubeLink.Collection,
    nothingOsMode: Boolean,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onDownloadAll: () -> Unit,
    onPlayTrack: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val artShape = if (collection.type == BrowseType.ARTIST) CircleShape else LINK_ART_SHAPE
    val kindLabel = when (collection.type) {
        BrowseType.ALBUM -> "YOUTUBE ALBUM"
        BrowseType.ARTIST -> "YOUTUBE CHANNEL"
        else -> "YOUTUBE PLAYLIST"
    }
    val titleColor = if (nothingOsMode) NothingThemeTokens.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth()) {
        LinkMetaChip(kindLabel, emphasized = true, nothingOsMode = nothingOsMode)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = collection.thumbnailUrl?.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(artShape)
                    .thumbnailBorder(artShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = collection.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (collection.songs.isNotEmpty()) {
                LinkActionButton(
                    label = "Play all",
                    icon = Icons.Rounded.PlayArrow,
                    primary = true,
                    nothingOsMode = nothingOsMode,
                    onClick = onPlayAll,
                )
                if (collection.type != BrowseType.ARTIST) {
                    LinkActionButton(
                        label = "Save all",
                        icon = Icons.Rounded.Download,
                        primary = false,
                        nothingOsMode = nothingOsMode,
                        onClick = onDownloadAll,
                    )
                }
            }
            LinkActionButton(
                label = "Open",
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                primary = collection.songs.isEmpty(),
                nothingOsMode = nothingOsMode,
                onClick = onOpen,
            )
        }
        if (collection.songs.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = if (nothingOsMode) NothingThemeTokens.BorderSubtle else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
            val preview = collection.songs.take(3)
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                preview.forEach { track ->
                    CollectionPreviewTrackRow(
                        song = track,
                        nothingOsMode = nothingOsMode,
                        onClick = { onPlayTrack(track) },
                        onMore = { onSongMore(track) },
                    )
                }
                if (collection.songs.size > preview.size) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpen)
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "View all ${collection.songs.size} tracks",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (nothingOsMode) NothingThemeTokens.White else MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionPreviewTrackRow(
    song: Song,
    nothingOsMode: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .longPressMenuClickable(onClick = onClick, onLongClick = onMore)
            .contextClick(onMore)
            .padding(vertical = 5.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnailUrl?.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .thumbnailBorder(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (nothingOsMode) NothingThemeTokens.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                song.artist.takeIf { it.isNotBlank() },
                song.durationText?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DirectLinkErrorBody(
    link: ParsedYouTubeLink,
    message: String,
    nothingOsMode: Boolean,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        LinkMetaChip("UNABLE TO PLAY IN APP", emphasized = true, nothingOsMode = nothingOsMode)
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = if (nothingOsMode) NothingThemeTokens.White else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = compactUrlDisplay(link),
            style = MaterialTheme.typography.bodySmall,
            color = if (nothingOsMode) NothingThemeTokens.MutedText else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinkActionButton(
                label = "Try again",
                icon = Icons.Rounded.Refresh,
                primary = true,
                nothingOsMode = nothingOsMode,
                onClick = onRetry,
            )
            LinkActionButton(
                label = "Open link",
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                primary = false,
                nothingOsMode = nothingOsMode,
                onClick = onOpenExternal,
            )
        }
    }
}

@Composable
private fun LinkMetaChip(
    text: String,
    emphasized: Boolean,
    nothingOsMode: Boolean,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    if (nothingOsMode) {
        NothingPill(
            text = text.uppercase(),
            isActive = emphasized,
            leadingIcon = leadingIcon,
            onClick = onClick ?: {},
        )
        return
    }
    Row(
        modifier = Modifier
            .clip(LINK_BADGE_SHAPE)
            .background(
                if (emphasized) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = LINK_BADGE_SHAPE,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (emphasized) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (emphasized) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun LinkChapterChip(
    chapter: VideoChapter,
    nothingOsMode: Boolean,
    onClick: () -> Unit,
) {
    val label = buildString {
        append(chapter.formattedTime)
        append(" · ")
        val clean = chapter.title
        append(if (clean.length > 26) "${clean.take(25)}…" else clean)
    }
    if (nothingOsMode) {
        NothingPill(
            text = label,
            isActive = false,
            onClick = onClick,
        )
        return
    }
    Box(
        modifier = Modifier
            .clip(LINK_BADGE_SHAPE)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = LINK_BADGE_SHAPE,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun LinkActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    nothingOsMode: Boolean,
    onClick: () -> Unit,
) {
    if (nothingOsMode) {
        val borderColor = if (primary) NothingThemeTokens.Red else NothingThemeTokens.Border
        val textColor = if (primary) NothingThemeTokens.White else NothingThemeTokens.MutedText
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(if (primary) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface)
                .border(1.dp, borderColor, RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (primary) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(NothingThemeTokens.Red),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = label.uppercase(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.4.sp,
                maxLines = 1,
            )
        }
        return
    }
    val containerColor = if (primary) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (primary) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (primary) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(LINK_ACTION_SHAPE)
            .background(containerColor)
            .border(0.75.dp, borderColor, LINK_ACTION_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            maxLines = 1,
        )
    }
}

private fun compactUrlDisplay(link: ParsedYouTubeLink): String {
    val videoId = link.videoId
    val browseId = link.browseId
    return when {
        videoId != null -> "youtube.com/watch?v=$videoId"
        browseId != null -> "youtube.com/playlist?list=${browseId.removePrefix("VL")}"
        else -> link.canonicalUrl.removePrefix("https://").removePrefix("http://")
    }
}
