package com.music.bitchord.data.ai

/**
 * Developer gatekeeper for Aura AI.
 * Requires typing "auramusic by likhith" (with or without spaces) into
 * the Settings screen search field to grant access.
 */
object AuraAccessGate {
    private const val SECRET_KEY = "auramusicbylikhith"
    private const val ADMIN_TELEMETRY_KEY = "adminofauramusic"
    const val ADMIN_ROLL_NUMBER = "3035"

    /**
     * Checks if the query matches the developer unlock secret,
     * stripping all whitespace and ignoring case.
     */
    fun isAuraAiUnlockCode(query: String): Boolean {
        if (query.isBlank()) return false
        val normalized = query.lowercase().replace(Regex("\\s+"), "")
        return normalized == SECRET_KEY
    }

    /**
     * Checks if the query matches the admin telemetry unlock phrase ("adminofauramusic"),
     * stripping all whitespace and ignoring case.
     */
    fun isAdminTelemetryUnlockCode(query: String): Boolean {
        if (query.isBlank()) return false
        val normalized = query.lowercase().replace(Regex("\\s+"), "")
        return normalized == ADMIN_TELEMETRY_KEY
    }

    /**
     * Verifies the 4-digit roll number PIN.
     */
    fun verifyAdminRollNumber(pin: String): Boolean {
        return pin.trim() == ADMIN_ROLL_NUMBER
    }
}
