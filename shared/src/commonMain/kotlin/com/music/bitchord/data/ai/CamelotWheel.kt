package com.music.bitchord.data.ai

/**
 * Harmonic relationship classifications based on the Camelot Mixing Wheel.
 */
enum class HarmonicRelationship(val label: String, val isCompatible: Boolean) {
    EXACT("EXACT MATCH", true),
    RELATIVE("RELATIVE KEY", true),
    ENERGY_UP("+1 ENERGY", true),
    ENERGY_DOWN("-1 SUBDOMINANT", true),
    CLASH("KEY CLASH", false),
}

/**
 * Camelot Wheel harmonic key representations for seamless DJ transitions.
 * Supports numbers 1..12 with 'A' (Minor) or 'B' (Major).
 */
data class CamelotKey(val number: Int, val letter: Char) {
    override fun toString(): String = "$number$letter"

    /**
     * Checks if another key is harmonically compatible without dissonance.
     * Compatible transitions:
     * - Identical key (e.g. 8A -> 8A)
     * - Relative major/minor switch (e.g. 8A -> 8B)
     * - Adjacent step on the 12-hour circle (e.g. 8A -> 7A or 8A -> 9A)
     */
    fun isCompatibleWith(other: CamelotKey): Boolean {
        if (this == other) return true
        if (number == other.number) return true
        val stepDiff = (number - other.number + 12) % 12
        return (stepDiff == 1 || stepDiff == 11) && letter == other.letter
    }

    /**
     * Returns the exact harmonic relationship with [other] on the Camelot Wheel.
     */
    fun getRelationship(other: CamelotKey): HarmonicRelationship {
        if (this == other) return HarmonicRelationship.EXACT
        if (number == other.number && letter != other.letter) return HarmonicRelationship.RELATIVE
        if (letter == other.letter) {
            val forwardDiff = (other.number - number + 12) % 12
            if (forwardDiff == 1) return HarmonicRelationship.ENERGY_UP
            if (forwardDiff == 11) return HarmonicRelationship.ENERGY_DOWN
        }
        return HarmonicRelationship.CLASH
    }

    companion object {
        fun fromString(str: String): CamelotKey? {
            val trimmed = str.trim().uppercase()
            if (trimmed.length < 2) return null
            val letter = trimmed.last()
            if (letter != 'A' && letter != 'B') return null
            val num = trimmed.dropLast(1).toIntOrNull() ?: return null
            if (num !in 1..12) return null
            return CamelotKey(num, letter)
        }
    }
}

