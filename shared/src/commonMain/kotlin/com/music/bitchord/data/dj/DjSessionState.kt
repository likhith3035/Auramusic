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

    private val _isHamsterMode = MutableStateFlow(false)
    val isHamsterMode: StateFlow<Boolean> = _isHamsterMode.asStateFlow()

    private val _isSplitCueEnabled = MutableStateFlow(false)
    val isSplitCueEnabled: StateFlow<Boolean> = _isSplitCueEnabled.asStateFlow()

    private val _isBrakingA = MutableStateFlow(false)
    val isBrakingA: StateFlow<Boolean> = _isBrakingA.asStateFlow()

    private val _isBrakingB = MutableStateFlow(false)
    val isBrakingB: StateFlow<Boolean> = _isBrakingB.asStateFlow()

    private val _isAutoDropping = MutableStateFlow(false)
    val isAutoDropping: StateFlow<Boolean> = _isAutoDropping.asStateFlow()

    private val _lowKillA = MutableStateFlow(false)
    val lowKillA: StateFlow<Boolean> = _lowKillA.asStateFlow()

    private val _midKillA = MutableStateFlow(false)
    val midKillA: StateFlow<Boolean> = _midKillA.asStateFlow()

    private val _hiKillA = MutableStateFlow(false)
    val hiKillA: StateFlow<Boolean> = _hiKillA.asStateFlow()

    private val _lowKillB = MutableStateFlow(false)
    val lowKillB: StateFlow<Boolean> = _lowKillB.asStateFlow()

    private val _midKillB = MutableStateFlow(false)
    val midKillB: StateFlow<Boolean> = _midKillB.asStateFlow()

    private val _hiKillB = MutableStateFlow(false)
    val hiKillB: StateFlow<Boolean> = _hiKillB.asStateFlow()

    private val _deckAVolume = MutableStateFlow(1.0f)
    val deckAVolume: StateFlow<Float> = _deckAVolume.asStateFlow()

    private val _deckBVolume = MutableStateFlow(1.0f)
    val deckBVolume: StateFlow<Float> = _deckBVolume.asStateFlow()

    private val _deckATrim = MutableStateFlow(1.0f)
    val deckATrim: StateFlow<Float> = _deckATrim.asStateFlow()

    private val _deckBTrim = MutableStateFlow(1.0f)
    val deckBTrim: StateFlow<Float> = _deckBTrim.asStateFlow()

    private val _keyLockA = MutableStateFlow(true)
    val keyLockA: StateFlow<Boolean> = _keyLockA.asStateFlow()

    private val _keyLockB = MutableStateFlow(true)
    val keyLockB: StateFlow<Boolean> = _keyLockB.asStateFlow()

    private val _deckAPitchPercent = MutableStateFlow(0.0f)
    val deckAPitchPercent: StateFlow<Float> = _deckAPitchPercent.asStateFlow()

    private val _deckBPitchPercent = MutableStateFlow(0.0f)
    val deckBPitchPercent: StateFlow<Float> = _deckBPitchPercent.asStateFlow()

    private val _pflA = MutableStateFlow(false)
    val pflA: StateFlow<Boolean> = _pflA.asStateFlow()

    private val _pflB = MutableStateFlow(false)
    val pflB: StateFlow<Boolean> = _pflB.asStateFlow()

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
    fun updateIsHamsterMode(enabled: Boolean) { _isHamsterMode.value = enabled }
    fun updateIsSplitCueEnabled(enabled: Boolean) { _isSplitCueEnabled.value = enabled }
    fun updateIsBrakingA(braking: Boolean) { _isBrakingA.value = braking }
    fun updateIsBrakingB(braking: Boolean) { _isBrakingB.value = braking }
    fun updateIsAutoDropping(dropping: Boolean) { _isAutoDropping.value = dropping }
    fun updateLowKillA(kill: Boolean) { _lowKillA.value = kill }
    fun updateMidKillA(kill: Boolean) { _midKillA.value = kill }
    fun updateHiKillA(kill: Boolean) { _hiKillA.value = kill }
    fun updateLowKillB(kill: Boolean) { _lowKillB.value = kill }
    fun updateMidKillB(kill: Boolean) { _midKillB.value = kill }
    fun updateHiKillB(kill: Boolean) { _hiKillB.value = kill }
    fun updateDeckAVolume(volume: Float) { _deckAVolume.value = volume.coerceIn(0f, 1f) }
    fun updateDeckBVolume(volume: Float) { _deckBVolume.value = volume.coerceIn(0f, 1f) }
    fun updateDeckATrim(trim: Float) { _deckATrim.value = trim.coerceIn(0.5f, 2.0f) }
    fun updateDeckBTrim(trim: Float) { _deckBTrim.value = trim.coerceIn(0.5f, 2.0f) }
    fun updateKeyLockA(locked: Boolean) { _keyLockA.value = locked }
    fun updateKeyLockB(locked: Boolean) { _keyLockB.value = locked }
    fun updateDeckAPitchPercent(percent: Float) { _deckAPitchPercent.value = percent.coerceIn(-0.16f, 0.16f) }
    fun updateDeckBPitchPercent(percent: Float) { _deckBPitchPercent.value = percent.coerceIn(-0.16f, 0.16f) }
    fun updatePflA(pfl: Boolean) { _pflA.value = pfl }
    fun updatePflB(pfl: Boolean) { _pflB.value = pfl }
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
    fun triggerBrakeA()
    fun triggerBrakeB()
    fun setDeckASpeed(speed: Float)
    fun setDeckBSpeed(speed: Float)
    fun nudgePitchA(positive: Boolean)
    fun nudgePitchB(positive: Boolean)
    fun scrubDeckA(deltaMs: Long)
    fun scrubDeckB(deltaMs: Long)
    fun seekDeckA(positionMs: Long)
    fun seekDeckB(positionMs: Long)
    fun beatJumpA(beats: Int)
    fun beatJumpB(beats: Int)
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
    fun selectDeckASong(song: Song)
    fun selectDeckBSong(song: Song)
    fun reorderQueue(fromIndex: Int, toIndex: Int)
    fun removeQueueItem(index: Int)
    fun toggleLowKillA()
    fun toggleLowKillB()
    fun toggleMidKillA()
    fun toggleHiKillA()
    fun toggleMidKillB()
    fun toggleHiKillB()
    fun setDeckAVolume(volume: Float)
    fun setDeckBVolume(volume: Float)
    fun setDeckATrim(trim: Float)
    fun setDeckBTrim(trim: Float)
    fun toggleKeyLockA()
    fun toggleKeyLockB()
    fun setDeckAPitchPercent(percent: Float)
    fun setDeckBPitchPercent(percent: Float)
    fun togglePflA()
    fun togglePflB()
    fun swapDecks()
    fun toggleHamsterMode()
    fun toggleSplitCue()
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
    override fun triggerBrakeA() {}
    override fun triggerBrakeB() {}
    override fun setDeckASpeed(speed: Float) {}
    override fun setDeckBSpeed(speed: Float) {}
    override fun nudgePitchA(positive: Boolean) {}
    override fun nudgePitchB(positive: Boolean) {}
    override fun scrubDeckA(deltaMs: Long) {}
    override fun scrubDeckB(deltaMs: Long) {}
    override fun seekDeckA(positionMs: Long) {}
    override fun seekDeckB(positionMs: Long) {}
    override fun beatJumpA(beats: Int) {}
    override fun beatJumpB(beats: Int) {}
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
    override fun selectDeckASong(song: Song) {}
    override fun selectDeckBSong(song: Song) {}
    override fun reorderQueue(fromIndex: Int, toIndex: Int) {}
    override fun removeQueueItem(index: Int) {}
    override fun toggleLowKillA() {}
    override fun toggleLowKillB() {}
    override fun toggleMidKillA() {}
    override fun toggleHiKillA() {}
    override fun toggleMidKillB() {}
    override fun toggleHiKillB() {}
    override fun setDeckAVolume(volume: Float) {}
    override fun setDeckBVolume(volume: Float) {}
    override fun setDeckATrim(trim: Float) {}
    override fun setDeckBTrim(trim: Float) {}
    override fun toggleKeyLockA() {}
    override fun toggleKeyLockB() {}
    override fun setDeckAPitchPercent(percent: Float) {}
    override fun setDeckBPitchPercent(percent: Float) {}
    override fun togglePflA() {}
    override fun togglePflB() {}
    override fun swapDecks() {}
    override fun toggleHamsterMode() {}
    override fun toggleSplitCue() {}
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
