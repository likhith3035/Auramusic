package com.music.bitchord.playback

import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartShuffleEngineTest {

    private fun testSong(id: String, tier: QueueTier = QueueTier.CONTEXT, isSmart: Boolean = false): Song {
        return Song(
            videoId = id,
            title = "Title $id",
            artist = "Artist $id",
            thumbnailUrl = null,
            queueTier = tier,
            isSmartRecommendation = isSmart,
        )
    }

    @Test
    fun testInterleaveRecommendationsCadence() {
        val queue = listOf(
            testSong("1"),
            testSong("2"),
            testSong("3"),
            testSong("4"),
            testSong("5"),
            testSong("6"),
            testSong("7"),
        )
        val recs = listOf(
            testSong("recA"),
            testSong("recB"),
            testSong("recC"),
        )

        // Currently at index 0 ("1" is playing).
        // Upcoming is "2", "3", "4", "5", "6", "7".
        // With cadence 3, recA is placed after 3rd upcoming ("4"), recB after 6th upcoming ("7").
        val result = SmartShuffleEngine.interleaveRecommendations(queue, currentIndex = 0, recommendations = recs)

        assertEquals(9, result.size)
        assertEquals("1", result[0].videoId)
        assertFalse(result[0].isSmartRecommendation)

        // Upcoming sequence:
        assertEquals("2", result[1].videoId)
        assertEquals("3", result[2].videoId)
        assertEquals("4", result[3].videoId)
        assertEquals("recA", result[4].videoId)
        assertTrue(result[4].isSmartRecommendation)

        assertEquals("5", result[5].videoId)
        assertEquals("6", result[6].videoId)
        assertEquals("7", result[7].videoId)
        assertEquals("recB", result[8].videoId)
        assertTrue(result[8].isSmartRecommendation)
    }

    @Test
    fun testInterleaveDoesNotDuplicateExistingQueueTracks() {
        val queue = listOf(
            testSong("1"),
            testSong("2"),
            testSong("3"),
            testSong("4"),
        )
        // rec contains "2" which is already in queue, and "freshRec"
        val recs = listOf(
            testSong("2"),
            testSong("freshRec"),
        )

        val result = SmartShuffleEngine.interleaveRecommendations(queue, currentIndex = 0, recommendations = recs)

        val ids = result.map { it.videoId }
        assertEquals(listOf("1", "2", "3", "4", "freshRec"), ids)
        val smartSongs = result.filter { it.isSmartRecommendation }
        assertEquals(1, smartSongs.size)
        assertEquals("freshRec", smartSongs[0].videoId)
    }

    @Test
    fun testStripRecommendationsRemovesInjectedTracksOnly() {
        val originalQueue = listOf(
            testSong("1"),
            testSong("2"),
            testSong("3"),
            testSong("4"),
        )
        val recs = listOf(testSong("rec1"), testSong("rec2"))
        val interleaved = SmartShuffleEngine.interleaveRecommendations(originalQueue, 0, recs)

        assertTrue(interleaved.any { it.isSmartRecommendation })

        val stripped = SmartShuffleEngine.stripRecommendations(interleaved)
        assertEquals(originalQueue.map { it.videoId }, stripped.map { it.videoId })
        assertFalse(stripped.any { it.isSmartRecommendation })
    }

    @Test
    fun testEmptyQueueOrRecommendationsReturnsUnchanged() {
        val emptyQueue = emptyList<Song>()
        val recs = listOf(testSong("r1"))
        assertEquals(emptyQueue, SmartShuffleEngine.interleaveRecommendations(emptyQueue, 0, recs))

        val queue = listOf(testSong("1"), testSong("2"))
        assertEquals(queue, SmartShuffleEngine.interleaveRecommendations(queue, 0, emptyList()))
    }
}
