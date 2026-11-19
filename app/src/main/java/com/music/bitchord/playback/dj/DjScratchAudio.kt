package com.music.bitchord.playback.dj

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance, zero-latency physical vinyl scratch sound emulation engine for AuraMusic DJ console.
 * Synthesizes authentic 16-bit 44.1kHz PCM acoustic waveforms in-memory:
 * 1. Forward chirp: upward FM glide with needle micro-groove friction noise.
 * 2. Reverse rub: throaty downward FM drag with sub-harmonic gritty texture.
 * 3. Spinback flutter: exponential pitch drop with platter rotational wobble tremolo.
 *
 * Buffers are written to application cache and loaded into a hardware-backed SoundPool
 * for sub-millisecond dynamic rate and volume modulation.
 */
class DjScratchAudio(private val context: Context) {

    private val tag = "DjScratchAudio"
    private val sampleRate = 44100

    private var soundPool: SoundPool? = null
    private var chirpSoundId: Int = 0
    private var rubSoundId: Int = 0
    private var spinSoundId: Int = 0

    private var chirpLoaded = false
    private var rubLoaded = false
    private var spinLoaded = false

    private var activeStreamId: Int = 0
    private var lastPlayTimeMs: Long = 0L

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    when (sampleId) {
                        chirpSoundId -> chirpLoaded = true
                        rubSoundId -> rubLoaded = true
                        spinSoundId -> spinLoaded = true
                    }
                }
            }

            prepareWavFiles()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize DjScratchAudio", e)
        }
    }

    private fun prepareWavFiles() {
        val cacheDir = context.cacheDir
        val chirpWav = File(cacheDir, "dj_scratch_chirp.wav")
        val rubWav = File(cacheDir, "dj_scratch_rub.wav")
        val spinWav = File(cacheDir, "dj_scratch_spin.wav")

        if (!chirpWav.exists() || chirpWav.length() == 0L) {
            val samples = synthesizeForwardChirp()
            writeWavFile(chirpWav, sampleRate, samples)
        }
        if (!rubWav.exists() || rubWav.length() == 0L) {
            val samples = synthesizeReverseRub()
            writeWavFile(rubWav, sampleRate, samples)
        }
        if (!spinWav.exists() || spinWav.length() == 0L) {
            val samples = synthesizeSpinbackFlutter()
            writeWavFile(spinWav, sampleRate, samples)
        }

        soundPool?.let { sp ->
            chirpSoundId = sp.load(chirpWav.absolutePath, 1)
            rubSoundId = sp.load(rubWav.absolutePath, 1)
            spinSoundId = sp.load(spinWav.absolutePath, 1)
        }
    }

    /**
     * Synthesizes forward slip chirp: 130ms duration.
     * Sweeps from 650Hz to 2100Hz with needle friction noise.
     */
    private fun synthesizeForwardChirp(): ShortArray {
        val durationSec = 0.130f
        val numSamples = (sampleRate * durationSec).toInt()
        val result = ShortArray(numSamples)
        val random = Random(42L)

        var phase = 0.0
        var noiseLp = 0.0

        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples.toFloat() // 0..1
            // Exponential pitch sweep up
            val freq = 650.0 + 1550.0 * (t * t * 1.2f).coerceAtMost(1.0f)
            val deltaPhase = 2.0 * PI * freq / sampleRate
            phase += deltaPhase

            // Overtones
            val tone = sin(phase) + 0.35 * sin(phase * 2.0) + 0.15 * sin(phase * 3.0)

            // Friction noise with high-frequency emphasis
            val rawNoise = (random.nextDouble() * 2.0 - 1.0)
            noiseLp = noiseLp * 0.4 + rawNoise * 0.6
            val friction = rawNoise - noiseLp

            // Envelope: 12ms attack, smooth release
            val attackSamples = (sampleRate * 0.012f).toInt()
            val releaseSamples = (sampleRate * 0.035f).toInt()
            val env = when {
                i < attackSamples -> i.toFloat() / attackSamples
                i > numSamples - releaseSamples -> (numSamples - i).toFloat() / releaseSamples
                else -> 1.0f
            }

            val mixed = (tone * 0.72 + friction * 0.28) * env * 0.88
            val sampleInt = (mixed * 32767.0).toInt().coerceIn(-32767, 32767)
            result[i] = sampleInt.toShort()
        }
        return result
    }

    /**
     * Synthesizes reverse rub: 155ms duration.
     * Throaty downward sweep from 500Hz to 175Hz with gritty texture and sub-harmonics.
     */
    private fun synthesizeReverseRub(): ShortArray {
        val durationSec = 0.155f
        val numSamples = (sampleRate * durationSec).toInt()
        val result = ShortArray(numSamples)
        val random = Random(1337L)

        var phase = 0.0
        var noiseLp = 0.0

        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples.toFloat() // 0..1
            // Downward sweep
            val freq = 500.0 * (1.0 - 0.65 * t).coerceAtLeast(0.3)
            val deltaPhase = 2.0 * PI * freq / sampleRate
            phase += deltaPhase

            // Sub-harmonic and grit
            val tone = sin(phase) + 0.40 * sin(phase * 0.5) + 0.25 * sin(phase * 2.0)

            val rawNoise = (random.nextDouble() * 2.0 - 1.0)
            noiseLp = noiseLp * 0.55 + rawNoise * 0.45
            val grit = noiseLp

            val attackSamples = (sampleRate * 0.010f).toInt()
            val releaseSamples = (sampleRate * 0.040f).toInt()
            val env = when {
                i < attackSamples -> i.toFloat() / attackSamples
                i > numSamples - releaseSamples -> (numSamples - i).toFloat() / releaseSamples
                else -> 1.0f
            }

            val mixed = (tone * 0.65 + grit * 0.35) * env * 0.88
            val sampleInt = (mixed * 32767.0).toInt().coerceIn(-32767, 32767)
            result[i] = sampleInt.toShort()
        }
        return result
    }

    /**
     * Synthesizes spinback flutter: 360ms duration.
     * Fast exponential drop from 2400Hz to 75Hz with 38Hz platter wobble tremolo.
     */
    private fun synthesizeSpinbackFlutter(): ShortArray {
        val durationSec = 0.360f
        val numSamples = (sampleRate * durationSec).toInt()
        val result = ShortArray(numSamples)
        val random = Random(999L)

        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate.toFloat()
            val normT = t / durationSec

            // Rapid exponential frequency decay
            val freq = 2300.0 * exp(-5.0 * normT) + 75.0
            val deltaPhase = 2.0 * PI * freq / sampleRate
            phase += deltaPhase

            // 38 Hz mechanical wobble tremolo
            val wobble = 0.65 + 0.35 * sin(2.0 * PI * 38.0 * t)

            val tone = sin(phase) + 0.3 * sin(phase * 2.0)
            val airWhoosh = (random.nextDouble() * 2.0 - 1.0) * exp(-2.5 * normT)

            val env = exp(-2.2 * normT).toFloat().coerceIn(0.0f, 1.0f)
            val mixed = (tone * 0.68 + airWhoosh * 0.32) * wobble * env * 0.90
            val sampleInt = (mixed * 32767.0).toInt().coerceIn(-32767, 32767)
            result[i] = sampleInt.toShort()
        }
        return result
    }

    private fun writeWavFile(file: File, sampleRate: Int, samples: ShortArray) {
        val totalAudioLen = samples.size * 2
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * 2 // 16-bit mono

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size
        header.putShort(1.toShort()) // PCM format
        header.putShort(1.toShort()) // Mono
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(2.toShort()) // BlockAlign
        header.putShort(16.toShort()) // BitsPerSample
        header.put("data".toByteArray())
        header.putInt(totalAudioLen)

        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val sampleBytes = ByteBuffer.allocate(totalAudioLen).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                sampleBytes.putShort(s)
            }
            fos.write(sampleBytes.array())
        }
    }

    /**
     * Triggers zero-latency scratch playback modulated by rotation velocity and stroke direction.
     * @param isForward True if rotating clockwise (forward track motion), False for counter-clockwise
     * @param velocity Angular velocity magnitude (typical range 0.5 to 25.0)
     */
    fun playScratch(isForward: Boolean, velocity: Float) {
        val sp = soundPool ?: return
        val now = System.currentTimeMillis()

        // Debounce ultrafast duplicate calls (< 25ms) unless strong stroke
        if (now - lastPlayTimeMs < 25L && kotlin.math.abs(velocity) < 6.0f) {
            return
        }
        lastPlayTimeMs = now

        val absVel = kotlin.math.abs(velocity)
        val rate = (0.75f + absVel * 0.05f).coerceIn(0.55f, 2.0f)
        val volume = (0.35f + absVel * 0.045f).coerceIn(0.20f, 1.0f)

        // Select acoustic buffer
        val (soundId, isReady) = when {
            velocity < -13.0f -> Pair(spinSoundId, spinLoaded)
            isForward -> Pair(chirpSoundId, chirpLoaded)
            else -> Pair(rubSoundId, rubLoaded)
        }

        if (!isReady || soundId == 0) return

        if (activeStreamId != 0) {
            sp.stop(activeStreamId)
        }

        activeStreamId = sp.play(soundId, volume, volume, 1, 0, rate)
    }

    /**
     * Stops currently sounding scratch stream immediately.
     */
    fun stopScratch() {
        if (activeStreamId != 0) {
            soundPool?.stop(activeStreamId)
            activeStreamId = 0
        }
    }

    /**
     * Releases SoundPool resources when exiting DJ mode.
     */
    fun release() {
        stopScratch()
        soundPool?.release()
        soundPool = null
        chirpLoaded = false
        rubLoaded = false
        spinLoaded = false
    }
}
