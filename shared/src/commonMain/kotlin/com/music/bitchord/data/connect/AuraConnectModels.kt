package com.music.bitchord.data.connect

import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable

/** Device category for Aura Connect LAN discovery and remote control. */
@Serializable
enum class AuraDeviceType {
    DESKTOP,
    TV,
    MOBILE,
}

/** Discovered or paired Aura Connect hardware node. */
@Serializable
data class AuraConnectDevice(
    val id: String,
    val name: String,
    val deviceType: AuraDeviceType,
    val host: String,
    val port: Int = 8765,
    val lastSeenMs: Long = 0L,
)

/** Lightweight serializable track model exchanged over the local network. */
@Serializable
data class ConnectTrack(
    val videoId: String,
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val durationMs: Long? = null,
    val isSmartRecommendation: Boolean = false,
)

fun Song.toConnectTrack(durationMs: Long? = null): ConnectTrack = ConnectTrack(
    videoId = videoId,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationText = durationText,
    durationMs = durationMs,
    isSmartRecommendation = isSmartRecommendation,
)

fun ConnectTrack.toSong(): Song = Song(
    videoId = videoId,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationText = durationText,
    isSmartRecommendation = isSmartRecommendation,
    queueTier = QueueTier.CONTEXT,
)

/** Live playback state synchronized across devices. */
@Serializable
data class AuraPlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val repeatMode: String = "OFF",
    val shuffleMode: String = "OFF",
    val eqPreset: String = "FLAT",
    val audioQuality: String = "320 kbps AAC",
    val track: ConnectTrack? = null,
    val queue: List<ConnectTrack> = emptyList(),
    val queueIndex: Int = -1,
    val updatedAtMs: Long = 0L,
)

/** Full status payload returned by the host. */
@Serializable
data class AuraStatusResponse(
    val device: AuraConnectDevice,
    val playback: AuraPlaybackState,
)

/** Active playback session transferred between devices. */
@Serializable
data class AuraTransferSession(
    val track: ConnectTrack? = null,
    val positionMs: Long = 0L,
    val queue: List<ConnectTrack> = emptyList(),
    val queueIndex: Int = 0,
    val isPlaying: Boolean = true,
)

/** Remote control command sent to a host device. */
@Serializable
data class AuraCommandRequest(
    val action: String,
    val positionMs: Long? = null,
    val volume: Float? = null,
    val index: Int? = null,
    val fromIndex: Int? = null,
    val toIndex: Int? = null,
    val track: ConnectTrack? = null,
    val playNext: Boolean? = null,
    val repeatMode: String? = null,
    val shuffleMode: String? = null,
    val eqPreset: String? = null,
    val isMuted: Boolean? = null,
    val session: AuraTransferSession? = null,
)

@Serializable
data class AuraPairRequest(
    val pin: String,
    val clientId: String,
    val clientName: String = "Aura Mobile",
)

@Serializable
data class AuraPairResponse(
    val token: String,
    val device: AuraConnectDevice,
)

@Serializable
data class AuraErrorResponse(
    val error: String,
    val code: String = "error",
)

/** Constants for Aura Connect network ports and discovery. */
object AuraConnectConstants {
    const val DEFAULT_HTTP_PORT = 8765
    const val DISCOVERY_UDP_PORT = 8764
    const val DISCOVERY_MAGIC = "AURA_CONNECT_DISCOVERY"
    const val BEACON_MAGIC = "AURA_CONNECT_BEACON"
}
