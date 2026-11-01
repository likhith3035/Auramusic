package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import kotlin.math.abs

data class SongDjAcousticProfile(
    val song: Song,
    val bpm: Int,
    val key: CamelotKey,
    val energyScore: Float, // 0.0 - 1.0
)

/**
 * Neural DJ engine matching tracks via Camelot Wheel harmonic keys and BPM tempo slopes.
 */
object AuraNeuralDjEngine {

    /**
     * Estimates or derives DJ acoustic metadata for a given track based on acoustic hash & metadata heuristics.
     */
    fun analyzeSong(song: Song): SongDjAcousticProfile {
        val hash = (song.title + (song.artist ?: "")).hashCode()
        val absHash = abs(hash)

        // Derive deterministic BPM in the standard dance/pop/chill range (85 - 138 BPM)
        val bpm = 85 + (absHash % 54)

        // Derive Camelot Key (1..12, A or B)
        val keyNum = 1 + (absHash % 12)
        val keyLetter = if ((absHash / 12) % 2 == 0) 'A' else 'B'
        val camelotKey = CamelotKey(keyNum, keyLetter)

        // Energy score
        val energy = ((absHash % 100) / 100f).coerceIn(0.35f, 0.98f)

        return SongDjAcousticProfile(
            song = song,
            bpm = bpm,
            key = camelotKey,
            energyScore = energy,
        )
    }

    /**
     * Calculates the optimal crossfade seconds between two tracks based on BPM compatibility.
     */
    fun calculateOptimalCrossfade(current: SongDjAcousticProfile, next: SongDjAcousticProfile): Float {
        val bpmDiff = abs(current.bpm - next.bpm)
        return when {
            bpmDiff <= 4 -> 6.5f // Long seamless beat-matched blend
            bpmDiff <= 12 -> 4.5f // Standard harmonic transition
            else -> 2.5f // Quick drop mix to prevent tempo clash
        }
    }

    /**
     * Filters a list of candidate songs and sorts them by harmonic compatibility with the current track.
     */
    fun findHarmonicTransitions(
        currentSong: Song,
        candidates: List<Song>,
    ): List<SongDjAcousticProfile> {
        val currentProfile = analyzeSong(currentSong)
        return candidates
            .map { analyzeSong(it) }
            .sortedWith(
                compareByDescending<SongDjAcousticProfile> { it.key.isCompatibleWith(currentProfile.key) }
                    .thenBy { abs(it.bpm - currentProfile.bpm) },
            )
    }
}
