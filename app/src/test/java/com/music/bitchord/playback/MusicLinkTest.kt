package com.music.bitchord.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicLinkTest {

    @Test
    fun parsesYouTubeVideoTimestampAndPlaylist() {
        val req = MusicLink.parseText("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL999&t=1m45s")
        assertEquals(
            LinkRequest.Track(
                videoId = "dQw4w9WgXcQ",
                startPositionMs = 105_000L,
                playlistBrowseId = "VLPL999",
                isShort = false,
                isLive = false,
            ),
            req,
        )
    }

    @Test
    fun parsesShortsAndLiveStreams() {
        val shortReq = MusicLink.parseText("https://youtube.com/shorts/shortVideo99?t=5")
        assertEquals(
            LinkRequest.Track(
                videoId = "shortVideo99",
                startPositionMs = 5_000L,
                playlistBrowseId = null,
                isShort = true,
                isLive = false,
            ),
            shortReq,
        )

        val liveReq = MusicLink.parseText("https://www.youtube.com/live/liveVideo77")
        assertEquals(
            LinkRequest.Track(
                videoId = "liveVideo77",
                startPositionMs = 0L,
                playlistBrowseId = null,
                isShort = false,
                isLive = true,
            ),
            liveReq,
        )
    }

    @Test
    fun parsesPagesSearchAndRejectsUnknownUrls() {
        assertEquals(
            LinkRequest.Page("VLOLAK5uy_abc"),
            MusicLink.parseText("https://music.youtube.com/playlist?list=OLAK5uy_abc"),
        )
        assertEquals(
            LinkRequest.Search("ambient piano", play = false),
            MusicLink.parseText("https://music.youtube.com/search?q=ambient+piano"),
        )
        assertNull(MusicLink.parseText("https://example.com/watch?v=123456"))
    }

    @Test
    fun parsesShareSheetTextWrappingUrl() {
        val req = MusicLink.parseText("Listen to this: https://youtu.be/dQw4w9WgXcQ?t=30")
        assertEquals(
            LinkRequest.Track(
                videoId = "dQw4w9WgXcQ",
                startPositionMs = 30_000L,
                playlistBrowseId = null,
                isShort = false,
                isLive = false,
            ),
            req,
        )
    }
}
