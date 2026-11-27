package com.music.bitchord.data.link

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.durationMillis

/**
 * Resolves any pasted or shared YouTube / YouTube Music link into playable tracks or a
 * browsable collection across Android and Desktop.
 */
sealed interface ResolvedYouTubeLink {
    val link: ParsedYouTubeLink

    data class Track(
        override val link: ParsedYouTubeLink,
        /** Exact audio from the pasted YouTube upload / video. */
        val videoSong: Song,
        /** Official studio audio match when the link is a music video / music upload, or null. */
        val studioMatch: Song? = null,
        val isLive: Boolean = false,
        val isShort: Boolean = false,
        val startPositionMs: Long = 0L,
        /** Saved resume offset for long videos (>= 10 min) when no explicit URL timestamp was supplied. */
        val resumePositionMs: Long = 0L,
        val playlistBrowseId: String? = null,
        val resolvedFromUniversalFallback: Boolean = false,
        val chapters: List<VideoChapter> = emptyList(),
    ) : ResolvedYouTubeLink {
        val isLongNonMusicVideo: Boolean
            get() = !isShort && !isLive &&
                (resolvedFromUniversalFallback || videoSong.isVideo) &&
                videoSong.durationMillis() >= YouTubeResumeStore.MIN_TRACK_DURATION_MS
    }

    data class Collection(
        override val link: ParsedYouTubeLink,
        val browseId: String,
        val title: String,
        val subtitle: String,
        val thumbnailUrl: String?,
        val type: BrowseType,
        val songs: List<Song>,
    ) : ResolvedYouTubeLink
}

object YouTubeLinkResolver {

    /** Maximum video runtime eligible for speculative studio-audio matching (~12 min). */
    private const val STUDIO_MATCH_MAX_SECONDS = 12 * 60L

    /**
     * Resolves metadata for a [ParsedYouTubeLink]:
     * - Track / Shorts / Live links try YT Music track metadata first and transparently
     *   fall back to NewPipe YouTube extraction so general YouTube videos also work.
     * - Playlist / Album / Browse links fetch the browsed song list header and tracks.
     */
    suspend fun resolve(link: ParsedYouTubeLink): Result<ResolvedYouTubeLink> {
        link.videoId?.let { videoId ->
            return resolveVideo(link, videoId)
        }
        val browseId = link.browseId ?: link.playlistBrowseId
        if (browseId != null) {
            return resolveCollection(link, browseId)
        }
        return Result.failure(IllegalArgumentException("Unsupported YouTube link"))
    }

    /**
     * Best-effort lookup for an official studio audio match when [track] is a music video
     * or music upload. Kept separate from [resolve] so the link card renders immediately and
     * promotes the studio option only if a confident match is found.
     */
    suspend fun findStudioMatch(track: ResolvedYouTubeLink.Track): Song? {
        if (track.studioMatch != null) return track.studioMatch
        if (track.isLive || track.isShort) return null
        val durationSec = parseDurationSeconds(track.videoSong.durationText)
        if (track.resolvedFromUniversalFallback && (durationSec == null || durationSec > STUDIO_MATCH_MAX_SECONDS)) {
            return null
        }
        if (!track.videoSong.isVideo && !track.resolvedFromUniversalFallback) return null
        val candidate = YtMusicRepository.resolveAudio(track.videoSong.copy(isVideo = true))
        return candidate.takeIf { it.videoId != track.videoSong.videoId }
    }

    /**
     * Fetches related tracks for a music link so Search shows follow-up picks underneath the card.
     * Long non-music videos (>= 10 min), Shorts, and livestreams stay standalone with no radio noise.
     */
    suspend fun findRelatedTracks(track: ResolvedYouTubeLink.Track, limit: Int = 8): List<Song> {
        if (track.isLongNonMusicVideo || track.isShort || track.isLive) return emptyList()
        val seedId = (track.studioMatch ?: track.videoSong).videoId
        return YtMusicRepository.radio(seedId)
            .getOrNull()
            .orEmpty()
            .filter { it.videoId != seedId && it.videoId != track.videoSong.videoId }
            .distinctBy { it.videoId }
            .take(limit)
    }

    private suspend fun resolveVideo(link: ParsedYouTubeLink, videoId: String): Result<ResolvedYouTubeLink> {
        val isShort = link.kind == YouTubeLinkKind.SHORTS
        val startMs = (link.startSeconds * 1000L).coerceAtLeast(0L)

        // Fast path: YT Music metadata gives full artist credits, album links, and artwork.
        if (link.kind != YouTubeLinkKind.LIVE) {
            val ytMusicSong = YtMusicRepository.trackLinks(videoId).getOrNull()
            if (ytMusicSong != null && ytMusicSong.title.isNotBlank()) {
                val baseSong = if (isShort) {
                    ytMusicSong.copy(isVideo = true, isVideoOrigin = true)
                } else {
                    ytMusicSong
                }
                val isLong = !isShort &&
                    baseSong.isVideo &&
                    baseSong.durationMillis() >= YouTubeResumeStore.MIN_TRACK_DURATION_MS
                val resumeMs = if (startMs == 0L && isLong) {
                    YouTubeResumeStore.savedPositionMs(videoId) ?: 0L
                } else {
                    0L
                }
                val chapters = if (isLong) {
                    StreamResolver.fetchLinkMetadata(videoId).getOrNull()?.chapters.orEmpty()
                } else {
                    emptyList()
                }
                return Result.success(
                    ResolvedYouTubeLink.Track(
                        link = link,
                        videoSong = baseSong,
                        isLive = false,
                        isShort = isShort,
                        startPositionMs = startMs,
                        resumePositionMs = resumeMs,
                        playlistBrowseId = link.playlistBrowseId,
                        resolvedFromUniversalFallback = false,
                        chapters = chapters,
                    ),
                )
            }
        }

        // Universal YouTube fallback: handles any public/unlisted YouTube video, Shorts, podcasts,
        // remixes, tutorials, and live broadcasts.
        return StreamResolver.fetchLinkMetadata(videoId).map { meta ->
            val durationText = meta.durationSeconds?.let(::formatDuration)
            val isLive = meta.isLive || link.kind == YouTubeLinkKind.LIVE
            val song = Song(
                videoId = meta.videoId,
                title = meta.title,
                artist = meta.artist.ifBlank { "YouTube" },
                thumbnailUrl = meta.thumbnailUrl,
                durationText = durationText,
                isVideo = true,
                isVideoOrigin = true,
            )
            val resumeMs = if (!isLive && !isShort && startMs == 0L &&
                YouTubeResumeStore.isEligibleLongVideo(song)
            ) {
                YouTubeResumeStore.savedPositionMs(videoId) ?: 0L
            } else {
                0L
            }
            ResolvedYouTubeLink.Track(
                link = link,
                videoSong = song,
                isLive = isLive,
                isShort = isShort,
                startPositionMs = startMs,
                resumePositionMs = resumeMs,
                playlistBrowseId = link.playlistBrowseId,
                resolvedFromUniversalFallback = true,
                chapters = meta.chapters,
            )
        }
    }

    private suspend fun resolveCollection(
        link: ParsedYouTubeLink,
        browseId: String,
    ): Result<ResolvedYouTubeLink> {
        if (browseId.startsWith("UC")) {
            return YtMusicRepository.artistPage(browseId).map { artist ->
                ResolvedYouTubeLink.Collection(
                    link = link,
                    browseId = browseId,
                    title = artist.name?.takeIf { it.isNotBlank() } ?: "YouTube Channel",
                    subtitle = artist.subscriberCountText?.takeIf { it.isNotBlank() }
                        ?.let { "$it · ${artist.songs.size} top tracks" }
                        ?: "${artist.songs.size} top tracks",
                    thumbnailUrl = artist.thumbnailUrl ?: artist.songs.firstOrNull()?.thumbnailUrl,
                    type = BrowseType.ARTIST,
                    songs = artist.songs,
                )
            }
        }
        return YtMusicRepository.browseSongs(browseId).map { page ->
            val header = page.header
            val type = when {
                browseId.startsWith("VL") -> BrowseType.PLAYLIST
                browseId.startsWith("MPRE") || browseId.startsWith("OLAK") -> BrowseType.ALBUM
                else -> BrowseType.PLAYLIST
            }
            ResolvedYouTubeLink.Collection(
                link = link,
                browseId = browseId,
                title = header?.title?.takeIf { it.isNotBlank() } ?: "YouTube Playlist",
                subtitle = header?.subtitle?.takeIf { it.isNotBlank() }
                    ?: "${page.songs.size} tracks",
                thumbnailUrl = header?.thumbnailUrl ?: page.songs.firstOrNull()?.thumbnailUrl,
                type = type,
                songs = page.songs,
            )
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600L
        val m = (seconds % 3600L) / 60L
        val s = seconds % 60L
        return if (h > 0L) {
            "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        } else {
            "$m:${s.toString().padStart(2, '0')}"
        }
    }

    private fun parseDurationSeconds(text: String?): Long? {
        val parts = text?.trim()?.split(':')?.mapNotNull { it.toLongOrNull() } ?: return null
        return when (parts.size) {
            2 -> parts[0] * 60L + parts[1]
            3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
            else -> null
        }
    }
}
