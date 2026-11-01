package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuraNeuralDjEngineTest {

    @Test
    fun testSongAnalysis() {
        val song = Song(
            videoId = "devara_fear_song",
            title = "Fear Song",
            artist = "Anirudh Ravichander",
            thumbnailUrl = null,
        )

        val profile = AuraNeuralDjEngine.analyzeSong(song)
        assertNotNull(profile)
        assertTrue(profile.bpm in 85..138)
        assertTrue(profile.key.number in 1..12)
        assertTrue(profile.energyScore in 0.0f..1.0f)
    }

    @Test
    fun testCrossfadeCalculation() {
        val current = SongDjAcousticProfile(
            song = Song("1", "Song A", "Artist", null),
            bpm = 120,
            key = CamelotKey(8, 'A'),
            energyScore = 0.8f,
        )

        val closeBpm = SongDjAcousticProfile(
            song = Song("2", "Song B", "Artist", null),
            bpm = 122,
            key = CamelotKey(8, 'A'),
            energyScore = 0.8f,
        )

        val farBpm = SongDjAcousticProfile(
            song = Song("3", "Song C", "Artist", null),
            bpm = 90,
            key = CamelotKey(4, 'B'),
            energyScore = 0.5f,
        )

        val crossfade1 = AuraNeuralDjEngine.calculateOptimalCrossfade(current, closeBpm)
        val crossfade2 = AuraNeuralDjEngine.calculateOptimalCrossfade(current, farBpm)

        assertEquals(6.5f, crossfade1)
        assertEquals(2.5f, crossfade2)
    }
}
