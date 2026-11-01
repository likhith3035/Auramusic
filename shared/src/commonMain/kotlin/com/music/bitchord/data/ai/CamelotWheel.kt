package com.music.bitchord.data.ai

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
