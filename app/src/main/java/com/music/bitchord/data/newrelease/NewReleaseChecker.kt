package com.music.bitchord.data.newrelease

import android.content.Context
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Checks for new music releases from artists the user subscribes to on YouTube Music.
 */
object NewReleaseChecker {

    private val ARTIST_SEPARATORS = listOf(", ", " & ", " feat. ", " ft. ", " / ", " x ", " X ")
    private val KNOWN_TYPES = setOf("album", "single", "ep", "video", "song")

    /**
     * Runs the new release detection algorithm.
     *
     * @param context Application context
     * @param forceAll If true, returns all detected releases for followed artists (even if previously seen),
     *                 useful for populating the "New For You" shelf on Home.
     *                 If false, returns only releases that have not yet been seen (for notifications).
     * @return List of new releases found
     */
    suspend fun checkNewReleases(
        context: Context,
        forceAll: Boolean = false,
    ): List<NewRelease> = withContext(Dispatchers.IO) {
        // 1. Signed-in check
        if (Innertube.cookie == null) {
            return@withContext emptyList()
        }

        // 2. Fetch subscribed artists and new releases in parallel
        val (subscribedArtists, releaseShelves) = coroutineScope {
            val subDeferred = async { YtMusicRepository.subscribedArtists().getOrNull().orEmpty() }
            val releasesDeferred = async {
                val primary = YtMusicRepository.homeSupplement("FEmusic_new_releases").getOrNull().orEmpty()
                val explore = YtMusicRepository.homeSupplement("FEmusic_explore").getOrNull().orEmpty()
                primary + explore
            }
            subDeferred.await() to releasesDeferred.await()
        }

        if (subscribedArtists.isEmpty() || releaseShelves.isEmpty()) {
            return@withContext emptyList()
        }

        // 3. Build lookup maps for followed artists
        // Normalise names by trimming, lowercasing, and stripping common punctuation
        val followedArtistsByName = HashMap<String, ShelfItem>()
        for (artist in subscribedArtists) {
            val norm = normalize(artist.title)
            if (norm.isNotBlank()) {
                followedArtistsByName[norm] = artist
            }
        }

        // 4. Scan release items across all shelves
        val allReleases = ArrayList<NewRelease>()
        val seenInCurrentRun = HashSet<String>()

        for (shelf in releaseShelves) {
            for (item in shelf.items) {
                val releaseId = item.browseId ?: item.videoId ?: continue
                if (!seenInCurrentRun.add(releaseId)) continue

                val matchedArtistName = findMatchingFollowedArtist(
                    subtitle = item.subtitle,
                    followedArtists = followedArtistsByName,
                ) ?: continue

                val type = parseReleaseType(item.subtitle)

                allReleases.add(
                    NewRelease(
                        id = releaseId,
                        title = item.title,
                        artistName = matchedArtistName,
                        thumbnailUrl = item.thumbnailUrl,
                        browseId = item.browseId,
                        videoId = item.videoId,
                        releaseType = type,
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            }
        }

        // 5. Check against cache
        val seenIds = NewReleaseCache.getSeenIds(context)
        val unseenReleases = allReleases.filter { it.id !in seenIds }

        // Update the cache with latest findings
        NewReleaseCache.update(
            context = context,
            newReleases = allReleases,
            newSeenIds = emptySet(),
            updateCheckTime = true,
        )

        if (forceAll) allReleases else unseenReleases
    }

    private fun findMatchingFollowedArtist(
        subtitle: String,
        followedArtists: Map<String, ShelfItem>,
    ): String? {
        val rawArtist = InnertubeParser.artistFromSubtitle(subtitle).trim()
        val normRaw = normalize(rawArtist)

        // 1. Direct match
        followedArtists[normRaw]?.let { return it.title }

        // 2. Delimited match (collaborations, features)
        var parts = listOf(rawArtist)
        for (sep in ARTIST_SEPARATORS) {
            parts = parts.flatMap { it.split(sep) }
        }

        for (part in parts) {
            val normPart = normalize(part.trim())
            followedArtists[normPart]?.let { return it.title }
        }

        // 3. Subtitle token check for artists with special characters
        for ((normName, artist) in followedArtists) {
            if (normName.length >= 3 && normRaw.contains(normName)) {
                return artist.title
            }
        }

        return null
    }

    private fun parseReleaseType(subtitle: String): String? {
        val parts = subtitle.split(" • ").map { it.trim() }
        for (part in parts) {
            val lower = part.lowercase(Locale.ROOT)
            if (lower in KNOWN_TYPES) {
                return part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
        return null
    }

    private fun normalize(name: String): String =
        name.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{Nd}]+"""), " ")
            .trim()
}
