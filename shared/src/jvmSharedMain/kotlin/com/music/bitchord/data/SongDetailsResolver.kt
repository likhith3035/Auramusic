package com.music.bitchord.data

import com.music.bitchord.data.model.ArtistRef
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.SongDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves extended metadata, contributors, release year, album information,
 * and audio stream specifications for a track with in-memory caching and safe timeouts.
 */
object SongDetailsResolver {
    private val YEAR_REGEX = Regex("""\b(19\d\d|20\d\d)\b""")
    private val cache = ConcurrentHashMap<String, SongDetails>()

    suspend fun resolve(
        song: Song,
        audioQuality: String? = null,
        streamSource: String? = null,
        playStats: com.music.bitchord.data.model.SongPlayStats? = null,
    ): SongDetails {
        cache[song.videoId]?.let { cached ->
            if (playStats != null && cached.playStats != playStats) {
                val updated = cached.copy(playStats = playStats)
                cache[song.videoId] = updated
                return updated
            }
            return cached
        }

        return withContext(Dispatchers.IO) {
            var current = song

            // If artist links or album ids are missing, lookup track links from watch queue
            if ((current.artists.isEmpty() || current.albumId == null) && current.videoId.isNotBlank() && current.localUri == null) {
                val linked = runCatching {
                    withTimeoutOrNull(3000L) {
                        YtMusicRepository.trackLinks(song.videoId).getOrNull()
                    }
                }.getOrNull()

                if (linked != null) {
                    current = current.copy(
                        artists = if (current.artists.isEmpty()) linked.artists else current.artists,
                        artistId = current.artistId ?: linked.artistId,
                        albumName = current.albumName ?: linked.albumName,
                        albumId = current.albumId ?: linked.albumId,
                    )
                }
            }

            var releaseYear: String? = null
            var description: String? = null

            val albumId = current.albumId
            if (albumId != null && current.localUri == null && !albumId.startsWith("local_")) {
                val albumPage = runCatching {
                    withTimeoutOrNull(3500L) {
                        YtMusicRepository.browseSongs(albumId).getOrNull()
                    }
                }.getOrNull()

                if (albumPage != null) {
                    val subtitle = albumPage.header?.subtitle.orEmpty()
                    releaseYear = YEAR_REGEX.find(subtitle)?.value
                    description = albumPage.description?.takeIf { it.isNotBlank() }
                }
            }

            val artistsList = if (current.artists.isNotEmpty()) {
                current.artists
            } else if (current.artist.isNotBlank()) {
                listOf(ArtistRef(name = current.artist, browseId = current.artistId))
            } else {
                emptyList()
            }

            val quality = audioQuality ?: if (song.localUri != null) "Local Audio (Original Quality)" else "High Quality (256 kbps Opus/AAC)"
            val source = streamSource ?: if (song.localUri != null) "Local Storage" else "YouTube Music"

            val details = SongDetails(
                song = current,
                releaseDateOrYear = releaseYear,
                artists = artistsList,
                albumName = current.albumName,
                albumId = current.albumId,
                durationText = current.durationText,
                audioQuality = quality,
                streamSource = source,
                viewCountText = null,
                likeCountText = null,
                description = description,
                playStats = playStats,
            )

            cache[song.videoId] = details
            details
        }
    }
}
