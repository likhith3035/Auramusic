package com.music.bitchord.data.ai

import kotlinx.serialization.Serializable

/**
 * Chronotype based on peak listening hours of the day.
 */
enum class AuraChronotype(val title: String, val description: String, val emoji: String) {
    EARLY_BIRD("Early Bird", "Energized morning listener fueled by upbeat acoustic rhythms", "🌅"),
    AFTERNOON_FLOW("Daytime Focuser", "Productive daytime listener balancing flow and focus", "☀️"),
    EVENING_UNWIND("Evening Chiller", "Golden hour relaxer unwinding after a long day", "🌆"),
    NIGHT_OWL("Midnight Dreamer", "Deep nocturnal melophile lost in moody soundscapes", "🌙"),
    CONTINUOUS_VIBE("Continuous Vibe", "Non-stop music companion listening around the clock", "⚡"),
}

/**
 * Behavioral summary and habit breakdown extracted from listening data.
 */
@Serializable
data class AuraUserBehaviorProfile(
    val chronotype: AuraChronotype,
    val repeatQuotient: Float, // 0.0 to 1.0 (proportion of plays spent on top repeated track)
    val totalMinutesToday: Long,
    val totalTracksToday: Int,
    val topRepeatedSongTitle: String?,
    val topRepeatedArtist: String?,
    val topRepeatedCount: Int,
    val topSinger: String?,
    val topSingerPlays: Int = 0,
    val topMusicDirector: String?,
    val topMusicDirectorPlays: Int = 0,
    val peakHour: Int? = null,
    val personalityTags: List<String>,
    val behavioralSummaryText: String,
    val roastSummaryText: String,
)
