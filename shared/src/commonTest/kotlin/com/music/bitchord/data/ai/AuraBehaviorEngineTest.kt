package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.DailyHighlightsSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuraBehaviorEngineTest {

    private fun mockSong(id: String, title: String, artist: String): Song =
        Song(videoId = id, title = title, artist = artist, thumbnailUrl = null)

    @Test
    fun testChronotypeDetectionNightOwl() {
        // Peak hour 23 (11 PM)
        val highlights = DailyHighlightsSummary(
            topRepeatedSong = mockSong("s1", "Chuttamalle", "Anirudh Ravichander"),
            topRepeatedCount = 5,
            peakHour = 23,
            totalPlayedMs = 7_200_000L,
            totalPlays = 15,
            totalTracksCount = 10,
        )
        val profile = AuraBehaviorEngine.analyzeDailyProfile(highlights)
        assertEquals(AuraChronotype.NIGHT_OWL, profile.chronotype)
        assertTrue(profile.personalityTags.contains("Late-Night Melophile"))
    }

    @Test
    fun testRepeatObsessionIndex() {
        val song = mockSong("s2", "Samajavaragamana", "Sid Sriram, Thaman S")
        val highlights = DailyHighlightsSummary(
            topRepeatedSong = song,
            topRepeatedCount = 12,
            totalPlays = 15,
            totalPlayedMs = 3_600_000L,
            topSinger = "Sid Sriram",
            topMusicDirector = "Thaman S",
        )
        val profile = AuraBehaviorEngine.analyzeDailyProfile(highlights)
        // 12 / 15 plays = 80% repeat quotient
        assertTrue(profile.repeatQuotient > 0.7f)
        assertTrue(profile.personalityTags.contains("Obsessive Looper"))
        assertTrue(profile.personalityTags.contains("Composer Connoisseur"))
        assertNotNull(profile.topSinger)
        assertEquals("Thaman S", profile.topMusicDirector)
    }
}
