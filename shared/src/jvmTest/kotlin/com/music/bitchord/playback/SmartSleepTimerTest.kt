package com.music.bitchord.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmartSleepTimerTest {

    @Test
    fun testInitialStateIsOff() {
        SmartSleepTimer.cancel()
        assertEquals(SleepTimerMode.Off, SmartSleepTimer.mode.value)
        assertEquals(SleepTimerStatus.OFF, SmartSleepTimer.status.value)
        assertFalse(SmartSleepTimer.isActive)
        assertNull(SmartSleepTimer.remainingMs())
    }

    @Test
    fun testStartDurationTimer() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startDuration(minutes = 30)
        assertTrue(SmartSleepTimer.isActive)
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
        val mode = SmartSleepTimer.mode.value as SleepTimerMode.Duration
        assertEquals(30, mode.totalMinutes)
        assertNotNull(SmartSleepTimer.remainingMs())
        assertTrue(SmartSleepTimer.remainingMs()!! > 0)
    }

    @Test
    fun testPauseAndResumeDurationTimer() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startDuration(minutes = 15)
        assertTrue(SmartSleepTimer.pause())
        assertEquals(SleepTimerStatus.PAUSED, SmartSleepTimer.status.value)
        val pausedRemaining = SmartSleepTimer.remainingMs()
        assertNotNull(pausedRemaining)

        assertTrue(SmartSleepTimer.resume())
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
    }

    @Test
    fun testStartSongCountTimer() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startSongCount(totalSongs = 5)
        assertTrue(SmartSleepTimer.isActive)
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
        val mode = SmartSleepTimer.mode.value as SleepTimerMode.SongCount
        assertEquals(5, mode.totalSongs)
        assertEquals(0, SmartSleepTimer.completedSongs.value)
        assertEquals(5, SmartSleepTimer.remainingSongs.value)

        // Song finishes
        val shouldStop1 = SmartSleepTimer.onTrackCompleted()
        assertFalse(shouldStop1)
        assertEquals(1, SmartSleepTimer.completedSongs.value)
        assertEquals(4, SmartSleepTimer.remainingSongs.value)
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
    }

    @Test
    fun testSongCountFinalTrackTransitionsToFinishing() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startSongCount(totalSongs = 1)
        // 1 song total: from start, remaining is 1, so the current track is already the final track
        assertTrue(SmartSleepTimer.shouldStopAfterCurrentTrack.value)

        // Once completed, it should fire stop
        val shouldStop = SmartSleepTimer.onTrackCompleted()
        assertTrue(shouldStop)
        assertEquals(SleepTimerStatus.OFF, SmartSleepTimer.status.value)
    }

    @Test
    fun testInactivityDetectionTrigger() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.setInactivityDetectionEnabled(true)
        SmartSleepTimer.setInactivityThresholdMinutes(15)
        SmartSleepTimer.recordUserActivity()

        assertFalse(SmartSleepTimer.checkInactivityExceeded(currentMs = SmartSleepTimer.lastActivityMs.value + 10 * 60_000L))
        assertTrue(SmartSleepTimer.checkInactivityExceeded(currentMs = SmartSleepTimer.lastActivityMs.value + 16 * 60_000L))
    }
}
