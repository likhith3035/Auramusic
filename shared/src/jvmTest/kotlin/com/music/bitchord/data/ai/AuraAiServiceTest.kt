package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuraAiServiceTest {

    private val sampleProfile = AuraUserBehaviorProfile(
        chronotype = AuraChronotype.NIGHT_OWL,
        repeatQuotient = 0.5f,
        totalMinutesToday = 120,
        totalTracksToday = 18,
        topRepeatedSongTitle = "Chuttamalle",
        topRepeatedArtist = "Anirudh",
        topRepeatedCount = 10,
        topSinger = "Shilpa Rao",
        topSingerPlays = 10,
        topMusicDirector = "Anirudh",
        topMusicDirectorPlays = 10,
        peakHour = 23,
        personalityTags = listOf("Late-Night Melophile"),
        behavioralSummaryText = "Listening for 120 mins.",
        roastSummaryText = "Playing Chuttamalle 10 times.",
    )

    @Test
    fun testResolveSingleSongReturnsDirectlyIfAlreadyResolved() = kotlinx.coroutines.runBlocking {
        val resolvedSong = Song(videoId = "real_video_id_123", title = "Chuttamalle", artist = "Anirudh", thumbnailUrl = "http://art")
        val result = AuraAiService.resolveSingleSong(resolvedSong)
        kotlin.test.assertEquals("real_video_id_123", result.videoId)
        kotlin.test.assertEquals("Chuttamalle", result.title)
    }

    @Test
    fun testBuildSystemPromptContainsAllRequiredInfo() {
        val prompt = AuraAiService.buildSystemPrompt(
            persona = AuraAiPersona.SAVAGE_ROASTER,
            profile = sampleProfile,
            currentSong = Song(videoId = "v1", title = "Fear Song", artist = "Anirudh", thumbnailUrl = null),
        )

        assertTrue(prompt.contains("Kami Likhith"))
        assertTrue(prompt.contains("https://github.com/likhith3035"))
        assertTrue(prompt.contains("https://www.instagram.com/lucky__likhith"))
        assertTrue(prompt.contains("https://linkedin.com/in/likhith-kami"))
        assertTrue(prompt.contains("10-Band Studio EQ"))
        assertTrue(prompt.contains("Listen Together"))
        assertTrue(prompt.contains("Chuttamalle"))
        assertTrue(prompt.contains("Fear Song"))
        assertTrue(prompt.contains("[SONG:"))
        assertTrue(prompt.contains("[ACTION:"))
    }
}
