package com.music.bitchord.data.favorites

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.data.LikeState
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable local storage for favorite / liked tracks.
 *
 * Ensures that liking songs works reliably for both guest listeners and signed-in accounts,
 * across offline listening, local device audio, and app restarts.
 */
object FavoriteStore {
    private const val PREF_NAME = "bitchord_favorites"
    private const val KEY_LIKED_IDS = "liked_ids"
    private const val KEY_DISLIKED_IDS = "disliked_ids"
    private const val KEY_FAVORITE_SONGS = "favorite_songs"

    private lateinit var prefs: SharedPreferences

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    private val _dislikedIds = MutableStateFlow<Set<String>>(emptySet())
    val dislikedIds: StateFlow<Set<String>> = _dislikedIds.asStateFlow()

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        load()
    }

    fun initWithPrefs(customPrefs: SharedPreferences) {
        prefs = customPrefs
        load()
    }

    fun clearForTesting() {
        if (!this::prefs.isInitialized) return
        prefs.edit().clear().apply()
        _likedIds.value = emptySet()
        _dislikedIds.value = emptySet()
        _favoriteSongs.value = emptyList()
    }

    private fun load() {
        if (!this::prefs.isInitialized) return
        val savedLiked = prefs.getStringSet(KEY_LIKED_IDS, emptySet())?.toSet().orEmpty()
        val savedDisliked = prefs.getStringSet(KEY_DISLIKED_IDS, emptySet())?.toSet().orEmpty()
        _likedIds.value = savedLiked
        _dislikedIds.value = savedDisliked
        LikeState.seedLiked(savedLiked)

        val rawSongs = prefs.getString(KEY_FAVORITE_SONGS, null)
        if (!rawSongs.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(rawSongs)
                val songs = mutableListOf<Song>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("videoId")
                    if (id.isNotBlank() && id in savedLiked) {
                        songs.add(
                            Song(
                                videoId = id,
                                title = obj.optString("title"),
                                artist = obj.optString("artist"),
                                thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotBlank() && it != "null" },
                                durationText = obj.optString("durationText").takeIf { it.isNotBlank() && it != "null" },
                                artistId = obj.optString("artistId").takeIf { it.isNotBlank() && it != "null" },
                                albumId = obj.optString("albumId").takeIf { it.isNotBlank() && it != "null" },
                                albumName = obj.optString("albumName").takeIf { it.isNotBlank() && it != "null" },
                                isVideo = obj.optBoolean("isVideo", false),
                            )
                        )
                    }
                }
                _favoriteSongs.value = songs
            }
        }
    }

    fun isLiked(videoId: String): Boolean = videoId in _likedIds.value

    fun setLike(videoId: String, status: LikeStatus, song: Song? = null) {
        if (!this::prefs.isInitialized) return
        when (status) {
            LikeStatus.LIKE -> {
                val nextLiked = _likedIds.value + videoId
                val nextDisliked = _dislikedIds.value - videoId
                _likedIds.value = nextLiked
                _dislikedIds.value = nextDisliked
                prefs.edit()
                    .putStringSet(KEY_LIKED_IDS, nextLiked)
                    .putStringSet(KEY_DISLIKED_IDS, nextDisliked)
                    .apply()

                val resolvedSong = song ?: _favoriteSongs.value.firstOrNull { it.videoId == videoId }
                if (resolvedSong != null) {
                    val updated = listOf(resolvedSong) + _favoriteSongs.value.filterNot { it.videoId == videoId }
                    _favoriteSongs.value = updated
                    persistSongs(updated)
                }
            }
            LikeStatus.DISLIKE -> {
                val nextLiked = _likedIds.value - videoId
                val nextDisliked = _dislikedIds.value + videoId
                _likedIds.value = nextLiked
                _dislikedIds.value = nextDisliked
                val updated = _favoriteSongs.value.filterNot { it.videoId == videoId }
                _favoriteSongs.value = updated
                prefs.edit()
                    .putStringSet(KEY_LIKED_IDS, nextLiked)
                    .putStringSet(KEY_DISLIKED_IDS, nextDisliked)
                    .apply()
                persistSongs(updated)
            }
            LikeStatus.INDIFFERENT -> {
                val nextLiked = _likedIds.value - videoId
                val nextDisliked = _dislikedIds.value - videoId
                _likedIds.value = nextLiked
                _dislikedIds.value = nextDisliked
                val updated = _favoriteSongs.value.filterNot { it.videoId == videoId }
                _favoriteSongs.value = updated
                prefs.edit()
                    .putStringSet(KEY_LIKED_IDS, nextLiked)
                    .putStringSet(KEY_DISLIKED_IDS, nextDisliked)
                    .apply()
                persistSongs(updated)
            }
        }
    }

    private fun persistSongs(songs: List<Song>) {
        if (!this::prefs.isInitialized) return
        runCatching {
            val array = JSONArray()
            songs.take(1000).forEach { s ->
                val obj = JSONObject().apply {
                    put("videoId", s.videoId)
                    put("title", s.title)
                    put("artist", s.artist)
                    put("thumbnailUrl", s.thumbnailUrl ?: "")
                    put("durationText", s.durationText ?: "")
                    put("artistId", s.artistId ?: "")
                    put("albumId", s.albumId ?: "")
                    put("albumName", s.albumName ?: "")
                    put("isVideo", s.isVideo)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_FAVORITE_SONGS, array.toString()).apply()
        }
    }
}
