package com.music.bitchord.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * Android compatibility wrapper delegating to [SmartSleepTimer].
 *
 * Preserves the exact public API for callers while backing it with the
 * unified Smart Sleep Timer system.
 */
object SleepTimer {

    val deadline: StateFlow<Long?> get() = SmartSleepTimer.deadlineMs

    val minutes: StateFlow<Int?> get() = SmartSleepTimer.minutes

    val afterTrack: StateFlow<Boolean> get() = SmartSleepTimer.shouldStopAfterCurrentTrack

    val PRESETS: List<Int> get() = SmartSleepTimer.TIME_PRESETS

    val isRunning: Boolean get() = SmartSleepTimer.isActive

    fun start(minutes: Int) {
        SmartSleepTimer.startDuration(minutes)
    }

    fun startAfterTrack() {
        SmartSleepTimer.startAfterCurrentSong()
    }

    fun cancel() {
        SmartSleepTimer.cancel()
    }

    fun remainingMs(): Long? = SmartSleepTimer.remainingMs()
}
