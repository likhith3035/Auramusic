package com.music.bitchord.ui.components

import com.music.bitchord.data.link.ParsedYouTubeLink
import com.music.bitchord.data.link.ResolvedYouTubeLink
import com.music.bitchord.data.link.YouTubeLinkKind
import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

class DirectLinkUiStateTest {

    @Test
    fun readyStateSelectsStudioMatchOrOriginalVideoAudio() {
        val videoSong = Song(
            videoId = "video123",
            title = "Song (Official Video)",
            artist = "Artist",
            thumbnailUrl = null,
            isVideo = true,
            isVideoOrigin = true,
        )
        val studioSong = Song(
            videoId = "audio456",
            title = "Song",
            artist = "Artist",
            thumbnailUrl = null,
            isVideo = false,
            isVideoOrigin = false,
        )
        val parsed = ParsedYouTubeLink(
            kind = YouTubeLinkKind.TRACK,
            rawUrl = "https://youtu.be/video123",
            canonicalUrl = "https://www.youtube.com/watch?v=video123",
            videoId = "video123",
        )
        val resolved = ResolvedYouTubeLink.Track(
            link = parsed,
            videoSong = videoSong,
            studioMatch = studioSong,
        )
        val studioSelected = DirectLinkUiState.Ready(resolved, useStudioMatch = true)
        assertEquals(studioSong, studioSelected.activeSong())

        val originalSelected = studioSelected.copy(useStudioMatch = false)
        assertEquals(videoSong, originalSelected.activeSong())

        val noStudioMatch = DirectLinkUiState.Ready(resolved.copy(studioMatch = null), useStudioMatch = true)
        assertEquals(videoSong, noStudioMatch.activeSong())
    }
}
