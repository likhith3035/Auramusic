package com.music.bitchord.data.webdav

/**
 * In-memory credentials for [com.music.bitchord.data.Http]'s interceptor.
 *
 * Held outside SharedPreferences so the network layer never touches prefs:
 * [com.music.bitchord.data.settings.AppSettings] publishes here on init and
 * on every WebDAV edit.
 */
object WebDavAuth {
    @Volatile var scheme: String? = null
    @Volatile var host: String? = null
    @Volatile var port: Int = -1
    @Volatile var authHeader: String? = null

    /** [host] and [header] as WebDavConfig derives them from the saved server. */
    fun update(host: String?, header: String?) {
        this.scheme = null
        this.host = host?.lowercase()
        this.port = -1
        authHeader = header
    }

    fun updateOrigin(scheme: String?, host: String?, port: Int, header: String?) {
        this.scheme = scheme?.lowercase()
        this.host = host?.lowercase()
        this.port = port
        authHeader = header
    }

    fun shouldAuthorize(requestHost: String): Boolean {
        val expected = host ?: return false
        val header = authHeader ?: return false
        if (header.isBlank()) return false
        return requestHost.equals(expected, ignoreCase = true)
    }

    fun shouldAuthorizeOrigin(requestScheme: String, requestHost: String, requestPort: Int): Boolean {
        val expected = host ?: return false
        val header = authHeader ?: return false
        if (header.isBlank()) return false
        if (!requestHost.equals(expected, ignoreCase = true)) return false
        val expectedScheme = scheme
        if (expectedScheme != null && !requestScheme.equals(expectedScheme, ignoreCase = true)) return false
        val normalizedReqPort = if (requestPort > 0) requestPort else defaultPortForScheme(requestScheme)
        val expectedPort = port
        if (expectedPort > 0 && normalizedReqPort != expectedPort) return false
        return true
    }

    fun shouldAuthorizeUrl(url: String): Boolean {
        val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return false
        val s = uri.scheme?.lowercase() ?: return false
        val h = uri.host ?: return false
        return shouldAuthorizeOrigin(s, h, uri.port)
    }

    private fun defaultPortForScheme(s: String): Int = when (s.lowercase()) {
        "https" -> 443
        "http" -> 80
        else -> -1
    }
}
