package com.music.bitchord.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure Kotlin zero-dependency real-time soundbite synthesizer for Desktop Listen Together rooms.
 * Synthesizes instantaneous party soundbites (Airhorn, Bass Drop, Scratch, Applause, Sparkle, Ba-Dum-Tss)
 * using standard Java SE AudioSystem without external dependencies.
 */
internal object DesktopSoundbiteAudio {

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 44100f

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

    fun play(sound: String) {
        audioScope.launch {
            runCatching {
                val pcm = synthesize(sound.lowercase())
                playPcm(pcm)
            }
        }
    }

    private fun playPcm(data: ShortArray) {
        val format = AudioFormat(SAMPLE_RATE, 16, 1, true, false)
        val line: SourceDataLine = AudioSystem.getSourceDataLine(format)
        line.open(format, data.size * 2)
        line.start()

        val byteBuffer = ByteBuffer.allocate(data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in data) {
            byteBuffer.putShort(s)
        }
        val bytes = byteBuffer.array()
        line.write(bytes, 0, bytes.size)
        line.drain()
        line.stop()
        line.close()
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

    private fun synthAirhorn(): ShortArray {
        val sr = SAMPLE_RATE.toInt()
        val blastDuration = (sr * 0.16).toInt()
        val gap = (sr * 0.05).toInt()
        val totalLength = blastDuration * 3 + gap * 2
        val out = ShortArray(totalLength)

        fun renderBlast(offset: Int, len: Int) {
            for (i in 0 until len) {
                val t = i.toDouble() / sr
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

    private fun synthBassDrop(): ShortArray {
        val sr = SAMPLE_RATE.toInt()
        val length = (sr * 0.75).toInt()
        val out = ShortArray(length)
        var phase = 0.0

        for (i in 0 until length) {
            val progress = i.toDouble() / length
            val freq = 160.0 * (1.0 - progress * 0.74)
            phase += 2.0 * PI * freq / sr
            val env = exp(-progress * 3.2)
            val sub = sin(phase)
            val driven = (sub * 1.5).coerceIn(-1.0, 1.0)
            out[i] = (driven * env * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthScratch(): ShortArray {
        val sr = SAMPLE_RATE.toInt()
        val length = (sr * 0.35).toInt()
        val out = ShortArray(length)
        var phase = 0.0
        val rnd = Random(42)

        for (i in 0 until length) {
            val progress = i.toDouble() / length
            val freq = 380.0 + 800.0 * sin(progress * PI * 3.0).coerceAtLeast(0.0)
            phase += 2.0 * PI * freq / sr
            val noise = (rnd.nextDouble() - 0.5) * 0.25
            val env = sin(progress * PI)
            val sample = ((sin(phase) + noise) * env * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
            out[i] = sample
        }
        return out
    }

    private fun synthApplause(): ShortArray {
        val sr = SAMPLE_RATE.toInt()
        val length = (sr * 0.85).toInt()
        val out = ShortArray(length)
        val rnd = Random(1337)
        var lastNoise = 0.0

        for (i in 0 until length) {
            val progress = i.toDouble() / length
            val white = rnd.nextDouble() * 2.0 - 1.0
            lastNoise = lastNoise * 0.65 + white * 0.35
            val clapMod = (sin(progress * 28.0 * PI) * 0.5 + 0.5)
            val env = sin(progress * PI) * (0.6 + 0.4 * clapMod)
            val sample = (lastNoise * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
            out[i] = sample
        }
        return out
    }

    private fun synthSparkle(): ShortArray {
        val sr = SAMPLE_RATE.toInt()
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51, 1567.98)
        val noteLen = (sr * 0.075).toInt()
        val totalLength = noteLen * notes.size + (sr * 0.25).toInt()
        val out = ShortArray(totalLength)

        for (n in notes.indices) {
            val freq = notes[n]
            val offset = n * noteLen
            for (i in 0 until (totalLength - offset)) {
                val t = i.toDouble() / sr
                val env = exp(-t * 8.0)
                val shimmer = sin(2 * PI * freq * t) + 0.3 * sin(2 * PI * (freq * 2) * t)
                val current = out[offset + i].toInt()
                val add = (shimmer * env * 14000.0).toInt()
                out[offset + i] = (current + add).coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun synthDrum(): ShortArray {
        val sr = SAMPLE_RATE.toInt()
        val length = (sr * 0.6).toInt()
        val out = ShortArray(length)
        val rnd = Random(99)

        val hit1Len = (sr * 0.14).toInt()
        for (i in 0 until hit1Len) {
            val t = i.toDouble() / sr
            val freq = 180.0 * (1.0 - (i.toDouble() / hit1Len) * 0.5)
            val env = exp(-t * 22.0)
            out[i] = (sin(2 * PI * freq * t) * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        val offset2 = (sr * 0.16).toInt()
        val hit2Len = (sr * 0.14).toInt()
        for (i in 0 until hit2Len) {
            val t = i.toDouble() / sr
            val freq = 240.0 * (1.0 - (i.toDouble() / hit2Len) * 0.5)
            val env = exp(-t * 22.0)
            out[offset2 + i] = (sin(2 * PI * freq * t) * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        val offset3 = (sr * 0.32).toInt()
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
