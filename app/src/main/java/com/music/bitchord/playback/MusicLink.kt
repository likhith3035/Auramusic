package com.music.bitchord.playback

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.music.bitchord.data.link.ParsedYouTubeLink
import com.music.bitchord.data.link.YouTubeLinkKind
import com.music.bitchord.data.link.YouTubeLinkParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What an intent from outside the app turned out to be asking for. */
sealed interface LinkRequest {
    /** A song, music video, Short, or live broadcast by video id. */
    data class Track(
        val videoId: String,
        val startPositionMs: Long = 0L,
        val playlistBrowseId: String? = null,
        val isShort: Boolean = false,
        val isLive: Boolean = false,
    ) : LinkRequest

    /** An album, playlist or artist page, by browse id. */
    data class Page(val browseId: String) : LinkRequest

    /**
     * Words rather than an id: "play Blinding Lights", or a shared search URL.
     *
     * [play] separates the two. A spoken request is an instruction — it should
     * start the best match, not leave a list on screen for someone whose phone
     * is in their pocket — while a search *link* is a page somebody meant to
     * show you.
     */
    data class Search(val query: String, val play: Boolean) : LinkRequest

    /** "Play music", with nothing said about what. */
    data object Resume : LinkRequest
}

/**
 * Links and voice requests handed to BitChord from elsewhere on the device.
 *
 * The same relay [PlayerDeepLink] is, and for the same reason: what has to
 * happen — start a queue, push a page, run a search — is all inside
 * `BitChordApp`'s composition, which [MainActivity][com.music.bitchord.MainActivity]
 * has no handle on. The activity reads the intent, this holds the answer, and
 * the composition serves it once its controller and view model exist.
 *
 * Kept apart from [PlayerDeepLink] rather than folded into it because the two
 * are answered by different things at different times: opening the player is a
 * boolean the sheet reads, while this is a request that may take a network
 * round trip before anything happens on screen.
 */
object MusicLink {

    /**
     * Marks an intent as already read.
     *
     * MainActivity is `singleTask` and declares no `configChanges`, so a theme
     * or font-size change destroys and recreates it — with `getIntent()` still
     * returning the link that launched it. Without this, rotating the phone an
     * hour later would replay the shared song over whatever is playing.
     */
    private const val EXTRA_CONSUMED = "bitchord.linkConsumed"

    private val _pending = MutableStateFlow<LinkRequest?>(null)

    /** The outstanding request, or null. Cleared by [handled]. */
    val pending: StateFlow<LinkRequest?> = _pending.asStateFlow()

    /** Reads an incoming intent, and reports whether it carried a request. */
    fun consume(intent: Intent?): Boolean {
        if (intent == null || intent.getBooleanExtra(EXTRA_CONSUMED, false)) return false
        val request = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString?.let(::parseText) ?: intent.data?.let(::parse)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::parseText)
            // The assistant's "play <something>". An empty query is the whole
            // point of the Resume case: "play music" names nothing, and the
            // useful answer is to carry on with what was already on.
            MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH -> {
                val query = intent.getStringExtra(SearchManager.QUERY).orEmpty().trim()
                if (query.isEmpty()) LinkRequest.Resume else LinkRequest.Search(query, play = true)
            }
            else -> null
        } ?: return false
        intent.putExtra(EXTRA_CONSUMED, true)
        _pending.value = request
        return true
    }

    /**
     * Called once the request has actually been acted on.
     *
     * By whoever acted, not by whoever set it — this object outlives the
     * composition, and a request left standing would be served again by the
     * next one, which is the shared song starting over on the first
     * configuration change.
     */
    fun handled() {
        _pending.value = null
    }

    /**
     * What a YouTube or YouTube Music URL points at, or null for one this app
     * has nothing to show for.
     */
    fun parse(uri: Uri): LinkRequest? = parseText(uri.toString())

    /** Parses raw URL or share-sheet text into a typed [LinkRequest], or null if unsupported. */
    fun parseText(text: String): LinkRequest? =
        YouTubeLinkParser.parse(text)?.let(::fromParsed)

    internal fun fromParsed(parsed: ParsedYouTubeLink): LinkRequest? = when (parsed.kind) {
        YouTubeLinkKind.TRACK, YouTubeLinkKind.SHORTS, YouTubeLinkKind.LIVE ->
            parsed.videoId?.let { videoId ->
                LinkRequest.Track(
                    videoId = videoId,
                    startPositionMs = (parsed.startSeconds * 1000L).coerceAtLeast(0L),
                    playlistBrowseId = parsed.playlistBrowseId,
                    isShort = parsed.kind == YouTubeLinkKind.SHORTS,
                    isLive = parsed.kind == YouTubeLinkKind.LIVE,
                )
            }
        YouTubeLinkKind.PAGE -> parsed.browseId?.let(LinkRequest::Page)
        YouTubeLinkKind.SEARCH -> parsed.searchQuery?.let { LinkRequest.Search(it, play = false) }
    }
}
