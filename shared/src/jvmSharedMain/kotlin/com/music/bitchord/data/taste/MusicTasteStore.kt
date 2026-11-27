package com.music.bitchord.data.taste

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

/**
 * Atomic JSON persistence for the listener's selected music languages, singers,
 * and music directors. Shared by Android and Desktop.
 */
object MusicTasteStore {

    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Volatile
    private var file: File? = null

    private val _preferences = MutableStateFlow(MusicTastePreferences())
    val preferences: StateFlow<MusicTastePreferences> = _preferences.asStateFlow()

    private val _tasteRefreshSeq = MutableStateFlow(0 to false)
    val tasteRefreshSeq: StateFlow<Pair<Int, Boolean>> = _tasteRefreshSeq.asStateFlow()

    fun notifyTasteUpdated(syncSubscriptions: Boolean = true) {
        _tasteRefreshSeq.value = (_tasteRefreshSeq.value.first + 1) to syncSubscriptions
    }

    /**
     * Initializes persistence in [baseDir]. Safe to call multiple times with the same path.
     */
    fun init(baseDir: File) {
        synchronized(lock) {
            baseDir.mkdirs()
            val target = File(baseDir, "music_taste_preferences.json")
            file = target
            _preferences.value = if (target.exists()) {
                runCatching {
                    json.decodeFromString(MusicTastePreferences.serializer(), target.readText())
                }.getOrDefault(MusicTastePreferences())
            } else {
                MusicTastePreferences()
            }
        }
    }

    fun toggleLanguage(languageId: String) {
        synchronized(lock) {
            val current = _preferences.value
            val updated = if (languageId in current.selectedLanguageIds) {
                current.selectedLanguageIds - languageId
            } else {
                current.selectedLanguageIds + languageId
            }
            updateAndPersist(
                current.copy(
                    selectedLanguageIds = updated,
                    tasteConfigured = updated.isNotEmpty() || current.selectedArtists.isNotEmpty(),
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun setLanguages(languageIds: List<String>, markConfigured: Boolean = languageIds.isNotEmpty()) {
        synchronized(lock) {
            val distinct = languageIds.distinct()
            val current = _preferences.value
            updateAndPersist(
                current.copy(
                    selectedLanguageIds = distinct,
                    tasteConfigured = markConfigured || current.selectedArtists.isNotEmpty(),
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun toggleArtist(pick: TasteArtistPick) {
        synchronized(lock) {
            val current = _preferences.value
            val exists = current.selectedArtists.any { sameArtist(it, pick) }
            val updated = if (exists) {
                current.selectedArtists.filterNot { sameArtist(it, pick) }
            } else {
                current.selectedArtists + pick
            }
            updateAndPersist(
                current.copy(
                    selectedArtists = updated,
                    tasteConfigured = current.selectedLanguageIds.isNotEmpty() || updated.isNotEmpty(),
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun saveSelection(
        languageIds: List<String>,
        artists: List<TasteArtistPick>,
        markConfigured: Boolean = true,
        autoSubscribeOnSignIn: Boolean? = null,
    ) {
        synchronized(lock) {
            val current = _preferences.value
            val deduplicatedArtists = ArrayList<TasteArtistPick>(artists.size)
            for (artist in artists) {
                if (deduplicatedArtists.none { sameArtist(it, artist) }) {
                    deduplicatedArtists.add(artist)
                }
            }
            updateAndPersist(
                current.copy(
                    selectedLanguageIds = languageIds.distinct(),
                    selectedArtists = deduplicatedArtists,
                    tasteConfigured = markConfigured,
                    autoSubscribeOnSignIn = autoSubscribeOnSignIn ?: current.autoSubscribeOnSignIn,
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun setAutoSubscribeOnSignIn(enabled: Boolean) {
        synchronized(lock) {
            val current = _preferences.value
            updateAndPersist(
                current.copy(
                    autoSubscribeOnSignIn = enabled,
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun clearAll() {
        synchronized(lock) {
            updateAndPersist(MusicTastePreferences(updatedAtMs = System.currentTimeMillis()))
        }
    }

    fun summaryLabel(): String {
        val prefs = _preferences.value
        val langCount = prefs.selectedLanguageIds.size
        val artistCount = prefs.selectedArtists.size
        if (langCount == 0 && artistCount == 0) return "Not customized yet"
        val langsText = when (langCount) {
            0 -> null
            1 -> MusicTasteCatalog.languageById(prefs.selectedLanguageIds.first())?.displayName ?: "1 language"
            else -> "$langCount languages"
        }
        val artistsText = when (artistCount) {
            0 -> null
            1 -> prefs.selectedArtists.first().name
            else -> "$artistCount artists"
        }
        return listOfNotNull(langsText, artistsText).joinToString(" · ")
    }

    fun sameArtist(a: TasteArtistPick, b: TasteArtistPick): Boolean {
        if (a.browseId == b.browseId && !a.browseId.startsWith("seed:")) return true
        return normalizeName(a.name) == normalizeName(b.name)
    }

    internal fun resetForTests() {
        synchronized(lock) {
            file = null
            _preferences.value = MusicTastePreferences()
        }
    }

    private fun normalizeName(name: String): String =
        name.trim().lowercase(Locale.ROOT).replace(Regex("""[^\p{L}\p{Nd}]+"""), "")

    private fun updateAndPersist(next: MusicTastePreferences) {
        _preferences.value = next
        val target = file ?: return
        runCatching {
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(json.encodeToString(MusicTastePreferences.serializer(), next))
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        }
    }
}
