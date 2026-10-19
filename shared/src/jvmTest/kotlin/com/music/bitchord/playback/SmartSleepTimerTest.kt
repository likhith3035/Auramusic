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

    @Test
    fun testStartAfterCurrentSong() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startAfterCurrentSong()
        assertTrue(SmartSleepTimer.isActive)
        assertTrue(SmartSleepTimer.shouldStopAfterCurrentTrack.value)
        assertEquals(SleepTimerMode.SongCount(1), SmartSleepTimer.mode.value)
        val shouldStop = SmartSleepTimer.onTrackCompleted()
        assertTrue(shouldStop)
        assertEquals(SleepTimerStatus.OFF, SmartSleepTimer.status.value)
    }

    @Test
    fun testVolumeFadeOutCurve() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startDuration(minutes = 10)
        SmartSleepTimer.fadeOutEnabled.value = true
        SmartSleepTimer.fadeOutDurationSeconds.value = 30

        // Beyond 30s: volume must be 1.0 (full volume)
        val vol1 = SmartSleepTimer.updateVolumeMultiplier(remainingMs = 45_000L)
        assertEquals(1.0f, vol1)

        // Midway through fade window (15s of 30s): volume should be ~0.5
        val vol2 = SmartSleepTimer.updateVolumeMultiplier(remainingMs = 15_000L)
        assertTrue(vol2 in 0.45f..0.55f, "Expected mid-fade volume near 0.5, got $vol2")

        // At end (0s): volume must be 0.0
        val vol3 = SmartSleepTimer.updateVolumeMultiplier(remainingMs = 0L)
        assertEquals(0.0f, vol3)

        // Reset or cancel restores 1.0
        SmartSleepTimer.cancel()
        assertEquals(1.0f, SmartSleepTimer.volumeMultiplier.value)
    }

    @Test
    fun testVolumeFadeOutDisabled() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startDuration(minutes = 10)
        SmartSleepTimer.fadeOutEnabled.value = false

        // Within 10s: since fadeOut is disabled, volume remains 1.0f
        val vol = SmartSleepTimer.updateVolumeMultiplier(remainingMs = 10_000L)
        assertEquals(1.0f, vol)
    }
}
