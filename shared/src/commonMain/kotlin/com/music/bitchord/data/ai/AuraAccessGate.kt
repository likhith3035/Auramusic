package com.music.bitchord.data.ai

/**
 * Developer gatekeeper for Aura AI.
 * Requires typing "auramusic by likhith" (with or without spaces) into
 * the Settings screen search field to grant access.
 */
object AuraAccessGate {
    private const val SECRET_KEY = "auramusicbylikhith"

    /**
     * Checks if the query matches the developer unlock secret,
     * stripping all whitespace and ignoring case.
     */
    fun isAuraAiUnlockCode(query: String): Boolean {
        if (query.isBlank()) return false
        val normalized = query.lowercase().replace(Regex("\\s+"), "")
        return normalized == SECRET_KEY
    }
}
