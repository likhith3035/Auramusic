package com.music.bitchord.data.taste

import com.music.bitchord.data.model.ArtistPage
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicTasteTest {

    private lateinit var tempDir: java.io.File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("aura_taste_test").toFile()
        MusicTasteStore.resetForTests()
        MusicTasteStore.init(tempDir)
    }

    @AfterTest
    fun teardown() {
        MusicTasteStore.resetForTests()
        tempDir.deleteRecursively()
    }

    @Test
    fun storePersistsAndReloadsLanguagesAndCreators() {
        val dsp = TasteArtistPick(
            browseId = "UC_DSP_TEST",
            name = "Devi Sri Prasad",
            thumbnailUrl = "https://example.com/dsp.jpg",
            role = ArtistRole.MUSIC_DIRECTOR,
            languageId = "telugu",
        )
        val arijit = TasteArtistPick(
            browseId = "UC_ARIJIT_TEST",
            name = "Arijit Singh",
            thumbnailUrl = "https://example.com/arijit.jpg",
            role = ArtistRole.SINGER,
            languageId = "hindi",
        )

        MusicTasteStore.saveSelection(
            languageIds = listOf("telugu", "hindi", "telugu"),
            artists = listOf(dsp, arijit, dsp),
            markConfigured = true,
            autoSubscribeOnSignIn = false,
        )

        val inMemory = MusicTasteStore.preferences.value
        assertTrue(inMemory.tasteConfigured)
        assertFalse(inMemory.autoSubscribeOnSignIn)
        assertEquals(listOf("telugu", "hindi"), inMemory.selectedLanguageIds)
        assertEquals(2, inMemory.selectedArtists.size)

        // Simulate fresh app restart
        MusicTasteStore.resetForTests()
        MusicTasteStore.init(tempDir)

        val reloaded = MusicTasteStore.preferences.value
        assertEquals(listOf("telugu", "hindi"), reloaded.selectedLanguageIds)
        assertEquals(listOf("Devi Sri Prasad", "Arijit Singh"), reloaded.selectedArtists.map { it.name })
        assertEquals("2 languages · 2 artists", MusicTasteStore.summaryLabel())
    }

    @Test
    fun catalogFiltersMusicDirectorsCleanlyAndDeduplicatesBilingualSeeds() {
        val teluguDirectors = MusicTasteCatalog.seedsForLanguages(
            languageIds = listOf("telugu"),
            role = ArtistRole.MUSIC_DIRECTOR,
        )
        assertTrue(teluguDirectors.any { it.displayName == "Devi Sri Prasad" })
        assertTrue(teluguDirectors.any { it.displayName == "Thaman S" })
        assertTrue(teluguDirectors.none { it.role == ArtistRole.SINGER })

        val mergedBilingual = MusicTasteCatalog.seedsForLanguages(
            languageIds = listOf("telugu", "tamil"),
            role = ArtistRole.MUSIC_DIRECTOR,
        )
        val anirudhCount = mergedBilingual.count { it.displayName == "Anirudh Ravichander" }
        assertEquals(1, anirudhCount)
        assertTrue(MusicTasteCatalog.CURATED_ARTISTS.all { !it.knownBrowseId.isNullOrBlank() })
    }

    @Test
    fun extractRelatedArtistsReadsCarouselAndFiltersNonChannelOrSelfItems() {
        val source = TasteArtistPick(
            browseId = "UC_THAMAN",
            name = "Thaman S",
            role = ArtistRole.MUSIC_DIRECTOR,
            languageId = "telugu",
        )
        val page = ArtistPage(
            songs = emptyList(),
            moreSongsBrowseId = null,
            sections = listOf(
                HomeShelf(
                    title = "Albums",
                    items = listOf(
                        ShelfItem(
                            title = "Guntur Kaaram",
                            subtitle = "Album",
                            thumbnailUrl = null,
                            videoId = null,
                            browseId = "MPRE_ALBUM_1",
                        ),
                    ),
                ),
                HomeShelf(
                    title = "Fans might also like",
                    items = listOf(
                        ShelfItem(
                            title = "Thaman S",
                            subtitle = "Artist",
                            thumbnailUrl = null,
                            videoId = null,
                            browseId = "UC_THAMAN",
                        ),
                        ShelfItem(
                            title = "Devi Sri Prasad",
                            subtitle = "Artist",
                            thumbnailUrl = "https://example.com/dsp.jpg",
                            videoId = null,
                            browseId = "UC_DSP",
                        ),
                        ShelfItem(
                            title = "Anirudh Ravichander",
                            subtitle = "Artist",
                            thumbnailUrl = "https://example.com/ani.jpg",
                            videoId = null,
                            browseId = "UC_ANI",
                        ),
                        ShelfItem(
                            title = "Kurchi Madathapetti",
                            subtitle = "Song",
                            thumbnailUrl = null,
                            videoId = "VIDEO_1",
                            browseId = null,
                        ),
                    ),
                ),
            ),
            thumbnailUrl = null,
            name = "Thaman S",
        )

        val related = MusicTasteRepository.extractRelatedArtistsFromPage(page, source)
        assertEquals(listOf("Devi Sri Prasad", "Anirudh Ravichander"), related.map { it.name })
        assertTrue(related.all { it.isRelatedSuggestion && it.relatedToArtistName == "Thaman S" })
        assertEquals(ArtistRole.MUSIC_DIRECTOR, related.first().role)
    }

    @Test
    fun interleaveSongsDistributesEvenlyWithoutDuplicates() {
        val s1 = Song("v1", "T1", "A1", null)
        val s2 = Song("v2", "T2", "A1", null)
        val s3 = Song("v3", "T3", "A2", null)
        val dup = Song("v1", "T1 dup", "A2", null)
        val s4 = Song("v4", "T4", "A2", null)

        val mixed = MusicTasteRepository.interleaveSongs(
            batches = listOf(listOf(s1, s2), listOf(dup, s3, s4)),
            maxTotal = 10,
        )
        assertEquals(listOf("v1", "v3", "v2", "v4"), mixed.map { it.videoId })
    }

    @Test
    fun audienceAndBrowseMatchPrefersOfficialHighAudienceArtistAndRejectsPlaceholders() {
        val fakeClone = com.music.bitchord.data.model.BrowseItem(
            browseId = "UC_CLONE",
            title = "Mithoon",
            subtitle = "Artist • 122 subscribers",
            thumbnailUrl = "https://www.gstatic.com/youtube/media/ytm/images/artist_avatar@1200.png",
            type = com.music.bitchord.data.model.BrowseType.ARTIST,
        )
        val official = com.music.bitchord.data.model.BrowseItem(
            browseId = "UC_OFFICIAL",
            title = "Mithoon",
            subtitle = "Artist • 277K subscribers",
            thumbnailUrl = "https://yt3.googleusercontent.com/real=w120-h120-l90-rj",
            type = com.music.bitchord.data.model.BrowseType.ARTIST,
        )
        val unrelated = com.music.bitchord.data.model.BrowseItem(
            browseId = "UC_OTHER",
            title = "CL (from 2NE1)",
            subtitle = "Artist • 5.68M monthly audience",
            thumbnailUrl = "https://yt3.googleusercontent.com/cl=w120-h120-l90-rj",
            type = com.music.bitchord.data.model.BrowseType.ARTIST,
        )
        val picked = MusicTasteRepository.bestArtistBrowseMatch(
            queryName = "Mithoon",
            rows = listOf(
                com.music.bitchord.data.model.SearchResult.Browse(fakeClone),
                com.music.bitchord.data.model.SearchResult.Browse(official),
            ),
        )
        assertEquals("UC_OFFICIAL", picked?.browseId)

        val rejectedUnrelated = MusicTasteRepository.bestArtistBrowseMatch(
            queryName = "Teddy Park",
            rows = listOf(com.music.bitchord.data.model.SearchResult.Browse(unrelated)),
        )
        assertEquals(null, rejectedUnrelated)
    }
}
