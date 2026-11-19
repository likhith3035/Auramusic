package com.music.bitchord.playback.dj

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.music.bitchord.data.ai.AuraNeuralDjEngine
import com.music.bitchord.data.dj.CrossfaderCurve
import com.music.bitchord.data.dj.DjAudioController
import com.music.bitchord.data.dj.DjBridge
import com.music.bitchord.data.dj.DjSessionState
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackService
import com.music.bitchord.playback.toMediaItem
import com.music.bitchord.playback.toSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Android playback implementation of the Dual-Deck DJ Engine.
 * Blends real audio between the primary session player (Deck A) and the
 * standby ExoPlayer (Deck B) using equal-power trigonometry and cut curves,
 * real 24dB state-variable DSP filters, beat looping, hot cues, and pitch bending.
 */
class DjAudioEngine(
    private val service: PlaybackService,
    private val scope: CoroutineScope,
) : DjAudioController {

    private var pollJob: Job? = null
    private var autoDropJob: Job? = null
    private var nudgeJobA: Job? = null
    private var nudgeJobB: Job? = null

    // Beat loop boundaries (startMs, endMs)
    private var loopA: Pair<Long, Long>? = null
    private var loopB: Pair<Long, Long>? = null

    // Hot cues
    private val hotCuesA = mutableListOf<Long?>(null, null, null)
    private val hotCuesB = mutableListOf<Long?>(null, null, null)

    private val player: ExoPlayer?
        get() = service.getActivePlayer()

    private val spare: ExoPlayer?
        get() = service.getSparePlayer()

    fun attach() {
        DjBridge.install(this)
    }

    override fun startDjMode() {
        DjSessionState.updateIsActive(true)
        DjSessionState.updateCrossfader(0.0f)
        DjSessionState.updateCrossfaderCurve(CrossfaderCurve.SMOOTH)
        DjSessionState.updateIsAutoDropping(false)
        DjSessionState.updateLowKillA(false)
        DjSessionState.updateLowKillB(false)
        DjSessionState.updateDeckASpeed(1.0f)
        DjSessionState.updateDeckBSpeed(1.0f)
        DjSessionState.updateDeckAFilter(0.0f)
        DjSessionState.updateDeckBFilter(0.0f)
        DjSessionState.updateDeckALoopBeats(null)
        DjSessionState.updateDeckBLoopBeats(null)
        DjSessionState.updateIsBeatSynced(false)
        DjSessionState.updateIsHamsterMode(false)
        DjSessionState.updateIsSplitCueEnabled(false)
        DjSessionState.updateIsBrakingA(false)
        DjSessionState.updateIsBrakingB(false)

        loopA = null
        loopB = null
        hotCuesA.fill(null)
        hotCuesB.fill(null)
        DjSessionState.updateDeckAHotCues(hotCuesA.toList())
        DjSessionState.updateDeckBHotCues(hotCuesB.toList())

        // Reset DSP filters
        service.getActiveFilter().open()
        service.getActiveFilter().leftChannelGain = 1.0f
        service.getActiveFilter().rightChannelGain = 1.0f
        service.getSpareFilter().open()
        service.getSpareFilter().leftChannelGain = 1.0f
        service.getSpareFilter().rightChannelGain = 1.0f

        val active = player ?: return
        val currentItem = active.currentMediaItem
        val songA = currentItem?.toSong()
        DjSessionState.updateDeckASong(songA)
        DjSessionState.updateDeckAPlaying(active.isPlaying)
        DjSessionState.updateDeckAPosition(active.currentPosition)
        DjSessionState.updateDeckADuration(active.duration)

        if (songA != null) {
            val profileA = AuraNeuralDjEngine.analyzeSong(songA)
            DjSessionState.updateDeckABpm(profileA.bpm)
            DjSessionState.updateDeckAKey(profileA.key.toString())
        }

        // Collect all items in queue
        val count = active.mediaItemCount
        val queue = (0 until count).map { active.getMediaItemAt(it).toSong() }
        DjSessionState.updateCandidateQueue(queue)

        // Preload Deck B with the next track in queue
        val currentIdx = active.currentMediaItemIndex
        val nextIdx = if (count > 1) {
            if (currentIdx + 1 < count) currentIdx + 1 else 0
        } else {
            currentIdx
        }

        prepareDeckB(nextIdx)
        applyVolumes()
        startPolling()
    }

    private fun prepareDeckB(index: Int) {
        val active = player ?: return
        val standby = spare ?: return
        val count = active.mediaItemCount
        if (count == 0 || index !in 0 until count) return

        val items = (0 until count).map { active.getMediaItemAt(it) }
        val targetItem = items[index]
        val songB = targetItem.toSong()

        DjSessionState.updateDeckBSong(songB)
        DjSessionState.updateDeckBPlaying(false)
        DjSessionState.updateDeckBPosition(0L)
        DjSessionState.updateDeckBDuration(0L)
        loopB = null
        DjSessionState.updateDeckBLoopBeats(null)
        hotCuesB.fill(null)
        DjSessionState.updateDeckBHotCues(hotCuesB.toList())

        val profileB = AuraNeuralDjEngine.analyzeSong(songB)
        DjSessionState.updateDeckBBpm(profileB.bpm)
        DjSessionState.updateDeckBKey(profileB.key.toString())

        standby.setMediaItems(items, index, 0L)
        standby.volume = 0f
        standby.playWhenReady = false
        standby.playbackParameters = PlaybackParameters.DEFAULT
        standby.prepare()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Main) {
            var lastMediaIdA: String? = player?.currentMediaItem?.mediaId
            while (isActive && DjSessionState.isDjActive.value) {
                val p = player
                if (p != null) {
                    val currentA = p.currentPosition
                    DjSessionState.updateDeckAPosition(currentA)
                    DjSessionState.updateDeckADuration(p.duration)
                    DjSessionState.updateDeckAPlaying(p.isPlaying)

                    val curIdA = p.currentMediaItem?.mediaId
                    if (curIdA != null && curIdA != lastMediaIdA) {
                        lastMediaIdA = curIdA
                        val newSongA = p.currentMediaItem?.toSong()
                        DjSessionState.updateDeckASong(newSongA)
                        if (newSongA != null) {
                            val profA = AuraNeuralDjEngine.analyzeSong(newSongA)
                            DjSessionState.updateDeckABpm(profA.bpm)
                            DjSessionState.updateDeckAKey(profA.key.toString())
                        }
                    }

                    // Check Beat Loop for Deck A
                    val activeLoopA = loopA
                    if (activeLoopA != null && p.isPlaying && currentA >= activeLoopA.second) {
                        p.seekTo(activeLoopA.first)
                    }
                }

                val s = spare
                if (s != null) {
                    val currentB = s.currentPosition
                    DjSessionState.updateDeckBPosition(currentB)
                    DjSessionState.updateDeckBDuration(s.duration)
                    DjSessionState.updateDeckBPlaying(s.isPlaying)

                    // Auto-cue to beginning if Deck B finishes
                    if (s.playbackState == Player.STATE_ENDED) {
                        s.seekTo(0L)
                        s.pause()
                        DjSessionState.updateDeckBPosition(0L)
                        DjSessionState.updateDeckBPlaying(false)
                    }

                    // Check Beat Loop for Deck B
                    val activeLoopB = loopB
                    if (activeLoopB != null && s.isPlaying && currentB >= activeLoopB.second) {
                        s.seekTo(activeLoopB.first)
                    }
                }

                delay(50L) // 50ms polling for responsive looping & position tracking
            }
        }
    }

    override fun setCrossfader(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        DjSessionState.updateCrossfader(clamped)

        // If Deck B volume starts rising and Deck B is not playing, auto-start Deck B
        val standby = spare
        if (standby != null && clamped > 0.04f && !standby.isPlaying && !standby.playWhenReady) {
            standby.playWhenReady = true
        }

        applyVolumes()
    }

    override fun setCrossfaderCurve(curve: CrossfaderCurve) {
        DjSessionState.updateCrossfaderCurve(curve)
        applyVolumes()
    }

    fun applyVolumes() {
        val p = player
        val s = spare
        val rawFraction = DjSessionState.crossfader.value
        val fraction = if (DjSessionState.isHamsterMode.value) 1.0f - rawFraction else rawFraction
        val master = service.currentVolumeMultiplier

        val gainA: Float
        val gainB: Float

        when (DjSessionState.crossfaderCurve.value) {
            CrossfaderCurve.SMOOTH -> {
                // Equal-power crossfade curve: cos(x * pi/2) and sin(x * pi/2)
                val angle = fraction * (PI / 2.0).toFloat()
                gainA = cos(angle)
                gainB = sin(angle)
            }
            CrossfaderCurve.CUT -> {
                // Scratch curve: full power on either deck as soon as moved 5% from extreme
                gainA = if (fraction >= 0.95f) 0.0f else 1.0f
                gainB = if (fraction <= 0.05f) 0.0f else 1.0f
            }
        }

        val killA = if (DjSessionState.lowKillA.value) 0.30f else 1.0f
        val killB = if (DjSessionState.lowKillB.value) 0.30f else 1.0f

        val splitCue = DjSessionState.isSplitCueEnabled.value
        if (splitCue) {
            // Deck A goes 100% to Left (Master audience)
            service.getActiveFilter().leftChannelGain = 1.0f
            service.getActiveFilter().rightChannelGain = 0.0f
            // Deck B goes 100% to Right (Cue headphone)
            service.getSpareFilter().leftChannelGain = 0.0f
            service.getSpareFilter().rightChannelGain = 1.0f

            p?.volume = (gainA * killA * master).coerceIn(0f, 1f)
            val monitorVolume = master.coerceIn(0f, 1f)
            s?.volume = (monitorVolume * killB).coerceIn(0f, 1f)
        } else {
            // Stereo normal
            service.getActiveFilter().leftChannelGain = 1.0f
            service.getActiveFilter().rightChannelGain = 1.0f
            service.getSpareFilter().leftChannelGain = 1.0f
            service.getSpareFilter().rightChannelGain = 1.0f

            p?.volume = (gainA * killA * master).coerceIn(0f, 1f)
            s?.volume = (gainB * killB * master).coerceIn(0f, 1f)
        }
    }

    override fun toggleHamsterMode() {
        DjSessionState.updateIsHamsterMode(!DjSessionState.isHamsterMode.value)
        applyVolumes()
    }

    override fun toggleSplitCue() {
        DjSessionState.updateIsSplitCueEnabled(!DjSessionState.isSplitCueEnabled.value)
        applyVolumes()
    }

    override fun triggerBrakeA() {
        val p = player ?: return
        if (!p.isPlaying || DjSessionState.isBrakingA.value) return
        nudgeJobA?.cancel()
        scope.launch(Dispatchers.Main) {
            DjSessionState.updateIsBrakingA(true)
            val initialSpeed = DjSessionState.deckASpeed.value
            val steps = 14
            val stepDelayMs = 45L
            for (i in 1..steps) {
                if (!isActive || !DjSessionState.isDjActive.value) break
                val factor = (steps - i).toFloat() / steps.toFloat()
                val currentSpeed = (initialSpeed * factor).coerceAtLeast(0.04f)
                p.playbackParameters = PlaybackParameters(currentSpeed)
                DjSessionState.updateDeckASpeed(currentSpeed)
                delay(stepDelayMs)
            }
            p.pause()
            p.playbackParameters = PlaybackParameters(1.0f)
            DjSessionState.updateDeckASpeed(1.0f)
            DjSessionState.updateDeckAPlaying(false)
            DjSessionState.updateIsBrakingA(false)
        }
    }

    override fun triggerBrakeB() {
        val s = spare ?: return
        if (!s.isPlaying || DjSessionState.isBrakingB.value) return
        nudgeJobB?.cancel()
        scope.launch(Dispatchers.Main) {
            DjSessionState.updateIsBrakingB(true)
            val initialSpeed = DjSessionState.deckBSpeed.value
            val steps = 14
            val stepDelayMs = 45L
            for (i in 1..steps) {
                if (!isActive || !DjSessionState.isDjActive.value) break
                val factor = (steps - i).toFloat() / steps.toFloat()
                val currentSpeed = (initialSpeed * factor).coerceAtLeast(0.04f)
                s.playbackParameters = PlaybackParameters(currentSpeed)
                DjSessionState.updateDeckBSpeed(currentSpeed)
                delay(stepDelayMs)
            }
            s.pause()
            s.playbackParameters = PlaybackParameters(1.0f)
            DjSessionState.updateDeckBSpeed(1.0f)
            DjSessionState.updateDeckBPlaying(false)
            DjSessionState.updateIsBrakingB(false)
        }
    }

    override fun scrubDeckA(deltaMs: Long) {
        val p = player ?: return
        val newPos = (p.currentPosition + deltaMs).coerceIn(0L, p.duration.coerceAtLeast(0L))
        p.seekTo(newPos)
        DjSessionState.updateDeckAPosition(newPos)
    }

    override fun scrubDeckB(deltaMs: Long) {
        val s = spare ?: return
        val newPos = (s.currentPosition + deltaMs).coerceIn(0L, s.duration.coerceAtLeast(0L))
        s.seekTo(newPos)
        DjSessionState.updateDeckBPosition(newPos)
    }

    override fun nudgePitchA(positive: Boolean) {
        val p = player ?: return
        nudgeJobA?.cancel()
        val baseSpeed = DjSessionState.deckASpeed.value
        val bump = if (positive) 1.04f else 0.96f
        p.playbackParameters = PlaybackParameters(baseSpeed * bump)

        nudgeJobA = scope.launch(Dispatchers.Main) {
            delay(320L)
            p.playbackParameters = PlaybackParameters(baseSpeed)
        }
    }

    override fun nudgePitchB(positive: Boolean) {
        val s = spare ?: return
        nudgeJobB?.cancel()
        val baseSpeed = DjSessionState.deckBSpeed.value
        val bump = if (positive) 1.04f else 0.96f
        s.playbackParameters = PlaybackParameters(baseSpeed * bump)

        nudgeJobB = scope.launch(Dispatchers.Main) {
            delay(320L)
            s.playbackParameters = PlaybackParameters(baseSpeed)
        }
    }

    override fun setDeckAFilter(amount: Float) {
        val clamped = amount.coerceIn(-1.0f, 1.0f)
        DjSessionState.updateDeckAFilter(clamped)
        val filter = service.getActiveFilter()

        if (clamped < -0.02f) {
            // Low-Pass filter sweep (turning left): sweeps cutoff from 20000Hz down to 250Hz
            val factor = kotlin.math.abs(clamped)
            val lowPass = (20000f * kotlin.math.exp(-factor * 4.38f)).coerceIn(250f, 20000f)
            filter.setCutoffs(lowPassHz = lowPass, highPassHz = 20f)
        } else if (clamped > 0.02f) {
            // High-Pass filter sweep (turning right): sweeps cutoff from 20Hz up to 4500Hz
            val highPass = (20f + 4480f * (clamped * clamped)).coerceIn(20f, 4500f)
            filter.setCutoffs(lowPassHz = 20000f, highPassHz = highPass)
        } else {
            filter.open()
        }
    }

    override fun setDeckBFilter(amount: Float) {
        val clamped = amount.coerceIn(-1.0f, 1.0f)
        DjSessionState.updateDeckBFilter(clamped)
        val filter = service.getSpareFilter()

        if (clamped < -0.02f) {
            val factor = kotlin.math.abs(clamped)
            val lowPass = (20000f * kotlin.math.exp(-factor * 4.38f)).coerceIn(250f, 20000f)
            filter.setCutoffs(lowPassHz = lowPass, highPassHz = 20f)
        } else if (clamped > 0.02f) {
            val highPass = (20f + 4480f * (clamped * clamped)).coerceIn(20f, 4500f)
            filter.setCutoffs(lowPassHz = 20000f, highPassHz = highPass)
        } else {
            filter.open()
        }
    }

    override fun toggleDeckALoop(beats: Int) {
        val p = player ?: return
        if (DjSessionState.deckALoopBeats.value == beats) {
            // Toggle off
            loopA = null
            DjSessionState.updateDeckALoopBeats(null)
        } else {
            val bpm = DjSessionState.deckABpm.value.coerceAtLeast(60)
            val beatMs = (60_000.0 / bpm).toLong()
            val loopDuration = beats * beatMs
            val start = p.currentPosition
            loopA = Pair(start, start + loopDuration)
            DjSessionState.updateDeckALoopBeats(beats)
        }
    }

    override fun toggleDeckBLoop(beats: Int) {
        val s = spare ?: return
        if (DjSessionState.deckBLoopBeats.value == beats) {
            // Toggle off
            loopB = null
            DjSessionState.updateDeckBLoopBeats(null)
        } else {
            val bpm = DjSessionState.deckBBpm.value.coerceAtLeast(60)
            val beatMs = (60_000.0 / bpm).toLong()
            val loopDuration = beats * beatMs
            val start = s.currentPosition
            loopB = Pair(start, start + loopDuration)
            DjSessionState.updateDeckBLoopBeats(beats)
        }
    }

    override fun triggerHotCueA(index: Int) {
        if (index !in 0..2) return
        val p = player ?: return
        val existing = hotCuesA[index]
        if (existing == null) {
            // Set hot cue at current position
            val cur = p.currentPosition
            hotCuesA[index] = cur
            DjSessionState.updateDeckAHotCues(hotCuesA.toList())
        } else {
            // Jump to cue and play
            p.seekTo(existing)
            if (!p.isPlaying) p.play()
            DjSessionState.updateDeckAPlaying(true)
        }
    }

    override fun clearHotCueA(index: Int) {
        if (index !in 0..2) return
        hotCuesA[index] = null
        DjSessionState.updateDeckAHotCues(hotCuesA.toList())
    }

    override fun triggerHotCueB(index: Int) {
        if (index !in 0..2) return
        val s = spare ?: return
        val existing = hotCuesB[index]
        if (existing == null) {
            val cur = s.currentPosition
            hotCuesB[index] = cur
            DjSessionState.updateDeckBHotCues(hotCuesB.toList())
        } else {
            s.seekTo(existing)
            if (!s.isPlaying) s.play()
            DjSessionState.updateDeckBPlaying(true)
        }
    }

    override fun clearHotCueB(index: Int) {
        if (index !in 0..2) return
        hotCuesB[index] = null
        DjSessionState.updateDeckBHotCues(hotCuesB.toList())
    }

    override fun toggleDeckAPlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
        DjSessionState.updateDeckAPlaying(p.isPlaying)
    }

    override fun toggleDeckBPlayPause() {
        val s = spare ?: return
        if (s.isPlaying) s.pause() else s.play()
        DjSessionState.updateDeckBPlaying(s.isPlaying)
    }

    override fun cueDeckA() {
        player?.seekTo(0L)
        DjSessionState.updateDeckAPosition(0L)
    }

    override fun cueDeckB() {
        spare?.seekTo(0L)
        DjSessionState.updateDeckBPosition(0L)
    }

    override fun setDeckASpeed(speed: Float) {
        val clamped = speed.coerceIn(0.85f, 1.15f)
        player?.playbackParameters = PlaybackParameters(clamped)
        DjSessionState.updateDeckASpeed(clamped)
    }

    override fun setDeckBSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.85f, 1.15f)
        spare?.playbackParameters = PlaybackParameters(clamped)
        DjSessionState.updateDeckBSpeed(clamped)
        DjSessionState.updateIsBeatSynced(false)
    }

    override fun syncDeckB() {
        val bpmA = DjSessionState.deckABpm.value
        val bpmB = DjSessionState.deckBBpm.value
        if (bpmA > 0 && bpmB > 0) {
            val ratio = (bpmA.toDouble() / bpmB.toDouble()).toFloat().coerceIn(0.75f, 1.33f)
            spare?.playbackParameters = PlaybackParameters(ratio)
            DjSessionState.updateDeckBSpeed(ratio)
            DjSessionState.updateIsBeatSynced(true)
        }
    }

    override fun autoDrop(durationMs: Long) {
        autoDropJob?.cancel()
        autoDropJob = scope.launch(Dispatchers.Main) {
            DjSessionState.updateIsAutoDropping(true)

            // Ensure Deck B is playing
            spare?.playWhenReady = true

            val startFraction = DjSessionState.crossfader.value
            val targetFraction = 1.0f
            val distance = targetFraction - startFraction
            val steps = (durationMs / 16L).coerceAtLeast(10L)

            for (i in 1..steps) {
                if (!isActive || !DjSessionState.isDjActive.value) break
                val progress = i.toFloat() / steps.toFloat()
                val currentFraction = startFraction + distance * progress
                setCrossfader(currentFraction)
                delay(16L)
            }

            if (isActive && DjSessionState.isDjActive.value) {
                commitTransition()
            }
            DjSessionState.updateIsAutoDropping(false)
        }
    }

    override fun commitTransition() {
        val oldActive = player ?: return
        val incoming = spare ?: return

        // Perform seamless handoff on PlaybackService
        service.adoptPlayerForDj(outgoing = oldActive, incoming = incoming)

        // Reset crossfader to Deck A (which is now the newly adopted track)
        DjSessionState.updateCrossfader(0.0f)
        DjSessionState.updateLowKillA(false)
        DjSessionState.updateLowKillB(false)
        DjSessionState.updateDeckASpeed(1.0f)
        DjSessionState.updateDeckBSpeed(1.0f)
        DjSessionState.updateDeckAFilter(0.0f)
        DjSessionState.updateDeckBFilter(0.0f)
        DjSessionState.updateDeckALoopBeats(null)
        DjSessionState.updateDeckBLoopBeats(null)
        DjSessionState.updateIsBeatSynced(false)

        loopA = null
        loopB = null
        hotCuesA.fill(null)
        hotCuesB.fill(null)
        DjSessionState.updateDeckAHotCues(hotCuesA.toList())
        DjSessionState.updateDeckBHotCues(hotCuesB.toList())

        service.getActiveFilter().open()
        service.getSpareFilter().open()

        // Update Deck A with newly current song
        val newCurrentSong = incoming.currentMediaItem?.toSong()
        DjSessionState.updateDeckASong(newCurrentSong)
        DjSessionState.updateDeckAPlaying(incoming.isPlaying)
        if (newCurrentSong != null) {
            val profile = AuraNeuralDjEngine.analyzeSong(newCurrentSong)
            DjSessionState.updateDeckABpm(profile.bpm)
            DjSessionState.updateDeckAKey(profile.key.toString())
        }

        // Clean up old active player (now spare)
        oldActive.playbackParameters = PlaybackParameters.DEFAULT

        // Preload next track on the new spare (Deck B)
        val count = incoming.mediaItemCount
        val nextIdx = if (count > 1) {
            val currIdx = incoming.currentMediaItemIndex
            if (currIdx + 1 < count) currIdx + 1 else 0
        } else {
            0
        }
        prepareDeckB(nextIdx)
        applyVolumes()
    }

    override fun selectDeckBSong(song: Song) {
        val active = player ?: return
        val count = active.mediaItemCount
        val idx = (0 until count).indexOfFirst { active.getMediaItemAt(it).mediaId == song.videoId }
        if (idx != -1) {
            prepareDeckB(idx)
        } else {
            val standby = spare ?: return
            val item = song.toMediaItem()
            DjSessionState.updateDeckBSong(song)
            DjSessionState.updateDeckBPlaying(false)
            DjSessionState.updateDeckBPosition(0L)
            DjSessionState.updateDeckBDuration(0L)
            loopB = null
            DjSessionState.updateDeckBLoopBeats(null)
            hotCuesB.fill(null)
            DjSessionState.updateDeckBHotCues(hotCuesB.toList())

            val profileB = AuraNeuralDjEngine.analyzeSong(song)
            DjSessionState.updateDeckBBpm(profileB.bpm)
            DjSessionState.updateDeckBKey(profileB.key.toString())

            standby.setMediaItem(item, 0L)
            standby.volume = 0f
            standby.playWhenReady = false
            standby.playbackParameters = PlaybackParameters.DEFAULT
            standby.prepare()
        }
    }

    override fun toggleLowKillA() {
        DjSessionState.updateLowKillA(!DjSessionState.lowKillA.value)
        applyVolumes()
    }

    override fun toggleLowKillB() {
        DjSessionState.updateLowKillB(!DjSessionState.lowKillB.value)
        applyVolumes()
    }

    override fun exitDjMode() {
        autoDropJob?.cancel()
        pollJob?.cancel()
        nudgeJobA?.cancel()
        nudgeJobB?.cancel()
        loopA = null
        loopB = null

        DjSessionState.updateIsActive(false)
        DjSessionState.updateIsAutoDropping(false)
        DjSessionState.updateDeckALoopBeats(null)
        DjSessionState.updateDeckBLoopBeats(null)
        DjSessionState.updateDeckAFilter(0.0f)
        DjSessionState.updateDeckBFilter(0.0f)
        DjSessionState.updateIsHamsterMode(false)
        DjSessionState.updateIsSplitCueEnabled(false)
        DjSessionState.updateIsBrakingA(false)
        DjSessionState.updateIsBrakingB(false)

        service.getActiveFilter().open()
        service.getActiveFilter().leftChannelGain = 1.0f
        service.getActiveFilter().rightChannelGain = 1.0f
        service.getSpareFilter().open()
        service.getSpareFilter().leftChannelGain = 1.0f
        service.getSpareFilter().rightChannelGain = 1.0f

        val p = player
        val s = spare

        // Restore active player volume
        p?.volume = service.currentVolumeMultiplier
        p?.playbackParameters = PlaybackParameters.DEFAULT

        // Mute and pause spare
        s?.playbackParameters = PlaybackParameters.DEFAULT
        s?.volume = 0f
        s?.pause()
    }
}
