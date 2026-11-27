package com.music.bitchord.data.link

import com.music.bitchord.data.model.Song
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeLinkResolverTest {

    @Test
    fun relatedTracksSkippedForLongVideosShortsAndLiveStreams() = runBlocking {
        val longPodcast = ResolvedYouTubeLink.Track(
            link = YouTubeLinkParser.parse("https://youtu.be/pod12345678")!!,
            videoSong = Song(
                videoId = "pod12345678",
                title = "Two Hour Interview",
                artist = "Podcast",
                thumbnailUrl = null,
                durationText = "1:45:00",
                isVideo = true,
            ),
            resolvedFromUniversalFallback = true,
        )
        assertTrue(longPodcast.isLongNonMusicVideo)
        assertTrue(YouTubeLinkResolver.findRelatedTracks(longPodcast).isEmpty())

        val shortTrack = ResolvedYouTubeLink.Track(
            link = YouTubeLinkParser.parse("https://youtube.com/shorts/shortId1234")!!,
            videoSong = Song(
                videoId = "shortId1234",
                title = "Quick Clip",
                artist = "Creator",
                thumbnailUrl = null,
                durationText = "0:45",
                isVideo = true,
            ),
            isShort = true,
        )
        assertNull(YouTubeLinkResolver.findStudioMatch(shortTrack))
        assertTrue(YouTubeLinkResolver.findRelatedTracks(shortTrack).isEmpty())

        val liveStream = ResolvedYouTubeLink.Track(
            link = YouTubeLinkParser.parse("https://youtube.com/live/liveId12345")!!,
            videoSong = Song(
                videoId = "liveId12345",
                title = "Live Radio",
                artist = "Channel",
                thumbnailUrl = null,
                isVideo = true,
            ),
            isLive = true,
        )
        assertFalse(liveStream.isLongNonMusicVideo)
        assertTrue(YouTubeLinkResolver.findRelatedTracks(liveStream).isEmpty())
    }
}
