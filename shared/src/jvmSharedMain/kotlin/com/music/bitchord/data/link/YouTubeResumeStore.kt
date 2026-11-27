package com.music.bitchord.data.link

import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.durationMillis
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight persisted resume-position store for long YouTube videos (podcasts, mixes, lectures).
 * Short music tracks (< 10 min) are intentionally ignored so normal songs always start at 0:00.
 */
object YouTubeResumeStore {

    const val MIN_TRACK_DURATION_MS = 10L * 60L * 1000L
    private const val MIN_RESUME_POSITION_MS = 15_000L
    private const val COMPLETE_REMAINING_MS = 20_000L
    private const val COMPLETE_RATIO = 0.96
    private const val MAX_ENTRIES = 120
    private const val SAVE_THROTTLE_MS = 5_000L
    private const val FILE_NAME = "youtube_resume_positions.json"

    @Serializable
    data class ResumeEntry(
        val positionMs: Long,
        val durationMs: Long,
        val updatedAtMs: Long,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val entries = ConcurrentHashMap<String, ResumeEntry>()
    private val lastFlushAtMs = ConcurrentHashMap<String, Long>()
    private val lock = Any()

    @Volatile
    private var file: File? = null

    fun init(directory: File) {
        synchronized(lock) {
            directory.mkdirs()
            val target = File(directory, FILE_NAME)
            file = target
            entries.clear()
            if (target.isFile) {
                runCatching {
                    val decoded = json.decodeFromString<Map<String, ResumeEntry>>(target.readText())
                    entries.putAll(decoded)
                }
            }
        }
    }

    /** True when [song] is a pasted/shared long non-music video that merits resume memory and standalone end behavior. */
    fun isEligibleLongVideo(song: Song?, knownDurationMs: Long = 0L): Boolean {
        song ?: return false
        if (!song.isVideo && !song.isVideoOrigin) return false
        val duration = maxOf(song.durationMillis(), knownDurationMs)
        return duration >= MIN_TRACK_DURATION_MS
    }

    /** True when [song] should finish standalone rather than seeding music AutoPlay radio behind it. */
    fun isStandaloneSharedMedia(song: Song?, knownDurationMs: Long = 0L): Boolean {
        song ?: return false
        if (song.playbackSourceType != PlaybackSourceType.SHARED_LINK) return false
        return isEligibleLongVideo(song, knownDurationMs)
    }

    /** Saved position in milliseconds for [videoId], or null if none / below resume floor. */
    fun savedPositionMs(videoId: String): Long? {
        val entry = entries[videoId] ?: return null
        return entry.positionMs.takeIf { it >= MIN_RESUME_POSITION_MS }
    }

    /** Convenience helper that inspects duration from both [song] metadata and live [playerDurationMs]. */
    fun recordSongProgress(
        song: Song?,
        positionMs: Long,
        playerDurationMs: Long = 0L,
        forceFlush: Boolean = false,
    ) {
        song ?: return
        if (!isEligibleLongVideo(song, playerDurationMs)) return
        val duration = maxOf(song.durationMillis(), playerDurationMs)
        recordProgress(
            videoId = song.videoId,
            positionMs = positionMs,
            durationMs = duration,
            forceFlush = forceFlush,
        )
    }

    /** Records playback progress for [videoId], clearing automatically once nearly complete. */
    fun recordProgress(
        videoId: String,
        positionMs: Long,
        durationMs: Long,
        forceFlush: Boolean = false,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        if (videoId.isBlank() || durationMs < MIN_TRACK_DURATION_MS) return
        val nearEnd = (durationMs - positionMs) <= COMPLETE_REMAINING_MS ||
            positionMs.toDouble() / durationMs.toDouble() >= COMPLETE_RATIO
        if (nearEnd) {
            clear(videoId)
            return
        }
        if (positionMs < MIN_RESUME_POSITION_MS) return

        val previous = entries[videoId]
        entries[videoId] = ResumeEntry(
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAtMs = nowMs,
        )
        if (entries.size > MAX_ENTRIES) {
            entries.entries
                .sortedBy { it.value.updatedAtMs }
                .take(entries.size - MAX_ENTRIES)
                .forEach { entries.remove(it.key) }
        }
        val lastFlush = lastFlushAtMs[videoId] ?: 0L
        if (forceFlush || previous == null || nowMs - lastFlush >= SAVE_THROTTLE_MS) {
            lastFlushAtMs[videoId] = nowMs
            flush()
        }
    }

    fun clear(videoId: String) {
        val removed = entries.remove(videoId)
        lastFlushAtMs.remove(videoId)
        if (removed != null) flush()
    }

    fun resetForTests() {
        synchronized(lock) {
            file = null
            entries.clear()
            lastFlushAtMs.clear()
        }
    }

    private fun flush() {
        val target = file ?: return
        synchronized(lock) {
            runCatching {
                val tmp = File(target.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(json.encodeToString(entries.toMap()))
                if (!tmp.renameTo(target)) {
                    target.writeText(tmp.readText())
                    tmp.delete()
                }
            }
        }
    }
}
