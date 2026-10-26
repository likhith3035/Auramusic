package com.music.bitchord.data.listentogether

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class ParsedJamInvite(
    val code: String,
    val serverUrl: String? = null,
)

/** Relays a BitChord web or scheme invite from [com.music.bitchord.MainActivity] to Compose. */
object JamInviteLink {

    const val ORIGIN = "https://github.com/likhith3035/Auramusic"

    private const val EXTRA_CONSUMED = "auramusic.jamInviteConsumed"
    private const val CUSTOM_SCHEME = "auramusic"
    private const val LEGACY_SCHEME = "bitchord"
    private const val CUSTOM_HOST = "party"

    private val _pending = MutableStateFlow<ParsedJamInvite?>(null)
    val pending: StateFlow<ParsedJamInvite?> = _pending.asStateFlow()

    /** Reads a web invite from a cold launch or a new intent on the existing task. */
    fun consume(intent: Intent?): Boolean {
        if (
            intent == null ||
            intent.action != Intent.ACTION_VIEW ||
            intent.getBooleanExtra(EXTRA_CONSUMED, false)
        ) return false

        val invite = parseInvite(intent.dataString) ?: return false
        intent.putExtra(EXTRA_CONSUMED, true)
        _pending.value = invite
        return true
    }

    private val OFFICIAL_HOSTS = setOf(
        "auramusic-kjov.onrender.com",
        "bitchord.kushagrasingh.in",
    )

    fun handled() {
        _pending.value = null
    }

    /** Returns the normalized party code only for the public invite URL shape. */
    fun parse(value: String?): String? = parseInvite(value)?.code

    /**
     * Parses an incoming invite:
     * 1. auramusic://party/<CODE>?server=<SERVER> (or bitchord://...)
     * 2. https://<server-domain>/invite/<CODE>?server=<SERVER>
     */
    fun parseInvite(value: String?): ParsedJamInvite? {
        val uri = runCatching { URI(value ?: return null) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val query = uri.rawQuery
        val server = extractQueryParam(query, "server")?.let { sanitizeServerUrl(it) }

        // 1. Custom scheme: auramusic://party/<CODE> or bitchord://party/<CODE>
        if ((scheme == CUSTOM_SCHEME || scheme == LEGACY_SCHEME) && host == CUSTOM_HOST) {
            val pathPart = uri.path.orEmpty().trim('/').takeIf { it.isNotBlank() }
            val candidate = pathPart ?: extractQueryParam(query, "code") ?: return null
            val code = cleanCode(candidate) ?: return null
            return ParsedJamInvite(code = code, serverUrl = server)
        }

        // 2. Web domain: https://<domain>/invite/<CODE>
        val match = INVITE_PATH.matchEntire(uri.path.orEmpty())
        val activeHost = runCatching { URI(ListenTogether.activePartyServerBase() ?: "").host?.lowercase() }.getOrNull()
        val defaultHost = runCatching { URI(ListenTogether.defaultServer).host?.lowercase() }.getOrNull()
        val isAllowedHost = host in OFFICIAL_HOSTS || (defaultHost != null && host == defaultHost) || (activeHost != null && host == activeHost)
        if (scheme == "https" && isAllowedHost && match != null) {
            val code = match.groupValues[1].uppercase()
            val hostServer = server ?: "${scheme}://${uri.authority}"
            return ParsedJamInvite(code = code, serverUrl = hostServer)
        }

        return null
    }

    fun url(code: String, customServer: String? = null): String {
        val base = (customServer?.trim()?.takeIf { it.isNotBlank() }
            ?: ListenTogether.activePartyServerBase()
            ?: ListenTogether.defaultServer).trimEnd('/')
        return "$base/invite/${code.uppercase()}"
    }

    fun schemeUrl(code: String, customServer: String? = null): String {
        val normalizedCode = code.uppercase()
        val base = customServer?.trim()?.trimEnd('/')
        return if (!base.isNullOrBlank()) {
            val encoded = runCatching { URLEncoder.encode(base, "UTF-8") }.getOrDefault(base)
            "auramusic://party/$normalizedCode?server=$encoded"
        } else {
            "auramusic://party/$normalizedCode"
        }
    }

    private fun cleanCode(raw: String): String? {
        val cleaned = raw.filter { it.isLetterOrDigit() }.uppercase()
        return if (cleaned.length == ListenTogether.CODE_LENGTH) cleaned else null
    }

    private fun extractQueryParam(query: String?, paramName: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split('&').asSequence()
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.isNotEmpty() && it[0].equals(paramName, ignoreCase = true) }
            ?.getOrNull(1)
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    }

    private fun sanitizeServerUrl(raw: String?): String? {
        val trimmed = raw?.trim()?.trimEnd('/') ?: return null
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        if (uri.host.isNullOrBlank()) return null
        return withScheme
    }

    private val INVITE_PATH = Regex("""/invite/([A-Za-z0-9]{${ListenTogether.CODE_LENGTH}})""")
}

