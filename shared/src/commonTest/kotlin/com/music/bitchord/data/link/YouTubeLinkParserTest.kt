package com.music.bitchord.data.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeLinkParserTest {

    @Test
    fun parsesStandardWatchAndShortUrls() {
        val watch = YouTubeLinkParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1m30s")
        assertNotNull(watch)
        assertEquals(YouTubeLinkKind.TRACK, watch.kind)
        assertEquals("dQw4w9WgXcQ", watch.videoId)
        assertEquals(90L, watch.startSeconds)

        val shortUrl = YouTubeLinkParser.parse("https://youtu.be/dQw4w9WgXcQ?t=45")
        assertNotNull(shortUrl)
        assertEquals(YouTubeLinkKind.TRACK, shortUrl.kind)
        assertEquals("dQw4w9WgXcQ", shortUrl.videoId)
        assertEquals(45L, shortUrl.startSeconds)
    }

    @Test
    fun parsesMusicShortsLiveAndEmbedUrls() {
        val music = YouTubeLinkParser.parse("https://music.youtube.com/watch?v=abcDEF12345&list=PL12345")
        assertNotNull(music)
        assertEquals(YouTubeLinkKind.TRACK, music.kind)
        assertEquals("abcDEF12345", music.videoId)
        assertEquals("VLPL12345", music.playlistBrowseId)

        val shorts = YouTubeLinkParser.parse("https://youtube.com/shorts/shortId1234")
        assertNotNull(shorts)
        assertEquals(YouTubeLinkKind.SHORTS, shorts.kind)
        assertEquals("shortId1234", shorts.videoId)

        val live = YouTubeLinkParser.parse("https://www.youtube.com/live/liveId67890?t=2m5s")
        assertNotNull(live)
        assertEquals(YouTubeLinkKind.LIVE, live.kind)
        assertEquals("liveId67890", live.videoId)
        assertEquals(125L, live.startSeconds)

        val embed = YouTubeLinkParser.parse("https://www.youtube.com/embed/embId123456#t=1h2m3s")
        assertNotNull(embed)
        assertEquals("embId123456", embed.videoId)
        assertEquals(3723L, embed.startSeconds)
    }

    @Test
    fun parsesPlaylistsBrowseChannelsAndSearchLinks() {
        val playlist = YouTubeLinkParser.parse("https://youtube.com/playlist?list=PLabcdef123")
        assertNotNull(playlist)
        assertEquals(YouTubeLinkKind.PAGE, playlist.kind)
        assertEquals("VLPLabcdef123", playlist.browseId)

        val channel = YouTubeLinkParser.parse("https://youtube.com/channel/UC123456789")
        assertNotNull(channel)
        assertEquals(YouTubeLinkKind.PAGE, channel.kind)
        assertEquals("UC123456789", channel.browseId)

        val handle = YouTubeLinkParser.parse("https://youtube.com/@DaftPunk")
        assertNotNull(handle)
        assertEquals(YouTubeLinkKind.SEARCH, handle.kind)
        assertEquals("@DaftPunk", handle.searchQuery)

        val search = YouTubeLinkParser.parse("https://www.youtube.com/results?search_query=lofi+beats")
        assertNotNull(search)
        assertEquals(YouTubeLinkKind.SEARCH, search.kind)
        assertEquals("lofi beats", search.searchQuery)
    }

    @Test
    fun parsesLinksInsideShareTextAndBareDomains() {
        val shared = YouTubeLinkParser.parse("Listen to this track!\nhttps://youtu.be/xyz98765432?si=track&t=75.")
        assertNotNull(shared)
        assertEquals("xyz98765432", shared.videoId)
        assertEquals(75L, shared.startSeconds)

        val bare = YouTubeLinkParser.parse("youtu.be/xyz98765432?t=10")
        assertNotNull(bare)
        assertEquals("xyz98765432", bare.videoId)
        assertEquals(10L, bare.startSeconds)
    }

    @Test
    fun rejectsPlainQueriesAndNonYouTubeUrls() {
        assertFalse(YouTubeLinkParser.looksLikeYouTubeLink("midnight city m83"))
        assertNull(YouTubeLinkParser.parse("https://open.spotify.com/track/123456"))
        assertNull(YouTubeLinkParser.parse("https://youtube.com/watch"))
    }

    @Test
    fun formatsPositiveTimestampsOnly() {
        assertNull(YouTubeLinkParser.formatTimestamp(0L))
        assertEquals("1:30", YouTubeLinkParser.formatTimestamp(90L))
        assertEquals("1:02:03", YouTubeLinkParser.formatTimestamp(3723L))
    }

    @Test
    fun parsesChaptersFromDescriptionTimestamps() {
        val description = """
            Some intro note without a timestamp
            0:00 Intro
            - 02:15 - Building the synth
            [12:30] Deep Dive: Mix Bus
            • 1:02:03 Finale & Outro
            02:15 Duplicate second ignored
        """.trimIndent()

        val chapters = YouTubeLinkParser.parseChaptersFromDescription(description)
        assertEquals(4, chapters.size)
        assertEquals(VideoChapter("Intro", 0L, "0:00"), chapters[0])
        assertEquals(VideoChapter("Building the synth", 135L, "2:15"), chapters[1])
        assertEquals(VideoChapter("Deep Dive: Mix Bus", 750L, "12:30"), chapters[2])
        assertEquals(VideoChapter("Finale & Outro", 3723L, "1:02:03"), chapters[3])
    }

    @Test
    fun formatsChapterTimesForZeroMinutesAndHours() {
        assertEquals("0:00", YouTubeLinkParser.formatChapterTime(0L))
        assertEquals("1:15", YouTubeLinkParser.formatChapterTime(75L))
        assertEquals("1:01:05", YouTubeLinkParser.formatChapterTime(3665L))
    }
}
