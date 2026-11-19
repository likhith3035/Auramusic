package com.music.bitchord.data.dj

import com.music.bitchord.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CrossfaderCurve {
    SMOOTH, // Equal-power trigonometric curve: cos^2 + sin^2 = 1.0
    CUT,    // Scratch / cut curve: instant full power within 5% fader travel
}

/**
 * Reactive state and controls for the Dual-Deck DJ Mode in AuraMusic.
 * Manages Deck A (active session player), Deck B (standby ExoPlayer),
 * equal-power crossfader blending, beatmatching, DSP filters, loops, hot cues, and seamless track handoff.
 */
object DjSessionState {
    private val _isDjActive = MutableStateFlow(false)
    val isDjActive: StateFlow<Boolean> = _isDjActive.asStateFlow()

    private val _crossfader = MutableStateFlow(0.0f)
    /**
     * Crossfader position: 0.0f = full Deck A (100%), 1.0f = full Deck B (100%).
     * Midpoint 0.50f blends both tracks at equal acoustic power.
     */
    val crossfader: StateFlow<Float> = _crossfader.asStateFlow()

    private val _crossfaderCurve = MutableStateFlow(CrossfaderCurve.SMOOTH)
    val crossfaderCurve: StateFlow<CrossfaderCurve> = _crossfaderCurve.asStateFlow()

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

    private val _deckAFilter = MutableStateFlow(0.0f) // -1.0f (LPF) .. 0.0f (Neutral) .. +1.0f (HPF)
    val deckAFilter: StateFlow<Float> = _deckAFilter.asStateFlow()

    private val _deckBFilter = MutableStateFlow(0.0f)
    val deckBFilter: StateFlow<Float> = _deckBFilter.asStateFlow()

    private val _deckALoopBeats = MutableStateFlow<Int?>(null) // null, 2, 4, 8
    val deckALoopBeats: StateFlow<Int?> = _deckALoopBeats.asStateFlow()

    private val _deckBLoopBeats = MutableStateFlow<Int?>(null)
    val deckBLoopBeats: StateFlow<Int?> = _deckBLoopBeats.asStateFlow()

    private val _deckAHotCues = MutableStateFlow<List<Long?>>(listOf(null, null, null))
    val deckAHotCues: StateFlow<List<Long?>> = _deckAHotCues.asStateFlow()

    private val _deckBHotCues = MutableStateFlow<List<Long?>>(listOf(null, null, null))
    val deckBHotCues: StateFlow<List<Long?>> = _deckBHotCues.asStateFlow()

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
    fun updateCrossfaderCurve(curve: CrossfaderCurve) { _crossfaderCurve.value = curve }
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
    fun updateDeckAFilter(filter: Float) { _deckAFilter.value = filter.coerceIn(-1f, 1f) }
    fun updateDeckBFilter(filter: Float) { _deckBFilter.value = filter.coerceIn(-1f, 1f) }
    fun updateDeckALoopBeats(beats: Int?) { _deckALoopBeats.value = beats }
    fun updateDeckBLoopBeats(beats: Int?) { _deckBLoopBeats.value = beats }
    fun updateDeckAHotCues(cues: List<Long?>) { _deckAHotCues.value = cues }
    fun updateDeckBHotCues(cues: List<Long?>) { _deckBHotCues.value = cues }
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
    fun setCrossfaderCurve(curve: CrossfaderCurve)
    fun toggleDeckAPlayPause()
    fun toggleDeckBPlayPause()
    fun cueDeckA()
    fun cueDeckB()
    fun setDeckASpeed(speed: Float)
    fun setDeckBSpeed(speed: Float)
    fun nudgePitchA(positive: Boolean)
    fun nudgePitchB(positive: Boolean)
    fun scrubDeckA(deltaMs: Long)
    fun scrubDeckB(deltaMs: Long)
    fun setDeckAFilter(amount: Float)
    fun setDeckBFilter(amount: Float)
    fun toggleDeckALoop(beats: Int)
    fun toggleDeckBLoop(beats: Int)
    fun triggerHotCueA(index: Int)
    fun clearHotCueA(index: Int)
    fun triggerHotCueB(index: Int)
    fun clearHotCueB(index: Int)
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
    override fun setCrossfaderCurve(curve: CrossfaderCurve) {}
    override fun toggleDeckAPlayPause() {}
    override fun toggleDeckBPlayPause() {}
    override fun cueDeckA() {}
    override fun cueDeckB() {}
    override fun setDeckASpeed(speed: Float) {}
    override fun setDeckBSpeed(speed: Float) {}
    override fun nudgePitchA(positive: Boolean) {}
    override fun nudgePitchB(positive: Boolean) {}
    override fun scrubDeckA(deltaMs: Long) {}
    override fun scrubDeckB(deltaMs: Long) {}
    override fun setDeckAFilter(amount: Float) {}
    override fun setDeckBFilter(amount: Float) {}
    override fun toggleDeckALoop(beats: Int) {}
    override fun toggleDeckBLoop(beats: Int) {}
    override fun triggerHotCueA(index: Int) {}
    override fun clearHotCueA(index: Int) {}
    override fun triggerHotCueB(index: Int) {}
    override fun clearHotCueB(index: Int) {}
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
