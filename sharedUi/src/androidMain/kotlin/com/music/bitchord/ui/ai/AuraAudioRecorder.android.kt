package com.music.bitchord.ui.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AndroidAudioRecorder(
    private val context: Context,
    private val onAskPermission: () -> Unit,
) : AuraAudioRecorder {

    override val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    override fun requestPermission() {
        onAskPermission()
    }

    override suspend fun recordSample(durationMs: Long): ByteArray? = withContext(Dispatchers.IO) {
        if (!hasPermission) {
            return@withContext null
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBufferSize.coerceAtLeast(sampleRate * 2)

        var record: AudioRecord? = null
        try {
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext null
            }

            record.startRecording()
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(minBufferSize.coerceAtLeast(1024))
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < durationMs) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                }
            }

            val pcmData = outputStream.toByteArray()
            if (pcmData.isEmpty()) return@withContext null

            createWavHeader(pcmData, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)
        } catch (e: Exception) {
            null
        } finally {
            try {
                record?.stop()
                record?.release()
            } catch (_: Exception) {}
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
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat 1 = PCM
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
    val context = LocalContext.current
    var hasPermissionState by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermissionState = granted
    }

    return remember(context, hasPermissionState) {
        AndroidAudioRecorder(
            context = context,
            onAskPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        )
    }
}
