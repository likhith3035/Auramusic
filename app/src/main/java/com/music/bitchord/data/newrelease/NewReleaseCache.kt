package com.music.bitchord.data.newrelease

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists known releases and seen states on disk so we only notify the user
 * about genuinely new releases and can instantly display the "New For You" shelf
 * without waiting for background network requests.
 */
object NewReleaseCache {

    private const val FILE_NAME = "new_releases_cache.json"
    private const val MAX_SEEN_IDS = 1000
    private const val MAX_CACHED_RELEASES = 50

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val mutex = Mutex()
    private var inMemoryData: CacheData? = null

    @Serializable
    private data class CacheData(
        val lastCheckTime: Long = 0L,
        val seenIds: List<String> = emptyList(),
        val cachedReleases: List<NewRelease> = emptyList(),
    )

    private fun cacheFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    suspend fun getCachedReleases(context: Context): List<NewRelease> = mutex.withLock {
        ensureLoaded(context).cachedReleases
    }

    suspend fun getSeenIds(context: Context): Set<String> = mutex.withLock {
        ensureLoaded(context).seenIds.toSet()
    }

    suspend fun getLastCheckTime(context: Context): Long = mutex.withLock {
        ensureLoaded(context).lastCheckTime
    }

    /**
     * Stores new releases and marks seen release IDs.
     */
    suspend fun update(
        context: Context,
        newReleases: List<NewRelease>,
        newSeenIds: Set<String> = emptySet(),
        updateCheckTime: Boolean = true,
    ) = mutex.withLock {
        val current = ensureLoaded(context)
        val combinedSeen = (current.seenIds + newSeenIds).distinct().takeLast(MAX_SEEN_IDS)
        val combinedReleases = (newReleases + current.cachedReleases)
            .distinctBy { it.id }
            .take(MAX_CACHED_RELEASES)

        val updated = current.copy(
            lastCheckTime = if (updateCheckTime) System.currentTimeMillis() else current.lastCheckTime,
            seenIds = combinedSeen,
            cachedReleases = combinedReleases,
        )
        inMemoryData = updated
        persist(context, updated)
    }

    /**
     * Mark all currently cached releases as seen by the user.
     */
    suspend fun markAllAsSeen(context: Context) = mutex.withLock {
        val current = ensureLoaded(context)
        val releaseIds = current.cachedReleases.map { it.id }
        val updated = current.copy(
            seenIds = (current.seenIds + releaseIds).distinct().takeLast(MAX_SEEN_IDS),
        )
        inMemoryData = updated
        persist(context, updated)
    }

    /**
     * Clears all cached releases and seen IDs (e.g. on sign out).
     */
    suspend fun clear(context: Context) = mutex.withLock {
        inMemoryData = CacheData()
        withContext(Dispatchers.IO) {
            runCatching { cacheFile(context).delete() }
        }
    }

    private suspend fun ensureLoaded(context: Context): CacheData {
        inMemoryData?.let { return it }
        val data = withContext(Dispatchers.IO) {
            val file = cacheFile(context)
            if (!file.exists()) return@withContext CacheData()
            runCatching {
                json.decodeFromString<CacheData>(file.readText())
            }.getOrElse { CacheData() }
        }
        inMemoryData = data
        return data
    }

    private suspend fun persist(context: Context, data: CacheData) {
        withContext(Dispatchers.IO) {
            runCatching {
                val file = cacheFile(context)
                val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
                tempFile.writeText(json.encodeToString(data))
                if (tempFile.renameTo(file)) {
                    // Success
                } else {
                    file.delete()
                    tempFile.renameTo(file)
                }
            }
        }
    }
}
