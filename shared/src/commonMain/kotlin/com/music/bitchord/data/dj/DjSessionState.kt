package com.music.bitchord.data.dj

import com.music.bitchord.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive state and controls for the Dual-Deck DJ Mode in AuraMusic.
 * Manages Deck A (active session player), Deck B (standby ExoPlayer),
 * equal-power crossfader blending, beatmatching, and seamless track handoff.
 */
object DjSessionState {
    private val _isDjActive = MutableStateFlow(false)
    val isDjActive: StateFlow<Boolean> = _isDjActive.asStateFlow()

    private val _crossfader = MutableStateFlow(0.0f)
    /**
     * Crossfader position: 0.0f = full Deck A (100%), 1.0f = full Deck B (100%).
     * Midpoint 0.50f blends both tracks at equal acoustic power (cos^2 + sin^2 = 1.0).
     */
    val crossfader: StateFlow<Float> = _crossfader.asStateFlow()

    private val _deckASong = MutableStateFlow<Song?>(null)
    val deckASong: StateFlow<Song?> = _deckASong.asStateFlow()

    private val _deckBSong = MutableStateFlow<Song?>(null)
    val deckBSong: StateFlow<Song?> = _deckBSong.asStateFlow()

    private val _deckAPlaying = MutableStateFlow(false)
    val deckAPlaying: StateFlow<Boolean> = _deckAPlaying.asStateFlow()

    private val _deckBPlaying = MutableStateFlow(false)
    val deckBPlaying: StateFlow<Boolean> = _deckBPlaying.asStateFlow()

    private val _deckAPositionMs = MutableStateFlow(0L)
    val deckAPositionMs: StateFlow<Long> = _deckAPositionMs.asStateFlow()

    private val _deckADurationMs = MutableStateFlow(0L)
    val deckADurationMs: StateFlow<Long> = _deckADurationMs.asStateFlow()

    private val _deckBPositionMs = MutableStateFlow(0L)
    val deckBPositionMs: StateFlow<Long> = _deckBPositionMs.asStateFlow()

    private val _deckBDurationMs = MutableStateFlow(0L)
    val deckBDurationMs: StateFlow<Long> = _deckBDurationMs.asStateFlow()

    private val _deckABpm = MutableStateFlow(120)
    val deckABpm: StateFlow<Int> = _deckABpm.asStateFlow()

    private val _deckBBpm = MutableStateFlow(120)
    val deckBBpm: StateFlow<Int> = _deckBBpm.asStateFlow()

    private val _deckAKey = MutableStateFlow("8A")
    val deckAKey: StateFlow<String> = _deckAKey.asStateFlow()

    private val _deckBKey = MutableStateFlow("8A")
    val deckBKey: StateFlow<String> = _deckBKey.asStateFlow()

    private val _deckASpeed = MutableStateFlow(1.0f)
    val deckASpeed: StateFlow<Float> = _deckASpeed.asStateFlow()

    private val _deckBSpeed = MutableStateFlow(1.0f)
    val deckBSpeed: StateFlow<Float> = _deckBSpeed.asStateFlow()

    private val _isBeatSynced = MutableStateFlow(false)
    val isBeatSynced: StateFlow<Boolean> = _isBeatSynced.asStateFlow()

    private val _isAutoDropping = MutableStateFlow(false)
    val isAutoDropping: StateFlow<Boolean> = _isAutoDropping.asStateFlow()

    private val _lowKillA = MutableStateFlow(false)
    val lowKillA: StateFlow<Boolean> = _lowKillA.asStateFlow()

    private val _lowKillB = MutableStateFlow(false)
    val lowKillB: StateFlow<Boolean> = _lowKillB.asStateFlow()

    private val _candidateQueue = MutableStateFlow<List<Song>>(emptyList())
    val candidateQueue: StateFlow<List<Song>> = _candidateQueue.asStateFlow()

    fun updateIsActive(active: Boolean) { _isDjActive.value = active }
    fun updateCrossfader(value: Float) { _crossfader.value = value.coerceIn(0f, 1f) }
    fun updateDeckASong(song: Song?) { _deckASong.value = song }
    fun updateDeckBSong(song: Song?) { _deckBSong.value = song }
    fun updateDeckAPlaying(playing: Boolean) { _deckAPlaying.value = playing }
    fun updateDeckBPlaying(playing: Boolean) { _deckBPlaying.value = playing }
    fun updateDeckAPosition(positionMs: Long) { _deckAPositionMs.value = positionMs.coerceAtLeast(0L) }
    fun updateDeckADuration(durationMs: Long) { _deckADurationMs.value = durationMs.coerceAtLeast(0L) }
    fun updateDeckBPosition(positionMs: Long) { _deckBPositionMs.value = positionMs.coerceAtLeast(0L) }
    fun updateDeckBDuration(durationMs: Long) { _deckBDurationMs.value = durationMs.coerceAtLeast(0L) }
    fun updateDeckABpm(bpm: Int) { _deckABpm.value = bpm }
    fun updateDeckBBpm(bpm: Int) { _deckBBpm.value = bpm }
    fun updateDeckAKey(key: String) { _deckAKey.value = key }
    fun updateDeckBKey(key: String) { _deckBKey.value = key }
    fun updateDeckASpeed(speed: Float) { _deckASpeed.value = speed }
    fun updateDeckBSpeed(speed: Float) { _deckBSpeed.value = speed }
    fun updateIsBeatSynced(synced: Boolean) { _isBeatSynced.value = synced }
    fun updateIsAutoDropping(dropping: Boolean) { _isAutoDropping.value = dropping }
    fun updateLowKillA(kill: Boolean) { _lowKillA.value = kill }
    fun updateLowKillB(kill: Boolean) { _lowKillB.value = kill }
    fun updateCandidateQueue(queue: List<Song>) { _candidateQueue.value = queue }
}

/**
 * Controller contract driving the underlying audio engine for DJ features.
 */
interface DjAudioController {
    fun startDjMode()
    fun exitDjMode()
    fun setCrossfader(fraction: Float)
    fun toggleDeckAPlayPause()
    fun toggleDeckBPlayPause()
    fun cueDeckA()
    fun cueDeckB()
    fun setDeckASpeed(speed: Float)
    fun setDeckBSpeed(speed: Float)
    fun syncDeckB()
    fun autoDrop(durationMs: Long = 3500L)
    fun commitTransition()
    fun selectDeckBSong(song: Song)
    fun toggleLowKillA()
    fun toggleLowKillB()
}

/**
 * Default no-op implementation when playback engine is not yet attached.
 */
object NoOpDjAudioController : DjAudioController {
    override fun startDjMode() {}
    override fun exitDjMode() {}
    override fun setCrossfader(fraction: Float) {}
    override fun toggleDeckAPlayPause() {}
    override fun toggleDeckBPlayPause() {}
    override fun cueDeckA() {}
    override fun cueDeckB() {}
    override fun setDeckASpeed(speed: Float) {}
    override fun setDeckBSpeed(speed: Float) {}
    override fun syncDeckB() {}
    override fun autoDrop(durationMs: Long) {}
    override fun commitTransition() {}
    override fun selectDeckBSong(song: Song) {}
    override fun toggleLowKillA() {}
    override fun toggleLowKillB() {}
}

/**
 * Global bridge allowing UI components to invoke DJ actions on the platform audio service.
 */
object DjBridge {
    private var controller: DjAudioController = NoOpDjAudioController

    fun install(newController: DjAudioController) {
        controller = newController
    }

    val current: DjAudioController
        get() = controller
}
