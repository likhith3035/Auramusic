package com.music.bitchord.data.newrelease

import com.music.bitchord.data.model.ShelfItem
import kotlinx.serialization.Serializable

/**
 * A newly detected release (single, album, or EP) from an artist the user follows.
 */
@Serializable
data class NewRelease(
    val id: String,
    val title: String,
    val artistName: String,
    val thumbnailUrl: String? = null,
    val browseId: String? = null,
    val videoId: String? = null,
    val releaseType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    /**
     * Converts this release into a [ShelfItem] that the Home feed can render.
     */
    fun toShelfItem(): ShelfItem = ShelfItem(
        title = title,
        subtitle = if (releaseType != null) "$releaseType • $artistName" else artistName,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = browseId,
    )
}
