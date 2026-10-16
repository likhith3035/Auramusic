package com.music.bitchord.playback

import com.music.bitchord.data.MonotonicClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mode of the smart sleep timer. */
sealed interface SleepTimerMode {
    data object Off : SleepTimerMode
    data class Duration(val totalMinutes: Int) : SleepTimerMode
    data class SongCount(val totalSongs: Int) : SleepTimerMode
}

/** Operational status of the sleep timer. */
enum class SleepTimerStatus {
    OFF,
    RUNNING,
    PAUSED,
    FINISHING_CURRENT_SONG,
}

/**
 * Universal Smart Sleep Timer and Listening Controller.
 * Shared across Android and Desktop.
 */
object SmartSleepTimer {

    val TIME_PRESETS = listOf(5, 10, 15, 30, 45, 60, 90, 120)
    val SONG_PRESETS = listOf(1, 3, 5, 10, 15)
    val INACTIVITY_PRESETS = listOf(15, 30, 45, 60)

    private val _mode = MutableStateFlow<SleepTimerMode>(SleepTimerMode.Off)
    val mode: StateFlow<SleepTimerMode> = _mode.asStateFlow()

    private val _status = MutableStateFlow(SleepTimerStatus.OFF)
    val status: StateFlow<SleepTimerStatus> = _status.asStateFlow()

    /** Deadline on [MonotonicClock.nowMs], or null when not in active countdown. */
    private val _deadlineMs = MutableStateFlow<Long?>(null)
    val deadlineMs: StateFlow<Long?> = _deadlineMs.asStateFlow()

    /** Chosen preset minutes, or null if off/custom/track-based. */
    val minutes = MutableStateFlow<Int?>(null)

    /** Frozen remaining ms when timer is paused. */
    private var pausedDurationRemainingMs: Long? = null

    /** Finish current playing track completely before stopping playback. */
    val finishCurrentSongOnExpiry = MutableStateFlow(true)

    /** Signal indicating the player must stop after the currently playing track ends. */
    val shouldStopAfterCurrentTrack = MutableStateFlow(false)

    // Track-based counting
    val completedSongs = MutableStateFlow(0)
    val remainingSongs = MutableStateFlow(0)

    // Inactivity detection
    val inactivityDetectionEnabled = MutableStateFlow(false)
    val inactivityThresholdMinutes = MutableStateFlow(30)
    val lastActivityMs = MutableStateFlow(MonotonicClock.nowMs())

    val isActive: Boolean
        get() = _status.value != SleepTimerStatus.OFF

    val isPaused: Boolean
        get() = _status.value == SleepTimerStatus.PAUSED

    /** Start a time-based duration countdown. */
    fun startDuration(minutes: Int) {
        require(minutes > 0) { "Minutes must be > 0" }
        cancel()
        _mode.value = SleepTimerMode.Duration(minutes)
        _status.value = SleepTimerStatus.RUNNING
        this.minutes.value = minutes
        _deadlineMs.value = MonotonicClock.nowMs() + (minutes * 60_000L)
        pausedDurationRemainingMs = null
        recordUserActivity()
    }

    /** Add extra minutes to an active duration timer. */
    fun addMinutes(minutes: Int) {
        if (_status.value == SleepTimerStatus.OFF) {
            startDuration(minutes)
            return
        }
        val currentRemaining = remainingMs() ?: 0L
        val newRemaining = currentRemaining + (minutes * 60_000L)
        if (_status.value == SleepTimerStatus.PAUSED) {
            pausedDurationRemainingMs = newRemaining
        } else {
            _deadlineMs.value = MonotonicClock.nowMs() + newRemaining
            _status.value = SleepTimerStatus.RUNNING
            shouldStopAfterCurrentTrack.value = false
        }
        recordUserActivity()
    }

    /** Start a song count timer. */
    fun startSongCount(totalSongs: Int) {
        require(totalSongs > 0) { "Total songs must be > 0" }
        cancel()
        _mode.value = SleepTimerMode.SongCount(totalSongs)
        _status.value = SleepTimerStatus.RUNNING
        this.minutes.value = null
        completedSongs.value = 0
        remainingSongs.value = totalSongs
        shouldStopAfterCurrentTrack.value = (totalSongs == 1)
        recordUserActivity()
    }

    /** Start "Stop after this song" (shortcut for 1 song). */
    fun startAfterCurrentSong() {
        startSongCount(1)
    }

    /** Pause active timer. */
    fun pause(): Boolean {
        if (_status.value != SleepTimerStatus.RUNNING) return false
        when (_mode.value) {
            is SleepTimerMode.Duration -> {
                pausedDurationRemainingMs = remainingMs()
                _deadlineMs.value = null
                _status.value = SleepTimerStatus.PAUSED
            }
            is SleepTimerMode.SongCount -> {
                _status.value = SleepTimerStatus.PAUSED
            }
            SleepTimerMode.Off -> return false
        }
        recordUserActivity()
        return true
    }

    /** Resume paused timer. */
    fun resume(): Boolean {
        if (_status.value != SleepTimerStatus.PAUSED) return false
        when (_mode.value) {
            is SleepTimerMode.Duration -> {
                val remaining = pausedDurationRemainingMs ?: 0L
                _deadlineMs.value = MonotonicClock.nowMs() + remaining
                _status.value = SleepTimerStatus.RUNNING
                pausedDurationRemainingMs = null
            }
            is SleepTimerMode.SongCount -> {
                _status.value = SleepTimerStatus.RUNNING
            }
            SleepTimerMode.Off -> return false
        }
        recordUserActivity()
        return true
    }

    /** Cancel all sleep timer modes. */
    fun cancel() {
        _mode.value = SleepTimerMode.Off
        _status.value = SleepTimerStatus.OFF
        _deadlineMs.value = null
        this.minutes.value = null
        pausedDurationRemainingMs = null
        shouldStopAfterCurrentTrack.value = false
        completedSongs.value = 0
        remainingSongs.value = 0
        recordUserActivity()
    }

    /** Returns remaining milliseconds for time-based timer, or null. */
    fun remainingMs(): Long? {
        if (_status.value == SleepTimerStatus.PAUSED) return pausedDurationRemainingMs
        val dl = _deadlineMs.value ?: return null
        return (dl - MonotonicClock.nowMs()).coerceAtLeast(0L)
    }

    /**
     * Called when the time countdown reaches zero.
     * @param isCurrentlyPlaying whether music is playing right now.
     * @return true if playback should pause immediately, false if waiting for current song to finish.
     */
    fun onDurationCountdownExpired(isCurrentlyPlaying: Boolean): Boolean {
        if (_status.value != SleepTimerStatus.RUNNING) return false
        if (!isCurrentlyPlaying || !finishCurrentSongOnExpiry.value) {
            cancel()
            return true
        }
        // Gracefully finish current song
        _status.value = SleepTimerStatus.FINISHING_CURRENT_SONG
        shouldStopAfterCurrentTrack.value = true
        _deadlineMs.value = null
        return false
    }

    /**
     * Called by playback service when a track actually ends (natural completion).
     * @return true if playback should stop now.
     */
    fun onTrackCompleted(): Boolean {
        if (_status.value == SleepTimerStatus.FINISHING_CURRENT_SONG || shouldStopAfterCurrentTrack.value) {
            cancel()
            return true
        }
        if (_status.value != SleepTimerStatus.RUNNING) return false

        val m = _mode.value
        if (m is SleepTimerMode.SongCount) {
            val done = completedSongs.value + 1
            completedSongs.value = done
            val left = (m.totalSongs - done).coerceAtLeast(0)
            remainingSongs.value = left
            if (left == 1) {
                // Next track is the final track: arm non-crossfade stop
                shouldStopAfterCurrentTrack.value = true
            } else if (left <= 0) {
                cancel()
                return true
            }
        }
        return false
    }

    /** Reset inactivity counter whenever user touches or controls the player. */
    fun recordUserActivity() {
        lastActivityMs.value = MonotonicClock.nowMs()
    }

    fun setInactivityDetectionEnabled(enabled: Boolean) {
        inactivityDetectionEnabled.value = enabled
        recordUserActivity()
    }

    fun setInactivityThresholdMinutes(minutes: Int) {
        require(minutes > 0)
        inactivityThresholdMinutes.value = minutes
        recordUserActivity()
    }

    fun checkInactivityExceeded(currentMs: Long = MonotonicClock.nowMs()): Boolean {
        if (!inactivityDetectionEnabled.value) return false
        val thresholdMs = inactivityThresholdMinutes.value * 60_000L
        return (currentMs - lastActivityMs.value) >= thresholdMs
    }

    /** Formats ms to MM:SS or HH:MM:SS string. */
    fun formatRemaining(remainingMs: Long): String {
        val totalSecs = (remainingMs / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            val hh = hours.toString()
            val mm = if (mins < 10) "0$mins" else mins.toString()
            val ss = if (secs < 10) "0$secs" else secs.toString()
            "$hh:$mm:$ss"
        } else {
            val mm = if (mins < 10) "0$mins" else mins.toString()
            val ss = if (secs < 10) "0$secs" else secs.toString()
            "$mm:$ss"
        }
    }
}
