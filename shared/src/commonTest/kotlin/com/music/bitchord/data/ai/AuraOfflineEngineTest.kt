package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.DailyHighlightsSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuraOfflineEngineTest {

    private val sampleProfile = AuraUserBehaviorProfile(
        chronotype = AuraChronotype.NIGHT_OWL,
        repeatQuotient = 0.6f,
        totalMinutesToday = 145,
        totalTracksToday = 22,
        topRepeatedSongTitle = "Chuttamalle",
        topRepeatedArtist = "Anirudh Ravichander",
        topRepeatedCount = 14,
        topSinger = "Shilpa Rao",
        topSingerPlays = 14,
        topMusicDirector = "Anirudh Ravichander",
        topMusicDirectorPlays = 14,
        peakHour = 23,
        personalityTags = listOf("Late-Night Melophile", "Obsessive Looper"),
        behavioralSummaryText = "You've listened to 145 minutes of music today.",
        roastSummaryText = "Did you forget other songs exist? You played 'Chuttamalle' 14 times.",
    )

    @Test
    fun testDeveloperAttributionAndSocials() {
        val reply = AuraOfflineEngine.respond(
            userPrompt = "Who developed this app?",
            profile = sampleProfile,
            persona = AuraAiPersona.COMPANION,
            currentSong = null
        )
        assertNotNull(reply)
        assertTrue(reply.text.contains("Kami Likhith"))
        assertTrue(reply.actionChips.any { it.label == "GitHub" && it.payload?.contains("likhith3035") == true })
        assertTrue(reply.actionChips.any { it.label == "Instagram" && it.payload?.contains("lucky__likhith") == true })
        assertTrue(reply.actionChips.any { it.label == "LinkedIn" })
    }

    @Test
    fun testAppFeaturesGuidance() {
        val reply = AuraOfflineEngine.respond(
            userPrompt = "How do I use the equalizer?",
            profile = sampleProfile,
            persona = AuraAiPersona.COMPANION,
            currentSong = null
        )
        assertNotNull(reply)
        assertTrue(reply.text.contains("10-Band Studio EQ"))
        assertTrue(reply.actionChips.any { it.type == "OPEN_EQ" })
    }

    @Test
    fun testRoastResponse() {
        val reply = AuraOfflineEngine.respond(
            userPrompt = "Roast my music taste",
            profile = sampleProfile,
            persona = AuraAiPersona.SAVAGE_ROASTER,
            currentSong = null
        )
        assertNotNull(reply)
        assertTrue(reply.text.contains("Chuttamalle"))
        assertTrue(reply.text.contains("Aura Taste Roast"))
    }
}
