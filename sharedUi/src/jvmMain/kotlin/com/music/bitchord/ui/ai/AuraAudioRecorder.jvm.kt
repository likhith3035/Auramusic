package com.music.bitchord.ui.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class JvmAudioRecorder : AuraAudioRecorder {
    override val hasPermission: Boolean = true

    override fun requestPermission() {}

    override suspend fun recordSample(durationMs: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 16000f
            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) {
                return@withContext null
            }

            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format)
            line.start()

            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < durationMs) {
                val count = line.read(buffer, 0, buffer.size)
                if (count > 0) {
                    outputStream.write(buffer, 0, count)
                }
            }

            line.stop()
            line.close()

            val pcmData = outputStream.toByteArray()
            if (pcmData.isEmpty()) return@withContext null

            createWavHeader(pcmData, sampleRate = 16000, channels = 1, bitsPerSample = 16)
        } catch (e: Exception) {
            null
        }
    }

    private fun createWavHeader(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Short,
        bitsPerSample: Short,
    ): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val buffer = ByteBuffer.allocate(44 + pcmData.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels)
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * bitsPerSample / 8).toShort())
        buffer.putShort(bitsPerSample)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmData.size)
        buffer.put(pcmData)
        return buffer.array()
    }
}

@Composable
actual fun rememberAudioRecorder(): AuraAudioRecorder {
    return remember { JvmAudioRecorder() }
}
