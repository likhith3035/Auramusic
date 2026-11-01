package com.music.bitchord.data.ai

import com.music.bitchord.data.stats.DailyHighlightsSummary

/**
 * Synthesizes raw listening logs into psychological listening habits,
 * chronotypes, repeat scores, and personality tags.
 */
object AuraBehaviorEngine {

    fun analyze(highlights: DailyHighlightsSummary?): AuraUserBehaviorProfile = analyzeDailyProfile(highlights)

    fun analyzeDailyProfile(highlights: DailyHighlightsSummary?): AuraUserBehaviorProfile {
        if (highlights == null) {
            return AuraUserBehaviorProfile(
                chronotype = AuraChronotype.EVENING_UNWIND,
                repeatQuotient = 0.0f,
                totalMinutesToday = 0,
                totalTracksToday = 0,
                topRepeatedSongTitle = null,
                topRepeatedArtist = null,
                topRepeatedCount = 0,
                topSinger = null,
                topSingerPlays = 0,
                topMusicDirector = null,
                topMusicDirectorPlays = 0,
                peakHour = null,
                personalityTags = listOf("Fresh Explorer"),
                behavioralSummaryText = "Welcome to AuraMusic! Start listening to tracks to unlock deep behavioral insights and personalized acoustic roasts.",
                roastSummaryText = "Your library is so fresh there's nothing to roast yet! Play a few songs and come back to see how your music taste fares.",
            )
        }
        val peak = highlights.peakHour ?: 14
        val chronotype = when (peak) {
            in 5..10 -> AuraChronotype.EARLY_BIRD
            in 11..16 -> AuraChronotype.AFTERNOON_FLOW
            in 17..21 -> AuraChronotype.EVENING_UNWIND
            else -> AuraChronotype.NIGHT_OWL
        }

        val totalPlays = highlights.totalPlays.coerceAtLeast(1)
        val repeatQuotient = (highlights.topRepeatedCount.toFloat() / totalPlays).coerceIn(0f, 1f)

        val tags = mutableListOf<String>()
        when (chronotype) {
            AuraChronotype.NIGHT_OWL -> tags.add("Late-Night Melophile")
            AuraChronotype.EARLY_BIRD -> tags.add("Morning Riser")
            AuraChronotype.AFTERNOON_FLOW -> tags.add("Deep Focus Listener")
            AuraChronotype.EVENING_UNWIND -> tags.add("Sunset Chiller")
            AuraChronotype.CONTINUOUS_VIBE -> tags.add("All-Day Streamer")
        }

        if (repeatQuotient >= 0.4f && highlights.topRepeatedCount >= 3) {
            tags.add("Obsessive Looper")
        } else if (repeatQuotient < 0.18f && totalPlays >= 6) {
            tags.add("Eclectic Explorer")
        }

        if (!highlights.topMusicDirector.isNullOrBlank()) {
            tags.add("Composer Connoisseur")
        }

        if (highlights.totalMinutes > 90) {
            tags.add("Audiophile Heavyweight")
        }

        val summary = buildString {
            append("You've immersed yourself in ${highlights.totalMinutes} minutes of music today across ${highlights.totalTracksCount} tracks. ")
            if (highlights.topRepeatedSong != null && highlights.topRepeatedCount > 1) {
                append("Your ultimate comfort loop is '${highlights.topRepeatedSong.title}' (${highlights.topRepeatedCount} plays). ")
            }
            if (!highlights.topSinger.isNullOrBlank()) {
                append("Your top vocalist is ${highlights.topSinger} with ${highlights.topSingerPlays} plays. ")
            }
            if (!highlights.topMusicDirector.isNullOrBlank()) {
                append("Sonic direction heavily shaped by ${highlights.topMusicDirector}.")
            }
        }

        val roast = buildString {
            if (repeatQuotient > 0.45f && highlights.topRepeatedSong != null) {
                append("Did you forget that other songs exist? You played '${highlights.topRepeatedSong.title}' ${highlights.topRepeatedCount} times today. Are you going through a cinematic heartbreak or did your replay button weld itself shut?")
            } else if (chronotype == AuraChronotype.NIGHT_OWL) {
                append("Streaming deep beats past midnight again? Your sleep schedule is as broken as an unbuffered 3G stream, but we have to admit—your late-night soundtrack is undeniably immaculate.")
            } else if (highlights.totalMinutes > 180) {
                append("Over 3 hours of listening today! Your headphones are practically surgically attached at this point.")
            } else {
                append("A respectable listening session! Jumping between artists this fast suggests your attention span might need its own dedicated EQ curve.")
            }
        }

        return AuraUserBehaviorProfile(
            chronotype = chronotype,
            repeatQuotient = repeatQuotient,
            totalMinutesToday = highlights.totalMinutes,
            totalTracksToday = highlights.totalTracksCount,
            topRepeatedSongTitle = highlights.topRepeatedSong?.title,
            topRepeatedArtist = highlights.topRepeatedSong?.artist,
            topRepeatedCount = highlights.topRepeatedCount,
            topSinger = highlights.topSinger,
            topSingerPlays = highlights.topSingerPlays,
            topMusicDirector = highlights.topMusicDirector,
            topMusicDirectorPlays = highlights.topMusicDirectorPlays,
            peakHour = highlights.peakHour,
            personalityTags = tags,
            behavioralSummaryText = summary,
            roastSummaryText = roast,
        )
    }
}
