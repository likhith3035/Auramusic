package com.music.bitchord.data.link

/** What kind of YouTube or YouTube Music destination a URL points at. */
enum class YouTubeLinkKind {
    TRACK,
    SHORTS,
    LIVE,
    PAGE,
    SEARCH,
}

/** Chapter timestamp marker parsed from a YouTube video's chapters or description. */
data class VideoChapter(
    val title: String,
    val startSeconds: Long,
    val formattedTime: String,
)

/**
 * Pure-Kotlin representation of a pasted or shared YouTube / YouTube Music URL.
 *
 * Kept in `:shared/commonMain` so Android intents, Android Search, Desktop Search,
 * and clipboard prompts all parse links identically.
 */
data class ParsedYouTubeLink(
    val kind: YouTubeLinkKind,
    val rawUrl: String,
    val canonicalUrl: String,
    val videoId: String? = null,
    val browseId: String? = null,
    /** Set when a `watch?v=...&list=...` URL also names a playlist/album context. */
    val playlistBrowseId: String? = null,
    val searchQuery: String? = null,
    val startSeconds: Long = 0L,
)

object YouTubeLinkParser {

    private val SUPPORTED_HOSTS = setOf(
        "youtu.be",
        "youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtube-nocookie.com",
    )

    private val URL_WITH_SCHEME = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val BARE_YOUTUBE_URL = Regex(
        """(?<![@\w/.-])((?:www\.|m\.|music\.)?(?:youtube\.com|youtube-nocookie\.com|youtu\.be)/\S+)""",
        RegexOption.IGNORE_CASE,
    )
    private val TIMESTAMP_TOKEN = Regex("""(\d+)([hms])""")

    /** Returns true when [text] contains a recognized YouTube or YouTube Music link. */
    fun looksLikeYouTubeLink(text: String): Boolean = parse(text) != null

    /** Extracts the first YouTube/YouTube Music URL from raw clipboard or share text. */
    fun extractFirstUrl(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val match = URL_WITH_SCHEME.find(trimmed)?.value
            ?: BARE_YOUTUBE_URL.find(trimmed)?.groupValues?.getOrNull(1)?.let { "https://$it" }
            ?: return null
        val cleaned = trimTrailingPunctuation(match)
        return cleaned.takeIf { parseUrlComponents(it)?.let(::parseComponents) != null }
    }

    /**
     * Parses [text] (either a bare URL or a sentence/share sheet block carrying one)
     * into a [ParsedYouTubeLink], or returns null when no supported YouTube target exists.
     */
    fun parse(text: String): ParsedYouTubeLink? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val candidate = URL_WITH_SCHEME.find(trimmed)?.value
            ?: BARE_YOUTUBE_URL.find(trimmed)?.groupValues?.getOrNull(1)?.let { "https://$it" }
            ?: return null
        val cleaned = trimTrailingPunctuation(candidate)
        val parts = parseUrlComponents(cleaned) ?: return null
        return parseComponents(parts)
    }

    /** Formats a non-negative second offset as `m:ss` or `h:mm:ss` (`0:00` included when non-null needed). */
    fun formatTimestamp(seconds: Long): String? {
        if (seconds <= 0L) return null
        return formatOffsetSeconds(seconds)
    }

    /** Formats any `seconds >= 0` offset (including `0:00` for chapter markers). */
    fun formatChapterTime(seconds: Long): String = formatOffsetSeconds(seconds.coerceAtLeast(0L))

    private fun formatOffsetSeconds(seconds: Long): String {
        val hours = seconds / 3600L
        val minutes = (seconds % 3600L) / 60L
        val secs = seconds % 60L
        return if (hours > 0L) {
            "$hours:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
        } else {
            "$minutes:${secs.toString().padStart(2, '0')}"
        }
    }

    /**
     * Parses `0:00 Intro` / `1:02:15 Topic` chapter lines out of a YouTube video description
     * when explicit chapter metadata isn't exposed directly by the extractor.
     */
    fun parseChaptersFromDescription(description: String): List<VideoChapter> {
        if (description.isBlank()) return emptyList()
        val linePattern = Regex("""^(?:[\-\u2022*]\s*)?(?:\[)?((?:\d{1,2}:)?\d{1,2}:\d{2})(?:\])?\s*[-–—:|]?\s*(.+)$""")
        val out = LinkedHashMap<Long, VideoChapter>()
        for (rawLine in description.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val match = linePattern.matchEntire(line) ?: continue
            val seconds = parseTimestampSeconds(match.groupValues[1])
            val title = match.groupValues[2].trim().trimStart('-', '–', '—', ':', '|').trim()
            if (title.isEmpty()) continue
            out.putIfAbsent(
                seconds,
                VideoChapter(
                    title = title,
                    startSeconds = seconds,
                    formattedTime = formatChapterTime(seconds),
                ),
            )
            if (out.size >= 60) break
        }
        return if (out.size >= 2) out.values.sortedBy { it.startSeconds } else emptyList()
    }

    internal fun parseTimestampSeconds(raw: String?): Long {
        val value = raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return 0L
        value.toLongOrNull()?.let { return it.coerceAtLeast(0L) }
        val colonParts = value.split(':')
        if (colonParts.size in 2..3 && colonParts.all { it.toLongOrNull() != null }) {
            return when (colonParts.size) {
                2 -> colonParts[0].toLong() * 60L + colonParts[1].toLong()
                3 -> colonParts[0].toLong() * 3600L + colonParts[1].toLong() * 60L + colonParts[2].toLong()
                else -> 0L
            }.coerceAtLeast(0L)
        }
        var total = 0L
        var matched = false
        for (token in TIMESTAMP_TOKEN.findAll(value)) {
            matched = true
            val amount = token.groupValues[1].toLongOrNull() ?: continue
            total += when (token.groupValues[2]) {
                "h" -> amount * 3600L
                "m" -> amount * 60L
                "s" -> amount
                else -> 0L
            }
        }
        return if (matched) total.coerceAtLeast(0L) else 0L
    }

    private fun parseComponents(parts: UrlParts): ParsedYouTubeLink? {
        val host = parts.host.lowercase().removePrefix("www.")
        if (host !in SUPPORTED_HOSTS) return null

        val listId = parts.query["list"]?.trim().orEmpty()
        val playlistBrowseId = toPlaylistBrowseId(listId)
        val startSeconds = listOfNotNull(
            parts.query["t"],
            parts.query["start"],
            parts.fragmentParams["t"],
        ).firstNotNullOfOrNull { param ->
            parseTimestampSeconds(param).takeIf { it > 0L }
        } ?: 0L

        if (host == "youtu.be") {
            val videoId = parts.segments.firstOrNull()?.takeIf(::isPlausibleVideoId) ?: return null
            return ParsedYouTubeLink(
                kind = YouTubeLinkKind.TRACK,
                rawUrl = parts.originalUrl,
                canonicalUrl = canonicalVideoUrl(videoId),
                videoId = videoId,
                playlistBrowseId = playlistBrowseId,
                startSeconds = startSeconds,
            )
        }

        val first = parts.segments.firstOrNull()
        return when {
            first == "watch" -> {
                val videoId = parts.query["v"]?.takeIf(::isPlausibleVideoId)
                when {
                    videoId != null -> ParsedYouTubeLink(
                        kind = YouTubeLinkKind.TRACK,
                        rawUrl = parts.originalUrl,
                        canonicalUrl = canonicalVideoUrl(videoId),
                        videoId = videoId,
                        playlistBrowseId = playlistBrowseId,
                        startSeconds = startSeconds,
                    )
                    playlistBrowseId != null -> ParsedYouTubeLink(
                        kind = YouTubeLinkKind.PAGE,
                        rawUrl = parts.originalUrl,
                        canonicalUrl = parts.originalUrl,
                        browseId = playlistBrowseId,
                        playlistBrowseId = playlistBrowseId,
                    )
                    else -> null
                }
            }
            first == "playlist" -> {
                val browseId = playlistBrowseId ?: return null
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.PAGE,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = parts.originalUrl,
                    browseId = browseId,
                    playlistBrowseId = browseId,
                )
            }
            first == "shorts" -> {
                val videoId = parts.segments.getOrNull(1)?.takeIf(::isPlausibleVideoId) ?: return null
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.SHORTS,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = canonicalVideoUrl(videoId),
                    videoId = videoId,
                    startSeconds = startSeconds,
                )
            }
            first == "live" -> {
                val videoId = parts.segments.getOrNull(1)?.takeIf(::isPlausibleVideoId) ?: return null
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.LIVE,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = canonicalVideoUrl(videoId),
                    videoId = videoId,
                    playlistBrowseId = playlistBrowseId,
                    startSeconds = startSeconds,
                )
            }
            first == "embed" || first == "v" -> {
                val videoId = parts.segments.getOrNull(1)?.takeIf(::isPlausibleVideoId) ?: return null
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.TRACK,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = canonicalVideoUrl(videoId),
                    videoId = videoId,
                    playlistBrowseId = playlistBrowseId,
                    startSeconds = startSeconds,
                )
            }
            first == "channel" || first == "browse" -> {
                val browseId = parts.segments.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.PAGE,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = parts.originalUrl,
                    browseId = browseId,
                )
            }
            first == "search" -> {
                val q = (parts.query["q"] ?: parts.query["search_query"])?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return null
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.SEARCH,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = parts.originalUrl,
                    searchQuery = q,
                )
            }
            first == "results" -> {
                val q = parts.query["search_query"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.SEARCH,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = parts.originalUrl,
                    searchQuery = q,
                )
            }
            first != null && first.startsWith("@") && first.length > 1 -> {
                ParsedYouTubeLink(
                    kind = YouTubeLinkKind.SEARCH,
                    rawUrl = parts.originalUrl,
                    canonicalUrl = parts.originalUrl,
                    searchQuery = first,
                )
            }
            playlistBrowseId != null -> ParsedYouTubeLink(
                kind = YouTubeLinkKind.PAGE,
                rawUrl = parts.originalUrl,
                canonicalUrl = parts.originalUrl,
                browseId = playlistBrowseId,
                playlistBrowseId = playlistBrowseId,
            )
            else -> null
        }
    }

    private fun canonicalVideoUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    private fun toPlaylistBrowseId(listId: String): String? {
        if (listId.isEmpty()) return null
        return if (listId.startsWith("VL")) listId else "VL$listId"
    }

    private fun isPlausibleVideoId(raw: String): Boolean {
        val value = raw.trim()
        return value.isNotEmpty() &&
            value.none { it == '/' || it == '?' || it == '&' || it == '#' || it.isWhitespace() }
    }

    private fun trimTrailingPunctuation(url: String): String =
        url.trimEnd('.', ',', ';', ')', ']', '>', '"', '\'')

    private data class UrlParts(
        val originalUrl: String,
        val host: String,
        val segments: List<String>,
        val query: Map<String, String>,
        val fragmentParams: Map<String, String>,
    )

    private fun parseUrlComponents(url: String): UrlParts? {
        val schemeIdx = url.indexOf("://")
        if (schemeIdx <= 0) return null
        val afterScheme = url.substring(schemeIdx + 3)
        val pathStart = afterScheme.indexOfAny(charArrayOf('/', '?', '#')).let {
            if (it >= 0) it else afterScheme.length
        }
        val authority = afterScheme.substring(0, pathStart)
        val hostPort = authority.substringAfterLast('@')
        val host = hostPort.substringBefore(':').trim()
        if (host.isEmpty()) return null

        val remainder = afterScheme.substring(pathStart)
        val fragmentIdx = remainder.indexOf('#')
        val fragment = if (fragmentIdx >= 0) remainder.substring(fragmentIdx + 1) else ""
        val beforeFragment = if (fragmentIdx >= 0) remainder.substring(0, fragmentIdx) else remainder
        val queryIdx = beforeFragment.indexOf('?')
        val path = if (queryIdx >= 0) beforeFragment.substring(0, queryIdx) else beforeFragment
        val queryString = if (queryIdx >= 0) beforeFragment.substring(queryIdx + 1) else ""

        val segments = path.split('/').filter { it.isNotEmpty() }.map(::percentDecode)
        val query = parseParams(queryString)
        val fragmentParams = parseParams(fragment)
        return UrlParts(
            originalUrl = url,
            host = host,
            segments = segments,
            query = query,
            fragmentParams = fragmentParams,
        )
    }

    private fun parseParams(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in raw.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val key = if (eq >= 0) percentDecode(pair.substring(0, eq)) else percentDecode(pair)
            val value = if (eq >= 0) percentDecode(pair.substring(eq + 1)) else ""
            if (key.isNotEmpty()) out.putIfAbsent(key, value)
        }
        return out
    }

    private fun percentDecode(input: String): String {
        if ('%' !in input && '+' !in input) return input
        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            when (val ch = input[i]) {
                '+' -> {
                    out.append(' ')
                    i++
                }
                '%' -> {
                    if (i + 2 < input.length) {
                        val bytes = ArrayList<Byte>(4)
                        var j = i
                        while (j + 2 < input.length && input[j] == '%') {
                            val b = input.substring(j + 1, j + 3).toIntOrNull(16) ?: break
                            bytes.add(b.toByte())
                            j += 3
                        }
                        if (bytes.isNotEmpty()) {
                            out.append(bytes.toByteArray().decodeToString())
                            i = j
                            continue
                        }
                    }
                    out.append(ch)
                    i++
                }
                else -> {
                    out.append(ch)
                    i++
                }
            }
        }
        return out.toString()
    }
}
