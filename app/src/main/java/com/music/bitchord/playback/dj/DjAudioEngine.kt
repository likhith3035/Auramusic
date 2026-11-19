package com.music.bitchord.playback.dj

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.music.bitchord.data.ai.AuraNeuralDjEngine
import com.music.bitchord.data.dj.DjAudioController
import com.music.bitchord.data.dj.DjBridge
import com.music.bitchord.data.dj.DjSessionState
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackService
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
 * standby ExoPlayer (Deck B) using equal-power trigonometry.
 */
class DjAudioEngine(
    private val service: PlaybackService,
    private val scope: CoroutineScope,
) : DjAudioController {

    private var pollJob: Job? = null
    private var autoDropJob: Job? = null

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
        DjSessionState.updateIsAutoDropping(false)
        DjSessionState.updateLowKillA(false)
        DjSessionState.updateLowKillB(false)
        DjSessionState.updateDeckASpeed(1.0f)
        DjSessionState.updateDeckBSpeed(1.0f)
        DjSessionState.updateIsBeatSynced(false)

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
            while (isActive && DjSessionState.isDjActive.value) {
                val p = player
                if (p != null) {
                    DjSessionState.updateDeckAPosition(p.currentPosition)
                    DjSessionState.updateDeckADuration(p.duration)
                    DjSessionState.updateDeckAPlaying(p.isPlaying)
                }

                val s = spare
                if (s != null) {
                    DjSessionState.updateDeckBPosition(s.currentPosition)
                    DjSessionState.updateDeckBDuration(s.duration)
                    DjSessionState.updateDeckBPlaying(s.isPlaying)
                }

                delay(100L)
            }
        }
    }

    override fun setCrossfader(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        DjSessionState.updateCrossfader(clamped)

        // If Deck B volume starts rising and Deck B is not playing, start Deck B
        val standby = spare
        if (standby != null && clamped > 0.04f && !standby.isPlaying && !standby.playWhenReady) {
            standby.playWhenReady = true
        }

        applyVolumes()
    }

    fun applyVolumes() {
        val p = player
        val s = spare
        val fraction = DjSessionState.crossfader.value
        val master = service.currentVolumeMultiplier

        // Equal-power crossfade curve: cos(x * pi/2) and sin(x * pi/2)
        val angle = fraction * (PI / 2.0).toFloat()
        val gainA = cos(angle)
        val gainB = sin(angle)

        val killA = if (DjSessionState.lowKillA.value) 0.30f else 1.0f
        val killB = if (DjSessionState.lowKillB.value) 0.30f else 1.0f

        p?.volume = (gainA * killA * master).coerceIn(0f, 1f)
        s?.volume = (gainB * killB * master).coerceIn(0f, 1f)
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
        DjSessionState.updateIsBeatSynced(false)

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
        DjSessionState.updateIsActive(false)
        DjSessionState.updateIsAutoDropping(false)

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
