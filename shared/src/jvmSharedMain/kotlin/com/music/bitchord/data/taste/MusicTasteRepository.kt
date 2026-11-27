package com.music.bitchord.data.taste

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.model.ArtistPage
import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolved audio preview snippet for an artist or music director inside the taste picker.
 */
data class ArtistPreviewTrack(
    val artistBrowseId: String,
    val song: Song,
    val streamUrl: String,
    val headers: Map<String, String>,
)

/**
 * Live YouTube Music integration and personalization helpers for music languages,
 * singers, and music directors.
 */
object MusicTasteRepository {

    private val resolutionSemaphore = Semaphore(permits = 6)

    private val resolvedSeedCache = ConcurrentHashMap<String, TasteArtistPick>()
    private val relatedCache = ConcurrentHashMap<String, List<TasteArtistPick>>()
    private val artistSongsCache = ConcurrentHashMap<String, List<Song>>()
    private val languagePlaylistCache = ConcurrentHashMap<String, List<ShelfItem>>()

    /**
     * Resolves a lead portrait URL for each language card using curated seeds.
     */
    suspend fun loadLanguagePortraits(
        languages: List<MusicLanguageOption> = MusicTasteCatalog.LANGUAGES,
    ): Map<String, String?> = coroutineScope {
        languages.map { language ->
            async {
                val leadSeed = MusicTasteCatalog.CURATED_ARTISTS.firstOrNull {
                    it.languageId == language.id
                }
                val resolved = leadSeed?.let { resolveCuratedSeed(it) }
                language.id to resolved?.thumbnailUrl
            }
        }.awaitAll().toMap()
    }

    /**
     * Loads concrete [TasteArtistPick] entries for [languageIds], preserving live channel IDs
     * and high-res portraits when online and falling back cleanly when offline.
     */
    suspend fun loadArtistsForLanguages(
        languageIds: List<String>,
        roleFilter: ArtistRole = ArtistRole.ALL,
    ): List<TasteArtistPick> = coroutineScope {
        val seeds = MusicTasteCatalog.seedsForLanguages(languageIds, roleFilter)
        seeds.map { seed ->
            async { resolveCuratedSeed(seed) }
        }.awaitAll()
    }

    /**
     * Resolves a single curated seed to its real YouTube Music artist channel + portrait.
     */
    suspend fun resolveCuratedSeed(seed: CuratedArtistSeed): TasteArtistPick {
        val cacheKey = seedCacheKey(seed)
        resolvedSeedCache[cacheKey]?.let { return it }

        val resolved = resolutionSemaphore.withPermit {
            resolvedSeedCache[cacheKey]?.let { return@withPermit it }
            val fallbackId = seed.knownBrowseId ?: MusicTasteCatalog.seedBrowseId(seed.languageId, seed.displayName)
            val fallback = TasteArtistPick(
                browseId = fallbackId,
                name = seed.displayName,
                thumbnailUrl = null,
                role = seed.role,
                languageId = seed.languageId,
            )
            val rows = YtMusicRepository.search(seed.query, SearchFilter.ARTISTS).getOrNull().orEmpty()
            val match = bestArtistBrowseMatch(seed.displayName, rows, seed.knownBrowseId)
            val cleanThumb = sanitizeArtistThumb(match?.thumbnailUrl)
                ?: if (seed.knownBrowseId != null) {
                    sanitizeArtistThumb(
                        YtMusicRepository.artistLanding(seed.knownBrowseId).getOrNull()?.thumbnailUrl,
                    )
                } else {
                    null
                }
            if (match == null && seed.knownBrowseId == null) {
                return@withPermit fallback
            }
            TasteArtistPick(
                browseId = match?.browseId ?: fallbackId,
                name = seed.displayName,
                thumbnailUrl = cleanThumb,
                role = seed.role,
                languageId = seed.languageId,
            ).also {
                if (it.thumbnailUrl != null) resolvedSeedCache[cacheKey] = it
            }
        }
        return resolved
    }

    /**
     * Searches live YouTube Music artists/composers matching [query].
     */
    suspend fun searchArtists(
        query: String,
        preferredLanguageId: String = "english",
        limit: Int = 24,
    ): List<TasteArtistPick> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val localMatches = MusicTasteCatalog.CURATED_ARTISTS
            .filter {
                it.displayName.contains(trimmed, ignoreCase = true) ||
                    it.query.contains(trimmed, ignoreCase = true)
            }
            .distinctBy { normalizeKey(it.displayName) }

        val liveRows = YtMusicRepository.search(trimmed, SearchFilter.ARTISTS)
            .getOrNull()
            .orEmpty()
            .mapNotNull { (it as? SearchResult.Browse)?.item }
            .filter { it.type == BrowseType.ARTIST && it.browseId.isNotBlank() }

        val result = ArrayList<TasteArtistPick>()
        val seenKeys = HashSet<String>()

        for (item in liveRows) {
            val norm = normalizeKey(item.title)
            if (!seenKeys.add(norm)) continue
            val seedMatch = localMatches.firstOrNull { normalizeKey(it.displayName) == norm }
                ?: MusicTasteCatalog.CURATED_ARTISTS.firstOrNull { normalizeKey(it.displayName) == norm }
            val pick = TasteArtistPick(
                browseId = item.browseId,
                name = item.title,
                thumbnailUrl = sanitizeArtistThumb(item.thumbnailUrl),
                role = seedMatch?.role ?: inferRoleFromQueryOrSubtitle(trimmed, item.subtitle),
                languageId = seedMatch?.languageId ?: preferredLanguageId,
            )
            result.add(pick)
            if (seedMatch != null) {
                resolvedSeedCache[seedCacheKey(seedMatch)] = pick
            }
            if (result.size >= limit) break
        }

        if (result.isEmpty()) {
            for (seed in localMatches.take(limit)) {
                val norm = normalizeKey(seed.displayName)
                if (seenKeys.add(norm)) {
                    result.add(resolveCuratedSeed(seed))
                }
            }
        }
        return result
    }

    /**
     * Dynamically discovers similar / related singers & music directors when a listener picks [artist].
     */
    suspend fun loadRelatedArtists(
        artist: TasteArtistPick,
        excludeBrowseIds: Set<String> = emptySet(),
        excludeNames: Set<String> = emptySet(),
        limit: Int = 8,
    ): List<TasteArtistPick> {
        val resolvedChannelId = resolveChannelIdIfSeed(artist)
        val cacheKey = "${resolvedChannelId ?: artist.name}:${artist.languageId}"
        val cached = relatedCache[cacheKey]
        if (cached != null) {
            return filterRelated(cached, artist, excludeBrowseIds, excludeNames, limit)
        }

        val fetched = ArrayList<TasteArtistPick>()
        if (resolvedChannelId != null) {
            val page = YtMusicRepository.artistLanding(resolvedChannelId).getOrNull()
            if (page != null) {
                fetched += extractRelatedArtistsFromPage(page, artist)
            }
        }

        // Fill with same-language catalog peers if carousel results are sparse or offline.
        if (fetched.size < 4) {
            val sameLangSeeds = MusicTasteCatalog.seedsForLanguages(listOf(artist.languageId), ArtistRole.ALL)
                .filterNot { normalizeKey(it.displayName) == normalizeKey(artist.name) }
                .take(6)
            for (seed in sameLangSeeds) {
                val resolved = resolveCuratedSeed(seed)
                if (fetched.none { normalizeKey(it.name) == normalizeKey(resolved.name) }) {
                    fetched += resolved.copy(
                        isRelatedSuggestion = true,
                        relatedToArtistName = artist.name,
                    )
                }
            }
        }

        relatedCache[cacheKey] = fetched
        return filterRelated(fetched, artist, excludeBrowseIds, excludeNames, limit)
    }

    /**
     * Resolves a real streamable track preview for [artist] so users can sample their sound inside onboarding.
     */
    suspend fun loadArtistPreviewTrack(artist: TasteArtistPick): Result<ArtistPreviewTrack> = runCatching {
        val song = topSongsForArtist(artist, limit = 1).firstOrNull()
            ?: error("No preview track found for ${artist.name}")
        val resolvedUrl = StreamResolver.resolve(song.videoId)
        ArtistPreviewTrack(
            artistBrowseId = artist.browseId,
            song = song,
            streamUrl = resolvedUrl,
            headers = StreamResolver.mediaHeadersFor(resolvedUrl),
        )
    }

    /**
     * Builds Home feed shelves tailored directly to [prefs], even when signed out.
     */
    suspend fun buildPersonalizedHomeShelves(prefs: MusicTastePreferences): List<HomeShelf> = coroutineScope {
        if (!prefs.tasteConfigured && prefs.selectedArtists.isEmpty() && prefs.selectedLanguageIds.isEmpty()) {
            return@coroutineScope emptyList()
        }

        val shelves = ArrayList<HomeShelf>()
        val selectedArtists = prefs.selectedArtists.take(5)
        if (selectedArtists.isNotEmpty()) {
            val songBatches = selectedArtists.map { artist ->
                async { topSongsForArtist(artist, limit = 4) }
            }.awaitAll()

            val mixedSongs = interleaveSongs(songBatches, maxTotal = 16)
            if (mixedSongs.isNotEmpty()) {
                val namesPreview = selectedArtists.take(3).joinToString(", ") { it.name }
                shelves += HomeShelf(
                    title = "Picked from your taste",
                    subtitle = "Based on $namesPreview",
                    items = mixedSongs.map { song ->
                        ShelfItem(
                            title = song.title,
                            subtitle = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                            videoId = song.videoId,
                            browseId = song.artistId,
                            isVideo = false,
                        )
                    },
                    layout = ShelfLayout.CARDS,
                )
            }
        }

        val languages = prefs.selectedLanguageIds.mapNotNull { MusicTasteCatalog.languageById(it) }.take(2)
        val languageShelves = languages.map { lang ->
            async { buildLanguageSpotlightShelf(lang) }
        }.awaitAll().filterNotNull()
        shelves.addAll(languageShelves)

        shelves
    }

    /**
     * Builds quick picks tracks drawn from selected artists and languages.
     */
    suspend fun fetchTasteQuickPicks(
        prefs: MusicTastePreferences,
        excludeSongIds: Set<String> = emptySet(),
        limit: Int = 20,
    ): List<Song> = coroutineScope {
        if (!prefs.tasteConfigured && prefs.selectedArtists.isEmpty() && prefs.selectedLanguageIds.isEmpty()) {
            return@coroutineScope emptyList()
        }

        val seedBatches = ArrayList<List<Song>>()
        val artistSongs = prefs.selectedArtists.take(6).map { artist ->
            async { topSongsForArtist(artist, limit = 5) }
        }.awaitAll()
        seedBatches.addAll(artistSongs)

        if (artistSongs.flatten().size < limit && prefs.selectedLanguageIds.isNotEmpty()) {
            val langSongs = prefs.selectedLanguageIds.take(3).map { langId ->
                async {
                    val lang = MusicTasteCatalog.languageById(langId) ?: return@async emptyList()
                    YtMusicRepository.search("${lang.displayName} latest hit songs", SearchFilter.SONGS)
                        .getOrNull()
                        .orEmpty()
                        .mapNotNull { result ->
                            when (result) {
                                is SearchResult.TopTrack -> result.song
                                is SearchResult.Track -> result.song
                                is SearchResult.Browse -> null
                            }
                        }
                        .take(8)
                }
            }.awaitAll()
            seedBatches.addAll(langSongs)
        }

        val interleaved = interleaveSongs(seedBatches, maxTotal = limit * 2)
        interleaved.filterNot { it.videoId in excludeSongIds }.take(limit)
    }

    /**
     * Subscribes to picked artists on YouTube Music when signed in.
     * Returns the number of channels subscribed cleanly.
     */
    suspend fun subscribeToPickedArtists(artists: List<TasteArtistPick>): Int = coroutineScope {
        if (Innertube.cookie == null) return@coroutineScope 0
        val channelIds = artists.mapNotNull { pick ->
            resolveChannelIdIfSeed(pick)
        }.distinct()
        if (channelIds.isEmpty()) return@coroutineScope 0

        channelIds.map { id ->
            async {
                resolutionSemaphore.withPermit {
                    YtMusicRepository.setSubscribed(id, true).isSuccess
                }
            }
        }.awaitAll().count { it }
    }

    internal fun extractRelatedArtistsFromPage(
        page: ArtistPage,
        sourceArtist: TasteArtistPick,
    ): List<TasteArtistPick> {
        val candidates = ArrayList<TasteArtistPick>()
        val seen = HashSet<String>()

        val prioritizedSections = page.sections.sortedByDescending { shelf ->
            val lower = shelf.title.lowercase(Locale.ROOT)
            when {
                "similar" in lower || "might also like" in lower || "fans" in lower -> 2
                shelf.items.any { it.browseId?.startsWith("UC") == true && it.videoId == null } -> 1
                else -> 0
            }
        }

        for (shelf in prioritizedSections) {
            val sectionMatches = shelf.items.filter { item ->
                item.videoId == null &&
                    !item.browseId.isNullOrBlank() &&
                    item.browseId.startsWith("UC") &&
                    normalizeKey(item.title) != normalizeKey(sourceArtist.name)
            }
            for (item in sectionMatches) {
                val channelId = item.browseId ?: continue
                val norm = normalizeKey(item.title)
                if (!seen.add(norm)) continue
                candidates += TasteArtistPick(
                    browseId = channelId,
                    name = item.title,
                    thumbnailUrl = sanitizeArtistThumb(item.thumbnailUrl),
                    role = MusicTasteCatalog.inferRoleByName(item.title) ?: ArtistRole.SINGER,
                    languageId = sourceArtist.languageId,
                    isRelatedSuggestion = true,
                    relatedToArtistName = sourceArtist.name,
                )
            }
        }
        return candidates
    }

    internal fun interleaveSongs(batches: List<List<Song>>, maxTotal: Int): List<Song> {
        val result = ArrayList<Song>()
        val seenIds = HashSet<String>()
        val iterators = batches.map { it.iterator() }.toMutableList()
        while (iterators.isNotEmpty() && result.size < maxTotal) {
            val it = iterators.iterator()
            while (it.hasNext() && result.size < maxTotal) {
                val cursor = it.next()
                var picked: Song? = null
                while (cursor.hasNext()) {
                    val candidate = cursor.next()
                    if (seenIds.add(candidate.videoId)) {
                        picked = candidate
                        break
                    }
                }
                if (picked != null) {
                    result.add(picked)
                } else {
                    it.remove()
                }
            }
        }
        return result
    }

    private suspend fun topSongsForArtist(artist: TasteArtistPick, limit: Int): List<Song> {
        val channelId = resolveChannelIdIfSeed(artist)
        val cacheKey = channelId ?: normalizeKey(artist.name)
        artistSongsCache[cacheKey]?.let { return it.take(limit) }

        val landingSongs = if (channelId != null) {
            YtMusicRepository.artistLanding(channelId).getOrNull()?.songs.orEmpty()
        } else {
            emptyList()
        }

        val songs = if (landingSongs.isNotEmpty()) {
            landingSongs
        } else {
            YtMusicRepository.search(artist.name, SearchFilter.SONGS)
                .getOrNull()
                .orEmpty()
                .mapNotNull {
                    when (it) {
                        is SearchResult.TopTrack -> it.song
                        is SearchResult.Track -> it.song
                        is SearchResult.Browse -> null
                    }
                }
        }
        if (songs.isNotEmpty()) {
            artistSongsCache[cacheKey] = songs
        }
        return songs.take(limit)
    }

    private suspend fun buildLanguageSpotlightShelf(language: MusicLanguageOption): HomeShelf? {
        val cached = languagePlaylistCache[language.id]
        val items = cached ?: run {
            val playlists = YtMusicRepository.search("${language.displayName} top hits playlists", SearchFilter.PLAYLISTS)
                .getOrNull()
                .orEmpty()
                .mapNotNull { (it as? SearchResult.Browse)?.item }
                .take(10)
                .map { item ->
                    ShelfItem(
                        title = item.title,
                        subtitle = item.subtitle.ifBlank { language.nativeScript },
                        thumbnailUrl = item.thumbnailUrl,
                        videoId = null,
                        browseId = item.browseId,
                        isVideo = false,
                    )
                }
            if (playlists.isNotEmpty()) {
                languagePlaylistCache[language.id] = playlists
            }
            playlists
        }
        if (items.isEmpty()) return null
        return HomeShelf(
            title = "${language.displayName} Spotlight",
            subtitle = language.nativeScript,
            items = items,
            layout = ShelfLayout.CARDS,
        )
    }

    private suspend fun resolveChannelIdIfSeed(artist: TasteArtistPick): String? {
        if (!artist.browseId.startsWith("seed:")) return artist.browseId
        resolvedSeedCache.values.firstOrNull {
            !it.browseId.startsWith("seed:") && normalizeKey(it.name) == normalizeKey(artist.name)
        }?.let { return it.browseId }

        val rows = YtMusicRepository.search(artist.name, SearchFilter.ARTISTS).getOrNull().orEmpty()
        return bestArtistBrowseMatch(artist.name, rows)?.browseId
    }

    private fun sanitizeArtistThumb(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        if ("artist_avatar@" in rawUrl) return null
        return rawUrl.artworkAt(CARD_ART_PX)
    }

    internal fun bestArtistBrowseMatch(
        queryName: String,
        rows: List<SearchResult>,
        knownBrowseId: String? = null,
    ): BrowseItem? {
        val candidates = rows.mapNotNull { (it as? SearchResult.Browse)?.item }
            .filter { it.type == BrowseType.ARTIST && it.browseId.startsWith("UC") }
        if (candidates.isEmpty()) return null

        if (knownBrowseId != null) {
            candidates.firstOrNull { it.browseId == knownBrowseId }?.let { return it }
        }

        val target = normalizeKey(queryName)
        return candidates
            .filter { item ->
                val cand = normalizeKey(item.title)
                cand == target || cand.contains(target) || target.contains(cand)
            }
            .maxByOrNull { item ->
                val cand = normalizeKey(item.title)
                val nameScore = when {
                    cand == target -> 2_000_000_000L
                    cand.contains(target) || target.contains(cand) -> 1_000_000_000L
                    else -> 0L
                }
                val thumbBonus = if (sanitizeArtistThumb(item.thumbnailUrl) != null) 100_000_000L else 0L
                nameScore + thumbBonus + parseAudienceScore(item.subtitle)
            }
    }

    private fun parseAudienceScore(subtitle: String): Long {
        val match = Regex("""(\d+(?:\.\d+)?)\s*([KMB])""", RegexOption.IGNORE_CASE).find(subtitle)
            ?: return 0L
        val num = match.groupValues[1].toDoubleOrNull() ?: return 0L
        val mult = when (match.groupValues[2].uppercase(Locale.ROOT)) {
            "B" -> 1_000_000_000.0
            "M" -> 1_000_000.0
            "K" -> 1_000.0
            else -> 1.0
        }
        return (num * mult).toLong()
    }

    private fun filterRelated(
        items: List<TasteArtistPick>,
        source: TasteArtistPick,
        excludeBrowseIds: Set<String>,
        excludeNames: Set<String>,
        limit: Int,
    ): List<TasteArtistPick> {
        val normSource = normalizeKey(source.name)
        val normExcluded = excludeNames.mapTo(HashSet()) { normalizeKey(it) }
        return items.filter { item ->
            item.browseId !in excludeBrowseIds &&
                normalizeKey(item.name) != normSource &&
                normalizeKey(item.name) !in normExcluded
        }.take(limit)
    }

    private fun inferRoleFromQueryOrSubtitle(query: String, subtitle: String): ArtistRole {
        val combined = "$query $subtitle".lowercase(Locale.ROOT)
        return when {
            "composer" in combined || "director" in combined || "producer" in combined -> ArtistRole.MUSIC_DIRECTOR
            else -> ArtistRole.SINGER
        }
    }

    private fun seedCacheKey(seed: CuratedArtistSeed): String =
        "${seed.languageId}:${normalizeKey(seed.displayName)}"

    private fun normalizeKey(text: String): String =
        text.trim().lowercase(Locale.ROOT).replace(Regex("""[^\p{L}\p{Nd}]+"""), "")
}
