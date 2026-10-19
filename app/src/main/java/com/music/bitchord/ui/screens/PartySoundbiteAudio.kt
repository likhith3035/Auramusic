package com.music.bitchord.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure Kotlin zero-dependency real-time soundbite synthesizer for Listen Together parties.
 * Plays punchy, instant room audio cues (Airhorn, Bass Drop, Scratch, Applause, Sparkle, Drum)
 * with 0 external network/asset downloads and zero latency.
 */
object PartySoundbiteAudio {

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 44100
    private val pcmCache = ConcurrentHashMap<String, ShortArray>()
    private val trackLock = Any()
    private var currentTrack: AudioTrack? = null

    private var lastLocalPlayedKey: String? = null
    private var lastLocalPlayedTimeMs = 0L

    data class SoundbiteInfo(
        val key: String,
        val label: String,
        val emoji: String,
    )

    val SOUNDBITES = listOf(
        SoundbiteInfo("airhorn", "Airhorn", "📯"),
        SoundbiteInfo("bassdrop", "Bass Drop", "🔊"),
        SoundbiteInfo("scratch", "Scratch", "📀"),
        SoundbiteInfo("applause", "Applause", "👏"),
        SoundbiteInfo("sparkle", "Sparkle", "✨"),
        SoundbiteInfo("drum", "Ba-Dum-Tss", "🥁"),
    )

    fun markLocalPlay(key: String) {
        lastLocalPlayedKey = key.lowercase()
        lastLocalPlayedTimeMs = SystemClock.elapsedRealtime()
    }

    fun wasRecentlyPlayedLocally(key: String, windowMs: Long = 1800L): Boolean {
        val now = SystemClock.elapsedRealtime()
        val match = key.equals(lastLocalPlayedKey, ignoreCase = true) && (now - lastLocalPlayedTimeMs) < windowMs
        if (match) {
            lastLocalPlayedKey = null
        }
        return match
    }

    fun play(sound: String) {
        audioScope.launch {
            runCatching {
                val key = sound.lowercase()
                val pcm = pcmCache.getOrPut(key) { synthesize(key) }
                playPcm(pcm, SAMPLE_RATE)
            }
        }
    }

    private fun playPcm(data: ShortArray, sampleRate: Int) {
        synchronized(trackLock) {
            runCatching {
                currentTrack?.stop()
                currentTrack?.release()
            }
            currentTrack = null
        }

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        }.getOrNull() ?: return

        track.write(data, 0, data.size)
        track.play()
        synchronized(trackLock) {
            currentTrack = track
        }

        audioScope.launch {
            val durationMs = (data.size.toLong() * 1000L / sampleRate) + 250L
            delay(durationMs)
            synchronized(trackLock) {
                if (currentTrack === track) {
                    runCatching {
                        track.stop()
                        track.release()
                    }
                    currentTrack = null
                }
            }
        }
    }

    private fun synthesize(sound: String): ShortArray {
        return when (sound) {
            "airhorn" -> synthAirhorn()
            "bassdrop" -> synthBassDrop()
            "scratch" -> synthScratch()
            "applause" -> synthApplause()
            "sparkle" -> synthSparkle()
            "drum" -> synthDrum()
            else -> synthAirhorn()
        }
    }

    /** Iconic brass triple-blast airhorn */
    private fun synthAirhorn(): ShortArray {
        val blastDuration = (SAMPLE_RATE * 0.16).toInt()
        val gap = (SAMPLE_RATE * 0.05).toInt()
        val totalLength = blastDuration * 3 + gap * 2
        val out = ShortArray(totalLength)

        fun renderBlast(offset: Int, len: Int) {
            for (i in 0 until len) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = (1.0 - (i.toDouble() / len) * 0.4).coerceIn(0.0, 1.0)
                val f1 = 466.16 // Bb4
                val f2 = 622.25 // Eb5
                val f3 = 932.33 // Bb5
                val wave = (sin(2 * PI * f1 * t) * 0.5 + sin(2 * PI * f2 * t) * 0.35 + sin(2 * PI * f3 * t) * 0.15)
                val sample = (wave * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
                out[offset + i] = sample
            }
        }

        renderBlast(0, blastDuration)
        renderBlast(blastDuration + gap, blastDuration)
        renderBlast((blastDuration + gap) * 2, blastDuration)
        return out
    }

    /** 808 bass drop sweeping from 160Hz down to 42Hz */
    private fun synthBassDrop(): ShortArray {
        val length = (SAMPLE_RATE * 0.75).toInt()
        val out = ShortArray(length)
        var phase = 0.0

        for (i in 0 until length) {
            val progress = i.toDouble() / length
            val freq = 160.0 * (1.0 - progress * 0.74) // sweeps down to ~42Hz
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val env = exp(-progress * 3.2)
            val sub = sin(phase)
            // Soft saturation / drive
            val driven = (sub * 1.5).coerceIn(-1.0, 1.0)
            out[i] = (driven * env * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    /** Vinyl DJ record scratch */
    private fun synthScratch(): ShortArray {
        val length = (SAMPLE_RATE * 0.35).toInt()
        val out = ShortArray(length)
        var phase = 0.0
        val rnd = Random(42)

        for (i in 0 until length) {
            val progress = i.toDouble() / length
            // Modulated frequency back and forth
            val freq = 380.0 + 800.0 * sin(progress * PI * 3.0).coerceAtLeast(0.0)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val noise = (rnd.nextDouble() - 0.5) * 0.25
            val env = sin(progress * PI)
            val sample = ((sin(phase) + noise) * env * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
            out[i] = sample
        }
        return out
    }

    /** Rapid crowd applause and clapping burst */
    private fun synthApplause(): ShortArray {
        val length = (SAMPLE_RATE * 0.85).toInt()
        val out = ShortArray(length)
        val rnd = Random(1337)
        var lastNoise = 0.0

        for (i in 0 until length) {
            val progress = i.toDouble() / length
            val white = rnd.nextDouble() * 2.0 - 1.0
            // Low-pass filter for clapping warmth
            lastNoise = lastNoise * 0.65 + white * 0.35

            // Clapping rhythm bursts
            val clapMod = (sin(progress * 28.0 * PI) * 0.5 + 0.5)
            val env = sin(progress * PI) * (0.6 + 0.4 * clapMod)
            val sample = (lastNoise * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
            out[i] = sample
        }
        return out
    }

    /** Ascending fairy/magic arpeggio twinkle */
    private fun synthSparkle(): ShortArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51, 1567.98) // C5, E5, G5, C6, E6, G6
        val noteLen = (SAMPLE_RATE * 0.075).toInt()
        val totalLength = noteLen * notes.size + (SAMPLE_RATE * 0.25).toInt()
        val out = ShortArray(totalLength)

        for (n in notes.indices) {
            val freq = notes[n]
            val offset = n * noteLen
            for (i in 0 until (totalLength - offset)) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 8.0)
                val shimmer = sin(2 * PI * freq * t) + 0.3 * sin(2 * PI * (freq * 2) * t)
                val current = out[offset + i].toInt()
                val add = (shimmer * env * 14000.0).toInt()
                out[offset + i] = (current + add).coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    /** Punchy comedy ba-dum-tss drum rimshot */
    private fun synthDrum(): ShortArray {
        val length = (SAMPLE_RATE * 0.6).toInt()
        val out = ShortArray(length)
        val rnd = Random(99)

        // Hit 1 (Ba): 0.0s - 0.15s (low tom)
        val hit1Len = (SAMPLE_RATE * 0.14).toInt()
        for (i in 0 until hit1Len) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 180.0 * (1.0 - (i.toDouble() / hit1Len) * 0.5)
            val env = exp(-t * 22.0)
            out[i] = (sin(2 * PI * freq * t) * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        // Hit 2 (Dum): 0.15s - 0.30s (higher tom)
        val offset2 = (SAMPLE_RATE * 0.16).toInt()
        val hit2Len = (SAMPLE_RATE * 0.14).toInt()
        for (i in 0 until hit2Len) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 240.0 * (1.0 - (i.toDouble() / hit2Len) * 0.5)
            val env = exp(-t * 22.0)
            out[offset2 + i] = (sin(2 * PI * freq * t) * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        // Hit 3 (Tss): 0.32s - end (cymbal crash)
        val offset3 = (SAMPLE_RATE * 0.32).toInt()
        val hit3Len = length - offset3
        var cymbalNoise = 0.0
        for (i in 0 until hit3Len) {
            val progress = i.toDouble() / hit3Len
            val white = rnd.nextDouble() * 2.0 - 1.0
            cymbalNoise = cymbalNoise * 0.3 + white * 0.7
            val env = exp(-progress * 6.5)
            out[offset3 + i] = (cymbalNoise * env * 21000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        return out
    }
}
