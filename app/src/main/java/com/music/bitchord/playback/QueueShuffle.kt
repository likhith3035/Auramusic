package com.music.bitchord.playback

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.ShuffleMode
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 3-State Queue Shuffle:
 * OFF -> STANDARD -> SMART -> OFF
 *
 * STANDARD: shuffles context and autoplay sections.
 * SMART: interleaves 1 harmonic recommendation every 3 tracks (cadence 3).
 * OFF: strips all smart recommendations and restores pristine original queue order.
 *
 * User-queued tracks (USER_QUEUE) are NEVER shuffled: they reflect deliberate user
 * intent and remain pinned at the front. Context tracks (CONTEXT) and AutoPlay tracks
 * (AUTOPLAY) are shuffled among themselves.
 */
object QueueShuffle {

    private val _mode = MutableStateFlow(ShuffleMode.OFF)
    val mode: StateFlow<ShuffleMode> = _mode.asStateFlow()

    private val _enabled = MutableStateFlow(false)

    /** Whether the queue is currently held in shuffled or smart shuffled order. */
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setMode(mode: ShuffleMode) {
        _mode.value = mode
        _enabled.value = mode != ShuffleMode.OFF
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        _mode.value = if (enabled) ShuffleMode.STANDARD else ShuffleMode.OFF
    }

    /** Entry IDs in their pre-shuffle order. Empty while shuffle is off. */
    private var original: List<String> = emptyList()

    fun toggle(player: Player, coroutineScope: CoroutineScope? = null) {
        val nextMode = _mode.value.next()
        when (nextMode) {
            ShuffleMode.STANDARD -> {
                shuffle(player)
                setMode(ShuffleMode.STANDARD)
            }
            ShuffleMode.SMART -> {
                setMode(ShuffleMode.SMART)
                smartShuffle(player, coroutineScope)
            }
            ShuffleMode.OFF -> {
                restore(player)
                setMode(ShuffleMode.OFF)
            }
        }
        // Persist the new state so it survives app restarts.
        AppSettings.setShuffleMode(_mode.value)
    }

    /**
     * Turns shuffle on without touching the current queue — for the Shuffle
     * button on an album or playlist page, where the queue it applies to is the
     * one about to replace this one. [playSongs] builds that one shuffled.
     */
    fun enableForNextQueue() {
        original = emptyList()
        setMode(ShuffleMode.STANDARD)
        AppSettings.setShuffleMode(ShuffleMode.STANDARD)
    }

    /**
     * The order a queue should go in when it is started while shuffle is on:
     * the track the user picked leads, the rest follow at random. The order it
     * arrived in is remembered, so turning shuffle off restores it.
     */
    fun startingOrder(songs: List<Song>, startIndex: Int): List<Song> {
        original = songs.map { it.queueEntryId ?: it.videoId }
        val rest = songs.filterIndexed { i, _ -> i != startIndex }
        val userQueue = rest.filter { it.queueTier == QueueTier.USER_QUEUE }
        val context = rest.filter { it.queueTier == QueueTier.CONTEXT }.shuffled()
        val autoplay = rest.filter { it.queueTier == QueueTier.AUTOPLAY }.shuffled()
        return listOf(songs[startIndex]) + userQueue + context + autoplay
    }

    /**
     * Rearranges everything after the playing track. That track keeps playing,
     * and whatever sits above it stays there — those have had their turn.
     *
     * Invariant: USER_QUEUE tracks are never shuffled. CONTEXT and AUTOPLAY are shuffled
     * within their respective sections.
     */
    private fun shuffle(player: Player) {
        val items = player.queueItems()
        original = items.map { it.queueEntryId ?: it.mediaId }
        val from = player.currentMediaItemIndex + 1
        val upcoming = items.drop(from)
        if (upcoming.isEmpty()) {
            _enabled.value = true
            return
        }

        val userQueueIndices = upcoming.indices.filter { upcoming[it].queueTier == QueueTier.USER_QUEUE }
        val contextIndices = upcoming.indices.filter { upcoming[it].queueTier == QueueTier.CONTEXT }
        val autoplayIndices = upcoming.indices.filter { upcoming[it].queueTier == QueueTier.AUTOPLAY }

        applyOrder(
            player,
            from,
            userQueueIndices + shuffledSection(contextIndices) + shuffledSection(autoplayIndices),
        )
        _enabled.value = true
    }

    private fun smartShuffle(player: Player, coroutineScope: CoroutineScope?) {
        val scope = coroutineScope ?: CoroutineScope(Dispatchers.Main)
        val from = player.currentMediaItemIndex + 1
        val currentIndex = player.currentMediaItemIndex
        val initialCount = player.mediaItemCount
        val items = player.queueItems()
        if (items.isEmpty() || from >= items.size) return

        val songs = items.map { it.toSong() }
        scope.launch {
            val enriched = withContext(Dispatchers.IO) {
                SmartShuffleEngine.enrichQueue(songs, currentIndex)
            }
            withContext(Dispatchers.Main) {
                if (enriched.size > songs.size && player.mediaItemCount == initialCount && player.currentMediaItemIndex == currentIndex) {
                    val newUpcoming = enriched.drop(from).map { it.toMediaItem() }
                    player.replaceMediaItems(from, initialCount, newUpcoming)
                }
            }
        }
    }

    /**
     * Randomises one queue section but never returns its unchanged order when
     * at least two tracks can move. A mathematically valid identity shuffle is
     * surprisingly common in short queues (one chance in two for two tracks),
     * and reads exactly like the first tap was ignored.
     */
    private fun shuffledSection(indices: List<Int>): List<Int> =
        avoidIdentityShuffle(indices, indices.shuffled())

    internal fun avoidIdentityShuffle(original: List<Int>, shuffled: List<Int>): List<Int> {
        if (original.size <= 1 || shuffled != original) return shuffled
        return shuffled.drop(1) + shuffled.first()
    }

    /** Puts the tracks still to come back into the order they were queued in. */
    private fun restore(player: Player) {
        val items = player.queueItems()
        val from = player.currentMediaItemIndex + 1
        val upcoming = items.drop(from)
        if (upcoming.isEmpty()) {
            original = emptyList()
            setMode(ShuffleMode.OFF)
            return
        }

        // If smart recommendations were added, strip them first
        val nonSmartUpcoming = upcoming.filterNot { it.toSong().isSmartRecommendation }
        if (nonSmartUpcoming.size < upcoming.size) {
            player.replaceMediaItems(from, player.mediaItemCount, nonSmartUpcoming)
        }

        val currentRemaining = player.queueItems().drop(from)
        val restored = restoreOrder(currentRemaining.map { it.queueEntryId ?: it.mediaId }, original)
        applyOrder(player, from, sections(restored, currentRemaining))
        original = emptyList()
        setMode(ShuffleMode.OFF)
    }

    /**
     * Where each of [upcoming] belongs once [original] is put back, as indices
     * into [upcoming].
     */
    internal fun restoreOrder(upcoming: List<String>, original: List<String>): List<Int> {
        val positions = HashMap<String, ArrayDeque<Int>>(upcoming.size)
        upcoming.forEachIndexed { index, id ->
            positions.getOrPut(id) { ArrayDeque() }.addLast(index)
        }
        val placed = BooleanArray(upcoming.size)
        val out = ArrayList<Int>(upcoming.size)
        for (id in original) {
            val index = positions[id]?.removeFirstOrNull() ?: continue
            placed[index] = true
            out += index
        }
        for (index in upcoming.indices) if (!placed[index]) out += index
        return out
    }

    /** [order] with sections strictly maintained: USER_QUEUE -> CONTEXT -> AUTOPLAY. */
    private fun sections(order: List<Int>, upcoming: List<MediaItem>): List<Int> =
        order.filter { upcoming[it].queueTier == QueueTier.USER_QUEUE } +
            order.filter { upcoming[it].queueTier == QueueTier.CONTEXT } +
            order.filter { upcoming[it].queueTier == QueueTier.AUTOPLAY }

    /**
     * Rearranges the live queue from [from] onwards, [order] naming where each
     * slot's new occupant is standing now.
     */
    private fun applyOrder(player: Player, from: Int, order: List<Int>) {
        if (order.isEmpty()) return
        if (player is MediaController) {
            player.sendCustomCommand(
                SessionCommand(ACTION_REORDER_QUEUE, Bundle.EMPTY),
                bundleOf(
                    EXTRA_REORDER_FROM to from,
                    EXTRA_REORDER_ORDER to order.toIntArray(),
                ),
            )
        } else {
            reorder(player, from, order.toIntArray())
        }
    }

    /** [applyOrder] as it arrives at the session — see [ACTION_REORDER_QUEUE]. */
    fun reorderFromCommand(player: Player, args: Bundle) {
        val from = args.getInt(EXTRA_REORDER_FROM, -1)
        val order = args.getIntArray(EXTRA_REORDER_ORDER) ?: return
        if (from >= 0) reorder(player, from, order)
    }

    private fun reorder(player: Player, from: Int, order: IntArray) {
        if (order.isEmpty() || from + order.size > player.mediaItemCount) return
        val target = List(order.size) { player.getMediaItemAt(from + order[it]) }
        player.replaceMediaItems(from, from + order.size, target)
    }

    private fun Player.queueItems(): List<MediaItem> =
        List(mediaItemCount) { getMediaItemAt(it) }
}
