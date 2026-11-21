package com.music.bitchord.playback

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song

/**
 * Smart Shuffle: interleaves fresh, harmonic recommendations into the upcoming queue.
 *
 * Cadence: 1 recommended track every 3 tracks.
 * Invariants:
 * - Current track and USER_QUEUE items are never displaced.
 * - Recommendations are marked with [Song.isSmartRecommendation] = true.
 * - Recommendations are de-duplicated against both the current queue and previously injected recommendations.
 * - Turning Smart Shuffle off immediately strips all recommendations and restores the original queue order.
 */
object SmartShuffleEngine {
    const val INJECTION_CADENCE = 3

    /**
     * Interleaves recommendations into [queue] starting from [currentIndex] + 1.
     */
    fun interleaveRecommendations(
        queue: List<Song>,
        currentIndex: Int,
        recommendations: List<Song>,
    ): List<Song> {
        if (queue.isEmpty() || recommendations.isEmpty()) return queue

        val splitPoint = (currentIndex + 1).coerceIn(0, queue.size)
        val pastAndCurrent = queue.take(splitPoint)
        val upcoming = queue.drop(splitPoint)
        if (upcoming.isEmpty()) return queue

        val existingIds = queue.map { it.videoId }.toSet()
        val availableRecs = recommendations
            .filter { it.videoId !in existingIds }
            .map { it.copy(isSmartRecommendation = true, queueTier = QueueTier.CONTEXT) }
            .toMutableList()

        if (availableRecs.isEmpty()) return queue

        val enrichedUpcoming = mutableListOf<Song>()
        var recIndex = 0

        for (i in upcoming.indices) {
            val item = upcoming[i]
            enrichedUpcoming.add(item)
            if ((i + 1) % INJECTION_CADENCE == 0 && recIndex < availableRecs.size && item.queueTier == QueueTier.CONTEXT) {
                enrichedUpcoming.add(availableRecs[recIndex++])
            }
        }

        return pastAndCurrent + enrichedUpcoming
    }

    /**
     * Strips all smart recommendations from the queue, returning the pristine original queue.
     */
    fun stripRecommendations(queue: List<Song>): List<Song> {
        return queue.filterNot { it.isSmartRecommendation }
    }

    /**
     * Asynchronously loads recommendations for the given seed songs and enriches the queue.
     */
    suspend fun enrichQueue(
        queue: List<Song>,
        currentIndex: Int,
        repository: YtMusicRepository = YtMusicRepository,
    ): List<Song> {
        if (queue.isEmpty()) return queue

        val seedTracks = mutableListOf<Song>()
        if (currentIndex in queue.indices) {
            seedTracks.add(queue[currentIndex])
        }
        val upcomingContext = queue.drop((currentIndex + 1).coerceAtLeast(0))
            .filter { it.queueTier == QueueTier.CONTEXT && !it.isSmartRecommendation }
        seedTracks.addAll(upcomingContext.take(2))

        val gathered = mutableListOf<Song>()
        val existingIds = queue.map { it.videoId }.toMutableSet()

        for (seed in seedTracks) {
            if (gathered.size >= 10) break
            val recResult = repository.radio(seed.videoId)
            val tracks = recResult.getOrNull().orEmpty()
            for (track in tracks) {
                if (track.videoId !in existingIds) {
                    existingIds.add(track.videoId)
                    gathered.add(
                        track.copy(
                            isSmartRecommendation = true,
                            smartSeedTitle = seed.title,
                            queueTier = QueueTier.CONTEXT,
                        )
                    )
                }
            }
        }

        return interleaveRecommendations(queue, currentIndex, gathered)
    }
}
