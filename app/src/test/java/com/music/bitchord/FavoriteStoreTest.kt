package com.music.bitchord

import android.content.SharedPreferences
import com.music.bitchord.data.favorites.FavoriteStore
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.Song
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavoriteStoreTest {

    private lateinit var fakePrefs: FakePrefs

    @Before
    fun setUp() {
        fakePrefs = FakePrefs()
        FavoriteStore.initWithPrefs(fakePrefs)
    }

    @After
    fun tearDown() {
        FavoriteStore.clearForTesting()
    }

    @Test
    fun `liking a song saves it in favorite store`() {
        val testSong = Song(videoId = "vid_1234567", title = "Midnight City", artist = "M83", thumbnailUrl = "https://art/1.jpg")
        FavoriteStore.setLike(testSong.videoId, LikeStatus.LIKE, testSong)

        assertTrue(FavoriteStore.isLiked("vid_1234567"))
        assertTrue("vid_1234567" in FavoriteStore.likedIds.value)
        assertEquals(1, FavoriteStore.favoriteSongs.value.size)
        assertEquals("Midnight City", FavoriteStore.favoriteSongs.value.first().title)
    }

    @Test
    fun `unliking a song removes it from favorite store`() {
        val testSong = Song(videoId = "vid_1234567", title = "Midnight City", artist = "M83", thumbnailUrl = "https://art/1.jpg")
        FavoriteStore.setLike(testSong.videoId, LikeStatus.LIKE, testSong)
        assertTrue(FavoriteStore.isLiked("vid_1234567"))

        FavoriteStore.setLike(testSong.videoId, LikeStatus.INDIFFERENT)
        assertFalse(FavoriteStore.isLiked("vid_1234567"))
        assertFalse("vid_1234567" in FavoriteStore.likedIds.value)
        assertTrue(FavoriteStore.favoriteSongs.value.isEmpty())
    }

    @Test
    fun `disliking a song puts it in disliked set and removes from liked`() {
        val testSong = Song(videoId = "vid_9999999", title = "Skip Song", artist = "Artist", thumbnailUrl = null)
        FavoriteStore.setLike(testSong.videoId, LikeStatus.LIKE, testSong)
        assertTrue(FavoriteStore.isLiked("vid_9999999"))

        FavoriteStore.setLike(testSong.videoId, LikeStatus.DISLIKE)
        assertFalse(FavoriteStore.isLiked("vid_9999999"))
        assertTrue("vid_9999999" in FavoriteStore.dislikedIds.value)
        assertTrue(FavoriteStore.favoriteSongs.value.isEmpty())
    }

    @Test
    fun `reloading from preferences restores saved likes`() {
        val songA = Song(videoId = "vid_aaa1111", title = "Song A", artist = "Artist A", thumbnailUrl = null)
        val songB = Song(videoId = "vid_bbb2222", title = "Song B", artist = "Artist B", thumbnailUrl = null)

        FavoriteStore.setLike(songA.videoId, LikeStatus.LIKE, songA)
        FavoriteStore.setLike(songB.videoId, LikeStatus.LIKE, songB)

        // Simulate reinitializing FavoriteStore from the same underlying prefs
        FavoriteStore.initWithPrefs(fakePrefs)

        assertEquals(setOf("vid_aaa1111", "vid_bbb2222"), FavoriteStore.likedIds.value)
        assertEquals(2, FavoriteStore.favoriteSongs.value.size)
    }

    private class FakePrefs : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data

        override fun getString(key: String?, defValue: String?): String? =
            (data[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? Set<String>)?.toMutableSet() ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (data[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (data[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (data[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (data[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(data)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var cleared = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) changes[key] = value
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (key != null) changes[key] = values?.toSet()
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) changes[key] = value
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) changes[key] = value
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) changes[key] = value
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) changes[key] = value
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) changes[key] = null
            }

            override fun clear(): SharedPreferences.Editor = apply {
                cleared = true
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (cleared) storage.clear()
                for ((k, v) in changes) {
                    if (v == null) storage.remove(k) else storage[k] = v
                }
            }
        }
    }
}
