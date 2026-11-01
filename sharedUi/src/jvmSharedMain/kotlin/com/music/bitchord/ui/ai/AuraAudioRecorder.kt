package com.music.bitchord.ui.ai

import androidx.compose.runtime.Composable

/**
 * Platform audio recorder interface for capturing acoustic ambient music and hum samples.
 */
interface AuraAudioRecorder {
    val hasPermission: Boolean
    fun requestPermission()
    suspend fun recordSample(durationMs: Long = 4000): ByteArray?
}

@Composable
expect fun rememberAudioRecorder(): AuraAudioRecorder
