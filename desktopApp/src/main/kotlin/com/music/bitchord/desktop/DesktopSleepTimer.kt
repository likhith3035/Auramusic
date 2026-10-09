package com.music.bitchord.desktop

import com.music.bitchord.playback.SmartSleepTimer
import kotlinx.coroutines.flow.StateFlow

/** Desktop equivalent of Android's sleep timer, delegating to [SmartSleepTimer]. */
object DesktopSleepTimer {

    val minutes: StateFlow<Int?> get() = SmartSleepTimer.minutes
    val afterTrack: StateFlow<Boolean> get() = SmartSleepTimer.shouldStopAfterCurrentTrack

    val presets = listOf(15, 30, 45, 60)

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

    fun isExpired(): Boolean = SmartSleepTimer.remainingMs()?.let { it <= 0L } ?: false
}
