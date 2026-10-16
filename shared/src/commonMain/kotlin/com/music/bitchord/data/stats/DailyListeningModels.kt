package com.music.bitchord.data.stats

import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable

/**
 * A recorded playback event in the rolling 24-hour daily window.
 */
@Serializable
data class DailyPlayRecord(
    val videoId: String,
    val title: String,
    val artist: String,
    val art: String? = null,
    val durationText: String? = null,
    val timestamp: Long,
    val playedMs: Long,
    val countsAsPlay: Boolean,
    val singer: String,
    val musicDirector: String? = null,
)

/**
 * Aggregated 1-Day / 24-Hour listening stats for the Library and Replay.
 */
data class DailyHighlightsSummary(
    val topRepeatedSong: Song? = null,
    val topRepeatedCount: Int = 0,
    val topSinger: String? = null,
    val topSingerMinutes: Long = 0L,
    val topSingerPlays: Int = 0,
    val topMusicDirector: String? = null,
    val topMusicDirectorMinutes: Long = 0L,
    val topMusicDirectorPlays: Int = 0,
    val totalPlayedMs: Long = 0L,
    val totalPlays: Int = 0,
    val totalTracksCount: Int = 0,
    val peakHour: Int? = null,
    val todaySongs: List<Song> = emptyList(),
) {
    val totalMinutes: Long get() = totalPlayedMs / 60_000L
    val isEmpty: Boolean get() = topRepeatedSong == null && totalPlayedMs <= 0L
}

/**
 * Classification of artist roles into lead singer/vocalist and music director/composer.
 */
data class ArtistRoles(
    val singer: String,
    val musicDirector: String?,
)
