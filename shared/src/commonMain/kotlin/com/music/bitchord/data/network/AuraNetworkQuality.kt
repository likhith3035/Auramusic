package com.music.bitchord.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkTier(
    val label: String,
    val maxQualityHeight: Int,
    val badge: String,
) {
    FAST("FAST", 1080, "1080P"),
    MODERATE("GOOD", 720, "720P"),
    SLOW("SLOW", 360, "360P"),
    OFFLINE("OFFLINE", 0, "OFFLINE");
}

data class NetworkQuality(
    val tier: NetworkTier = NetworkTier.FAST,
    val isMetered: Boolean = false,
    val bandwidthKbps: Int = 25_000,
    val isWifi: Boolean = true,
)

object AuraNetworkMonitor {
    private val _quality = MutableStateFlow(NetworkQuality())
    val quality: StateFlow<NetworkQuality> = _quality.asStateFlow()

    fun update(
        isOnline: Boolean,
        isMetered: Boolean,
        bandwidthKbps: Int,
        isWifi: Boolean,
    ) {
        val tier = when {
            !isOnline -> NetworkTier.OFFLINE
            bandwidthKbps in 1..2_999 -> NetworkTier.SLOW
            bandwidthKbps in 3_000..12_000 -> NetworkTier.MODERATE
            isMetered && bandwidthKbps < 5_000 -> NetworkTier.SLOW
            else -> NetworkTier.FAST
        }
        _quality.value = NetworkQuality(
            tier = tier,
            isMetered = isMetered,
            bandwidthKbps = bandwidthKbps,
            isWifi = isWifi,
        )
    }

    /** Suggested video quality height based on real-time network throughput */
    fun recommendedHeight(): Int = quality.value.tier.maxQualityHeight
}

