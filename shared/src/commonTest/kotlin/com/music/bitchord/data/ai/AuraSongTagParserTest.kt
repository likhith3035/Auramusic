package com.music.bitchord.data.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuraSongTagParserTest {

    @Test
    fun testExtractsSongTagsFromAiResponse() {
        val text = """
            Here are two songs tailored for your midnight mood:
            [SONG: Chuttamalle | Anirudh Ravichander, Shilpa Rao | Breezy coastal melody]
            [SONG: Samajavaragamana | Sid Sriram, Thaman S | Pure acoustic euphoria]
            Enjoy these tracks!
            [ACTION: OPEN_URL:https://github.com/likhith3035 | GitHub Profile]
            [ACTION: OPEN_EQ | Open Studio EQ]
        """.trimIndent()

        val parsed = AuraSongTagParser.parse(text)
        assertEquals(2, parsed.recommendedSongs.size)
        assertEquals("Chuttamalle", parsed.recommendedSongs[0].title)
        assertEquals("Anirudh Ravichander, Shilpa Rao", parsed.recommendedSongs[0].artist)
        assertEquals("Breezy coastal melody", parsed.recommendedSongs[0].reason)

        assertEquals("Samajavaragamana", parsed.recommendedSongs[1].title)
        assertEquals("Sid Sriram, Thaman S", parsed.recommendedSongs[1].artist)

        assertEquals(2, parsed.actionChips.size)
        assertEquals("OPEN_URL", parsed.actionChips[0].type)
        assertEquals("https://github.com/likhith3035", parsed.actionChips[0].payload)
        assertEquals("GitHub Profile", parsed.actionChips[0].label)
        assertEquals("OPEN_EQ", parsed.actionChips[1].type)

        assertTrue(parsed.cleanText.contains("Here are two songs"))
        assertTrue(parsed.cleanText.contains("Enjoy these tracks!"))
        assertTrue(!parsed.cleanText.contains("[SONG:"))
        assertTrue(!parsed.cleanText.contains("[ACTION:"))
    }

    @Test
    fun testOrphanBulletRemoval() {
        val raw = """
            Top matches for your lyrics:
            - [SONG: Kesariya | Pritam | 98% Match]
            * [SONG: Samajavaragamana | Sid Sriram | 88% Match]
            1. [SONG: Butta Bomma | Armaan Malik | 76% Match]
            Enjoy listening!
        """.trimIndent()

        val parsed = AuraSongTagParser.parse(raw)
        assertEquals(3, parsed.recommendedSongs.size)
        assertTrue(parsed.cleanText.contains("Top matches for your lyrics:"))
        assertTrue(parsed.cleanText.contains("Enjoy listening!"))
        assertTrue(!parsed.cleanText.contains("-"))
        assertTrue(!parsed.cleanText.contains("*"))
        assertTrue(!parsed.cleanText.contains("1."))
    }
}
