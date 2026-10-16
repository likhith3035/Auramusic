package com.music.bitchord.data.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class MusicDirectorClassifierTest {

    @Test
    fun testClassifiesSingerAndMusicDirector() {
        val result = MusicDirectorClassifier.classify(
            artistString = "Arijit Singh, Pritam",
            creditedArtists = listOf("Arijit Singh", "Pritam")
        )
        assertEquals("Arijit Singh", result.singer)
        assertEquals("Pritam", result.musicDirector)
    }

    @Test
    fun testReversedOrderFindsDirector() {
        val result = MusicDirectorClassifier.classify(
            artistString = "Anirudh Ravichander, Dhanush",
            creditedArtists = emptyList()
        )
        assertEquals("Dhanush", result.singer)
        assertEquals("Anirudh Ravichander", result.musicDirector)
    }

    @Test
    fun testSoloArtist() {
        val result = MusicDirectorClassifier.classify(
            artistString = "Taylor Swift",
            creditedArtists = emptyList()
        )
        assertEquals("Taylor Swift", result.singer)
        assertEquals("Taylor Swift", result.musicDirector)
    }
}
