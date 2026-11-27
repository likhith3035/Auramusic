package com.music.bitchord.data.link

import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.Song
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeResumeStoreTest {
    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        YouTubeResumeStore.resetForTests()
        tempDir = createTempDirectory("youtube-resume-store-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        YouTubeResumeStore.resetForTests()
        tempDir.deleteRecursively()
    }

    @Test
    fun ignoresShortVideosUnderTenMinutes() {
        YouTubeResumeStore.init(tempDir)
        YouTubeResumeStore.recordProgress(
            videoId = "shortVid123",
            positionMs = 120_000L,
            durationMs = 5L * 60L * 1000L,
            forceFlush = true,
        )

        assertNull(YouTubeResumeStore.savedPositionMs("shortVid123"))
    }

    @Test
    fun persistsAndReloadsProgressForLongVideos() {
        YouTubeResumeStore.init(tempDir)
        val durationMs = 25L * 60L * 1000L
        YouTubeResumeStore.recordProgress(
            videoId = "longVid1234",
            positionMs = 420_000L,
            durationMs = durationMs,
            forceFlush = true,
        )
        assertEquals(420_000L, YouTubeResumeStore.savedPositionMs("longVid1234"))

        YouTubeResumeStore.resetForTests()
        YouTubeResumeStore.init(tempDir)
        assertEquals(420_000L, YouTubeResumeStore.savedPositionMs("longVid1234"))
    }

    @Test
    fun ignoresEarlySecondsAndClearsNearCompletion() {
        YouTubeResumeStore.init(tempDir)
        val durationMs = 20L * 60L * 1000L

        YouTubeResumeStore.recordProgress(
            videoId = "longVid5678",
            positionMs = 10_000L,
            durationMs = durationMs,
            forceFlush = true,
        )
        assertNull(YouTubeResumeStore.savedPositionMs("longVid5678"))

        YouTubeResumeStore.recordProgress(
            videoId = "longVid5678",
            positionMs = 300_000L,
            durationMs = durationMs,
            forceFlush = true,
        )
        assertEquals(300_000L, YouTubeResumeStore.savedPositionMs("longVid5678"))

        // Past 96% completion should clear the stored resume position.
        YouTubeResumeStore.recordProgress(
            videoId = "longVid5678",
            positionMs = (durationMs * 0.97).toLong(),
            durationMs = durationMs,
            forceFlush = true,
        )
        assertNull(YouTubeResumeStore.savedPositionMs("longVid5678"))
    }

    @Test
    fun detectsEligibleLongVideosAndStandaloneSharedMedia() {
        val shortTrack = Song(
            videoId = "track000001",
            title = "Short Music Video",
            artist = "Artist",
            thumbnailUrl = null,
            durationText = "3:45",
            isVideo = true,
            playbackSourceType = PlaybackSourceType.SHARED_LINK,
        )
        val longMusicAudio = Song(
            videoId = "symphony001",
            title = "Symphony Live Audio",
            artist = "Orchestra",
            thumbnailUrl = null,
            durationText = "45:10",
            isVideo = false,
            isVideoOrigin = false,
            playbackSourceType = PlaybackSourceType.BROWSE,
        )
        val longPodcastVideo = Song(
            videoId = "pod00000001",
            title = "Deep Dive Episode",
            artist = "Host",
            thumbnailUrl = null,
            durationText = "45:10",
            isVideo = true,
            isVideoOrigin = true,
            playbackSourceType = PlaybackSourceType.SHARED_LINK,
        )
        val longSearchVideo = longPodcastVideo.copy(playbackSourceType = PlaybackSourceType.SEARCH)

        assertFalse(YouTubeResumeStore.isEligibleLongVideo(shortTrack))
        assertFalse(YouTubeResumeStore.isStandaloneSharedMedia(shortTrack))
        assertFalse(YouTubeResumeStore.isEligibleLongVideo(longMusicAudio))
        assertFalse(YouTubeResumeStore.isStandaloneSharedMedia(longMusicAudio))

        assertTrue(YouTubeResumeStore.isEligibleLongVideo(longPodcastVideo))
        assertTrue(YouTubeResumeStore.isStandaloneSharedMedia(longPodcastVideo))
        assertTrue(YouTubeResumeStore.isEligibleLongVideo(longSearchVideo))
        assertFalse(YouTubeResumeStore.isStandaloneSharedMedia(longSearchVideo))
        assertTrue(
            YouTubeResumeStore.isStandaloneSharedMedia(
                shortTrack.copy(durationText = null),
                knownDurationMs = 15L * 60L * 1000L,
            ),
        )
    }
}
