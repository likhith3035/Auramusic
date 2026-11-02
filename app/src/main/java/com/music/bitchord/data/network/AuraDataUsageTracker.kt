package com.music.bitchord.data.network

import android.net.TrafficStats
import android.os.Process
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.MusicVideoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Real-time network telemetry and data usage accounting for AuraMusic.
 * Tracks bytes transferred across Cellular vs Wi-Fi and attributes them
 * to Video streaming, Studio Audio, or Artwork/Caches.
 * Zero Unicode emojis - strict vector and typography design.
 */
object AuraDataUsageTracker {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var trackingJob: Job? = null
    private var lastUidRx: Long = -1L
    private var lastUidTx: Long = -1L

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    fun start() {
        if (trackingJob != null) return
        _isTracking.value = true

        val initialRx = TrafficStats.getUidRxBytes(Process.myUid())
        val initialTx = TrafficStats.getUidTxBytes(Process.myUid())
        if (initialRx != TrafficStats.UNSUPPORTED.toLong() && initialRx >= 0) {
            lastUidRx = initialRx
        }
        if (initialTx != TrafficStats.UNSUPPORTED.toLong() && initialTx >= 0) {
            lastUidTx = initialTx
        }

        trackingJob = scope.launch {
            while (isActive) {
                delay(2500L)
                measureTick()
            }
        }
    }

    private fun measureTick() {
        val currentRx = TrafficStats.getUidRxBytes(Process.myUid())
        val currentTx = TrafficStats.getUidTxBytes(Process.myUid())

        if (currentRx == TrafficStats.UNSUPPORTED.toLong() || currentRx < 0) return

        if (lastUidRx >= 0 && currentRx >= lastUidRx) {
            val deltaRx = currentRx - lastUidRx
            val deltaTx = if (currentTx >= lastUidTx && lastUidTx >= 0) currentTx - lastUidTx else 0L
            val totalDelta = deltaRx + deltaTx

            if (totalDelta > 0) {
                val quality = AuraNetworkMonitor.quality.value
                val isCellular = quality.isMetered || !quality.isWifi
                val isVideo = MusicVideoEngine.playing.value

                var cellularDelta = 0L
                var wifiDelta = 0L
                var videoDelta = 0L
                var audioDelta = 0L
                var otherDelta = 0L

                if (isCellular) {
                    cellularDelta = totalDelta
                } else {
                    wifiDelta = totalDelta
                }

                if (isVideo) {
                    videoDelta = totalDelta
                } else if (totalDelta > 80_000) {
                    // Chunk typical of an audio stream chunk
                    audioDelta = totalDelta
                } else {
                    otherDelta = totalDelta
                }

                AppSettings.addDataUsage(
                    cellularDelta = cellularDelta,
                    wifiDelta = wifiDelta,
                    videoDelta = videoDelta,
                    audioDelta = audioDelta,
                    otherDelta = otherDelta,
                )
            }
        }

        lastUidRx = currentRx
        if (currentTx >= 0) lastUidTx = currentTx
    }

    /** Record bandwidth saved (e.g., from local cache hit or 360p downscale) */
    fun recordSaved(bytes: Long) {
        if (bytes > 0) {
            AppSettings.addSavedBytes(bytes)
        }
    }

    /** Format byte count into human-readable metric string */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0.0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}

