package com.music.bitchord.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sonic branding audio cue and tactile haptic chime for AuraMusic app launches.
 *
 * Synthesizes a warm, elegant harmonic chord sweep (F# maj9 / F#3 + C#4 + F#4 + C#5)
 * in pure 16-bit 44.1kHz PCM with a smooth 25ms cosine attack and exponential decay,
 * paired with a subtle haptic vibration tick on supported devices.
 */
object AuraIntroAudioCue {
    private const val TAG = "AuraIntroAudioCue"
    private const val SAMPLE_RATE = 44100
    private const val DURATION_SECONDS = 0.40f // 400ms chime

    // In-memory cache of the synthesized PCM buffer
    @Volatile
    private var cachedChimePcm: ShortArray? = null

    /**
     * Synthesizes the harmonic chime waveform into a short array.
     */
    private fun generateChimePcm(): ShortArray {
        val numSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val pcm = ShortArray(numSamples)

        // Musical frequencies (F# major chord structure with crystalline harmonics)
        val f1 = 185.00 // F#3 (warm sub foundation)
        val f2 = 277.18 // C#4 (perfect fifth body)
        val f3 = 369.99 // F#4 (octave resonance)
        val f4 = 554.37 // C#5 (crystalline high bell)

        val attackSamples = (SAMPLE_RATE * 0.025f).toInt() // 25ms anti-click attack

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // Harmonics synthesis
            val s1 = sin(2.0 * PI * f1 * t) * 0.36
            val s2 = sin(2.0 * PI * f2 * t) * 0.30
            val s3 = sin(2.0 * PI * f3 * t) * 0.22
            val s4 = sin(2.0 * PI * f4 * t) * 0.12

            val combined = s1 + s2 + s3 + s4

            // Envelope: 25ms cosine attack ramp followed by natural exponential decay
            val envelope = when {
                i < attackSamples -> 0.5 * (1.0 - cos(PI * i / attackSamples))
                else -> {
                    val decayT = (i - attackSamples).toDouble() / (numSamples - attackSamples)
                    exp(-3.5 * decayT)
                }
            }

            val sample = (combined * envelope).coerceIn(-1.0, 1.0)
            pcm[i] = (sample * 32767.0).toInt().toShort()
        }

        return pcm
    }

    /**
     * Plays the warm Aura startup chime and triggers a subtle haptic tick.
     * Guaranteed safe, non-blocking, and never crashes.
     */
    fun playChime(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // 1. Trigger subtle haptic pulse
                triggerHaptic(context)

                // 2. Retrieve or synthesize PCM data
                val pcm = cachedChimePcm ?: generateChimePcm().also { cachedChimePcm = it }

                // 3. Configure AudioTrack for sonification
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                val bufferSize = maxOf(minBufferSize, pcm.size * 2)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()

                // Release AudioTrack after playback finishes
                kotlinx.coroutines.delay((DURATION_SECONDS * 1000).toLong() + 100L)
                try {
                    track.stop()
                    track.release()
                } catch (ignored: Throwable) {}
            } catch (e: Throwable) {
                Log.w(TAG, "Non-fatal startup chime failure", e)
            }
        }
    }

    private fun triggerHaptic(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(15L)
                }
            }
        } catch (ignored: Throwable) {}
    }
}
