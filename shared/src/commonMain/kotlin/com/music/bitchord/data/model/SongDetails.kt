package com.music.bitchord.data.model

/**
 * Rich metadata and credits for a song.
 *
 * Surfaces contributing vocalists, producers, release year/date,
 * album relationships, and audio playback quality.
 */
data class SongDetails(
    val song: Song,
    val releaseDateOrYear: String? = null,
    val artists: List<ArtistRef> = emptyList(),
    val albumName: String? = null,
    val albumId: String? = null,
    val durationText: String? = null,
    val audioQuality: String? = null,
    val streamSource: String? = null,
    val viewCountText: String? = null,
    val likeCountText: String? = null,
    val description: String? = null,
)
